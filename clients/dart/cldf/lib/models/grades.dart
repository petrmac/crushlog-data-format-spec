import 'package:json_annotation/json_annotation.dart';

part 'grades.g.dart';

/// Strongly typed grades with enum keys
@JsonSerializable()
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

  /// British technical grade
  @JsonKey(name: 'british')
  final String? british;

  /// Australian grade
  @JsonKey(name: 'australian')
  final String? australian;

  /// Saxon grade
  @JsonKey(name: 'saxon')
  final String? saxon;

  /// Ewbank grade
  @JsonKey(name: 'ewbank')
  final String? ewbank;

  /// Nordic grade
  @JsonKey(name: 'nordic')
  final String? nordic;

  /// Brazilian grade
  @JsonKey(name: 'brazilian')
  final String? brazilian;

  const Grades({
    this.vScale,
    this.yds,
    this.french,
    this.font,
    this.uiaa,
    this.british,
    this.australian,
    this.saxon,
    this.ewbank,
    this.nordic,
    this.brazilian,
  });

  factory Grades.fromJson(Map<String, dynamic> json) => _$GradesFromJson(json);
  Map<String, dynamic> toJson() => _$GradesToJson(this);

  /// Create a copy with updated fields
  Grades copyWith({
    String? vScale,
    String? yds,
    String? french,
    String? font,
    String? uiaa,
    String? british,
    String? australian,
    String? saxon,
    String? ewbank,
    String? nordic,
    String? brazilian,
  }) {
    return Grades(
      vScale: vScale ?? this.vScale,
      yds: yds ?? this.yds,
      french: french ?? this.french,
      font: font ?? this.font,
      uiaa: uiaa ?? this.uiaa,
      british: british ?? this.british,
      australian: australian ?? this.australian,
      saxon: saxon ?? this.saxon,
      ewbank: ewbank ?? this.ewbank,
      nordic: nordic ?? this.nordic,
      brazilian: brazilian ?? this.brazilian,
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
      case 'british':
        return british;
      case 'australian':
        return australian;
      case 'saxon':
        return saxon;
      case 'ewbank':
        return ewbank;
      case 'nordic':
        return nordic;
      case 'brazilian':
        return brazilian;
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
      case 'british':
        return copyWith(british: grade);
      case 'australian':
        return copyWith(australian: grade);
      case 'saxon':
        return copyWith(saxon: grade);
      case 'ewbank':
        return copyWith(ewbank: grade);
      case 'nordic':
        return copyWith(nordic: grade);
      case 'brazilian':
        return copyWith(brazilian: grade);
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
      uiaa != null ||
      british != null ||
      australian != null ||
      saxon != null ||
      ewbank != null ||
      nordic != null ||
      brazilian != null;

  /// Convert to simplified Map format for backwards compatibility
  Map<String, String> toMap() {
    final map = <String, String>{};
    if (vScale != null) map['vScale'] = vScale!;
    if (yds != null) map['yds'] = yds!;
    if (french != null) map['french'] = french!;
    if (font != null) map['font'] = font!;
    if (uiaa != null) map['uiaa'] = uiaa!;
    if (british != null) map['british'] = british!;
    if (australian != null) map['australian'] = australian!;
    if (saxon != null) map['saxon'] = saxon!;
    if (ewbank != null) map['ewbank'] = ewbank!;
    if (nordic != null) map['nordic'] = nordic!;
    if (brazilian != null) map['brazilian'] = brazilian!;
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
      british: map['british'],
      australian: map['australian'],
      saxon: map['saxon'],
      ewbank: map['ewbank'],
      nordic: map['nordic'],
      brazilian: map['brazilian'],
    );
  }
}
