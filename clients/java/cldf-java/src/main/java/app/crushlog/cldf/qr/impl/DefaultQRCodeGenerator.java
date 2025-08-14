package app.crushlog.cldf.qr.impl;

import java.awt.image.BufferedImage;

import app.crushlog.cldf.models.Location;
import app.crushlog.cldf.models.Route;
import app.crushlog.cldf.qr.*;
import lombok.extern.slf4j.Slf4j;

/**
 * Default implementation of QRCodeGenerator. Uses composition to delegate to specialized
 * components.
 */
@Slf4j
public class DefaultQRCodeGenerator implements QRCodeGenerator {

  private final QRDataGenerator dataGenerator;
  private final QRImageGenerator imageGenerator;

  public DefaultQRCodeGenerator() {
    this.dataGenerator = new QRDataGenerator();
    this.imageGenerator = new QRImageGenerator();
  }

  public DefaultQRCodeGenerator(QRDataGenerator dataGenerator, QRImageGenerator imageGenerator) {
    this.dataGenerator = dataGenerator;
    this.imageGenerator = imageGenerator;
  }

  @Override
  public QRCodeData generateData(Route route, QROptions options) {
    log.debug("Generating QR code data for route: {}", route.getName());
    return dataGenerator.generateRouteData(route, options);
  }

  @Override
  public QRCodeData generateData(Location location, QROptions options) {
    log.debug("Generating QR code data for location: {}", location.getName());
    return dataGenerator.generateLocationData(location, options);
  }

  @Override
  public BufferedImage generateImage(Route route, QROptions options) {
    QRCodeData data = generateData(route, options);
    String payload = getPayloadString(data, options);

    QRImageOptions imageOptions = QRImageOptions.builder().build();
    return imageGenerator.generateImage(payload, imageOptions);
  }

  @Override
  public BufferedImage generateImage(Location location, QROptions options) {
    QRCodeData data = generateData(location, options);
    String payload = getPayloadString(data, options);

    QRImageOptions imageOptions = QRImageOptions.builder().build();
    return imageGenerator.generateImage(payload, imageOptions);
  }

  @Override
  public BufferedImage generateImage(String data, QRImageOptions imageOptions) {
    return imageGenerator.generateImage(data, imageOptions);
  }

  @Override
  public byte[] generatePNG(String data, QRImageOptions imageOptions) {
    return imageGenerator.generatePNG(data, imageOptions);
  }

  @Override
  public String generateSVG(String data, QRImageOptions imageOptions) {
    return imageGenerator.generateSVG(data, imageOptions);
  }

  private String getPayloadString(QRCodeData data, QROptions options) {
    return switch (options.getFormat()) {
      case JSON -> dataGenerator.toJson(data);
      case URL -> data.getUrl();
      case CUSTOM_URI -> {
        // For custom URI, we need to reconstruct from the data
        // This is a simplified version
        yield "cldf://global/route/" + extractUuidFromClid(data.getClid());
      }
    };
  }

  private String extractUuidFromClid(String clid) {
    if (clid == null) return "";
    String[] parts = clid.split(":");
    return parts.length >= 3 ? parts[2] : "";
  }
}
