import 'dart:convert';

import 'package:cldf/cldf.dart';
import 'package:test/test.dart';

void main() {
  group('DateTime Serialization (Java Compatibility)', () {
    const converter = FlexibleDateTimeConverter();

    test('should parse ISO 8601 formats with timezone', () {
      // Test cases from Java FlexibleDateTimeDeserializerSpec
      final testCases = {
        '2024-01-29T12:00:00.000+00:00': DateTime.utc(2024, 1, 29, 12),
        '2024-01-29T12:00:00.000Z': DateTime.utc(2024, 1, 29, 12),
        '2024-01-29T12:00:00+00:00': DateTime.utc(2024, 1, 29, 12),
        '2024-01-29T12:00:00Z': DateTime.utc(2024, 1, 29, 12),
        '2024-01-29T12:00:00.123+00:00': DateTime.utc(
          2024,
          1,
          29,
          12,
          0,
          0,
          123,
        ),
        '2024-01-29T12:00:00.123Z': DateTime.utc(2024, 1, 29, 12, 0, 0, 123),
      };

      for (final entry in testCases.entries) {
        final parsed = converter.fromJson(entry.key);
        expect(parsed, isNotNull);
        expect(parsed!.toUtc(), equals(entry.value));
      }
    });

    test('should handle timezone offsets', () {
      // Note: DateTime.parse handles timezone offsets correctly
      final testCases = {
        '2024-01-29T12:00:00-05:00': DateTime.utc(
          2024,
          1,
          29,
          17,
        ), // EST to UTC
        '2024-01-29T12:00:00+02:00': DateTime.utc(
          2024,
          1,
          29,
          10,
        ), // CEST to UTC
        '2024-06-15T14:30:00+05:30': DateTime.utc(2024, 6, 15, 9), // IST to UTC
      };

      for (final entry in testCases.entries) {
        final parsed = converter.fromJson(entry.key);
        expect(parsed, isNotNull);
        expect(parsed!.toUtc(), equals(entry.value));
      }
    });

    test('should handle edge cases', () {
      final testCases = {
        '2024-12-31T23:59:59+14:00': true, // Valid timezone offset
        '2024-01-01T00:00:00-12:00': true, // Valid timezone offset
        '2024-06-15T10:30:45.999Z': true, // Milliseconds
      };

      for (final entry in testCases.entries) {
        expect(() => converter.fromJson(entry.key), returnsNormally);
      }
    });

    test('should handle null and empty strings', () {
      expect(converter.fromJson(null), isNull);
      expect(converter.fromJson(''), isNull);
      expect(converter.fromJson('  '), isNull);
    });

    test('should handle whitespace', () {
      final result = converter.fromJson('  2024-01-29T12:00:00Z  ');
      expect(result, equals(DateTime.utc(2024, 1, 29, 12)));
    });

    test('should throw on invalid formats', () {
      final invalidFormats = [
        'invalid-date',
        '2024/01/29 12:00:00',
        'Jan 29, 2024 12:00 PM',
        '2024-01-29', // Date only, no time
      ];

      for (final invalid in invalidFormats) {
        expect(
          () => converter.fromJson(invalid),
          throwsA(isA<FormatException>()),
          reason: 'Should throw for: $invalid',
        );
      }
    });

    test('should serialize to ISO 8601 UTC', () {
      final date = DateTime(2024, 1, 29, 12);
      final result = converter.toJson(date);
      expect(
        result,
        matches(RegExp(r'^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$')),
      );
    });
  });

  group('LocalDate Serialization (Java Compatibility)', () {
    const converter = FlexibleLocalDateConverter();

    test('should parse various date formats', () {
      // Test cases from Java FlexibleLocalDateDeserializerSpec
      final testCases = {
        '2024-01-29': '2024-01-29',
        '2024/01/29': '2024-01-29',
        '2024.01.29': '2024-01-29',
        '01/29/2024': '2024-01-29',
        '29/01/2024': '2024-01-29',
        '29.01.2024': '2024-01-29',
        '01-29-2024': '2024-01-29',
        '29-01-2024': '2024-01-29',
        '20240129': '2024-01-29',
      };

      for (final entry in testCases.entries) {
        final result = converter.fromJson(entry.key);
        expect(
          result,
          equals(entry.value),
          reason: 'Failed for input: ${entry.key}',
        );
      }
    });

    test('should handle edge cases', () {
      final testCases = {
        '2024-02-29': '2024-02-29', // Leap year
        '2024-01-01': '2024-01-01', // New Year's Day
        '2024-12-31': '2024-12-31', // New Year's Eve
        '2024/12/25': '2024-12-25', // Slash format
        '12/25/2024': '2024-12-25', // US format
        '25/12/2024': '2024-12-25', // European format
      };

      for (final entry in testCases.entries) {
        final result = converter.fromJson(entry.key);
        expect(result, equals(entry.value));
      }
    });

    test('should handle null and empty strings', () {
      expect(converter.fromJson(null), isNull);
      expect(converter.fromJson(''), isNull);
      expect(converter.fromJson('  '), isNull);
    });

    test('should handle whitespace', () {
      final result = converter.fromJson('  2024-01-29  ');
      expect(result, equals('2024-01-29'));
    });

    test('should throw on invalid formats', () {
      final invalidFormats = [
        'not-a-date-at-all',
        '2024-13-01', // Invalid month
        'totally-invalid',
      ];

      for (final invalid in invalidFormats) {
        expect(
          () => converter.fromJson(invalid),
          throwsA(isA<FormatException>()),
          reason: 'Should throw for: $invalid',
        );
      }
    });

    test('should always output ISO format', () {
      final result = converter.toJson('2024-01-29');
      expect(result, equals('2024-01-29'));
    });
  });

  group('Model Serialization Integration', () {
    test('Manifest with flexible datetime should serialize/deserialize', () {
      final manifest = Manifest(
        version: '1.0.0',
        creationDate: DateTime.utc(2024, 1, 29, 12),
        platform: Platform.iOS,
        appVersion: '1.0.0',
      );

      final json = manifest.toJson();
      expect(
        json['creationDate'],
        matches(RegExp(r'^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$')),
      );

      // Test round trip
      final decoded = Manifest.fromJson(json);
      expect(decoded.creationDate.toUtc(), equals(manifest.creationDate));
    });

    test('Climb with flexible date should serialize/deserialize', () {
      final climb = Climb(
        id: 1,
        date: '2024-01-29',
        type: ClimbType.route,
        finishType: FinishType.redpoint,
        routeName: 'Test Route',
      );

      final json = climb.toJson();
      expect(json['date'], equals('2024-01-29'));

      // Test round trip
      final decoded = Climb.fromJson(json);
      expect(decoded.date, equals(climb.date));
    });

    test('Session with flexible date should serialize/deserialize', () {
      final session = Session(
        id: 1,
        date: '2024-01-29',
        location: 'Test Location',
      );

      final json = session.toJson();
      expect(json['date'], equals('2024-01-29'));

      // Test round trip
      final decoded = Session.fromJson(json);
      expect(decoded.date, equals(session.date));
    });
  });

  group('Java Interoperability', () {
    test('should deserialize Java-serialized Manifest', () {
      // JSON from Java test
      final javaJson = '''
        {
          "version": "1.0.0",
          "format": "CLDF",
          "creationDate": "2024-01-29T12:00:00Z",
          "appVersion": "2.1.0",
          "platform": "Desktop"
        }
      ''';

      final manifest = Manifest.fromJson(json.decode(javaJson));
      expect(manifest.version, equals('1.0.0'));
      expect(
        manifest.creationDate.toUtc(),
        equals(DateTime.utc(2024, 1, 29, 12)),
      );
    });

    test(
      'should deserialize Java-serialized Session with various date formats',
      () {
        final dateFormats = [
          '2024-01-29',
          '2024/01/29',
          '01/29/2024',
          '29/01/2024',
        ];

        for (final dateFormat in dateFormats) {
          final javaJson =
              '''
          {
            "id": 1,
            "date": "$dateFormat",
            "location": "Test Location"
          }
        ''';

          final session = Session.fromJson(json.decode(javaJson));
          expect(
            session.date,
            equals('2024-01-29'),
            reason: 'Failed for format: $dateFormat',
          );
        }
      },
    );
  });

  group('Date String Validation', () {
    test('should validate ISO date strings', () {
      expect('2024-01-29'.isValidIsoDate, isTrue);
      expect('2024-02-29'.isValidIsoDate, isTrue); // Leap year
      expect('2023-02-29'.isValidIsoDate, isFalse); // Not leap year
      expect('2024-13-01'.isValidIsoDate, isFalse); // Invalid month
      expect('2024-01-32'.isValidIsoDate, isFalse); // Invalid day
      expect('not-a-date'.isValidIsoDate, isFalse);
    });
  });
}
