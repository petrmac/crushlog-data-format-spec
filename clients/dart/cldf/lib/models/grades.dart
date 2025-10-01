import 'package:json_annotation/json_annotation.dart';

part 'grades.g.dart';

/// Strongly typed grades with enum keys
@JsonSerializable(includeIfNull: false)
class Grades {
  /// V-Scale grade (bouldering)
  @JsonKey(name: 'vScale')
  final String? vScale;

  /// Yosemite Decimal System grade
  @JsonKey(name: 'yds')
  final String? yds;

  /// French sport climbing grade
  @JsonKey(name: 'french')
  final String? french;

  /// Fontainebleau bouldering grade
  @JsonKey(name: 'font')
  final String? font;

  /// UIAA grade
  @JsonKey(name: 'uiaa')
  final String? uiaa;

  const Grades({this.vScale, this.yds, this.french, this.font, this.uiaa});

  factory Grades.fromJson(Map<String, dynamic> json) => _$GradesFromJson(json);
  Map<String, dynamic> toJson() => _$GradesToJson(this);

  /// Create a copy with updated fields
  Grades copyWith({
    String? vScale,
    String? yds,
    String? french,
    String? font,
    String? uiaa,
  }) {
    return Grades(
      vScale: vScale ?? this.vScale,
      yds: yds ?? this.yds,
      french: french ?? this.french,
      font: font ?? this.font,
      uiaa: uiaa ?? this.uiaa,
    );
  }

  /// Get grade by system name
  String? getGrade(String system) {
    switch (system.toLowerCase()) {
      case 'vscale':
      case 'v_scale':
        return vScale;
      case 'yds':
        return yds;
      case 'french':
        return french;
      case 'font':
      case 'fontainebleau':
        return font;
      case 'uiaa':
        return uiaa;
      default:
        return null;
    }
  }

  /// Set grade for a specific system
  Grades setGrade(String system, String grade) {
    switch (system.toLowerCase()) {
      case 'vscale':
      case 'v_scale':
        return copyWith(vScale: grade);
      case 'yds':
        return copyWith(yds: grade);
      case 'french':
        return copyWith(french: grade);
      case 'font':
      case 'fontainebleau':
        return copyWith(font: grade);
      case 'uiaa':
        return copyWith(uiaa: grade);
      default:
        return this;
    }
  }

  /// Check if any grades are present
  bool get hasGrades =>
      vScale != null ||
      yds != null ||
      french != null ||
      font != null ||
      uiaa != null;

  /// Convert to simplified Map format for backwards compatibility
  Map<String, String> toMap() {
    final map = <String, String>{};
    if (vScale != null) map['vScale'] = vScale!;
    if (yds != null) map['yds'] = yds!;
    if (french != null) map['french'] = french!;
    if (font != null) map['font'] = font!;
    if (uiaa != null) map['uiaa'] = uiaa!;
    return map;
  }

  /// Create from Map for backwards compatibility
  factory Grades.fromMap(Map<String, String> map) {
    return Grades(
      vScale: map['vScale'],
      yds: map['yds'],
      french: map['french'],
      font: map['font'],
      uiaa: map['uiaa'],
    );
  }
}
