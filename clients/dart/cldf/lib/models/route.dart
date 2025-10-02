import 'package:json_annotation/json_annotation.dart';

import 'enums/protection_rating.dart';
import 'enums/route_type.dart';
import 'enums/route_characteristics.dart';
import 'media/media.dart';

part 'route.g.dart';

/// A climbing route or boulder problem
@JsonSerializable(includeIfNull: false)
class Route {
  /// Creates a new [Route] instance
  Route({
    required this.id,
    this.clid,
    required this.locationId,
    this.sectorId,
    required this.name,
    required this.routeType,
    this.routeCharacteristics,
    this.grades,
    this.height,
    this.color,
    this.firstAscent,
    this.protectionRating,
    this.qualityRating,
    this.beta,
    this.gearNotes,
    this.tags,
    this.media,
    this.customFields,
  });

  /// Creates a [Route] from JSON
  factory Route.fromJson(Map<String, dynamic> json) => _$RouteFromJson(json);

  /// Unique identifier
  final int id;

  /// CrushLog ID - globally unique identifier (v1.3.0+)
  final String? clid;

  /// Reference to location
  final int locationId;

  /// Reference to sector
  final int? sectorId;

  /// Route name
  final String name;

  /// Type of route
  final RouteType routeType;

  /// Route protection characteristics (trad, bolted)
  final RouteCharacteristics? routeCharacteristics;

  /// Grades in different systems
  final Map<String, String>? grades;

  /// Height in meters
  final double? height;

  /// Hold color
  final String? color;

  /// First ascent information
  final FirstAscent? firstAscent;

  /// Protection rating
  final ProtectionRating? protectionRating;

  /// Quality rating (0-5)
  final int? qualityRating;

  /// Beta information
  final String? beta;

  /// Specific gear requirements or protection notes
  final String? gearNotes;

  /// Associated tags
  final List<String>? tags;

  /// Media associated with this route (topos, beta videos, etc.)
  final Media? media;

  /// Custom fields
  final Map<String, dynamic>? customFields;

  /// Converts this [Route] to JSON
  Map<String, dynamic> toJson() => _$RouteToJson(this);
}

/// First ascent information
@JsonSerializable(includeIfNull: false)
class FirstAscent {
  /// Creates a new [FirstAscent] instance
  FirstAscent({this.name, this.date, this.info});

  /// Creates a [FirstAscent] from JSON
  factory FirstAscent.fromJson(Map<String, dynamic> json) =>
      _$FirstAscentFromJson(json);

  /// Name of the first ascensionist
  final String? name;

  /// Date of first ascent
  final String? date;

  /// Additional notes
  final String? info;

  /// Converts this [FirstAscent] to JSON
  Map<String, dynamic> toJson() => _$FirstAscentToJson(this);
}
