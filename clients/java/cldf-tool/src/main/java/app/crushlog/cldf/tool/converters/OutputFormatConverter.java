package app.crushlog.cldf.tool.converters;

import app.crushlog.cldf.tool.utils.OutputFormat;
import picocli.CommandLine.ITypeConverter;

/** Case-insensitive converter for OutputFormat enum. */
public class OutputFormatConverter implements ITypeConverter<OutputFormat> {

  @Override
  public OutputFormat convert(String value) throws Exception {
    if (value == null || value.isEmpty()) {
      throw new IllegalArgumentException("Output format cannot be null or empty");
    }

    // Convert to uppercase for matching since our enum values are uppercase
    String upperValue = value.toUpperCase();

    try {
      return OutputFormat.valueOf(upperValue);
    } catch (IllegalArgumentException e) {
      // Provide helpful error message with valid options
      throw new IllegalArgumentException(
          String.format(
              "Invalid output format: '%s'. Valid options are: text, json, yaml (case-insensitive)",
              value));
    }
  }
}
