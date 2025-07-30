package io.cldf.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Type of belay used for roped climbs. */
public enum BelayType {
  // Top rope belay
  TOP_ROPE("topRope"),
  // Lead climbing
  LEAD("lead"),
  // Auto-belay device
  AUTO_BELAY("autoBelay");

  private final String value;

  BelayType(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @JsonCreator
  public static BelayType fromValue(String value) {
    if (value == null) {
      return null;
    }
    for (BelayType type : values()) {
      if (type.value.equalsIgnoreCase(value)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown belay type: " + value);
  }
}
