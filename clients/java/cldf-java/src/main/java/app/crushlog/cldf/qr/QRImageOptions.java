package app.crushlog.cldf.qr;

import lombok.Builder;
import lombok.Data;

/** Options for QR code image generation. */
@Data
@Builder
public class QRImageOptions {
  @Builder.Default private int size = 256;

  @Builder.Default private int margin = 4;

  @Builder.Default private ErrorCorrectionLevel errorCorrectionLevel = ErrorCorrectionLevel.M;

  @Builder.Default private QRColor foregroundColor = QRColor.BLACK;

  @Builder.Default private QRColor backgroundColor = QRColor.WHITE;

  private String logoPath;

  @Builder.Default private int logoSize = 60; // Size of logo in pixels

  @Builder.Default private boolean roundedCorners = false;

  @Builder.Default private int cornerRadius = 10;

  /** Error correction level for QR code. */
  public enum ErrorCorrectionLevel {
    /** Low - 7% correction */
    L,
    /** Medium - 15% correction */
    M,
    /** Quartile - 25% correction */
    Q,
    /** High - 30% correction */
    H
  }
}
