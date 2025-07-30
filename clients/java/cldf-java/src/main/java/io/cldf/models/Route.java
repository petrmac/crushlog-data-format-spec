package io.cldf.models;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.cldf.models.enums.ProtectionRating;
import io.cldf.models.enums.RouteCharacteristics;
import io.cldf.models.enums.RouteType;
import io.cldf.utils.FlexibleDateTimeDeserializer;
import io.cldf.utils.FlexibleLocalDateDeserializer;
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
  private Integer id;

  @JsonProperty(required = true)
  private Integer locationId;

  private Integer sectorId;

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

  private ProtectionRating protectionRating;

  private String gearNotes;
  private List<String> tags;

  @JsonDeserialize(using = FlexibleDateTimeDeserializer.class)
  private OffsetDateTime createdAt;

  @JsonDeserialize(using = FlexibleDateTimeDeserializer.class)
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

    @JsonDeserialize(using = FlexibleLocalDateDeserializer.class)
    private LocalDate date;

    private String info;
  }
}
