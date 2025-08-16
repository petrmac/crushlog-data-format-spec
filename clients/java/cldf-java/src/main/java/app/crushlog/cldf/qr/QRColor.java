package app.crushlog.cldf.qr;

/**
 * Simple color representation without AWT dependency. This allows QR code generation in native
 * images.
 */
public class QRColor {
  private final int rgb;

  /** Predefined black color */
  public static final QRColor BLACK = new QRColor(0x000000);

  /** Predefined white color */
  public static final QRColor WHITE = new QRColor(0xFFFFFF);

  /**
   * Create a color from RGB values.
   *
   * @param r Red component (0-255)
   * @param g Green component (0-255)
   * @param b Blue component (0-255)
   */
  public QRColor(int r, int g, int b) {
    this.rgb = ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
  }

  /**
   * Create a color from RGB int value.
   *
   * @param rgb RGB value as integer (0xRRGGBB)
   */
  public QRColor(int rgb) {
    this.rgb = rgb & 0xFFFFFF;
  }

  /**
   * Get the RGB value as integer.
   *
   * @return RGB value
   */
  public int getRGB() {
    return rgb | 0xFF000000; // Add alpha channel
  }

  /**
   * Get the red component.
   *
   * @return Red value (0-255)
   */
  public int getRed() {
    return (rgb >> 16) & 0xFF;
  }

  /**
   * Get the green component.
   *
   * @return Green value (0-255)
   */
  public int getGreen() {
    return (rgb >> 8) & 0xFF;
  }

  /**
   * Get the blue component.
   *
   * @return Blue value (0-255)
   */
  public int getBlue() {
    return rgb & 0xFF;
  }

  /**
   * Convert to hex string.
   *
   * @return Hex color string (e.g., "#FF0000")
   */
  public String toHex() {
    return String.format("#%06X", rgb);
  }

  @Override
  public String toString() {
    return toHex();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    QRColor qrColor = (QRColor) obj;
    return rgb == qrColor.rgb;
  }

  @Override
  public int hashCode() {
    return rgb;
  }
}
