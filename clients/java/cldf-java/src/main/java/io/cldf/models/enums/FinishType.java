package io.cldf.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** How the climb was completed. */
public enum FinishType {
  // Common finish types for both boulder and route
  FLASH("flash"),
  TOP("top"),
  REPEAT("repeat"),
  PROJECT("project"),
  ATTEMPT("attempt"), // Attempting the climb but falling or giving up

  // Route-specific finish types (onsight and redpoint are route-only)
  ONSIGHT("onsight"),
  REDPOINT("redpoint");

  // Note: flash, repeat, project, and attempt appear in both boulder and route schemas
  // but we define them once here. The validation should be done at a higher level
  // based on the climb type.

  private final String value;

  FinishType(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @JsonCreator
  public static FinishType fromValue(String value) {
    if (value == null) {
      return null;
    }
    for (FinishType type : values()) {
      if (type.value.equalsIgnoreCase(value)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown finish type: " + value);
  }
}
