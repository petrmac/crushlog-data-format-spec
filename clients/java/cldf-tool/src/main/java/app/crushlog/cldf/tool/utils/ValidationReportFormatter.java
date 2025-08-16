package app.crushlog.cldf.tool.utils;

import java.io.IOException;
import java.util.Map;

import app.crushlog.cldf.tool.commands.ValidateCommand;

/**
 * Formatter for validation reports in various formats. Extracted to allow proper testing without
 * code duplication.
 */
public class ValidationReportFormatter {

  public String formatReport(
      ValidateCommand.ValidationReport report, ValidateCommand.ReportFormat format)
      throws IOException {
    switch (format) {
      case xml:
        return formatXmlReport(report);
      case json:
        return formatJsonReport(report);
      case text:
      default:
        return formatTextReport(report);
    }
  }

  public String formatTextReport(ValidateCommand.ValidationReport report) {
    StringBuilder sb = new StringBuilder();

    sb.append("\nValidation Report\n");
    sb.append("=================\n\n");

    sb.append("File: ").append(report.getFile()).append("\n");
    sb.append("Timestamp: ").append(report.getTimestamp()).append("\n");
    sb.append("Result: ").append(report.isValid() ? "VALID" : "INVALID").append("\n\n");

    // Statistics
    sb.append("Statistics:\n");
    sb.append("-----------\n");
    sb.append("  Locations: ").append(report.getStatistics().getLocations()).append("\n");
    sb.append("  Sessions: ").append(report.getStatistics().getSessions()).append("\n");
    sb.append("  Climbs: ").append(report.getStatistics().getClimbs()).append("\n");
    if (report.getStatistics().getRoutes() > 0) {
      sb.append("  Routes: ").append(report.getStatistics().getRoutes()).append("\n");
    }
    if (report.getStatistics().getSectors() > 0) {
      sb.append("  Sectors: ").append(report.getStatistics().getSectors()).append("\n");
    }
    if (report.getStatistics().getTags() > 0) {
      sb.append("  Tags: ").append(report.getStatistics().getTags()).append("\n");
    }
    if (report.getStatistics().getMediaItems() > 0) {
      sb.append("  Media Items: ").append(report.getStatistics().getMediaItems()).append("\n");
    }
    sb.append("\n");

    // Errors
    if (!report.getErrors().isEmpty()) {
      sb.append("Errors (").append(report.getErrors().size()).append("):\n");
      sb.append("------------\n");
      for (String error : report.getErrors()) {
        sb.append("  ✗ ").append(error).append("\n");
      }
      sb.append("\n");
    }

    // Warnings
    if (!report.getWarnings().isEmpty()) {
      sb.append("Warnings (").append(report.getWarnings().size()).append("):\n");
      sb.append("--------------\n");
      for (String warning : report.getWarnings()) {
        sb.append("  ⚠ ").append(warning).append("\n");
      }
      sb.append("\n");
    }

    // Checksum results
    if (report.getChecksumResult() != null) {
      sb.append("Checksums:\n");
      sb.append("----------\n");
      sb.append("  Algorithm: ").append(report.getChecksumResult().getAlgorithm()).append("\n");
      sb.append("  Valid: ")
          .append(report.getChecksumResult().isValid() ? "YES" : "NO")
          .append("\n");
      if (!report.getChecksumResult().getResults().isEmpty()) {
        for (Map.Entry<String, Boolean> entry :
            report.getChecksumResult().getResults().entrySet()) {
          sb.append("  ")
              .append(entry.getValue() ? "✓" : "✗")
              .append(" ")
              .append(entry.getKey())
              .append("\n");
        }
      }
      sb.append("\n");
    }

    // Summary
    sb.append("Summary:\n");
    sb.append("--------\n");
    if (report.isValid() && report.getWarnings().isEmpty()) {
      sb.append("✓ Validation passed\n");
    } else if (report.isValid()) {
      sb.append("✓ Validation passed with ")
          .append(report.getWarnings().size())
          .append(" warning(s)\n");
    } else {
      sb.append("✗ Validation failed with ")
          .append(report.getErrors().size())
          .append(" error(s) and ")
          .append(report.getWarnings().size())
          .append(" warning(s)\n");
    }

    return sb.toString();
  }

  public String formatJsonReport(ValidateCommand.ValidationReport report) throws IOException {
    // Simple JSON formatting
    StringBuilder sb = new StringBuilder();
    sb.append("{\n");
    sb.append("  \"file\": \"").append(report.getFile()).append("\",\n");
    sb.append("  \"timestamp\": \"").append(report.getTimestamp()).append("\",\n");
    sb.append("  \"valid\": ").append(report.isValid()).append(",\n");
    sb.append("  \"errors\": ").append(report.getErrors().size()).append(",\n");
    sb.append("  \"warnings\": ").append(report.getWarnings().size()).append("\n");
    sb.append("}");
    return sb.toString();
  }

  public String formatXmlReport(ValidateCommand.ValidationReport report) {
    // Simple XML formatting
    StringBuilder sb = new StringBuilder();
    sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    sb.append("<validationReport>\n");
    sb.append("  <file>").append(report.getFile()).append("</file>\n");
    sb.append("  <timestamp>").append(report.getTimestamp()).append("</timestamp>\n");
    sb.append("  <valid>").append(report.isValid()).append("</valid>\n");

    sb.append("  <statistics>\n");
    sb.append("    <locations>")
        .append(report.getStatistics().getLocations())
        .append("</locations>\n");
    sb.append("    <sessions>")
        .append(report.getStatistics().getSessions())
        .append("</sessions>\n");
    sb.append("    <climbs>").append(report.getStatistics().getClimbs()).append("</climbs>\n");
    sb.append("    <routes>").append(report.getStatistics().getRoutes()).append("</routes>\n");
    sb.append("    <sectors>").append(report.getStatistics().getSectors()).append("</sectors>\n");
    sb.append("    <tags>").append(report.getStatistics().getTags()).append("</tags>\n");
    sb.append("    <mediaItems>")
        .append(report.getStatistics().getMediaItems())
        .append("</mediaItems>\n");
    sb.append("  </statistics>\n");

    if (!report.getErrors().isEmpty()) {
      sb.append("  <errors count=\"").append(report.getErrors().size()).append("\">\n");
      for (String error : report.getErrors()) {
        sb.append("    <error>").append(escapeXml(error)).append("</error>\n");
      }
      sb.append("  </errors>\n");
    }

    if (!report.getWarnings().isEmpty()) {
      sb.append("  <warnings count=\"").append(report.getWarnings().size()).append("\">\n");
      for (String warning : report.getWarnings()) {
        sb.append("    <warning>").append(escapeXml(warning)).append("</warning>\n");
      }
      sb.append("  </warnings>\n");
    }

    sb.append("</validationReport>");
    return sb.toString();
  }

  public String escapeXml(String text) {
    return text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;");
  }
}
