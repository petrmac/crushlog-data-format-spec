package io.cldf.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Platform that created the export. */
public enum Platform {
  IOS("iOS"),
  ANDROID("Android"),
  WEB("Web"),
  DESKTOP("Desktop");

  private final String value;

  Platform(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @JsonCreator
  public static Platform fromValue(String value) {
    if (value == null) {
      return null;
    }
    for (Platform platform : values()) {
      if (platform.value.equalsIgnoreCase(value)) {
        return platform;
      }
    }
    throw new IllegalArgumentException("Unknown platform: " + value);
  }
}
