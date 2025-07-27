package io.cldf.models;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Represents a MediaItem in the CLDF archive. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaItem {

  @JsonProperty(required = true)
  private String id;

  @JsonProperty(required = true)
  private String climbId;

  @JsonProperty(required = true)
  private MediaType type;

  private MediaSource source;

  private String assetId;

  private String filename;

  private String thumbnailPath;

  @Builder.Default private boolean embedded = false;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
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

  public enum MediaType {
    photo,
    video
  }

  public enum MediaSource {
    photos_library,
    local,
    embedded
  }
}
