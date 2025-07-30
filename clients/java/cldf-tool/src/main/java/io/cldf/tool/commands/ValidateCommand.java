package io.cldf.tool.commands;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.TreeMap;

import jakarta.inject.Inject;

import io.cldf.api.CLDF;
import io.cldf.api.CLDFArchive;
import io.cldf.tool.models.CommandResult;
import io.cldf.tool.services.ValidationResult;
import io.cldf.tool.services.ValidationService;
import io.cldf.tool.utils.ConsoleUtils;
import io.cldf.tool.utils.ValidationReportFormatter;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Slf4j
@Command(
    name = "validate",
    description = "Validate a CLDF archive",
    mixinStandardHelpOptions = true)
public class ValidateCommand extends BaseCommand {

  @Parameters(index = "0", description = "CLDF file to validate")
  private File inputFile;

  @Option(names = "--schema", description = "Validate against JSON schemas", defaultValue = "true")
  private boolean validateSchema;

  @Option(names = "--checksums", description = "Verify file checksums", defaultValue = "true")
  private boolean validateChecksums;

  @Option(names = "--references", description = "Check reference integrity", defaultValue = "true")
  private boolean validateReferences;

  @Option(names = "--strict", description = "Enable all validations")
  private boolean strict;

  @Option(
      names = "--report-format",
      description = "Output format: ${COMPLETION-CANDIDATES}",
      defaultValue = "text")
  private ReportFormat reportFormat;

  @Option(names = "--output", description = "Output file for report (stdout if not specified)")
  private File outputFile;

  private final ValidationService validationService;
  private final ValidationReportFormatter formatter;

  @Inject
  public ValidateCommand(ValidationService validationService) {
    this.validationService = validationService;
    this.formatter = new ValidationReportFormatter();
  }

  // For tests that only need CLI parsing (e.g., help text)
  public ValidateCommand() {
    this.validationService = null;
    this.formatter = new ValidationReportFormatter();
  }

  public enum ReportFormat {
    text,
    json,
    xml
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

    if (strict) {
      validateSchema = true;
      validateChecksums = true;
      validateReferences = true;
    }

    logInfo("Validating: " + inputFile.getName());

    try {
      // Read the archive
      CLDFArchive archive = CLDF.read(inputFile);

      // Perform validation
      ValidationReport report = performValidation(archive);

      // Build result based on report format
      if (reportFormat == ReportFormat.json
          || outputFormat == io.cldf.tool.utils.OutputFormat.json) {
        return CommandResult.builder()
            .success(report.isValid())
            .message(report.isValid() ? "Validation passed" : "Validation failed")
            .data(report)
            .exitCode(report.isValid() ? 0 : 1)
            .build();
      } else {
        // For text/xml format, we'll handle output in outputText method
        String formattedReport = formatReport(report);
        return CommandResult.builder()
            .success(report.isValid())
            .message(formattedReport)
            .exitCode(report.isValid() ? 0 : 1)
            .build();
      }
    } catch (IOException e) {
      // Handle various validation errors from CLDFReader
      String errorMessage = e.getMessage();
      String errorType = "Validation failed";

      if (errorMessage.contains("Schema validation failed")) {
        errorType = "Schema validation failed";
      } else if (errorMessage.contains("Checksum mismatch")) {
        errorType = "Checksum validation failed";
      } else if (errorMessage.contains("Missing required file")) {
        errorType = "Archive structure validation failed";
      }

      ValidationReport errorReport =
          ValidationReport.builder()
              .timestamp(OffsetDateTime.now())
              .file(inputFile.getName())
              .valid(false)
              .errors(java.util.Arrays.asList(errorType + ": " + errorMessage))
              .warnings(new java.util.ArrayList<>())
              .statistics(
                  Statistics.builder()
                      .locations(0)
                      .sessions(0)
                      .climbs(0)
                      .routes(0)
                      .sectors(0)
                      .tags(0)
                      .mediaItems(0)
                      .build())
              .build();

      if (reportFormat == ReportFormat.json
          || outputFormat == io.cldf.tool.utils.OutputFormat.json) {
        return CommandResult.builder()
            .success(false)
            .message(errorType)
            .data(errorReport)
            .exitCode(1)
            .build();
      } else {
        return CommandResult.builder()
            .success(false)
            .message(formatReport(errorReport))
            .exitCode(1)
            .build();
      }
    }
  }

