package app.crushlog.cldf.api;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import app.crushlog.cldf.domain.CLIDService;
import app.crushlog.cldf.models.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;

/**
 * Writes CLDF (CrushLog Data Format) archives to ZIP files. Supports pretty printing and automatic
 * checksum generation.
 */
@Slf4j
public class CLDFWriter {

  private static final String MANIFEST_FILE = "manifest.json";
  private static final String LOCATIONS_FILE = "locations.json";
  private static final String CLIMBS_FILE = "climbs.json";
  private static final String SESSIONS_FILE = "sessions.json";
  private static final String CHECKSUMS_FILE = "checksums.json";
  private static final String ROUTES_FILE = "routes.json";
  private static final String SECTORS_FILE = "sectors.json";
  private static final String TAGS_FILE = "tags.json";
  private static final String MEDIA_METADATA_FILE = "media-metadata.json";

  private final ObjectMapper objectMapper;
  private final boolean validateSchemas;
  private final SchemaValidator schemaValidator;
  private final CLIDService clidService;
  private boolean autoGenerateCLIDs = true;
  private boolean validateCLIDs = true;

  /**
   * Creates a CLDFWriter with default settings (pretty printing enabled, schema validation
   * enabled).
   */
  public CLDFWriter() {
    this(true, true);
  }

  /**
   * Creates a CLDFWriter with specified formatting settings.
   *
   * @param prettyPrint whether to enable pretty printing for JSON files
   */
  public CLDFWriter(boolean prettyPrint) {
    this(prettyPrint, true);
  }

  /**
   * Creates a CLDFWriter with specified formatting and validation settings.
   *
   * @param prettyPrint whether to enable pretty printing for JSON files
   * @param validateSchemas whether to validate JSON schemas before writing
   */
  public CLDFWriter(boolean prettyPrint, boolean validateSchemas) {
    this.validateSchemas = validateSchemas;
    this.schemaValidator = validateSchemas ? new SchemaValidator() : null;
    this.clidService = new CLIDService();
    this.objectMapper = new ObjectMapper();
    this.objectMapper.registerModule(new JavaTimeModule());
    this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    this.objectMapper.setSerializationInclusion(
        com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);
    if (prettyPrint) {
      this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }
  }

  /**
   * Sets whether to automatically generate CLIDs for entities that don't have them. Default is
   * true.
   */
  public CLDFWriter withAutoGenerateCLIDs(boolean autoGenerate) {
    this.autoGenerateCLIDs = autoGenerate;
    return this;
  }

  /**
   * Sets whether to validate existing CLIDs for correct format and type matching. Default is true.
   */
  public CLDFWriter withValidateCLIDs(boolean validate) {
    this.validateCLIDs = validate;
    return this;
  }

  /**
   * Writes a CLDF archive to a file.
   *
   * @param archive the CLDFArchive to write
   * @param file the output file
   * @throws IOException if an I/O error occurs
   */
  public void write(CLDFArchive archive, File file) throws IOException {
    try (FileOutputStream fos = new FileOutputStream(file)) {
      write(archive, fos);
    }
  }

  /**
   * Writes a CLDF archive to an output stream.
   *
   * @param archive the CLDFArchive to write
   * @param outputStream the output stream
   * @throws IOException if an I/O error occurs
   */
  public void write(CLDFArchive archive, OutputStream outputStream) throws IOException {
    log.debug(
        "Writing CLDF archive with settings: autoGenerateCLIDs={}, validateCLIDs={}, validateSchemas={}",
        autoGenerateCLIDs,
        validateCLIDs,
        validateSchemas);

    validateArchive(archive);
    processCLIDs(archive);

    Map<String, byte[]> fileContents = new HashMap<>();
    Map<String, String> checksums = new HashMap<>();

    prepareFileContents(archive, fileContents, checksums);
    createChecksumsFile(fileContents, checksums);

    if (validateSchemas) {
      validateAllSchemas(fileContents);
    }

    writeZipArchive(outputStream, fileContents);

    log.info("Successfully wrote CLDF archive with {} files", fileContents.size());
  }

  /**
   * Processes CLIDs for the archive if auto-generation or validation is enabled.
   *
   * @param archive the archive to process
   * @throws IOException if CLID processing fails
   */
  private void processCLIDs(CLDFArchive archive) throws IOException {
    if (autoGenerateCLIDs || validateCLIDs) {
      log.info("Processing CLIDs: autoGenerate={}, validate={}", autoGenerateCLIDs, validateCLIDs);
      try {
        clidService.processArchiveCLIDs(archive, autoGenerateCLIDs, validateCLIDs);
      } catch (CLIDService.CLIDValidationException e) {
        log.error("CLID validation failed: {}", e.getMessage());
        throw new IOException("CLID validation failed: " + e.getMessage(), e);
      }
    }
  }

