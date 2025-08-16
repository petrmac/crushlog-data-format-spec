package app.crushlog.cldf.tool.commands;

import java.io.IOException;
import java.util.*;

import jakarta.inject.Inject;

import app.crushlog.cldf.schema.SchemaService;
import app.crushlog.cldf.tool.models.CommandResult;
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

  private final SchemaService schemaService;

  @Inject
  public SchemaCommand(SchemaService schemaService) {
    this.schemaService = schemaService;
  }

  // For PicoCLI framework - it needs a no-arg constructor
  public SchemaCommand() {
    this.schemaService = null;
  }

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
    try {
      if (schemaService == null) {
        throw new IllegalStateException("SchemaService not initialized");
      }
      Map<String, Object> schemaInfo = schemaService.getSchemaInfo(component);

      return CommandResult.builder()
          .success(true)
          .message("Schema information retrieved successfully")
          .data(schemaInfo)
          .build();
    } catch (IllegalArgumentException e) {
      return CommandResult.builder().success(false).message(e.getMessage()).exitCode(1).build();
    } catch (IOException e) {
      log.error("Failed to retrieve schema information", e);
      return CommandResult.builder()
          .success(false)
          .message("Failed to retrieve schema information: " + e.getMessage())
          .exitCode(1)
          .build();
    }
  }

  @Override
  protected void outputText(CommandResult result) {
    if (result.isSuccess() && result.getData() != null) {
      @SuppressWarnings("unchecked")
      Map<String, Object> data = (Map<String, Object>) result.getData();
      data.forEach(
          (key, value) -> {
            output.write(key.toUpperCase() + " SCHEMA:");
            if (value instanceof Map<?, ?>) {
              @SuppressWarnings("unchecked")
              Map<String, Object> mapValue = (Map<String, Object>) value;
              printMap(mapValue, 1);
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
            @SuppressWarnings("unchecked")
            Map<String, Object> mapValue = (Map<String, Object>) value;
            printMap(mapValue, indent + 1);
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
          if (item instanceof Map<?, ?>) {
            @SuppressWarnings("unchecked")
            Map<String, Object> mapItem = (Map<String, Object>) item;
            printMap(mapItem, indent);
          } else {
            output.write(prefix + "- " + item);
          }
        });
  }
}
