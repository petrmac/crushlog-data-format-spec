package app.crushlog.cldf.tool.utils;

import java.util.Optional;

/** Utility class for parsing and working with CLID v1 format. CLID v1 format: clid:v1:type:uuid */
public class CLIDUtils {

  private CLIDUtils() {
    // Utility class, prevent instantiation
  }

  /**
   * Determines the entity type from a v1 CLID.
   *
   * @param clid The CLID string in v1 format (clid:v1:type:uuid)
   * @return Optional containing the entity type (e.g., "route", "location"), or empty if invalid
   */
  public static Optional<String> extractEntityType(String clid) {
    if (!isValidV1CLID(clid)) {
      return Optional.empty();
    }

    String[] parts = clid.split(":");
    return Optional.of(parts[2]); // type is at index 2 in v1 format
  }

  /**
   * Extracts the UUID from a v1 CLID.
   *
   * @param clid The CLID string in v1 format (clid:v1:type:uuid)
   * @return Optional containing the UUID portion, or empty if invalid
   */
  public static Optional<String> extractUuid(String clid) {
    if (!isValidV1CLID(clid)) {
      return Optional.empty();
    }

    String[] parts = clid.split(":");
    return Optional.of(parts[3]); // uuid is at index 3 in v1 format
  }

  /**
   * Validates that a CLID is in v1 format.
   *
   * @param clid The CLID string to validate
   * @return true if the CLID is valid v1 format, false otherwise
   */
  public static boolean isValidV1CLID(String clid) {
    if (clid == null || !clid.startsWith("clid:")) {
      return false;
    }

    String[] parts = clid.split(":");

    // Must have exactly 4 parts: clid:v1:type:uuid
    if (parts.length != 4) {
      return false;
    }

    // Second part must be "v1"
    if (!"v1".equals(parts[1])) {
      return false;
    }

    // Third part (type) must not be empty
    if (parts[2].isEmpty()) {
      return false;
    }

    // Fourth part (uuid) must not be empty
    if (parts[3].isEmpty()) {
      return false;
    }

    return true;
  }

  /**
   * Creates a custom URI from a v1 CLID.
   *
   * @param clid The CLID string in v1 format
   * @return Optional containing the custom URI (e.g., "cldf://route/uuid"), or empty if invalid
   */
  public static Optional<String> toCustomUri(String clid) {
    if (!isValidV1CLID(clid)) {
      return Optional.empty();
    }

    Optional<String> type = extractEntityType(clid);
    Optional<String> uuid = extractUuid(clid);

    if (type.isPresent() && uuid.isPresent()) {
      return Optional.of("cldf://" + type.get() + "/" + uuid.get());
    }
    return Optional.empty();
  }
}
