import 'dart:convert';
import 'dart:typed_data';
import 'package:image/image.dart' as img;
import 'package:zxing2/qrcode.dart';
import '../models/route.dart';
import '../models/location.dart';
import '../models/enums/route_type.dart';

/// Parsed QR code data
class ParsedQRData {
  final int version;
  final String? clid;
  final String? url;
  final String? shortClid;
  final String? ipfsHash;
  final RouteInfo? route;
  final LocationInfo? location;
  final bool hasOfflineData;
  final bool blockchainVerified;

  ParsedQRData({
    this.version = 1,
    this.clid,
    this.url,
    this.shortClid,
    this.ipfsHash,
    this.route,
    this.location,
    this.hasOfflineData = false,
    this.blockchainVerified = false,
  });

  factory ParsedQRData.fromJson(Map<String, dynamic> json) {
    return ParsedQRData(
      version: json['version'] ?? 1,
      clid: json['clid'],
      url: json['url'],
      ipfsHash: json['ipfsHash'],
      route: json['route'] != null ? RouteInfo.fromJson(json['route']) : null,
      location: json['location'] != null
          ? LocationInfo.fromJson(json['location'])
          : json['loc'] != null
          ? LocationInfo.fromJson(json['loc'])
          : null,
      hasOfflineData:
          json['hasOfflineData'] ??
          (json['route'] != null ||
              json['location'] != null ||
              json['loc'] != null),
      blockchainVerified: json['meta']?['blockchain'] ?? false,
    );
  }
}

/// Route information from QR code
class RouteInfo {
  final int? id;
  final String? name;
  final String? grade;
  final String? gradeSystem;
  final String? type;
  final double? height;

  RouteInfo({
    this.id,
    this.name,
    this.grade,
    this.gradeSystem,
    this.type,
    this.height,
  });

  factory RouteInfo.fromJson(Map<String, dynamic> json) {
    return RouteInfo(
      id: json['id'],
      name: json['name'],
      grade: json['grade'],
      gradeSystem: json['gradeSystem'],
      type: json['type'],
      height: json['height']?.toDouble(),
    );
  }
}

/// Location information from QR code
class LocationInfo {
  final String? clid;
  final int? id;
  final String? name;
  final String? country;
  final String? state;
  final String? city;
  final bool indoor;

  LocationInfo({
    this.clid,
    this.id,
    this.name,
    this.country,
    this.state,
    this.city,
    this.indoor = false,
  });

  factory LocationInfo.fromJson(Map<String, dynamic> json) {
    return LocationInfo(
      clid: json['clid'],
      id: json['id'],
      name: json['name'],
      country: json['country'],
      state: json['state'],
      city: json['city'],
      indoor: json['indoor'] ?? false,
    );
  }
}

/// QR code scanner for CLDF data
class QRScanner {
  static final _urlPattern = RegExp(r'https?://[^/]+/g/([a-f0-9-]+)');
  static final _uriPattern = RegExp(r'cldf://global/route/([a-f0-9-]+)');

  /// Parse QR code data from string
  ParsedQRData? parse(String data) {
    if (data.isEmpty) return null;

    final trimmed = data.trim();

    // Try to parse as JSON
    if (trimmed.startsWith('{')) {
      try {
        final json = jsonDecode(trimmed);
        return ParsedQRData.fromJson(json);
      } catch (e) {
        // Not valid JSON, continue with other formats
      }
    }

    // Try to parse as URL
    if (trimmed.startsWith('http://') || trimmed.startsWith('https://')) {
      return _parseUrl(trimmed);
    }

    // Try to parse as custom URI
    if (trimmed.startsWith('cldf://')) {
      return _parseUri(trimmed);
    }

    return null;
  }

  /// Scan QR code from image bytes (PNG, JPEG, etc.)
  Future<ParsedQRData?> scanImage(Uint8List imageBytes) async {
    try {
      // Decode image
      final image = img.decodeImage(imageBytes);
      if (image == null) return null;

      // Convert to grayscale for better QR detection
      final grayscale = img.grayscale(image);

      // Create luminance source for ZXing
      final source = _ImageLuminanceSource(grayscale);

      // Decode QR code
      final reader = QRCodeReader();
      final result = reader.decode(BinaryBitmap(HybridBinarizer(source)));

      if (result?.text != null) {
        return parse(result!.text!);
      }
    } catch (e) {
      // Failed to decode QR code
      print('QR scan error: $e');
    }

    return null;
  }

