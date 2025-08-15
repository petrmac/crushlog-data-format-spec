package app.crushlog.cldf.qr.impl;

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
  public byte[] generatePNG(Route route, QROptions options, QRImageOptions imageOptions) {
    QRCodeData data = generateData(route, options);
    String payload = getPayloadString(data, options);
    return imageGenerator.generatePNG(payload, imageOptions);
  }

  @Override
  public byte[] generatePNG(Location location, QROptions options, QRImageOptions imageOptions) {
    QRCodeData data = generateData(location, options);
    String payload = getPayloadString(data, options);
    return imageGenerator.generatePNG(payload, imageOptions);
  }

  @Override
  public byte[] generatePNG(String data, QRImageOptions imageOptions) {
    return imageGenerator.generatePNG(data, imageOptions);
  }

  @Override
  public String generateSVG(String data, QRImageOptions imageOptions) {
    return imageGenerator.generateSVG(data, imageOptions);
  }
}
