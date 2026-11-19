import 'package:json_annotation/json_annotation.dart';

import 'enums/climb_type.dart';

part 'grade_scale.g.dart';

/// How the levels of a [GradeScale] were laid out when it was created.
@JsonEnum(valueField: 'value')
enum GradeScaleSpread {
  /// Every level covers the same number of grades.
  even('even'),

  /// The upper levels cover more grades than the lower ones.
  widerTop('widerTop');

  /// The value written to the archive.
  final String value;
  const GradeScaleSpread(this.value);
}

/// A grading scale used at a location, such as a gym's numbered or coloured
/// levels. Each level starts at a grade on the difficulty axis.
@JsonSerializable(includeIfNull: false, explicitToJson: true)
class GradeScale {
  /// Creates a new [GradeScale] instance
  GradeScale({
    required this.id,
    this.climbType,
    this.spread,
    required this.updatedAt,
    this.deletedAt,
    required this.levels,
  });

  /// Creates a [GradeScale] from JSON
  factory GradeScale.fromJson(Map<String, dynamic> json) =>
      _$GradeScaleFromJson(json);

  /// Globally unique id of the scale. Climbs refer to it in
  /// [GradeScaleRef.id].
  final String id;

  /// The kind of climb the scale grades. Boulder when absent.
  final ClimbType? climbType;

  /// How the levels were laid out when the scale was created.
  final GradeScaleSpread? spread;

  /// When the scale last changed. Of two copies of a scale, the newer wins.
  @JsonKey(toJson: _dateTimeToJson, fromJson: _dateTimeFromJson)
  final DateTime updatedAt;

  /// Set when the scale was removed. A removed scale stays listed so that
  /// climbs logged on it still resolve.
  @JsonKey(toJson: _nullableDateTimeToJson, fromJson: _nullableDateTimeFromJson)
  final DateTime? deletedAt;

  /// The levels, easiest first.
  final List<GradeScaleLevel> levels;

  /// Whether the scale was removed.
  bool get isDeleted => deletedAt != null;

  /// The level with number [level], or null when the scale has none.
  GradeScaleLevel? levelNumbered(int level) {
    for (final GradeScaleLevel candidate in levels) {
      if (candidate.level == level) return candidate;
    }
    return null;
  }

  /// Converts this [GradeScale] to JSON
  Map<String, dynamic> toJson() => _$GradeScaleToJson(this);
}

/// One level of a [GradeScale].
@JsonSerializable(includeIfNull: false)
class GradeScaleLevel {
  /// Creates a new [GradeScaleLevel] instance
  GradeScaleLevel({
    required this.level,
    required this.startGrade,
    required this.startNumeric,
    this.color,
  });

  /// Creates a [GradeScaleLevel] from JSON
  factory GradeScaleLevel.fromJson(Map<String, dynamic> json) =>
      _$GradeScaleLevelFromJson(json);

  /// Level number, from 1 (easiest).
  final int level;

  /// The grade where the level starts: a Font grade for a boulder scale,
  /// a French grade for a route scale.
  final String startGrade;

  /// The value of [startGrade] on the difficulty axis.
  final int startNumeric;

  /// Optional colour of the level, as a name or a hex value.
  final String? color;

  /// Converts this [GradeScaleLevel] to JSON
  Map<String, dynamic> toJson() => _$GradeScaleLevelToJson(this);
}

/// The level of a [GradeScale] a climb was logged on.
@JsonSerializable(includeIfNull: false)
class GradeScaleRef {
  /// Creates a new [GradeScaleRef] instance
  GradeScaleRef({required this.id, required this.level});

  /// Creates a [GradeScaleRef] from JSON
  factory GradeScaleRef.fromJson(Map<String, dynamic> json) =>
      _$GradeScaleRefFromJson(json);

  /// The id of the scale, as listed in the location's grade scales.
  final String id;

  /// The level number within the scale, from 1 (easiest).
  final int level;

  /// Converts this [GradeScaleRef] to JSON
  Map<String, dynamic> toJson() => _$GradeScaleRefToJson(this);
}

String _dateTimeToJson(DateTime dateTime) => dateTime.toUtc().toIso8601String();
DateTime _dateTimeFromJson(String json) => DateTime.parse(json);
String? _nullableDateTimeToJson(DateTime? dateTime) =>
    dateTime?.toUtc().toIso8601String();
DateTime? _nullableDateTimeFromJson(String? json) =>
    json == null ? null : DateTime.parse(json);
