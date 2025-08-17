package app.crushlog.cldf.qr.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.crushlog.cldf.models.Location;
import app.crushlog.cldf.models.Route;
import app.crushlog.cldf.models.enums.RouteType;
import app.crushlog.cldf.qr.*;
import app.crushlog.cldf.qr.model.QRPayload;
import app.crushlog.cldf.qr.result.QRError;
import app.crushlog.cldf.qr.result.Result;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import lombok.extern.slf4j.Slf4j;

/**
 * Functional implementation of QR code scanner using Result types. This implementation avoids
 * throwing exceptions and instead models errors as data. Uses pure Java image reading to eliminate
 * AWT dependencies.
 */
@Slf4j
public class DefaultQRScanner implements QRScanner {

  private static final ObjectMapper objectMapper =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  private static final Pattern URL_PATTERN = Pattern.compile("https?://[^/]+/g/([a-zA-Z0-9-]+)");
  private static final Pattern URI_PATTERN = Pattern.compile("cldf://global/route/([a-zA-Z0-9-]+)");

  private final PureJavaImageReader imageReader = new PureJavaImageReader();
  private final QRCodeReader qrCodeReader = new QRCodeReader();

  @Override
  public Result<ParsedQRData, QRError> parse(String data) {
    if (data == null || data.trim().isEmpty()) {
      return Result.failure(QRError.parseError("QR data cannot be null or empty"));
    }

    String trimmedData = data.trim();

    // Try to parse as JSON first
    if (trimmedData.startsWith("{")) {
      return parseJsonData(trimmedData);
    }

    // Try to parse as URL
    if (trimmedData.startsWith("http://") || trimmedData.startsWith("https://")) {
      return parseUrlData(trimmedData);
    }

    // Try to parse as custom URI
    if (trimmedData.startsWith("cldf://")) {
      return parseUriData(trimmedData);
    }

    return Result.failure(
        QRError.of(
            QRError.ErrorType.INVALID_FORMAT,
            "Unrecognized QR code format",
            trimmedData.substring(0, Math.min(50, trimmedData.length()))));
  }

  // BufferedImage scan method is deprecated and moved to interface default

  @Override
  public Result<ParsedQRData, QRError> scan(byte[] imageBytes) {
    if (imageBytes == null || imageBytes.length == 0) {
      return Result.failure(QRError.scanError("Image bytes cannot be null or empty"));
    }

    try {
      // Create LuminanceSource from PNG bytes using pure Java
      LuminanceSource source = imageReader.createLuminanceSource(imageBytes);

      // Create binary bitmap
      BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

      // Decode QR code
      com.google.zxing.Result zxingResult = qrCodeReader.decode(bitmap);

      // Parse the decoded text
      String decodedText = zxingResult.getText();
      return parse(decodedText);

    } catch (NotFoundException e) {
      return Result.failure(QRError.scanError("No QR code found in image"));
    } catch (ChecksumException e) {
      return Result.failure(QRError.scanError("QR code checksum validation failed"));
    } catch (FormatException e) {
      return Result.failure(QRError.scanError("Invalid QR code format"));
    } catch (IOException e) {
      return Result.failure(QRError.scanError("Failed to read image: " + e.getMessage()));
    } catch (Exception e) {
      log.error("Unexpected error scanning QR code", e);
      return Result.failure(QRError.scanError("Failed to scan QR code: " + e.getMessage()));
    }
  }

  @Override
  public Result<Route, QRError> toRoute(ParsedQRData data) {
    if (data.getRoute() == null) {
      return Result.failure(
          QRError.of(QRError.ErrorType.MISSING_DATA, "No route data found in QR code"));
    }

    return Result.tryExecute(
        () -> buildRouteFromData(data),
        e ->
            QRError.of(
                QRError.ErrorType.PARSE_ERROR, "Failed to convert to Route", e.getMessage()));
  }

  /** Builds a Route object from parsed QR data. */
  private Route buildRouteFromData(ParsedQRData data) {
    Route.RouteBuilder builder = Route.builder();

    setRouteBasicFields(builder, data);
    setRouteGrades(builder, data.getRoute());
    setRouteLocationId(builder, data);

    return builder.build();
  }

  /** Sets basic route fields from QR data. */
  private void setRouteBasicFields(Route.RouteBuilder builder, ParsedQRData data) {
    if (data.getClid() != null) {
      builder.clid(data.getClid());
    }

    ParsedQRData.RouteInfo routeInfo = data.getRoute();

    if (routeInfo.getId() != null) {
      builder.id(routeInfo.getId());
    }
    if (routeInfo.getName() != null) {
      builder.name(routeInfo.getName());
    }
    if (routeInfo.getType() != null) {
      RouteType type =
          "boulder".equalsIgnoreCase(routeInfo.getType()) ? RouteType.BOULDER : RouteType.ROUTE;
      builder.routeType(type);
    }
    if (routeInfo.getHeight() != null) {
      builder.height(routeInfo.getHeight());
    }
  }

