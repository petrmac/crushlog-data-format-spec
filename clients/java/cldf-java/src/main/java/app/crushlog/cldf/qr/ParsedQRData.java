package app.crushlog.cldf.qr;

import lombok.Builder;
import lombok.Data;

/** Parsed QR code data. */
@Data
@Builder
public class ParsedQRData {
  private Integer version;
  private String clid;
  private String shortClid;
  private String url;
  private String ipfsHash;
  private RouteInfo route;
  private LocationInfo location;
  private boolean hasOfflineData;
  private boolean blockchainVerified;

  /** Route information from QR code. */
  @Data
  @Builder
  public static class RouteInfo {
    private Integer id;
    private String name;
    private String grade;
    private String gradeSystem; // Added to preserve grade system
    private String type;
    private Double height;
  }

  /** Location information from QR code. */
  @Data
  @Builder
  public static class LocationInfo {
    private String clid;
    private Integer id;
    private String name;
    private String country;
    private String state;
    private String city;
    private boolean indoor;
  }
}
