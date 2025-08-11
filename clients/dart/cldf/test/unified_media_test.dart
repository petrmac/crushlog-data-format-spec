import 'package:cldf/cldf.dart';
import 'package:test/test.dart';

void main() {
  group('Unified Media Model Tests', () {
    group('MediaDesignation', () {
      test('should have all designation values', () {
        expect(MediaDesignation.values.length, equals(9));
        expect(MediaDesignation.topo.value, equals('topo'));
        expect(MediaDesignation.beta.value, equals('beta'));
        expect(MediaDesignation.approach.value, equals('approach'));
        expect(MediaDesignation.log.value, equals('log'));
        expect(MediaDesignation.overview.value, equals('overview'));
        expect(MediaDesignation.conditions.value, equals('conditions'));
        expect(MediaDesignation.gear.value, equals('gear'));
        expect(MediaDesignation.descent.value, equals('descent'));
        expect(MediaDesignation.other.value, equals('other'));
      });

      test('should parse from string value', () {
        expect(
          MediaDesignation.fromValue('topo'),
          equals(MediaDesignation.topo),
        );
        expect(
          MediaDesignation.fromValue('beta'),
          equals(MediaDesignation.beta),
        );
        expect(
          MediaDesignation.fromValue(null),
          equals(MediaDesignation.other),
        );
        expect(
          MediaDesignation.fromValue('unknown'),
          equals(MediaDesignation.other),
        );
      });
    });

    group('MediaItem with designation', () {
      test('should create media item with designation and caption', () {
        final item = MediaItem(
          type: MediaType.photo,
          path: 'media/route_topo.jpg',
          designation: MediaDesignation.topo,
          caption: 'Full route topo with pitch breakdown',
          source: MediaSource.embedded,
          timestamp: '2024-01-15T10:30:00Z',
        );

        expect(item.designation, equals(MediaDesignation.topo));
        expect(item.caption, equals('Full route topo with pitch breakdown'));
        expect(item.timestamp, equals('2024-01-15T10:30:00Z'));
      });

      test('should serialize to JSON with new fields', () {
        final item = MediaItem(
          type: MediaType.video,
          path: 'https://youtube.com/watch?v=example',
          designation: MediaDesignation.beta,
          caption: 'Beta video showing the crux sequence',
          source: MediaSource.external,
          timestamp: '2024-01-20T14:00:00Z',
        );

        final json = item.toJson();
        expect(json['designation'], equals('beta'));
        expect(json['caption'], equals('Beta video showing the crux sequence'));
        expect(json['source'], equals('external'));
        expect(json['timestamp'], equals('2024-01-20T14:00:00Z'));
      });

      test('should deserialize from JSON with new fields', () {
        final json = {
          'type': 'photo',
          'path': 'media/approach.jpg',
          'designation': 'approach',
          'caption': 'Approach trail from parking',
          'source': 'embedded',
          'timestamp': '2024-02-01T08:00:00Z',
          'metadata': {
            'coordinates': {'latitude': 37.7340, 'longitude': -119.6378},
          },
        };

        final item = MediaItem.fromJson(json);
        expect(item.designation, equals(MediaDesignation.approach));
        expect(item.caption, equals('Approach trail from parking'));
        expect(item.timestamp, equals('2024-02-01T08:00:00Z'));
        expect(item.metadata?['coordinates']['latitude'], equals(37.7340));
      });
    });

    group('Route with media', () {
      test('should create route with media', () {
        final media = Media(
          items: [
            MediaItem(
              type: MediaType.photo,
              path: 'media/dawn_wall_topo.jpg',
              designation: MediaDesignation.topo,
              source: MediaSource.embedded,
              caption: 'Full route topo',
            ),
            MediaItem(
              type: MediaType.video,
              path: 'https://youtube.com/watch?v=example',
              designation: MediaDesignation.beta,
              source: MediaSource.external,
              caption: 'Beta video',
            ),
          ],
          count: 2,
        );

        final route = Route(
          id: 101,
          locationId: 1,
          name: 'The Dawn Wall',
          routeType: RouteType.route,
          media: media,
        );

        expect(route.media, isNotNull);
        expect(route.media?.count, equals(2));
        expect(route.media?.items?.length, equals(2));
        expect(
          route.media?.items?[0].designation,
          equals(MediaDesignation.topo),
        );
        expect(
          route.media?.items?[1].designation,
          equals(MediaDesignation.beta),
        );
      });

      test('should serialize route with media to JSON', () {
        final route = Route(
          id: 102,
          locationId: 1,
          name: 'Test Route',
          routeType: RouteType.boulder,
          media: Media(
            items: [
              MediaItem(
                type: MediaType.photo,
                path: 'media/boulder_topo.jpg',
                designation: MediaDesignation.topo,
                source: MediaSource.embedded,
              ),
            ],
            count: 1,
          ),
        );

        final json = route.toJson();
        expect(json['media'], isNotNull);
        expect(json['media']['count'], equals(1));
        expect(json['media']['items'][0]['designation'], equals('topo'));
      });
    });

    group('Location with media', () {
      test('should create location with media', () {
        final media = Media(
          items: [
            MediaItem(
              type: MediaType.photo,
              path: 'media/crag_overview.jpg',
              designation: MediaDesignation.overview,
              source: MediaSource.embedded,
              caption: 'Main wall overview',
            ),
            MediaItem(
              type: MediaType.photo,
              path: 'media/crag_approach.jpg',
              designation: MediaDesignation.approach,
              source: MediaSource.embedded,
              caption: 'Approach from parking lot',
            ),
          ],
          count: 2,
        );

        final location = Location(
          id: 10,
          name: 'Test Crag',
          isIndoor: false,
          media: media,
        );

        expect(location.media, isNotNull);
        expect(location.media?.count, equals(2));
        expect(
          location.media?.items?[0].designation,
          equals(MediaDesignation.overview),
        );
        expect(
          location.media?.items?[1].designation,
          equals(MediaDesignation.approach),
        );
      });
    });

    group('Sector with media', () {
      test('should create sector with media', () {
        final media = Media(
          items: [
            MediaItem(
              type: MediaType.photo,
              path: 'media/sector_map.jpg',
              designation: MediaDesignation.overview,
              source: MediaSource.embedded,
              caption: 'Sector map with route locations',
            ),
          ],
          count: 1,
        );

        final sector = Sector(
          id: 20,
          locationId: 10,
          name: 'Main Wall',
          media: media,
        );

        expect(sector.media, isNotNull);
        expect(sector.media?.count, equals(1));
        expect(
          sector.media?.items?[0].designation,
          equals(MediaDesignation.overview),
        );
        expect(
          sector.media?.items?[0].caption,
          equals('Sector map with route locations'),
        );
      });
    });

    group('Media filtering by designation', () {
      test('should filter media items by designation', () {
        final items = [
          MediaItem(
            type: MediaType.photo,
            path: 'topo1.jpg',
            designation: MediaDesignation.topo,
          ),
          MediaItem(
            type: MediaType.video,
            path: 'beta1.mp4',
            designation: MediaDesignation.beta,
          ),
          MediaItem(
            type: MediaType.photo,
            path: 'topo2.jpg',
            designation: MediaDesignation.topo,
          ),
          MediaItem(
            type: MediaType.photo,
            path: 'overview.jpg',
            designation: MediaDesignation.overview,
          ),
        ];

        final topos = items
            .where((item) => item.designation == MediaDesignation.topo)
            .toList();
        expect(topos.length, equals(2));
        expect(topos[0].path, equals('topo1.jpg'));
        expect(topos[1].path, equals('topo2.jpg'));

        final betas = items
            .where((item) => item.designation == MediaDesignation.beta)
            .toList();
        expect(betas.length, equals(1));
        expect(betas[0].path, equals('beta1.mp4'));
      });
    });
  });
}
