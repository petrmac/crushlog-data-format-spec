package app.crushlog.cldf.clid;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import app.crushlog.cldf.clid.RouteModel.*;

/**
 * CLID (CrushLog ID) Generator Generates globally unique, deterministic identifiers for climbing
 * routes and locations Based on UUID v5 for deterministic IDs and UUID v4 for random IDs
 */
public class CLIDGenerator {

  // Current CLID version
  public static final String CURRENT_VERSION = "v1";

  // CrushLog namespace UUID (registered for climbing data)
  public static final UUID CRUSHLOG_NAMESPACE =
      UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8");

  /**
   * Format a CLID with the current version
   *
   * @param type Entity type
   * @param uuid UUID string
   * @return Formatted CLID string
   */
  private static String formatCLID(EntityType type, UUID uuid) {
    return "clid:%s:%s:%s".formatted(CURRENT_VERSION, type.value, uuid);
  }

  /**
   * Format a CLID with the current version using type string
   *
   * @param typeStr Entity type string
   * @param uuid UUID string
   * @return Formatted CLID string
   */
  private static String formatCLID(String typeStr, UUID uuid) {
    return "clid:%s:%s:%s".formatted(CURRENT_VERSION, typeStr, uuid);
  }

  /** Generate a deterministic CLID for a location */
  public static String generateLocationCLID(Location location) {
    // Validate required fields
    ValidationResult validation = validateLocation(location);
    if (!validation.isValid()) {
      throw new IllegalArgumentException(
          "Invalid location data: " + String.join(", ", validation.getErrors()));
    }

    // Build deterministic components
    List<String> components =
        Arrays.asList(
            location.country().toUpperCase(),
            location.state() != null ? location.state().toLowerCase() : "",
            location.city() != null ? normalizeString(location.city()) : "",
            normalizeString(location.name()),
            "%.6f".formatted(location.coordinates().lat()),
            "%.6f".formatted(location.coordinates().lon()),
            location.isIndoor() ? "indoor" : "outdoor");

    String input = components.stream().filter(s -> !s.isEmpty()).collect(Collectors.joining(":"));

    UUID uuid = generateUUIDv5(input);

    return formatCLID(EntityType.LOCATION, uuid);
  }

  /** Generate a deterministic CLID for a route */
  public static String generateRouteCLID(String locationCLID, RouteModel.Route route) {
    // Validate required fields
    ValidationResult validation = validateRoute(route);
    if (!validation.isValid()) {
      throw new IllegalArgumentException(
          "Invalid route data: " + String.join(", ", validation.getErrors()));
    }

    // Extract location UUID (remove prefix)
    // Handle both v1 format and potential legacy format
    String locationUuid =
        locationCLID.contains(":location:")
            ? locationCLID.substring(locationCLID.lastIndexOf(':') + 1)
            : locationCLID;

    // Build deterministic components
    List<String> components =
        Arrays.asList(
            locationUuid,
            normalizeString(route.name()),
            standardizeGrade(route.grade()),
            route.firstAscent() != null && route.firstAscent().year() != null
                ? route.firstAscent().year().toString()
                : "",
            route.firstAscent() != null && route.firstAscent().name() != null
                ? normalizeString(route.firstAscent().name())
                : "",
            route.height() != null ? "%.1f".formatted(route.height()) : "",
            route.type().toString());

    String input = String.join(":", components);
    UUID uuid = generateUUIDv5(input);

    return formatCLID(EntityType.ROUTE, uuid);
  }

  /** Generate a deterministic CLID for a sector */
  public static String generateSectorCLID(String locationCLID, Sector sector) {
    // Extract location UUID (handle v1 format)
    String locationUuid =
        locationCLID.contains(":location:")
            ? locationCLID.substring(locationCLID.lastIndexOf(':') + 1)
            : locationCLID;

    List<String> components =
        Arrays.asList(
            locationUuid,
            normalizeString(sector.name()),
            sector.order() != null ? sector.order().toString() : "0");

    String input = String.join(":", components);
    UUID uuid = generateUUIDv5(input);

    return formatCLID(EntityType.SECTOR, uuid);
  }

  /** Generate a random UUID v4 for user content */
  public static String generateRandomCLID(EntityType type) {
    UUID uuid = UUID.randomUUID();
    return formatCLID(type, uuid);
  }

