package app.crushlog.cldf.api;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import app.crushlog.cldf.models.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;

/**
 * Reads CLDF (CrushLog Data Format) archives from ZIP files. Supports validation of checksums and
 * JSON schemas.
 */
@Slf4j
public class CLDFReader {

  private static final String MANIFEST_FILE = "manifest.json";
  private static final String LOCATIONS_FILE = "locations.json";
  private static final String CLIMBS_FILE = "climbs.json";
  private static final String SESSIONS_FILE = "sessions.json";
  private static final String CHECKSUMS_FILE = "checksums.json";
  private static final String ROUTES_FILE = "routes.json";
  private static final String SECTORS_FILE = "sectors.json";
  private static final String TAGS_FILE = "tags.json";
  private static final String MEDIA_METADATA_FILE = "media-metadata.json";
  private static final String ALGORITHM = "SHA-256";

  private final ObjectMapper objectMapper;
  private final boolean validateChecksums;
  private final boolean validateSchemas;
  private final SchemaValidator schemaValidator;

  /** Creates a CLDFReader with default settings (checksum and schema validation enabled). */
  public CLDFReader() {
    this(true, true);
  }

  /**
   * Creates a CLDFReader with specified validation settings.
   *
   * @param validateChecksums whether to validate file checksums
   * @param validateSchemas whether to validate JSON schemas
   */
  public CLDFReader(boolean validateChecksums, boolean validateSchemas) {
    this.validateChecksums = validateChecksums;
    this.validateSchemas = validateSchemas;
    this.schemaValidator = validateSchemas ? new SchemaValidator() : null;
    this.objectMapper = new ObjectMapper();
    this.objectMapper.registerModule(new JavaTimeModule());
  }

  /**
   * Reads a CLDF archive from a file.
   *
   * @param file the CLDF archive file
   * @return the parsed CLDFArchive
   * @throws IOException if an I/O error occurs
   */
  public CLDFArchive read(File file) throws IOException {
    if (!file.exists()) {
      throw new FileNotFoundException("CLDF file not found: " + file.getAbsolutePath());
    }

    try (FileInputStream fis = new FileInputStream(file)) {
      return read(fis);
    }
  }

  /**
   * Reads a CLDF archive from an input stream.
   *
   * @param inputStream the input stream containing the CLDF archive
   * @return the parsed CLDFArchive
   * @throws IOException if an I/O error occurs
   */
  public CLDFArchive read(InputStream inputStream) throws IOException {
    Map<String, byte[]> fileContents = extractZipContents(inputStream);
    validateRequiredFiles(fileContents);

    if (validateSchemas) {
      validateAllSchemas(fileContents);
    }

    return buildArchive(fileContents);
  }

  /**
   * Extracts all files from the ZIP archive into a map.
   *
   * @param inputStream the input stream containing the ZIP archive
   * @return map of file names to their byte content
   * @throws IOException if an I/O error occurs
   */
  private Map<String, byte[]> extractZipContents(InputStream inputStream) throws IOException {
    Map<String, byte[]> fileContents = new HashMap<>();

    try (ZipArchiveInputStream zis = new ZipArchiveInputStream(inputStream)) {
      ZipArchiveEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        if (!entry.isDirectory()) {
          byte[] content = readEntryContent(zis);
          fileContents.put(entry.getName(), content);
        }
      }
    }

