package app.crushlog.cldf.qr.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.InflaterInputStream;

import com.google.zxing.LuminanceSource;
import com.google.zxing.RGBLuminanceSource;

/**
 * Pure Java image reader that extracts pixel data from PNG without using AWT. This enables QR code
 * scanning in GraalVM native image.
 */
public class PureJavaImageReader {

  private static final byte[] PNG_SIGNATURE = {
    (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
  };

  /**
   * Create a LuminanceSource from PNG bytes without using AWT. This reads the PNG structure
   * directly and extracts pixel data.
   */
  public LuminanceSource createLuminanceSource(byte[] pngBytes) throws IOException {
    if (!isPNG(pngBytes)) {
      throw new IOException("Not a valid PNG file");
    }

    ByteArrayInputStream bais = new ByteArrayInputStream(pngBytes);

    // Skip PNG signature
    bais.skip(8);

    int width = 0;
    int height = 0;
    int bitDepth = 0;
    int colorType = 0;
    List<byte[]> imageDataChunks = new ArrayList<>();

    // Read chunks
    while (bais.available() > 0) {
      // Read chunk length
      int chunkLength = readInt(bais);

      if (chunkLength < 0 || bais.available() < chunkLength + 8) {
        // Invalid chunk length or not enough data
        break;
      }

      // Read chunk type
      byte[] chunkTypeBytes = new byte[4];
      bais.read(chunkTypeBytes);
      String chunkType = new String(chunkTypeBytes, "ASCII");

      if ("IHDR".equals(chunkType)) {
        // Image header (13 bytes total)
        width = readInt(bais);
        height = readInt(bais);
        bitDepth = bais.read();
        colorType = bais.read();
        // Skip compression method, filter method, interlace method (3 bytes)
        bais.skip(3);
      } else if ("IDAT".equals(chunkType)) {
        // Image data (compressed) - there can be multiple IDAT chunks
        byte[] chunkData = new byte[chunkLength];
        bais.read(chunkData);
        imageDataChunks.add(chunkData);
      } else if ("IEND".equals(chunkType)) {
        // End of PNG
        break;
      } else {
        // Skip other chunks
        bais.skip(chunkLength);
      }

      // Skip CRC
      bais.skip(4);
    }

    if (width == 0 || height == 0 || imageDataChunks.isEmpty()) {
      throw new IOException("Invalid PNG structure");
    }

    // Combine all IDAT chunks
    ByteArrayOutputStream combined = new ByteArrayOutputStream();
    for (byte[] chunk : imageDataChunks) {
      combined.write(chunk);
    }
    byte[] compressedData = combined.toByteArray();

    // Decompress and process the image data
    byte[] decompressedData = decompress(compressedData);

    // Process the decompressed data to extract pixels
    int[] pixels = processImageData(decompressedData, width, height, colorType, bitDepth);

    return new RGBLuminanceSource(width, height, pixels);
  }

  /** Decompress PNG data using DEFLATE algorithm. */
  private byte[] decompress(byte[] compressedData) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (InflaterInputStream iis =
        new InflaterInputStream(new ByteArrayInputStream(compressedData))) {
      byte[] buffer = new byte[1024];
      int len;
      while ((len = iis.read(buffer)) > 0) {
        baos.write(buffer, 0, len);
      }
    }
    return baos.toByteArray();
  }

  /**
   * Process decompressed PNG data to extract pixel values. Handles different color types and
   * applies PNG filters.
   */
  private int[] processImageData(byte[] data, int width, int height, int colorType, int bitDepth)
      throws IOException {
    int[] pixels = new int[width * height];

    // Calculate bytes per pixel based on color type
    int bytesPerPixel = getBytesPerPixel(colorType, bitDepth);
    int bytesPerScanline = width * bytesPerPixel + 1; // +1 for filter byte

    // Check if we have enough data (with some tolerance for compression)
    int expectedSize = height * bytesPerScanline;
    if (data.length < expectedSize - height) { // Allow for some compression
      // Try to work with what we have
      int actualHeight = data.length / bytesPerScanline;
      if (actualHeight < 1) {
        throw new IOException(
            "Insufficient data for image dimensions: expected "
                + expectedSize
                + ", got "
                + data.length);
      }
      // Adjust height to what we can actually process
      height = actualHeight;
      pixels = new int[width * height];
    }

    // Process each scanline
    byte[] currentScanline = new byte[width * bytesPerPixel];
    byte[] previousScanline = new byte[width * bytesPerPixel];

    for (int y = 0; y < height; y++) {
      int offset = y * bytesPerScanline;
      int filterType = data[offset] & 0xFF;

      // Copy raw scanline data
      System.arraycopy(data, offset + 1, currentScanline, 0, width * bytesPerPixel);

      // Apply PNG filter
      applyFilter(currentScanline, previousScanline, filterType, bytesPerPixel);

      // Convert to RGB pixels
      for (int x = 0; x < width; x++) {
        pixels[y * width + x] = getPixel(currentScanline, x * bytesPerPixel, colorType, bitDepth);
      }

      // Save current scanline as previous for next iteration
      System.arraycopy(currentScanline, 0, previousScanline, 0, currentScanline.length);
    }

    return pixels;
  }

