import 'package:test/test.dart';
import 'package:cldf/cldf.dart';

void main() {
  group('Manifest Model Tests', () {
    test('should create Manifest with required fields', () {
      final now = DateTime.now();
      final manifest = Manifest(
        version: '1.0.0',
        creationDate: now,
        platform: Platform.mobile,
        appVersion: '2.3.4',
      );

      expect(manifest.version, equals('1.0.0'));
      expect(manifest.format, equals('CLDF'));
      expect(manifest.creationDate, equals(now));
      expect(manifest.platform, equals(Platform.mobile));
      expect(manifest.appVersion, equals('2.3.4'));
      expect(manifest.description, isNull);
      expect(manifest.creator, isNull);
      expect(manifest.exportConfig, isNull);
    });

    test('should create Manifest with all fields', () {
      final now = DateTime.now();
      final creator = Creator(
        name: 'John Doe',
        email: 'john@example.com',
        userId: 'user123',
      );
      final exportConfig = ExportConfig(
        includeMedia: true,
        mediaStrategy: 'embedded',
        mediaQuality: 80,
        anonymized: false,
      );

      final manifest = Manifest(
        version: '1.0.0',
        format: 'CLDF',
        creationDate: now,
        platform: Platform.mobile,
        appVersion: '3.0.0',
        description: 'Test export',
        creator: creator,
        exportConfig: exportConfig,
      );

      expect(manifest.description, equals('Test export'));
      expect(manifest.creator, isNotNull);
      expect(manifest.creator!.name, equals('John Doe'));
      expect(manifest.exportConfig, isNotNull);
      expect(manifest.exportConfig!.includeMedia, isTrue);
    });

    test('should serialize and deserialize Manifest', () {
      final now = DateTime.now();
      final manifest = Manifest(
        version: '1.0.0',
        creationDate: now,
        platform: Platform.mobile,
        appVersion: '2.3.4',
        description: 'Test manifest',
      );

      final json = manifest.toJson();
      final deserialized = Manifest.fromJson(json);

      expect(deserialized.version, equals(manifest.version));
      expect(deserialized.format, equals(manifest.format));
      expect(
        deserialized.creationDate.toIso8601String(),
        equals(manifest.creationDate.toIso8601String()),
      );
      expect(deserialized.platform, equals(manifest.platform));
      expect(deserialized.appVersion, equals(manifest.appVersion));
      expect(deserialized.description, equals(manifest.description));
    });
  });

  group('Creator Model Tests', () {
    test('should create Creator with all fields', () {
      final creator = Creator(
        name: 'Jane Doe',
        email: 'jane@example.com',
        userId: 'user456',
      );

      expect(creator.name, equals('Jane Doe'));
      expect(creator.email, equals('jane@example.com'));
      expect(creator.userId, equals('user456'));
    });

    test('should create Creator with optional fields', () {
      final creator = Creator(name: 'John');

      expect(creator.name, equals('John'));
      expect(creator.email, isNull);
      expect(creator.userId, isNull);
    });

    test('should serialize and deserialize Creator', () {
      final creator = Creator(
        name: 'Test User',
        email: 'test@example.com',
        userId: 'test123',
      );

      final json = creator.toJson();
      final deserialized = Creator.fromJson(json);

      expect(deserialized.name, equals(creator.name));
      expect(deserialized.email, equals(creator.email));
      expect(deserialized.userId, equals(creator.userId));
    });
  });

  group('ExportConfig Model Tests', () {
    test('should create ExportConfig with defaults', () {
      final config = ExportConfig();

      expect(config.includeMedia, isFalse);
      expect(config.mediaStrategy, isNull);
      expect(config.mediaQuality, isNull);
      expect(config.anonymized, isNull);
    });

    test('should create ExportConfig with all fields', () {
      final config = ExportConfig(
        includeMedia: true,
        mediaStrategy: 'reference',
        mediaQuality: 90,
        anonymized: true,
      );

      expect(config.includeMedia, isTrue);
      expect(config.mediaStrategy, equals('reference'));
      expect(config.mediaQuality, equals(90));
      expect(config.anonymized, isTrue);
    });

    test('should serialize and deserialize ExportConfig', () {
      final config = ExportConfig(
        includeMedia: true,
        mediaStrategy: 'embedded',
        mediaQuality: 85,
        anonymized: false,
      );

      final json = config.toJson();
      final deserialized = ExportConfig.fromJson(json);

      expect(deserialized.includeMedia, equals(config.includeMedia));
      expect(deserialized.mediaStrategy, equals(config.mediaStrategy));
      expect(deserialized.mediaQuality, equals(config.mediaQuality));
      expect(deserialized.anonymized, equals(config.anonymized));
    });
  });
}
