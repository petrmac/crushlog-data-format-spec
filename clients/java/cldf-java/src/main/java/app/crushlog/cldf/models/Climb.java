package app.crushlog.cldf.models;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import app.crushlog.cldf.models.enums.BelayType;
import app.crushlog.cldf.models.enums.ClimbType;
import app.crushlog.cldf.models.enums.FinishType;
import app.crushlog.cldf.models.enums.GradeSystem;
import app.crushlog.cldf.models.enums.RockType;
import app.crushlog.cldf.models.enums.TerrainType;
import app.crushlog.cldf.models.media.Media;
import app.crushlog.cldf.utils.FlexibleLocalDateDeserializer;
import app.crushlog.cldf.utils.GradeAxis;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
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

  /** CrushLog ID - globally unique identifier (v1.3.0+) */
  private String clid;

  private Integer sessionId;

  private Integer routeId;

  /** CrushLog ID of the route - globally unique identifier for server matching (v1.4.0+) */
  private String routeClid;

  @JsonProperty(required = true)
  @JsonDeserialize(using = FlexibleLocalDateDeserializer.class)
  private LocalDate date;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
  private LocalTime time;

  private String routeName;

  private GradeInfo grades;

  @JsonProperty(required = true)
  private ClimbType type;

  @JsonProperty(required = true)
  private FinishType finishType;

  @Builder.Default private int attempts = 1;

  @Builder.Default private int repeats = 0;

  @JsonProperty("isRepeat")
  @Builder.Default
  private boolean isRepeat = false;

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

  @JsonProperty("isIndoor")
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

    /**
     * The grade's value on the difficulty axis, when the archive carries it. Use {@link
     * #effectiveNumeric()} to also cover archives that do not.
     */
    private Integer numeric;

    /**
     * True when the grade was derived from a level of a grading scale and not read off the climb.
     */
    private Boolean estimated;

    /** The level of a grading scale this climb was logged on. */
    private ScaleRef scale;

    /**
     * The grade's value on the difficulty axis: {@code numeric} when present, otherwise computed
     * from the system and the grade.
     *
     * @return the value, or null when the grade is not on its system's ladder
     */
    public Integer effectiveNumeric() {
      return numeric != null ? numeric : GradeAxis.numericFor(system, grade);
    }
  }

  /** The level of a grading scale a climb was logged on. */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ScaleRef {
    /** The id of the scale, as listed in the location's grade scales. */
    @JsonProperty(required = true)
    private String id;

    /** The level number within the scale, from 1 (easiest). */
    @JsonProperty(required = true)
    private Integer level;
  }
}
