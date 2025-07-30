package io.cldf.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Protection rating for traditional routes. */
public enum ProtectionRating {
  BOMBPROOF("bombproof"),
  GOOD("good"),
  ADEQUATE("adequate"),
  RUNOUT("runout"),
  SERIOUS("serious"),
  X("x");

  private final String value;

  ProtectionRating(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @JsonCreator
  public static ProtectionRating fromValue(String value) {
    if (value == null) {
      return null;
    }
    for (ProtectionRating type : values()) {
      if (type.value.equalsIgnoreCase(value)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown protection rating: " + value);
  }
}
