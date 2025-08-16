package app.crushlog.cldf.models;

import app.crushlog.cldf.models.enums.PredefinedTagKey;
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
  private Integer id;

  @JsonProperty(required = true)
  private String name;

  @JsonProperty(required = true)
  private boolean isPredefined;

  private PredefinedTagKey predefinedTagKey;

  private String color;

  private String category;
}
