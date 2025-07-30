package io.cldf.tool.services;

import java.util.List;

import lombok.Builder;
import lombok.Data;

/** Result of a CLDF archive validation. Contains validation status, errors, and warnings. */
@Data
@Builder
public class ValidationResult {
  private boolean valid;
  private List<String> errors;
  private List<String> warnings;

  public boolean hasWarnings() {
    return warnings != null && !warnings.isEmpty();
  }

  public String getSummary() {
    if (valid && !hasWarnings()) {
      return "Validation passed";
    } else if (valid && hasWarnings()) {
      return String.format("Validation passed with %d warnings", warnings.size());
    } else {
      return String.format(
          "Validation failed with %d errors and %d warnings", errors.size(), warnings.size());
    }
  }
}
