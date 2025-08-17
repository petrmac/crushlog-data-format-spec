package app.crushlog.cldf.tool.converters;

import app.crushlog.cldf.tool.models.ReportFormat;
import picocli.CommandLine.ITypeConverter;

/** Case-insensitive converter for ReportFormat enum. */
public class ReportFormatConverter implements ITypeConverter<ReportFormat> {

  @Override
  public ReportFormat convert(String value) throws Exception {
    if (value == null || value.isEmpty()) {
      throw new IllegalArgumentException("Report format cannot be null or empty");
    }

    // Convert to uppercase for matching since our enum values are uppercase
    String upperValue = value.toUpperCase();

    try {
      return ReportFormat.valueOf(upperValue);
    } catch (IllegalArgumentException e) {
      // Provide helpful error message with valid options
      throw new IllegalArgumentException(
          String.format(
              "Invalid report format: '%s'. Valid options are: text, json, xml (case-insensitive)",
              value));
    }
  }
}
