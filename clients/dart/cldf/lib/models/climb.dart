import 'package:json_annotation/json_annotation.dart';

import '../utils/local_date_converter.dart';
import 'enums/belay_type.dart';
import 'enums/climb_type.dart';
import 'enums/finish_type.dart';
import 'enums/grade_system.dart';
import 'enums/rock_type.dart';
import 'enums/terrain_type.dart';

part 'climb.g.dart';

// Helper functions for date conversion
String _dateFromJson(String? json) =>
    const FlexibleLocalDateConverter().fromJson(json) ?? '';
String? _dateToJson(String? date) =>
    const FlexibleLocalDateConverter().toJson(date);

/// A single climbing attempt or completion
@JsonSerializable()
class Climb {

  /// Creates a new [Climb] instance
  Climb({
    required this.id,
    this.sessionId,
    this.routeId,
    required this.date,
    this.time,
    this.routeName,
    this.grades,
    required this.type,
    required this.finishType,
    this.attempts = 1,
    this.repeats = 0,
    this.isRepeat = false,
    this.belayType,
    this.duration,
    this.falls,
    this.height,
    this.rating,
    this.notes,
    this.tags,
    this.beta,
    this.media,
    this.color,
    this.rockType,
    this.terrainType,
    this.isIndoor,
    this.partners,
    this.weather,
    this.customFields,
  });

  /// Creates a [Climb] from JSON
  factory Climb.fromJson(Map<String, dynamic> json) => _$ClimbFromJson(json);
  /// Unique identifier
  final int id;

  /// Reference to session
  final int? sessionId;

  /// Reference to route by ID
  final int? routeId;

  /// Date of the climb (YYYY-MM-DD)
  @JsonKey(fromJson: _dateFromJson, toJson: _dateToJson)
  final String date;

  /// Time of the climb (HH:MM:SS)
  final String? time;

  /// Name of the route (when route is not in database)
  final String? routeName;

  /// Grade information
  final GradeInfo? grades;

  /// Type of climb
  final ClimbType type;

  /// How the climb was finished
  final FinishType finishType;

  /// Number of attempts
  final int attempts;

  /// Number of repeats
  final int repeats;

  /// Whether this is a repeat
  final bool isRepeat;

  /// Belay type for rope climbs
  final BelayType? belayType;

  /// Duration in minutes
  final int? duration;

  /// Number of falls
  final int? falls;

  /// Height in meters
  final double? height;

  /// Quality rating (0-5)
  final int? rating;

  /// Notes
  final String? notes;

  /// Associated tags
  final List<String>? tags;

  /// Beta information
  final String? beta;

  /// Media references
  final Media? media;

  /// Hold color
  final String? color;

  /// Rock type
  final RockType? rockType;

  /// Terrain type
  final TerrainType? terrainType;

  /// Whether this was indoor
  final bool? isIndoor;

  /// Climbing partners
  final List<String>? partners;

  /// Weather conditions
  final String? weather;

  /// Custom fields
  final Map<String, dynamic>? customFields;

  /// Converts this [Climb] to JSON
  Map<String, dynamic> toJson() => _$ClimbToJson(this);
}

/// Grade information
@JsonSerializable()
class GradeInfo {

  /// Creates a new [GradeInfo] instance
  GradeInfo({
    required this.system,
    required this.grade,
    this.conversions,
  });

  /// Creates a [GradeInfo] from JSON
  factory GradeInfo.fromJson(Map<String, dynamic> json) =>
      _$GradeInfoFromJson(json);
  /// The grading system used
  final GradeSystem system;
  
  /// The grade value
  final String grade;
  
  /// Grade conversions to other systems
  final Map<String, String>? conversions;

  /// Converts this [GradeInfo] to JSON
  Map<String, dynamic> toJson() => _$GradeInfoToJson(this);
}

/// Media references
@JsonSerializable()
class Media {

  /// Creates a new [Media] instance
  Media({
    this.photos,
    this.videos,
    this.count,
  });

  /// Creates a [Media] from JSON
  factory Media.fromJson(Map<String, dynamic> json) => _$MediaFromJson(json);
  /// List of photo references
  final List<String>? photos;
  
  /// List of video references
  final List<String>? videos;
  
  /// Total media count
  final int? count;

  /// Converts this [Media] to JSON
  Map<String, dynamic> toJson() => _$MediaToJson(this);
}
