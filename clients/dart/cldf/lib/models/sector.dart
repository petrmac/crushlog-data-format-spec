import 'package:json_annotation/json_annotation.dart';

import 'location.dart' show Coordinates;
import 'media/media.dart';

part 'sector.g.dart';

/// A sector within a climbing location
@JsonSerializable(includeIfNull: false)
class Sector {
  /// Creates a new [Sector] instance
  Sector({
    required this.id,
    this.clid,
    required this.locationId,
    required this.name,
    this.isDefault = false,
    this.description,
    this.approach,
    this.coordinates,
    this.media,
    this.createdAt,
    this.tags,
    this.customFields,
  });

  /// Creates a [Sector] from JSON
  factory Sector.fromJson(Map<String, dynamic> json) => _$SectorFromJson(json);

  /// Unique identifier
  final int id;

  /// CrushLog ID - globally unique identifier (v1.3.0+)
  final String? clid;

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

  /// Media associated with this sector (overview photos, approach maps, etc.)
  final Media? media;

  /// Creation timestamp
  @JsonKey(toJson: _dateTimeToJson, fromJson: _dateTimeFromJson)
  final DateTime? createdAt;

  /// Associated tags
  final List<String>? tags;

  /// Custom fields
  final Map<String, dynamic>? customFields;

  /// Converts this [Sector] to JSON
  Map<String, dynamic> toJson() => _$SectorToJson(this);
}

// Helper functions for DateTime serialization
String? _dateTimeToJson(DateTime? dateTime) =>
    dateTime?.toUtc().toIso8601String();
DateTime? _dateTimeFromJson(String? json) =>
    json == null ? null : DateTime.parse(json);
