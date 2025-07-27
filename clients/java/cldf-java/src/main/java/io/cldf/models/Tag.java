package io.cldf.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
/** Represents a Tag in the CLDF archive. */
public class Tag {

  @JsonProperty(required = true)
  private String id;

  @JsonProperty(required = true)
  private String name;

  @JsonProperty(required = true)
  private Boolean isPredefined;

  private PredefinedTagKey predefinedTagKey;

  private String color;

  private String category;

  public enum PredefinedTagKey {
    overhang,
    slab,
    vertical,
    roof,
    crack,
    corner,
    arete,
    dyno,
    crimpy,
    slopers,
    jugs,
    pockets,
    technical,
    powerful,
    endurance
  }
}
