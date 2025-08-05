import 'package:json_annotation/json_annotation.dart';

import 'location.dart' show Coordinates;

part 'sector.g.dart';

/// A sector within a climbing location
@JsonSerializable()
class Sector {
  /// Creates a new [Sector] instance
  Sector({
    required this.id,
    required this.locationId,
    required this.name,
    this.isDefault = false,
    this.description,
    this.approach,
    this.coordinates,
    this.createdAt,
    this.tags,
    this.customFields,
  });

  /// Creates a [Sector] from JSON
  factory Sector.fromJson(Map<String, dynamic> json) => _$SectorFromJson(json);

  /// Unique identifier
  final int id;

  /// Reference to location
  final int locationId;

  /// Sector name
  final String name;

  /// Whether this is the default sector for the location
  final bool isDefault;

  /// Description
  final String? description;

  /// Approach information
  final String? approach;

  /// GPS coordinates
  final Coordinates? coordinates;

  /// Creation timestamp
  final DateTime? createdAt;

  /// Associated tags
  final List<String>? tags;

  /// Custom fields
  final Map<String, dynamic>? customFields;

  /// Converts this [Sector] to JSON
  Map<String, dynamic> toJson() => _$SectorToJson(this);
}
