package app.crushlog.cldf.tool.services;

import java.io.File;
import java.io.IOException;

import app.crushlog.cldf.api.CLDFArchive;
import app.crushlog.cldf.tool.models.ValidationReport;

/**
 * Service for comprehensive validation of CLDF archives and generation of validation reports. This
 * service orchestrates various validation strategies including structure validation, checksum
 * verification, and statistics gathering.
 */
public interface ValidationReportService {

  /**
   * Validates a CLDF archive file with default validation options.
   *
   * @param file the CLDF archive file to validate
   * @return a comprehensive validation report
   * @throws IOException if the file cannot be read or parsed
   */
  ValidationReport validateFile(File file) throws IOException;

  /**
   * Validates a CLDF archive file with specified validation options.
   *
   * @param file the CLDF archive file to validate
   * @param options validation options specifying which checks to perform
   * @return a comprehensive validation report
   * @throws IOException if the file cannot be read or parsed
   */
  ValidationReport validateFile(File file, ValidationOptions options) throws IOException;

  /**
   * Validates an already loaded CLDF archive with default validation options.
   *
   * @param archive the CLDF archive to validate
   * @param fileName the name of the file for reporting purposes
   * @return a comprehensive validation report
   */
  ValidationReport validateArchive(CLDFArchive archive, String fileName);

  /**
   * Validates an already loaded CLDF archive with specified validation options.
   *
   * @param archive the CLDF archive to validate
   * @param fileName the name of the file for reporting purposes
   * @param options validation options specifying which checks to perform
   * @return a comprehensive validation report
   */
  ValidationReport validateArchive(CLDFArchive archive, String fileName, ValidationOptions options);

  /**
   * Validates an already loaded CLDF archive with access to raw file contents. This method is
   * useful when checksum validation is needed.
   *
   * @param archive the CLDF archive to validate
   * @param fileName the name of the file for reporting purposes
   * @param archiveFile the original archive file for checksum validation
   * @param options validation options specifying which checks to perform
   * @return a comprehensive validation report
   * @throws IOException if checksum calculation fails
   */
  ValidationReport validateArchive(
      CLDFArchive archive, String fileName, File archiveFile, ValidationOptions options)
      throws IOException;

  /** Options for controlling which validations to perform. */
  class ValidationOptions {
    private boolean validateSchema = true;
    private boolean validateChecksums = true;
    private boolean validateReferences = true;
    private boolean strict = false;

    public ValidationOptions() {}

    public ValidationOptions(
        boolean validateSchema, boolean validateChecksums, boolean validateReferences) {
      this.validateSchema = validateSchema;
      this.validateChecksums = validateChecksums;
      this.validateReferences = validateReferences;
    }

    /** Creates validation options from command line flags. */
    public static ValidationOptions fromFlags(
        boolean validateSchema,
        boolean validateChecksums,
        boolean validateReferences,
        boolean strict) {
      ValidationOptions options = new ValidationOptions();
      if (strict) {
        options.setStrict(true);
        options.setValidateSchema(true);
        options.setValidateChecksums(true);
        options.setValidateReferences(true);
      } else {
        options.setValidateSchema(validateSchema);
        options.setValidateChecksums(validateChecksums);
        options.setValidateReferences(validateReferences);
      }
      return options;
    }

    public boolean isValidateSchema() {
      return validateSchema;
    }

    public void setValidateSchema(boolean validateSchema) {
      this.validateSchema = validateSchema;
    }

    public boolean isValidateChecksums() {
      return validateChecksums;
    }

    public void setValidateChecksums(boolean validateChecksums) {
      this.validateChecksums = validateChecksums;
    }

    public boolean isValidateReferences() {
      return validateReferences;
    }

    public void setValidateReferences(boolean validateReferences) {
      this.validateReferences = validateReferences;
    }

    public boolean isStrict() {
      return strict;
    }

    public void setStrict(boolean strict) {
      this.strict = strict;
      if (strict) {
        this.validateSchema = true;
        this.validateChecksums = true;
        this.validateReferences = true;
      }
    }
  }
}
