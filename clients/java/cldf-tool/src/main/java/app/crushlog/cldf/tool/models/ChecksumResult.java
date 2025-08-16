package app.crushlog.cldf.tool.models;

import java.util.Map;

/**
 * Immutable record representing the result of checksum validation.
 *
 * @param algorithm The checksum algorithm used (e.g., SHA-256, MD5)
 * @param valid Whether all checksums are valid
 * @param results Map of file names to their validation results
 */
public record ChecksumResult(String algorithm, boolean valid, Map<String, Boolean> results) {

  /** Builder for ChecksumResult to maintain compatibility with existing code. */
  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String algorithm;
    private boolean valid;
    private Map<String, Boolean> results;

    public Builder algorithm(String algorithm) {
      this.algorithm = algorithm;
      return this;
    }

    public Builder valid(boolean valid) {
      this.valid = valid;
      return this;
    }

    public Builder results(Map<String, Boolean> results) {
      this.results = results;
      return this;
    }

    public ChecksumResult build() {
      return new ChecksumResult(algorithm, valid, results);
    }
  }
}