  /// Convert parsed QR data to Route object
  Route? toRoute(ParsedQRData data) {
    if (data.route == null) return null;

    final routeInfo = data.route!;
    final grades = _parseGrades(routeInfo.grade, routeInfo.gradeSystem);

    return Route(
      id: routeInfo.id ?? 0,
      clid: data.clid,
      name: routeInfo.name ?? '',
      grades: grades,
      routeType: _parseRouteType(routeInfo.type) ?? RouteType.route,
      height: routeInfo.height,
      locationId: data.location?.id ?? 0,
    );
  }

  /// Convert parsed QR data to Location object
  Location? toLocation(ParsedQRData data) {
    if (data.location == null) return null;

    final locInfo = data.location!;

    return Location(
      id: locInfo.id ?? 0,
      clid: locInfo.clid ?? data.clid,
      name: locInfo.name ?? '',
      country: locInfo.country,
      state: locInfo.state,
      city: locInfo.city,
      isIndoor: locInfo.indoor,
    );
  }

  ParsedQRData? _parseUrl(String url) {
    final match = _urlPattern.firstMatch(url);
    if (match != null) {
      return ParsedQRData(
        version: 1,
        url: url,
        shortClid: match.group(1),
        hasOfflineData: false,
      );
    }
    return null;
  }

  ParsedQRData? _parseUri(String uri) {
    final match = _uriPattern.firstMatch(uri);
    if (match != null) {
      final uuid = match.group(1);

      final data = ParsedQRData(
        version: 1,
        url: uri,
        clid: 'clid:route:$uuid',
        hasOfflineData: false,
      );

      // Parse query parameters
      final queryStart = uri.indexOf('?');
      if (queryStart != -1) {
        final query = uri.substring(queryStart + 1);
        final params = Uri.splitQueryString(query);

        if (params.containsKey('name') || params.containsKey('grade')) {
          final routeInfo = RouteInfo(
            name: params['name'],
            grade: params['grade'],
            gradeSystem: params['gradeSystem'],
          );

          return ParsedQRData(
            version: data.version,
            url: data.url,
            clid: data.clid,
            ipfsHash: params['cldf'],
            route: routeInfo,
            hasOfflineData: true,
          );
        }
      }

      return data;
    }
    return null;
  }

  Map<String, String>? _parseGrades(String? grade, String? gradeSystem) {
    if (grade == null || gradeSystem == null) return null;

    switch (gradeSystem.toLowerCase()) {
      case 'vscale':
      case 'v_scale':
        return {'vScale': grade};
      case 'font':
      case 'fontainebleau':
        return {'font': grade};
      case 'french':
        return {'french': grade};
      case 'yds':
        return {'yds': grade};
      case 'uiaa':
        return {'uiaa': grade};
      default:
        return {'yds': grade}; // Default to YDS
    }
  }

  RouteType? _parseRouteType(String? type) {
    if (type == null) return null;

    switch (type.toLowerCase()) {
      case 'boulder':
        return RouteType.boulder;
      case 'route':
      default:
        return RouteType.route;
    }
  }
}

/// Luminance source adapter for image package
class _ImageLuminanceSource extends LuminanceSource {
  final img.Image image;
  final Int8List _luminances;

  _ImageLuminanceSource(this.image)
    : _luminances = _extractLuminances(image),
      super(image.width, image.height);

  static Int8List _extractLuminances(img.Image image) {
    final luminances = Int8List(image.width * image.height);
    var index = 0;

    for (int y = 0; y < image.height; y++) {
      for (int x = 0; x < image.width; x++) {
        final pixel = image.getPixel(x, y);
        // Convert to grayscale luminance
        final r = pixel.r.toInt();
        final g = pixel.g.toInt();
        final b = pixel.b.toInt();
        // Use standard luminance formula
        luminances[index++] = ((0.299 * r + 0.587 * g + 0.114 * b)).toInt();
      }
    }

    return luminances;
  }

  @override
  Int8List getRow(int y, Int8List? row) {
    final width = this.width;
    row ??= Int8List(width);
    final offset = y * width;
    for (int x = 0; x < width; x++) {
      row[x] = _luminances[offset + x];
    }
    return row;
  }

  @override
  Int8List getMatrix() => _luminances;
}
