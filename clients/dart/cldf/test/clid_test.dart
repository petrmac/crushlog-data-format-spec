import 'package:test/test.dart';
import 'package:cldf/clid/clid_generator.dart';
import 'package:cldf/clid/models/location.dart';
import 'package:cldf/clid/models/route.dart';
import 'package:cldf/clid/models/sector.dart';
import 'package:cldf/clid/models/coordinates.dart';

void main() {
  group('CLID (CrushLog ID) Generation', () {
    test('Generate deterministic location CLID for El Capitan', () {
      final elCapitan = Location.outdoor(
        country: 'US',
        state: 'CA',
        city: 'Yosemite Valley',
        name: 'El Capitan',
        lat: 37.734000,
        lon: -119.637700,
      );

      final clid = CLIDGenerator.generateLocationCLID(elCapitan);

      // Should always generate the same CLID for the same location
      final clid2 = CLIDGenerator.generateLocationCLID(elCapitan);

      expect(clid, isNotNull);
      expect(clid, startsWith('clid:location:'));
      expect(
        clid,
        equals(clid2),
        reason: 'Same location should generate same CLID',
      );

      // Validate the generated CLID
      expect(CLIDGenerator.validate(clid), isTrue);

      // Parse and verify
      final parsed = CLIDGenerator.parse(clid);
      expect(parsed.type, equals(EntityType.location));
      expect(parsed.namespace, equals('clid'));
      expect(parsed.url, isNotNull);
    });

    test('Generate deterministic route CLID for The Nose', () {
      // First generate location ID
      final elCapitan = Location.outdoor(
        country: 'US',
        state: 'CA',
        city: 'Yosemite Valley',
        name: 'El Capitan',
        lat: 37.734000,
        lon: -119.637700,
      );

      final locationCLID = CLIDGenerator.generateLocationCLID(elCapitan);

      // Create The Nose route
      final theNose = Route(
        name: 'The Nose',
        grade: '5.14a',
        type: RouteType.trad,
        firstAscent: FirstAscent(year: 1958, name: 'Warren Harding'),
        height: 900.0,
      );

      final routeCLID = CLIDGenerator.generateRouteCLID(locationCLID, theNose);

      // Should always generate the same CLID for the same route
      final routeCLID2 = CLIDGenerator.generateRouteCLID(locationCLID, theNose);

      expect(routeCLID, isNotNull);
      expect(routeCLID, startsWith('clid:route:'));
      expect(
        routeCLID,
        equals(routeCLID2),
        reason: 'Same route should generate same CLID',
      );

      // Validate the generated CLID
      expect(CLIDGenerator.validate(routeCLID), isTrue);

      // Parse and verify
      final parsed = CLIDGenerator.parse(routeCLID);
      expect(parsed.type, equals(EntityType.route));
    });

    test('Generate CLID for indoor gym', () {
      final gym = Location.indoor(
        country: 'US',
        state: 'CO',
        city: 'Boulder',
        name: 'The Spot',
        lat: 40.017900,
        lon: -105.281600,
      );

      final gymCLID = CLIDGenerator.generateLocationCLID(gym);

      expect(gymCLID, isNotNull);
      expect(gymCLID, startsWith('clid:location:'));
      expect(CLIDGenerator.validate(gymCLID), isTrue);
    });

    test('Generate sector CLID', () {
      final location = Location.outdoor(
        country: 'US',
        state: 'CA',
        name: 'El Capitan',
        lat: 37.734000,
        lon: -119.637700,
      );

      final locationCLID = CLIDGenerator.generateLocationCLID(location);

      final dawnWall = Sector(name: 'Dawn Wall', order: 1);
      final sectorCLID = CLIDGenerator.generateSectorCLID(
        locationCLID,
        dawnWall,
      );

      expect(sectorCLID, isNotNull);
      expect(sectorCLID, startsWith('clid:sector:'));
      expect(CLIDGenerator.validate(sectorCLID), isTrue);
    });

    test('Generate random CLID for climb', () {
      final climbCLID1 = CLIDGenerator.generateRandomCLID(EntityType.climb);
      final climbCLID2 = CLIDGenerator.generateRandomCLID(EntityType.climb);

      expect(climbCLID1, isNotNull);
      expect(climbCLID2, isNotNull);
      expect(climbCLID1, startsWith('clid:climb:'));
      expect(climbCLID2, startsWith('clid:climb:'));
      expect(
        climbCLID1,
        isNot(equals(climbCLID2)),
        reason: 'Random CLIDs should be different',
      );

      expect(CLIDGenerator.validate(climbCLID1), isTrue);
      expect(CLIDGenerator.validate(climbCLID2), isTrue);
    });

    test('Parse and validate CLID', () {
      const testCLID = 'clid:route:550e8400-e29b-41d4-a716-446655440000';

      final parsed = CLIDGenerator.parse(testCLID);

      expect(parsed.namespace, equals('clid'));
      expect(parsed.type, equals(EntityType.route));
      expect(parsed.uuid, equals('550e8400-e29b-41d4-a716-446655440000'));
      expect(parsed.fullId, equals(testCLID));
      expect(parsed.shortForm, equals('550e8400'));
      expect(parsed.url, equals('https://crushlog.pro/g/550e8400'));
    });

    test('Validate invalid CLID format', () {
      expect(CLIDGenerator.validate('invalid-id'), isFalse);
      expect(CLIDGenerator.validate('route:123'), isFalse);
      expect(CLIDGenerator.validate('clid:invalid:123'), isFalse);

      expect(() => CLIDGenerator.parse('invalid-id'), throwsArgumentError);
    });

    test('Validation errors for invalid location', () {
      final invalidLocation = Location(
        country: 'USA', // Should be 2-letter code
        name: '', // Empty name
        coordinates: Coordinates(lat: 91.0, lon: 181.0), // Invalid
        isIndoor: false,
      );

      expect(
        () => CLIDGenerator.generateLocationCLID(invalidLocation),
        throwsArgumentError,
      );
    });

    test('Short form generation', () {
      final location = Location.outdoor(
        country: 'FR',
        name: 'Ceuse',
        lat: 44.506000,
        lon: 5.940000,
      );

      final clid = CLIDGenerator.generateLocationCLID(location);
      final shortForm = CLIDGenerator.toShortForm(clid);

      expect(shortForm, isNotNull);
      expect(shortForm.length, equals(8));
    });

    test('Boulder problem without height', () {
      final location = Location.outdoor(
        country: 'FR',
        name: 'Fontainebleau',
        lat: 48.404000,
        lon: 2.692000,
      );

      final locationCLID = CLIDGenerator.generateLocationCLID(location);

      final boulder = Route(
        name: 'Rainbow Rocket',
        grade: '8A',
        type: RouteType.boulder,
        // No height needed for boulders
      );

      final routeCLID = CLIDGenerator.generateRouteCLID(locationCLID, boulder);

      expect(routeCLID, isNotNull);
      expect(CLIDGenerator.validate(routeCLID), isTrue);
    });

    test('Different locations generate different CLIDs', () {
      final location1 = Location.outdoor(
        country: 'US',
        name: 'Yosemite',
        lat: 37.734000,
        lon: -119.637700,
      );

      final location2 = Location.outdoor(
        country: 'US',
        name: 'Joshua Tree',
        lat: 33.873415,
        lon: -115.900992,
      );

      final id1 = CLIDGenerator.generateLocationCLID(location1);
      final id2 = CLIDGenerator.generateLocationCLID(location2);

      expect(
        id1,
        isNot(equals(id2)),
        reason: 'Different locations should have different IDs',
      );
    });

    test('JSON serialization of models', () {
      final location = Location.outdoor(
        country: 'US',
        state: 'CA',
        name: 'El Capitan',
        lat: 37.734000,
        lon: -119.637700,
      );

      final json = location.toJson();
      final restored = Location.fromJson(json);

      expect(restored.country, equals(location.country));
      expect(restored.name, equals(location.name));
      expect(restored.coordinates.lat, equals(location.coordinates.lat));
      expect(restored.coordinates.lon, equals(location.coordinates.lon));
      expect(restored.isIndoor, equals(location.isIndoor));
    });
  });
}
