package io.cldf.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Type of climb. */
public enum ClimbType {
  // Bouldering climb
  BOULDER("boulder"),
  // Roped route
  ROUTE("route");

  private final String value;

  ClimbType(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @JsonCreator
  public static ClimbType fromValue(String value) {
    if (value == null) {
      return null;
    }
    for (ClimbType type : values()) {
      if (type.value.equalsIgnoreCase(value)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown climb type: " + value);
  }
}
