import 'package:test/test.dart';
import 'package:cldf/cldf.dart';

void main() {
  group('Manifest Model Tests', () {
    test('should create Manifest with required fields', () {
      final now = DateTime.now();
      final manifest = Manifest(
        version: '1.0.0',
        creationDate: now,
        platform: Platform.iOS,
        appVersion: '2.3.4',
      );

      expect(manifest.version, equals('1.0.0'));
      expect(manifest.format, equals('CLDF'));
      expect(manifest.creationDate, equals(now));
      expect(manifest.platform, equals(Platform.iOS));
      expect(manifest.appVersion, equals('2.3.4'));
      expect(manifest.author, isNull);
      expect(manifest.exportOptions, isNull);
    });

    test('should create Manifest with all fields', () {
      final now = DateTime.now();
      final author = Author(
        name: 'John Doe',
        email: 'john@example.com',
        website: 'https://example.com',
      );
      final exportOptions = ExportConfig(
        includeMedia: true,
        mediaStrategy: 'embedded',
      );

      final manifest = Manifest(
        version: '1.0.0',
        format: 'CLDF',
        creationDate: now,
        platform: Platform.iOS,
        appVersion: '3.0.0',
        author: author,
        exportOptions: exportOptions,
      );

      expect(manifest.author, isNotNull);
      expect(manifest.author!.name, equals('John Doe'));
      expect(manifest.exportOptions, isNotNull);
      expect(manifest.exportOptions!.includeMedia, isTrue);
    });

    test('should serialize and deserialize Manifest', () {
      final now = DateTime.now();
      final manifest = Manifest(
        version: '1.0.0',
        creationDate: now,
        platform: Platform.iOS,
        appVersion: '2.3.4',
      );

      final json = manifest.toJson();
      final deserialized = Manifest.fromJson(json);

      expect(deserialized.version, equals(manifest.version));
      expect(deserialized.format, equals(manifest.format));
      expect(
        deserialized.creationDate.toUtc().toIso8601String(),
        equals(manifest.creationDate.toUtc().toIso8601String()),
      );
      expect(deserialized.platform, equals(manifest.platform));
      expect(deserialized.appVersion, equals(manifest.appVersion));
    });
  });

  group('Author Model Tests', () {
    test('should create Author with all fields', () {
      final author = Author(
        name: 'Jane Doe',
        email: 'jane@example.com',
        website: 'https://janedoe.com',
      );

      expect(author.name, equals('Jane Doe'));
      expect(author.email, equals('jane@example.com'));
      expect(author.website, equals('https://janedoe.com'));
    });

    test('should create Author with optional fields', () {
      final author = Author(name: 'John');

      expect(author.name, equals('John'));
      expect(author.email, isNull);
      expect(author.website, isNull);
    });

    test('should serialize and deserialize Author', () {
      final author = Author(
        name: 'Test User',
        email: 'test@example.com',
        website: 'https://testuser.com',
      );

      final json = author.toJson();
      final deserialized = Author.fromJson(json);

      expect(deserialized.name, equals(author.name));
      expect(deserialized.email, equals(author.email));
      expect(deserialized.website, equals(author.website));
    });
  });

  group('ExportConfig Model Tests', () {
    test('should create ExportConfig with defaults', () {
      final config = ExportConfig();

      expect(config.includeMedia, isFalse);
      expect(config.mediaStrategy, isNull);
    });

    test('should create ExportConfig with all fields', () {
      final config = ExportConfig(
        includeMedia: true,
        mediaStrategy: 'reference',
      );

      expect(config.includeMedia, isTrue);
      expect(config.mediaStrategy, equals('reference'));
    });

    test('should serialize and deserialize ExportConfig', () {
      final config = ExportConfig(
        includeMedia: true,
        mediaStrategy: 'embedded',
      );

      final json = config.toJson();
      final deserialized = ExportConfig.fromJson(json);

      expect(deserialized.includeMedia, equals(config.includeMedia));
      expect(deserialized.mediaStrategy, equals(config.mediaStrategy));
    });
  });
}
