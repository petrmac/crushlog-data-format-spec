package app.crushlog.cldf.tool.commands;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import jakarta.inject.Inject;

import app.crushlog.cldf.tool.converters.ReportFormatConverter;
import app.crushlog.cldf.tool.models.CommandResult;
import app.crushlog.cldf.tool.models.ReportFormat;
import app.crushlog.cldf.tool.models.ValidationReport;
import app.crushlog.cldf.tool.services.ValidationReportService;
import app.crushlog.cldf.tool.services.ValidationReportService.ValidationOptions;
import app.crushlog.cldf.tool.utils.ValidationReportFormatter;
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
      description = "Output format: text, json, xml (case-insensitive)",
      defaultValue = "text",
      converter = ReportFormatConverter.class)
  private ReportFormat reportFormat;

  @Option(names = "--output", description = "Output file for report (stdout if not specified)")
  private File outputFile;

  private final ValidationReportService validationReportService;
  private final ValidationReportFormatter formatter;

  @Inject
  public ValidateCommand(ValidationReportService validationReportService) {
    this.validationReportService = validationReportService;
    this.formatter = new ValidationReportFormatter();
  }

  // For tests that only need CLI parsing (e.g., help text)
  public ValidateCommand() {
    this.validationReportService = null;
    this.formatter = new ValidationReportFormatter();
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

    logInfo("Validating: " + inputFile.getName());

    // If strict mode is enabled, enable all validations
    if (strict) {
      validateSchema = true;
      validateChecksums = true;
      validateReferences = true;
    }

    // Create validation options from command flags
    ValidationOptions options =
        ValidationOptions.fromFlags(validateSchema, validateChecksums, validateReferences, strict);

    try {
      // Perform validation using the service
      ValidationReport report = validationReportService.validateFile(inputFile, options);

      // Build result based on report format
      if (reportFormat == ReportFormat.JSON
          || outputFormat == app.crushlog.cldf.tool.utils.OutputFormat.JSON) {
        return CommandResult.builder()
            .success(report.isValid())
            .message(report.isValid() ? "Validation passed" : "Validation failed")
            .data(report)
            .exitCode(report.isValid() ? 0 : 1)
            .build();
      } else {
        // For text/xml format, we'll handle output in outputText method
        String formattedReport = formatter.formatReport(report, reportFormat);
        return CommandResult.builder()
            .success(report.isValid())
            .message(formattedReport)
            .exitCode(report.isValid() ? 0 : 1)
            .build();
      }
    } catch (IOException e) {
      log.error("Validation failed", e);
      return CommandResult.builder()
          .success(false)
          .message("Validation failed: " + e.getMessage())
          .exitCode(1)
          .build();
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

  // All validation logic has been moved to ValidationReportService
}
