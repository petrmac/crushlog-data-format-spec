package io.cldf.models;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.cldf.models.enums.MediaSource;
import io.cldf.models.enums.MediaType;
import io.cldf.utils.FlexibleDateTimeDeserializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Represents a MediaMetadataItem in the CLDF archive (for standalone media files). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaMetadataItem {

  @JsonProperty(required = true)
  private Integer id;

  @JsonProperty(required = true)
  private Integer climbId;

  @JsonProperty(required = true)
  private MediaType type;

  private MediaSource source;

  private String assetId;

  private String filename;

  private String thumbnailPath;

  @Builder.Default private boolean embedded = false;

  @JsonDeserialize(using = FlexibleDateTimeDeserializer.class)
  private OffsetDateTime createdAt;

  private Metadata metadata;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Metadata {
    private Integer width;
    private Integer height;
    private Integer size;
    private Double duration;
    private Location location;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Location {
      private Double latitude;
      private Double longitude;
    }
  }
}
