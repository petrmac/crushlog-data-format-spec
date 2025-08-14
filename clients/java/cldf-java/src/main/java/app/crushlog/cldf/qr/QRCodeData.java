package app.crushlog.cldf.qr;

import java.util.Map;

import lombok.Builder;
import lombok.Data;

/** Represents QR code data payload. */
@Data
@Builder
public class QRCodeData {
  private final int version;
  private final String clid;
  private final String url;
  private final String ipfsHash;
  private final Map<String, Object> routeData;
  private final Map<String, Object> locationData;
  private final Map<String, Object> metadata;

  /**
   * Convert to JSON string.
   *
   * @return JSON representation of this QR code data
   */
  public String toJson() {
    // This will be implemented by the generator
    return null;
  }

  /**
   * Get the primary URL for this QR code.
   *
   * @return The main URL
   */
  public String getPrimaryUrl() {
    return url;
  }

  /**
   * Check if this QR code has offline data.
   *
   * @return true if offline data is present
   */
  public boolean hasOfflineData() {
    return routeData != null || locationData != null;
  }
}
