package app.crushlog.cldf.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import lombok.extern.slf4j.Slf4j;

/** Validates JSON data against CLDF schemas using the NetworkNT JSON Schema Validator. */
@Slf4j
public class SchemaValidator {

  private static final String DEFAULT_SCHEMAS_BASE_PATH = "/schemas/";
  private static final Map<String, String> FILE_TO_SCHEMA_MAPPING = new HashMap<>();

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
  private final JsonSchemaFactory schemaFactory;
  private final Map<String, JsonSchema> schemaCache;

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
    // Create schema factory with custom config to disable network fetching
    SchemaValidatorsConfig config = new SchemaValidatorsConfig();
    config.setHandleNullableField(true);
    this.schemaFactory =
        JsonSchemaFactory.getInstance(
            SpecVersion.VersionFlag.V7,
            builder ->
                builder.schemaMappers(
                    schemaMappers ->
                        schemaMappers.mapPrefix(
                            "https://cldf.io/schemas/", "classpath:/schemas/")));
    this.schemaCache = new HashMap<>();
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

      JsonSchema schema = loadSchema(schemaFile);
      JsonNode jsonNode = objectMapper.readTree(jsonContent);

      Set<ValidationMessage> errors = schema.validate(jsonNode);

      if (errors.isEmpty()) {
        return ValidationResult.success(filename);
      }

      List<ValidationResult.ValidationError> validationErrors = new ArrayList<>();
      for (ValidationMessage error : errors) {
        validationErrors.add(
            new ValidationResult.ValidationError(
                error.getInstanceLocation().toString(), error.getMessage(), error.getType()));
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

  private JsonSchema loadSchema(String schemaFile) throws IOException {
    if (schemaCache.containsKey(schemaFile)) {
      return schemaCache.get(schemaFile);
    }

    // Load from classpath resources
    String resourcePath = schemasBasePath + schemaFile;
    try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
      if (is == null) {
        throw new IOException("Schema not found: " + resourcePath);
      }
      JsonNode schemaNode = objectMapper.readTree(is);

      // Create config that maps the schema ID URLs to classpath resources
      SchemaValidatorsConfig config = new SchemaValidatorsConfig();
      config.setHandleNullableField(true);
      config.setTypeLoose(false);
      // Don't fail on fields that have default values
      config.setReadOnly(false);
      config.setWriteOnly(false);

      JsonSchema schema = schemaFactory.getSchema(schemaNode, config);
      schemaCache.put(schemaFile, schema);
      return schema;
    }
  }
}
