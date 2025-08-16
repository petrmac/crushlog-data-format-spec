package app.crushlog.cldf.qr.result;

import lombok.Value;

/**
 * Represents errors that can occur during QR code operations. This is a data class that models
 * errors as values rather than exceptions.
 */
@Value
public class QRError {

  ErrorType type;
  String message;
  String details;

  /** Create a QRError with just a type and message. */
  public static QRError of(ErrorType type, String message) {
    return new QRError(type, message, null);
  }

  /** Create a QRError with full details. */
  public static QRError of(ErrorType type, String message, String details) {
    return new QRError(type, message, details);
  }

  /** Create a QRError from an exception. */
  public static QRError from(ErrorType type, Exception e) {
    String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    String details = e.getCause() != null ? e.getCause().getMessage() : null;
    return new QRError(type, message, details);
  }

  /** Create a parse error. */
  public static QRError parseError(String message) {
    return of(ErrorType.PARSE_ERROR, message);
  }

  /** Create a scan error. */
  public static QRError scanError(String message) {
    return of(ErrorType.SCAN_ERROR, message);
  }

  /** Create a validation error. */
  public static QRError validationError(String message) {
    return of(ErrorType.VALIDATION_ERROR, message);
  }

  /** Create a generation error. */
  public static QRError generationError(String message) {
    return of(ErrorType.GENERATION_ERROR, message);
  }

  /** Create an image processing error. */
  public static QRError imageError(String message) {
    return of(ErrorType.IMAGE_ERROR, message);
  }

  /** Get a human-readable description of the error. */
  public String getDescription() {
    if (details != null && !details.isEmpty()) {
      return String.format("[%s] %s: %s", type, message, details);
    }
    return String.format("[%s] %s", type, message);
  }

  /** Types of errors that can occur in QR operations. */
  public enum ErrorType {
    PARSE_ERROR("Failed to parse QR data"),
    SCAN_ERROR("Failed to scan QR code"),
    VALIDATION_ERROR("QR data validation failed"),
    GENERATION_ERROR("Failed to generate QR code"),
    IMAGE_ERROR("Image processing error"),
    INVALID_FORMAT("Invalid QR format"),
    MISSING_DATA("Required data missing"),
    UNSUPPORTED_VERSION("Unsupported QR version");

    private final String description;

    ErrorType(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }
}
