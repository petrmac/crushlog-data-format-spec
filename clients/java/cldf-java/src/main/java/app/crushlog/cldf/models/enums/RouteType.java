package app.crushlog.cldf.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Type of route. */
public enum RouteType {
  BOULDER("boulder"),
  ROUTE("route");

  private final String value;

  RouteType(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @JsonCreator
  public static RouteType fromValue(String value) {
    if (value == null) {
      return null;
    }
    for (RouteType type : values()) {
      if (type.value.equalsIgnoreCase(value)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown route type: " + value);
  }
}
