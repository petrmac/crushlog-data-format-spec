package io.cldf.tool.services;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import jakarta.inject.Singleton;

import io.cldf.api.CLDFArchive;
import io.cldf.api.CLDFWriter;
import io.cldf.models.*;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class ValidationService {

  public ValidationService() {
    // No initialization needed
  }

  public ValidationResult validate(CLDFArchive archive) {
    List<String> errors = new ArrayList<>();
    List<String> warnings = new ArrayList<>();

    // Basic structure validation
    validateBasicStructure(archive, errors);

    // Schema validation
    validateSchemas(archive, errors);

    // Business rules
    validateBusinessRules(archive, warnings);

    return ValidationResult.builder()
        .valid(errors.isEmpty())
        .errors(errors)
        .warnings(warnings)
        .build();
  }

  private void validateBasicStructure(CLDFArchive archive, List<String> errors) {
    if (archive.getManifest() == null) {
      errors.add("Manifest is required");
    }

    if (archive.getLocations() == null || archive.getLocations().isEmpty()) {
      errors.add("At least one location is required");
    }

    if (archive.getSessions() == null || archive.getSessions().isEmpty()) {
      errors.add("At least one session is required");
    }

    if (archive.getClimbs() == null || archive.getClimbs().isEmpty()) {
      errors.add("At least one climb is required");
    }
  }

  private void validateSchemas(CLDFArchive archive, List<String> errors) {
    // Schema validation is already performed by CLDFWriter when validateSchemas is enabled
    // We can use CLDFWriter to validate without actually writing
    try {
      CLDFWriter validator = new CLDFWriter(false, true);
      // This will throw if validation fails
      File tempFile = File.createTempFile("cldf_validate_", ".tmp");
      try {
        validator.write(archive, tempFile);
      } finally {
        tempFile.delete();
      }
    } catch (Exception e) {
      errors.add("Schema validation failed: " + e.getMessage());
    }
  }

  private void validateBusinessRules(CLDFArchive archive, List<String> warnings) {
    // Check for future dates
    long futureClimbs =
        archive.getClimbs().stream()
            .filter(
                climb ->
                    climb.getDate() != null && climb.getDate().isAfter(java.time.LocalDate.now()))
            .count();

    if (futureClimbs > 0) {
      warnings.add(String.format("%d climbs have dates in the future", futureClimbs));
    }

    // Check for duplicate climb names within same day
    Map<String, Long> climbsByDateAndName =
        archive.getClimbs().stream()
            .filter(climb -> climb.getDate() != null && climb.getRouteName() != null)
            .collect(
                Collectors.groupingBy(
                    climb -> climb.getDate() + "|" + climb.getRouteName(), Collectors.counting()));

    climbsByDateAndName.entrySet().stream()
        .filter(e -> e.getValue() > 1)
        .forEach(
            e -> {
              String[] parts = e.getKey().split("\\|");
              warnings.add(
                  String.format(
                      "Route '%s' appears %d times on %s", parts[1], e.getValue(), parts[0]));
            });
  }

  @Data
  @Builder
  public static class ValidationResult {
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
}
