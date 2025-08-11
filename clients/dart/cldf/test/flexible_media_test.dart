import 'package:cldf/cldf.dart';
import 'package:test/test.dart';

void main() {
  group('Flexible Media Model Tests', () {
    group('MediaItem', () {
      test('should create media item with required fields', () {
        final item = MediaItem(
          type: MediaType.photo,
          path: '/path/to/photo.jpg',
        );

        expect(item.type, equals(MediaType.photo));
        expect(item.path, equals('/path/to/photo.jpg'));
        expect(item.assetId, isNull);
        expect(item.thumbnailPath, isNull);
        expect(item.source, isNull);
        expect(item.metadata, isNull);
      });

      test('should create media item with all fields', () {
        final metadata = {'width': 1920, 'height': 1080, 'size': 500000};

        final item = MediaItem(
          type: MediaType.video,
          path: 'https://example.com/video.mp4',
          assetId: 'PHAsset123',
          thumbnailPath: '/thumbs/video_thumb.jpg',
          source: MediaSource.cloud,
          metadata: metadata,
        );

        expect(item.type, equals(MediaType.video));
        expect(item.path, equals('https://example.com/video.mp4'));
        expect(item.assetId, equals('PHAsset123'));
        expect(item.thumbnailPath, equals('/thumbs/video_thumb.jpg'));
        expect(item.source, equals(MediaSource.cloud));
        expect(item.metadata, equals(metadata));
      });

      test('should serialize to JSON', () {
        final item = MediaItem(
          type: MediaType.photo,
          path: 'media/climb_photo.jpg',
          source: MediaSource.embedded,
          thumbnailPath: 'media/thumbs/climb_photo_thumb.jpg',
        );

        final json = item.toJson();
        expect(json['type'], equals('photo'));
        expect(json['path'], equals('media/climb_photo.jpg'));
        expect(json['source'], equals('embedded'));
        expect(
          json['thumbnailPath'],
          equals('media/thumbs/climb_photo_thumb.jpg'),
        );
      });

      test('should deserialize from JSON', () {
        final json = {
          'type': 'video',
          'path': '/videos/send.mp4',
          'assetId': 'asset-456',
          'source': 'local',
          'metadata': {'duration': 45.5, 'width': 1280, 'height': 720},
        };

        final item = MediaItem.fromJson(json);
        expect(item.type, equals(MediaType.video));
        expect(item.path, equals('/videos/send.mp4'));
        expect(item.assetId, equals('asset-456'));
        expect(item.source, equals(MediaSource.local));
        expect(item.metadata?['duration'], equals(45.5));
        expect(item.metadata?['width'], equals(1280));
        expect(item.metadata?['height'], equals(720));
      });
    });

    group('Media', () {
      test('should create empty media', () {
        final media = Media();
        expect(media.items, isNull);
        expect(media.count, isNull);
      });

      test('should create media with items', () {
        final items = [
          MediaItem(type: MediaType.photo, path: 'photo1.jpg'),
          MediaItem(type: MediaType.video, path: 'video1.mp4'),
        ];

        final media = Media(items: items, count: 2);

        expect(media.items?.length, equals(2));
        expect(media.count, equals(2));
        expect(media.items?[0].type, equals(MediaType.photo));
        expect(media.items?[1].type, equals(MediaType.video));
      });

      test('should serialize to JSON', () {
        final media = Media(
          items: [
            MediaItem(
              type: MediaType.photo,
              path: 'IMG_1234.jpg',
              source: MediaSource.reference,
            ),
          ],
          count: 1,
        );

        final json = media.toJson();
        expect(json['count'], equals(1));
        expect(json['items'], isList);
        expect(json['items'].length, equals(1));
        expect(json['items'][0]['type'], equals('photo'));
        expect(json['items'][0]['path'], equals('IMG_1234.jpg'));
        expect(json['items'][0]['source'], equals('reference'));
      });

      test('should deserialize from JSON', () {
        final json = {
          'items': [
            {'type': 'photo', 'path': 'photo1.jpg', 'source': 'embedded'},
            {
              'type': 'video',
              'path': 'video1.mp4',
              'source': 'cloud',
              'assetId': 'asset123',
            },
          ],
          'count': 2,
        };

        final media = Media.fromJson(json);
        expect(media.count, equals(2));
        expect(media.items?.length, equals(2));
        expect(media.items?[0].type, equals(MediaType.photo));
        expect(media.items?[0].source, equals(MediaSource.embedded));
        expect(media.items?[1].type, equals(MediaType.video));
        expect(media.items?[1].source, equals(MediaSource.cloud));
        expect(media.items?[1].assetId, equals('asset123'));
      });
    });

    group('MediaSource Enum', () {
      test('should have new source values', () {
        expect(MediaSource.values, contains(MediaSource.local));
        expect(MediaSource.values, contains(MediaSource.cloud));
        expect(MediaSource.values, contains(MediaSource.reference));
        expect(MediaSource.values, contains(MediaSource.embedded));
      });

      test('should have correct string values', () {
        expect(MediaSource.local.value, equals('local'));
        expect(MediaSource.cloud.value, equals('cloud'));
        expect(MediaSource.reference.value, equals('reference'));
        expect(MediaSource.embedded.value, equals('embedded'));
      });

      test('should maintain backward compatibility', () {
        expect(MediaSource.values, contains(MediaSource.external));
        expect(MediaSource.values, contains(MediaSource.photosLibrary));
        expect(MediaSource.photosLibrary.value, equals('photos_library'));
      });
    });

    group('Climb with flexible Media', () {
      test('should create climb with media', () {
        final media = Media(
          items: [
            MediaItem(
              type: MediaType.photo,
              path: 'climbs/send_photo.jpg',
              source: MediaSource.local,
            ),
          ],
          count: 1,
        );

        final climb = Climb(
          id: 1,
          date: '2024-01-15',
          type: ClimbType.boulder,
          finishType: FinishType.flash,
          media: media,
        );

        expect(climb.media, isNotNull);
        expect(climb.media?.count, equals(1));
        expect(climb.media?.items?.first.type, equals(MediaType.photo));
      });

      test('should serialize climb with media to JSON', () {
        final climb = Climb(
          id: 2,
          date: '2024-01-20',
          type: ClimbType.route,
          finishType: FinishType.redpoint,
          routeName: 'Test Route',
          media: Media(
            items: [
              MediaItem(
                type: MediaType.video,
                path: 'videos/send.mp4',
                thumbnailPath: 'thumbs/send_thumb.jpg',
                source: MediaSource.embedded,
              ),
            ],
            count: 1,
          ),
        );

        final json = climb.toJson();
        expect(json['media'], isNotNull);
        expect(json['media']['count'], equals(1));
        expect(json['media']['items'][0]['type'], equals('video'));
        expect(json['media']['items'][0]['source'], equals('embedded'));
      });
    });

    group('CrushLog Conversion Example', () {
      test('should convert from CrushLog media model', () {
        // Simulating CrushLog's ClimbMedia structure
        final hasPhoto = true;
        final photoPath = '/photos/climb123.jpg';
        final String? photoAssetId = 'PHAsset789';
        final photoThumbnailPath = '/thumbs/climb123_thumb.jpg';

        final hasVideo = true;
        final videoPath = '/videos/climb123.mp4';
        final String? videoAssetId = 'PHAsset790';
        final videoThumbnailPath = '/thumbs/climb123_video_thumb.jpg';

        // Convert to CLDF Media
        final items = <MediaItem>[];

        if (hasPhoto) {
          items.add(
            MediaItem(
              type: MediaType.photo,
              path: photoPath,
              assetId: photoAssetId,
              thumbnailPath: photoThumbnailPath,
              source: photoAssetId != null
                  ? MediaSource.cloud
                  : MediaSource.local,
            ),
          );
        }

        if (hasVideo) {
          items.add(
            MediaItem(
              type: MediaType.video,
              path: videoPath,
              assetId: videoAssetId,
              thumbnailPath: videoThumbnailPath,
              source: videoAssetId != null
                  ? MediaSource.cloud
                  : MediaSource.local,
            ),
          );
        }

        final media = Media(items: items, count: items.length);

        expect(media.count, equals(2));
        expect(media.items?.length, equals(2));
        expect(media.items?[0].type, equals(MediaType.photo));
        expect(media.items?[0].source, equals(MediaSource.cloud));
        expect(media.items?[1].type, equals(MediaType.video));
        expect(media.items?[1].source, equals(MediaSource.cloud));
      });

      test('should handle simple reference-only media', () {
        // Simple case - just photo references
        final media = Media(
          items: [
            MediaItem(
              type: MediaType.photo,
              path: 'IMG_1234.jpg',
              source: MediaSource.reference,
            ),
          ],
          count: 1,
        );

        expect(media.items?.length, equals(1));
        expect(media.items?[0].source, equals(MediaSource.reference));
        expect(media.items?[0].assetId, isNull);
        expect(media.items?[0].thumbnailPath, isNull);
      });
    });
  });
}
