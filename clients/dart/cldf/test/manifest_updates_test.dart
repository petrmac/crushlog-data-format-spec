import 'package:test/test.dart';
import 'package:cldf/cldf.dart';

void main() {
  group('Manifest Updates Tests', () {
    group('Stats', () {
      test('should serialize and deserialize Stats correctly', () {
        final stats = Stats(
          climbsCount: 100,
          sessionsCount: 20,
          locationsCount: 5,
          routesCount: 50,
          tagsCount: 15,
          mediaCount: 25,
        );

        final json = stats.toJson();
        expect(json['climbsCount'], equals(100));
        expect(json['sessionsCount'], equals(20));
        expect(json['locationsCount'], equals(5));
        expect(json['routesCount'], equals(50));
        expect(json['tagsCount'], equals(15));
        expect(json['mediaCount'], equals(25));

        final deserialized = Stats.fromJson(json);
        expect(deserialized.climbsCount, equals(100));
        expect(deserialized.sessionsCount, equals(20));
        expect(deserialized.locationsCount, equals(5));
        expect(deserialized.routesCount, equals(50));
        expect(deserialized.tagsCount, equals(15));
        expect(deserialized.mediaCount, equals(25));
      });

      test('should handle null values in Stats', () {
        final stats = Stats();
        final json = stats.toJson();

        expect(json['climbsCount'], isNull);
        expect(json['sessionsCount'], isNull);

        final deserialized = Stats.fromJson({});
        expect(deserialized.climbsCount, isNull);
        expect(deserialized.sessionsCount, isNull);
      });
    });

    group('DateRange', () {
      test('should serialize and deserialize DateRange correctly', () {
        final dateRange = DateRange(start: '2024-01-01', end: '2024-12-31');

        final json = dateRange.toJson();
        expect(json['start'], equals('2024-01-01'));
        expect(json['end'], equals('2024-12-31'));

        final deserialized = DateRange.fromJson(json);
        expect(deserialized.start, equals('2024-01-01'));
        expect(deserialized.end, equals('2024-12-31'));
      });
    });

    group('ExportConfig with DateRange', () {
      test('should serialize and deserialize ExportConfig with DateRange', () {
        final config = ExportConfig(
          includeMedia: true,
          mediaStrategy: 'thumbnails',
          dateRange: DateRange(start: '2024-01-01', end: '2024-12-31'),
        );

        final json = config.toJson();
        expect(json['includeMedia'], isTrue);
        expect(json['mediaStrategy'], equals('thumbnails'));
        expect(json['dateRange'], isNotNull);
        expect(json['dateRange']['start'], equals('2024-01-01'));
        expect(json['dateRange']['end'], equals('2024-12-31'));

        final deserialized = ExportConfig.fromJson(json);
        expect(deserialized.includeMedia, isTrue);
        expect(deserialized.mediaStrategy, equals('thumbnails'));
        expect(deserialized.dateRange, isNotNull);
        expect(deserialized.dateRange!.start, equals('2024-01-01'));
        expect(deserialized.dateRange!.end, equals('2024-12-31'));
      });
    });

    group('Manifest with new fields', () {
      test(
        'should serialize and deserialize Manifest with stats and source',
        () {
          final manifest = Manifest(
            version: '1.0.0',
            creationDate: DateTime.utc(2024, 1, 15),
            platform: Platform.iOS,
            appVersion: '2.0.0',
            source: 'CrushLog',
            stats: Stats(
              climbsCount: 100,
              sessionsCount: 20,
              locationsCount: 5,
              routesCount: 50,
              tagsCount: 15,
              mediaCount: 25,
            ),
            exportOptions: ExportConfig(
              includeMedia: true,
              dateRange: DateRange(start: '2024-01-01', end: '2024-12-31'),
            ),
          );

          final json = manifest.toJson();
          expect(json['source'], equals('CrushLog'));
          expect(json['stats'], isNotNull);
          expect(json['stats']['climbsCount'], equals(100));
          expect(json['exportOptions'], isNotNull);
          expect(json['exportOptions']['dateRange'], isNotNull);

          final deserialized = Manifest.fromJson(json);
          expect(deserialized.source, equals('CrushLog'));
          expect(deserialized.stats, isNotNull);
          expect(deserialized.stats!.climbsCount, equals(100));
          expect(deserialized.exportOptions, isNotNull);
          expect(deserialized.exportOptions!.dateRange, isNotNull);
        },
      );

      test('should use author field and map to JSON correctly', () {
        final manifest = Manifest(
          version: '1.0.0',
          creationDate: DateTime.utc(2024, 1, 15),
          platform: Platform.iOS,
          appVersion: '2.0.0',
          author: Author(
            name: 'John Doe',
            email: 'john@example.com',
            website: 'https://example.com',
          ),
        );

        final json = manifest.toJson();
        expect(json['author'], isNotNull);
        expect(json['author']['name'], equals('John Doe'));
        expect(json['author']['email'], equals('john@example.com'));
        expect(json['author']['website'], equals('https://example.com'));
        expect(json['creator'], isNull); // Should not have creator in JSON

        final deserialized = Manifest.fromJson(json);
        expect(deserialized.author, isNotNull);
        expect(deserialized.author!.name, equals('John Doe'));
        expect(deserialized.author!.email, equals('john@example.com'));
        expect(deserialized.author!.website, equals('https://example.com'));
      });

      test('should read author field from JSON', () {
        final json = {
          'version': '1.0.0',
          'format': 'CLDF',
          'creationDate': '2024-01-15T00:00:00.000Z',
          'platform': 'iOS',
          'appVersion': '1.0.0',
          'author': {
            'name': 'Test User',
            'email': 'test@example.com',
            'website': 'https://example.com',
          },
        };

        final manifest = Manifest.fromJson(json);
        expect(manifest.author, isNotNull);
        expect(manifest.author!.name, equals('Test User'));
        expect(manifest.author!.email, equals('test@example.com'));
        expect(manifest.author!.website, equals('https://example.com'));
      });

      test('should handle missing author field', () {
        final json = {
          'version': '1.0.0',
          'format': 'CLDF',
          'creationDate': '2024-01-15T00:00:00.000Z',
          'platform': 'iOS',
          'appVersion': '1.0.0',
        };

        final manifest = Manifest.fromJson(json);
        expect(manifest.author, isNull);
      });
    });

    group('Author with website field', () {
      test('should serialize and deserialize Author with website', () {
        final author = Author(
          name: 'John Doe',
          email: 'john@example.com',
          website: 'https://example.com',
        );

        final json = author.toJson();
        expect(json['name'], equals('John Doe'));
        expect(json['email'], equals('john@example.com'));
        expect(json['website'], equals('https://example.com'));
        expect(json.containsKey('userId'), isFalse);

        final deserialized = Author.fromJson(json);
        expect(deserialized.name, equals('John Doe'));
        expect(deserialized.email, equals('john@example.com'));
        expect(deserialized.website, equals('https://example.com'));
      });
    });
  });
}
