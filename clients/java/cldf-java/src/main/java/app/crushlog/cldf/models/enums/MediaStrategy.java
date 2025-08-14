package app.crushlog.cldf.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Media export strategy. */
public enum MediaStrategy {
  REFERENCE("reference"),
  THUMBNAILS("thumbnails"),
  FULL("full");

  private final String value;

  MediaStrategy(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @JsonCreator
  public static MediaStrategy fromValue(String value) {
    if (value == null) {
      return null;
    }
    for (MediaStrategy strategy : values()) {
      if (strategy.value.equalsIgnoreCase(value)) {
        return strategy;
      }
    }
    throw new IllegalArgumentException("Unknown media strategy: " + value);
  }
}
