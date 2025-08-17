package app.crushlog.cldf.constants;

/**
 * Constants used throughout the CLDF library. Centralizes commonly used string literals to avoid
 * duplication.
 */
public final class CLDFConstants {

  private CLDFConstants() {
    // Private constructor to prevent instantiation
  }

  // File names
  public static final String MANIFEST_JSON = "manifest.json";
  public static final String LOCATIONS_JSON = "locations.json";
  public static final String SESSIONS_JSON = "sessions.json";
  public static final String CLIMBS_JSON = "climbs.json";
  public static final String ROUTES_JSON = "routes.json";
  public static final String SECTORS_JSON = "sectors.json";
  public static final String TAGS_JSON = "tags.json";
  public static final String MEDIA_METADATA_JSON = "media-metadata.json";
  public static final String CHECKSUMS_JSON = "checksums.json";

  // Schema file names
  public static final String LOCATIONS_SCHEMA_JSON = "locations.schema.json";
  public static final String CLIMBS_SCHEMA_JSON = "climbs.schema.json";
  public static final String ROUTES_SCHEMA_JSON = "routes.schema.json";
  public static final String SECTORS_SCHEMA_JSON = "sectors.schema.json";
  public static final String SESSIONS_SCHEMA_JSON = "sessions.schema.json";
  public static final String TAGS_SCHEMA_JSON = "tags.schema.json";
  public static final String MANIFEST_SCHEMA_JSON = "manifest.schema.json";
  public static final String MEDIA_METADATA_SCHEMA_JSON = "media-metadata.schema.json";
  public static final String CHECKSUMS_SCHEMA_JSON = "checksums.schema.json";

  // Field names
  public static final String FIELD_LOCATIONS = "locations";
  public static final String FIELD_SESSIONS = "sessions";
  public static final String FIELD_CLIMBS = "climbs";
  public static final String FIELD_ROUTES = "routes";
  public static final String FIELD_SECTORS = "sectors";
  public static final String FIELD_TAGS = "tags";
  public static final String FIELD_COUNT = "count";
  public static final String FIELD_STATS = "stats";
  public static final String FIELD_IS_INDOOR = "isIndoor";
  public static final String FIELD_INDOOR_COUNT = "indoorCount";
  public static final String FIELD_OUTDOOR_COUNT = "outdoorCount";
  public static final String FIELD_LOCATION_ID = "locationId";
  public static final String FIELD_SESSION_ID = "sessionId";
  public static final String FIELD_CLIMB_ID = "climbId";
  public static final String FIELD_PLATFORM = "platform";
  public static final String FIELD_CLIMB_TYPE = "climbType";
  public static final String FIELD_ROUTE_TYPE = "routeType";
  public static final String FIELD_BOULDER = "boulder";
  public static final String FIELD_ROUTE = "route";
  public static final String FIELD_V_SCALE = "vScale";
  public static final String FIELD_FORMAT = "format";
  public static final String FIELD_ENUMS = "enums";
  public static final String FIELD_LOCATION = "location";
  public static final String FIELD_CONTAINER = "container";
  public static final String FIELD_FILES = "files";

  // Common values
  public static final String DEFAULT_VERSION = "1.0.0";
  public static final String UNKNOWN = "Unknown";
  public static final String ERROR_PREFIX = "Error: ";

  // Common message parts
  public static final String ID_SUFFIX = " ID";
  public static final String NAME_SUFFIX = " Name";
  public static final String FAILED_TO_SCAN_QR_CODE = "Failed to scan QR code: ";
  public static final String FAILED_TO_EXTRACT_ROUTE = "Failed to extract route: ";
  public static final String FAILED_TO_EXTRACT_LOCATION = "Failed to extract location: ";
  public static final String EXTRACTED_ROUTE = "Extracted route: ";
  public static final String EXTRACTED_LOCATION = "Extracted location: ";
}
