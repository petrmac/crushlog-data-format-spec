package io.cldf.models;

import java.time.OffsetDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Represents a climbing location such as a crag, boulder area, or climbing gym. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Location {

  @JsonProperty(required = true)
  private Integer id;

  @JsonProperty(required = true)
  private String name;

  @JsonProperty(required = true)
  private Boolean isIndoor;

  private Coordinates coordinates;

  private String country;

  private String state;

  @Builder.Default private Boolean starred = false;

  private RockType rockType;

  private TerrainType terrainType;

  private String accessInfo;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
  private OffsetDateTime createdAt;

  private Map<String, Object> customFields;

  /** Geographic coordinates for a location. */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Coordinates {
    @JsonProperty(required = true)
    private Double latitude;

    @JsonProperty(required = true)
    private Double longitude;
  }

  /** Types of rock found at climbing locations. */
  public enum RockType {
    // Sandstone
    sandstone,
    // Limestone
    limestone,
    // Granite
    granite,
    // Basalt
    basalt,
    // Gneiss
    gneiss,
    // Quartzite
    quartzite,
    // Conglomerate
    conglomerate,
    // Schist
    schist,
    // Dolomite
    dolomite,
    // Slate
    slate,
    // Rhyolite
    rhyolite,
    // Gabbro
    gabbro,
    // Volcanic tuff
    volcanicTuff,
    // Andesite
    andesite,
    // Chalk
    chalk
  }

  /** Type of terrain (natural or artificial). */
  public enum TerrainType {
    // Natural rock
    natural,
    // Artificial climbing wall
    artificial
  }
}
