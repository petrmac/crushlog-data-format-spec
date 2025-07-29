package io.cldf.tool.commands;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cldf.tool.models.CommandResult;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Slf4j
@Command(
    name = "schema",
    description = "Show schema information for CLDF components",
    mixinStandardHelpOptions = true)
public class SchemaCommand extends BaseCommand {

  @Option(
      names = {"-c", "--component"},
      description = "Component to show schema for: ${COMPLETION-CANDIDATES}",
      completionCandidates = ComponentType.class,
      defaultValue = "all")
  private String component;

  @Option(
      names = {"--format"},
      description = "Output format for schema details",
      defaultValue = "detailed")
  private String format;

  static class ComponentType extends ArrayList<String> {
    ComponentType() {
      super(
          Arrays.asList(
              "all",
              "manifest",
              "location",
              "route",
              "climb",
              "session",
              "tag",
              "dateFormats",
              "enums",
              "commonMistakes",
              "exampleData"));
    }
  }

  @Override
  protected CommandResult execute() throws Exception {
    Map<String, Object> schemaInfo = new HashMap<>();

    switch (component.toLowerCase()) {
      case "all":
        schemaInfo = buildCompleteSchema();
        break;
      case "manifest":
        schemaInfo.put("manifest", loadSchemaFromClasspath("manifest.schema.json"));
        break;
      case "location":
        schemaInfo.put("location", loadSchemaFromClasspath("locations.schema.json"));
        break;
      case "route":
        schemaInfo.put("route", loadSchemaFromClasspath("routes.schema.json"));
        break;
      case "climb":
        schemaInfo.put("climb", loadSchemaFromClasspath("climbs.schema.json"));
        break;
      case "session":
        schemaInfo.put("session", loadSchemaFromClasspath("sessions.schema.json"));
        break;
      case "tag":
        schemaInfo.put("tag", loadSchemaFromClasspath("tags.schema.json"));
        break;
      case "dateformats":
        schemaInfo.put("dateFormats", buildDateFormatsInfo());
        break;
      case "enums":
        schemaInfo.put("enums", buildEnumsFromSchemas());
        break;
      case "commonmistakes":
        schemaInfo.put("commonMistakes", buildCommonMistakes());
        break;
      case "exampledata":
        schemaInfo.put("exampleData", buildExampleData());
        break;
      default:
        throw new IllegalArgumentException("Unknown component: " + component);
    }

    return CommandResult.builder()
        .success(true)
        .message("Schema information retrieved successfully")
        .data(schemaInfo)
        .build();
  }

  @Override
  protected void outputText(CommandResult result) {
    if (result.isSuccess() && result.getData() != null) {
      Map<String, Object> data = (Map<String, Object>) result.getData();
      data.forEach(
          (key, value) -> {
            output.write(key.toUpperCase() + " SCHEMA:");
            if (value instanceof Map) {
              printMap((Map<String, Object>) value, 1);
            } else if (value instanceof List) {
              printList((List<?>) value, 1);
            } else {
              output.write("  " + value.toString());
            }
            output.write("");
          });
    } else {
      output.writeError("Failed to retrieve schema information");
    }
  }

  private void printMap(Map<String, Object> map, int indent) {
    String prefix = "  ".repeat(indent);
    map.forEach(
        (key, value) -> {
          if (value instanceof Map) {
            output.write(prefix + key + ":");
            printMap((Map<String, Object>) value, indent + 1);
          } else if (value instanceof List) {
            output.write(prefix + key + ":");
            printList((List<?>) value, indent + 1);
          } else {
            output.write(prefix + key + ": " + value);
          }
        });
  }

  private void printList(List<?> list, int indent) {
    String prefix = "  ".repeat(indent);
    list.forEach(
        item -> {
          if (item instanceof Map) {
            printMap((Map<String, Object>) item, indent);
          } else {
            output.write(prefix + "- " + item);
          }
        });
  }

