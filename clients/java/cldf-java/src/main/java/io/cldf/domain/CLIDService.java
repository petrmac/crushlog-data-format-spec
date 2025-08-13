package io.cldf.domain;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.cldf.api.CLDFArchive;
import io.cldf.globalid.CLID;
import io.cldf.globalid.CLIDGenerator;
import io.cldf.globalid.RouteModel;
import io.cldf.models.*;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Domain service for managing CLIDs (CrushLog IDs). Handles automatic generation of missing CLIDs
 * and validation of existing ones.
 */
@Slf4j
public class CLIDService {

  /**
   * Process and validate CLIDs in a CLDF archive. - Generates missing CLIDs for entities -
   * Validates existing CLIDs for correct format and type matching - Ensures CLID uniqueness within
   * the archive
   *
   * @param archive The archive to process
   * @param generateMissing Whether to generate CLIDs for entities that don't have them
   * @param validateExisting Whether to validate existing CLIDs
   * @throws CLIDValidationException if validation fails
   */
  public void processArchiveCLIDs(
      CLDFArchive archive, boolean generateMissing, boolean validateExisting) {
    log.debug(
        "Processing archive CLIDs: generateMissing={}, validateExisting={}",
        generateMissing,
        validateExisting);

    ProcessingContext context =
        ProcessingContext.builder()
            .generateMissing(generateMissing)
            .validateExisting(validateExisting)
            .clidRegistry(new HashMap<>())
            .locationCLIDs(buildLocationCLIDMap(archive.getLocations()))
            .build();

    // Process all entity types using functional streams
    ProcessingResult result =
        Stream.of(
                processLocations(archive.getLocations(), context),
                processRoutes(archive.getRoutes(), context),
                processSectors(archive.getSectors(), context),
                processClimbs(archive.getClimbs(), context),
                processSessions(archive.getSessions(), context))
            .reduce(ProcessingResult.empty(), ProcessingResult::combine);

    log.info(
        "CLID processing complete: {} generated, {} validated, {} total unique CLIDs",
        result.getGeneratedCount(),
        result.getValidatedCount(),
        context.getClidRegistry().size());
  }

  private ProcessingResult processLocations(List<Location> locations, ProcessingContext context) {
    if (locations == null || locations.isEmpty()) {
      return ProcessingResult.empty();
    }

    log.debug("Processing {} locations", locations.size());

    return locations.stream()
        .map(
            location ->
                processEntity(
                    location.getClid(),
                    () -> generateLocationCLID(location),
                    clid ->
                        validateAndRegister(
                            clid,
                            location,
                            CLIDGenerator.EntityType.LOCATION,
                            "Location",
                            location.getName(),
                            context),
                    location::setClid,
                    context))
        .reduce(ProcessingResult.empty(), ProcessingResult::combine);
  }

  private ProcessingResult processRoutes(List<Route> routes, ProcessingContext context) {
    if (routes == null || routes.isEmpty()) {
      return ProcessingResult.empty();
    }

    log.debug("Processing {} routes", routes.size());

    return routes.stream()
        .map(
            route -> {
              String locationCLID = context.getLocationCLIDs().get(route.getLocationId());
              return processEntity(
                  route.getClid(),
                  () -> generateRouteCLID(route, locationCLID),
                  clid ->
                      validateAndRegister(
                          clid,
                          route,
                          CLIDGenerator.EntityType.ROUTE,
                          "Route",
                          route.getName(),
                          context),
                  route::setClid,
                  context);
            })
        .reduce(ProcessingResult.empty(), ProcessingResult::combine);
  }

  private ProcessingResult processSectors(List<Sector> sectors, ProcessingContext context) {
    if (sectors == null || sectors.isEmpty()) {
      return ProcessingResult.empty();
    }

    log.debug("Processing {} sectors", sectors.size());

    return sectors.stream()
        .map(
            sector -> {
              String locationCLID = context.getLocationCLIDs().get(sector.getLocationId());
              return processEntity(
                  sector.getClid(),
                  () -> generateSectorCLID(sector, locationCLID),
                  clid ->
                      validateAndRegister(
                          clid,
                          sector,
                          CLIDGenerator.EntityType.SECTOR,
                          "Sector",
                          sector.getName(),
                          context),
                  sector::setClid,
                  context);
            })
        .reduce(ProcessingResult.empty(), ProcessingResult::combine);
  }

