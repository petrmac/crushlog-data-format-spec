import 'package:test/test.dart';
import 'package:cldf/cldf.dart';

void main() {
  group('Platform Enum Tests', () {
    group('Serialization', () {
      test('should serialize iOS correctly', () {
        expect(Platform.iOS.value, equals('iOS'));
        // Platform enum serializes using its value field
        final manifest = Manifest(
          version: '1.0.0',
          creationDate: DateTime.now(),
          platform: Platform.iOS,
          appVersion: '1.0.0',
        );
        expect(manifest.toJson()['platform'], equals('iOS'));
      });

      test('should serialize Android correctly', () {
        expect(Platform.android.value, equals('Android'));
        final manifest = Manifest(
          version: '1.0.0',
          creationDate: DateTime.now(),
          platform: Platform.android,
          appVersion: '1.0.0',
        );
        expect(manifest.toJson()['platform'], equals('Android'));
      });

      test('should serialize Web correctly', () {
        expect(Platform.web.value, equals('Web'));
        final manifest = Manifest(
          version: '1.0.0',
          creationDate: DateTime.now(),
          platform: Platform.web,
          appVersion: '1.0.0',
        );
        expect(manifest.toJson()['platform'], equals('Web'));
      });

      test('should serialize Desktop correctly', () {
        expect(Platform.desktop.value, equals('Desktop'));
        final manifest = Manifest(
          version: '1.0.0',
          creationDate: DateTime.now(),
          platform: Platform.desktop,
          appVersion: '1.0.0',
        );
        expect(manifest.toJson()['platform'], equals('Desktop'));
      });
    });

    group('Deserialization', () {
      test('should deserialize iOS correctly', () {
        final manifest = Manifest(
          version: '1.0.0',
          creationDate: DateTime.now(),
          platform: Platform.iOS,
          appVersion: '1.0.0',
        );
        final serialized = manifest.toJson();
        final deserialized = Manifest.fromJson(serialized);
        expect(deserialized.platform, equals(Platform.iOS));
      });

      test('should deserialize Android correctly', () {
        final manifest = Manifest(
          version: '1.0.0',
          creationDate: DateTime.now(),
          platform: Platform.android,
          appVersion: '1.0.0',
        );
        final serialized = manifest.toJson();
        final deserialized = Manifest.fromJson(serialized);
        expect(deserialized.platform, equals(Platform.android));
      });

      test('should deserialize Web correctly', () {
        final manifest = Manifest(
          version: '1.0.0',
          creationDate: DateTime.now(),
          platform: Platform.web,
          appVersion: '1.0.0',
        );
        final serialized = manifest.toJson();
        final deserialized = Manifest.fromJson(serialized);
        expect(deserialized.platform, equals(Platform.web));
      });

      test('should deserialize Desktop correctly', () {
        final manifest = Manifest(
          version: '1.0.0',
          creationDate: DateTime.now(),
          platform: Platform.desktop,
          appVersion: '1.0.0',
        );
        final serialized = manifest.toJson();
        final deserialized = Manifest.fromJson(serialized);
        expect(deserialized.platform, equals(Platform.desktop));
      });
    });

    group('Validation', () {
      test('should only accept valid platform values', () {
        // All valid values from schema
        final validPlatforms = ['iOS', 'Android', 'Web', 'Desktop'];

        for (final platformValue in validPlatforms) {
          final json = {
            'version': '1.0.0',
            'format': 'CLDF',
            'creationDate': DateTime.now().toIso8601String(),
            'platform': platformValue,
            'appVersion': '1.0.0',
          };

          // Should not throw
          expect(() => Manifest.fromJson(json), returnsNormally);
        }
      });

      test('should throw on invalid platform values', () {
        final invalidPlatforms = [
          'mobile',
          'Mobile',
          'API',
          'api',
          'Unknown',
          '',
        ];

        for (final platformValue in invalidPlatforms) {
          final json = {
            'version': '1.0.0',
            'format': 'CLDF',
            'creationDate': DateTime.now().toIso8601String(),
            'platform': platformValue,
            'appVersion': '1.0.0',
          };

          // Should throw exception for invalid enum value
          expect(() => Manifest.fromJson(json), throwsException);
        }
      });

      test('should have all values match schema exactly', () {
        // From manifest.schema.json: "enum": ["iOS", "Android", "Web", "Desktop"]
        final schemaValues = ['iOS', 'Android', 'Web', 'Desktop'];
        final enumValues = Platform.values.map((p) => p.value).toList();

        expect(enumValues, equals(schemaValues));
      });
    });

    group('Enum Properties', () {
      test('should have correct number of values', () {
        expect(Platform.values.length, equals(4));
      });

      test('should have unique values', () {
        final values = Platform.values.map((p) => p.value).toList();
        final uniqueValues = values.toSet().toList();
        expect(values.length, equals(uniqueValues.length));
      });

      test('should be case-sensitive', () {
        // Ensure the enum values are exactly as specified in schema
        expect(Platform.iOS.value, equals('iOS'));
        expect(Platform.iOS.value, isNot(equals('ios')));
        expect(Platform.iOS.value, isNot(equals('IOS')));
      });
    });

    group('Integration with Manifest', () {
      test('should work correctly in full manifest serialization', () {
        final manifest = Manifest(
          version: '1.0.0',
          creationDate: DateTime.utc(2024, 1, 15, 10, 30),
          platform: Platform.android,
          appVersion: '2.5.0',
          description: 'Test export',
          author: Author(name: 'Test User', email: 'test@example.com'),
        );

        final json = manifest.toJson();
        expect(json['platform'], equals('Android'));

        final deserialized = Manifest.fromJson(json);
        expect(deserialized.platform, equals(Platform.android));
        expect(deserialized.platform.value, equals('Android'));
      });

      test('should maintain platform value in archive manifest', () {
        final archive = CLDFArchive(
          manifest: Manifest(
            version: '1.0.0',
            creationDate: DateTime.now(),
            platform: Platform.web,
            appVersion: '1.0.0',
          ),
          locations: [
            Location(
              id: 1,
              name: 'Test Location',
              country: 'US',
              isIndoor: true,
            ),
          ],
        );

        // Check platform value in manifest
        expect(archive.manifest.platform, equals(Platform.web));
        expect(archive.manifest.platform.value, equals('Web'));

        // Serialize manifest to JSON
        final manifestJson = archive.manifest.toJson();
        expect(manifestJson['platform'], equals('Web'));

        // Deserialize manifest back
        final deserializedManifest = Manifest.fromJson(manifestJson);
        expect(deserializedManifest.platform, equals(Platform.web));
        expect(deserializedManifest.platform.value, equals('Web'));
      });
    });
  });
}
