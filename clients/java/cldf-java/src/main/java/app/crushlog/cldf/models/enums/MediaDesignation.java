package app.crushlog.cldf.models.enums;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Purpose or type of media content */
public enum MediaDesignation {
  TOPO("topo"), // Route diagram/map
  BETA("beta"), // How-to information
  APPROACH("approach"), // Access/approach info
  LOG("log"), // Climb documentation
  OVERVIEW("overview"), // General view/panorama
  CONDITIONS("conditions"), // Current conditions
  GEAR("gear"), // Gear placement/requirements
  DESCENT("descent"), // Descent information
  OTHER("other"); // Unspecified purpose

  private final String value;

  MediaDesignation(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @JsonCreator
  public static MediaDesignation fromValue(String value) {
    if (value == null) {
      return OTHER; // Default to OTHER if null
    }
    return Arrays.stream(MediaDesignation.values())
        .filter(designation -> designation.value.equalsIgnoreCase(value))
        .findFirst()
        .orElse(OTHER); // Default to OTHER if not found
  }

  @Override
  public String toString() {
    return value;
  }
}
