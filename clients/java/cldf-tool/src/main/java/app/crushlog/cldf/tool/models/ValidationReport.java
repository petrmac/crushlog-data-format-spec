package app.crushlog.cldf.tool.models;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Immutable record representing a validation report for a CLDF archive.
 *
 * @param file The name of the file being validated
 * @param timestamp The time when validation was performed
 * @param valid Overall validation result (structure + checksums)
 * @param structureValid Whether the archive structure is valid
 * @param checksumResult Result of checksum validation (optional)
 * @param statistics Statistics about the archive contents
 * @param errors List of validation errors
 * @param warnings List of validation warnings
 */
public record ValidationReport(
    String file,
    OffsetDateTime timestamp,
    boolean valid,
    boolean structureValid,
    ChecksumResult checksumResult,
    Statistics statistics,
    List<String> errors,
    List<String> warnings) {

  /** Builder for ValidationReport to maintain compatibility with existing code. */
  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String file;
    private OffsetDateTime timestamp;
    private boolean valid;
    private boolean structureValid;
    private ChecksumResult checksumResult;
    private Statistics statistics;
    private List<String> errors;
    private List<String> warnings;

    public Builder file(String file) {
      this.file = file;
      return this;
    }

    public Builder timestamp(OffsetDateTime timestamp) {
      this.timestamp = timestamp;
      return this;
    }

    public Builder valid(boolean valid) {
      this.valid = valid;
      return this;
    }

    public Builder structureValid(boolean structureValid) {
      this.structureValid = structureValid;
      return this;
    }

    public Builder checksumResult(ChecksumResult checksumResult) {
      this.checksumResult = checksumResult;
      return this;
    }

    public Builder statistics(Statistics statistics) {
      this.statistics = statistics;
      return this;
    }

    public Builder errors(List<String> errors) {
      this.errors = errors;
      return this;
    }

    public Builder warnings(List<String> warnings) {
      this.warnings = warnings;
      return this;
    }

    public ValidationReport build() {
      return new ValidationReport(
          file, timestamp, valid, structureValid, checksumResult, statistics, errors, warnings);
    }
  }

  // Getters for backward compatibility with code that uses getters
  public String getFile() {
    return file;
  }

  public OffsetDateTime getTimestamp() {
    return timestamp;
  }

  public boolean isValid() {
    return valid;
  }

  public boolean isStructureValid() {
    return structureValid;
  }

  public ChecksumResult getChecksumResult() {
    return checksumResult;
  }

  public Statistics getStatistics() {
    return statistics;
  }

  public List<String> getErrors() {
    return errors;
  }

  public List<String> getWarnings() {
    return warnings;
  }
}
