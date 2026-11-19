import '../models/enums/grade_system.dart';

/// The difficulty axis of the CrushLog Data Format.
///
/// Every grade of the five grading systems has an integer value on one
/// axis per discipline: Font and V-scale share the boulder axis; French,
/// YDS and UIAA share the route axis. A higher value is a harder grade.
/// The tables are part of the format (see `grades.md` in the specification),
/// so every reader and writer computes the same value.
class GradeAxis {
  GradeAxis._();

  static const Map<String, int> _font = <String, int>{
    '4': 0, '4A': 25, '4B': 50, '4C': 75, //
    '5A': 100, '5A+': 150, '5B': 200, '5B+': 250, '5C': 300, '5C+': 350,
    '6A': 400, '6A+': 450, '6B': 500, '6B+': 550, '6C': 600, '6C+': 650,
    '7A': 700, '7A+': 800, '7B': 900, '7B+': 950, '7C': 1000, '7C+': 1100,
    '8A': 1200, '8A+': 1300, '8B': 1400, '8B+': 1500, '8C': 1600,
    '8C+': 1700, '9A': 1800,
  };

  static const List<String> _vScale = <String>[
    'VB', 'V0', 'V1', 'V2', 'V3', 'V4', 'V5', 'V6', 'V7', 'V8', 'V9', //
    'V10', 'V11', 'V12', 'V13', 'V14', 'V15', 'V16', 'V17',
  ];

  static const List<String> _french = <String>[
    '4a', '4b', '4c', '5a', '5b', '5c', '6a', '6a+', '6b', '6b+', '6c', //
    '6c+', '7a', '7a+', '7b', '7b+', '7c', '7c+', '8a', '8a+', '8b', '8b+',
    '8c', '8c+', '9a', '9a+', '9b', '9b+', '9c', '9c+',
  ];

  static const List<String> _yds = <String>[
    '5.6', '5.7', '5.8', '5.9', '5.10a', '5.10b', '5.10c', '5.10d', //
    '5.11a', '5.11b', '5.11c', '5.11d', '5.12a', '5.12b', '5.12c', '5.12d',
    '5.13a', '5.13b', '5.13c', '5.13d', '5.14a', '5.14b', '5.14c', '5.14d',
    '5.15a', '5.15b', '5.15c', '5.15d', '5.16a', '5.16b',
  ];

  static const List<String> _uiaa = <String>[
    'IV', 'IV+', 'V-', 'V', 'V+', 'VI-', 'VI', 'VI+', 'VII-', 'VII', //
    'VII+', 'VIII-', 'VIII', 'VIII+', 'IX-', 'IX', 'IX+', 'X-', 'X', 'X+',
    'XI-', 'XI', 'XI+', 'XII-', 'XII', 'XII+', 'XIII-', 'XIII', 'XIII+',
    'XIV-',
  ];

  /// Distance between two neighbouring grades of an evenly spaced ladder.
  static const int step = 100;

  static final Map<GradeSystem, Map<String, int>> _tables =
      <GradeSystem, Map<String, int>>{
        GradeSystem.font: _font,
        GradeSystem.vScale: _evenly(_vScale),
        GradeSystem.french: _evenly(_french),
        GradeSystem.yds: _evenly(_yds),
        GradeSystem.uiaa: _evenly(_uiaa),
      };

  static Map<String, int> _evenly(List<String> grades) => <String, int>{
    for (int i = 0; i < grades.length; i++) grades[i]: i * step,
  };

  /// The grades of [system], easiest first.
  static List<String> ladder(GradeSystem system) =>
      List<String>.unmodifiable(_tables[system]!.keys);

  /// The value of [grade] on the difficulty axis, or null when [grade] is
  /// not a grade of [system]'s ladder. Grade names are case sensitive.
  static int? numericFor(GradeSystem system, String grade) =>
      _tables[system]![grade];

  /// The grade of [system] nearest to [numeric]. A value half way between
  /// two grades gives the easier one; a value outside the ladder gives its
  /// nearest end.
  static String gradeFor(GradeSystem system, int numeric) {
    final Map<String, int> table = _tables[system]!;
    String nearest = table.keys.first;
    int smallest = (table[nearest]! - numeric).abs();
    for (final MapEntry<String, int> entry in table.entries) {
      final int distance = (entry.value - numeric).abs();
      if (distance < smallest) {
        smallest = distance;
        nearest = entry.key;
      }
    }
    return nearest;
  }
}
