package io.cldf.models;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.cldf.models.enums.ClimbType;
import io.cldf.models.enums.RockType;
import io.cldf.models.enums.SessionType;
import io.cldf.models.enums.TerrainType;
import io.cldf.utils.FlexibleLocalDateDeserializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
/** Represents a Session in the CLDF archive. */
public class Session {

  @JsonProperty(required = true)
  private Integer id;

  @JsonProperty(required = true)
  @JsonDeserialize(using = FlexibleLocalDateDeserializer.class)
  private LocalDate date;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
  private LocalTime startTime;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
  private LocalTime endTime;

  @JsonProperty(required = true)
  private String location;

  private Integer locationId;

  private Boolean isIndoor;

  private ClimbType climbType;

  private SessionType sessionType;

  private List<String> partners;

  private Weather weather;

  private String notes;

  private RockType rockType;

  private TerrainType terrainType;

  private Integer approachTime;

  @JsonProperty("isOngoing")
  @Builder.Default
  private boolean isOngoing = false;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Weather {
    private String conditions;
    private Double temperature;
    private Double humidity;
    private String wind;
  }
}
