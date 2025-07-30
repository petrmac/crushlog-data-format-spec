package io.cldf.models;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.cldf.models.enums.BelayType;
import io.cldf.models.enums.ClimbType;
import io.cldf.models.enums.FinishType;
import io.cldf.models.enums.GradeSystem;
import io.cldf.models.enums.RockType;
import io.cldf.models.enums.TerrainType;
import io.cldf.utils.FlexibleLocalDateDeserializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single climbing attempt or completion. Contains all information about a specific
 * climb including grades, attempts, and outcome.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Climb {

  @JsonProperty(required = true)
  private Integer id;

  private Integer sessionId;

  private String routeId;

  @JsonProperty(required = true)
  @JsonDeserialize(using = FlexibleLocalDateDeserializer.class)
  private LocalDate date;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
  private LocalTime time;

  @JsonProperty(required = true)
  private String routeName;

  private GradeInfo grades;

  @JsonProperty(required = true)
  private ClimbType type;

  @JsonProperty(required = true)
  private FinishType finishType;

  @Builder.Default private int attempts = 1;

  @Builder.Default private int repeats = 0;

  @Builder.Default private boolean isRepeat = false;

  private BelayType belayType;

  private Integer duration;

  private Integer falls;

  private Double height;

  private Integer rating;

  private String notes;

  private List<String> tags;

  private String beta;

  private Media media;

  private String color;

  private RockType rockType;

  private TerrainType terrainType;

  private Boolean isIndoor;

  private List<String> partners;

  private String weather;

  private Map<String, Object> customFields;

  /** Grade information for a climb. */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class GradeInfo {
    @JsonProperty(required = true)
    private GradeSystem system;

    @JsonProperty(required = true)
    private String grade;

    private Map<String, String> conversions;
  }

  /** Media references for a climb. */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Media {
    private List<String> photos;
    private List<String> videos;
    private Integer count;
  }
}
