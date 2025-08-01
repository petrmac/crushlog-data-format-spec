import 'package:test/test.dart';
import 'package:cldf/cldf.dart';
import 'dart:io';

void main() {
  group('MCP-Dart Interoperability', () {
    test('should read CLDF file created by MCP/Java CLI', () async {
      // We'll test with the files that were created earlier
      const testFilePath = 'test/mcp-created-test.cldf';

      // Check if the file exists
      final file = File(testFilePath);
      if (!await file.exists()) {
        print('Test file not found at $testFilePath - skipping test');
        return;
      }

      // Now read it with the Dart library
      final reader = CLDFReader();
      final archive = await reader.readFile(testFilePath);

      // Verify the structure
      expect(archive.manifest, isNotNull);
      expect(archive.manifest.version, equals('1.0.0'));
      expect(archive.manifest.format, equals('CLDF'));

      // Check locations
      expect(archive.locations, isNotEmpty);
      final location = archive.locations.first;
      expect(location.id, equals(1));
      expect(location.name, equals('Test Crag'));
      expect(location.country, equals('Slovakia'));
      expect(location.coordinates?.latitude, closeTo(48.8566, 0.0001));
      expect(location.coordinates?.longitude, closeTo(2.3522, 0.0001));

      // Check routes
      expect(archive.routes, isNotNull);
      expect(archive.routes!, isNotEmpty);
      final route = archive.routes!.first;
      expect(route.id, equals('1'));
      expect(route.name, equals('Test Route'));
      expect(route.locationId, equals('1'));
      expect(route.grades, isNotNull);
      expect(route.grades!['french'], equals('6a'));

      // Check sessions
      expect(archive.sessions, isNotNull);
      expect(archive.sessions!, isNotEmpty);
      final session = archive.sessions!.first;
      expect(session.id, equals(1));
      expect(session.date, equals('2024-01-15'));
      expect(session.locationId, equals(1));

      // Check climbs
      expect(archive.climbs, isNotNull);
      expect(archive.climbs!, isNotEmpty);
      final climb = archive.climbs!.first;
      expect(climb.id, equals(1));
      expect(climb.date, equals('2024-01-15'));
      expect(climb.routeId, equals('1'));
      expect(climb.type, equals(ClimbType.route));
      expect(climb.finishType, equals(FinishType.redpoint));

      // Verify dates are properly converted
      // Test various date formats that might come from MCP
      if (archive.climbs != null) {
        for (final climbItem in archive.climbs!) {
          // All dates should be in ISO format after parsing
          expect(
              RegExp(r'^\d{4}-\d{2}-\d{2}$').hasMatch(climbItem.date), isTrue,
              reason: 'Date should be in ISO format: ${climbItem.date}');
        }
      }
    });

    test('should handle MCP file with mixed date formats', () async {
      // This test assumes MCP created a file with various date formats
      const mixedDatesFile = 'test/mcp-mixed-dates.cldf';

      final file = File(mixedDatesFile);
      if (await file.exists()) {
        final reader = CLDFReader();
        final archive = await reader.readFile(mixedDatesFile);

        // Check that all dates are normalized to ISO format
        if (archive.sessions != null) {
          for (final session in archive.sessions!) {
            expect(
                RegExp(r'^\d{4}-\d{2}-\d{2}$').hasMatch(session.date), isTrue);
          }
        }

        if (archive.climbs != null) {
          for (final climb in archive.climbs!) {
            expect(RegExp(r'^\d{4}-\d{2}-\d{2}$').hasMatch(climb.date), isTrue);
          }
        }
      }
    });

    test('should handle MCP file with all optional fields', () async {
      // Test reading a comprehensive CLDF file with all optional fields populated
      const fullDataFile = 'test/mcp-full-data.cldf';

      final file = File(fullDataFile);
      if (await file.exists()) {
        final reader = CLDFReader();
        final archive = await reader.readFile(fullDataFile);

        // Verify comprehensive data
        expect(archive.manifest.appVersion, isNotNull);

        // Check route with all fields
        if (archive.routes != null && archive.routes!.isNotEmpty) {
          final routeWithFA =
              archive.routes!.where((r) => r.firstAscent != null);
          if (routeWithFA.isNotEmpty) {
            final route = routeWithFA.first;
            expect(route.firstAscent?.climberName, isNotNull);
            expect(route.firstAscent?.date,
                matches(RegExp(r'^\d{4}-\d{2}-\d{2}$')));
          }
        }

        // Check climb with all optional fields
        if (archive.climbs != null && archive.climbs!.isNotEmpty) {
          final climbWithBeta = archive.climbs!.where((c) => c.beta != null);
          if (climbWithBeta.isNotEmpty) {
            final climb = climbWithBeta.first;
            expect(climb.beta, isNotNull);
            expect(climb.rating, isNotNull);
            expect(climb.falls, isNotNull);
          }
        }
      }
    });
  });
}