  /** Apply PNG filter to scanline data. */
  private void applyFilter(byte[] current, byte[] previous, int filterType, int bytesPerPixel)
      throws IOException {
    switch (filterType) {
      case 0: // None
        break;
      case 1: // Sub
        for (int i = bytesPerPixel; i < current.length; i++) {
          current[i] = (byte) ((current[i] + current[i - bytesPerPixel]) & 0xFF);
        }
        break;
      case 2: // Up
        for (int i = 0; i < current.length; i++) {
          current[i] = (byte) ((current[i] + previous[i]) & 0xFF);
        }
        break;
      case 3: // Average
        for (int i = 0; i < current.length; i++) {
          int left = (i >= bytesPerPixel) ? (current[i - bytesPerPixel] & 0xFF) : 0;
          int up = previous[i] & 0xFF;
          current[i] = (byte) ((current[i] + (left + up) / 2) & 0xFF);
        }
        break;
      case 4: // Paeth
        for (int i = 0; i < current.length; i++) {
          int left = (i >= bytesPerPixel) ? (current[i - bytesPerPixel] & 0xFF) : 0;
          int up = previous[i] & 0xFF;
          int upLeft = (i >= bytesPerPixel) ? (previous[i - bytesPerPixel] & 0xFF) : 0;
          current[i] = (byte) ((current[i] + paethPredictor(left, up, upLeft)) & 0xFF);
        }
        break;
      default:
        throw new IOException("Unknown PNG filter type: " + filterType);
    }
  }

  /** Paeth predictor function for filter type 4. */
  private int paethPredictor(int a, int b, int c) {
    int p = a + b - c;
    int pa = Math.abs(p - a);
    int pb = Math.abs(p - b);
    int pc = Math.abs(p - c);
    if (pa <= pb && pa <= pc) return a;
    else if (pb <= pc) return b;
    else return c;
  }

  /** Get bytes per pixel based on color type and bit depth. */
  private int getBytesPerPixel(int colorType, int bitDepth) {
    switch (colorType) {
      case 0: // Grayscale
        return bitDepth / 8;
      case 2: // RGB
        return 3 * (bitDepth / 8);
      case 3: // Palette
        return 1;
      case 4: // Grayscale with alpha
        return 2 * (bitDepth / 8);
      case 6: // RGBA
        return 4 * (bitDepth / 8);
      default:
        return 1;
    }
  }

  /** Convert bytes to RGB pixel value based on color type. */
  private int getPixel(byte[] data, int offset, int colorType, int bitDepth) {
    switch (colorType) {
      case 0: // Grayscale
        int gray = data[offset] & 0xFF;
        return (0xFF << 24) | (gray << 16) | (gray << 8) | gray;
      case 2: // RGB
        int r = data[offset] & 0xFF;
        int g = data[offset + 1] & 0xFF;
        int b = data[offset + 2] & 0xFF;
        return (0xFF << 24) | (r << 16) | (g << 8) | b;
      case 3: // Palette - simplified, would need palette data
        int paletteIndex = data[offset] & 0xFF;
        // For simplicity, create grayscale from palette index
        return (0xFF << 24) | (paletteIndex << 16) | (paletteIndex << 8) | paletteIndex;
      case 4: // Grayscale with alpha
        int grayA = data[offset] & 0xFF;
        int alpha = data[offset + 1] & 0xFF;
        return (alpha << 24) | (grayA << 16) | (grayA << 8) | grayA;
      case 6: // RGBA
        int rA = data[offset] & 0xFF;
        int gA = data[offset + 1] & 0xFF;
        int bA = data[offset + 2] & 0xFF;
        int a = data[offset + 3] & 0xFF;
        return (a << 24) | (rA << 16) | (gA << 8) | bA;
      default:
        return 0xFF000000; // Opaque black
    }
  }

  private boolean isPNG(byte[] data) {
    if (data.length < 8) return false;
    for (int i = 0; i < 8; i++) {
      if (data[i] != PNG_SIGNATURE[i]) return false;
    }
    return true;
  }

  private int readInt(ByteArrayInputStream bais) throws IOException {
    int ch1 = bais.read();
    int ch2 = bais.read();
    int ch3 = bais.read();
    int ch4 = bais.read();
    if ((ch1 | ch2 | ch3 | ch4) < 0) {
      throw new IOException("Unexpected end of stream");
    }
    return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + ch4);
  }
}
