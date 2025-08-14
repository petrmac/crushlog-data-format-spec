package app.crushlog.cldf.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Supported grading systems. */
public enum GradeSystem {
  // V-Scale for bouldering
  V_SCALE("vScale"),
  // Fontainebleau grading
  FONT("font"),
  // French sport climbing grades
  FRENCH("french"),
  // Yosemite Decimal System
  YDS("yds"),
  // UIAA grading system
  UIAA("uiaa");

  private final String value;

  GradeSystem(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @JsonCreator
  public static GradeSystem fromValue(String value) {
    if (value == null) {
      return null;
    }
    for (GradeSystem system : values()) {
      if (system.value.equalsIgnoreCase(value)) {
        return system;
      }
    }
    throw new IllegalArgumentException("Unknown grade system: " + value);
  }
}
