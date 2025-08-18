import 'dart:convert';
import 'dart:typed_data';
import 'package:qr/qr.dart';
import 'package:image/image.dart' as img;
import '../models/route.dart';
import '../models/location.dart';
import '../clid/cldf_clid_adapter.dart';

/// Options for QR code generation
class QROptions {
  final String baseUrl;
  final bool includeOfflineData;
  final String? ipfsHash;
  final QRFormat format;

  const QROptions({
    this.baseUrl = 'https://crushlog.pro',
    this.includeOfflineData = true,
    this.ipfsHash,
    this.format = QRFormat.json,
  });
}

/// QR code format types
enum QRFormat { json, url, customUri }

/// QR code data payload (v1 spec with CLIDs)
class QRCodeData {
  final int version;
  final String? clid;
  final String? url;
  final String? cldf; // IPFS hash or archive identifier
  final Map<String, dynamic>? route;
  final Map<String, dynamic>? loc; // 'loc' instead of 'location' per spec
  final Map<String, dynamic>? meta;
  final Map<String, dynamic>? verification;

  QRCodeData({
    this.version = 1,
    this.clid,
    this.url,
    this.cldf,
    this.route,
    this.loc,
    this.meta,
    this.verification,
  });

  Map<String, dynamic> toJson() => {
    'v': version, // 'v' instead of 'version' per spec
    if (clid != null) 'clid': clid,
    if (url != null) 'url': url,
    if (cldf != null) 'cldf': cldf,
    if (route != null) 'route': route,
    if (loc != null) 'loc': loc,
    if (meta != null) 'meta': meta,
    if (verification != null) 'verification': verification,
  };
}

/// Generates QR codes for routes and locations
class QRGenerator {
  /// Generate QR code data for a route
  QRCodeData generateRouteData(
    Route route,
    QROptions options, {
    Location? location,
  }) {
    final clid = _getRouteCLID(route, location);
    final routeData = _buildRouteData(route, options);
    final shortClid = _extractShortCLID(clid);
    final locationData = _buildLocationDataForRoute(location, options);

    return QRCodeData(
      version: 1,
      clid: clid,
      url: '${options.baseUrl}/g/$shortClid', // Using /g/ endpoint per spec
      cldf: options.ipfsHash,
      route: routeData,
      loc: locationData,
      meta: _buildMetadata(location),
      verification: _buildVerification(),
    );
  }

  /// Get or generate CLID for route
  String _getRouteCLID(Route route, Location? location) {
    if (route.clid != null) {
      return route.clid!;
    }
    if (location != null) {
      return CLDFClidAdapter.generateRouteCLID(route, location);
    }
    return CLDFClidAdapter.generateStandaloneRouteCLID(route);
  }

  /// Build route data for QR code
  Map<String, dynamic>? _buildRouteData(Route route, QROptions options) {
    if (!options.includeOfflineData) {
      return null;
    }

    final data = <String, dynamic>{
      'id': route.id,
      'name': route.name,
      'type': route.routeType.name,
    };

    _addGradeData(data, route.grades);

    if (route.height != null) {
      data['height'] = route.height;
    }

    return data;
  }

  /// Add grade information to route data
  void _addGradeData(Map<String, dynamic> data, Map<String, dynamic>? grades) {
    if (grades == null) return;

    // Prioritize V-scale over YDS
    if (grades['vScale'] != null) {
      data['grade'] = grades['vScale'];
      data['gradeSystem'] = 'vScale';
    } else if (grades['yds'] != null) {
      data['grade'] = grades['yds'];
      data['gradeSystem'] = 'YDS';
    }
  }

  /// Build location data for route QR code
  Map<String, dynamic>? _buildLocationDataForRoute(
    Location? location,
    QROptions options,
  ) {
    if (location == null) return null;

    final data = <String, dynamic>{
      'clid': location.clid ?? generateLocationData(location, options).clid,
      'name': location.name,
    };

    if (location.country != null) {
      data['country'] = location.country;
    }

    return data;
  }

  /// Extract short CLID from full CLID
  String _extractShortCLID(String clid) {
    final clidParts = clid.split(':');
    final uuid = clidParts.length == 4
        ? clidParts[3]
        : clidParts[2]; // v1 format has UUID at index 3
    return uuid.substring(0, 8);
  }

  /// Build metadata for QR code
  Map<String, dynamic> _buildMetadata(Location? location) {
    final meta = <String, dynamic>{'created': DateTime.now().toIso8601String()};

    if (location?.isIndoor == true) {
      meta['expires'] = _getResetDate();
    }

    return meta;
  }

  /// Build verification data for QR code
  Map<String, dynamic> _buildVerification() {
    return {'method': 'uuid-v5', 'timestamp': DateTime.now().toIso8601String()};
  }

