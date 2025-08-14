package app.crushlog.cldf.qr;

import app.crushlog.cldf.qr.impl.DefaultQRCodeGenerator;
import app.crushlog.cldf.qr.impl.DefaultQRScanner;

/** Factory for creating QR code generators and scanners. */
public class QRCodeFactory {

  private QRCodeFactory() {
    // Private constructor to prevent instantiation
  }

  /**
   * Create a default QR code generator.
   *
   * @return QRCodeGenerator instance
   */
  public static QRCodeGenerator createGenerator() {
    return new DefaultQRCodeGenerator();
  }

  /**
   * Create a functional QR code scanner.
   *
   * @return FunctionalQRScanner instance
   */
  public static QRScanner createScanner() {
    return new DefaultQRScanner();
  }

  /**
   * Create default QR options.
   *
   * @return Default QROptions
   */
  public static QROptions defaultOptions() {
    return QROptions.builder().build();
  }

  /**
   * Create default image options.
   *
   * @return Default QRImageOptions
   */
  public static QRImageOptions defaultImageOptions() {
    return QRImageOptions.builder().build();
  }

  /**
   * Create high quality image options with error correction.
   *
   * @return High quality QRImageOptions
   */
  public static QRImageOptions highQualityImageOptions() {
    return QRImageOptions.builder()
        .size(512)
        .errorCorrectionLevel(QRImageOptions.ErrorCorrectionLevel.H)
        .margin(8)
        .build();
  }

  /**
   * Create compact image options for small displays.
   *
   * @return Compact QRImageOptions
   */
  public static QRImageOptions compactImageOptions() {
    return QRImageOptions.builder()
        .size(128)
        .errorCorrectionLevel(QRImageOptions.ErrorCorrectionLevel.L)
        .margin(2)
        .build();
  }
}
