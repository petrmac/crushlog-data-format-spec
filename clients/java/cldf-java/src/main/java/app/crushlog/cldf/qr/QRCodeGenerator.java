package app.crushlog.cldf.qr;

import java.awt.image.BufferedImage;

import app.crushlog.cldf.models.Location;
import app.crushlog.cldf.models.Route;

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
   * Generate QR code image for a route.
   *
   * @param route The route to generate QR code for
   * @param options Generation options
   * @return BufferedImage containing the QR code
   */
  BufferedImage generateImage(Route route, QROptions options);

  /**
   * Generate QR code image for a location.
   *
   * @param location The location to generate QR code for
   * @param options Generation options
   * @return BufferedImage containing the QR code
   */
  BufferedImage generateImage(Location location, QROptions options);

  /**
   * Generate QR code image from data payload.
   *
   * @param data The data to encode
   * @param imageOptions Image generation options
   * @return BufferedImage containing the QR code
   */
  BufferedImage generateImage(String data, QRImageOptions imageOptions);

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
