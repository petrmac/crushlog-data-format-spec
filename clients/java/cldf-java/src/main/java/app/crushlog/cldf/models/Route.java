package app.crushlog.cldf.models;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import app.crushlog.cldf.models.enums.ProtectionRating;
import app.crushlog.cldf.models.enums.RouteCharacteristics;
import app.crushlog.cldf.models.enums.RouteType;
import app.crushlog.cldf.models.media.Media;
import app.crushlog.cldf.utils.FlexibleDateTimeDeserializer;
import app.crushlog.cldf.utils.FlexibleLocalDateDeserializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Represents a climbing route or boulder problem. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Route {

  @JsonProperty(required = true)
  private Integer id;

  /** CrushLog ID - globally unique identifier (v1.3.0+) */
  private String clid;

  @JsonProperty(required = true)
  private Integer locationId;

  private Integer sectorId;

  @JsonProperty(required = true)
  private String name;

  @JsonProperty(required = true)
  private RouteType routeType;

  private RouteCharacteristics routeCharacteristics;

  private Grades grades;

  private Double height;

  private FirstAscent firstAscent;

  private Integer qualityRating;

  private String color;

  private String beta;

  private ProtectionRating protectionRating;

  private String gearNotes;
  private List<String> tags;

  /** Media associated with this route (topos, beta videos, etc.) */
  private Media media;

  /** QR code data for physical route marking (v1.3.0+) */
  private QrCode qrCode;

  @JsonDeserialize(using = FlexibleDateTimeDeserializer.class)
  private OffsetDateTime createdAt;

  @JsonDeserialize(using = FlexibleDateTimeDeserializer.class)
  private OffsetDateTime updatedAt;

  /** Grade information for different grading systems. */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Grades {
    @JsonProperty("vScale")
    private String vScale;

    private String font;
    private String french;
    private String yds;
    private String uiaa;
  }

  /** First ascent information. */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class FirstAscent {
    private String name;

    @JsonDeserialize(using = FlexibleLocalDateDeserializer.class)
    private LocalDate date;

    private String info;
  }

  /** QR code data for physical route marking (v1.3.0+). */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class QrCode {
    /** Generated QR code data string. */
    private String data;

    /** Public URL for the route. */
    private String url;

    /** IPFS hash for CLDF archive reference. */
    private String ipfsHash;

    /** Blockchain transaction hash for permanent record. */
    private String blockchainTx;

    /** When the QR code was generated. */
    @JsonDeserialize(using = FlexibleDateTimeDeserializer.class)
    private OffsetDateTime generatedAt;
  }
}