  /**
   * Prepares all file contents and calculates their checksums.
   *
   * @param archive the archive to serialize
   * @param fileContents map to store file contents
   * @param checksums map to store file checksums
   * @throws IOException if serialization fails
   */
  private void prepareFileContents(
      CLDFArchive archive, Map<String, byte[]> fileContents, Map<String, String> checksums)
      throws IOException {
    prepareManifest(archive, fileContents, checksums);
    prepareCoreFiles(archive, fileContents, checksums);
    prepareOptionalFiles(archive, fileContents, checksums);
    prepareMediaFiles(archive, fileContents, checksums);
  }

  /**
   * Prepares the manifest file.
   *
   * @param archive the archive
   * @param fileContents map to store file contents
   * @param checksums map to store file checksums
   * @throws IOException if serialization fails
   */
  private void prepareManifest(
      CLDFArchive archive, Map<String, byte[]> fileContents, Map<String, String> checksums)
      throws IOException {
    if (archive.getManifest() == null) {
      throw new IllegalArgumentException("Manifest is required");
    }

    if (archive.getManifest().getStats() == null) {
      log.debug("Calculating archive statistics");
      archive.getManifest().setStats(calculateStats(archive));
    }

    byte[] manifestBytes = serializeToJson(archive.getManifest());
    fileContents.put(MANIFEST_FILE, manifestBytes);
    checksums.put(MANIFEST_FILE, calculateSHA256(manifestBytes));
  }

  /**
   * Prepares core data files (locations, climbs, sessions).
   *
   * @param archive the archive
   * @param fileContents map to store file contents
   * @param checksums map to store file checksums
   * @throws IOException if serialization fails
   */
  private void prepareCoreFiles(
      CLDFArchive archive, Map<String, byte[]> fileContents, Map<String, String> checksums)
      throws IOException {
    if (archive.getLocations() != null && !archive.getLocations().isEmpty()) {
      LocationsFile locationsFile =
          LocationsFile.builder().locations(archive.getLocations()).build();
      addFileToContents(LOCATIONS_FILE, locationsFile, fileContents, checksums);
    }

    if (archive.getClimbs() != null && !archive.getClimbs().isEmpty()) {
      ClimbsFile climbsFile = ClimbsFile.builder().climbs(archive.getClimbs()).build();
      addFileToContents(CLIMBS_FILE, climbsFile, fileContents, checksums);
    }

    if (archive.getSessions() != null && !archive.getSessions().isEmpty()) {
      SessionsFile sessionsFile = SessionsFile.builder().sessions(archive.getSessions()).build();
      addFileToContents(SESSIONS_FILE, sessionsFile, fileContents, checksums);
    }
  }

  /**
   * Prepares optional data files (routes, sectors, tags, media metadata).
   *
   * @param archive the archive
   * @param fileContents map to store file contents
   * @param checksums map to store file checksums
   * @throws IOException if serialization fails
   */
  private void prepareOptionalFiles(
      CLDFArchive archive, Map<String, byte[]> fileContents, Map<String, String> checksums)
      throws IOException {
    if (archive.hasRoutes()) {
      RoutesFile routesFile = RoutesFile.builder().routes(archive.getRoutes()).build();
      addFileToContents(ROUTES_FILE, routesFile, fileContents, checksums);
    }

    if (archive.hasSectors()) {
      SectorsFile sectorsFile = SectorsFile.builder().sectors(archive.getSectors()).build();
      addFileToContents(SECTORS_FILE, sectorsFile, fileContents, checksums);
    }

    if (archive.hasTags()) {
      TagsFile tagsFile = TagsFile.builder().tags(archive.getTags()).build();
      addFileToContents(TAGS_FILE, tagsFile, fileContents, checksums);
    }

    if (archive.hasMedia()) {
      MediaMetadataFile mediaFile =
          MediaMetadataFile.builder().media(archive.getMediaItems()).build();
      addFileToContents(MEDIA_METADATA_FILE, mediaFile, fileContents, checksums);
    }
  }

  /**
   * Prepares embedded media files.
   *
   * @param archive the archive
   * @param fileContents map to store file contents
   * @param checksums map to store file checksums
   * @throws IOException if checksum calculation fails
   */
  private void prepareMediaFiles(
      CLDFArchive archive, Map<String, byte[]> fileContents, Map<String, String> checksums)
      throws IOException {
    if (archive.hasEmbeddedMedia()) {
      for (Map.Entry<String, byte[]> entry : archive.getMediaFiles().entrySet()) {
        fileContents.put(entry.getKey(), entry.getValue());
        checksums.put(entry.getKey(), calculateSHA256(entry.getValue()));
      }
    }
  }

  /**
   * Helper method to serialize an object and add it to file contents.
   *
   * @param filename the file name
   * @param object the object to serialize
   * @param fileContents map to store file contents
   * @param checksums map to store file checksums
   * @throws IOException if serialization fails
   */
  private void addFileToContents(
      String filename,
      Object object,
      Map<String, byte[]> fileContents,
      Map<String, String> checksums)
      throws IOException {
    byte[] bytes = serializeToJson(object);
    fileContents.put(filename, bytes);
    checksums.put(filename, calculateSHA256(bytes));
  }

