import 'package:test/test.dart';
import 'package:cldf/cldf.dart';

void main() {
  group('Route Model Tests', () {
    test('should create Route with required fields', () {
      final route = Route(
        id: 1,
        locationId: 100,
        name: 'The Nose',
        routeType: RouteType.route,
      );

      expect(route.id, equals(1));
      expect(route.locationId, equals(100));
      expect(route.name, equals('The Nose'));
      expect(route.routeType, equals(RouteType.route));
      expect(route.sectorId, isNull);
      expect(route.routeCharacteristics, isNull);
      expect(route.grades, isNull);
      expect(route.height, isNull);
      expect(route.color, isNull);
      expect(route.firstAscent, isNull);
      expect(route.protectionRating, isNull);
      expect(route.qualityRating, isNull);
      expect(route.beta, isNull);
      expect(route.gearNotes, isNull);
      expect(route.tags, isNull);
      expect(route.customFields, isNull);
    });

    test('should create Route with all fields', () {
      final firstAscent = FirstAscent(
        name: 'Lynn Hill',
        date: '1993-09-19',
        info: 'First free ascent',
      );

      final route = Route(
        id: 2,
        locationId: 101,
        sectorId: 10,
        name: 'Midnight Lightning',
        routeType: RouteType.boulder,
        routeCharacteristics: RouteCharacteristics.trad,
        grades: {'vScale': 'V8', 'font': '7B+'},
        height: 4.5,
        color: 'black',
        firstAscent: firstAscent,
        protectionRating: ProtectionRating.good,
        qualityRating: 5,
        beta: 'Start with the lightning bolt hold',
        gearNotes: 'Crash pads recommended',
        tags: ['classic', 'highball'],
        customFields: {'isIconic': true},
      );

      expect(route.sectorId, equals(10));
      expect(route.routeCharacteristics, equals(RouteCharacteristics.trad));
      expect(route.grades, isNotNull);
      expect(route.grades!['vScale'], equals('V8'));
      expect(route.height, equals(4.5));
      expect(route.color, equals('black'));
      expect(route.firstAscent, isNotNull);
      expect(route.protectionRating, equals(ProtectionRating.good));
      expect(route.qualityRating, equals(5));
      expect(route.beta, equals('Start with the lightning bolt hold'));
      expect(route.gearNotes, equals('Crash pads recommended'));
      expect(route.tags, equals(['classic', 'highball']));
      expect(route.customFields!['isIconic'], isTrue);
    });

    test('should serialize and deserialize Route', () {
      final route = Route(
        id: 3,
        locationId: 102,
        sectorId: 20,
        name: 'Test Route',
        routeType: RouteType.route,
        routeCharacteristics: RouteCharacteristics.bolted,
        grades: {'yds': '5.10a', 'french': '6a'},
        height: 25.0,
        beta: 'Crux at the third bolt',
        tags: ['overhang', 'technical'],
      );

      final json = route.toJson();
      final deserialized = Route.fromJson(json);

      expect(deserialized.id, equals(route.id));
      expect(deserialized.locationId, equals(route.locationId));
      expect(deserialized.sectorId, equals(route.sectorId));
      expect(deserialized.name, equals(route.name));
      expect(deserialized.routeType, equals(route.routeType));
      expect(
        deserialized.routeCharacteristics,
        equals(route.routeCharacteristics),
      );
      expect(deserialized.grades, equals(route.grades));
      expect(deserialized.height, equals(route.height));
      expect(deserialized.beta, equals(route.beta));
      expect(deserialized.tags, equals(route.tags));
    });
  });

  group('FirstAscent Model Tests', () {
    test('should create FirstAscent with all fields', () {
      final fa = FirstAscent(
        name: 'Alex Honnold',
        date: '2017-06-03',
        info: 'Free solo ascent',
      );

      expect(fa.name, equals('Alex Honnold'));
      expect(fa.date, equals('2017-06-03'));
      expect(fa.info, equals('Free solo ascent'));
    });

    test('should create FirstAscent with optional fields', () {
      final fa = FirstAscent(name: 'Unknown');

      expect(fa.name, equals('Unknown'));
      expect(fa.date, isNull);
      expect(fa.info, isNull);
    });

    test('should serialize and deserialize FirstAscent', () {
      final fa = FirstAscent(
        name: 'Tommy Caldwell',
        date: '2015-01-14',
        info: 'Dawn Wall first free ascent',
      );

      final json = fa.toJson();
      final deserialized = FirstAscent.fromJson(json);

      expect(deserialized.name, equals(fa.name));
      expect(deserialized.date, equals(fa.date));
      expect(deserialized.info, equals(fa.info));
    });
  });
}
