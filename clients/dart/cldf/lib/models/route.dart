import 'package:json_annotation/json_annotation.dart';

import 'enums/protection_rating.dart';
import 'enums/route_type.dart';

part 'route.g.dart';

/// A climbing route or boulder problem
@JsonSerializable()
class Route {
  /// Creates a new [Route] instance
  Route({
    required this.id,
    required this.locationId,
    this.sectorId,
    required this.name,
    required this.routeType,
    this.grades,
    this.height,
    this.bolts,
    this.color,
    this.firstAscent,
    this.protection,
    this.popularity,
    this.qualityRating,
    this.beta,
    this.tags,
    this.customFields,
  });

  /// Creates a [Route] from JSON
  factory Route.fromJson(Map<String, dynamic> json) => _$RouteFromJson(json);

  /// Unique identifier
  final int id;

  /// Reference to location
  final int locationId;

  /// Reference to sector
  final int? sectorId;

  /// Route name
  final String name;

  /// Type of route
  final RouteType routeType;

  /// Grades in different systems
  final Map<String, String>? grades;

  /// Height in meters
  final double? height;

  /// Number of bolts
  final int? bolts;

  /// Hold color
  final String? color;

  /// First ascent information
  final FirstAscent? firstAscent;

  /// Protection rating
  final ProtectionRating? protection;

  /// Popularity rating
  final int? popularity;

  /// Quality rating (0-5)
  final int? qualityRating;

  /// Beta information
  final String? beta;

  /// Associated tags
  final List<String>? tags;

  /// Custom fields
  final Map<String, dynamic>? customFields;

  /// Converts this [Route] to JSON
  Map<String, dynamic> toJson() => _$RouteToJson(this);
}

/// First ascent information
@JsonSerializable()
class FirstAscent {
  /// Creates a new [FirstAscent] instance
  FirstAscent({this.climberName, this.date, this.notes});

  /// Creates a [FirstAscent] from JSON
  factory FirstAscent.fromJson(Map<String, dynamic> json) =>
      _$FirstAscentFromJson(json);

  /// Name of the first ascensionist
  final String? climberName;

  /// Date of first ascent
  final String? date;

  /// Additional notes
  final String? notes;

  /// Converts this [FirstAscent] to JSON
  Map<String, dynamic> toJson() => _$FirstAscentToJson(this);
}
