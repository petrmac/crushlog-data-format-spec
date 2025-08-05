import 'package:cldf/cldf.dart';
import 'package:test/test.dart';

void main() {
  group('Updated Model Tests', () {
    group('Tag Model', () {
      test('should create tag with isPredefined field', () {
        final tag = Tag(
          id: 1,
          name: 'overhang',
          isPredefined: true,
          predefinedTagKey: 'overhang',
          category: 'terrain',
        );

        expect(tag.id, equals(1));
        expect(tag.name, equals('overhang'));
        expect(tag.isPredefined, isTrue);
        expect(tag.predefinedTagKey, equals('overhang'));
        expect(tag.category, equals('terrain'));
        expect(tag.fullName, equals('terrain:overhang'));
      });

      test('should serialize tag with isPredefined to JSON', () {
        final tag = Tag(
          id: 1,
          name: 'custom tag',
          isPredefined: false,
          description: 'A custom tag',
          color: '#FF0000',
        );

        final json = tag.toJson();
        expect(json['id'], equals(1));
        expect(json['name'], equals('custom tag'));
        expect(json['isPredefined'], isFalse);
        expect(json['predefinedTagKey'], isNull);
        expect(json['description'], equals('A custom tag'));
        expect(json['color'], equals('#FF0000'));
      });

      test('should deserialize tag from JSON', () {
        final json = {
          'id': 2,
          'name': 'crimpy',
          'isPredefined': true,
          'predefinedTagKey': 'crimpy',
          'category': 'style',
        };

        final tag = Tag.fromJson(json);
        expect(tag.id, equals(2));
        expect(tag.name, equals('crimpy'));
        expect(tag.isPredefined, isTrue);
        expect(tag.predefinedTagKey, equals('crimpy'));
        expect(tag.category, equals('style'));
      });

      test('should handle fullName without category', () {
        final tag = Tag(id: 1, name: 'test', isPredefined: false);

        expect(tag.fullName, equals('test'));
      });
    });

    group('Location Model', () {
      test('should create location with starred and createdAt fields', () {
        final createdAt = DateTime.now();
        final location = Location(
          id: 1,
          name: 'Test Crag',
          isIndoor: false,
          country: 'USA',
          starred: true,
          createdAt: createdAt,
        );

        expect(location.id, equals(1));
        expect(location.name, equals('Test Crag'));
        expect(location.isIndoor, isFalse);
        expect(location.starred, isTrue);
        expect(location.createdAt, equals(createdAt));
      });

      test('should default starred to false', () {
        final location = Location(id: 1, name: 'Test Gym', isIndoor: true);

        expect(location.starred, isFalse);
      });

      test('should serialize location with new fields to JSON', () {
        final createdAt = DateTime.parse('2024-01-15T10:00:00Z');
        final location = Location(
          id: 1,
          name: 'Boulder Field',
          isIndoor: false,
          starred: true,
          createdAt: createdAt,
          coordinates: Coordinates(latitude: 40.0, longitude: -105.0),
        );

        final json = location.toJson();
        expect(json['id'], equals(1));
        expect(json['name'], equals('Boulder Field'));
        expect(json['isIndoor'], isFalse);
        expect(json['starred'], isTrue);
        expect(json['createdAt'], equals('2024-01-15T10:00:00.000Z'));
        expect(json['coordinates'], isNotNull);
      });

      test('should deserialize location from JSON', () {
        final json = {
          'id': 2,
          'name': 'Indoor Gym',
          'isIndoor': true,
          'country': 'Canada',
          'starred': false,
          'createdAt': '2024-01-10T15:30:00Z',
          'city': 'Toronto',
        };

        final location = Location.fromJson(json);
        expect(location.id, equals(2));
        expect(location.name, equals('Indoor Gym'));
        expect(location.isIndoor, isTrue);
        expect(location.country, equals('Canada'));
        expect(location.starred, isFalse);
        expect(location.createdAt, isNotNull);
        expect(location.city, equals('Toronto'));
      });
    });

    group('Sector Model', () {
      test('should have isDefault and createdAt fields', () {
        final createdAt = DateTime.now();
        final sector = Sector(
          id: 1,
          locationId: 10,
          name: 'Main Wall',
          isDefault: true,
          createdAt: createdAt,
          approach: 'Follow the trail for 10 minutes',
        );

        expect(sector.id, equals(1));
        expect(sector.locationId, equals(10));
        expect(sector.name, equals('Main Wall'));
        expect(sector.isDefault, isTrue);
        expect(sector.createdAt, equals(createdAt));
        expect(sector.approach, equals('Follow the trail for 10 minutes'));
      });

      test('should default isDefault to false', () {
        final sector = Sector(id: 1, locationId: 10, name: 'Side Wall');

        expect(sector.isDefault, isFalse);
      });

      test('should serialize and deserialize sector', () {
        final sector = Sector(
          id: 5,
          locationId: 20,
          name: 'North Face',
          isDefault: true,
          description: 'The main climbing area',
          createdAt: DateTime.parse('2024-01-01T00:00:00Z'),
        );

        final json = sector.toJson();
        expect(json['id'], equals(5));
        expect(json['locationId'], equals(20));
        expect(json['name'], equals('North Face'));
        expect(json['isDefault'], isTrue);
        expect(json['description'], equals('The main climbing area'));
        expect(json['createdAt'], equals('2024-01-01T00:00:00.000Z'));

        final deserialized = Sector.fromJson(json);
        expect(deserialized.id, equals(sector.id));
        expect(deserialized.isDefault, equals(sector.isDefault));
        expect(deserialized.createdAt, isNotNull);
      });
    });

    group('MediaItem Model', () {
      test('should have int id and int climbId', () {
        final mediaItem = MediaItem(
          id: 123,
          type: MediaType.photo,
          source: MediaSource.embedded,
          path: 'media/photo.jpg',
          climbId: 456,
        );

        expect(mediaItem.id, equals(123));
        expect(mediaItem.id, isA<int>());
        expect(mediaItem.climbId, equals(456));
        expect(mediaItem.climbId, isA<int>());
      });

      test('should handle media without climbId', () {
        final mediaItem = MediaItem(
          id: 1,
          type: MediaType.video,
          source: MediaSource.external,
          path: 'https://example.com/video.mp4',
          routeId: 100,
          locationId: 50,
        );

        expect(mediaItem.climbId, isNull);
        expect(mediaItem.routeId, equals(100));
        expect(mediaItem.locationId, equals(50));
      });
    });

    group('SessionType Enum', () {
      test('should have multiPitch and boardSession values', () {
        expect(SessionType.values, contains(SessionType.multiPitch));
        expect(SessionType.values, contains(SessionType.boardSession));
        expect(SessionType.multiPitch.value, equals('multiPitch'));
        expect(SessionType.boardSession.value, equals('boardSession'));
      });

      test('should serialize correctly', () {
        final session = Session(
          id: 1,
          date: DateTime.now().toIso8601String().substring(0, 10),
          startTime: '09:00:00',
          sessionType: SessionType.multiPitch,
        );

        final json = session.toJson();
        expect(json['sessionType'], equals('multiPitch'));
      });
    });

    group('RockType Enum', () {
      test('should have new rock types', () {
        expect(RockType.values, contains(RockType.dolomite));
        expect(RockType.values, contains(RockType.slate));
        expect(RockType.values, contains(RockType.gabbro));
        expect(RockType.values, contains(RockType.volcanicTuff));
        expect(RockType.values, contains(RockType.andesite));
        expect(RockType.values, contains(RockType.chalk));
      });

      test('should have correct values', () {
        expect(RockType.dolomite.value, equals('dolomite'));
        expect(RockType.slate.value, equals('slate'));
        expect(RockType.gabbro.value, equals('gabbro'));
        expect(RockType.volcanicTuff.value, equals('volcanicTuff'));
        expect(RockType.andesite.value, equals('andesite'));
        expect(RockType.chalk.value, equals('chalk'));
      });

      test('should not have tuff (replaced by volcanicTuff)', () {
        final hasOldTuff = RockType.values.any((r) => r.value == 'tuff');
        expect(hasOldTuff, isFalse);
      });
    });

    group('Route Model', () {
      test('should have routeCharacteristics field', () {
        final route = Route(
          id: 1,
          locationId: 100,
          name: 'Test Route',
          routeType: RouteType.route,
          routeCharacteristics: RouteCharacteristics.trad,
          gearNotes: 'Standard rack with extra #2 cam',
        );

        expect(route.id, equals(1));
        expect(route.locationId, equals(100));
        expect(route.name, equals('Test Route'));
        expect(route.routeType, equals(RouteType.route));
        expect(route.routeCharacteristics, equals(RouteCharacteristics.trad));
        expect(route.gearNotes, equals('Standard rack with extra #2 cam'));
      });

      test('should serialize route with new fields to JSON', () {
        final route = Route(
          id: 2,
          locationId: 200,
          name: 'Sport Route',
          routeType: RouteType.route,
          routeCharacteristics: RouteCharacteristics.bolted,
          gearNotes: '12 quickdraws',
          height: 25.5,
          bolts: 12,
        );

        final json = route.toJson();
        expect(json['id'], equals(2));
        expect(json['locationId'], equals(200));
        expect(json['name'], equals('Sport Route'));
        expect(json['routeType'], equals('route'));
        expect(json['routeCharacteristics'], equals('bolted'));
        expect(json['gearNotes'], equals('12 quickdraws'));
        expect(json['height'], equals(25.5));
        expect(json['bolts'], equals(12));
      });

      test('should deserialize route from JSON', () {
        final json = {
          'id': 3,
          'locationId': 300,
          'name': 'Classic Crack',
          'routeType': 'route',
          'routeCharacteristics': 'trad',
          'gearNotes': 'Doubles from #0.5 to #3',
          'protection': 'adequate',
          'qualityRating': 4,
        };

        final route = Route.fromJson(json);
        expect(route.id, equals(3));
        expect(route.locationId, equals(300));
        expect(route.name, equals('Classic Crack'));
        expect(route.routeType, equals(RouteType.route));
        expect(route.routeCharacteristics, equals(RouteCharacteristics.trad));
        expect(route.gearNotes, equals('Doubles from #0.5 to #3'));
        expect(route.protection, equals(ProtectionRating.adequate));
        expect(route.qualityRating, equals(4));
      });

      test('should handle route without optional fields', () {
        final route = Route(
          id: 4,
          locationId: 400,
          name: 'Boulder Problem',
          routeType: RouteType.boulder,
        );

        expect(route.routeCharacteristics, isNull);
        expect(route.gearNotes, isNull);

        final json = route.toJson();
        expect(json['routeCharacteristics'], isNull);
        expect(json['gearNotes'], isNull);
      });
    });

    group('Session Model', () {
      test('should have isOngoing field with default false', () {
        final session = Session(id: 1, date: '2024-01-15');

        expect(session.id, equals(1));
        expect(session.date, equals('2024-01-15'));
        expect(session.isOngoing, isFalse);
      });

      test('should create session with isOngoing true', () {
        final session = Session(
          id: 2,
          date: '2024-01-20',
          startTime: '09:00:00',
          isOngoing: true,
          location: 'Local Gym',
          isIndoor: true,
        );

        expect(session.isOngoing, isTrue);
        expect(session.location, equals('Local Gym'));
        expect(session.startTime, equals('09:00:00'));
      });

      test('should serialize session with isOngoing to JSON', () {
        final session = Session(
          id: 3,
          date: '2024-01-25',
          isOngoing: true,
          sessionType: SessionType.boardSession,
        );

        final json = session.toJson();
        expect(json['id'], equals(3));
        expect(json['date'], equals('2024-01-25'));
        expect(json['isOngoing'], isTrue);
        expect(json['sessionType'], equals('boardSession'));
      });

      test('should deserialize session from JSON', () {
        final json = {
          'id': 4,
          'date': '2024-02-01',
          'isOngoing': false,
          'sessionType': 'multiPitch',
          'location': 'El Capitan',
          'isIndoor': false,
        };

        final session = Session.fromJson(json);
        expect(session.id, equals(4));
        expect(session.date, equals('2024-02-01'));
        expect(session.isOngoing, isFalse);
        expect(session.sessionType, equals(SessionType.multiPitch));
        expect(session.location, equals('El Capitan'));
        expect(session.isIndoor, isFalse);
      });

      test(
        'should handle session without isOngoing in JSON (default to false)',
        () {
          final json = {
            'id': 5,
            'date': '2024-02-05',
            'location': 'Boulder Gym',
          };

          final session = Session.fromJson(json);
          expect(session.id, equals(5));
          expect(session.date, equals('2024-02-05'));
          expect(session.isOngoing, isFalse); // Should default to false
        },
      );
    });
  });
}
