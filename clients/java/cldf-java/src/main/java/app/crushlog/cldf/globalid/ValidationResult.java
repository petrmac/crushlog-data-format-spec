package app.crushlog.cldf.globalid;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

/** Validation result for ID generation */
@Getter
public class ValidationResult {
  private final List<String> errors = new ArrayList<>();
  private final List<String> warnings = new ArrayList<>();

  public void addError(String error) {
    errors.add(error);
  }

  public void addWarning(String warning) {
    warnings.add(warning);
  }

  public boolean isValid() {
    return errors.isEmpty();
  }

  @Override
  public String toString() {
    if (isValid()) {
      return "Valid" + (warnings.isEmpty() ? "" : " with warnings: " + warnings);
    }
    return "Invalid: " + errors;
  }
}
