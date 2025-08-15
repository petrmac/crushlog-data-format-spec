package app.crushlog.cldf.qr.impl;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import app.crushlog.cldf.clid.CLID;
import app.crushlog.cldf.clid.CLIDGenerator;
import app.crushlog.cldf.models.Location;
import app.crushlog.cldf.models.Route;
import app.crushlog.cldf.models.enums.RouteType;
import app.crushlog.cldf.qr.QRCodeData;
import app.crushlog.cldf.qr.QROptions;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

/**
 * Generates QR code data payloads. This class is responsible for creating the data structure only,
 * not the actual QR code image.
 */
@Slf4j
public class QRDataGenerator {

  private static final String PROTOCOL_VERSION = "1";
  private static final String DEFAULT_BASE_URL = "https://crushlog.pro";
  private final ObjectMapper objectMapper = new ObjectMapper();

  /** Generate QR code data for a route. */
  public QRCodeData generateRouteData(Route route, QROptions options) {
    Objects.requireNonNull(route, "Route cannot be null");
    Objects.requireNonNull(options, "Options cannot be null");

    String routeClid = ensureClid(route);
    String baseUrl = options.getBaseUrl() != null ? options.getBaseUrl() : DEFAULT_BASE_URL;

    QRCodeData.QRCodeDataBuilder builder =
        QRCodeData.builder()
            .version(Integer.parseInt(PROTOCOL_VERSION))
            .clid(routeClid)
            .url(buildUrl(routeClid, baseUrl));

    if (options.isIncludeIPFS() && options.getIpfsHash() != null) {
      builder.ipfsHash(options.getIpfsHash());
    }

    builder.routeData(buildRouteData(route));

    if (options.getLocation() != null) {
      builder.locationData(buildLocationData(options.getLocation()));
    }

    builder.metadata(buildMetadata(options, options.getLocation()));

    return builder.build();
  }

  /** Generate QR code data for a location. */
  public QRCodeData generateLocationData(Location location, QROptions options) {
    Objects.requireNonNull(location, "Location cannot be null");
    Objects.requireNonNull(options, "Options cannot be null");

    String locationClid = ensureLocationClid(location);
    String baseUrl = options.getBaseUrl() != null ? options.getBaseUrl() : DEFAULT_BASE_URL;

    return QRCodeData.builder()
        .version(Integer.parseInt(PROTOCOL_VERSION))
        .clid(locationClid)
        .url(buildLocationUrl(locationClid, baseUrl))
        .locationData(buildLocationData(location))
        .metadata(buildMetadata(options, location))
        .build();
  }

  /** Convert QRCodeData to JSON string. */
  public String toJson(QRCodeData data) {
    try {
      ObjectNode root = objectMapper.createObjectNode();

      root.put("v", data.getVersion());
      root.put("clid", data.getClid());
      root.put("url", data.getUrl());

      if (data.getIpfsHash() != null) {
        root.put("cldf", data.getIpfsHash());
      }

      if (data.getRouteData() != null) {
        root.set("route", objectMapper.valueToTree(data.getRouteData()));
      }

      if (data.getLocationData() != null) {
        root.set("loc", objectMapper.valueToTree(data.getLocationData()));
      }

      if (data.getMetadata() != null) {
        root.set("meta", objectMapper.valueToTree(data.getMetadata()));
      }

      return objectMapper.writeValueAsString(root);
    } catch (Exception e) {
      log.error("Failed to serialize QR code data", e);
      throw new RuntimeException("Failed to serialize QR code data", e);
    }
  }

  /** Generate simple URL for QR code. */
  public String generateSimpleUrl(Route route, QROptions options) {
    String routeClid = ensureClid(route);
    String baseUrl = options.getBaseUrl() != null ? options.getBaseUrl() : DEFAULT_BASE_URL;
    return buildUrl(routeClid, baseUrl);
  }

  /** Generate custom URI for mobile apps. */
  public String generateCustomUri(Route route, QROptions options) {
    String routeClid = ensureClid(route);
    String uuid = extractUuid(routeClid);

    try {
      StringBuilder uri = new StringBuilder("cldf://global/route/");
      uri.append(uuid);
      uri.append("?v=").append(PROTOCOL_VERSION);

      if (options.getIpfsHash() != null) {
        uri.append("&cldf=")
            .append(URLEncoder.encode(options.getIpfsHash(), StandardCharsets.UTF_8.name()));
      }

      if (route.getName() != null) {
        uri.append("&name=")
            .append(URLEncoder.encode(route.getName(), StandardCharsets.UTF_8.name()));
      }

      Map<String, String> gradeInfo = extractGradeWithSystem(route);
      if (gradeInfo != null) {
        uri.append("&grade=")
            .append(URLEncoder.encode(gradeInfo.get("value"), StandardCharsets.UTF_8.name()));
        uri.append("&gradeSystem=")
            .append(URLEncoder.encode(gradeInfo.get("system"), StandardCharsets.UTF_8.name()));
      }

      return uri.toString();
    } catch (Exception e) {
      throw new RuntimeException("Failed to generate custom URI", e);
    }
  }

  private String ensureClid(Route route) {
    String clid = route.getClid();
    if (clid == null || clid.isEmpty()) {
      log.warn("Route {} missing CLID, generating one", route.getName());
      clid = CLIDGenerator.generateRandomCLID(CLIDGenerator.EntityType.ROUTE);
    }
    return clid;
  }

