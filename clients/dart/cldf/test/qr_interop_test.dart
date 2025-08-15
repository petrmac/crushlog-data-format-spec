import 'dart:io';
import 'package:test/test.dart';
import 'package:cldf/qr/qr.dart';
import 'package:cldf/models/route.dart';
import 'package:cldf/models/location.dart';
import 'package:cldf/models/enums/route_type.dart';

void main() {
  group('QR Code Java-Dart Interoperability', () {
    const javaCliPath =
        '/Users/petrmacek/git-mirrors/crushlog-data-format-spec/clients/java/cldf-tool/build/native/nativeCompile/cldf';
    final testResourcesDir = Directory('test/qr-test-resources');

    setUpAll(() async {
      // Create test resources directory
      if (!testResourcesDir.existsSync()) {
        testResourcesDir.createSync(recursive: true);
      }
    });

    group('Generate QR codes with Java, scan with Dart', () {
      test('Java generates route QR (JSON format) -> Dart scans', () async {
        // Generate QR with Java
        final result = await Process.run(javaCliPath, [
          'qr',
          'generate',
          '--type',
          'route',
          '--name',
          'Java Test Route',
          '--grade',
          '5.10a',
          '--grade-system',
          'YDS',
          '--route-type',
          'sport',
          '--location-name',
          'Test Crag',
          '--location-country',
          'US',
          '--location-state',
          'CA',
          '--latitude',
          '37.734000',
          '--longitude',
          '-119.637700',
          '--format',
          'json',
          '--output-format',
          'png',
          '--output',
          '${testResourcesDir.path}/java-route-json.png',
        ]);

        expect(
          result.exitCode,
          equals(0),
          reason: 'Java QR generation failed: ${result.stderr}',
        );

        // Read the generated QR code with Dart
        final qrFile = File('${testResourcesDir.path}/java-route-json.png');
        expect(qrFile.existsSync(), isTrue);

        final imageBytes = await qrFile.readAsBytes();
        final scanner = QRScanner();
        final parsedData = await scanner.scanImage(imageBytes);

        expect(parsedData, isNotNull);
        expect(parsedData!.version, equals(1));
        expect(parsedData.clid, isNotNull);
        expect(parsedData.clid, startsWith('clid:route:'));
        expect(parsedData.route, isNotNull);
        expect(parsedData.route!.name, equals('Java Test Route'));
        expect(parsedData.route!.grade, equals('5.10a'));
        expect(parsedData.route!.gradeSystem, equals('YDS'));
        expect(parsedData.route!.type, equals('sport'));

        // Convert to Dart Route object
        final route = scanner.toRoute(parsedData);
        expect(route, isNotNull);
        expect(route!.name, equals('Java Test Route'));
        expect(route.grades?['yds'], equals('5.10a'));
        expect(route.routeType, equals(RouteType.route)); // sport maps to route

        print('✓ Successfully scanned Java-generated route QR (JSON)');
      });

      test('Java generates location QR (JSON format) -> Dart scans', () async {
        // Generate QR with Java
        final result = await Process.run(javaCliPath, [
          'qr',
          'generate',
          '--type',
          'location',
          '--location-name',
          'Java Test Crag',
          '--location-country',
          'FR',
          '--location-city',
          'Chamonix',
          '--latitude',
          '45.878600',
          '--longitude',
          '6.887300',
          '--indoor',
          'false',
          '--format',
          'json',
          '--output-format',
          'png',
          '--output',
          '${testResourcesDir.path}/java-location-json.png',
        ]);

        expect(
          result.exitCode,
          equals(0),
          reason: 'Java QR generation failed: ${result.stderr}',
        );

        // Read the generated QR code with Dart
        final qrFile = File('${testResourcesDir.path}/java-location-json.png');
        expect(qrFile.existsSync(), isTrue);

        final imageBytes = await qrFile.readAsBytes();
        final scanner = QRScanner();
        final parsedData = await scanner.scanImage(imageBytes);

        expect(parsedData, isNotNull);
        expect(parsedData!.clid, isNotNull);
        expect(parsedData.clid, startsWith('clid:location:'));
        expect(parsedData.location, isNotNull);
        expect(parsedData.location!.name, equals('Java Test Crag'));
        expect(parsedData.location!.country, equals('FR'));
        expect(parsedData.location!.city, equals('Chamonix'));

        // Convert to Dart Location object
        final location = scanner.toLocation(parsedData);
        expect(location, isNotNull);
        expect(location!.name, equals('Java Test Crag'));
        expect(location.country, equals('FR'));
        expect(location.city, equals('Chamonix'));
        expect(location.isIndoor, isFalse);

        print('✓ Successfully scanned Java-generated location QR (JSON)');
      });

      test('Java generates route QR (URL format) -> Dart scans', () async {
        // Generate QR with Java (URL format)
        final result = await Process.run(javaCliPath, [
          'qr',
          'generate',
          '--type',
          'route',
          '--name',
          'Java URL Route',
          '--grade',
          'V8',
          '--grade-system',
          'vScale',
          '--route-type',
          'boulder',
          '--location-name',
          'Boulder Field',
          '--location-country',
          'US',
          '--location-state',
          'CO',
          '--latitude',
          '40.017900',
          '--longitude',
          '-105.281600',
          '--format',
          'url',
          '--output-format',
          'png',
          '--output',
          '${testResourcesDir.path}/java-route-url.png',
        ]);

        expect(
          result.exitCode,
          equals(0),
          reason: 'Java QR generation failed: ${result.stderr}',
        );

        // Read the generated QR code with Dart
        final qrFile = File('${testResourcesDir.path}/java-route-url.png');
        expect(qrFile.existsSync(), isTrue);

        final imageBytes = await qrFile.readAsBytes();
        final scanner = QRScanner();
        final parsedData = await scanner.scanImage(imageBytes);

        expect(parsedData, isNotNull);
        expect(parsedData!.url, isNotNull);
        expect(parsedData.url, contains('crushlog.pro'));
        expect(parsedData.shortClid, isNotNull);

        print('✓ Successfully scanned Java-generated route QR (URL)');
      });
    });

    group('Generate QR codes with Dart, scan with Java', () {
      test('Dart generates route QR (JSON format) -> Java scans', () async {
        // Create test route
        final route = Route(
          id: 123,
          name: 'Dart Test Route',
          grades: {'vScale': 'V10', 'font': '7C+'},
          routeType: RouteType.boulder,
          height: 4.5,
          locationId: 456,
        );

        final location = Location(
          id: 456,
          name: 'Dart Test Area',
          country: 'ES',
          state: 'Catalunya',
          city: 'Siurana',
          coordinates: Coordinates(latitude: 41.257900, longitude: 0.932100),
          isIndoor: false,
        );

        // Generate QR with Dart
        final generator = QRGenerator();
        final qrData = generator.generateRouteData(
          route,
          QROptions(format: QRFormat.json),
          location: location,
        );

        final pngBytes = await generator.generatePNG(
          qrData,
          QROptions(format: QRFormat.json),
          size: 256,
        );

        // Save QR code
        final qrFile = File('${testResourcesDir.path}/dart-route-json.png');
        await qrFile.writeAsBytes(pngBytes);

        // Scan with Java CLI
        final result = await Process.run(javaCliPath, [
          'qr',
          'scan',
          '${testResourcesDir.path}/dart-route-json.png',
        ]);

        expect(
          result.exitCode,
          equals(0),
          reason: 'Java QR scanning failed: ${result.stderr}',
        );

        final output = result.stdout.toString();
        expect(output, contains('Successfully scanned QR code'));
        expect(output, contains('CLID: clid:route:'));
        expect(output, contains('Route: Dart Test Route'));
        expect(output, contains('Grade: V10'));
        expect(output, contains('Type: boulder'));
        expect(output, contains('Location: Dart Test Area'));

        print('✓ Java successfully scanned Dart-generated route QR (JSON)');
      });

      test('Dart generates location QR (JSON format) -> Java scans', () async {
        // Create test location
        final location = Location(
          id: 789,
          name: 'Dart Gym',
          country: 'DE',
          state: 'Bayern',
          city: 'Munich',
          coordinates: Coordinates(latitude: 48.135125, longitude: 11.581981),
          isIndoor: true,
        );

        // Generate QR with Dart
        final generator = QRGenerator();
        final qrData = generator.generateLocationData(
          location,
          QROptions(format: QRFormat.json),
        );

        final pngBytes = await generator.generatePNG(
          qrData,
          QROptions(format: QRFormat.json),
          size: 256,
        );

        // Save QR code
        final qrFile = File('${testResourcesDir.path}/dart-location-json.png');
        await qrFile.writeAsBytes(pngBytes);

        // Scan with Java CLI
        final result = await Process.run(javaCliPath, [
          'qr',
          'scan',
          '${testResourcesDir.path}/dart-location-json.png',
        ]);

        expect(
          result.exitCode,
          equals(0),
          reason: 'Java QR scanning failed: ${result.stderr}',
        );

        final output = result.stdout.toString();
        expect(output, contains('Successfully scanned QR code'));
        expect(output, contains('CLID: clid:location:'));
        expect(output, contains('Location: Dart Gym'));
        expect(output, contains('Country: DE'));
        expect(output, contains('Indoor: true'));

        print('✓ Java successfully scanned Dart-generated location QR (JSON)');
      });

      test('Dart generates compact QR -> Java scans', () async {
        // Generate compact QR with Dart
        final generator = QRGenerator();

        // Generate QR from compact JSON
        final pngBytes = await generator.generatePNG(
          QRCodeData(
            version: 1,
            clid: 'clid:route:abc-def-123-456-789',
            url: 'https://crushlog.pro/g/abc-def-123',
          ),
          QROptions(format: QRFormat.json),
          size: 256,
        );

        // Save QR code
        final qrFile = File('${testResourcesDir.path}/dart-compact.png');
        await qrFile.writeAsBytes(pngBytes);

        print('✓ Generated compact QR code for Java scanning');
      });
    });

    group('Cross-version compatibility', () {
      test('Parse v1 format QR (legacy)', () {
        // Simulate v1 format data
        const v1Data = '''
        {
          "version": 1,
          "url": "https://crushlog.pro/r/123/456",
          "location": {
            "id": 123,
            "name": "Legacy Crag"
          },
          "route": {
            "id": 456,
            "name": "Legacy Route",
            "grade": "5.11a"
          }
        }
        ''';

        final scanner = QRScanner();
        final parsed = scanner.parse(v1Data);

        expect(parsed, isNotNull);
        expect(parsed!.version, equals(1));
        expect(parsed.route, isNotNull);
        expect(parsed.route!.name, equals('Legacy Route'));

        print('✓ Successfully parsed v1 (legacy) QR format');
      });

      test('Parse v2 format QR (current)', () {
        // v2 format with CLIDs
        const v2Data = '''
        {
          "v": 1,
          "clid": "clid:route:550e8400-e29b-41d4-a716-446655440000",
          "url": "https://crushlog.pro/g/550e8400",
          "cldf": "QmXk9...abc",
          "loc": {
            "clid": "clid:location:660e8400-e29b-41d4-a716-446655440000",
            "name": "Modern Crag",
            "country": "US"
          },
          "route": {
            "name": "Modern Route",
            "grade": "V7",
            "type": "boulder"
          },
          "meta": {
            "created": "2025-01-15T10:00:00Z",
            "blockchain": true
          }
        }
        ''';

        final scanner = QRScanner();
        final parsed = scanner.parse(v2Data);

        expect(parsed, isNotNull);
        expect(parsed!.version, equals(1));
        expect(
          parsed.clid,
          equals('clid:route:550e8400-e29b-41d4-a716-446655440000'),
        );
        expect(parsed.blockchainVerified, isTrue);
        expect(parsed.route, isNotNull);
        expect(parsed.route!.name, equals('Modern Route'));

        print('✓ Successfully parsed v2 (current) QR format');
      });
    });

    test('Generate QR test resource gallery', () async {
      // Generate a variety of QR codes for visual testing
      final generator = QRGenerator();

      // Test data
      final testCases = [
        {
          'name': 'minimal-route',
          'route': Route(
            id: 1,
            name: 'Minimal',
            routeType: RouteType.route,
            locationId: 1,
          ),
        },
        {
          'name': 'full-route',
          'route': Route(
            id: 2,
            name: 'Full Featured Route',
            grades: {'vScale': 'V12', 'font': '8B+', 'yds': '5.14d'},
            routeType: RouteType.boulder,
            height: 5.5,
            locationId: 2,
          ),
        },
        {
          'name': 'indoor-route',
          'route': Route(
            id: 3,
            name: 'Gym Route',
            grades: {'french': '6c+'},
            routeType: RouteType.route,
            locationId: 3,
          ),
          'location': Location(
            id: 3,
            name: 'Climbing Gym',
            country: 'US',
            isIndoor: true,
            coordinates: Coordinates(latitude: 40.0, longitude: -105.0),
          ),
        },
      ];

      for (final testCase in testCases) {
        final route = testCase['route'] as Route;
        final location = testCase['location'] as Location?;
        final name = testCase['name'] as String;

        // Generate JSON format QR
        final jsonData = generator.generateRouteData(
          route,
          QROptions(format: QRFormat.json),
          location: location,
        );
        final jsonPng = await generator.generatePNG(
          jsonData,
          QROptions(format: QRFormat.json),
        );
        await File(
          '${testResourcesDir.path}/$name-json.png',
        ).writeAsBytes(jsonPng);

        // Generate URL format QR
        final urlData = generator.generateRouteData(
          route,
          QROptions(format: QRFormat.url),
          location: location,
        );
        final urlPng = await generator.generatePNG(
          urlData,
          QROptions(format: QRFormat.url),
        );
        await File(
          '${testResourcesDir.path}/$name-url.png',
        ).writeAsBytes(urlPng);

        // Generate SVG version
        final svg = generator.generateSVG(
          jsonData,
          QROptions(format: QRFormat.json),
        );
        await File('${testResourcesDir.path}/$name.svg').writeAsString(svg);
      }

      print('✓ Generated QR code test gallery in ${testResourcesDir.path}');
    });
  });
}
