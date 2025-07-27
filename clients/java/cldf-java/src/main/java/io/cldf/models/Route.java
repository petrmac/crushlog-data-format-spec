package io.cldf.models;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Represents a climbing route or boulder problem. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Route {

  @JsonProperty(required = true)
  private String id;

  @JsonProperty(required = true)
  private String locationId;

  private String sectorId;

  @JsonProperty(required = true)
  private String name;

  @JsonProperty(required = true)
  private RouteType routeType;

  private RouteCharacteristics routeCharacteristics;

  private Grades grades;

  private Double height;

  private FirstAscent firstAscent;

  private Integer qualityRating;

  private String color;

  private String beta;

  private ProtectionRating protection;

  private List<String> tags;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
  private OffsetDateTime createdAt;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
  private OffsetDateTime updatedAt;

  /** Grade information for different grading systems. */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Grades {
    @JsonProperty("vScale")
    private String vScale;

    private String font;
    private String french;
    private String yds;
    private String uiaa;
  }

  /** First ascent information. */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class FirstAscent {
    private String name;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate date;

    private String info;
  }

  /** Type of route. */
  public enum RouteType {
    boulder,
    route
  }

  /** Characteristics of the route. */
  public enum RouteCharacteristics {
    trad,
    bolted
  }
}