  private ProcessingResult processClimbs(List<Climb> climbs, ProcessingContext context) {
    if (climbs == null || climbs.isEmpty()) {
      return ProcessingResult.empty();
    }

    log.debug("Processing {} climbs", climbs.size());

    return climbs.stream()
        .map(
            climb ->
                processEntity(
                    climb.getClid(),
                    () -> CLIDGenerator.generateRandomCLID(CLIDGenerator.EntityType.CLIMB),
                    clid ->
                        validateAndRegister(
                            clid,
                            climb,
                            CLIDGenerator.EntityType.CLIMB,
                            "Climb",
                            getClimbIdentifier(climb),
                            context),
                    climb::setClid,
                    context))
        .reduce(ProcessingResult.empty(), ProcessingResult::combine);
  }

  private ProcessingResult processSessions(List<Session> sessions, ProcessingContext context) {
    if (sessions == null || sessions.isEmpty()) {
      return ProcessingResult.empty();
    }

    log.debug("Processing {} sessions", sessions.size());

    return sessions.stream()
        .map(
            session ->
                processEntity(
                    session.getClid(),
                    () -> CLIDGenerator.generateRandomCLID(CLIDGenerator.EntityType.SESSION),
                    clid ->
                        validateAndRegister(
                            clid,
                            session,
                            CLIDGenerator.EntityType.SESSION,
                            "Session",
                            getSessionIdentifier(session),
                            context),
                    session::setClid,
                    context))
        .reduce(ProcessingResult.empty(), ProcessingResult::combine);
  }

  private ProcessingResult processEntity(
      String existingClid,
      CLIDSupplier generator,
      Function<String, Boolean> validator,
      CLIDSetter setter,
      ProcessingContext context) {

    boolean generated = false;
    boolean validated = false;

    if (existingClid == null || existingClid.isEmpty()) {
      if (context.isGenerateMissing() && generator.canGenerate()) {
        String newClid = generator.generate();
        setter.setClid(newClid);
        validator.apply(newClid); // Register the new CLID
        generated = true;
        log.debug("Generated CLID: {}", newClid);
      }
    } else {
      if (context.isValidateExisting()) {
        validator.apply(existingClid);
        validated = true;
      }
    }

    return ProcessingResult.builder()
        .generatedCount(generated ? 1 : 0)
        .validatedCount(validated ? 1 : 0)
        .build();
  }

  private boolean validateAndRegister(
      String clid,
      Object entity,
      CLIDGenerator.EntityType expectedType,
      String entityName,
      String identifier,
      ProcessingContext context) {
    if (clid == null) return false;

    // Validate CLID format and type
    if (context.isValidateExisting()) {
      validateCLID(clid, expectedType, entityName, identifier);
    }

    // Register CLID for uniqueness check
    registerCLID(clid, entity, context.getClidRegistry());

    return true;
  }

  private String generateLocationCLID(Location location) {
    if (!canGenerateDeterministicLocationCLID(location)) {
      return CLIDGenerator.generateRandomCLID(CLIDGenerator.EntityType.LOCATION);
    }

    io.cldf.globalid.Location genLocation =
        new io.cldf.globalid.Location(
            location.getCountry(),
            location.getState(),
            location.getCity(),
            location.getName(),
            new io.cldf.globalid.Coordinates(
                location.getCoordinates().getLatitude(), location.getCoordinates().getLongitude()),
            location.getIsIndoor());
    return CLIDGenerator.generateLocationCLID(genLocation);
  }

  private String generateRouteCLID(Route route, String locationCLID) {
    if (!canGenerateDeterministicRouteCLID(route) || locationCLID == null) {
      return CLIDGenerator.generateRandomCLID(CLIDGenerator.EntityType.ROUTE);
    }

    String grade =
        Optional.ofNullable(route.getGrades())
            .flatMap(
                g ->
                    Stream.of(g.getYds(), g.getFrench(), g.getUiaa(), g.getVScale(), g.getFont())
                        .filter(Objects::nonNull)
                        .findFirst())
            .orElse(null);

    if (grade == null) {
      return CLIDGenerator.generateRandomCLID(CLIDGenerator.EntityType.ROUTE);
    }

    RouteModel.RouteType genType =
        route.getRouteType() == io.cldf.models.enums.RouteType.BOULDER
            ? RouteModel.RouteType.BOULDER
            : RouteModel.RouteType.SPORT;

    RouteModel.FirstAscent genFirstAscent =
        Optional.ofNullable(route.getFirstAscent())
            .map(
                fa ->
                    new RouteModel.FirstAscent(
                        fa.getName(),
                        Optional.ofNullable(fa.getDate())
                            .map(java.time.LocalDate::getYear)
                            .orElse(null)))
            .orElse(null);

    RouteModel.Route genRoute =
        new RouteModel.Route(route.getName(), grade, genType, genFirstAscent, route.getHeight());

    return CLIDGenerator.generateRouteCLID(locationCLID, genRoute);
  }

