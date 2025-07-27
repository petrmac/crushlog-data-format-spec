package io.cldf.tool.commands;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.format.DateTimeFormatter;

import io.cldf.api.CLDF;
import io.cldf.api.CLDFArchive;
import io.cldf.models.*;
import io.cldf.tool.utils.ConsoleUtils;
import io.cldf.tool.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Slf4j
@Command(
    name = "convert",
    description = "Convert between CLDF and other formats",
    mixinStandardHelpOptions = true)
public class ConvertCommand implements Runnable {

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
  private OutputFormat format;

  @Option(
      names = "--include-headers",
      description = "Include headers in CSV output",
      defaultValue = "true")
  private boolean includeHeaders;

  @Option(names = "--date-format", description = "Date format pattern", defaultValue = "yyyy-MM-dd")
  private String dateFormat;

  enum OutputFormat {
    json,
    csv
  }

  @Override
  public void run() {
    try {
      if (!inputFile.exists()) {
        log.error("File not found: {}", inputFile.getAbsolutePath());
        System.exit(1);
      }

      ConsoleUtils.printHeader("CLDF Convert");
      ConsoleUtils.printInfo("Converting: " + inputFile.getName());
      ConsoleUtils.printInfo("Format: " + format);

      // Read the archive
      CLDFArchive archive = CLDF.read(inputFile);

      // Perform conversion
      switch (format) {
        case json:
          convertToJson(archive);
          break;
        case csv:
          convertToCsv(archive);
          break;
      }

      ConsoleUtils.printSuccess("Conversion complete: " + outputFile.getName());

    } catch (Exception e) {
      log.error("Conversion failed", e);
      System.exit(1);
    }
  }

  private void convertToJson(CLDFArchive archive) throws IOException {
    String json = JsonUtils.toJson(archive, true);
    try (PrintWriter writer = new PrintWriter(outputFile)) {
      writer.write(json);
    }
  }

  private void convertToCsv(CLDFArchive archive) throws IOException {
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
      }
    }
  }

  private String escapeCsv(String value) {
    if (value == null) return "";
    if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
      return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
  }
}
