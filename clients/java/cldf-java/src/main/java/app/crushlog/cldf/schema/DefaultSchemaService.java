package app.crushlog.cldf.schema;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import app.crushlog.cldf.constants.CLDFConstants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

/** Default implementation of SchemaService using functional programming patterns. */
@Slf4j
public class DefaultSchemaService implements SchemaService {

  private static final String SCHEMA_PATH_PREFIX = "schemas/";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  // Schema file mappings
  private static final Map<String, String> SCHEMA_FILES =
      Map.of(
          "manifest",
          CLDFConstants.MANIFEST_SCHEMA_JSON,
          "location",
          CLDFConstants.LOCATIONS_SCHEMA_JSON,
          CLDFConstants.FIELD_ROUTE,
          CLDFConstants.ROUTES_SCHEMA_JSON,
          "climb",
          CLDFConstants.CLIMBS_SCHEMA_JSON,
          "session",
          CLDFConstants.SESSIONS_SCHEMA_JSON,
          "tag",
          CLDFConstants.TAGS_SCHEMA_JSON);

  @Override
  public Map<String, Object> getSchemaInfo(String componentName) throws IOException {
    String normalizedName = componentName.toLowerCase();

    return switch (normalizedName) {
      case "all" -> buildCompleteSchema();
      case "dateformats" -> Map.of("dateFormats", buildDateFormatsInfo());
      case CLDFConstants.FIELD_ENUMS -> Map.of(CLDFConstants.FIELD_ENUMS, buildEnumsFromSchemas());
      case "commonmistakes" -> Map.of("commonMistakes", buildCommonMistakes());
      case "exampledata" -> Map.of("exampleData", buildExampleData());
      default ->
          SCHEMA_FILES.containsKey(normalizedName)
              ? Map.of(normalizedName, loadSchemaFromClasspath(SCHEMA_FILES.get(normalizedName)))
              : throwUnknownComponent(normalizedName);
    };
  }

  private Map<String, Object> throwUnknownComponent(String component) {
    throw new IllegalArgumentException("Unknown component: " + component);
  }

