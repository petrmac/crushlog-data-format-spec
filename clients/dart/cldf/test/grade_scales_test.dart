import 'dart:io';

import 'package:cldf/cldf.dart';
import 'package:path/path.dart' as p;
import 'package:test/test.dart';

void main() {
  group('difficulty axis', () {
    test('Font and V-scale share the axis', () {
      expect(GradeAxis.numericFor(GradeSystem.font, '4'), 0);
      expect(GradeAxis.numericFor(GradeSystem.font, '4A'), 25);
      expect(GradeAxis.numericFor(GradeSystem.font, '5A+'), 150);
      expect(GradeAxis.numericFor(GradeSystem.font, '6A'), 400);
      expect(GradeAxis.numericFor(GradeSystem.font, '6A+'), 450);
      expect(GradeAxis.numericFor(GradeSystem.font, '7A+'), 800);
      expect(GradeAxis.numericFor(GradeSystem.font, '7B+'), 950);
      expect(GradeAxis.numericFor(GradeSystem.font, '9A'), 1800);

      expect(GradeAxis.numericFor(GradeSystem.vScale, 'VB'), 0);
      expect(GradeAxis.numericFor(GradeSystem.vScale, 'V3'), 400);
      expect(GradeAxis.numericFor(GradeSystem.vScale, 'V17'), 1800);
    });

    test('route systems share the axis', () {
      expect(GradeAxis.numericFor(GradeSystem.french, '4a'), 0);
      expect(GradeAxis.numericFor(GradeSystem.french, '6a'), 600);
      expect(GradeAxis.numericFor(GradeSystem.french, '9c+'), 2900);
      expect(GradeAxis.numericFor(GradeSystem.yds, '5.6'), 0);
      expect(GradeAxis.numericFor(GradeSystem.yds, '5.10a'), 400);
      expect(GradeAxis.numericFor(GradeSystem.yds, '5.16b'), 2900);
      expect(GradeAxis.numericFor(GradeSystem.uiaa, 'IV'), 0);
      expect(GradeAxis.numericFor(GradeSystem.uiaa, 'VII'), 900);
      expect(GradeAxis.numericFor(GradeSystem.uiaa, 'XIV-'), 2900);
    });

    test('every ladder is strictly increasing', () {
      for (final GradeSystem system in GradeSystem.values) {
        final List<String> ladder = GradeAxis.ladder(system);
        expect(ladder, isNotEmpty, reason: system.value);
        for (int i = 1; i < ladder.length; i++) {
          expect(
            GradeAxis.numericFor(system, ladder[i])!,
            greaterThan(GradeAxis.numericFor(system, ladder[i - 1])!),
            reason: '${system.value} ${ladder[i]}',
          );
        }
      }
      expect(GradeAxis.ladder(GradeSystem.font), hasLength(29));
      expect(GradeAxis.ladder(GradeSystem.vScale), hasLength(19));
      expect(GradeAxis.ladder(GradeSystem.french), hasLength(30));
      expect(GradeAxis.ladder(GradeSystem.yds), hasLength(30));
      expect(GradeAxis.ladder(GradeSystem.uiaa), hasLength(30));
    });

    test('a grade that is not on the ladder has no value', () {
      expect(GradeAxis.numericFor(GradeSystem.font, '6A/B'), isNull);
      expect(GradeAxis.numericFor(GradeSystem.vScale, 'V3-4'), isNull);
      expect(GradeAxis.numericFor(GradeSystem.french, ''), isNull);
    });

    test('a value becomes the nearest grade, ties going to the easier', () {
      expect(GradeAxis.gradeFor(GradeSystem.font, 400), '6A');
      expect(GradeAxis.gradeFor(GradeSystem.font, 450), '6A+');
      expect(GradeAxis.gradeFor(GradeSystem.vScale, 450), 'V3');
      expect(GradeAxis.gradeFor(GradeSystem.vScale, 451), 'V4');
      expect(GradeAxis.gradeFor(GradeSystem.vScale, -10), 'VB');
      expect(GradeAxis.gradeFor(GradeSystem.vScale, 99999), 'V17');
    });
  });

  group('climb grades', () {
    test('an archive written before these fields reads as before', () {
      final GradeInfo grades = GradeInfo.fromJson(<String, dynamic>{
        'system': 'font',
        'grade': '6A',
      });
      expect(grades.numeric, isNull);
      expect(grades.estimated, isNull);
      expect(grades.scale, isNull);
      // The value is computed when the file does not carry it.
      expect(grades.effectiveNumeric, 400);
      expect(grades.toJson(), <String, dynamic>{
        'system': 'font',
        'grade': '6A',
      });
    });

    test('a level climb keeps its value, estimate mark and level', () {
      final Map<String, dynamic> json = <String, dynamic>{
        'system': 'font',
        'grade': '6A',
        'numeric': 400,
        'estimated': true,
        'scale': <String, dynamic>{
          'id': '6f1c2a9e-0b7d-4c55-9a3e-2d8f4b1c7e10',
          'level': 3,
        },
      };
      final GradeInfo grades = GradeInfo.fromJson(json);
      expect(grades.numeric, 400);
      expect(grades.estimated, isTrue);
      expect(grades.scale!.id, '6f1c2a9e-0b7d-4c55-9a3e-2d8f4b1c7e10');
      expect(grades.scale!.level, 3);
      expect(grades.effectiveNumeric, 400);
      expect(
        GradeInfo.fromJson(grades.toJson()).toJson(),
        grades.toJson(),
        reason: 'round trip',
      );
      expect(grades.toJson()['scale'], json['scale']);
    });

    test('the stored value wins over the computed one', () {
      final GradeInfo grades = GradeInfo.fromJson(<String, dynamic>{
        'system': 'font',
        'grade': '6A',
        'numeric': 425,
      });
      expect(grades.effectiveNumeric, 425);
    });

    test('an unknown grade has no value', () {
      final GradeInfo grades = GradeInfo(
        system: GradeSystem.font,
        grade: 'blue',
      );
      expect(grades.effectiveNumeric, isNull);
    });
  });

  group('location grade scales', () {
    final Map<String, dynamic> scaleJson = <String, dynamic>{
      'id': '6f1c2a9e-0b7d-4c55-9a3e-2d8f4b1c7e10',
      'climbType': 'boulder',
      'spread': 'widerTop',
      'updatedAt': '2026-11-19T10:00:00.000Z',
      'levels': <Map<String, dynamic>>[
        <String, dynamic>{'level': 1, 'startGrade': '4', 'startNumeric': 0},
        <String, dynamic>{
          'level': 2,
          'startGrade': '5C',
          'startNumeric': 300,
          'color': 'blue',
        },
        <String, dynamic>{'level': 3, 'startGrade': '6A', 'startNumeric': 400},
      ],
    };

    test('a location without scales reads and writes as before', () {
      final Location location = Location.fromJson(<String, dynamic>{
        'id': 1,
        'name': 'Gym',
        'isIndoor': true,
      });
      expect(location.gradeScales, isNull);
      expect(location.toJson().containsKey('gradeScales'), isFalse);
    });

    test('scales round-trip through JSON', () {
      final Location location = Location.fromJson(<String, dynamic>{
        'id': 1,
        'name': 'Gym',
        'isIndoor': true,
        'gradeScales': <Map<String, dynamic>>[scaleJson],
      });
      final GradeScale scale = location.gradeScales!.single;
      expect(scale.id, '6f1c2a9e-0b7d-4c55-9a3e-2d8f4b1c7e10');
      expect(scale.climbType, ClimbType.boulder);
      expect(scale.spread, GradeScaleSpread.widerTop);
      expect(scale.updatedAt.toUtc(), DateTime.utc(2026, 11, 19, 10));
      expect(scale.deletedAt, isNull);
      expect(scale.levels, hasLength(3));
      expect(scale.levels[1].startGrade, '5C');
      expect(scale.levels[1].startNumeric, 300);
      expect(scale.levels[1].color, 'blue');
      expect(scale.levels[0].color, isNull);

      final Location again = Location.fromJson(
        Map<String, dynamic>.from(location.toJson()),
      );
      expect(again.gradeScales!.single.toJson(), scale.toJson());
    });

    test('a removed scale keeps its levels and carries deletedAt', () {
      final GradeScale scale = GradeScale.fromJson(<String, dynamic>{
        ...scaleJson,
        'deletedAt': '2026-11-20T08:00:00.000Z',
      });
      expect(scale.isDeleted, isTrue);
      expect(scale.levels, hasLength(3));
    });

    test('a level is found by number', () {
      final GradeScale scale = GradeScale.fromJson(scaleJson);
      expect(scale.levelNumbered(2)!.startGrade, '5C');
      expect(scale.levelNumbered(9), isNull);
    });
  });

  group('archive', () {
    late Directory tempDir;

    setUp(() {
      tempDir = Directory.systemTemp.createTempSync('cldf_grade_scales_');
    });

    tearDown(() {
      if (tempDir.existsSync()) tempDir.deleteSync(recursive: true);
    });

    test('scales and level climbs survive export and import', () async {
      final String path = p.join(tempDir.path, 'scales.cldf');
      final CLDFArchive original = CLDFArchive(
        manifest: Manifest(
          version: '1.0.0',
          creationDate: DateTime(2026, 11, 19, 10, 30),
          platform: Platform.iOS,
          appVersion: '1.3.0',
        ),
        locations: <Location>[
          Location(
            id: 1,
            name: 'Gym',
            isIndoor: true,
            gradeScales: <GradeScale>[
              GradeScale(
                id: '6f1c2a9e-0b7d-4c55-9a3e-2d8f4b1c7e10',
                climbType: ClimbType.boulder,
                spread: GradeScaleSpread.even,
                updatedAt: DateTime.utc(2026, 11, 19, 10),
                levels: <GradeScaleLevel>[
                  GradeScaleLevel(level: 1, startGrade: '4', startNumeric: 0),
                  GradeScaleLevel(
                    level: 2,
                    startGrade: '6A',
                    startNumeric: 400,
                    color: 'red',
                  ),
                ],
              ),
            ],
          ),
        ],
        sessions: <Session>[
          Session(id: 1, date: '2026-11-19', location: 'Gym', locationId: 1),
        ],
        climbs: <Climb>[
          Climb(
            id: 1,
            sessionId: 1,
            date: '2026-11-19',
            type: ClimbType.boulder,
            finishType: FinishType.top,
            grades: GradeInfo(
              system: GradeSystem.font,
              grade: '6A',
              numeric: 400,
              estimated: true,
              scale: GradeScaleRef(
                id: '6f1c2a9e-0b7d-4c55-9a3e-2d8f4b1c7e10',
                level: 2,
              ),
            ),
          ),
        ],
      );

      await CLDFWriter().writeFile(path, original);
      final CLDFArchive read = await CLDFReader().readFile(path);

      final GradeScale scale = read.locations.single.gradeScales!.single;
      expect(
        scale.toJson(),
        original.locations.single.gradeScales!.single.toJson(),
      );
      final GradeInfo grades = read.climbs!.single.grades!;
      expect(grades.grade, '6A');
      expect(grades.numeric, 400);
      expect(grades.estimated, isTrue);
      expect(grades.scale!.id, '6f1c2a9e-0b7d-4c55-9a3e-2d8f4b1c7e10');
      expect(grades.scale!.level, 2);
    });
  });
}
