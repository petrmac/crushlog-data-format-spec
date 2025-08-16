package app.crushlog.cldf.tool.converters;

import picocli.CommandLine.ITypeConverter;

/**
 * A case-insensitive enum converter for PicoCLI. Allows users to input enum values in any case
 * (lowercase, uppercase, mixed).
 */
public class CaseInsensitiveEnumConverter<T extends Enum<T>> implements ITypeConverter<T> {
  private final Class<T> enumClass;

  public CaseInsensitiveEnumConverter(Class<T> enumClass) {
    this.enumClass = enumClass;
  }

  @Override
  public T convert(String value) throws Exception {
    if (value == null || value.isEmpty()) {
      throw new IllegalArgumentException("Value cannot be null or empty");
    }

    // Try exact match first for backwards compatibility
    try {
      return Enum.valueOf(enumClass, value);
    } catch (IllegalArgumentException e) {
      // Fall back to case-insensitive search
      for (T enumConstant : enumClass.getEnumConstants()) {
        if (enumConstant.name().equalsIgnoreCase(value)) {
          return enumConstant;
        }
      }

      // If no match found, throw exception with available options
      String validValues = String.join(", ", getValidValues());
      throw new IllegalArgumentException(
          String.format(
              "Invalid value '%s'. Expected one of: %s (case-insensitive)", value, validValues));
    }
  }

  private String[] getValidValues() {
    T[] constants = enumClass.getEnumConstants();
    String[] values = new String[constants.length];
    for (int i = 0; i < constants.length; i++) {
      values[i] = constants[i].name().toLowerCase();
    }
    return values;
  }
}
