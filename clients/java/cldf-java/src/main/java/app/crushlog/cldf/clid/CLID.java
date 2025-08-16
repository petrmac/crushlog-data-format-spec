package app.crushlog.cldf.clid;

import java.util.UUID;
import java.util.regex.Pattern;

import lombok.NonNull;

/** Represents a parsed CLID (CrushLog ID) */
public record CLID(
    @NonNull String namespace,
    @NonNull String version,
    @NonNull EntityType type,
    @NonNull String uuid,
    @NonNull String fullId,
    @NonNull String shortForm,
    @NonNull String url) {
  private static final Pattern UUID_PATTERN =
      Pattern.compile(
          "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
          Pattern.CASE_INSENSITIVE);

  private static final String EXPECTED_NAMESPACE = "clid";

  /** Factory method to create a CLID from a string with validation */
  public static CLID fromString(String clid) {
    if (clid == null || clid.trim().isEmpty()) {
      throw new IllegalArgumentException("CLID cannot be null or empty");
    }

    String[] parts = clid.split(":");

    if (parts.length != 4) {
      throw new IllegalArgumentException(
          "Invalid CLID format '%s'. Expected format: namespace:version:type:uuid".formatted(clid));
    }

    // Validate namespace
    if (!EXPECTED_NAMESPACE.equals(parts[0])) {
      throw new IllegalArgumentException(
          "Invalid namespace '%s'. Expected '%s'".formatted(parts[0], EXPECTED_NAMESPACE));
    }

    // Validate version
    String version = parts[1];
    if (!version.matches("^v\\d+$")) {
      throw new IllegalArgumentException(
          "Invalid version '%s'. Expected format: v<number> (e.g., v1)".formatted(version));
    }

    // Validate entity type
    EntityType entityType;
    try {
      entityType = EntityType.fromString(parts[2]);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          "Invalid entity type '%s'. Valid types: %s"
              .formatted(parts[2], java.util.Arrays.toString(EntityType.values())));
    }

    // Validate UUID format
    String uuidStr = parts[3];
    if (!UUID_PATTERN.matcher(uuidStr).matches()) {
      throw new IllegalArgumentException("Invalid UUID format '%s'".formatted(uuidStr));
    }

    // Validate it's a parseable UUID
    try {
      UUID.fromString(uuidStr);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          "Invalid UUID '%s': %s".formatted(uuidStr, e.getMessage()));
    }

    final String substring = uuidStr.substring(0, Math.min(uuidStr.length(), 8));
    return new CLID(
        parts[0],
        version,
        entityType,
        uuidStr,
        clid,
        substring,
        "https://crushlog.pro/g/%s".formatted(substring));
  }

  /** Check if a string is a valid CLID */
  public static boolean isValid(String clid) {
    try {
      fromString(clid);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  @Override
  @NonNull
  public String toString() {
    return fullId;
  }
}
