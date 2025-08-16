package app.crushlog.cldf.tool.config;

import jakarta.inject.Singleton;

import app.crushlog.cldf.qr.QRScanner;
import app.crushlog.cldf.qr.impl.DefaultQRScanner;
import io.micronaut.context.annotation.Factory;

/** Factory to provide QRScanner implementation from cldf-java module. */
@Factory
public class QRScannerFactory {

  @Singleton
  public QRScanner qrScanner() {
    return new DefaultQRScanner();
  }
}
