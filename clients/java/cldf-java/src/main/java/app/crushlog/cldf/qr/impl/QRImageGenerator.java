package app.crushlog.cldf.qr.impl;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import app.crushlog.cldf.qr.QRImageOptions;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.extern.slf4j.Slf4j;

/** Generates QR code images using ZXing library. */
@Slf4j
public class QRImageGenerator {

  private final QRCodeWriter qrCodeWriter = new QRCodeWriter();

  /**
   * Generate QR code image from data.
   *
   * @param data The data to encode
   * @param options Image generation options
   * @return BufferedImage containing the QR code
   */
  public BufferedImage generateImage(String data, QRImageOptions options) {
    try {
      Map<EncodeHintType, Object> hints = createHints(options);

      BitMatrix bitMatrix =
          qrCodeWriter.encode(
              data, BarcodeFormat.QR_CODE, options.getSize(), options.getSize(), hints);

      MatrixToImageConfig config =
          new MatrixToImageConfig(
              options.getForegroundColor().getRGB(), options.getBackgroundColor().getRGB());

      BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix, config);

      // Apply customizations
      if (options.getLogoPath() != null) {
        qrImage = addLogo(qrImage, options.getLogoPath(), options.getLogoSize());
      }

      if (options.isRoundedCorners()) {
        qrImage = applyRoundedCorners(qrImage, options.getCornerRadius());
      }

      return qrImage;

    } catch (WriterException e) {
      log.error("Failed to generate QR code image", e);
      throw new RuntimeException("Failed to generate QR code image", e);
    }
  }

  /**
   * Generate QR code as PNG byte array.
   *
   * @param data The data to encode
   * @param options Image generation options
   * @return Byte array containing PNG image
   */
  public byte[] generatePNG(String data, QRImageOptions options) {
    try {
      BufferedImage image = generateImage(data, options);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ImageIO.write(image, "PNG", baos);
      return baos.toByteArray();
    } catch (IOException e) {
      log.error("Failed to convert QR code to PNG", e);
      throw new RuntimeException("Failed to convert QR code to PNG", e);
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
      Map<EncodeHintType, Object> hints = createHints(options);

      BitMatrix bitMatrix =
          qrCodeWriter.encode(
              data, BarcodeFormat.QR_CODE, options.getSize(), options.getSize(), hints);

      return convertToSVG(bitMatrix, options);

    } catch (WriterException e) {
      log.error("Failed to generate QR code SVG", e);
      throw new RuntimeException("Failed to generate QR code SVG", e);
    }
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

  private BufferedImage addLogo(BufferedImage qrImage, String logoPath, int logoSize) {
    try {
      File logoFile = new File(logoPath);
      if (!logoFile.exists()) {
        log.warn("Logo file not found: {}", logoPath);
        return qrImage;
      }

      BufferedImage logo = ImageIO.read(logoFile);

      // Scale logo
      Image scaledLogo = logo.getScaledInstance(logoSize, logoSize, Image.SCALE_SMOOTH);

      // Create new image with logo
      BufferedImage combined =
          new BufferedImage(qrImage.getWidth(), qrImage.getHeight(), BufferedImage.TYPE_INT_ARGB);

      Graphics2D g = combined.createGraphics();
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      // Draw QR code
      g.drawImage(qrImage, 0, 0, null);

      // Calculate logo position (center)
      int x = (qrImage.getWidth() - logoSize) / 2;
      int y = (qrImage.getHeight() - logoSize) / 2;

      // Draw white background for logo
      g.setColor(Color.WHITE);
      g.fillRect(x - 5, y - 5, logoSize + 10, logoSize + 10);

      // Draw logo
      g.drawImage(scaledLogo, x, y, null);

      g.dispose();

      return combined;

    } catch (IOException e) {
      log.error("Failed to add logo to QR code", e);
      return qrImage;
    }
  }

  private BufferedImage applyRoundedCorners(BufferedImage image, int cornerRadius) {
    int width = image.getWidth();
    int height = image.getHeight();

    BufferedImage rounded = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

    Graphics2D g2 = rounded.createGraphics();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    // Create rounded rectangle shape
    RoundRectangle2D roundedRect =
        new RoundRectangle2D.Float(0, 0, width, height, cornerRadius, cornerRadius);

    // Set the clip to the rounded rectangle
    g2.setClip(roundedRect);

    // Draw the original image
    g2.drawImage(image, 0, 0, null);

    g2.dispose();

    return rounded;
  }

  private String convertToSVG(BitMatrix matrix, QRImageOptions options) {
    StringBuilder svg = new StringBuilder();

    int width = matrix.getWidth();
    int height = matrix.getHeight();
    int moduleSize = options.getSize() / width;

    svg.append(
        String.format(
            "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"%d\" viewBox=\"0 0 %d %d\">\n",
            options.getSize(), options.getSize(), options.getSize(), options.getSize()));

    // Background
    svg.append(
        String.format(
            "  <rect width=\"100%%\" height=\"100%%\" fill=\"%s\"/>\n",
            toHexColor(options.getBackgroundColor())));

    // QR code modules
    svg.append(String.format("  <g fill=\"%s\">\n", toHexColor(options.getForegroundColor())));

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

  private String toHexColor(Color color) {
    return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
  }
}
