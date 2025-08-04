package io.cldf.models.media;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Media references for climbs */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Media {

  /** List of media items */
  private List<MediaItem> items;

  /** Total media count */
  private Integer count;
}