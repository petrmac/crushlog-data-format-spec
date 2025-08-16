package app.crushlog.cldf.tool.services;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import app.crushlog.cldf.api.CLDF;
import app.crushlog.cldf.api.CLDFArchive;
import app.crushlog.cldf.models.Checksums;
import app.crushlog.cldf.tool.models.ChecksumResult;
import app.crushlog.cldf.tool.models.Statistics;
import app.crushlog.cldf.tool.models.ValidationReport;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;

/**
 * Default implementation of ValidationReportService that provides comprehensive validation of CLDF
 * archives including structure, checksums, and references.
 */
@Slf4j
@Singleton
public class DefaultValidationReportService implements ValidationReportService {

  private static final String CHECKSUMS_FILE = "checksums.json";
  private static final String SHA256_ALGORITHM = "SHA-256";

  private final ValidationService validationService;

  @Inject
  public DefaultValidationReportService(ValidationService validationService) {
    this.validationService = validationService;
  }

  @Override
  public ValidationReport validateFile(File file) throws IOException {
    return validateFile(file, new ValidationOptions());
  }

  @Override
  public ValidationReport validateFile(File file, ValidationOptions options) throws IOException {
    if (!file.exists()) {
      return createErrorReport(
          file.getName(), "File not found", "File not found: " + file.getAbsolutePath());
    }

    try {
      // Read the archive
      CLDFArchive archive = CLDF.read(file);

      // Validate with file access for checksums
      return validateArchive(archive, file.getName(), file, options);
    } catch (IOException e) {
      return handleReadError(file.getName(), e);
    }
  }

  @Override
  public ValidationReport validateArchive(CLDFArchive archive, String fileName) {
    return validateArchive(archive, fileName, new ValidationOptions());
  }

  @Override
  public ValidationReport validateArchive(
      CLDFArchive archive, String fileName, ValidationOptions options) {
    try {
      return validateArchive(archive, fileName, null, options);
    } catch (IOException e) {
      // Should not happen when archiveFile is null
      log.error("Unexpected IOException during validation", e);
      return createErrorReport(fileName, "Validation error", e.getMessage());
    }
  }

  @Override
  public ValidationReport validateArchive(
      CLDFArchive archive, String fileName, File archiveFile, ValidationOptions options)
      throws IOException {

    ValidationReport.Builder reportBuilder = ValidationReport.builder();
    reportBuilder.timestamp(OffsetDateTime.now());
    reportBuilder.file(fileName);

    List<String> allErrors = new ArrayList<>();
    List<String> allWarnings = new ArrayList<>();
    boolean overallValid = true;

    // Structure validation
    if (options.isValidateSchema() || options.isValidateReferences()) {
      ValidationResult structureResult = validationService.validate(archive);
      reportBuilder.structureValid(structureResult.isValid());
      allErrors.addAll(structureResult.getErrors());
      allWarnings.addAll(structureResult.getWarnings());
      overallValid = overallValid && structureResult.isValid();
    } else {
      reportBuilder.structureValid(true);
    }

    // Checksum validation
    ChecksumResult checksumResult = null;
    if (options.isValidateChecksums() && archive.getChecksums() != null && archiveFile != null) {
      checksumResult = validateChecksums(archive, archiveFile);
      reportBuilder.checksumResult(checksumResult);
      overallValid = overallValid && checksumResult.valid();

      // Add checksum errors to the error list
      if (!checksumResult.valid()) {
        for (Map.Entry<String, Boolean> entry : checksumResult.results().entrySet()) {
          if (!entry.getValue()) {
            allErrors.add("Checksum mismatch for file: " + entry.getKey());
          }
        }
      }
    }

    // Gather statistics
    Statistics statistics = gatherStatistics(archive);
    reportBuilder.statistics(statistics);

    // Set final results
    reportBuilder.valid(overallValid);
    reportBuilder.errors(allErrors);
    reportBuilder.warnings(allWarnings);

    return reportBuilder.build();
  }

