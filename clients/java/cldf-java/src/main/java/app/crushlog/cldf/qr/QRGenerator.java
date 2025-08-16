package app.crushlog.cldf.qr;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

import app.crushlog.cldf.clid.CLID;
import app.crushlog.cldf.clid.CLIDGenerator;
import app.crushlog.cldf.models.Location;
import app.crushlog.cldf.models.Route;
import app.crushlog.cldf.models.enums.RouteType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Generates QR code data payloads for CLDF entities. Supports both v1 (legacy) and v2 (CLID-based)
 * formats.
 */
@Slf4j
public class QRGenerator {

  private static final String PROTOCOL_VERSION = "1";
  private static final String DEFAULT_BASE_URL = "https://crushlog.pro";
  private static final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Generate a hybrid QR code payload for a route with both embedded data and online reference.
   *
   * @param route The route to generate QR code for
   * @param baseUrl The base URL for online references
   * @param options QR generation options
   * @return JSON string containing the QR code payload
   */
  public static String generateHybrid(Route route, String baseUrl, QROptions options) {
    Objects.requireNonNull(route, "Route cannot be null");
    Objects.requireNonNull(options, "Options cannot be null");

    String effectiveBaseUrl = baseUrl != null ? baseUrl : DEFAULT_BASE_URL;

    // Ensure route has a CLID
    String routeClid = route.getClid();
    if (routeClid == null || routeClid.isEmpty()) {
      log.warn("Route {} missing CLID, generating one", route.getName());
      routeClid = CLIDGenerator.generateRandomCLID(CLIDGenerator.EntityType.ROUTE);
    }

    try {
      ObjectNode payload = objectMapper.createObjectNode();

      // Protocol version
      payload.put("v", PROTOCOL_VERSION);

      // CLID
      payload.put("clid", routeClid);

      // URL with short CLID (first 8 chars of UUID)
      String shortClid = extractShortClid(routeClid);
      String url = String.format("%s/g/%s", effectiveBaseUrl, shortClid);
      payload.put("url", url);

      // IPFS hash if enabled
      if (options.isIncludeIPFS() && options.getIpfsHash() != null) {
        payload.put("cldf", options.getIpfsHash());
      }

      // Route data
      ObjectNode routeData = objectMapper.createObjectNode();
      routeData.put("name", route.getName());

      // Add grade if available
      if (route.getGrades() != null) {
        String grade = extractPrimaryGrade(route);
        if (grade != null) {
          routeData.put("grade", grade);
        }
      }

      // Add route type
      if (route.getRouteType() != null) {
        routeData.put("type", route.getRouteType().toString().toLowerCase());
      }

      // Add height if available
      if (route.getHeight() != null) {
        routeData.put("height", route.getHeight());
      }

      payload.set("route", routeData);

      // Add location data if available
      if (options.getLocation() != null) {
        ObjectNode locationData = createLocationNode(options.getLocation());
        payload.set("loc", locationData);
      }

      // Add metadata
      ObjectNode metadata = objectMapper.createObjectNode();
      metadata.put("created", System.currentTimeMillis());

      if (options.isBlockchainRecord()
          && options.getLocation() != null
          && !options.getLocation().getIsIndoor()) {
        metadata.put("blockchain", true);
        if (options.getBlockchainNetwork() != null) {
          metadata.put("network", options.getBlockchainNetwork());
        }
      }

      payload.set("meta", metadata);

      return objectMapper.writeValueAsString(payload);

    } catch (Exception e) {
      log.error("Failed to generate QR code payload for route: {}", route.getName(), e);
      throw new QRGenerationException("Failed to generate QR code payload", e);
    }
  }

  /**
   * Generate a simple QR code with just the URL.
   *
   * @param route The route to generate QR code for
   * @param baseUrl The base URL
   * @return URL string for the QR code
   */
  public static String generateSimple(Route route, String baseUrl) {
    Objects.requireNonNull(route, "Route cannot be null");

    String effectiveBaseUrl = baseUrl != null ? baseUrl : DEFAULT_BASE_URL;

    // Use CLID if available
    if (route.getClid() != null) {
      String shortClid = extractShortClid(route.getClid());
      return String.format("%s/g/%s", effectiveBaseUrl, shortClid);
    }

    throw new QRGenerationException("Route must have CLID for QR generation");
  }

  /**
   * Generate a custom URI scheme QR code for mobile apps.
   *
   * @param route The route to generate QR code for
   * @param options QR generation options
   * @return Custom URI string
   */
  public static String generateCustomURI(Route route, QROptions options) {
    Objects.requireNonNull(route, "Route cannot be null");
    Objects.requireNonNull(options, "Options cannot be null");

    if (route.getClid() == null) {
      throw new QRGenerationException("Route must have CLID for URI generation");
    }

    String uuid = extractUUID(route.getClid());
    StringBuilder uri = new StringBuilder("cldf://global/route/");
    uri.append(uuid);
    uri.append("?v=1");

    if (options.getIpfsHash() != null) {
      uri.append("&cldf=").append(URLEncoder.encode(options.getIpfsHash(), StandardCharsets.UTF_8));
    }

    if (route.getName() != null) {
      uri.append("&name=").append(URLEncoder.encode(route.getName(), StandardCharsets.UTF_8));
    }

    String grade = extractPrimaryGrade(route);
    if (grade != null) {
      uri.append("&grade=").append(URLEncoder.encode(grade, StandardCharsets.UTF_8));
    }

    return uri.toString();
  }

