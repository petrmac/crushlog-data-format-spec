import 'dart:io';

import 'package:cldf/cldf.dart';
import 'package:path/path.dart' as p;
import 'package:test/test.dart';

void main() {
  group('Round-trip Integration Tests', () {
    late Directory tempDir;
    late String testFilePath;

    setUp(() {
      tempDir = Directory.systemTemp.createTempSync('cldf_round_trip_test_');
      testFilePath = p.join(tempDir.path, 'test.cldf');
    });

    tearDown(() {
      if (tempDir.existsSync()) {
        tempDir.deleteSync(recursive: true);
      }
    });

    test(
      'should export and import minimal archive without data loss',
      () async {
        // Create minimal test data
        final originalArchive = CLDFArchive(
          manifest: Manifest(
            version: '1.0.0',
            creationDate: DateTime(2024, 1, 15, 10, 30),
            platform: Platform.iOS,
            appVersion: '2.5.0',
            author: Author(name: 'Test User', email: 'test@example.com'),
          ),
          locations: [
            Location(
              id: 1,
              name: 'Test Crag',
              isIndoor: false,
              country: 'USA',
              state: 'Colorado',
              city: 'Boulder',
              coordinates: Coordinates(latitude: 40.0150, longitude: -105.2705),
            ),
          ],
        );

        // Export to file
        final writer = CLDFWriter();
        await writer.writeFile(testFilePath, originalArchive);

        // Verify file exists
        expect(File(testFilePath).existsSync(), isTrue);

        // Import from file
        final reader = CLDFReader();
        final importedArchive = await reader.readFile(testFilePath);

        // Verify manifest
        expect(
          importedArchive.manifest.version,
          originalArchive.manifest.version,
        );
        expect(importedArchive.manifest.format, 'CLDF');
        expect(
          importedArchive.manifest.platform,
          originalArchive.manifest.platform,
        );
        expect(
          importedArchive.manifest.appVersion,
          originalArchive.manifest.appVersion,
        );
        expect(
          importedArchive.manifest.author?.name,
          originalArchive.manifest.author?.name,
        );
        expect(
          importedArchive.manifest.author?.email,
          originalArchive.manifest.author?.email,
        );

        // Verify locations
        expect(importedArchive.locations.length, 1);
        final importedLocation = importedArchive.locations.first;
        final originalLocation = originalArchive.locations.first;
        expect(importedLocation.id, originalLocation.id);
        expect(importedLocation.name, originalLocation.name);
        expect(importedLocation.isIndoor, originalLocation.isIndoor);
        expect(importedLocation.country, originalLocation.country);
        expect(importedLocation.state, originalLocation.state);
        expect(importedLocation.city, originalLocation.city);
        expect(
          importedLocation.coordinates?.latitude,
          originalLocation.coordinates?.latitude,
        );
        expect(
          importedLocation.coordinates?.longitude,
          originalLocation.coordinates?.longitude,
        );
      },
    );

    test(
      'should export and import complete archive without data loss',
      () async {
        // Create comprehensive test data
        final originalArchive = CLDFArchive(
          manifest: Manifest(
            version: '1.0.0',
            creationDate: DateTime(2024, 1, 15, 10, 30),
            platform: Platform.android,
            appVersion: '2.5.0',
            author: Author(
              name: 'Test User',
              email: 'test@example.com',
              website: 'https://example.com',
            ),
            source: 'TestApp',
          ),
          locations: [
            Location(
              id: 1,
              name: 'Test Gym',
              isIndoor: true,
              country: 'USA',
              state: 'Colorado',
              city: 'Denver',
              address: '123 Climbing St',
              starred: true,
              rockType: RockType.granite,
              terrainType: TerrainType.artificial,
              accessInfo: 'Open 24/7',
            ),
            Location(
              id: 2,
              name: 'Test Crag',
              isIndoor: false,
              country: 'USA',
              state: 'Colorado',
              city: 'Boulder',
              coordinates: Coordinates(latitude: 40.0150, longitude: -105.2705),
              rockType: RockType.granite,
              terrainType: TerrainType.natural,
            ),
          ],
          sectors: [
            Sector(
              id: 1,
              locationId: 2,
              name: 'Main Wall',
              isDefault: true,
              approach: 'Follow the trail for 10 minutes',
              coordinates: Coordinates(latitude: 40.0155, longitude: -105.2710),
            ),
          ],
          routes: [
            Route(
              id: 1,
              locationId: 1,
              name: 'Test Boulder V4',
              routeType: RouteType.boulder,
              grades: {'V': 'V4', 'Font': '6A+'},
              color: '#FF0000',
              qualityRating: 4,
            ),
            Route(
              id: 2,
              locationId: 2,
              sectorId: 1,
              name: 'Classic Route',
              routeType: RouteType.route,
              routeCharacteristics: RouteCharacteristics.bolted,
              grades: {'YDS': '5.10a', 'French': '6a'},
              height: 25.5,
              firstAscent: FirstAscent(
                name: 'John Doe',
                date: '1995-06-15',
                info: 'First ascent on a sunny day',
              ),
              protectionRating: ProtectionRating.good,
              qualityRating: 5,
              beta: 'Crux at the third bolt',
              gearNotes: 'Bring 12 quickdraws',
              tags: ['classic', 'endurance'],
            ),
          ],
          sessions: [
            Session(
              id: 1,
              date: '2024-01-15',
              locationId: 1,
              sessionType: SessionType.indoorClimbing,
              notes: 'Great training session',
              isOngoing: false,
              climbType: ClimbType.boulder,
              rockType: RockType.granite,
              terrainType: TerrainType.artificial,
            ),
          ],
          climbs: [
            Climb(
              id: 1,
              sessionId: 1,
              routeId: 1,
              date: '2024-01-15',
              time: '14:30:00',
              type: ClimbType.boulder,
              finishType: FinishType.flash,
              attempts: 1,
              repeats: 0,
              isRepeat: false,
              duration: 5,
              height: 4.5,
              rating: 4,
              notes: 'Felt solid',
              tags: ['crimpy', 'technical'],
              beta: 'Use the heel hook',
              color: '#FF0000',
              rockType: RockType.granite,
              terrainType: TerrainType.artificial,
              isIndoor: true,
              partners: ['Jane Doe'],
              weather: null,
            ),
          ],
          tags: [],
          mediaItems: [
            MediaMetadataItem(
              id: 1,
              filename: 'test.jpg',
              type: MediaType.photo,
              source: MediaSource.local,
              path: 'test.jpg',
              createdAt: DateTime(2024, 1, 15),
              width: 1920,
              height: 1080,
              size: 1024000,
              mimeType: 'image/jpeg',
            ),
          ],
        );

        // Export to file
        final writer = CLDFWriter();
        await writer.writeFile(testFilePath, originalArchive);

        // Import from file
        final reader = CLDFReader();
        final importedArchive = await reader.readFile(testFilePath);

        // Verify all data is preserved
        expect(
          importedArchive.locations.length,
          originalArchive.locations.length,
        );
        expect(
          importedArchive.sectors?.length,
          originalArchive.sectors?.length,
        );
        expect(importedArchive.routes?.length, originalArchive.routes?.length);
        expect(
          importedArchive.sessions?.length,
          originalArchive.sessions?.length,
        );
        expect(importedArchive.climbs?.length, originalArchive.climbs?.length);
        expect(importedArchive.tags ?? [], originalArchive.tags ?? []);
        expect(
          importedArchive.mediaItems?.length,
          originalArchive.mediaItems?.length,
        );

        // Verify detailed route data
        final importedRoute = importedArchive.routes![1];
        final originalRoute = originalArchive.routes![1];
        expect(importedRoute.id, originalRoute.id);
        expect(importedRoute.name, originalRoute.name);
        expect(importedRoute.routeType, originalRoute.routeType);
        expect(
          importedRoute.routeCharacteristics,
          originalRoute.routeCharacteristics,
        );
        expect(importedRoute.grades, originalRoute.grades);
        expect(importedRoute.height, originalRoute.height);
        expect(
          importedRoute.firstAscent?.name,
          originalRoute.firstAscent?.name,
        );
        expect(
          importedRoute.firstAscent?.date,
          originalRoute.firstAscent?.date,
        );
        expect(importedRoute.protectionRating, originalRoute.protectionRating);
        expect(importedRoute.qualityRating, originalRoute.qualityRating);
        expect(importedRoute.beta, originalRoute.beta);
        expect(importedRoute.gearNotes, originalRoute.gearNotes);
        expect(importedRoute.tags, originalRoute.tags);

        // Verify climb data with enums
        final importedClimb = importedArchive.climbs!.first;
        final originalClimb = originalArchive.climbs!.first;
        expect(importedClimb.type, originalClimb.type);
        expect(importedClimb.finishType, originalClimb.finishType);
        expect(importedClimb.rockType, originalClimb.rockType);
        expect(importedClimb.terrainType, originalClimb.terrainType);
        expect(importedClimb.partners, originalClimb.partners);

        // Verify session enums
        final importedSession = importedArchive.sessions!.first;
        final originalSession = originalArchive.sessions!.first;
        expect(importedSession.sessionType, originalSession.sessionType);
        expect(importedSession.climbType, originalSession.climbType);
        expect(importedSession.rockType, originalSession.rockType);
        expect(importedSession.terrainType, originalSession.terrainType);

        // Verify stats are calculated
        expect(importedArchive.manifest.stats, isNotNull);
        expect(importedArchive.manifest.stats!.locationsCount, 2);
        expect(importedArchive.manifest.stats!.sectorsCount, 1);
        expect(importedArchive.manifest.stats!.routesCount, 2);
        expect(importedArchive.manifest.stats!.sessionsCount, 1);
        expect(importedArchive.manifest.stats!.climbsCount, 1);
        expect(importedArchive.manifest.stats!.tagsCount, 0);
        expect(importedArchive.manifest.stats!.mediaCount, 1);
      },
    );

    test('should handle archive with media files', () async {
      // Create test media content
      final mediaContent = List<int>.generate(1024, (i) => i % 256);

      final originalArchive = CLDFArchive(
        manifest: Manifest(
          version: '1.0.0',
          creationDate: DateTime.now(),
          platform: Platform.web,
          appVersion: '1.0.0',
        ),
        locations: [Location(id: 1, name: 'Test Location', isIndoor: true)],
        mediaItems: [
          MediaMetadataItem(
            id: 1,
            filename: 'test-image.jpg',
            type: MediaType.photo,
            source: MediaSource.embedded,
            path: 'media/test-image.jpg',
          ),
        ],
        mediaFiles: {'media/test-image.jpg': mediaContent},
      );

      // Export and import
      final writer = CLDFWriter();
      await writer.writeFile(testFilePath, originalArchive);

      final reader = CLDFReader();
      final importedArchive = await reader.readFile(testFilePath);

      // Verify media metadata
      expect(importedArchive.mediaItems?.length, 1);
      expect(importedArchive.mediaItems!.first.filename, 'test-image.jpg');
      expect(importedArchive.mediaItems!.first.source, MediaSource.embedded);

      // Verify media file content
      expect(importedArchive.hasEmbeddedMedia, isTrue);
      expect(importedArchive.mediaFiles?.length, 1);
      expect(importedArchive.mediaFiles!['media/test-image.jpg'], mediaContent);
    });

    test('should preserve custom fields', () async {
      final customData = {
        'customField1': 'value1',
        'customField2': 42,
        'nestedField': {'key': 'value'},
      };

      final originalArchive = CLDFArchive(
        manifest: Manifest(
          version: '1.0.0',
          creationDate: DateTime.now(),
          platform: Platform.desktop,
          appVersion: '1.0.0',
        ),
        locations: [
          Location(
            id: 1,
            name: 'Test Location',
            isIndoor: false,
            customFields: customData,
          ),
        ],
      );

      // Export and import
      final writer = CLDFWriter();
      await writer.writeFile(testFilePath, originalArchive);

      final reader = CLDFReader();
      final importedArchive = await reader.readFile(testFilePath);

      // Verify custom fields are preserved
      final importedLocation = importedArchive.locations.first;
      expect(importedLocation.customFields, isNotNull);
      expect(importedLocation.customFields, customData);
    });

    test('should handle empty optional collections', () async {
      final originalArchive = CLDFArchive(
        manifest: Manifest(
          version: '1.0.0',
          creationDate: DateTime.now(),
          platform: Platform.iOS,
          appVersion: '1.0.0',
        ),
        locations: [Location(id: 1, name: 'Test Location', isIndoor: true)],
        // All optional collections are null or empty
        sectors: null,
        routes: [],
        sessions: null,
        climbs: [],
        tags: null,
        mediaItems: [],
      );

      // Export and import
      final writer = CLDFWriter();
      await writer.writeFile(testFilePath, originalArchive);

      final reader = CLDFReader();
      final importedArchive = await reader.readFile(testFilePath);

      // Verify handling of null/empty collections
      expect(importedArchive.locations.length, 1);
      expect(importedArchive.sectors, isNull);
      expect(importedArchive.routes ?? [], isEmpty);
      expect(importedArchive.sessions, isNull);
      expect(importedArchive.climbs ?? [], isEmpty);
      expect(importedArchive.tags, isNull);
      expect(importedArchive.mediaItems ?? [], isEmpty);
    });
  });
}
