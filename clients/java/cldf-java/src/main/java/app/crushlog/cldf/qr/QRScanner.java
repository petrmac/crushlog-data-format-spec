package app.crushlog.cldf.qr;

import java.awt.image.BufferedImage;
import java.util.Optional;

import app.crushlog.cldf.models.Location;
import app.crushlog.cldf.models.Route;
import app.crushlog.cldf.qr.impl.DefaultQRScanner;
import app.crushlog.cldf.qr.result.QRError;
import app.crushlog.cldf.qr.result.Result;

/**
 * Functional interface for QR code scanning that uses Result types instead of exceptions. This
 * provides a more modern, testable API that models success and failure as data.
 *
 * <p>Also provides static convenience methods for backward compatibility.
 */
public interface QRScanner {

  /**
   * Parse QR code data string.
   *
   * @param data The QR code data string
   * @return Result containing parsed data or error
   */
  Result<ParsedQRData, QRError> parse(String data);

  /**
   * Scan a QR code from an image.
   *
   * @param image The image containing the QR code
   * @return Result containing parsed data or error
   */
  Result<ParsedQRData, QRError> scan(BufferedImage image);

  /**
   * Scan a QR code from image bytes.
   *
   * @param imageBytes The image bytes
   * @return Result containing parsed data or error
   */
  Result<ParsedQRData, QRError> scan(byte[] imageBytes);

  /**
   * Convert parsed QR data to a Route object.
   *
   * @param data The parsed QR data
   * @return Result containing route or error
   */
  Result<Route, QRError> toRoute(ParsedQRData data);

  /**
   * Convert parsed QR data to a Location object.
   *
   * @param data The parsed QR data
   * @return Result containing location or error
   */
  Result<Location, QRError> toLocation(ParsedQRData data);

  /**
   * Validate QR code data.
   *
   * @param data The QR code data string
   * @return Result containing validated data or error
   */
  Result<String, QRError> validate(String data);

  /**
   * Parse and convert to Route in one operation.
   *
   * @param data The QR code data string
   * @return Result containing route or error
   */
  default Result<Route, QRError> parseToRoute(String data) {
    return parse(data).flatMap(this::toRoute);
  }

  /**
   * Parse and convert to Location in one operation.
   *
   * @param data The QR code data string
   * @return Result containing location or error
   */
  default Result<Location, QRError> parseToLocation(String data) {
    return parse(data).flatMap(this::toLocation);
  }

  /**
   * Scan image and convert to Route in one operation.
   *
   * @param image The image containing the QR code
   * @return Result containing route or error
   */
  default Result<Route, QRError> scanToRoute(BufferedImage image) {
    return scan(image).flatMap(this::toRoute);
  }

  /**
   * Scan image and convert to Location in one operation.
   *
   * @param image The image containing the QR code
   * @return Result containing location or error
   */
  default Result<Location, QRError> scanToLocation(BufferedImage image) {
    return scan(image).flatMap(this::toLocation);
  }

  // Static convenience methods for backward compatibility and ease of use

  /**
   * Static method to parse QR code data. For backward compatibility with existing code.
   *
   * @param data The QR code data string
   * @return Parsed QR data
   * @throws QRParseException if parsing fails
   */
  static ParsedQRData parseString(String data) throws QRParseException {
    QRScanner scanner = new DefaultQRScanner();
    return scanner.parse(data).orElseThrow(() -> new QRParseException("Failed to parse QR data"));
  }

  /**
   * Static method to convert parsed data to Route. For backward compatibility with existing code.
   *
   * @param data The parsed QR data
   * @return Optional containing route or empty
   */
  static Optional<Route> toRouteStatic(ParsedQRData data) {
    QRScanner scanner = new DefaultQRScanner();
    return scanner.toRoute(data).getSuccess();
  }

  /**
   * Static method to convert parsed data to Location. For backward compatibility with existing
   * code.
   *
   * @param data The parsed QR data
   * @return Optional containing location or empty
   */
  static Optional<Location> toLocationStatic(ParsedQRData data) {
    QRScanner scanner = new DefaultQRScanner();
    return scanner.toLocation(data).getSuccess();
  }

  /** Exception thrown when QR code parsing fails. For backward compatibility. */
  class QRParseException extends RuntimeException {
    public QRParseException(String message) {
      super(message);
    }

    public QRParseException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
