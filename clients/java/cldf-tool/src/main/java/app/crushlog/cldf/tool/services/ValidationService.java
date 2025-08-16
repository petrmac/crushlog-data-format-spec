package app.crushlog.cldf.tool.services;

import app.crushlog.cldf.api.CLDFArchive;

/**
 * Interface for validating CLDF archives. Provides methods to validate the structure, schema
 * compliance, and business rules.
 */
public interface ValidationService {

  /**
   * Validates a CLDF archive against schemas and business rules.
   *
   * @param archive the CLDF archive to validate
   * @return validation result containing errors and warnings
   */
  ValidationResult validate(CLDFArchive archive);
}
