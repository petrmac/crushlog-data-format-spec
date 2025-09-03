import 'package:uuid/uuid.dart';

import 'models/location.dart';
import 'models/route.dart';
import 'models/sector.dart';
import 'models/clid.dart';
import 'models/validation_result.dart';
import 'utils/string_utils.dart';

/// CLID (CrushLog ID) Generator
/// Generates globally unique, deterministic identifiers for climbing routes and locations
/// Based on UUID v5 for deterministic IDs and UUID v4 for random IDs
class CLIDGenerator {
  /// CrushLog namespace UUID (registered for climbing data)
  static const String crushlogNamespace =
      '6ba7b810-9dad-11d1-80b4-00c04fd430c8';

  static final Uuid _uuid = const Uuid();

  /// UUID validation pattern
  static final RegExp _uuidPattern = RegExp(
    r'^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$',
    caseSensitive: false,
  );

  /// Generate a deterministic CLID for a location
  static String generateLocationCLID(Location location) {
    // Validate required fields
    final validation = _validateLocation(location);
    if (!validation.isValid) {
      throw ArgumentError(
        'Invalid location data: ${validation.errors.join(', ')}',
      );
    }

    // Build deterministic components
    final components = [
      location.country.toUpperCase(),
      location.state?.toLowerCase() ?? '',
      location.city != null ? StringUtils.normalize(location.city!) : '',
      StringUtils.normalize(location.name),
      location.coordinates.lat.toStringAsFixed(6),
      location.coordinates.lon.toStringAsFixed(6),
      location.isIndoor ? 'indoor' : 'outdoor',
    ];

    final input = components.where((s) => s.isNotEmpty).join(':');
    final uuid = _uuid.v5(crushlogNamespace, input);

    return 'clid:v1:location:$uuid';
  }

  /// Generate a deterministic CLID for a route
  static String generateRouteCLID(String locationCLID, Route route) {
    // Validate required fields
    final validation = _validateRoute(route);
    if (!validation.isValid) {
      throw ArgumentError(
        'Invalid route data: ${validation.errors.join(', ')}',
      );
    }

    // Extract location UUID (remove prefix)
    final locationUuid = locationCLID.replaceFirst('clid:location:', '');

    // Build deterministic components
    final components = [
      locationUuid,
      StringUtils.normalize(route.name),
      StringUtils.standardizeGrade(route.grade),
      route.firstAscent?.year?.toString() ?? '',
      route.firstAscent?.name != null
          ? StringUtils.normalize(route.firstAscent!.name!)
          : '',
      route.height?.toStringAsFixed(1) ?? '',
    ];

    final input = components.join(':');
    final uuid = _uuid.v5(crushlogNamespace, input);

    return 'clid:v1:route:$uuid';
  }

  /// Generate a deterministic CLID for a sector
  static String generateSectorCLID(String locationCLID, Sector sector) {
    // Extract location UUID
    final locationUuid = locationCLID.replaceFirst('clid:v1:location:', '');

    final components = [
      locationUuid,
      StringUtils.normalize(sector.name),
      sector.order?.toString() ?? '0',
    ];

    final input = components.join(':');
    final uuid = _uuid.v5(crushlogNamespace, input);

    return 'clid:v1:sector:$uuid';
  }

  /// Generate a random UUID v4 for user content
  static String generateRandomCLID(EntityType type) {
    final uuid = _uuid.v4();
    return 'clid:v1:${type.value}:$uuid';
  }

  /// Parse a CLID into its components
  static CLID parse(String clid) {
    final parts = clid.split(':');

    // Only support v1 format (clid:v1:type:uuid)
    if (parts.length != 4 || parts[0] != 'clid' || parts[1] != 'v1') {
      throw ArgumentError(
        'Invalid CLID format. Expected clid:v1:type:uuid, got: $clid',
      );
    }

    final type = EntityType.values.firstWhere(
      (e) => e.value == parts[2],
      orElse: () => throw ArgumentError('Unknown entity type: ${parts[2]}'),
    );

    final uuid = parts[3];
    final shortForm = uuid.substring(0, uuid.length.clamp(0, 8));

    return CLID(
      namespace: 'clid',
      type: type,
      uuid: uuid,
      fullId: clid,
      shortForm: shortForm,
      url: 'https://crushlog.pro/g/$shortForm',
    );
  }

  /// Validate a CLID
  static bool validate(String clid) {
    try {
      final parsed = parse(clid);
      return _uuidPattern.hasMatch(parsed.uuid);
    } catch (_) {
      return false;
    }
  }

  /// Generate a short URL-safe version of the CLID
  static String toShortForm(String clid) {
    final parsed = parse(clid);
    return parsed.shortForm;
  }

  /// Validate location data
  static ValidationResult _validateLocation(Location location) {
    final errors = <String>[];
    final warnings = <String>[];

    if (location.country.length != 2) {
      errors.add('Country must be ISO 3166-1 alpha-2 code');
    }

    if (location.name.trim().isEmpty) {
      errors.add('Location name is required');
    }

    if (location.coordinates.lat.abs() > 90) {
      errors.add('Latitude must be between -90 and 90');
    }

    if (location.coordinates.lon.abs() > 180) {
      errors.add('Longitude must be between -180 and 180');
    }

    return ValidationResult(errors: errors, warnings: warnings);
  }

  /// Validate route data
  static ValidationResult _validateRoute(Route route) {
    final errors = <String>[];
    final warnings = <String>[];

    if (route.name.trim().isEmpty) {
      errors.add('Route name is required');
    }

    if (route.grade.trim().isEmpty) {
      errors.add('Route grade is required');
    }

    // Warnings for optional but recommended fields
    if (route.firstAscent == null) {
      warnings.add('First ascent year recommended for historical routes');
    }

    return ValidationResult(errors: errors, warnings: warnings);
  }
}

/// Entity types for CLDF
enum EntityType {
  location('location'),
  route('route'),
  sector('sector'),
  climb('climb'),
  session('session'),
  media('media');

  final String value;
  const EntityType(this.value);

  static EntityType fromString(String value) {
    return EntityType.values.firstWhere(
      (type) => type.value == value,
      orElse: () => throw ArgumentError('Unknown entity type: $value'),
    );
  }
}
