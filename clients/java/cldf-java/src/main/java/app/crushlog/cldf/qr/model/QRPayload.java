package app.crushlog.cldf.qr.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Root model for QR code JSON payload. This class represents the complete structure that can be
 * unmarshalled from QR JSON data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class QRPayload {

  @JsonProperty("v")
  private Integer version;

  @JsonProperty("clid")
  private String clid;

  @JsonProperty("url")
  private String url;

  @JsonProperty("cldf")
  private String ipfsHash;

  @JsonProperty("route")
  private RoutePayload route;

  @JsonProperty("loc")
  private LocationPayload location;

  // Alternative location field name
  @JsonProperty("location")
  private LocationPayload locationAlt;

  @JsonProperty("meta")
  private MetaPayload meta;

  /** Get location from either 'loc' or 'location' field. */
  public LocationPayload getEffectiveLocation() {
    return location != null ? location : locationAlt;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class RoutePayload {
    @JsonProperty("id")
    private Integer id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("grade")
    private String grade;

    @JsonProperty("gradeSystem")
    private String gradeSystem;

    @JsonProperty("type")
    private String type;

    @JsonProperty("height")
    private Double height;

    @JsonProperty("fa")
    private FirstAscentPayload firstAscent;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class LocationPayload {
    @JsonProperty("clid")
    private String clid;

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("country")
    private String country;

    @JsonProperty("state")
    private String state;

    @JsonProperty("city")
    private String city;

    @JsonProperty("indoor")
    private Boolean indoor;

    @JsonProperty("coords")
    private CoordinatesPayload coordinates;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CoordinatesPayload {
    @JsonProperty("lat")
    private Double latitude;

    @JsonProperty("lng")
    private Double longitude;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class FirstAscentPayload {
    @JsonProperty("name")
    private String name;

    @JsonProperty("year")
    private Integer year;

    @JsonProperty("date")
    private String date;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class MetaPayload {
    @JsonProperty("blockchain")
    private Boolean blockchain;

    @JsonProperty("verified")
    private Boolean verified;

    @JsonProperty("timestamp")
    private Long timestamp;
  }
}
