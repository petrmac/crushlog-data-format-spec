package app.crushlog.cldf.qr.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

import com.google.zxing.common.BitMatrix;

/**
 * Pure Java PNG generator that works with GraalVM native image. This implementation doesn't use AWT
 * and generates PNG bytes directly.
 */
public class PureJavaPNGGenerator {

  private static final int PNG_HEADER_SIZE = 8;
  private static final byte[] PNG_SIGNATURE = {
    (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
  };

  /**
   * Generate a PNG image from a BitMatrix without using AWT.
   *
   * @param matrix The QR code bit matrix
   * @param size The desired image size in pixels
   * @param foregroundColor RGB color for the QR code (0x000000 for black)
   * @param backgroundColor RGB color for the background (0xFFFFFF for white)
   * @return PNG image as byte array
   */
  public byte[] generatePNG(BitMatrix matrix, int size, int foregroundColor, int backgroundColor)
      throws IOException {
    int width = matrix.getWidth();
    int height = matrix.getHeight();

    // For QR codes, we want a square output at the exact requested size
    int outputWidth = size;
    int outputHeight = size;

    // Calculate scale as a float for accurate scaling
    float scale = (float) size / Math.max(width, height);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    // Write PNG signature
    baos.write(PNG_SIGNATURE);

    // Write IHDR chunk
    writeIHDRChunk(baos, outputWidth, outputHeight);

    // Write IDAT chunk (image data)
    writeIDATChunk(baos, matrix, outputWidth, outputHeight, foregroundColor, backgroundColor);

    // Write IEND chunk
    writeIENDChunk(baos);

    return baos.toByteArray();
  }

  private void writeIHDRChunk(ByteArrayOutputStream baos, int width, int height)
      throws IOException {
    ByteArrayOutputStream data = new ByteArrayOutputStream();

    // Width (4 bytes)
    writeInt(data, width);
    // Height (4 bytes)
    writeInt(data, height);
    // Bit depth (1 byte) - 8 bits per channel
    data.write(8);
    // Color type (1 byte) - 2 = RGB
    data.write(2);
    // Compression method (1 byte) - 0 = deflate
    data.write(0);
    // Filter method (1 byte) - 0 = adaptive filtering
    data.write(0);
    // Interlace method (1 byte) - 0 = no interlace
    data.write(0);

    writeChunk(baos, "IHDR", data.toByteArray());
  }

  private void writeIDATChunk(
      ByteArrayOutputStream baos,
      BitMatrix matrix,
      int outputWidth,
      int outputHeight,
      int foregroundColor,
      int backgroundColor)
      throws IOException {
    int matrixWidth = matrix.getWidth();
    int matrixHeight = matrix.getHeight();

    // Calculate scale factors
    float scaleX = (float) outputWidth / matrixWidth;
    float scaleY = (float) outputHeight / matrixHeight;

    // Extract RGB components
    int fgRed = (foregroundColor >> 16) & 0xFF;
    int fgGreen = (foregroundColor >> 8) & 0xFF;
    int fgBlue = foregroundColor & 0xFF;

    int bgRed = (backgroundColor >> 16) & 0xFF;
    int bgGreen = (backgroundColor >> 8) & 0xFF;
    int bgBlue = backgroundColor & 0xFF;

    // Create uncompressed image data
    ByteArrayOutputStream imageData = new ByteArrayOutputStream();

    for (int y = 0; y < outputHeight; y++) {
      // Filter type byte for each scanline (0 = None)
      imageData.write(0);

      int matrixY = (int) (y / scaleY);
      for (int x = 0; x < outputWidth; x++) {
        int matrixX = (int) (x / scaleX);
        // Check bounds and get matrix value
        boolean isBlack =
            matrixX < matrixWidth && matrixY < matrixHeight && matrix.get(matrixX, matrixY);

        if (isBlack) {
          imageData.write(fgRed);
          imageData.write(fgGreen);
          imageData.write(fgBlue);
        } else {
          imageData.write(bgRed);
          imageData.write(bgGreen);
          imageData.write(bgBlue);
        }
      }
    }

    // Compress the image data
    byte[] uncompressed = imageData.toByteArray();
    byte[] compressed = compress(uncompressed);

    writeChunk(baos, "IDAT", compressed);
  }

  private void writeIENDChunk(ByteArrayOutputStream baos) throws IOException {
    writeChunk(baos, "IEND", new byte[0]);
  }

  private void writeChunk(ByteArrayOutputStream baos, String type, byte[] data) throws IOException {
    // Length (4 bytes)
    writeInt(baos, data.length);

    // Type (4 bytes)
    baos.write(type.getBytes("ASCII"));

    // Data
    baos.write(data);

    // CRC (4 bytes)
    CRC32 crc = new CRC32();
    crc.update(type.getBytes("ASCII"));
    crc.update(data);
    writeInt(baos, (int) crc.getValue());
  }

  private void writeInt(ByteArrayOutputStream baos, int value) throws IOException {
    baos.write((value >> 24) & 0xFF);
    baos.write((value >> 16) & 0xFF);
    baos.write((value >> 8) & 0xFF);
    baos.write(value & 0xFF);
  }

  private byte[] compress(byte[] data) {
    Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION);
    deflater.setInput(data);
    deflater.finish();

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] buffer = new byte[1024];

    while (!deflater.finished()) {
      int count = deflater.deflate(buffer);
      baos.write(buffer, 0, count);
    }

    deflater.end();
    return baos.toByteArray();
  }
}
