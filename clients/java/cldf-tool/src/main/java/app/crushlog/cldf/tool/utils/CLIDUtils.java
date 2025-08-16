package app.crushlog.cldf.tool.utils;

import java.util.Optional;

/**
 * Utility class for parsing and validating Crushlog IDs (CLIDs).
 *
 * <p>CLIDs are unique identifiers for climbing entities in the Crushlog ecosystem. They follow a
 * versioned format to ensure forward compatibility while maintaining backward compatibility where
 * possible.
 *
 * <h2>CLID Format</h2>
 *
 * <p>The v1 CLID format is: {@code clid:v1:type:uuid}
 *
 * <ul>
 *   <li><b>clid</b> - Fixed prefix identifying this as a CLID
 *   <li><b>v1</b> - Version identifier (currently only v1 is supported)
 *   <li><b>type</b> - Entity type (route, location, sector, climb, or session)
 *   <li><b>uuid</b> - Unique identifier for the entity
 * </ul>
 *
 * <h2>Example CLIDs</h2>
 *
 * <pre>{@code
 * clid:v1:route:550e8400-e29b-41d4-a716-446655440000     // A climbing route
 * clid:v1:location:660e8400-e29b-41d4-a716-446655440000  // A climbing location
 * clid:v1:sector:770e8400-e29b-41d4-a716-446655440000    // A sector within a location
 * clid:v1:climb:880e8400-e29b-41d4-a716-446655440000     // An individual climb attempt
 * clid:v1:session:990e8400-e29b-41d4-a716-446655440000   // A climbing session
 * }</pre>
 *
 * <h2>Usage Examples</h2>
 *
 * <pre>{@code
 * String clid = "clid:v1:route:550e8400-e29b-41d4-a716-446655440000";
 *
 * // Validate CLID format
 * if (CLIDUtils.isValidV1CLID(clid)) {
 *     // Extract entity type
 *     Optional<String> type = CLIDUtils.extractEntityType(clid);
 *     type.ifPresent(t -> System.out.println("Type: " + t)); // Prints: Type: route
 *
 *     // Extract UUID
 *     Optional<String> uuid = CLIDUtils.extractUuid(clid);
 *     uuid.ifPresent(u -> System.out.println("UUID: " + u)); // Prints the UUID
 *
 *     // Convert to custom URI
 *     Optional<String> uri = CLIDUtils.toCustomUri(clid);
 *     uri.ifPresent(u -> System.out.println("URI: " + u)); // Prints: URI: cldf://route/550e8400...
 * }
 * }</pre>
 *
 * <p><b>Note:</b> This implementation only supports the v1 CLID format. Legacy formats without
 * version identifiers are not supported to ensure data consistency and forward compatibility.
 *
 * @since 1.0.0
 * @author CLDF Development Team
 */
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
