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
    Map<String, byte[]> fileContents = new HashMap<>();
    Map<String, String> actualChecksums = new HashMap<>();

    try (ZipArchiveInputStream zis = new ZipArchiveInputStream(inputStream)) {
      ZipArchiveEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        if (!entry.isDirectory()) {
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          byte[] buffer = new byte[8192];
          int len;
          while ((len = zis.read(buffer)) > 0) {
            baos.write(buffer, 0, len);
          }
          byte[] content = baos.toByteArray();
          fileContents.put(entry.getName(), content);

          if (validateChecksums && !entry.getName().equals(CHECKSUMS_FILE)) {
            actualChecksums.put(entry.getName(), calculateSHA256(content));
          }
        }
      }
    }

    // Check required files
    if (!fileContents.containsKey(MANIFEST_FILE)) {
      throw new IOException("Missing required file: " + MANIFEST_FILE);
    }
    if (!fileContents.containsKey(CHECKSUMS_FILE)) {
      throw new IOException("Missing required file: " + CHECKSUMS_FILE);
    }

    // Validate schemas if enabled
    if (validateSchemas) {
      // Validate all required files first
      for (String filename : new String[] {MANIFEST_FILE, LOCATIONS_FILE, CHECKSUMS_FILE}) {
        ValidationResult result =
            schemaValidator.validateWithResult(filename, fileContents.get(filename));
        if (!result.valid()) {
          StringBuilder errorMessage = new StringBuilder();
          errorMessage.append("Schema validation failed for ").append(filename).append(":\n");
          for (ValidationResult.ValidationError error : result.errors()) {
            errorMessage.append("  - ").append(error.message()).append("\n");
          }
          throw new IOException(errorMessage.toString());
        }
      }

      // Validate optional files if present
      for (String filename :
          new String[] {
            CLIMBS_FILE, SESSIONS_FILE, ROUTES_FILE, SECTORS_FILE, TAGS_FILE, MEDIA_METADATA_FILE
          }) {
        if (fileContents.containsKey(filename)) {
          ValidationResult result =
              schemaValidator.validateWithResult(filename, fileContents.get(filename));
          if (!result.valid()) {
            StringBuilder errorMessage = new StringBuilder();
            errorMessage.append("Schema validation failed for ").append(filename).append(":\n");
            for (ValidationResult.ValidationError error : result.errors()) {
              errorMessage.append("  - ").append(error.message()).append("\n");
            }
            throw new IOException(errorMessage.toString());
          }
        }
      }
    }

    // Parse files
    CLDFArchive archive = new CLDFArchive();

    // Parse manifest
    Manifest manifest = parseJson(fileContents.get(MANIFEST_FILE), Manifest.class);
    archive.setManifest(manifest);

    // Validate CLDF format
    if (!"CLDF".equals(manifest.getFormat())) {
      throw new IOException("Invalid format. Expected 'CLDF', got: " + manifest.getFormat());
    }

    // Parse checksums and validate if enabled
    Checksums checksums = parseJson(fileContents.get(CHECKSUMS_FILE), Checksums.class);
    archive.setChecksums(checksums);

    if (validateChecksums) {
      validateChecksums(checksums, actualChecksums);
    }

    // Parse required files
    LocationsFile locationsFile = parseJson(fileContents.get(LOCATIONS_FILE), LocationsFile.class);
    archive.setLocations(locationsFile.getLocations());

    // Parse optional climbs and sessions files
    if (fileContents.containsKey(CLIMBS_FILE)) {
      ClimbsFile climbsFile = parseJson(fileContents.get(CLIMBS_FILE), ClimbsFile.class);
      archive.setClimbs(climbsFile.getClimbs());
    }

    if (fileContents.containsKey(SESSIONS_FILE)) {
      SessionsFile sessionsFile = parseJson(fileContents.get(SESSIONS_FILE), SessionsFile.class);
      archive.setSessions(sessionsFile.getSessions());
    }

    // Parse optional files
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

    if (fileContents.containsKey(MEDIA_METADATA_FILE)) {
      MediaMetadataFile mediaFile =
          parseJson(fileContents.get(MEDIA_METADATA_FILE), MediaMetadataFile.class);
      archive.setMediaItems(mediaFile.getMedia());
    }

    // Store raw media files if present
    Map<String, byte[]> mediaFiles = new HashMap<>();
    for (Map.Entry<String, byte[]> entry : fileContents.entrySet()) {
      if (entry.getKey().startsWith("media/")) {
        mediaFiles.put(entry.getKey(), entry.getValue());
      }
    }
    if (!mediaFiles.isEmpty()) {
      archive.setMediaFiles(mediaFiles);
    }

    return archive;
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