  private Object loadSchemaFromClasspath(String schemaFile) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    try (InputStream is =
        getClass().getClassLoader().getResourceAsStream("schemas/" + schemaFile)) {
      if (is == null) {
        throw new IOException("Schema file not found: schemas/" + schemaFile);
      }
      return mapper.readValue(is, Object.class);
    }
  }

  private Map<String, Object> buildCompleteSchema() throws IOException {
    Map<String, Object> schema = new HashMap<>();
    schema.put("dateFormats", buildDateFormatsInfo());
    schema.put("enums", buildEnumsFromSchemas());
    schema.put("manifest", loadSchemaFromClasspath("manifest.schema.json"));
    schema.put("location", loadSchemaFromClasspath("locations.schema.json"));
    schema.put("route", loadSchemaFromClasspath("routes.schema.json"));
    schema.put("climb", loadSchemaFromClasspath("climbs.schema.json"));
    schema.put("session", loadSchemaFromClasspath("sessions.schema.json"));
    schema.put("tag", loadSchemaFromClasspath("tags.schema.json"));
    schema.put("commonMistakes", buildCommonMistakes());
    schema.put("exampleData", buildExampleData());
    return schema;
  }

  private Map<String, Object> buildEnumsFromSchemas() {
    Map<String, Object> enums = new HashMap<>();

    try {
      ObjectMapper mapper = new ObjectMapper();

      // Extract enums from manifest schema
      try (InputStream is =
          getClass().getClassLoader().getResourceAsStream("schemas/manifest.schema.json")) {
        if (is != null) {
          JsonNode manifestSchema = mapper.readTree(is);
          JsonNode platformProp = manifestSchema.at("/properties/platform/enum");
          if (platformProp.isArray()) {
            List<String> platforms = new ArrayList<>();
            platformProp.forEach(node -> platforms.add(node.asText()));
            enums.put("platform", platforms);
          }
        }
      }

      // Extract enums from climbs schema
      try (InputStream is =
          getClass().getClassLoader().getResourceAsStream("schemas/climbs.schema.json")) {
        if (is != null) {
          JsonNode climbsSchema = mapper.readTree(is);

          // Extract climb type enum
          JsonNode typeProp = climbsSchema.at("/properties/type/enum");
          if (typeProp.isArray()) {
            List<String> types = new ArrayList<>();
            typeProp.forEach(node -> types.add(node.asText()));
            enums.put("climbType", types);
          }

          // Extract belay type enum
          JsonNode belayTypeProp = climbsSchema.at("/properties/belayType/enum");
          if (belayTypeProp.isArray()) {
            List<String> belayTypes = new ArrayList<>();
            belayTypeProp.forEach(node -> belayTypes.add(node.asText()));
            enums.put("belayType", belayTypes);
          }
        }
      }

      // Extract enums from locations schema
      try (InputStream is =
          getClass().getClassLoader().getResourceAsStream("schemas/locations.schema.json")) {
        if (is != null) {
          JsonNode locationsSchema = mapper.readTree(is);

          JsonNode rockTypeProp = locationsSchema.at("/properties/rockType/enum");
          if (rockTypeProp.isArray()) {
            List<String> rockTypes = new ArrayList<>();
            rockTypeProp.forEach(node -> rockTypes.add(node.asText()));
            enums.put("rockType", rockTypes);
          }

          JsonNode terrainTypeProp = locationsSchema.at("/properties/terrainType/enum");
          if (terrainTypeProp.isArray()) {
            List<String> terrainTypes = new ArrayList<>();
            terrainTypeProp.forEach(node -> terrainTypes.add(node.asText()));
            enums.put("terrainType", terrainTypes);
          }
        }
      }

      // Add hardcoded enums that are context-dependent
      enums.put("routeType", Arrays.asList("boulder", "route"));

      Map<String, Object> finishType = new HashMap<>();
      finishType.put("boulder", Arrays.asList("flash", "top", "repeat", "project", "attempt"));
      finishType.put(
          "route",
          Arrays.asList("flash", "top", "repeat", "project", "attempt", "onsight", "redpoint"));
      enums.put("finishType", finishType);

      enums.put("gradeSystem", Arrays.asList("vScale", "font", "french", "yds", "uiaa"));
      enums.put(
          "sessionType",
          Arrays.asList(
              "sportClimbing",
              "multiPitch",
              "tradClimbing",
              "bouldering",
              "indoorClimbing",
              "indoorBouldering",
              "boardSession"));

    } catch (IOException e) {
      log.warn("Failed to extract enums from schemas", e);
      // Return basic enums as fallback
      return buildBasicEnums();
    }

    return enums;
  }

  private Map<String, Object> buildBasicEnums() {
    Map<String, Object> enums = new HashMap<>();
    enums.put("platform", Arrays.asList("iOS", "Android", "Web", "Desktop"));
    enums.put("routeType", Arrays.asList("boulder", "route"));
    enums.put("climbType", Arrays.asList("boulder", "route"));
    return enums;
  }

  private Map<String, Object> buildDateFormatsInfo() {
    Map<String, Object> dateInfo = new HashMap<>();
    dateInfo.put("description", "CLDF supports flexible date parsing with multiple formats");
    dateInfo.put(
        "supportedFormats",
        Arrays.asList(
            "ISO-8601 with milliseconds and offset: yyyy-MM-dd'T'HH:mm:ss.SSSXXX (e.g., 2024-01-29T12:00:00.000+00:00)",
            "ISO-8601 with milliseconds and Z: yyyy-MM-dd'T'HH:mm:ss.SSS'Z' (e.g., 2024-01-29T12:00:00.000Z)",
            "ISO-8601 without milliseconds with offset: yyyy-MM-dd'T'HH:mm:ssXXX (e.g., 2024-01-29T12:00:00+00:00)",
            "ISO-8601 without milliseconds with Z: yyyy-MM-dd'T'HH:mm:ss'Z' (e.g., 2024-01-29T12:00:00Z)",
            "ISO-8601 standard format",
            "For LocalDate fields: yyyy-MM-dd, yyyy/MM/dd, MM/dd/yyyy, dd/MM/yyyy, and other common formats"));

    Map<String, String> examples = new HashMap<>();
    examples.put("offsetDateTime", "2024-01-29T12:00:00Z");
    examples.put("localDate", "2024-01-29");
    dateInfo.put("examples", examples);

    return dateInfo;
  }

  private List<String> buildCommonMistakes() {
    return Arrays.asList(
        "Route IDs should be strings, and routeId in climbs should also be strings (both must match)",
        "Location IDs should be integers, but locationId in routes should be strings",
        "FinishType values for boulder climbs: flash, top, repeat, project, attempt (NOT onsight, redpoint)",
        "FinishType values for route climbs: flash, top, repeat, project, attempt, onsight, redpoint",
        "Grades object requires both 'system' and 'grade' fields",
        "All tags must have an 'id' field",
        "Manifest requires appVersion and platform fields",
        "Date formats are flexible but OffsetDateTime needs time zone info");
  }

  private Map<String, Object> buildExampleData() {
    Map<String, Object> examples = new HashMap<>();

    Map<String, Object> minimal = new HashMap<>();

    Map<String, Object> manifest = new HashMap<>();
    manifest.put("format", "CLDF");
    manifest.put("version", "1.0.0");
    manifest.put("creationDate", "2024-01-29T12:00:00Z");
    manifest.put("appVersion", "1.0.0");
    manifest.put("platform", "Desktop");
    minimal.put("manifest", manifest);

    List<Map<String, Object>> locations = new ArrayList<>();
    Map<String, Object> location = new HashMap<>();
    location.put("id", 1);
    location.put("name", "Test Location");
    location.put("country", "USA");
    location.put("isIndoor", false);
    Map<String, Double> coordinates = new HashMap<>();
    coordinates.put("latitude", 40.0);
    coordinates.put("longitude", -105.0);
    location.put("coordinates", coordinates);
    locations.add(location);
    minimal.put("locations", locations);

    List<Map<String, Object>> routes = new ArrayList<>();
    Map<String, Object> route = new HashMap<>();
    route.put("id", "1");
    route.put("name", "Test Route");
    route.put("locationId", "1");
    route.put("routeType", "boulder");
    Map<String, String> grades = new HashMap<>();
    grades.put("vScale", "V4");
    route.put("grades", grades);
    routes.add(route);
    minimal.put("routes", routes);

    List<Map<String, Object>> climbs = new ArrayList<>();
    Map<String, Object> climb = new HashMap<>();
    climb.put("id", 1);
    climb.put("date", "2024-01-29");
    climb.put("routeId", "1");
    climb.put("routeName", "Test Route");
    climb.put("type", "boulder");
    Map<String, String> climbGrades = new HashMap<>();
    climbGrades.put("system", "vScale");
    climbGrades.put("grade", "V4");
    climb.put("grades", climbGrades);
    climb.put("finishType", "top");
    climb.put("attempts", 1);
    climbs.add(climb);
    minimal.put("climbs", climbs);

    List<Map<String, Object>> tags = new ArrayList<>();
    Map<String, Object> tag = new HashMap<>();
    tag.put("id", "1");
    tag.put("name", "test");
    tag.put("category", "style");
    tags.add(tag);
    minimal.put("tags", tags);

    Map<String, String> checksums = new HashMap<>();
    checksums.put("algorithm", "SHA-256");
    minimal.put("checksums", checksums);

    examples.put("minimal", minimal);
    return examples;
  }
}
