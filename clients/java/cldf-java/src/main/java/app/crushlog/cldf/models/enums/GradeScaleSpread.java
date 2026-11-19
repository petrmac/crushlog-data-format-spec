package app.crushlog.cldf.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** How the levels of a grading scale were laid out when it was created. */
public enum GradeScaleSpread {
  // Every level covers the same number of grades
  EVEN("even"),
  // The upper levels cover more grades than the lower ones
  WIDER_TOP("widerTop");

  private final String value;

  GradeScaleSpread(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @JsonCreator
  public static GradeScaleSpread fromValue(String value) {
    if (value == null) {
      return null;
    }
    for (GradeScaleSpread spread : values()) {
      if (spread.value.equalsIgnoreCase(value)) {
        return spread;
      }
    }
    throw new IllegalArgumentException("Unknown grade scale spread: " + value);
  }
}