  @Override
  public Map<String, Object> buildCompleteSchema() {
    Map<String, Object> completeSchema = new HashMap<>();
    completeSchema.put("dateFormats", buildDateFormatsInfo());
    completeSchema.put(CLDFConstants.FIELD_ENUMS, buildEnumsFromSchemas());
    completeSchema.put("commonMistakes", buildCommonMistakes());
    completeSchema.put("exampleData", buildExampleData());

    // Load all schema files using streams
    Map<String, Object> schemas =
        SCHEMA_FILES.entrySet().stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> {
                      try {
                        return loadSchemaFromClasspath(entry.getValue());
                      } catch (IOException e) {
                        log.error("Failed to load schema: {}", entry.getValue(), e);
                        return Collections.emptyMap();
                      }
                    }));

    completeSchema.putAll(schemas);
    return completeSchema;
  }

  @Override
  public Map<String, Object> buildEnumsFromSchemas() {
    try {
      Map<String, Object> enums = new HashMap<>();

      // Extract enums from manifest schema
      extractEnumFromSchema("manifest.schema.json", "/properties/platform/enum", "platform")
          .ifPresent(values -> enums.put("platform", values));

      // Extract enums from climbs schema
      extractEnumFromSchema(
              "climbs.schema.json", "/definitions/climb/properties/type/enum", "climbType")
          .ifPresent(values -> enums.put("climbType", values));
      extractEnumFromSchema(
              "climbs.schema.json", "/definitions/climb/properties/belayType/enum", "belayType")
          .ifPresent(values -> enums.put("belayType", values));

      // Extract enums from locations schema
      extractEnumFromSchema("locations.schema.json", "/definitions/rockType/enum", "rockType")
          .ifPresent(values -> enums.put("rockType", values));
      extractEnumFromSchema("locations.schema.json", "/definitions/terrainType/enum", "terrainType")
          .ifPresent(values -> enums.put("terrainType", values));

      // Add context-dependent enums
      enums.put("routeType", List.of("boulder", "route"));

      Map<String, Object> finishType = new HashMap<>();
      finishType.put("boulder", List.of("flash", "top", "repeat", "project", "attempt"));
      finishType.put(
          "route", List.of("flash", "top", "repeat", "project", "attempt", "onsight", "redpoint"));
      enums.put("finishType", finishType);

      enums.put("gradeSystem", List.of("vScale", "font", "french", "yds", "uiaa"));
      enums.put(
          "sessionType",
          List.of(
              "sportClimbing",
              "multiPitch",
              "tradClimbing",
              "bouldering",
              "indoorClimbing",
              "indoorBouldering",
              "boardSession"));

      return enums;
    } catch (Exception e) {
      log.warn("Failed to extract enums from schemas, using fallback", e);
      return buildBasicEnums();
    }
  }

  @Override
  public Map<String, Object> buildDateFormatsInfo() {
    return Map.of(
        "description", "CLDF supports flexible date parsing with multiple formats",
        "supportedFormats",
            List.of(
                "ISO-8601 with milliseconds and offset: yyyy-MM-dd'T'HH:mm:ss.SSSXXX (e.g., 2024-01-29T12:00:00.000+00:00)",
                "ISO-8601 with milliseconds and Z: yyyy-MM-dd'T'HH:mm:ss.SSS'Z' (e.g., 2024-01-29T12:00:00.000Z)",
                "ISO-8601 without milliseconds with offset: yyyy-MM-dd'T'HH:mm:ssXXX (e.g., 2024-01-29T12:00:00+00:00)",
                "ISO-8601 without milliseconds with Z: yyyy-MM-dd'T'HH:mm:ss'Z' (e.g., 2024-01-29T12:00:00Z)",
                "ISO-8601 standard format",
                "For LocalDate fields: yyyy-MM-dd, yyyy/MM/dd, MM/dd/yyyy, dd/MM/yyyy, and other common formats"),
        "examples", Map.of("offsetDateTime", "2024-01-29T12:00:00Z", "localDate", "2024-01-29"));
  }

  @Override
  public List<String> buildCommonMistakes() {
    return List.of(
        "Route IDs should be strings, and routeId in climbs should also be strings (both must match)",
        "Location IDs should be integers, but locationId in routes should be strings",
        "FinishType values for boulder climbs: flash, top, repeat, project, attempt (NOT onsight, redpoint)",
        "FinishType values for route climbs: flash, top, repeat, project, attempt, onsight, redpoint",
        "Grades object requires both 'system' and 'grade' fields",
        "All tags must have an 'id' field",
        "Manifest requires appVersion and platform fields",
        "Date formats are flexible but OffsetDateTime needs time zone info");
  }

  @Override
  public Map<String, Object> buildExampleData() {
    Map<String, Object> minimal = new HashMap<>();

    // Build manifest example
    minimal.put(
        "manifest",
        Map.of(
            "format", "CLDF",
            "version", "1.0.0",
            "creationDate", "2024-01-29T12:00:00Z",
            "appVersion", "1.0.0",
            "platform", "Desktop"));

    // Build locations example
    minimal.put(
        "locations",
        List.of(
            Map.of(
                "id",
                1,
                "name",
                "Test Location",
                "country",
                "USA",
                "isIndoor",
                false,
                "coordinates",
                Map.of("latitude", 40.0, "longitude", -105.0))));

    // Build routes example
    minimal.put(
        "routes",
        List.of(
            Map.of(
                "id", "1",
                "name", "Test Route",
                "locationId", "1",
                "routeType", "boulder",
                "grades", Map.of("vScale", "V4"))));

    // Build climbs example
    minimal.put(
        "climbs",
        List.of(
            Map.of(
                "id", 1,
                "date", "2024-01-29",
                "routeId", "1",
                "routeName", "Test Route",
                "type", "boulder",
                "grades", Map.of("system", "vScale", "grade", "V4"),
                "finishType", "top",
                "attempts", 1)));

    // Build tags example
    minimal.put("tags", List.of(Map.of("id", "1", "name", "test", "category", "style")));

    // Build checksums example
    minimal.put("checksums", Map.of("algorithm", "SHA-256"));

    return Map.of("minimal", minimal);
  }

  private Object loadSchemaFromClasspath(String schemaFile) throws IOException {
    try (InputStream is =
        getClass().getClassLoader().getResourceAsStream(SCHEMA_PATH_PREFIX + schemaFile)) {
      if (is == null) {
        throw new IOException("Schema file not found: " + SCHEMA_PATH_PREFIX + schemaFile);
      }
      return OBJECT_MAPPER.readValue(is, Object.class);
    }
  }

  private Optional<List<String>> extractEnumFromSchema(
      String schemaFile, String jsonPath, String enumName) {
    try (InputStream is =
        getClass().getClassLoader().getResourceAsStream(SCHEMA_PATH_PREFIX + schemaFile)) {
      if (is == null) {
        log.debug("Schema file not found: {}", schemaFile);
        return Optional.empty();
      }

      JsonNode schema = OBJECT_MAPPER.readTree(is);
      JsonNode enumNode = schema.at(jsonPath);

      if (enumNode.isArray()) {
        List<String> values =
            StreamSupport.stream(enumNode.spliterator(), false)
                .map(JsonNode::asText)
                .collect(Collectors.toList());
        return Optional.of(values);
      }
    } catch (IOException e) {
      log.debug("Failed to extract enum {} from {}", enumName, schemaFile, e);
    }
    return Optional.empty();
  }

  private Map<String, Object> buildBasicEnums() {
    return Map.of(
        "platform", List.of("iOS", "Android", "Web", "Desktop"),
        "routeType", List.of("boulder", "route"),
        "climbType", List.of("boulder", "route"));
  }
}
