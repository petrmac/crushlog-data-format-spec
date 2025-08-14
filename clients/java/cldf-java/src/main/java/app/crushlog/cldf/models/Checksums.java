package app.crushlog.cldf.models;

import java.time.OffsetDateTime;
import java.util.Map;

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
/** Container for Checksums data in CLDF archive. */
public class Checksums {

  @JsonProperty(required = true)
  @Builder.Default
  private String algorithm = "SHA-256";

  @JsonProperty(required = true)
  private Map<String, String> files;

  @JsonDeserialize(using = FlexibleDateTimeDeserializer.class)
  private OffsetDateTime generatedAt;
}
