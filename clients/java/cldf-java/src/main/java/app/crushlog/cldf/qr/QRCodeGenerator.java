package app.crushlog.cldf.qr;

import app.crushlog.cldf.models.Location;
import app.crushlog.cldf.models.Route;
import app.crushlog.cldf.qr.impl.QRDataGenerator;

/**
 * Interface for QR code generation. Provides methods for generating QR codes in various formats.
 */
public interface QRCodeGenerator {

  /**
   * Generate QR code data for a route.
   *
   * @param route The route to generate QR code for
   * @param options Generation options
   * @return QR code data payload
   */
  QRCodeData generateData(Route route, QROptions options);

  /**
   * Generate QR code data for a location.
   *
   * @param location The location to generate QR code for
   * @param options Generation options
   * @return QR code data payload
   */
  QRCodeData generateData(Location location, QROptions options);

  /**
   * Generate QR code PNG for a route.
   *
   * @param route The route to generate QR code for
   * @param options Generation options
   * @param imageOptions Image generation options
   * @return Byte array containing PNG image
   */
  default byte[] generatePNG(Route route, QROptions options, QRImageOptions imageOptions) {
    QRCodeData data = generateData(route, options);
    String payload = getPayloadString(data, options);
    return generatePNG(payload, imageOptions);
  }

  /**
   * Generate QR code PNG for a location.
   *
   * @param location The location to generate QR code for
   * @param options Generation options
   * @param imageOptions Image generation options
   * @return Byte array containing PNG image
   */
  default byte[] generatePNG(Location location, QROptions options, QRImageOptions imageOptions) {
    QRCodeData data = generateData(location, options);
    String payload = getPayloadString(data, options);
    return generatePNG(payload, imageOptions);
  }

  /** Get payload string from QR code data. Default implementation for convenience. */
  default String getPayloadString(QRCodeData data, QROptions options) {
    return switch (options.getFormat()) {
      case JSON -> new QRDataGenerator().toJson(data);
      case URL -> data.getUrl();
      case CUSTOM_URI -> "cldf://global/route/" + extractUuidFromClid(data.getClid());
    };
  }

  /** Extract UUID from CLID. Default implementation for convenience. */
  default String extractUuidFromClid(String clid) {
    if (clid == null) return "";
    try {
      app.crushlog.cldf.clid.CLID parsed = app.crushlog.cldf.clid.CLID.fromString(clid);
      return parsed.uuid();
    } catch (Exception e) {
      // Fallback for malformed CLIDs
      String[] parts = clid.split(":");
      if (parts.length == 4) {
        // New format: clid:v1:type:uuid
        return parts[3];
      } else if (parts.length == 3) {
        // Old format: clid:type:uuid
        return parts[2];
      }
      return "";
    }
  }

  /**
   * Generate QR code as byte array (PNG format).
   *
   * @param data The data to encode
   * @param imageOptions Image generation options
   * @return Byte array containing PNG image
   */
  byte[] generatePNG(String data, QRImageOptions imageOptions);

  /**
   * Generate QR code as SVG string.
   *
   * @param data The data to encode
   * @param imageOptions Image generation options
   * @return SVG string
   */
  String generateSVG(String data, QRImageOptions imageOptions);
}
