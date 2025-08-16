package app.crushlog.cldf.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Type of terrain (natural or artificial). */
public enum TerrainType {
  // Natural rock
  NATURAL("natural"),
  // Artificial climbing wall
  ARTIFICIAL("artificial");

  private final String value;

  TerrainType(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @JsonCreator
  public static TerrainType fromValue(String value) {
    if (value == null) {
      return null;
    }
    for (TerrainType type : values()) {
      if (type.value.equalsIgnoreCase(value)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown terrain type: " + value);
  }
}