    return fileContents;
  }

  /**
   * Reads the content of a ZIP entry.
   *
   * @param zis the ZIP archive input stream
   * @return the content as byte array
   * @throws IOException if an I/O error occurs
   */
  private byte[] readEntryContent(ZipArchiveInputStream zis) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] buffer = new byte[8192];
    int len;
    while ((len = zis.read(buffer)) > 0) {
      baos.write(buffer, 0, len);
    }
    return baos.toByteArray();
  }

  /**
   * Validates that required files are present in the archive.
   *
   * @param fileContents map of file names to their content
   * @throws IOException if required files are missing
   */
  private void validateRequiredFiles(Map<String, byte[]> fileContents) throws IOException {
    if (!fileContents.containsKey(MANIFEST_FILE)) {
      throw new IOException("Missing required file: " + MANIFEST_FILE);
    }
    if (!fileContents.containsKey(CHECKSUMS_FILE)) {
      throw new IOException("Missing required file: " + CHECKSUMS_FILE);
    }
  }

  /**
   * Validates all schemas for files in the archive.
   *
   * @param fileContents map of file names to their content
   * @throws IOException if schema validation fails
   */
  private void validateAllSchemas(Map<String, byte[]> fileContents) throws IOException {
    String[] requiredFiles = {MANIFEST_FILE, LOCATIONS_FILE, CHECKSUMS_FILE};
    String[] optionalFiles = {
      CLIMBS_FILE, SESSIONS_FILE, ROUTES_FILE, SECTORS_FILE, TAGS_FILE, MEDIA_METADATA_FILE
    };

    validateSchemaForFiles(fileContents, requiredFiles, true);
    validateSchemaForFiles(fileContents, optionalFiles, false);
  }

  /**
   * Validates schemas for a set of files.
   *
   * @param fileContents map of file names to their content
   * @param filenames array of file names to validate
   * @param required whether the files are required to exist
   * @throws IOException if schema validation fails
   */
  private void validateSchemaForFiles(
      Map<String, byte[]> fileContents, String[] filenames, boolean required) throws IOException {
    for (String filename : filenames) {
      if (fileContents.containsKey(filename) || required) {
        validateSingleSchema(filename, fileContents.get(filename));
      }
    }
  }

  /**
   * Validates the schema for a single file.
   *
   * @param filename the name of the file
   * @param content the file content
   * @throws IOException if schema validation fails
   */
  private void validateSingleSchema(String filename, byte[] content) throws IOException {
    ValidationResult result = schemaValidator.validateWithResult(filename, content);
    if (!result.valid()) {
      StringBuilder errorMessage = new StringBuilder();
      errorMessage.append("Schema validation failed for ").append(filename).append(":\n");
      for (ValidationResult.ValidationError error : result.errors()) {
        errorMessage.append("  - ").append(error.message()).append("\n");
      }
      throw new IOException(errorMessage.toString());
    }
  }

  /**
   * Builds the CLDFArchive from the extracted file contents.
   *
   * @param fileContents map of file names to their content
   * @return the constructed CLDFArchive
   * @throws IOException if parsing or validation fails
   */
  private CLDFArchive buildArchive(Map<String, byte[]> fileContents) throws IOException {
    CLDFArchive archive = new CLDFArchive();

    parseAndSetManifest(archive, fileContents);
    parseAndValidateChecksums(archive, fileContents);
    parseRequiredFiles(archive, fileContents);
    parseOptionalFiles(archive, fileContents);
    extractMediaFiles(archive, fileContents);

    return archive;
  }

  /**
   * Parses and sets the manifest in the archive.
   *
   * @param archive the archive to populate
   * @param fileContents map of file names to their content
   * @throws IOException if parsing or validation fails
   */
  private void parseAndSetManifest(CLDFArchive archive, Map<String, byte[]> fileContents)
      throws IOException {
    Manifest manifest = parseJson(fileContents.get(MANIFEST_FILE), Manifest.class);
    archive.setManifest(manifest);

    if (!"CLDF".equals(manifest.getFormat())) {
      throw new IOException("Invalid format. Expected 'CLDF', got: " + manifest.getFormat());
    }
  }

  /**
   * Parses and validates checksums.
   *
   * @param archive the archive to populate
   * @param fileContents map of file names to their content
   * @throws IOException if parsing or validation fails
   */
  private void parseAndValidateChecksums(CLDFArchive archive, Map<String, byte[]> fileContents)
      throws IOException {
    Checksums checksums = parseJson(fileContents.get(CHECKSUMS_FILE), Checksums.class);
    archive.setChecksums(checksums);

    if (validateChecksums) {
      Map<String, String> actualChecksums = calculateActualChecksums(fileContents);
      validateChecksums(checksums, actualChecksums);
    }
  }

  /**
   * Calculates actual checksums for all files except checksums.json.
   *
   * @param fileContents map of file names to their content
   * @return map of file names to their calculated checksums
   * @throws IOException if checksum calculation fails
   */
  private Map<String, String> calculateActualChecksums(Map<String, byte[]> fileContents)
      throws IOException {
    Map<String, String> actualChecksums = new HashMap<>();

    for (Map.Entry<String, byte[]> entry : fileContents.entrySet()) {
      if (!entry.getKey().equals(CHECKSUMS_FILE)) {
        actualChecksums.put(entry.getKey(), calculateSHA256(entry.getValue()));
      }
    }

    return actualChecksums;
  }

  /**
   * Parses required files and populates the archive.
   *
   * @param archive the archive to populate
   * @param fileContents map of file names to their content
   * @throws IOException if parsing fails
   */
  private void parseRequiredFiles(CLDFArchive archive, Map<String, byte[]> fileContents)
      throws IOException {
    LocationsFile locationsFile = parseJson(fileContents.get(LOCATIONS_FILE), LocationsFile.class);
    archive.setLocations(locationsFile.getLocations());
  }

  /**
   * Parses optional files and populates the archive.
   *
   * @param archive the archive to populate
   * @param fileContents map of file names to their content
   * @throws IOException if parsing fails
   */
  private void parseOptionalFiles(CLDFArchive archive, Map<String, byte[]> fileContents)
      throws IOException {
    parseOptionalClimbsAndSessions(archive, fileContents);
    parseOptionalRouteData(archive, fileContents);
    parseOptionalMediaMetadata(archive, fileContents);
  }

  /**
   * Parses optional climbs and sessions files.
   *
   * @param archive the archive to populate
   * @param fileContents map of file names to their content
   * @throws IOException if parsing fails
   */
  private void parseOptionalClimbsAndSessions(CLDFArchive archive, Map<String, byte[]> fileContents)
      throws IOException {
    if (fileContents.containsKey(CLIMBS_FILE)) {
      ClimbsFile climbsFile = parseJson(fileContents.get(CLIMBS_FILE), ClimbsFile.class);
      archive.setClimbs(climbsFile.getClimbs());
    }

    if (fileContents.containsKey(SESSIONS_FILE)) {
      SessionsFile sessionsFile = parseJson(fileContents.get(SESSIONS_FILE), SessionsFile.class);
      archive.setSessions(sessionsFile.getSessions());
    }
  }

  /**
   * Parses optional route-related files.
   *
   * @param archive the archive to populate
   * @param fileContents map of file names to their content
   * @throws IOException if parsing fails
   */
  private void parseOptionalRouteData(CLDFArchive archive, Map<String, byte[]> fileContents)
      throws IOException {
    if (fileContents.containsKey(ROUTES_FILE)) {
      RoutesFile routesFile = parseJson(fileContents.get(ROUTES_FILE), RoutesFile.class);
      archive.setRoutes(routesFile.getRoutes());
    }

    if (fileContents.containsKey(SECTORS_FILE)) {
      SectorsFile sectorsFile = parseJson(fileContents.get(SECTORS_FILE), SectorsFile.class);
      archive.setSectors(sectorsFile.getSectors());
    }

    if (fileContents.containsKey(TAGS_FILE)) {
      TagsFile tagsFile = parseJson(fileContents.get(TAGS_FILE), TagsFile.class);
      archive.setTags(tagsFile.getTags());
    }
  }

  /**
   * Parses optional media metadata file.
   *
   * @param archive the archive to populate
   * @param fileContents map of file names to their content
   * @throws IOException if parsing fails
   */
  private void parseOptionalMediaMetadata(CLDFArchive archive, Map<String, byte[]> fileContents)
      throws IOException {
    if (fileContents.containsKey(MEDIA_METADATA_FILE)) {
      MediaMetadataFile mediaFile =
          parseJson(fileContents.get(MEDIA_METADATA_FILE), MediaMetadataFile.class);
      archive.setMediaItems(mediaFile.getMedia());
    }
  }

  /**
   * Extracts raw media files from the archive.
   *
   * @param archive the archive to populate
   * @param fileContents map of file names to their content
   */
  private void extractMediaFiles(CLDFArchive archive, Map<String, byte[]> fileContents) {
    Map<String, byte[]> mediaFiles = new HashMap<>();
    for (Map.Entry<String, byte[]> entry : fileContents.entrySet()) {
      if (entry.getKey().startsWith("media/")) {
        mediaFiles.put(entry.getKey(), entry.getValue());
      }
    }
    if (!mediaFiles.isEmpty()) {
      archive.setMediaFiles(mediaFiles);
    }
  }

  private <T> T parseJson(byte[] content, Class<T> clazz) throws IOException {
    return objectMapper.readValue(content, clazz);
  }

  private void validateChecksums(Checksums checksums, Map<String, String> actualChecksums)
      throws IOException {
    if (!ALGORITHM.equals(checksums.getAlgorithm())) {
      throw new IOException("Unsupported checksum algorithm: " + checksums.getAlgorithm());
    }

    for (Map.Entry<String, String> entry : checksums.getFiles().entrySet()) {
      String filename = entry.getKey();
      String expectedChecksum = entry.getValue();
      String actualChecksum = actualChecksums.get(filename);

      if (actualChecksum == null) {
        log.warn("File referenced in checksums but not found in archive: {}", filename);
      } else if (!expectedChecksum.equals(actualChecksum)) {
        throw new IOException(
            String.format(
                "Checksum mismatch for file '%s'. Expected: %s, Actual: %s",
                filename, expectedChecksum, actualChecksum));
      }
    }
  }

  private String calculateSHA256(byte[] data) throws IOException {
    try {
      MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
      byte[] hash = digest.digest(data);
      StringBuilder hexString = new StringBuilder();
      for (byte b : hash) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) {
          hexString.append('0');
        }
        hexString.append(hex);
      }
      return hexString.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new IOException("SHA-256 algorithm not available", e);
    }
  }
}