  /** Sets route grades if available. */
  private void setRouteGrades(Route.RouteBuilder builder, ParsedQRData.RouteInfo routeInfo) {
    if (routeInfo.getGrade() != null && routeInfo.getGradeSystem() != null) {
      Route.Grades.GradesBuilder gradesBuilder = Route.Grades.builder();
      setGradeBySystem(routeInfo.getGrade(), routeInfo.getGradeSystem(), gradesBuilder);
      builder.grades(gradesBuilder.build());
    } else if (routeInfo.getGrade() != null) {
      log.warn("Grade without grade system found, ignoring: {}", routeInfo.getGrade());
    }
  }

  /** Sets location ID if available. */
  private void setRouteLocationId(Route.RouteBuilder builder, ParsedQRData data) {
    if (data.getLocation() != null && data.getLocation().getId() != null) {
      builder.locationId(data.getLocation().getId());
    }
  }

  @Override
  public Result<Location, QRError> toLocation(ParsedQRData data) {
    if (data.getLocation() == null) {
      return Result.failure(
          QRError.of(QRError.ErrorType.MISSING_DATA, "No location data found in QR code"));
    }

    return Result.tryExecute(
        () -> {
          ParsedQRData.LocationInfo locInfo = data.getLocation();
          Location.LocationBuilder builder = Location.builder();

          if (locInfo.getClid() != null) {
            builder.clid(locInfo.getClid());
          }

          if (locInfo.getId() != null) {
            builder.id(locInfo.getId());
          }

          if (locInfo.getName() != null) {
            builder.name(locInfo.getName());
          }
          if (locInfo.getCountry() != null) {
            builder.country(locInfo.getCountry());
          }
          if (locInfo.getState() != null) {
            builder.state(locInfo.getState());
          }
          if (locInfo.getCity() != null) {
            builder.city(locInfo.getCity());
          }

          builder.isIndoor(locInfo.isIndoor());

          return builder.build();
        },
        e ->
            QRError.of(
                QRError.ErrorType.PARSE_ERROR, "Failed to convert to Location", e.getMessage()));
  }

  @Override
  public Result<String, QRError> validate(String data) {
    return parse(data)
        .flatMap(
            parsed -> {
              // Validate required fields based on version
              if (parsed.getVersion() == null || parsed.getVersion() < 1) {
                return Result.failure(QRError.validationError("Invalid or missing version"));
              }

              // Must have either CLID, URL, or IPFS hash
              if (parsed.getClid() == null
                  && parsed.getUrl() == null
                  && parsed.getIpfsHash() == null) {
                return Result.failure(
                    QRError.validationError("QR code must contain CLID, URL, or IPFS hash"));
              }

              return Result.success(data);
            });
  }