  /** Generate UUID v5 (deterministic) from input string */
  private static UUID generateUUIDv5(String input) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-1");
      md.update(toBytes());
      byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));

      // Set version (5) and variant bits
      hash[6] = (byte) ((hash[6] & 0x0f) | 0x50); // Version 5
      hash[8] = (byte) ((hash[8] & 0x3f) | 0x80); // Variant 10

      return toUUID(Arrays.copyOf(hash, 16));
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-1 algorithm not available", e);
    }
  }

  /** Convert UUID to byte array */
  private static byte[] toBytes() {
    UUID uuid = CRUSHLOG_NAMESPACE;
    long msb = uuid.getMostSignificantBits();
    long lsb = uuid.getLeastSignificantBits();
    byte[] buffer = new byte[16];

    for (int i = 0; i < 8; i++) {
      buffer[i] = (byte) (msb >>> 8 * (7 - i));
      buffer[i + 8] = (byte) (lsb >>> 8 * (7 - i));
    }

    return buffer;
  }

  /** Convert byte array to UUID */
  private static UUID toUUID(byte[] hash) {
    long msb = 0;
    long lsb = 0;

    for (int i = 0; i < 8; i++) {
      msb = (msb << 8) | (hash[i] & 0xff);
    }
    for (int i = 8; i < 16; i++) {
      lsb = (lsb << 8) | (hash[i] & 0xff);
    }

    return new UUID(msb, lsb);
  }

  /**
   * Validate a CLID string
   *
   * @param clid The CLID string to validate
   * @return true if valid, false otherwise
   */
  public static boolean validate(String clid) {
    return CLID.isValid(clid);
  }

  /**
   * Parse a CLID string into a CLID object
   *
   * @param clid The CLID string to parse
   * @return The parsed CLID object
   * @throws IllegalArgumentException if the CLID is invalid
   */
  public static CLID parse(String clid) {
    return CLID.fromString(clid);
  }

  /**
   * Extract the short form (first 8 characters of UUID) from a CLID
   *
   * @param clid The full CLID string
   * @return The 8-character short form
   */
  public static String toShortForm(String clid) {
    CLID parsed = CLID.fromString(clid);
    return parsed.shortForm();
  }

  /** Normalize string for consistent ID generation */
  private static String normalizeString(String input) {
    return input
        .toLowerCase()
        .trim()
        .replaceAll("\\s+", "-")
        .replaceAll("[^\\w\\-]", "")
        .replaceAll("-+", "-")
        .replaceAll("^-|-$", "");
  }

  /** Standardize grade format */
  private static String standardizeGrade(String grade) {
    return grade.replaceAll("\\s", "").toLowerCase();
  }

  /** Validate location data */
  private static ValidationResult validateLocation(Location location) {
    ValidationResult result = new ValidationResult();

    if (location.country() == null || location.country().length() != 2) {
      result.addError("Country must be ISO 3166-1 alpha-2 code");
    }

    if (location.name() == null || location.name().trim().isEmpty()) {
      result.addError("Location name is required");
    }

    if (location.coordinates() == null) {
      result.addError("Coordinates are required");
    } else {
      if (Math.abs(location.coordinates().lat()) > 90) {
        result.addError("Latitude must be between -90 and 90");
      }
      if (Math.abs(location.coordinates().lon()) > 180) {
        result.addError("Longitude must be between -180 and 180");
      }
    }

    return result;
  }

  /** Validate route data */
  private static ValidationResult validateRoute(RouteModel.Route route) {
    ValidationResult result = new ValidationResult();

    if (route.name() == null || route.name().trim().isEmpty()) {
      result.addError("Route name is required");
    }

    if (route.grade() == null || route.grade().trim().isEmpty()) {
      result.addError("Route grade is required");
    }

    if (route.type() == null) {
      result.addError("Route type is required");
    }

    // Warnings for optional but recommended fields
    if (route.firstAscent() == null && route.type() != RouteType.BOULDER) {
      result.addWarning("First ascent year recommended for historical routes");
    }

    if (route.height() == null && route.type() != RouteType.BOULDER) {
      result.addWarning("Route height recommended for rope routes");
    }

    return result;
  }
}
