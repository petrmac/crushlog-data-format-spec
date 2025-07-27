package io.cldf.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
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

  private static final String SCHEMAS_BASE_PATH = "/schemas/";
  private static final Map<String, String> FILE_TO_SCHEMA_MAPPING = new HashMap<>();

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
    this.objectMapper = new ObjectMapper();
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
   * Validates JSON content against the appropriate schema based on the filename.
   *
   * @param filename The name of the file being validated (e.g., "manifest.json")
   * @param jsonContent The JSON content as a byte array
   * @return true if valid, false otherwise
   * @throws IOException if schema cannot be loaded or content cannot be parsed
   */
  public boolean validate(String filename, byte[] jsonContent) throws IOException {
    String schemaFile = FILE_TO_SCHEMA_MAPPING.get(filename);
    if (schemaFile == null) {
      log.warn("No schema mapping found for file: {}", filename);
      return true; // Allow unknown files
    }

    JsonSchema schema = loadSchema(schemaFile);
    JsonNode jsonNode = objectMapper.readTree(jsonContent);

    Set<ValidationMessage> errors = schema.validate(jsonNode);

    if (!errors.isEmpty()) {
      log.error("Validation errors for {}: {}", filename, errors);
      return false;
    }

    return true;
  }

  /**
   * Validates JSON content against the appropriate schema and throws exception on failure.
   *
   * @param filename The name of the file being validated
   * @param jsonContent The JSON content as a byte array
   * @throws IOException if validation fails or schema cannot be loaded
   */
  public void validateOrThrow(String filename, byte[] jsonContent) throws IOException {
    String schemaFile = FILE_TO_SCHEMA_MAPPING.get(filename);
    if (schemaFile == null) {
      log.debug("No schema mapping found for file: {}", filename);
      return; // Allow unknown files
    }

    JsonSchema schema = loadSchema(schemaFile);
    JsonNode jsonNode = objectMapper.readTree(jsonContent);

    Set<ValidationMessage> errors = schema.validate(jsonNode);

    if (!errors.isEmpty()) {
      StringBuilder errorMessage = new StringBuilder();
      errorMessage.append("Schema validation failed for ").append(filename).append(":\n");
      for (ValidationMessage error : errors) {
        errorMessage.append("  - ").append(error.getMessage()).append("\n");
      }
      throw new IOException(errorMessage.toString());
    }
  }

  /**
   * Validates an object against the appropriate schema after serializing to JSON.
   *
   * @param filename The name of the file being validated
   * @param object The object to validate
   * @throws IOException if validation fails
   */
  public void validateObject(String filename, Object object) throws IOException {
    byte[] jsonBytes = objectMapper.writeValueAsBytes(object);
    validateOrThrow(filename, jsonBytes);
  }

  private JsonSchema loadSchema(String schemaFile) throws IOException {
    if (schemaCache.containsKey(schemaFile)) {
      return schemaCache.get(schemaFile);
    }

    // Load from classpath resources
    String resourcePath = SCHEMAS_BASE_PATH + schemaFile;
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