  private Result<ParsedQRData, QRError> parseJsonData(String jsonData) {
    return Result.<ParsedQRData, QRError>tryExecute(
        () -> {
          try {
            QRPayload payload = objectMapper.readValue(jsonData, QRPayload.class);
            return buildParsedDataFromPayload(payload);
          } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
          }
        },
        e -> QRError.parseError("Invalid JSON format in QR code: " + e.getMessage()));
  }

  /** Builds ParsedQRData from a QRPayload. */
  private ParsedQRData buildParsedDataFromPayload(QRPayload payload) {
    ParsedQRData.ParsedQRDataBuilder builder = ParsedQRData.builder();

    setBasicPayloadFields(builder, payload);
    setRouteDataFromPayload(builder, payload);
    setLocationDataFromPayload(builder, payload);
    setMetadataFromPayload(builder, payload);
    setOfflineDataFlag(builder, payload);

    return builder.build();
  }

  /** Sets basic fields from payload. */
  private void setBasicPayloadFields(ParsedQRData.ParsedQRDataBuilder builder, QRPayload payload) {
    builder.version(payload.getVersion() != null ? payload.getVersion() : 1);

    if (payload.getClid() != null) {
      builder.clid(payload.getClid());
    }
    if (payload.getUrl() != null) {
      builder.url(payload.getUrl());
    }
    if (payload.getIpfsHash() != null) {
      builder.ipfsHash(payload.getIpfsHash());
    }
  }

  /** Sets route data from payload. */
  private void setRouteDataFromPayload(
      ParsedQRData.ParsedQRDataBuilder builder, QRPayload payload) {
    if (payload.getRoute() != null) {
      builder.route(mapRoutePayload(payload.getRoute()));
    }
  }

  /** Sets location data from payload. */
  private void setLocationDataFromPayload(
      ParsedQRData.ParsedQRDataBuilder builder, QRPayload payload) {
    QRPayload.LocationPayload locationPayload = payload.getEffectiveLocation();
    if (locationPayload != null) {
      builder.location(mapLocationPayload(locationPayload));
    }
  }

  /** Sets metadata from payload. */
  private void setMetadataFromPayload(ParsedQRData.ParsedQRDataBuilder builder, QRPayload payload) {
    if (payload.getMeta() != null && payload.getMeta().getBlockchain() != null) {
      builder.blockchainVerified(payload.getMeta().getBlockchain());
    }
  }

  /** Sets the offline data flag based on available data. */
  private void setOfflineDataFlag(ParsedQRData.ParsedQRDataBuilder builder, QRPayload payload) {
    QRPayload.LocationPayload locationPayload = payload.getEffectiveLocation();
    builder.hasOfflineData(payload.getRoute() != null || locationPayload != null);
  }

  private Result<ParsedQRData, QRError> parseUrlData(String url) {
    Matcher matcher = URL_PATTERN.matcher(url);
    if (matcher.find()) {
      return Result.success(
          ParsedQRData.builder()
              .version(1)
              .url(url)
              .shortClid(matcher.group(1))
              .hasOfflineData(false)
              .build());
    }

    return Result.failure(
        QRError.of(QRError.ErrorType.INVALID_FORMAT, "Unrecognized URL format", url));
  }

  private Result<ParsedQRData, QRError> parseUriData(String uri) {
    Matcher matcher = URI_PATTERN.matcher(uri);
    if (matcher.find()) {
      String uuid = matcher.group(1);

      ParsedQRData.ParsedQRDataBuilder builder =
          ParsedQRData.builder()
              .version(1)
              .url(uri)
              .clid("clid:v1:route:" + uuid)
              .hasOfflineData(false);

      // Parse query parameters
      parseUriQueryParams(uri, builder);

      return Result.success(builder.build());
    }

    return Result.failure(
        QRError.of(QRError.ErrorType.INVALID_FORMAT, "Unrecognized URI format", uri));
  }

  private void parseUriQueryParams(String uri, ParsedQRData.ParsedQRDataBuilder builder) {
    int queryStart = uri.indexOf('?');
    if (queryStart == -1) {
      return;
    }

    String query = uri.substring(queryStart + 1);
    String[] params = query.split("&");

    ParsedQRData.RouteInfo.RouteInfoBuilder routeBuilder = ParsedQRData.RouteInfo.builder();
    boolean hasRouteInfo = false;

    for (String param : params) {
      String[] keyValue = param.split("=", 2);
      if (keyValue.length != 2) continue;

      String key = keyValue[0];
      String value = keyValue[1];

      switch (key) {
        case "cldf":
          builder.ipfsHash(value);
          break;
        case "name":
          routeBuilder.name(decodeUrlValue(value));
          hasRouteInfo = true;
          break;
        case "grade":
          routeBuilder.grade(decodeUrlValue(value));
          hasRouteInfo = true;
          break;
        case "gradeSystem":
          routeBuilder.gradeSystem(decodeUrlValue(value));
          hasRouteInfo = true;
          break;
        case "v":
          builder.version(Integer.parseInt(value));
          break;
        default:
          // Unknown parameter, ignore
          log.debug("Unknown URI parameter: {}", key);
          break;
      }
    }

    if (hasRouteInfo) {
      builder.route(routeBuilder.build());
      builder.hasOfflineData(true);
    }
  }

  private String decodeUrlValue(String value) {
    try {
      return java.net.URLDecoder.decode(value, StandardCharsets.UTF_8);
    } catch (Exception e) {
      return value;
    }
  }

  private ParsedQRData.RouteInfo mapRoutePayload(QRPayload.RoutePayload routePayload) {
    return ParsedQRData.RouteInfo.builder()
        .id(routePayload.getId())
        .name(routePayload.getName())
        .grade(routePayload.getGrade())
        .gradeSystem(routePayload.getGradeSystem())
        .type(routePayload.getType())
        .height(routePayload.getHeight())
        .build();
  }

  private ParsedQRData.LocationInfo mapLocationPayload(QRPayload.LocationPayload locationPayload) {
    return ParsedQRData.LocationInfo.builder()
        .clid(locationPayload.getClid())
        .id(locationPayload.getId())
        .name(locationPayload.getName())
        .country(locationPayload.getCountry())
        .state(locationPayload.getState())
        .city(locationPayload.getCity())
        .indoor(locationPayload.getIndoor() != null ? locationPayload.getIndoor() : false)
        .build();
  }

  private void setGradeBySystem(
      String grade, String gradeSystem, Route.Grades.GradesBuilder gradesBuilder) {
    // Set grade based on explicit system - no guessing
    switch (gradeSystem.toLowerCase()) {
      case "vscale":
      case "v_scale":
        gradesBuilder.vScale(grade);
        break;
      case "font":
      case "fontainebleau":
        gradesBuilder.font(grade);
        break;
      case "french":
        gradesBuilder.french(grade);
        break;
      case "yds":
        gradesBuilder.yds(grade);
        break;
      case "uiaa":
        gradesBuilder.uiaa(grade);
        break;
      default:
        // If unknown system, store as YDS for backward compatibility
        log.warn("Unknown grade system: {}, storing as YDS", gradeSystem);
        gradesBuilder.yds(grade);
    }
  }
}
