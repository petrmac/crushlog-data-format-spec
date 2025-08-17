package app.crushlog.cldf.qr;

import app.crushlog.cldf.models.Location;
import app.crushlog.cldf.models.Route;
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
   * Scan image bytes and convert to Route in one operation.
   *
   * @param imageBytes The image bytes containing the QR code
   * @return Result containing route or error
   */
  default Result<Route, QRError> scanToRoute(byte[] imageBytes) {
    return scan(imageBytes).flatMap(this::toRoute);
  }

  /**
   * Scan image bytes and convert to Location in one operation.
   *
   * @param imageBytes The image bytes containing the QR code
   * @return Result containing location or error
   */
  default Result<Location, QRError> scanToLocation(byte[] imageBytes) {
    return scan(imageBytes).flatMap(this::toLocation);
  }
}
