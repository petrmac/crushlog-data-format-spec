package app.crushlog.cldf.tool.commands;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.format.DateTimeFormatter;

import app.crushlog.cldf.api.CLDF;
import app.crushlog.cldf.api.CLDFArchive;
import app.crushlog.cldf.models.*;
import app.crushlog.cldf.tool.models.CommandResult;
import app.crushlog.cldf.tool.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Slf4j
@Command(
    name = "convert",
    description = "Convert between CLDF and other formats",
    mixinStandardHelpOptions = true)
public class ConvertCommand extends BaseCommand {

  @Parameters(index = "0", description = "Input CLDF file")
  private File inputFile;

  @Option(
      names = {"-o", "--output"},
      description = "Output file",
      required = true)
  private File outputFile;

  @Option(
      names = "--format",
      description = "Target format: ${COMPLETION-CANDIDATES}",
      required = true)
  private ConvertFormat format;

  @Option(
      names = "--include-headers",
      description = "Include headers in CSV output",
      defaultValue = "true")
  private boolean includeHeaders;

  @Option(names = "--date-format", description = "Date format pattern", defaultValue = "yyyy-MM-dd")
  private String dateFormat;

  enum ConvertFormat {
    json,
    csv
  }

  @Override
  protected CommandResult execute() throws Exception {
    if (!inputFile.exists()) {
      return CommandResult.builder()
          .success(false)
          .message("File not found: " + inputFile.getAbsolutePath())
          .exitCode(1)
          .build();
    }

    logInfo("Converting: " + inputFile.getName());
    logInfo("Format: " + format);

    // Read the archive
    CLDFArchive archive = CLDF.read(inputFile);

    // Perform conversion
    ConversionResult result;
    switch (format) {
      case json:
        result = convertToJson(archive);
        break;
      case csv:
        result = convertToCsv(archive);
        break;
      default:
        throw new IllegalArgumentException("Unsupported format: " + format);
    }

    // Build result data
    java.util.Map<String, Object> resultData = new java.util.HashMap<>();
    resultData.put("inputFile", inputFile.getName());
    resultData.put("outputFile", outputFile.getAbsolutePath());
    resultData.put("format", format.name());
    resultData.put("itemsConverted", result.itemCount);
    resultData.put("outputSize", outputFile.length());

    return CommandResult.builder()
        .success(true)
        .message("Successfully converted " + result.itemCount + " items to " + format)
        .data(resultData)
        .build();
  }

  @Override
  protected void outputText(CommandResult result) {
    if (!result.isSuccess()) {
      output.writeError(result.getMessage());
      return;
    }

    output.write(result.getMessage());

    var data = (java.util.Map<String, Object>) result.getData();
    if (!quiet && data != null) {
      output.write(
          """

          Conversion Details
          ==================
          Output: %s
          Format: %s
          Items:  %d
          Size:   %d bytes
          """
              .formatted(
                  data.get("outputFile"),
                  data.get("format"),
                  data.get("itemsConverted"),
                  data.get("outputSize")));
    }
  }

  private ConversionResult convertToJson(CLDFArchive archive) throws IOException {
    String json = JsonUtils.toJson(archive, true);
    try (PrintWriter writer = new PrintWriter(outputFile)) {
      writer.write(json);
    }

    int itemCount =
        (archive.getClimbs() != null ? archive.getClimbs().size() : 0)
            + (archive.getSessions() != null ? archive.getSessions().size() : 0)
            + (archive.getLocations() != null ? archive.getLocations().size() : 0);

    return new ConversionResult(itemCount);
  }

  private ConversionResult convertToCsv(CLDFArchive archive) throws IOException {
    int itemCount = 0;
    try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormat);

      // Write headers
      if (includeHeaders) {
        writer.println("Date,Location,Route Name,Type,Grade,Finish Type,Attempts,Rating,Notes");
      }

      // Write climbs
      for (Climb climb : archive.getClimbs()) {
        // Find location name
        String locationName = "Unknown";
        if (climb.getSessionId() != null) {
          Session session =
              archive.getSessions().stream()
                  .filter(s -> s.getId().equals(climb.getSessionId()))
                  .findFirst()
                  .orElse(null);
          if (session != null) {
            locationName = session.getLocation();
          }
        }

        // Format row
        writer.printf(
            "%s,%s,%s,%s,%s,%s,%d,%s,%s%n",
            climb.getDate().format(formatter),
            escapeCsv(locationName),
            escapeCsv(climb.getRouteName()),
            climb.getType(),
            climb.getGrades() != null ? climb.getGrades().getGrade() : "",
            climb.getFinishType(),
            climb.getAttempts(),
            climb.getRating() != null ? String.valueOf(climb.getRating()) : "",
            escapeCsv(climb.getNotes() != null ? climb.getNotes() : ""));
        itemCount++;
      }
    }

    return new ConversionResult(itemCount);
  }

  private String escapeCsv(String value) {
    if (value == null) return "";
    if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
      return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
  }

  private static class ConversionResult {
    final int itemCount;

    ConversionResult(int itemCount) {
      this.itemCount = itemCount;
    }
  }
}
