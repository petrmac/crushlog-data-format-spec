package io.cldf.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Characteristics of the route. */
public enum RouteCharacteristics {
  TRAD("trad"),
  BOLTED("bolted");

  private final String value;

  RouteCharacteristics(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @JsonCreator
  public static RouteCharacteristics fromValue(String value) {
    if (value == null) {
      return null;
    }
    for (RouteCharacteristics type : values()) {
      if (type.value.equalsIgnoreCase(value)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown route characteristics: " + value);
  }
}
