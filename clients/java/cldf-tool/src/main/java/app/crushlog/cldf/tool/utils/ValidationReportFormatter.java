package app.crushlog.cldf.tool.utils;

import java.io.IOException;
import java.util.Map;

import app.crushlog.cldf.tool.models.ReportFormat;
import app.crushlog.cldf.tool.models.ValidationReport;

/**
 * Formatter for validation reports in various formats. Extracted to allow proper testing without
 * code duplication.
 */
public class ValidationReportFormatter {

  public String formatReport(ValidationReport report, ReportFormat format) throws IOException {
    switch (format) {
      case XML:
        return formatXmlReport(report);
      case JSON:
        return formatJsonReport(report);
      case TEXT:
      default:
        return formatTextReport(report);
    }
  }

  public String formatTextReport(ValidationReport report) {
    StringBuilder sb = new StringBuilder();

    sb.append("\nValidation Report\n");
    sb.append("=================\n\n");

    sb.append("File: ").append(report.getFile()).append("\n");
    sb.append("Timestamp: ").append(report.getTimestamp()).append("\n");
    sb.append("Result: ").append(report.isValid() ? "VALID" : "INVALID").append("\n\n");

    // Statistics
    sb.append("Statistics:\n");
    sb.append("-----------\n");
    sb.append("  Locations: ").append(report.getStatistics().locations()).append("\n");
    sb.append("  Sessions: ").append(report.getStatistics().sessions()).append("\n");
    sb.append("  Climbs: ").append(report.getStatistics().climbs()).append("\n");
    if (report.getStatistics().routes() > 0) {
      sb.append("  Routes: ").append(report.getStatistics().routes()).append("\n");
    }
    if (report.getStatistics().sectors() > 0) {
      sb.append("  Sectors: ").append(report.getStatistics().sectors()).append("\n");
    }
    if (report.getStatistics().tags() > 0) {
      sb.append("  Tags: ").append(report.getStatistics().tags()).append("\n");
    }
    if (report.getStatistics().mediaItems() > 0) {
      sb.append("  Media Items: ").append(report.getStatistics().mediaItems()).append("\n");
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
      sb.append("  Algorithm: ").append(report.getChecksumResult().algorithm()).append("\n");
      sb.append("  Valid: ").append(report.getChecksumResult().valid() ? "YES" : "NO").append("\n");
      if (!report.getChecksumResult().results().isEmpty()) {
        for (Map.Entry<String, Boolean> entry : report.getChecksumResult().results().entrySet()) {
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

  public String formatJsonReport(ValidationReport report) throws IOException {
    // Create compact JSON (no spaces after colons)
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    sb.append("\"file\":\"").append(report.getFile()).append("\",");
    sb.append("\"timestamp\":\"").append(report.getTimestamp()).append("\",");
    sb.append("\"valid\":").append(report.isValid()).append(",");
    sb.append("\"structureValid\":").append(report.isStructureValid()).append(",");

    // Add statistics
    sb.append("\"statistics\":{");
    sb.append("\"locations\":").append(report.getStatistics().locations()).append(",");
    sb.append("\"sessions\":").append(report.getStatistics().sessions()).append(",");
    sb.append("\"climbs\":").append(report.getStatistics().climbs()).append(",");
    sb.append("\"routes\":").append(report.getStatistics().routes()).append(",");
    sb.append("\"sectors\":").append(report.getStatistics().sectors()).append(",");
    sb.append("\"tags\":").append(report.getStatistics().tags()).append(",");
    sb.append("\"mediaItems\":").append(report.getStatistics().mediaItems());
    sb.append("},");

    sb.append("\"errors\":").append(report.getErrors().size()).append(",");
    sb.append("\"warnings\":").append(report.getWarnings().size());
    sb.append("}");
    return sb.toString();
  }

  public String formatXmlReport(ValidationReport report) {
    // Simple XML formatting
    StringBuilder sb = new StringBuilder();
    sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    sb.append("<validationReport>\n");
    sb.append("  <file>").append(report.getFile()).append("</file>\n");
    sb.append("  <timestamp>").append(report.getTimestamp()).append("</timestamp>\n");
    sb.append("  <valid>").append(report.isValid()).append("</valid>\n");

    sb.append("  <statistics>\n");
    sb.append("    <locations>")
        .append(report.getStatistics().locations())
        .append("</locations>\n");
    sb.append("    <sessions>").append(report.getStatistics().sessions()).append("</sessions>\n");
    sb.append("    <climbs>").append(report.getStatistics().climbs()).append("</climbs>\n");
    sb.append("    <routes>").append(report.getStatistics().routes()).append("</routes>\n");
    sb.append("    <sectors>").append(report.getStatistics().sectors()).append("</sectors>\n");
    sb.append("    <tags>").append(report.getStatistics().tags()).append("</tags>\n");
    sb.append("    <mediaItems>")
        .append(report.getStatistics().mediaItems())
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
