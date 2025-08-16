package app.crushlog.cldf.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Type of climbing session. */
public enum SessionType {
  SPORT_CLIMBING("sportClimbing"),
  MULTI_PITCH("multiPitch"),
  TRAD_CLIMBING("tradClimbing"),
  BOULDERING("bouldering"),
  INDOOR_CLIMBING("indoorClimbing"),
  INDOOR_BOULDERING("indoorBouldering"),
  BOARD_SESSION("boardSession");

  private final String value;

  SessionType(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @JsonCreator
  public static SessionType fromValue(String value) {
    if (value == null) {
      return null;
    }
    for (SessionType type : values()) {
      if (type.value.equalsIgnoreCase(value)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown session type: " + value);
  }
}
