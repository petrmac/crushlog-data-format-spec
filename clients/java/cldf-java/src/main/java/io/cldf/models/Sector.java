package io.cldf.models;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.cldf.utils.FlexibleDateTimeDeserializer;
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