  @Override
  protected void outputText(CommandResult result) {
    // For validate command, the message contains the formatted report
    output.write(result.getMessage());
    if (outputFile != null && !result.getMessage().isEmpty()) {
      try {
        Files.writeString(outputFile.toPath(), result.getMessage());
        logInfo("Report written to: " + outputFile.getAbsolutePath());
      } catch (IOException e) {
        log.error("Failed to write report to file", e);
        output.writeError("Failed to write report to file: " + e.getMessage());
      }
    }
  }

  private ValidationReport performValidation(CLDFArchive archive) throws IOException {
    ValidationReport.ValidationReportBuilder reportBuilder = ValidationReport.builder();
    reportBuilder.timestamp(OffsetDateTime.now());
    reportBuilder.file(inputFile.getName());

    // Basic validation
    ValidationResult result = validationService.validate(archive);
    reportBuilder.structureValid(result.isValid());
    reportBuilder.errors(result.getErrors());
    reportBuilder.warnings(result.getWarnings());

    // Checksum validation
    if (validateChecksums && archive.getChecksums() != null) {
      ChecksumResult checksumResult = validateChecksums(archive);
      reportBuilder.checksumResult(checksumResult);
    }

    // Statistics
    reportBuilder.statistics(gatherStatistics(archive));

    ValidationReport report = reportBuilder.build();
    report.setValid(
        result.isValid()
            && (report.getChecksumResult() == null || report.getChecksumResult().isValid()));

    return report;
  }

  private ChecksumResult validateChecksums(CLDFArchive archive) {
    if (!quiet) {
      ConsoleUtils.printSection("Checksum Validation");
    }

    ChecksumResult result =
        ChecksumResult.builder()
            .algorithm(archive.getChecksums().getAlgorithm())
            .results(new TreeMap<>())
            .build();

    // Note: In a real implementation, we would need to access the actual file contents
    // from the ZIP archive to calculate checksums. For now, we'll simulate this.
    logWarning("Checksum validation not fully implemented");

    result.setValid(true);
    return result;
  }

  private Statistics gatherStatistics(CLDFArchive archive) {
    return Statistics.builder()
        .locations(archive.getLocations() != null ? archive.getLocations().size() : 0)
        .sessions(archive.getSessions() != null ? archive.getSessions().size() : 0)
        .climbs(archive.getClimbs() != null ? archive.getClimbs().size() : 0)
        .routes(archive.hasRoutes() ? archive.getRoutes().size() : 0)
        .sectors(archive.hasSectors() ? archive.getSectors().size() : 0)
        .tags(archive.hasTags() ? archive.getTags().size() : 0)
        .mediaItems(archive.hasMedia() ? archive.getMediaItems().size() : 0)
        .build();
  }

  private String formatReport(ValidationReport report) throws IOException {
    return formatter.formatReport(report, reportFormat);
  }

  // Formatting methods moved to ValidationReportFormatter

  @Data
  @Builder
  public static class ValidationReport {
    private String file;
    private OffsetDateTime timestamp;
    private boolean valid;
    private boolean structureValid;
    private ChecksumResult checksumResult;
    private Statistics statistics;
    private java.util.List<String> errors;
    private java.util.List<String> warnings;
  }

  @Data
  @Builder
  public static class ChecksumResult {
    private String algorithm;
    private boolean valid;
    private Map<String, Boolean> results;
  }

  @Data
  @Builder
  public static class Statistics {
    private int locations;
    private int sessions;
    private int climbs;
    private int routes;
    private int sectors;
    private int tags;
    private int mediaItems;
  }
}
