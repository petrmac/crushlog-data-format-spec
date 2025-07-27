package io.cldf.models;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
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
  private String id;

  @JsonProperty(required = true)
  private String locationId;

  @JsonProperty(required = true)
  private String name;

  @JsonProperty("isDefault")
  @Builder.Default
  private boolean isDefault = false;

  private String description;

  private String approach;

  private Coordinates coordinates;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
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
