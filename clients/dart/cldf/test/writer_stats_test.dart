import 'dart:io';
import 'package:test/test.dart';
import 'package:cldf/cldf.dart';
import 'package:cldf/models/enums/route_type.dart';

void main() {
  group('CLDFWriter Stats Calculation', () {
    late Directory tempDir;

    setUp(() {
      tempDir = Directory.systemTemp.createTempSync('cldf_writer_stats_test_');
    });

    tearDown(() {
      if (tempDir.existsSync()) {
        tempDir.deleteSync(recursive: true);
      }
    });

    test('should automatically calculate stats when not provided', () async {
      // Create test data
      final locations = [
        Location(
          id: 1,
          name: 'Test Gym',
          isIndoor: true,
        ),
        Location(
          id: 2,
          name: 'Outdoor Crag',
          isIndoor: false,
        ),
      ];

      final sectors = [
        Sector(
          id: 1,
          locationId: 2,
          name: 'Main Wall',
        ),
      ];

      final routes = [
        Route(
          id: 1,
          locationId: 2,
          sectorId: 1,
          name: 'Test Route',
          routeType: RouteType.boulder,
        ),
        Route(
          id: 2,
          locationId: 2,
          sectorId: 1,
          name: 'Another Route',
          routeType: RouteType.route,
        ),
      ];

      final sessions = [
        Session(
          id: 1,
          date: '2024-01-15',
          location: 'Test Gym',
          locationId: 1,
          isIndoor: true,
        ),
      ];

      final climbs = [
        Climb(
          id: 1,
          sessionId: 1,
          date: '2024-01-15',
          routeName: 'Boulder Problem',
          type: ClimbType.boulder,
          finishType: FinishType.top,
          attempts: 3,
          grades: GradeInfo(
            system: GradeSystem.vScale,
            grade: 'V4',
          ),
          isIndoor: true,
        ),
        Climb(
          id: 2,
          sessionId: 1,
          date: '2024-01-15',
          routeName: 'Another Problem',
          type: ClimbType.boulder,
          finishType: FinishType.flash,
          attempts: 1,
          grades: GradeInfo(
            system: GradeSystem.vScale,
            grade: 'V3',
          ),
          isIndoor: true,
        ),
      ];

      final tags = [
        Tag(
          id: 1,
          name: 'project',
          color: '#FF5733',
          isPredefined: true,
        ),
      ];

      final mediaItems = [
        MediaItem(
          id: 1,
          climbId: 1,
          type: MediaType.photo,
          source: MediaSource.local,
          path: 'media/climb1.jpg',
          filename: 'climb1.jpg',
        ),
        MediaItem(
          id: 2,
          climbId: 2,
          type: MediaType.video,
          source: MediaSource.local,
          path: 'media/climb2.mp4',
          filename: 'climb2.mp4',
        ),
      ];

      // Create manifest without stats
      final manifest = Manifest(
        version: '1.0.0',
        format: 'CLDF',
        creationDate: DateTime.now(),
        platform: Platform.iOS,
        appVersion: '1.0.0',
        // stats is intentionally null
      );

      // Create archive
      final archive = CLDFArchive(
        manifest: manifest,
        locations: locations,
        sectors: sectors,
        routes: routes,
        sessions: sessions,
        climbs: climbs,
        tags: tags,
        mediaItems: mediaItems,
      );

      // Write archive
      final writer = CLDFWriter();
      final outputPath = '${tempDir.path}/test_stats.cldf';
      await writer.writeFile(outputPath, archive);

      // Read back the archive
      final reader = CLDFReader();
      final readArchive = await reader.readFile(outputPath);

      // Verify stats were calculated
      expect(readArchive.manifest.stats, isNotNull);
      expect(readArchive.manifest.stats!.locationsCount, equals(2));
      expect(readArchive.manifest.stats!.sectorsCount, equals(1));
      expect(readArchive.manifest.stats!.routesCount, equals(2));
      expect(readArchive.manifest.stats!.sessionsCount, equals(1));
      expect(readArchive.manifest.stats!.climbsCount, equals(2));
      expect(readArchive.manifest.stats!.tagsCount, equals(1));
      expect(readArchive.manifest.stats!.mediaCount, equals(2));
    });

    test('should preserve existing stats when provided', () async {
      // Create test data with minimal content
      final locations = [
        Location(
          id: 1,
          name: 'Test Location',
          isIndoor: true,
        ),
      ];

      // Create manifest with custom stats (different from actual counts)
      final customStats = Stats(
        locationsCount: 100,
        sectorsCount: 50,
        routesCount: 200,
        sessionsCount: 10,
        climbsCount: 500,
        tagsCount: 20,
        mediaCount: 1000,
      );

      final manifest = Manifest(
        version: '1.0.0',
        format: 'CLDF',
        creationDate: DateTime.now(),
        platform: Platform.android,
        appVersion: '2.0.0',
        stats: customStats,
      );

      // Create archive with minimal data
      final archive = CLDFArchive(
        manifest: manifest,
        locations: locations,
      );

      // Write archive
      final writer = CLDFWriter();
      final outputPath = '${tempDir.path}/test_preserve_stats.cldf';
      await writer.writeFile(outputPath, archive);

      // Read back the archive
      final reader = CLDFReader();
      final readArchive = await reader.readFile(outputPath);

      // Verify custom stats were preserved (not recalculated)
      expect(readArchive.manifest.stats, isNotNull);
      expect(readArchive.manifest.stats!.locationsCount, equals(100));
      expect(readArchive.manifest.stats!.sectorsCount, equals(50));
      expect(readArchive.manifest.stats!.routesCount, equals(200));
      expect(readArchive.manifest.stats!.sessionsCount, equals(10));
      expect(readArchive.manifest.stats!.climbsCount, equals(500));
      expect(readArchive.manifest.stats!.tagsCount, equals(20));
      expect(readArchive.manifest.stats!.mediaCount, equals(1000));
    });
  });
}