package io.cldf.tool.services;

import java.io.File;
import java.io.IOException;

import io.cldf.api.CLDFArchive;

/**
 * Interface for reading and writing CLDF archives. Provides methods to handle CLDF file I/O
 * operations.
 */
public interface CLDFService {

  /**
   * Reads a CLDF archive from a file.
   *
   * @param file the CLDF file to read
   * @return the loaded CLDF archive
   * @throws IOException if reading fails
   */
  CLDFArchive read(File file) throws IOException;

  /**
   * Writes a CLDF archive to a file.
   *
   * @param archive the CLDF archive to write
   * @param file the target file
   * @param prettyPrint whether to format the JSON output
   * @throws IOException if writing fails
   */
  void write(CLDFArchive archive, File file, boolean prettyPrint) throws IOException;
}