  /// Generate QR code data for a location
  QRCodeData generateLocationData(Location location, QROptions options) {
    final clid = _getLocationCLID(location);
    final locationData = _buildLocationData(location, options);
    final shortClid = _extractShortCLID(clid);

    return QRCodeData(
      version: 1,
      clid: clid,
      url: '${options.baseUrl}/g/$shortClid', // Using /g/ endpoint per spec
      cldf: options.ipfsHash,
      loc: locationData,
      meta: _buildMetadata(location),
      verification: _buildVerification(),
    );
  }

  /// Get or generate CLID for location
  String _getLocationCLID(Location location) {
    return location.clid ?? CLDFClidAdapter.generateLocationCLID(location);
  }

  /// Build location data for QR code
  Map<String, dynamic>? _buildLocationData(
    Location location,
    QROptions options,
  ) {
    if (!options.includeOfflineData) {
      return null;
    }

    final data = <String, dynamic>{
      'id': location.id,
      'name': location.name,
      'indoor': location.isIndoor,
    };

    if (location.country != null) {
      data['country'] = location.country;
    }
    if (location.state != null) {
      data['state'] = location.state;
    }
    if (location.city != null) {
      data['city'] = location.city;
    }

    return data;
  }

  /// Generate QR code image as PNG bytes
  Future<Uint8List> generatePNG(
    QRCodeData data,
    QROptions options, {
    int size = 256,
    int foregroundColor = 0xFF000000,
    int backgroundColor = 0xFFFFFFFF,
  }) async {
    final payload = _createPayload(data, options);

    // Generate QR code using pure Dart library
    final qr = QrCode.fromData(
      data: payload,
      errorCorrectLevel: QrErrorCorrectLevel.M,
    );
    final qrImage = QrImage(qr);

    final moduleCount = qr.moduleCount;
    final scale = size ~/ moduleCount;
    final actualSize = moduleCount * scale;

    // Create image
    final image = img.Image(width: actualSize, height: actualSize);

    // Fill background
    img.fill(
      image,
      color: img.ColorRgb8(
        (backgroundColor >> 16) & 0xFF,
        (backgroundColor >> 8) & 0xFF,
        backgroundColor & 0xFF,
      ),
    );

    // Draw QR modules
    for (int y = 0; y < moduleCount; y++) {
      for (int x = 0; x < moduleCount; x++) {
        if (qrImage.isDark(y, x)) {
          img.fillRect(
            image,
            x1: x * scale,
            y1: y * scale,
            x2: (x + 1) * scale - 1,
            y2: (y + 1) * scale - 1,
            color: img.ColorRgb8(
              (foregroundColor >> 16) & 0xFF,
              (foregroundColor >> 8) & 0xFF,
              foregroundColor & 0xFF,
            ),
          );
        }
      }
    }

    return img.encodePng(image);
  }

  /// Generate QR code as SVG string
  String generateSVG(
    QRCodeData data,
    QROptions options, {
    int size = 256,
    String foregroundColor = '#000000',
    String backgroundColor = '#FFFFFF',
  }) {
    final payload = _createPayload(data, options);

    // For simplicity, we'll create a basic SVG
    // In production, you might want to use a more sophisticated approach
    final qrCode = QrCode.fromData(
      data: payload,
      errorCorrectLevel: QrErrorCorrectLevel.M,
    );
    final qrImage = QrImage(qrCode);

    final moduleCount = qrCode.moduleCount;
    final moduleSize = size ~/ moduleCount;

    final svg = StringBuffer();
    svg.write('<svg xmlns="http://www.w3.org/2000/svg" ');
    svg.write('width="$size" height="$size" viewBox="0 0 $size $size">\n');
    svg.write('<rect width="100%" height="100%" fill="$backgroundColor"/>\n');
    svg.write('<g fill="$foregroundColor">\n');

    for (int y = 0; y < moduleCount; y++) {
      for (int x = 0; x < moduleCount; x++) {
        if (qrImage.isDark(y, x)) {
          svg.write('<rect x="${x * moduleSize}" y="${y * moduleSize}" ');
          svg.write('width="$moduleSize" height="$moduleSize"/>\n');
        }
      }
    }

    svg.write('</g>\n</svg>');
    return svg.toString();
  }

  String _createPayload(QRCodeData data, QROptions options) {
    switch (options.format) {
      case QRFormat.json:
        return jsonEncode(data.toJson());
      case QRFormat.url:
        return data.url ?? '';
      case QRFormat.customUri:
        final parts = data.clid?.split(':');
        if (parts != null && parts.length >= 3) {
          final type = parts[1];
          final uuid = parts[2];
          return 'cldf://$type/$uuid';
        }
        return '';
    }
  }

  /// Get reset date for indoor routes (30 days from now)
  String _getResetDate() {
    final resetDate = DateTime.now().add(Duration(days: 30));
    return resetDate.toIso8601String();
  }
}
