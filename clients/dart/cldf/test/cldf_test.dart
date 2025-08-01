import 'package:cldf/cldf.dart';
import 'package:test/test.dart';

void main() {
  group('CLDF Models', () {
    test('Manifest can be created and serialized', () {
      final manifest = Manifest(
        version: '1.0.0',
        creationDate: DateTime(2024, 1, 15),
        platform: Platform.mobile,
        appVersion: '1.0.0',
      );

      final json = manifest.toJson();
      expect(json['version'], '1.0.0');
      expect(json['format'], 'CLDF');
      expect(json['platform'], 'Mobile');
    });

    test('Location can be created and serialized', () {
      final location = Location(
        id: 1,
        name: 'Test Crag',
        country: 'USA',
        isIndoor: false,
        coordinates: Coordinates(
          latitude: 40.0,
          longitude: -105.0,
        ),
      );

      final json = location.toJson();
      expect(json['id'], 1);
      expect(json['name'], 'Test Crag');
      expect(json['isIndoor'], false);
      expect(json['coordinates']['latitude'], 40.0);
    });

    test('Climb can be created and serialized', () {
      final climb = Climb(
        id: 1,
        date: '2024-01-15',
        routeName: 'Test Route',
        type: ClimbType.route,
        finishType: FinishType.redpoint,
        attempts: 2,
      );

      final json = climb.toJson();
      expect(json['id'], 1);
      expect(json['routeName'], 'Test Route');
      expect(json['type'], 'route');
      expect(json['finishType'], 'redpoint');
      expect(json['attempts'], 2);
    });
  });

  group('CLDF Archive', () {
    test('Archive can be created with minimal data', () {
      final archive = CLDFArchive(
        manifest: Manifest(
          version: '1.0.0',
          creationDate: DateTime.now(),
          platform: Platform.mobile,
          appVersion: '1.0.0',
        ),
        locations: [
          Location(
            id: 1,
            name: 'Test Location',
            country: 'USA',
            isIndoor: true,
          ),
        ],
      );

      expect(archive.locations.length, 1);
      expect(archive.hasRoutes, false);
      expect(archive.hasClimbs, false);
    });
  });
}
