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
    this.description,
    this.coordinates,
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

  /// Description
  final String? description;

  /// GPS coordinates
  final Coordinates? coordinates;

  /// Associated tags
  final List<String>? tags;

  /// Custom fields
  final Map<String, dynamic>? customFields;

  /// Converts this [Sector] to JSON
  Map<String, dynamic> toJson() => _$SectorToJson(this);
}
