package io.cldf.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Types of rock found at climbing locations. */
public enum RockType {
  // Sandstone
  SANDSTONE("sandstone"),
  // Limestone
  LIMESTONE("limestone"),
  // Granite
  GRANITE("granite"),
  // Basalt
  BASALT("basalt"),
  // Gneiss
  GNEISS("gneiss"),
  // Quartzite
  QUARTZITE("quartzite"),
  // Conglomerate
  CONGLOMERATE("conglomerate"),
  // Schist
  SCHIST("schist"),
  // Dolomite
  DOLOMITE("dolomite"),
  // Slate
  SLATE("slate"),
  // Rhyolite
  RHYOLITE("rhyolite"),
  // Gabbro
  GABBRO("gabbro"),
  // Volcanic tuff
  VOLCANIC_TUFF("volcanicTuff"),
  // Andesite
  ANDESITE("andesite"),
  // Chalk
  CHALK("chalk");

  private final String value;

  RockType(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @JsonCreator
  public static RockType fromValue(String value) {
    if (value == null) {
      return null;
    }
    for (RockType type : values()) {
      if (type.value.equalsIgnoreCase(value)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown rock type: " + value);
  }
}