  private String ensureLocationClid(Location location) {
    String clid = location.getClid();
    if (clid == null || clid.isEmpty()) {
      log.warn("Location {} missing CLID, generating one", location.getName());
      clid = CLIDGenerator.generateRandomCLID(CLIDGenerator.EntityType.LOCATION);
    }
    return clid;
  }

  private String buildUrl(String clid, String baseUrl) {
    String shortClid = extractShortClid(clid);
    return String.format("%s/g/%s", baseUrl, shortClid);
  }

  private String buildLocationUrl(String clid, String baseUrl) {
    String shortClid = extractShortClid(clid);
    return String.format("%s/l/%s", baseUrl, shortClid);
  }

  private Map<String, Object> buildRouteData(Route route) {
    Map<String, Object> data = new HashMap<>();
    data.put("name", route.getName());

    // Store grade with its system
    Map<String, String> gradeInfo = extractGradeWithSystem(route);
    if (gradeInfo != null) {
      data.put("grade", gradeInfo.get("value"));
      data.put("gradeSystem", gradeInfo.get("system"));
    }

    if (route.getRouteType() != null) {
      data.put("type", route.getRouteType().toString().toLowerCase());
    }

    if (route.getHeight() != null) {
      data.put("height", route.getHeight());
    }

    return data;
  }

  private Map<String, Object> buildLocationData(Location location) {
    Map<String, Object> data = new HashMap<>();

    if (location.getClid() != null) {
      data.put("clid", location.getClid());
    }

    data.put("name", location.getName());

    if (location.getCountry() != null) {
      data.put("country", location.getCountry());
    }

    if (location.getState() != null) {
      data.put("state", location.getState());
    }

    if (location.getCity() != null) {
      data.put("city", location.getCity());
    }

    data.put("indoor", location.getIsIndoor());

    return data;
  }

  private Map<String, Object> buildMetadata(QROptions options, Location location) {
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("created", System.currentTimeMillis());

    if (options.isBlockchainRecord() && location != null && !location.getIsIndoor()) {
      metadata.put("blockchain", true);
      if (options.getBlockchainNetwork() != null) {
        metadata.put("network", options.getBlockchainNetwork());
      }
    }

    return metadata;
  }

  private String extractPrimaryGrade(Route route) {
    Map<String, String> gradeInfo = extractGradeWithSystem(route);
    return gradeInfo != null ? gradeInfo.get("value") : null;
  }

  private Map<String, String> extractGradeWithSystem(Route route) {
    if (route.getGrades() == null) {
      return null;
    }

    Map<String, String> gradeInfo = new HashMap<>();

    // Priority based on route type
    if (route.getRouteType() == RouteType.BOULDER) {
      if (route.getGrades().getVScale() != null) {
        gradeInfo.put("value", route.getGrades().getVScale());
        gradeInfo.put("system", "vScale");
        return gradeInfo;
      }
      if (route.getGrades().getFont() != null) {
        gradeInfo.put("value", route.getGrades().getFont());
        gradeInfo.put("system", "font");
        return gradeInfo;
      }
    }

    // For non-boulder routes or fallback
    if (route.getGrades().getYds() != null) {
      gradeInfo.put("value", route.getGrades().getYds());
      gradeInfo.put("system", "yds");
      return gradeInfo;
    }
    if (route.getGrades().getFrench() != null) {
      gradeInfo.put("value", route.getGrades().getFrench());
      gradeInfo.put("system", "french");
      return gradeInfo;
    }
    if (route.getGrades().getUiaa() != null) {
      gradeInfo.put("value", route.getGrades().getUiaa());
      gradeInfo.put("system", "uiaa");
      return gradeInfo;
    }
    if (route.getGrades().getVScale() != null) {
      gradeInfo.put("value", route.getGrades().getVScale());
      gradeInfo.put("system", "vScale");
      return gradeInfo;
    }
    if (route.getGrades().getFont() != null) {
      gradeInfo.put("value", route.getGrades().getFont());
      gradeInfo.put("system", "font");
      return gradeInfo;
    }

    return null;
  }

  private String extractShortClid(String clid) {
    try {
      // Parse CLID to extract UUID
      CLID parsed = CLID.fromString(clid);
      String uuid = parsed.uuid();
      return uuid.substring(0, Math.min(8, uuid.length()));
    } catch (Exception e) {
      // Fallback for old or malformed CLIDs
      String[] parts = clid.split(":");
      if (parts.length == 3) {
        // Old format: clid:type:uuid
        String uuid = parts[2];
        return uuid.substring(0, Math.min(8, uuid.length()));
      } else if (parts.length == 4) {
        // New format: clid:v1:type:uuid
        String uuid = parts[3];
        return uuid.substring(0, Math.min(8, uuid.length()));
      }
      log.error("Failed to extract short CLID from: {}", clid, e);
      return clid;
    }
  }

  private String extractUuid(String clid) {
    try {
      CLID parsed = CLID.fromString(clid);
      return parsed.uuid();
    } catch (Exception e) {
      log.error("Failed to extract UUID from CLID: {}", clid, e);
      throw new RuntimeException("Invalid CLID format: " + clid, e);
    }
  }
}
