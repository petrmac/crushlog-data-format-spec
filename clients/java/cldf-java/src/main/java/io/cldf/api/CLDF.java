package io.cldf.api;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Main entry point for CLDF operations. Provides convenient static methods for reading and writing
 * CLDF archives.
 */
public class CLDF {

  private CLDF() {
    // Utility class
  }

  /**
   * Read a CLDF archive from a file.
   *
   * @param file The CLDF file to read
   * @return The parsed CLDF archive
   * @throws IOException if an I/O error occurs
   */
  public static CLDFArchive read(File file) throws IOException {
    CLDFReader reader = new CLDFReader();
    return reader.read(file);
  }

  /**
   * Read a CLDF archive from an input stream.
   *
   * @param inputStream The input stream containing CLDF data
   * @return The parsed CLDF archive
   * @throws IOException if an I/O error occurs
   */
  public static CLDFArchive read(InputStream inputStream) throws IOException {
    CLDFReader reader = new CLDFReader();
    return reader.read(inputStream);
  }

  /**
   * Write a CLDF archive to a file.
   *
   * @param archive The CLDF archive to write
   * @param file The file to write to
   * @throws IOException if an I/O error occurs
   */
  public static void write(CLDFArchive archive, File file) throws IOException {
    CLDFWriter writer = new CLDFWriter();
    writer.write(archive, file);
  }

  /**
   * Write a CLDF archive to an output stream.
   *
   * @param archive The CLDF archive to write
   * @param outputStream The output stream to write to
   * @throws IOException if an I/O error occurs
   */
  public static void write(CLDFArchive archive, OutputStream outputStream) throws IOException {
    CLDFWriter writer = new CLDFWriter();
    writer.write(archive, outputStream);
  }

  /**
   * Create a new CLDFReader with custom settings.
   *
   * @param validateChecksums Whether to validate file checksums
   * @param validateSchemas Whether to validate against JSON schemas
   * @return A configured CLDFReader instance
   */
  public static CLDFReader createReader(boolean validateChecksums, boolean validateSchemas) {
    return new CLDFReader(validateChecksums, validateSchemas);
  }

  /**
   * Create a new CLDFWriter with custom settings.
   *
   * @param prettyPrint Whether to format JSON output
   * @return A configured CLDFWriter instance with schema validation enabled
   */
  public static CLDFWriter createWriter(boolean prettyPrint) {
    return new CLDFWriter(prettyPrint);
  }

  /**
   * Create a new CLDFWriter with custom settings.
   *
   * @param prettyPrint Whether to format JSON output
   * @param validateSchemas Whether to validate against JSON schemas before writing
   * @return A configured CLDFWriter instance
   */
  public static CLDFWriter createWriter(boolean prettyPrint, boolean validateSchemas) {
    return new CLDFWriter(prettyPrint, validateSchemas);
  }
}
