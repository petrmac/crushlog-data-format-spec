package app.crushlog.cldf.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.Error;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.dialect.BasicDialectRegistry;
import com.networknt.schema.dialect.Dialects;
import lombok.extern.slf4j.Slf4j;

/** Validates JSON data against CLDF schemas using the NetworkNT JSON Schema Validator. */
@Slf4j
public class SchemaValidator {

  private static final String DEFAULT_SCHEMAS_BASE_PATH = "/schemas/";
  private static final String SCHEMA_URL_PREFIX = "https://cldf.io/schemas/";
  private static final Map<String, String> FILE_TO_SCHEMA_MAPPING = new HashMap<>();

  // Schema files that may be referenced via $ref
  private static final String[] ALL_SCHEMA_FILES = {
      "manifest.schema.json",
      "locations.schema.json",
      "climbs.schema.json",
      "sessions.schema.json",
      "routes.schema.json",
      "sectors.schema.json",
      "tags.schema.json",
      "media-metadata.schema.json",
      "media.schema.json",
      "checksums.schema.json",
      "definitions.schema.json" // Common definitions referenced by other schemas
  };

  private final String schemasBasePath;

  static {
    FILE_TO_SCHEMA_MAPPING.put("manifest.json", "manifest.schema.json");
    FILE_TO_SCHEMA_MAPPING.put("locations.json", "locations.schema.json");
    FILE_TO_SCHEMA_MAPPING.put("climbs.json", "climbs.schema.json");
    FILE_TO_SCHEMA_MAPPING.put("sessions.json", "sessions.schema.json");
    FILE_TO_SCHEMA_MAPPING.put("routes.json", "routes.schema.json");
    FILE_TO_SCHEMA_MAPPING.put("sectors.json", "sectors.schema.json");
    FILE_TO_SCHEMA_MAPPING.put("tags.json", "tags.schema.json");
    FILE_TO_SCHEMA_MAPPING.put("media-metadata.json", "media-metadata.schema.json");
    FILE_TO_SCHEMA_MAPPING.put("checksums.json", "checksums.schema.json");
  }

  private final ObjectMapper objectMapper;
  private final SchemaRegistry schemaRegistry;
  private final Map<String, Schema> schemaCache;

  public SchemaValidator() {
    this(DEFAULT_SCHEMAS_BASE_PATH);
  }

  public SchemaValidator(String schemasBasePath) {
    this.schemasBasePath = schemasBasePath;
    this.objectMapper = new ObjectMapper();
    // Register JSR310 module for Java 8 time types
    this.objectMapper.findAndRegisterModules();
    // Configure to omit null fields during serialization
    this.objectMapper.setSerializationInclusion(
        com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);
    // Configure to write dates as strings, not arrays
    this.objectMapper.disable(
        com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // Pre-load all schemas to map URLs to classpath resources
    Map<String, String> schemaResources = loadAllSchemas(schemasBasePath);

    // Create schema registry with Draft 7 dialect and URL mapping (json-schema-validator 2.0.0+)
    this.schemaRegistry = SchemaRegistry.builder()
        .defaultDialectId("http://json-schema.org/draft-07/schema#")
        .dialectRegistry(new BasicDialectRegistry(Dialects.getDraft7()))
        .schemaLoader(loader -> loader
            .resourceLoaders(resources -> resources
                .resources(schemaResources)))
        .build();
    this.schemaCache = new HashMap<>();
  }

  /**
   * Load all schema files from classpath and map them to their URL identifiers.
   */
  private Map<String, String> loadAllSchemas(String basePath) {
    Map<String, String> schemas = new HashMap<>();
    for (String schemaFile : ALL_SCHEMA_FILES) {
      String resourcePath = basePath + schemaFile;
      try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
        if (is != null) {
          String content = new String(is.readAllBytes());
          // Map both the full URL and just the filename
          schemas.put(SCHEMA_URL_PREFIX + schemaFile, content);
          log.debug("Loaded schema: {} -> {}", SCHEMA_URL_PREFIX + schemaFile, schemaFile);
        } else {
          log.debug("Schema not found in classpath: {}", resourcePath);
        }
      } catch (IOException e) {
        log.warn("Failed to load schema: {}", resourcePath, e);
      }
    }
    return schemas;
  }

  /**
   * Validates JSON content and returns a ValidationResult. This is the preferred method for new
   * code as it provides a cleaner API without throwing exceptions for validation failures.
   *
   * @param filename The name of the file being validated (e.g., "manifest.json")
   * @param jsonContent The JSON content as a byte array
   * @return ValidationResult containing success/failure status and any errors
   */
  public ValidationResult validateWithResult(String filename, byte[] jsonContent) {
    try {
      String schemaFile = FILE_TO_SCHEMA_MAPPING.get(filename);
      if (schemaFile == null) {
        log.debug("No schema mapping found for file: {}", filename);
        return ValidationResult.success(filename); // Allow unknown files
      }

      Schema schema = loadSchema(schemaFile);
      JsonNode jsonNode = objectMapper.readTree(jsonContent);

      List<Error> errors = schema.validate(jsonNode);

      if (errors.isEmpty()) {
        return ValidationResult.success(filename);
      }

      List<ValidationResult.ValidationError> validationErrors = new ArrayList<>();
      for (Error error : errors) {
        validationErrors.add(
            new ValidationResult.ValidationError(
                error.getInstanceLocation().toString(), error.getMessage(), error.getKeyword()));
      }

      return ValidationResult.failure(filename, validationErrors);
    } catch (IOException e) {
      // If we can't parse the JSON or load the schema, return a failure
      return ValidationResult.failure(
          filename,
          List.of(
              new ValidationResult.ValidationError(
                  "$", "Failed to validate: " + e.getMessage(), "parse_error")));
    }
  }

  /**
   * Validates an object against the appropriate schema and returns a ValidationResult. This is the
   * preferred method for new code as it provides a cleaner API without throwing exceptions for
   * validation failures.
   *
   * @param filename The name of the file being validated
   * @param object The object to validate
   * @return ValidationResult containing success/failure status and any errors
   */
  public ValidationResult validateObjectWithResult(String filename, Object object) {
    try {
      byte[] jsonBytes = objectMapper.writeValueAsBytes(object);
      return validateWithResult(filename, jsonBytes);
    } catch (Exception e) {
      // If we can't serialize the object, return a failure
      return ValidationResult.failure(
          filename,
          List.of(
              new ValidationResult.ValidationError(
                  "$", "Failed to serialize object: " + e.getMessage(), "serialization_error")));
    }
  }

  private Schema loadSchema(String schemaFile) throws IOException {
    if (schemaCache.containsKey(schemaFile)) {
      return schemaCache.get(schemaFile);
    }

    // Load from classpath resources
    String resourcePath = schemasBasePath + schemaFile;
    try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
      if (is == null) {
        throw new IOException("Schema not found: " + resourcePath);
      }
      String schemaContent = new String(is.readAllBytes());

      // Create schema from content (json-schema-validator 2.0.0+)
      Schema schema = schemaRegistry.getSchema(schemaContent, InputFormat.JSON);
      schemaCache.put(schemaFile, schema);
      return schema;
    }
  }
}
