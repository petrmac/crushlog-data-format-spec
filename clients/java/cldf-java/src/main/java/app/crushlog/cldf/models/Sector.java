package app.crushlog.cldf.models;

import java.time.OffsetDateTime;

import app.crushlog.cldf.models.media.Media;
import app.crushlog.cldf.utils.FlexibleDateTimeDeserializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
/** Represents a Sector in the CLDF archive. */
public class Sector {

  @JsonProperty(required = true)
  private Integer id;

  /** CrushLog ID - globally unique identifier (v1.3.0+) */
  private String clid;

  @JsonProperty(required = true)
  private Integer locationId;

  @JsonProperty(required = true)
  private String name;

  @JsonProperty("isDefault")
  @Builder.Default
  private boolean isDefault = false;

  private String description;

  private String approach;

  private Coordinates coordinates;

  /** Media associated with this sector (overview photos, approach maps, etc.) */
  private Media media;

  @JsonDeserialize(using = FlexibleDateTimeDeserializer.class)
  private OffsetDateTime createdAt;

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
