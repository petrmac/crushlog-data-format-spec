package io.cldf.api;

import java.util.Collections;
import java.util.List;

/**
 * Represents the result of a schema validation operation. This provides a cleaner API than throwing
 * exceptions, allowing consumers to handle validation errors more gracefully.
 */
public record ValidationResult(boolean valid, String filename, List<ValidationError> errors) {

  /** Creates a successful validation result. */
  public static ValidationResult success(String filename) {
    return new ValidationResult(true, filename, Collections.emptyList());
  }

  /** Creates a failed validation result with errors. */
  public static ValidationResult failure(String filename, List<ValidationError> errors) {
    return new ValidationResult(false, filename, List.copyOf(errors));
  }

  /** Represents a single validation error. */
  public record ValidationError(String path, String message, String type, Object invalidValue) {

    /** Creates a ValidationError without an invalid value. */
    public ValidationError(String path, String message, String type) {
      this(path, message, type, null);
    }
  }
}
