package io.cldf.models.media;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.cldf.models.enums.MediaDesignation;
import io.cldf.models.enums.MediaSource;
import io.cldf.models.enums.MediaType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Individual media item */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaItem {

  /** Media type */
  @JsonProperty(required = true)
  private MediaType type;

  /** Primary path/URL to the media file */
  @JsonProperty(required = true)
  private String path;

  /** Asset ID for cloud/library stored media */
  private String assetId;

  /** Path to thumbnail (for performance) */
  private String thumbnailPath;

  /** Source of the media */
  private MediaSource source;

  /** Purpose or type of media content */
  private MediaDesignation designation;

  /** User-provided description or caption */
  private String caption;

  /** When the media was created or taken */
  private String timestamp;

  /** Additional metadata */
  private Map<String, Object> metadata;
}