  /**
   * Generate QR code for a location.
   *
   * @param location The location to generate QR code for
   * @param baseUrl The base URL
   * @return JSON string containing the QR code payload
   */
  public static String generateLocationQR(Location location, String baseUrl) {
    Objects.requireNonNull(location, "Location cannot be null");

    String effectiveBaseUrl = baseUrl != null ? baseUrl : DEFAULT_BASE_URL;

    // Ensure location has a CLID
    String locationClid = location.getClid();
    if (locationClid == null || locationClid.isEmpty()) {
      log.warn("Location {} missing CLID, generating one", location.getName());
      locationClid = CLIDGenerator.generateRandomCLID(CLIDGenerator.EntityType.LOCATION);
    }

    try {
      ObjectNode payload = objectMapper.createObjectNode();

      payload.put("v", PROTOCOL_VERSION);
      payload.put("clid", locationClid);

      String shortClid = extractShortClid(locationClid);
      String url = String.format("%s/l/%s", effectiveBaseUrl, shortClid);
      payload.put("url", url);

      ObjectNode locationData = createLocationNode(location);
      payload.set("location", locationData);

      return objectMapper.writeValueAsString(payload);

    } catch (Exception e) {
      log.error("Failed to generate QR code payload for location: {}", location.getName(), e);
      throw new QRGenerationException("Failed to generate location QR code payload", e);
    }
  }

  private static ObjectNode createLocationNode(Location location) {
    ObjectNode locationData = objectMapper.createObjectNode();

    if (location.getClid() != null) {
      locationData.put("clid", location.getClid());
    }

    locationData.put("name", location.getName());

    if (location.getCountry() != null) {
      locationData.put("country", location.getCountry());
    }

    if (location.getState() != null) {
      locationData.put("state", location.getState());
    }

    if (location.getCity() != null) {
      locationData.put("city", location.getCity());
    }

    locationData.put("indoor", location.getIsIndoor());

    return locationData;
  }

  private static String extractPrimaryGrade(Route route) {
    if (route.getGrades() == null) {
      return null;
    }

    // Return first available grade based on route type
    if (route.getRouteType() == RouteType.BOULDER) {
      if (route.getGrades().getVScale() != null) return route.getGrades().getVScale();
      if (route.getGrades().getFont() != null) return route.getGrades().getFont();
    } else {
      if (route.getGrades().getYds() != null) return route.getGrades().getYds();
      if (route.getGrades().getFrench() != null) return route.getGrades().getFrench();
      if (route.getGrades().getUiaa() != null) return route.getGrades().getUiaa();
    }

    // Return any available grade if type-specific not found
    return Optional.ofNullable(route.getGrades().getYds())
        .or(() -> Optional.ofNullable(route.getGrades().getFrench()))
        .or(() -> Optional.ofNullable(route.getGrades().getUiaa()))
        .or(() -> Optional.ofNullable(route.getGrades().getVScale()))
        .or(() -> Optional.ofNullable(route.getGrades().getFont()))
        .orElse(null);
  }

  private static String extractShortClid(String clid) {
    try {
      // Parse CLID to extract UUID
      CLID parsed = CLID.fromString(clid);
      String uuid = parsed.uuid();
      // Take first 8 chars of UUID
      return uuid.substring(0, Math.min(8, uuid.length()));
    } catch (Exception e) {
      // Fallback: Try to extract UUID part manually for v1 format
      if (clid != null && clid.startsWith("clid:v1:")) {
        String[] parts = clid.split(":");
        if (parts.length == 4) {
          String uuid = parts[3];
          return uuid.substring(0, Math.min(8, uuid.length()));
        }
      }
      log.error("Failed to extract short CLID from: {}", clid, e);
      return clid; // Return full CLID as fallback
    }
  }

  private static String extractUUID(String clid) {
    try {
      CLID parsed = CLID.fromString(clid);
      return parsed.uuid();
    } catch (Exception e) {
      log.error("Failed to extract UUID from CLID: {}", clid, e);
      throw new QRGenerationException("Invalid CLID format: " + clid, e);
    }
  }

  /** Options for QR code generation. */
  @Data
  @Builder
  public static class QROptions {
    @Builder.Default private boolean includeIPFS = false;

    private String ipfsHash;

    @Builder.Default private boolean blockchainRecord = false;

    private String blockchainNetwork;

    private Location location;

    @Builder.Default private boolean includeThumbnail = false;

    @Builder.Default private int qrSize = 256;

    @Builder.Default private String errorCorrectionLevel = "M";
  }

  /** Exception thrown when QR code generation fails. */
  public static class QRGenerationException extends RuntimeException {
    public QRGenerationException(String message) {
      super(message);
    }

    public QRGenerationException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
