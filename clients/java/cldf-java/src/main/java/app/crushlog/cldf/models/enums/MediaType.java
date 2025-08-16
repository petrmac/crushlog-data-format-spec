package app.crushlog.cldf.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Type of media. */
public enum MediaType {
  PHOTO("photo"),
  VIDEO("video");

  private final String value;

  MediaType(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @JsonCreator
  public static MediaType fromValue(String value) {
    if (value == null) {
      return null;
    }
    for (MediaType type : values()) {
      if (type.value.equalsIgnoreCase(value)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown media type: " + value);
  }
}
