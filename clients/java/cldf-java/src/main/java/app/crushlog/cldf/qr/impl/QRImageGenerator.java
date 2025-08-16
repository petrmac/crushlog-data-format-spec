package app.crushlog.cldf.qr.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import app.crushlog.cldf.qr.QRImageOptions;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.extern.slf4j.Slf4j;

/**
 * Generates QR code images using ZXing library with pure Java implementation. No AWT dependencies -
 * works in both JVM and GraalVM native image.
 */
@Slf4j
public class QRImageGenerator {

  private final QRCodeWriter qrCodeWriter = new QRCodeWriter();
  private final PureJavaPNGGenerator pngGenerator = new PureJavaPNGGenerator();

  /**
   * Generate QR code as PNG byte array.
   *
   * @param data The data to encode
   * @param options Image generation options
   * @return Byte array containing PNG image
   */
  public byte[] generatePNG(String data, QRImageOptions options) {
    try {
      BitMatrix bitMatrix = generateBitMatrix(data, options);

      return pngGenerator.generatePNG(
          bitMatrix,
          options.getSize(),
          options.getForegroundColor().getRGB(),
          options.getBackgroundColor().getRGB());
    } catch (WriterException | IOException e) {
      log.error("Failed to generate QR code PNG", e);
      throw new RuntimeException("Failed to generate QR code PNG", e);
    }
  }

  /**
   * Generate QR code as SVG string.
   *
   * @param data The data to encode
   * @param options Image generation options
   * @return SVG string
   */
  public String generateSVG(String data, QRImageOptions options) {
    try {
      BitMatrix bitMatrix = generateBitMatrix(data, options);
      return convertToSVG(bitMatrix, options);
    } catch (WriterException e) {
      log.error("Failed to generate QR code SVG", e);
      throw new RuntimeException("Failed to generate QR code SVG", e);
    }
  }

  /** Generate the QR code bit matrix. */
  private BitMatrix generateBitMatrix(String data, QRImageOptions options) throws WriterException {
    Map<EncodeHintType, Object> hints = createHints(options);

    return qrCodeWriter.encode(
        data, BarcodeFormat.QR_CODE, options.getSize(), options.getSize(), hints);
  }

  private Map<EncodeHintType, Object> createHints(QRImageOptions options) {
    Map<EncodeHintType, Object> hints = new HashMap<>();

    // Convert our error correction level to ZXing's
    ErrorCorrectionLevel ecLevel =
        switch (options.getErrorCorrectionLevel()) {
          case L -> ErrorCorrectionLevel.L;
          case M -> ErrorCorrectionLevel.M;
          case Q -> ErrorCorrectionLevel.Q;
          case H -> ErrorCorrectionLevel.H;
        };

    hints.put(EncodeHintType.ERROR_CORRECTION, ecLevel);
    hints.put(EncodeHintType.MARGIN, options.getMargin());
    hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

    return hints;
  }

  private String convertToSVG(BitMatrix matrix, QRImageOptions options) {
    StringBuilder svg = new StringBuilder();

    int width = matrix.getWidth();
    int height = matrix.getHeight();
    int moduleSize = Math.max(1, options.getSize() / width);
    int actualSize = Math.max(width * moduleSize, height * moduleSize);

    svg.append(
        String.format(
            "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"%d\" viewBox=\"0 0 %d %d\">\n",
            actualSize, actualSize, actualSize, actualSize));

    // Background
    svg.append(
        String.format(
            "  <rect width=\"100%%\" height=\"100%%\" fill=\"%s\"/>\n",
            options.getBackgroundColor().toHex()));

    // QR code modules
    svg.append(String.format("  <g fill=\"%s\">\n", options.getForegroundColor().toHex()));

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        if (matrix.get(x, y)) {
          svg.append(
              String.format(
                  "    <rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\"/>\n",
                  x * moduleSize, y * moduleSize, moduleSize, moduleSize));
        }
      }
    }

    svg.append("  </g>\n");
    svg.append("</svg>");

    return svg.toString();
  }
}
