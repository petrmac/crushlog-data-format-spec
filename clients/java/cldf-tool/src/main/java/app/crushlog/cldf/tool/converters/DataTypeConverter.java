package app.crushlog.cldf.tool.converters;

import app.crushlog.cldf.tool.models.DataType;
import picocli.CommandLine.ITypeConverter;

/** Case-insensitive converter for DataType enum. */
public class DataTypeConverter implements ITypeConverter<DataType> {

  @Override
  public DataType convert(String value) throws Exception {
    if (value == null || value.isEmpty()) {
      throw new IllegalArgumentException("Data type cannot be null or empty");
    }

    // Convert to uppercase for matching since our enum values are uppercase
    String upperValue = value.toUpperCase();

    try {
      return DataType.valueOf(upperValue);
    } catch (IllegalArgumentException e) {
      // Provide helpful error message with valid options
      throw new IllegalArgumentException(
          String.format(
              "Invalid data type: '%s'. Valid options are: locations, routes, sectors, climbs, sessions, tags, media, all (case-insensitive)",
              value));
    }
  }
}