  /**
   * Validates checksums for all files in the archive.
   *
   * @param archive the CLDF archive containing expected checksums
   * @param archiveFile the archive file to read for actual checksums
   * @return checksum validation result
   * @throws IOException if file reading fails
   */
  private ChecksumResult validateChecksums(CLDFArchive archive, File archiveFile)
      throws IOException {

    Checksums expectedChecksums = archive.getChecksums();
    String algorithm = expectedChecksums.getAlgorithm();

    // Only support SHA-256 for now
    if (!SHA256_ALGORITHM.equals(algorithm)) {
      log.warn("Unsupported checksum algorithm: {}. Skipping checksum validation.", algorithm);
      return ChecksumResult.builder()
          .algorithm(algorithm)
          .valid(false)
          .results(Map.of("error", false))
          .build();
    }

    // Extract and calculate actual checksums
    Map<String, String> actualChecksums = calculateArchiveChecksums(archiveFile);

    // Compare checksums
    Map<String, Boolean> results = new TreeMap<>();
    boolean allValid = true;

    for (Map.Entry<String, String> entry : expectedChecksums.getFiles().entrySet()) {
      String filename = entry.getKey();
      String expectedChecksum = entry.getValue();
      String actualChecksum = actualChecksums.get(filename);

      if (actualChecksum == null) {
        log.warn("File referenced in checksums but not found in archive: {}", filename);
        results.put(filename, false);
        allValid = false;
      } else {
        boolean matches = expectedChecksum.equalsIgnoreCase(actualChecksum);
        results.put(filename, matches);
        if (!matches) {
          log.error(
              "Checksum mismatch for {}: expected={}, actual={}",
              filename,
              expectedChecksum,
              actualChecksum);
          allValid = false;
        }
      }
    }

    // Check for extra files not in checksums
    for (String filename : actualChecksums.keySet()) {
      if (!expectedChecksums.getFiles().containsKey(filename) && !CHECKSUMS_FILE.equals(filename)) {
        log.warn("File in archive not listed in checksums: {}", filename);
        results.put(filename, true); // Not an error, just a warning
      }
    }

    return ChecksumResult.builder().algorithm(algorithm).valid(allValid).results(results).build();
  }

  /**
   * Calculates SHA-256 checksums for all files in the archive.
   *
   * @param archiveFile the ZIP archive file
   * @return map of file names to their calculated checksums
   * @throws IOException if file reading fails
   */
  private Map<String, String> calculateArchiveChecksums(File archiveFile) throws IOException {
    Map<String, String> checksums = new HashMap<>();

    try (FileInputStream fis = new FileInputStream(archiveFile);
        ZipArchiveInputStream zis = new ZipArchiveInputStream(fis)) {

      ZipArchiveEntry entry;
      while ((entry = zis.getNextZipEntry()) != null) {
        if (!entry.isDirectory() && !CHECKSUMS_FILE.equals(entry.getName())) {
          byte[] content = readEntryContent(zis);
          String checksum = calculateSHA256(content);
          checksums.put(entry.getName(), checksum);
        }
      }
    }

    return checksums;
  }

  /**
   * Reads the content of a ZIP entry.
   *
   * @param zis the ZIP archive input stream
   * @return the content as byte array
   * @throws IOException if reading fails
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
   * Calculates SHA-256 checksum for the given data.
   *
   * @param data the data to checksum
   * @return hex string representation of the checksum
   * @throws IOException if algorithm is not available
   */
  private String calculateSHA256(byte[] data) throws IOException {
    try {
      MessageDigest digest = MessageDigest.getInstance(SHA256_ALGORITHM);
      byte[] hash = digest.digest(data);

      // Convert to hex string
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

  /**
   * Gathers statistics from the archive.
   *
   * @param archive the CLDF archive
   * @return statistics about the archive contents
   */
  private Statistics gatherStatistics(CLDFArchive archive) {
    return Statistics.builder()
        .locations(archive.getLocations() != null ? archive.getLocations().size() : 0)
        .sessions(archive.getSessions() != null ? archive.getSessions().size() : 0)
        .climbs(archive.getClimbs() != null ? archive.getClimbs().size() : 0)
        .routes(archive.hasRoutes() ? archive.getRoutes().size() : 0)
        .sectors(archive.hasSectors() ? archive.getSectors().size() : 0)
        .tags(archive.hasTags() ? archive.getTags().size() : 0)
        .mediaItems(archive.hasMedia() ? archive.getMediaItems().size() : 0)
        .build();
  }

  /**
   * Handles errors that occur when reading the archive file.
   *
   * @param fileName the name of the file
   * @param e the IOException that occurred
   * @return an error report
   */
  private ValidationReport handleReadError(String fileName, IOException e) {
    String errorMessage = e.getMessage();
    String errorType = "Validation failed";

    if (errorMessage != null) {
      if (errorMessage.contains("Schema validation failed")) {
        errorType = "Schema validation failed";
      } else if (errorMessage.contains("Checksum mismatch")) {
        errorType = "Checksum validation failed";
      } else if (errorMessage.contains("Missing required file")) {
        errorType = "Archive structure validation failed";
      }
    }

    return createErrorReport(fileName, errorType, errorMessage);
  }

  /**
   * Creates an error report for validation failures.
   *
   * @param fileName the name of the file
   * @param errorType the type of error
   * @param errorMessage the error message
   * @return a validation report indicating failure
   */
  private ValidationReport createErrorReport(
      String fileName, String errorType, String errorMessage) {

    return ValidationReport.builder()
        .timestamp(OffsetDateTime.now())
        .file(fileName)
        .valid(false)
        .structureValid(false)
        .errors(List.of(errorType + ": " + errorMessage))
        .warnings(new ArrayList<>())
        .statistics(
            Statistics.builder()
                .locations(0)
                .sessions(0)
                .climbs(0)
                .routes(0)
                .sectors(0)
                .tags(0)
                .mediaItems(0)
                .build())
        .build();
  }
}
