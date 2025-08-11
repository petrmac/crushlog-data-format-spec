package io.cldf.models;

import java.time.OffsetDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.cldf.models.enums.RockType;
import io.cldf.models.enums.TerrainType;
import io.cldf.models.media.Media;
import io.cldf.utils.FlexibleDateTimeDeserializer;
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

  private String city;

  private String address;

  @Builder.Default private boolean starred = false;

  private RockType rockType;

  private TerrainType terrainType;

  private String accessInfo;

  /** Media associated with this location (overview, approach photos, etc.) */
  private Media media;

  @JsonDeserialize(using = FlexibleDateTimeDeserializer.class)
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
}
