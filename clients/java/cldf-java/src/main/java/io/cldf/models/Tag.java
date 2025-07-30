package io.cldf.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.cldf.models.enums.PredefinedTagKey;
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
  private Integer id;

  @JsonProperty(required = true)
  private String name;

  @JsonProperty(required = true)
  private boolean isPredefined;

  private PredefinedTagKey predefinedTagKey;

  private String color;

  private String category;
}
