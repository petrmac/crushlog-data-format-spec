import 'package:test/test.dart';
import 'package:cldf/cldf.dart';

void main() {
  group('Session Model Tests', () {
    test('should create Session with required fields', () {
      final session = Session(id: 1, date: '2024-01-15');

      expect(session.id, equals(1));
      expect(session.date, equals('2024-01-15'));
      expect(session.isOngoing, isFalse); // Default value
      expect(session.startTime, isNull);
      expect(session.endTime, isNull);
      expect(session.location, isNull);
      expect(session.locationId, isNull);
      expect(session.isIndoor, isNull);
      expect(session.climbType, isNull);
      expect(session.sessionType, isNull);
      expect(session.partners, isNull);
      expect(session.weather, isNull);
      expect(session.notes, isNull);
      expect(session.rockType, isNull);
      expect(session.terrainType, isNull);
      expect(session.approachTime, isNull);
      expect(session.departureTime, isNull);
      expect(session.customFields, isNull);
    });

    test('should create Session with all fields', () {
      final weather = Weather(
        conditions: 'Sunny',
        temperature: 22.5,
        humidity: 65.0,
        wind: 'Light breeze',
      );

      final session = Session(
        id: 2,
        date: '2024-01-20',
        startTime: '09:00:00',
        endTime: '16:30:00',
        location: 'Yosemite',
        locationId: 100,
        isIndoor: false,
        climbType: ClimbType.boulder,
        sessionType: SessionType.bouldering,
        partners: ['Alex', 'Sam'],
        weather: weather,
        notes: 'Great conditions today',
        rockType: RockType.granite,
        terrainType: TerrainType.natural,
        approachTime: 30,
        departureTime: 25,
        isOngoing: true,
        customFields: {'elevation': 1200},
      );

      expect(session.startTime, equals('09:00:00'));
      expect(session.endTime, equals('16:30:00'));
      expect(session.location, equals('Yosemite'));
      expect(session.locationId, equals(100));
      expect(session.isIndoor, isFalse);
      expect(session.climbType, equals(ClimbType.boulder));
      expect(session.sessionType, equals(SessionType.bouldering));
      expect(session.partners, equals(['Alex', 'Sam']));
      expect(session.weather, isNotNull);
      expect(session.notes, equals('Great conditions today'));
      expect(session.rockType, equals(RockType.granite));
      expect(session.terrainType, equals(TerrainType.natural));
      expect(session.approachTime, equals(30));
      expect(session.departureTime, equals(25));
      expect(session.isOngoing, isTrue);
      expect(session.tags, equals(['outdoor', 'sunny']));
      expect(session.customFields!['elevation'], equals(1200));
    });

    test('should serialize and deserialize Session', () {
      final session = Session(
        id: 3,
        date: '2024-02-01',
        startTime: '10:00:00',
        endTime: '14:00:00',
        location: 'Local Gym',
        isIndoor: true,
        sessionType: SessionType.indoorClimbing,
        partners: ['John', 'Jane'],
        notes: 'Competition day',
        isOngoing: false,
      );

      final json = session.toJson();
      final deserialized = Session.fromJson(json);

      expect(deserialized.id, equals(session.id));
      expect(deserialized.date, equals(session.date));
      expect(deserialized.startTime, equals(session.startTime));
      expect(deserialized.endTime, equals(session.endTime));
      expect(deserialized.location, equals(session.location));
      expect(deserialized.isIndoor, equals(session.isIndoor));
      expect(deserialized.sessionType, equals(session.sessionType));
      expect(deserialized.partners, equals(session.partners));
      expect(deserialized.notes, equals(session.notes));
      expect(deserialized.isOngoing, equals(session.isOngoing));
    });

    test('should handle date conversion properly', () {
      final json = {'id': 4, 'date': '2024-03-15', 'isOngoing': false};

      final session = Session.fromJson(json);
      expect(session.date, equals('2024-03-15'));

      final serialized = session.toJson();
      expect(serialized['date'], equals('2024-03-15'));
    });

    test('should handle null date conversion', () {
      final json = {'id': 5, 'date': null, 'isOngoing': false};

      final session = Session.fromJson(json);
      expect(session.date, equals('')); // Default empty string for null dates
    });
  });

  group('Weather Model Tests', () {
    test('should create Weather with all fields', () {
      final weather = Weather(
        conditions: 'Partly cloudy',
        temperature: 18.5,
        humidity: 70.0,
        wind: 'Strong gusts',
      );

      expect(weather.conditions, equals('Partly cloudy'));
      expect(weather.temperature, equals(18.5));
      expect(weather.humidity, equals(70.0));
      expect(weather.wind, equals('Strong gusts'));
    });

    test('should create Weather with optional fields', () {
      final weather = Weather(conditions: 'Clear');

      expect(weather.conditions, equals('Clear'));
      expect(weather.temperature, isNull);
      expect(weather.humidity, isNull);
      expect(weather.wind, isNull);
    });

    test('should serialize and deserialize Weather', () {
      final weather = Weather(
        conditions: 'Rainy',
        temperature: 15.0,
        humidity: 85.0,
        wind: 'Calm',
      );

      final json = weather.toJson();
      final deserialized = Weather.fromJson(json);

      expect(deserialized.conditions, equals(weather.conditions));
      expect(deserialized.temperature, equals(weather.temperature));
      expect(deserialized.humidity, equals(weather.humidity));
      expect(deserialized.wind, equals(weather.wind));
    });
  });

  group('SessionType Enum Tests', () {
    test('should have expected session types', () {
      expect(SessionType.values, contains(SessionType.sportClimbing));
      expect(SessionType.values, contains(SessionType.multiPitch));
      expect(SessionType.values, contains(SessionType.tradClimbing));
      expect(SessionType.values, contains(SessionType.bouldering));
      expect(SessionType.values, contains(SessionType.indoorClimbing));
      expect(SessionType.values, contains(SessionType.indoorBouldering));
      expect(SessionType.values, contains(SessionType.boardSession));
    });
  });
}
