package app.crushlog.cldf.qr;

import app.crushlog.cldf.models.Location;
import lombok.Builder;
import lombok.Data;

/** Options for QR code generation. */
@Data
@Builder
public class QROptions {
  @Builder.Default private String baseUrl = "https://crushlog.pro";

  @Builder.Default private boolean includeIPFS = false;

  private String ipfsHash;

  @Builder.Default private boolean blockchainRecord = false;

  private String blockchainNetwork;

  private Location location;

  @Builder.Default private QRDataFormat format = QRDataFormat.JSON;

  /** Data format for QR code payload. */
  public enum QRDataFormat {
    /** Full JSON payload with embedded data */
    JSON,
    /** Simple URL only */
    URL,
    /** Custom URI for mobile apps */
    CUSTOM_URI
  }
}
