package app.crushlog.cldf.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Source of media. */
public enum MediaSource {
  LOCAL("local"),
  CLOUD("cloud"),
  REFERENCE("reference"),
  EMBEDDED("embedded"),
  EXTERNAL("external"),
  // Legacy value for backward compatibility
  PHOTOS_LIBRARY("photos_library");

  private final String value;

  MediaSource(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @JsonCreator
  public static MediaSource fromValue(String value) {
    if (value == null) {
      return null;
    }
    for (MediaSource source : values()) {
      if (source.value.equalsIgnoreCase(value)) {
        return source;
      }
    }
    throw new IllegalArgumentException("Unknown media source: " + value);
  }
}
