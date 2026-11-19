package app.crushlog.cldf.models;

import java.time.OffsetDateTime;
import java.util.List;

import app.crushlog.cldf.models.enums.ClimbType;
import app.crushlog.cldf.models.enums.GradeScaleSpread;
import app.crushlog.cldf.utils.FlexibleDateTimeDeserializer;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A grading scale used at a location, such as a gym's numbered or coloured levels. Each level
 * starts at a grade on the difficulty axis.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeScale {

  /** Globally unique id of the scale. Climbs refer to it in their grades. */
  @JsonProperty(required = true)
  private String id;

  /** The kind of climb the scale grades. Boulder when absent. */
  private ClimbType climbType;

  /** How the levels were laid out when the scale was created. */
  private GradeScaleSpread spread;

  /** When the scale last changed. Of two copies of a scale, the newer one wins. */
  @JsonProperty(required = true)
  @JsonDeserialize(using = FlexibleDateTimeDeserializer.class)
  private OffsetDateTime updatedAt;

  /**
   * Set when the scale was removed. A removed scale stays listed so that climbs logged on it still
   * resolve.
   */
  @JsonDeserialize(using = FlexibleDateTimeDeserializer.class)
  private OffsetDateTime deletedAt;

  /** The levels, easiest first. */
  @JsonProperty(required = true)
  private List<Level> levels;

  /**
   * Whether the scale was removed.
   *
   * @return true when the scale carries a removal time
   */
  @JsonIgnore
  public boolean isDeleted() {
    return deletedAt != null;
  }

  /**
   * Finds a level by its number.
   *
   * @param number the level number, from 1
   * @return the level, or null when the scale has no such level
   */
  public Level levelNumbered(int number) {
    if (levels == null) {
      return null;
    }
    for (Level candidate : levels) {
      if (candidate.getLevel() != null && candidate.getLevel() == number) {
        return candidate;
      }
    }
    return null;
  }

  /** One level of a grading scale. */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Level {

    /** Level number, from 1 (easiest). */
    @JsonProperty(required = true)
    private Integer level;

    /**
     * The grade where the level starts: a Font grade for a boulder scale, a French grade for a
     * route scale.
     */
    @JsonProperty(required = true)
    private String startGrade;

    /** The value of the start grade on the difficulty axis. */
    @JsonProperty(required = true)
    private Integer startNumeric;

    /** Optional colour of the level, as a name or a hex value. */
    private String color;
  }
}
