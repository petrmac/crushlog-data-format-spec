import 'dart:io';
import 'dart:convert';
import 'package:test/test.dart';
import 'package:cldf/qr/qr.dart';
import 'package:cldf/models/route.dart';
import 'package:cldf/models/location.dart';
import 'package:cldf/models/enums/route_type.dart';

void main() {
  group('QR Code Java-Dart Interoperability', () {
    // Use the wrapper script which automatically falls back to JAR if native is not available
    // Try to find the Java CLI tool - check multiple possible locations
    String findJavaCli() {
      // For CI environment - use relative path from dart test directory
      final relativePath = '../../../java/cldf-tool/cldf';
      final absolutePath = '/Users/petrmacek/git-mirrors/crushlog-data-format-spec/clients/java/cldf-tool/cldf';
      
      // Check relative path first (for CI)
      if (File(relativePath).existsSync()) {
        print('Using Java CLI at relative path: $relativePath');
        return relativePath;
      }
      // Fall back to absolute path (for local development)
      if (File(absolutePath).existsSync()) {
        print('Using Java CLI at absolute path: $absolutePath');
        return absolutePath;
      }
      
      // If neither exists, return the relative path and let the test fail with a clear message
      print('Expected CLI path: $relativePath');
      return relativePath;
    }
    
    final javaCliPath = findJavaCli();
    final testResourcesDir = Directory('test/qr-test-resources');
    
    // Helper to check if we should skip interop tests
    bool shouldSkipInterop() {
      if (!File(javaCliPath).existsSync()) {
        final skipInterop = Platform.environment['SKIP_INTEROP_TESTS_IF_NO_CLI'] == 'true';
        if (skipInterop) {
          print('Skipping: Java CLI not available');
          return true;
        }
      }
      return false;
    }

    setUpAll(() async {
      // Create test resources directory
      if (!testResourcesDir.existsSync()) {
        testResourcesDir.createSync(recursive: true);
      }
      
      // Check if Java CLI is available
      if (!File(javaCliPath).existsSync()) {
        final skipInterop = Platform.environment['SKIP_INTEROP_TESTS_IF_NO_CLI'] == 'true';
        if (skipInterop) {
          print('Java CLI not found at $javaCliPath - skipping interop tests');
          return;
        }
        throw Exception('Java CLI not found at $javaCliPath\n'
            'Please build the Java project first: cd ../../../java && ./gradlew :cldf-tool:fatJar\n'
            'Or set SKIP_INTEROP_TESTS_IF_NO_CLI=true to skip these tests');
      }
    });

    group('Generate QR codes with Java, scan with Dart', () {
      test('Java generates route QR (JSON format) -> Dart scans', () async {
        if (shouldSkipInterop()) return;
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
          '--route-type',
          'sport',
          '--location-name',
          'Test Crag',
          '--country',
          'US',
          '--state',
          'CA',
          '--latitude',
          '37.734000',
          '--longitude',
          '-119.637700',
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
        expect(parsedData.clid, startsWith('clid:v1:route:'));
        expect(parsedData.route, isNotNull);
        expect(parsedData.route!.name, equals('Java Test Route'));
        expect(parsedData.route!.grade, equals('5.10a'));
        expect(parsedData.route!.gradeSystem, equals('yds'));
        expect(parsedData.route!.type, equals('route'));

        // Convert to Dart Route object
        final route = scanner.toRoute(parsedData);
        expect(route, isNotNull);
        expect(route!.name, equals('Java Test Route'));
        expect(route.grades?['yds'], equals('5.10a'));
        expect(route.routeType, equals(RouteType.route)); // sport maps to route

        print('✓ Successfully scanned Java-generated route QR (JSON)');
      });

      test('Java generates location QR (JSON format) -> Dart scans', () async {
        if (shouldSkipInterop()) return;
        // Generate QR with Java
        final result = await Process.run(javaCliPath, [
          'qr',
          'generate',
          '--type',
          'location',
          '--name',
          'Java Test Crag',
          '--country',
          'FR',
          '--city',
          'Chamonix',
          '--latitude',
          '45.878600',
          '--longitude',
          '6.887300',
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
        expect(parsedData.clid, startsWith('clid:v1:location:'));
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
        if (shouldSkipInterop()) return;
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
          '--route-type',
          'boulder',
          '--location-name',
          'Boulder Field',
          '--country',
          'US',
          '--state',
          'CO',
          '--latitude',
          '40.017900',
          '--longitude',
          '-105.281600',
          '--format',
          'url',
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
        if (shouldSkipInterop()) return;
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
        final jsonData = json.decode(output);
        expect(jsonData['clid'], startsWith('clid:v1:route:'));
        expect(jsonData['route'], isNotNull);
        expect(jsonData['route']['name'], equals('Dart Test Route'));
        expect(jsonData['route']['grade'], equals('V10'));
        expect(jsonData['route']['type'], equals('boulder'));
        // Location is embedded in route for this test

        print('✓ Java successfully scanned Dart-generated route QR (JSON)');
      });

      test('Dart generates location QR (JSON format) -> Java scans', () async {
        if (shouldSkipInterop()) return;
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
        final jsonData = json.decode(output);
        expect(jsonData['clid'], startsWith('clid:v1:location:'));
        expect(jsonData['location'], isNotNull);
        expect(jsonData['location']['name'], equals('Dart Gym'));
        expect(jsonData['location']['country'], equals('DE'));
        expect(jsonData['location']['indoor'], isTrue);

        print('✓ Java successfully scanned Dart-generated location QR (JSON)');
      });

      test('Dart generates compact QR -> Java scans', () async {
        if (shouldSkipInterop()) return;
        // Generate compact QR with Dart
        final generator = QRGenerator();

        // Generate QR from compact JSON
        final pngBytes = await generator.generatePNG(
          QRCodeData(
            version: 1,
            clid: 'clid:v1:route:abc-def-123-456-789',
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
