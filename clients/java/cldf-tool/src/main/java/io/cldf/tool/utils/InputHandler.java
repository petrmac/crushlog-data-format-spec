package io.cldf.tool.utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InputHandler {

  private final ObjectMapper jsonMapper;

  public InputHandler() {
    this.jsonMapper = JsonUtils.createPrettyMapper();
  }

  /**
   * Read JSON from stdin
   *
   * @param clazz The class type to deserialize to
   * @return The deserialized object
   */
  public <T> T readJsonFromStdin(Class<T> clazz) throws IOException {
    return readJson(System.in, clazz);
  }

  /**
   * Read JSON from a file or stdin if filename is "-"
   *
   * @param filename The filename or "-" for stdin
   * @param clazz The class type to deserialize to
   * @return The deserialized object
   */
  public <T> T readJson(String filename, Class<T> clazz) throws IOException {
    if ("-".equals(filename)) {
      return readJsonFromStdin(clazz);
    } else {
      return readJsonFromFile(new File(filename), clazz);
    }
  }

  /**
   * Read JSON from a file
   *
   * @param file The file to read from
   * @param clazz The class type to deserialize to
   * @return The deserialized object
   */
  public <T> T readJsonFromFile(File file, Class<T> clazz) throws IOException {
    try (InputStream is = new FileInputStream(file)) {
      return readJson(is, clazz);
    }
  }

  /**
   * Read JSON from an input stream
   *
   * @param inputStream The input stream to read from
   * @param clazz The class type to deserialize to
   * @return The deserialized object
   */
  public <T> T readJson(InputStream inputStream, Class<T> clazz) throws IOException {
    return jsonMapper.readValue(inputStream, clazz);
  }

  /**
   * Read all text from stdin
   *
   * @return The text content
   */
  public String readTextFromStdin() throws IOException {
    return readText(System.in);
  }

  /**
   * Read all text from an input stream
   *
   * @param inputStream The input stream to read from
   * @return The text content
   */
  public String readText(InputStream inputStream) throws IOException {
    try (Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8)) {
      scanner.useDelimiter("\\A");
      return scanner.hasNext() ? scanner.next() : "";
    }
  }

  /**
   * Check if stdin has data available
   *
   * @return true if stdin has data
   */
  public boolean hasStdinData() {
    try {
      return System.in.available() > 0;
    } catch (IOException e) {
      log.debug("Could not check stdin availability", e);
      return false;
    }
  }
}
