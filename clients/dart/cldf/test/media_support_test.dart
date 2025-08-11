import 'dart:io';
import 'dart:typed_data';
import 'package:cldf/cldf.dart';
import 'package:test/test.dart';
import 'package:path/path.dart' as path;

void main() {
  group('Media Support Tests', () {
    late Directory tempDir;

    setUp(() {
      tempDir = Directory.systemTemp.createTempSync('cldf_media_test_');
    });

    tearDown(() {
      tempDir.deleteSync(recursive: true);
    });

    test('should create archive with embedded media files', () async {
      // Create test data
      final manifest = Manifest(
        version: '1.0.0',
        format: 'CLDF',
        creationDate: DateTime.now(),
        appVersion: '1.0.0',
        platform: Platform.desktop,
      );

      final location = Location(
        id: 1,
        name: 'Test Crag',
        country: 'US',
        isIndoor: false,
      );

      final route = Route(
        id: 1,
        locationId: 1,
        name: 'Test Route',
        routeType: RouteType.route,
      );

      final climb = Climb(
        id: 1,
        date: DateTime.now().toIso8601String().substring(
          0,
          10,
        ), // YYYY-MM-DD format
        type: ClimbType.route,
        finishType: FinishType.redpoint,
        routeId: 1,
      );

      // Create media items with proper structure
      final mediaItems = [
        MediaMetadataItem(
          id: 1,
          type: MediaType.photo,
          source: MediaSource.embedded,
          path: 'media/climb_photo.jpg',
          filename: 'climb_photo.jpg',
          climbId: 1,
          size: 50000,
          width: 1920,
          height: 1080,
        ),
        MediaMetadataItem(
          id: 2,
          type: MediaType.video,
          source: MediaSource.embedded,
          path: 'media/climb_video.mp4',
          filename: 'climb_video.mp4',
          climbId: 1,
          size: 100000,
          width: 1280,
          height: 720,
          duration: 10,
        ),
      ];

      // Create fake media file content
      final photoBytes = Uint8List.fromList('fake photo content'.codeUnits);
      final videoBytes = Uint8List.fromList('fake video content'.codeUnits);

      final mediaFiles = <String, List<int>>{
        'media/climb_photo.jpg': photoBytes,
        'media/climb_video.mp4': videoBytes,
      };

      // Create archive
      final archive = CLDFArchive(
        manifest: manifest,
        locations: [location],
        routes: [route],
        climbs: [climb],
        mediaItems: mediaItems,
        mediaFiles: mediaFiles,
      );

      // Write archive
      final writer = CLDFWriter();
      final outputFile = path.join(tempDir.path, 'test_with_media.cldf');
      await writer.writeFile(outputFile, archive);

      // Verify file exists
      expect(File(outputFile).existsSync(), isTrue);

      // Read archive back
      final reader = CLDFReader();
      final readArchive = await reader.readFile(outputFile);

      // Verify media items
      expect(readArchive.hasMedia, isTrue);
      expect(readArchive.mediaItems?.length, equals(2));
      expect(readArchive.mediaItems?[0].type, equals(MediaType.photo));
      expect(readArchive.mediaItems?[1].type, equals(MediaType.video));

      // Verify embedded media files
      expect(readArchive.hasEmbeddedMedia, isTrue);
      expect(readArchive.mediaFiles?.length, equals(2));
      expect(
        readArchive.mediaFiles?.containsKey('media/climb_photo.jpg'),
        isTrue,
      );
      expect(
        readArchive.mediaFiles?.containsKey('media/climb_video.mp4'),
        isTrue,
      );

      // Verify media file content
      final photoContent = readArchive.getMediaFile('media/climb_photo.jpg');
      expect(photoContent, equals(photoBytes));

      final videoContent = readArchive.getMediaFile('media/climb_video.mp4');
      expect(videoContent, equals(videoBytes));
    });

    test('should handle media without embedded files', () async {
      // Create test data
      final manifest = Manifest(
        version: '1.0.0',
        format: 'CLDF',
        creationDate: DateTime.now(),
        appVersion: '1.0.0',
        platform: Platform.desktop,
      );

      final location = Location(
        id: 1,
        name: 'Test Crag',
        country: 'US',
        isIndoor: false,
      );

      final climb = Climb(
        id: 1,
        date: DateTime.now().toIso8601String().substring(0, 10),
        type: ClimbType.route,
        finishType: FinishType.flash,
      );

      // Create media items without embedding (using reference)
      final mediaItems = [
        MediaMetadataItem(
          id: 1,
          type: MediaType.photo,
          source: MediaSource.reference,
          path: 'photos/climb123.jpg',
          filename: 'photo.jpg',
          climbId: 1,
        ),
      ];

      // Create archive without media files
      final archive = CLDFArchive(
        manifest: manifest,
        locations: [location],
        climbs: [climb],
        mediaItems: mediaItems,
      );

      // Write archive
      final writer = CLDFWriter();
      final outputFile = path.join(tempDir.path, 'test_reference_media.cldf');
      await writer.writeFile(outputFile, archive);

      // Read archive back
      final reader = CLDFReader();
      final readArchive = await reader.readFile(outputFile);

      // Verify media items exist but files don't
      expect(readArchive.hasMedia, isTrue);
      expect(readArchive.mediaItems?.length, equals(1));
      expect(readArchive.mediaItems?[0].source, equals(MediaSource.reference));
      expect(readArchive.hasEmbeddedMedia, isFalse);
      expect(readArchive.mediaFiles, isNull);
    });

    test('should get media file paths', () async {
      // Create archive with media
      final manifest = Manifest(
        version: '1.0.0',
        format: 'CLDF',
        creationDate: DateTime.now(),
        appVersion: '1.0.0',
        platform: Platform.desktop,
      );

      final climb = Climb(
        id: 1,
        date: DateTime.now().toIso8601String().substring(0, 10),
        type: ClimbType.boulder,
        finishType: FinishType.flash,
      );

      final mediaFiles = <String, List<int>>{
        'media/photo1.jpg': [1, 2, 3],
        'media/videos/video1.mp4': [4, 5, 6],
        'media/photos/photo2.png': [7, 8, 9],
      };

      final archive = CLDFArchive(
        manifest: manifest,
        locations: [], // Required field
        climbs: [climb],
        mediaFiles: mediaFiles,
      );

      // Test media file paths
      final paths = archive.mediaFilePaths;
      expect(paths.length, equals(3));
      expect(paths.contains('media/photo1.jpg'), isTrue);
      expect(paths.contains('media/videos/video1.mp4'), isTrue);
      expect(paths.contains('media/photos/photo2.png'), isTrue);
    });

    test('should handle media metadata correctly', () async {
      final mediaItem = MediaMetadataItem(
        id: 1,
        type: MediaType.photo,
        source: MediaSource.embedded,
        path: 'media/summit.jpg',
        filename: 'summit.jpg',
        climbId: 1,
        createdAt: DateTime.parse('2024-01-15T10:30:00Z'),
        size: 2500000,
        width: 3840,
        height: 2160,
        coordinates: MediaCoordinates(latitude: 45.1234, longitude: -122.5678),
      );

      // Create archive
      final manifest = Manifest(
        version: '1.0.0',
        format: 'CLDF',
        creationDate: DateTime.now(),
        appVersion: '1.0.0',
        platform: Platform.iOS,
      );

      final location = Location(
        id: 1,
        name: 'Test Crag',
        country: 'US',
        isIndoor: false,
      );

      final climb = Climb(
        id: 1,
        date: DateTime.now().toIso8601String().substring(0, 10),
        type: ClimbType.route,
        finishType: FinishType.onsight,
      );

      final archive = CLDFArchive(
        manifest: manifest,
        locations: [location],
        climbs: [climb],
        mediaItems: [mediaItem],
      );

      // Write and read back
      final writer = CLDFWriter();
      final outputFile = path.join(tempDir.path, 'test_metadata.cldf');
      await writer.writeFile(outputFile, archive);

      final reader = CLDFReader();
      final readArchive = await reader.readFile(outputFile);

      // Verify metadata
      final readItem = readArchive.mediaItems?[0];
      expect(readItem?.width, equals(3840));
      expect(readItem?.height, equals(2160));
      expect(readItem?.size, equals(2500000));
      expect(readItem?.coordinates?.latitude, equals(45.1234));
      expect(readItem?.coordinates?.longitude, equals(-122.5678));
    });

    test('should serialize media to JSON correctly', () {
      final mediaItem = MediaMetadataItem(
        id: 1,
        type: MediaType.video,
        source: MediaSource.external,
        path: 'https://example.com/video.mp4',
        filename: 'aerial_send.mp4',
        climbId: 1,
        size: 10000000,
        width: 1920,
        height: 1080,
        duration: 45,
      );

      final json = mediaItem.toJson();

      expect(json['id'], equals(1));
      expect(json['climbId'], equals(1));
      expect(json['type'], equals('video'));
      expect(json['source'], equals('external'));
      expect(json['path'], equals('https://example.com/video.mp4'));
      expect(json['filename'], equals('aerial_send.mp4'));
      expect(json['size'], equals(10000000));
      expect(json['width'], equals(1920));
      expect(json['height'], equals(1080));
      expect(json['duration'], equals(45));
    });

    test('should deserialize media from JSON correctly', () {
      final json = {
        'id': 42,
        'climbId': 123,
        'type': 'photo',
        'source': 'embedded',
        'path': 'media/climb.jpg',
        'filename': 'climb.jpg',
        'size': 1500000,
        'width': 2048,
        'height': 1536,
        'createdAt': '2024-01-15T10:30:00Z',
        'coordinates': {'latitude': 37.7749, 'longitude': -122.4194},
      };

      final mediaItem = MediaMetadataItem.fromJson(json);

      expect(mediaItem.id, equals(42));
      expect(mediaItem.climbId, equals(123));
      expect(mediaItem.type, equals(MediaType.photo));
      expect(mediaItem.source, equals(MediaSource.embedded));
      expect(mediaItem.path, equals('media/climb.jpg'));
      expect(mediaItem.filename, equals('climb.jpg'));
      expect(mediaItem.size, equals(1500000));
      expect(mediaItem.width, equals(2048));
      expect(mediaItem.height, equals(1536));
      expect(
        mediaItem.createdAt,
        equals(DateTime.parse('2024-01-15T10:30:00Z')),
      );
      expect(mediaItem.coordinates?.latitude, equals(37.7749));
      expect(mediaItem.coordinates?.longitude, equals(-122.4194));
    });

    test('should handle missing optional fields', () {
      final json = {
        'id': 1,
        'type': 'photo',
        'source': 'reference',
        'path': '/photos/climb.jpg',
      };

      final mediaItem = MediaMetadataItem.fromJson(json);

      expect(mediaItem.id, equals(1));
      expect(mediaItem.type, equals(MediaType.photo));
      expect(mediaItem.source, equals(MediaSource.reference));
      expect(mediaItem.path, equals('/photos/climb.jpg'));
      expect(mediaItem.climbId, isNull);
      expect(mediaItem.filename, isNull);
      expect(mediaItem.size, isNull);
      expect(mediaItem.coordinates, isNull);
    });

    test('should extract media files to directory', () async {
      // Create archive with media
      final manifest = Manifest(
        version: '1.0.0',
        format: 'CLDF',
        creationDate: DateTime.now(),
        appVersion: '1.0.0',
        platform: Platform.desktop,
      );

      final location = Location(
        id: 1,
        name: 'Test Crag',
        country: 'US',
        isIndoor: false,
      );

      final climb = Climb(
        id: 1,
        date: DateTime.now().toIso8601String().substring(0, 10),
        type: ClimbType.boulder,
        finishType: FinishType.flash,
      );

      final mediaFiles = <String, List<int>>{
        'media/photo.jpg': 'photo content'.codeUnits,
        'media/videos/send.mp4': 'video content'.codeUnits,
      };

      final archive = CLDFArchive(
        manifest: manifest,
        locations: [location],
        climbs: [climb],
        mediaFiles: mediaFiles,
      );

      // Write archive
      final writer = CLDFWriter();
      final outputFile = path.join(tempDir.path, 'archive.cldf');
      await writer.writeFile(outputFile, archive);

      // Extract media files
      final extractDir = Directory(path.join(tempDir.path, 'extracted'));
      extractDir.createSync();

      for (final entry in mediaFiles.entries) {
        final filePath = path.join(extractDir.path, entry.key);
        final file = File(filePath);
        file.createSync(recursive: true);
        file.writeAsBytesSync(entry.value);
      }

      // Verify extracted files
      expect(
        File(path.join(extractDir.path, 'media/photo.jpg')).existsSync(),
        isTrue,
      );
      expect(
        File(path.join(extractDir.path, 'media/videos/send.mp4')).existsSync(),
        isTrue,
      );

      final photoContent = File(
        path.join(extractDir.path, 'media/photo.jpg'),
      ).readAsStringSync();
      expect(photoContent, equals('photo content'));

      final videoContent = File(
        path.join(extractDir.path, 'media/videos/send.mp4'),
      ).readAsStringSync();
      expect(videoContent, equals('video content'));
    });
  });
}