  /**
   * Creates the checksums file.
   *
   * @param fileContents map to store file contents
   * @param checksums map of file checksums
   * @throws IOException if serialization fails
   */
  private void createChecksumsFile(Map<String, byte[]> fileContents, Map<String, String> checksums)
      throws IOException {
    Checksums checksumsObj =
        Checksums.builder()
            .algorithm("SHA-256")
            .files(checksums)
            .generatedAt(OffsetDateTime.now())
            .build();
    byte[] checksumsBytes = serializeToJson(checksumsObj);
    fileContents.put(CHECKSUMS_FILE, checksumsBytes);
  }

  /**
   * Validates all schemas for the prepared files.
   *
   * @param fileContents map of file contents to validate
   * @throws IOException if schema validation fails
   */
  private void validateAllSchemas(Map<String, byte[]> fileContents) throws IOException {
    log.debug("Validating schemas for {} files", fileContents.size());

    for (Map.Entry<String, byte[]> entry : fileContents.entrySet()) {
      if (!entry.getKey().startsWith("media/")) {
        validateSingleFileSchema(entry.getKey(), entry.getValue());
      }
    }

    log.debug("Schema validation passed for all files");
  }

  /**
   * Validates the schema for a single file.
   *
   * @param filename the file name
   * @param content the file content
   * @throws IOException if schema validation fails
   */
  private void validateSingleFileSchema(String filename, byte[] content) throws IOException {
    ValidationResult result = schemaValidator.validateWithResult(filename, content);
    if (!result.valid()) {
      StringBuilder errorMessage = new StringBuilder();
      errorMessage.append("Schema validation failed for ").append(filename).append(":\n");
      for (ValidationResult.ValidationError error : result.errors()) {
        errorMessage.append("  - ").append(error.message()).append("\n");
      }
      log.error(
          "Schema validation failed for {}: {} errors found", filename, result.errors().size());
      throw new IOException(errorMessage.toString());
    }
  }

  /**
   * Writes the ZIP archive to the output stream.
   *
   * @param outputStream the output stream
   * @param fileContents map of file contents to write
   * @throws IOException if writing fails
   */
  private void writeZipArchive(OutputStream outputStream, Map<String, byte[]> fileContents)
      throws IOException {
    try (ZipArchiveOutputStream zos = new ZipArchiveOutputStream(outputStream)) {
      zos.setLevel(9); // Maximum compression

      for (Map.Entry<String, byte[]> entry : fileContents.entrySet()) {
        ZipArchiveEntry zipEntry = new ZipArchiveEntry(entry.getKey());
        zos.putArchiveEntry(zipEntry);
        zos.write(entry.getValue());
        zos.closeArchiveEntry();
      }

      zos.finish();
    }
  }

  private void validateArchive(CLDFArchive archive) {
    if (archive == null) {
      throw new IllegalArgumentException("Archive cannot be null");
    }
    if (archive.getManifest() == null) {
      throw new IllegalArgumentException("Manifest is required");
    }
    // Only validate that we have at least some data
    boolean hasLocations = archive.getLocations() != null && !archive.getLocations().isEmpty();
    boolean hasClimbs = archive.getClimbs() != null && !archive.getClimbs().isEmpty();
    boolean hasSessions = archive.getSessions() != null && !archive.getSessions().isEmpty();
    boolean hasRoutes = archive.getRoutes() != null && !archive.getRoutes().isEmpty();

    if (!hasLocations && !hasClimbs && !hasSessions && !hasRoutes) {
      throw new IllegalArgumentException(
          "Archive must contain at least one of: locations, climbs, sessions, or routes");
    }
  }

  private byte[] serializeToJson(Object obj) throws IOException {
    return objectMapper.writeValueAsString(obj).getBytes(StandardCharsets.UTF_8);
  }

  private String calculateSHA256(byte[] data) throws IOException {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
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

  private Manifest.Stats calculateStats(CLDFArchive archive) {
    return Manifest.Stats.builder()
        .climbsCount(Optional.ofNullable(archive.getClimbs()).map(List::size).orElse(0))
        .sessionsCount(Optional.ofNullable(archive.getSessions()).map(List::size).orElse(0))
        .locationsCount(Optional.ofNullable(archive.getLocations()).map(List::size).orElse(0))
        .routesCount(Optional.ofNullable(archive.getRoutes()).map(List::size).orElse(0))
        .sectorsCount(Optional.ofNullable(archive.getSectors()).map(List::size).orElse(0))
        .tagsCount(Optional.ofNullable(archive.getTags()).map(List::size).orElse(0))
        .mediaCount(Optional.ofNullable(archive.getMediaItems()).map(List::size).orElse(0))
        .build();
  }
}
