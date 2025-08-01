import 'package:json_annotation/json_annotation.dart';

import 'enums/rock_type.dart';
import 'enums/terrain_type.dart';

part 'location.g.dart';

/// A climbing location (crag, gym, etc.)
@JsonSerializable()
class Location {
  /// Creates a new [Location] instance
  Location({
    required this.id,
    required this.name,
    required this.country,
    required this.isIndoor,
    this.state,
    this.city,
    this.address,
    this.coordinates,
    this.rockType,
    this.terrainType,
    this.accessInfo,
    this.tags,
    this.customFields,
  });

  /// Creates a [Location] from JSON
  factory Location.fromJson(Map<String, dynamic> json) =>
      _$LocationFromJson(json);

  /// Unique identifier
  final int id;

  /// Location name
  final String name;

  /// Country
  final String country;

  /// Whether this is an indoor location
  final bool isIndoor;

  /// State or province
  final String? state;

  /// City
  final String? city;

  /// Full address
  final String? address;

  /// GPS coordinates
  final Coordinates? coordinates;

  /// Type of rock
  final RockType? rockType;

  /// Type of terrain
  final TerrainType? terrainType;

  /// Access information
  final String? accessInfo;

  /// Associated tags
  final List<String>? tags;

  /// Custom fields
  final Map<String, dynamic>? customFields;

  /// Converts this [Location] to JSON
  Map<String, dynamic> toJson() => _$LocationToJson(this);
}

/// GPS coordinates
@JsonSerializable()
class Coordinates {
  /// Creates a new [Coordinates] instance
  Coordinates({
    required this.latitude,
    required this.longitude,
    this.elevation,
  });

  /// Creates a [Coordinates] from JSON
  factory Coordinates.fromJson(Map<String, dynamic> json) =>
      _$CoordinatesFromJson(json);

  /// Latitude coordinate
  final double latitude;

  /// Longitude coordinate
  final double longitude;

  /// Elevation in meters
  final double? elevation;

  /// Converts this [Coordinates] to JSON
  Map<String, dynamic> toJson() => _$CoordinatesToJson(this);
}
