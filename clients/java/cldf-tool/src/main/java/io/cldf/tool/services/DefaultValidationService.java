package io.cldf.tool.services;

import java.util.*;
import java.util.stream.Collectors;

import jakarta.inject.Singleton;

import io.cldf.api.CLDFArchive;
import io.cldf.models.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class DefaultValidationService implements ValidationService {

  public DefaultValidationService() {
    // No initialization needed
  }

  @Override
  public ValidationResult validate(CLDFArchive archive) {
    List<String> errors = new ArrayList<>();
    List<String> warnings = new ArrayList<>();

    // Basic structure validation
    validateBasicStructure(archive, errors);

    // Schema validation
    validateSchemas(archive, errors);

    // Business rules
    validateBusinessRules(archive, warnings);

    // Reference integrity validation
    validateReferenceIntegrity(archive, errors);

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
    io.cldf.api.SchemaValidator schemaValidator = new io.cldf.api.SchemaValidator();

    // Validate manifest
    if (archive.getManifest() != null) {
      io.cldf.api.ValidationResult result =
          schemaValidator.validateObjectWithResult("manifest.json", archive.getManifest());
      if (!result.valid()) {
        for (io.cldf.api.ValidationResult.ValidationError error : result.errors()) {
          errors.add(String.format("manifest.json%s: %s", error.path(), error.message()));
        }
      }
    }

    // Validate locations
    if (archive.getLocations() != null && !archive.getLocations().isEmpty()) {
      io.cldf.api.ValidationResult result =
          schemaValidator.validateObjectWithResult(
              "locations.json",
              io.cldf.models.LocationsFile.builder().locations(archive.getLocations()).build());
      if (!result.valid()) {
        for (io.cldf.api.ValidationResult.ValidationError error : result.errors()) {
          errors.add(String.format("locations.json%s: %s", error.path(), error.message()));
        }
      }
    }

    // Validate sessions
    if (archive.getSessions() != null && !archive.getSessions().isEmpty()) {
      io.cldf.api.ValidationResult result =
          schemaValidator.validateObjectWithResult(
              "sessions.json",
              io.cldf.models.SessionsFile.builder().sessions(archive.getSessions()).build());
      if (!result.valid()) {
        for (io.cldf.api.ValidationResult.ValidationError error : result.errors()) {
          errors.add(String.format("sessions.json%s: %s", error.path(), error.message()));
        }
      }
    }

    // Validate climbs
    if (archive.getClimbs() != null && !archive.getClimbs().isEmpty()) {
      io.cldf.api.ValidationResult result =
          schemaValidator.validateObjectWithResult(
              "climbs.json",
              io.cldf.models.ClimbsFile.builder().climbs(archive.getClimbs()).build());
      if (!result.valid()) {
        for (io.cldf.api.ValidationResult.ValidationError error : result.errors()) {
          errors.add(String.format("climbs.json%s: %s", error.path(), error.message()));
        }
      }
    }

    // Validate optional files
    if (archive.hasRoutes()) {
      io.cldf.api.ValidationResult result =
          schemaValidator.validateObjectWithResult(
              "routes.json",
              io.cldf.models.RoutesFile.builder().routes(archive.getRoutes()).build());
      if (!result.valid()) {
        for (io.cldf.api.ValidationResult.ValidationError error : result.errors()) {
          errors.add(String.format("routes.json%s: %s", error.path(), error.message()));
        }
      }
    }

    if (archive.hasSectors()) {
      io.cldf.api.ValidationResult result =
          schemaValidator.validateObjectWithResult(
              "sectors.json",
              io.cldf.models.SectorsFile.builder().sectors(archive.getSectors()).build());
      if (!result.valid()) {
        for (io.cldf.api.ValidationResult.ValidationError error : result.errors()) {
          errors.add(String.format("sectors.json%s: %s", error.path(), error.message()));
        }
      }
    }

    if (archive.hasTags()) {
      io.cldf.api.ValidationResult result =
          schemaValidator.validateObjectWithResult(
              "tags.json", io.cldf.models.TagsFile.builder().tags(archive.getTags()).build());
      if (!result.valid()) {
        for (io.cldf.api.ValidationResult.ValidationError error : result.errors()) {
          errors.add(String.format("tags.json%s: %s", error.path(), error.message()));
        }
      }
    }

    if (archive.hasMedia()) {
      io.cldf.api.ValidationResult result =
          schemaValidator.validateObjectWithResult(
              "media-metadata.json",
              io.cldf.models.MediaMetadataFile.builder().media(archive.getMediaItems()).build());
      if (!result.valid()) {
        for (io.cldf.api.ValidationResult.ValidationError error : result.errors()) {
          errors.add(String.format("media-metadata.json%s: %s", error.path(), error.message()));
        }
      }
    }
  }

  private void validateBusinessRules(CLDFArchive archive, List<String> warnings) {
    // Skip business rules if climbs are not available
    if (archive.getClimbs() == null || archive.getClimbs().isEmpty()) {
      return;
    }

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

  private void validateReferenceIntegrity(CLDFArchive archive, List<String> errors) {
    // Create lookup maps for validation
    Set<Integer> locationIds = new HashSet<>();
    Set<Integer> sessionIds = new HashSet<>();
    Set<Integer> routeIds = new HashSet<>();
    Set<Integer> sectorIds = new HashSet<>();

    // Collect all valid IDs
    if (archive.getLocations() != null) {
      locationIds =
          archive.getLocations().stream()
              .map(Location::getId)
              .filter(Objects::nonNull)
              .collect(Collectors.toSet());
    }

    if (archive.getSessions() != null) {
      sessionIds =
          archive.getSessions().stream()
              .map(Session::getId)
              .filter(Objects::nonNull)
              .collect(Collectors.toSet());
    }

    if (archive.getRoutes() != null) {
      routeIds =
          archive.getRoutes().stream()
              .map(Route::getId)
              .filter(Objects::nonNull)
              .collect(Collectors.toSet());
    }

    if (archive.getSectors() != null) {
      sectorIds =
          archive.getSectors().stream()
              .map(Sector::getId)
              .filter(Objects::nonNull)
              .collect(Collectors.toSet());
    }

    // Validate session references
    if (archive.getSessions() != null) {
      for (Session session : archive.getSessions()) {
        if (session.getLocationId() != null && !locationIds.contains(session.getLocationId())) {
          errors.add(
              String.format(
                  "Session %d references non-existent location %d",
                  session.getId(), session.getLocationId()));
        }
      }
    }

    // Validate climb references
    if (archive.getClimbs() != null) {
      for (Climb climb : archive.getClimbs()) {
        if (climb.getSessionId() != null && !sessionIds.contains(climb.getSessionId())) {
          errors.add(
              String.format(
                  "Climb %d references non-existent session %d",
                  climb.getId(), climb.getSessionId()));
        }
        if (climb.getRouteId() != null && !routeIds.contains(climb.getRouteId())) {
          errors.add(
              String.format(
                  "Climb %d references non-existent route %d", climb.getId(), climb.getRouteId()));
        }
      }
    }

    // Validate route references
    if (archive.getRoutes() != null) {
      for (Route route : archive.getRoutes()) {
        if (route.getSectorId() != null && !sectorIds.contains(route.getSectorId())) {
          errors.add(
              String.format(
                  "Route %d references non-existent sector %d",
                  route.getId(), route.getSectorId()));
        }
        if (route.getLocationId() != null && !locationIds.contains(route.getLocationId())) {
          errors.add(
              String.format(
                  "Route %d references non-existent location %d",
                  route.getId(), route.getLocationId()));
        }
      }
    }

    // Validate sector references
    if (archive.getSectors() != null) {
      for (Sector sector : archive.getSectors()) {
        if (sector.getLocationId() != null && !locationIds.contains(sector.getLocationId())) {
          errors.add(
              String.format(
                  "Sector %d references non-existent location %d",
                  sector.getId(), sector.getLocationId()));
        }
      }
    }
  }
}
