package app.crushlog.cldf.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Predefined tag keys. */
public enum PredefinedTagKey {
  OVERHANG("overhang"),
  SLAB("slab"),
  VERTICAL("vertical"),
  ROOF("roof"),
  CRACK("crack"),
  CORNER("corner"),
  ARETE("arete"),
  DYNO("dyno"),
  CRIMPY("crimpy"),
  SLOPERS("slopers"),
  JUGS("jugs"),
  POCKETS("pockets"),
  TECHNICAL("technical"),
  POWERFUL("powerful"),
  ENDURANCE("endurance");

  private final String value;

  PredefinedTagKey(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @JsonCreator
  public static PredefinedTagKey fromValue(String value) {
    if (value == null) {
      return null;
    }
    for (PredefinedTagKey key : values()) {
      if (key.value.equalsIgnoreCase(value)) {
        return key;
      }
    }
    throw new IllegalArgumentException("Unknown predefined tag key: " + value);
  }
}