  private String generateSectorCLID(Sector sector, String locationCLID) {
    if (locationCLID == null || sector.getName() == null) {
      return CLIDGenerator.generateRandomCLID(CLIDGenerator.EntityType.SECTOR);
    }

    io.cldf.globalid.Sector genSector =
        new io.cldf.globalid.Sector(
            sector.getName(), null // Sector doesn't have order in the model
            );
    return CLIDGenerator.generateSectorCLID(locationCLID, genSector);
  }

  private boolean canGenerateDeterministicLocationCLID(Location location) {
    return location.getName() != null
        && location.getCountry() != null
        && location.getCoordinates() != null
        && location.getCoordinates().getLatitude() != null
        && location.getCoordinates().getLongitude() != null
        && location.getIsIndoor() != null;
  }

  private boolean canGenerateDeterministicRouteCLID(Route route) {
    return route.getName() != null && route.getGrades() != null && route.getRouteType() != null;
  }

  private void validateCLID(
      String clid, CLIDGenerator.EntityType expectedType, String entityName, String identifier) {
    try {
      CLID parsed = CLID.fromString(clid);

      if (parsed.type() != expectedType) {
        log.warn(
            "CLID type mismatch for {} '{}': expected '{}' but found '{}'",
            entityName,
            identifier,
            expectedType.getValue(),
            parsed.type().getValue());
        throw new CLIDValidationException(
            String.format(
                "%s '%s' has CLID with wrong type. Expected '%s' but found '%s': %s",
                entityName, identifier, expectedType.getValue(), parsed.type().getValue(), clid));
      }
      log.trace("CLID validation passed for {} '{}': {}", entityName, identifier, clid);
    } catch (IllegalArgumentException e) {
      log.warn("Invalid CLID format for {} '{}': {}", entityName, identifier, clid);
      throw new CLIDValidationException(
          String.format(
              "%s '%s' has invalid CLID format: %s - %s",
              entityName, identifier, clid, e.getMessage()));
    }
  }

  private void registerCLID(String clid, Object entity, Map<String, Object> registry) {
    Object existing = registry.putIfAbsent(clid, entity);
    if (existing != null) {
      log.error(
          "Duplicate CLID detected: '{}' already used by {}, attempted to use for {}",
          clid,
          existing.getClass().getSimpleName(),
          entity.getClass().getSimpleName());
      throw new CLIDValidationException(
          String.format(
              "Duplicate CLID '%s' found. Already used by %s, attempted to use for %s",
              clid, existing.getClass().getSimpleName(), entity.getClass().getSimpleName()));
    }
  }

  private Map<Integer, String> buildLocationCLIDMap(List<Location> locations) {
    return Optional.ofNullable(locations)
        .map(
            locs ->
                locs.stream()
                    .filter(loc -> loc.getId() != null && loc.getClid() != null)
                    .collect(Collectors.toMap(Location::getId, Location::getClid, (a, b) -> a)))
        .orElse(Collections.emptyMap());
  }

  private String getClimbIdentifier(Climb climb) {
    return Optional.ofNullable(climb.getRouteName()).orElse("ID:" + climb.getId());
  }

  private String getSessionIdentifier(Session session) {
    return Optional.ofNullable(session.getDate())
        .map(Object::toString)
        .orElse("ID:" + session.getId());
  }

  /** Exception thrown when CLID validation fails. */
  public static class CLIDValidationException extends RuntimeException {
    public CLIDValidationException(String message) {
      super(message);
    }
  }

  /** Context for CLID processing operations */
  @Data
  @Builder
  private static class ProcessingContext {
    private final boolean generateMissing;
    private final boolean validateExisting;
    private final Map<String, Object> clidRegistry;
    private final Map<Integer, String> locationCLIDs;
  }

  /** Result of CLID processing operations */
  @Data
  @Builder
  private static class ProcessingResult {
    private final int generatedCount;
    private final int validatedCount;

    public static ProcessingResult empty() {
      return ProcessingResult.builder().generatedCount(0).validatedCount(0).build();
    }

    public static ProcessingResult combine(ProcessingResult a, ProcessingResult b) {
      return ProcessingResult.builder()
          .generatedCount(a.generatedCount + b.generatedCount)
          .validatedCount(a.validatedCount + b.validatedCount)
          .build();
    }
  }

  /** Functional interface for CLID generation */
  @FunctionalInterface
  private interface CLIDSupplier {
    String generate();

    default boolean canGenerate() {
      return true;
    }
  }

  /** Functional interface for setting CLID on an entity */
  @FunctionalInterface
  private interface CLIDSetter {
    void setClid(String clid);
  }
}
