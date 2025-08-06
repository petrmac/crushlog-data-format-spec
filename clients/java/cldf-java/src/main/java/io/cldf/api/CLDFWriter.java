package io.cldf.api;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.cldf.models.*;
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
  private final boolean prettyPrint;
  private final boolean validateSchemas;
  private final SchemaValidator schemaValidator;

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
    this.prettyPrint = prettyPrint;
    this.validateSchemas = validateSchemas;
    this.schemaValidator = validateSchemas ? new SchemaValidator() : null;
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
    validateArchive(archive);

    Map<String, byte[]> fileContents = new HashMap<>();
    Map<String, String> checksums = new HashMap<>();

    // Prepare manifest
    if (archive.getManifest() == null) {
      throw new IllegalArgumentException("Manifest is required");
    }
    
    // Calculate and set stats if not already present
    if (archive.getManifest().getStats() == null) {
      archive.getManifest().setStats(calculateStats(archive));
    }
    
    byte[] manifestBytes = serializeToJson(archive.getManifest());
    fileContents.put(MANIFEST_FILE, manifestBytes);
    checksums.put(MANIFEST_FILE, calculateSHA256(manifestBytes));

    // Prepare files - all are now optional except manifest and checksums
    if (archive.getLocations() != null && !archive.getLocations().isEmpty()) {
      LocationsFile locationsFile =
          LocationsFile.builder().locations(archive.getLocations()).build();
      byte[] locationsBytes = serializeToJson(locationsFile);
      fileContents.put(LOCATIONS_FILE, locationsBytes);
      checksums.put(LOCATIONS_FILE, calculateSHA256(locationsBytes));
    }

    if (archive.getClimbs() != null && !archive.getClimbs().isEmpty()) {
      ClimbsFile climbsFile = ClimbsFile.builder().climbs(archive.getClimbs()).build();
      byte[] climbsBytes = serializeToJson(climbsFile);
      fileContents.put(CLIMBS_FILE, climbsBytes);
      checksums.put(CLIMBS_FILE, calculateSHA256(climbsBytes));
    }

    if (archive.getSessions() != null && !archive.getSessions().isEmpty()) {
      SessionsFile sessionsFile = SessionsFile.builder().sessions(archive.getSessions()).build();
      byte[] sessionsBytes = serializeToJson(sessionsFile);
      fileContents.put(SESSIONS_FILE, sessionsBytes);
      checksums.put(SESSIONS_FILE, calculateSHA256(sessionsBytes));
    }

    // Prepare optional files
    if (archive.hasRoutes()) {
      RoutesFile routesFile = RoutesFile.builder().routes(archive.getRoutes()).build();
      byte[] routesBytes = serializeToJson(routesFile);
      fileContents.put(ROUTES_FILE, routesBytes);
      checksums.put(ROUTES_FILE, calculateSHA256(routesBytes));
    }

    if (archive.hasSectors()) {
      SectorsFile sectorsFile = SectorsFile.builder().sectors(archive.getSectors()).build();
      byte[] sectorsBytes = serializeToJson(sectorsFile);
      fileContents.put(SECTORS_FILE, sectorsBytes);
      checksums.put(SECTORS_FILE, calculateSHA256(sectorsBytes));
    }

    if (archive.hasTags()) {
      TagsFile tagsFile = TagsFile.builder().tags(archive.getTags()).build();
      byte[] tagsBytes = serializeToJson(tagsFile);
      fileContents.put(TAGS_FILE, tagsBytes);
      checksums.put(TAGS_FILE, calculateSHA256(tagsBytes));
    }

    if (archive.hasMedia()) {
      MediaMetadataFile mediaFile =
          MediaMetadataFile.builder().media(archive.getMediaItems()).build();
      byte[] mediaBytes = serializeToJson(mediaFile);
      fileContents.put(MEDIA_METADATA_FILE, mediaBytes);
      checksums.put(MEDIA_METADATA_FILE, calculateSHA256(mediaBytes));
    }

    // Add embedded media files
    if (archive.hasEmbeddedMedia()) {
      for (Map.Entry<String, byte[]> entry : archive.getMediaFiles().entrySet()) {
        fileContents.put(entry.getKey(), entry.getValue());
        checksums.put(entry.getKey(), calculateSHA256(entry.getValue()));
      }
    }

    // Create checksums file
    Checksums checksumsObj =
        Checksums.builder()
            .algorithm("SHA-256")
            .files(checksums)
            .generatedAt(OffsetDateTime.now())
            .build();
    byte[] checksumsBytes = serializeToJson(checksumsObj);
    fileContents.put(CHECKSUMS_FILE, checksumsBytes);

    // Validate schemas if enabled
    if (validateSchemas) {
      for (Map.Entry<String, byte[]> entry : fileContents.entrySet()) {
        // Skip media files
        if (!entry.getKey().startsWith("media/")) {
          ValidationResult result =
              schemaValidator.validateWithResult(entry.getKey(), entry.getValue());
          if (!result.valid()) {
            StringBuilder errorMessage = new StringBuilder();
            errorMessage
                .append("Schema validation failed for ")
                .append(entry.getKey())
                .append(":\n");
            for (ValidationResult.ValidationError error : result.errors()) {
              errorMessage.append("  - ").append(error.message()).append("\n");
            }
            throw new IOException(errorMessage.toString());
          }
        }
      }
    }

    // Write ZIP archive
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
