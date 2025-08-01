import 'dart:convert';
import 'dart:io';

import 'package:cldf/cldf.dart';
import 'package:test/test.dart';

void main() {
  group('Full MCP-Dart Interoperability Test', () {
    test('Create with Java CLI and read with Dart', () async {
      // Check if Java CLI is available
      final cliPath =
          '/Users/petrmacek/git-mirrors/crushlog-data-format-spec/clients/java/cldf-tool/build/native/nativeCompile/cldf';
      
      if (!File(cliPath).existsSync()) {
        print('Java CLI not available, skipping interoperability test');
        print('Expected CLI path: $cliPath');
        return;
      }
      // Test data with various date formats
      final testData = {
        'manifest': {
          'version': '1.0.0',
          'format': 'CLDF',
          'creationDate': '2024-01-15T10:00:00Z',
          'platform': 'Desktop',
          'appVersion': '1.0.0',
        },
        'locations': [
          {
            'id': 1,
            'name': 'Test Crag',
            'country': 'Slovakia',
            'isIndoor': false,
            'coordinates': {'latitude': 48.8566, 'longitude': 2.3522},
          },
        ],
        'routes': [
          {
            'id': '1',
            'locationId': '1',
            'name': 'Test Route',
            'routeType': 'route',
          },
        ],
        'sessions': [
          {'id': 1, 'date': '2024-01-15', 'locationId': 1},
          {'id': 2, 'date': '2024/01/16', 'locationId': 1},
          {'id': 3, 'date': '01-17-2024', 'locationId': 1},
        ],
        'climbs': [
          {
            'id': 1,
            'date': '2024-01-15',
            'sessionId': 1,
            'routeId': '1',
            'type': 'route',
            'finishType': 'redpoint',
          },
          {
            'id': 2,
            'date': '2024/01/16',
            'sessionId': 2,
            'routeId': '1',
            'type': 'route',
            'finishType': 'flash',
          },
        ],
        'tags': [],
        'checksums': {'algorithm': 'SHA-256'},
      };

      // Write test data to JSON file
      final jsonFile = File('test/interop-test-data.json');
      await jsonFile.writeAsString(jsonEncode(testData));

      // Create CLDF using Java CLI
      final outputPath = 'test/dart-java-interop.cldf';

      print('Creating CLDF file using Java CLI...');
      final result = await Process.run(cliPath, [
        'create',
        '--template',
        'empty',
        '--output',
        outputPath,
        '--from-json',
        jsonFile.path,
      ], workingDirectory: Directory.current.path);

      if (result.exitCode != 0) {
        print('Failed to create CLDF file: ${result.stderr}');
        // Clean up
        await jsonFile.delete();
        fail('Java CLI failed to create CLDF file');
      }

      print('Java CLI output: ${result.stdout}');

      // Read with Dart library
      print('Reading CLDF file with Dart library...');
      final reader = CLDFReader();
      final archive = await reader.readFile(outputPath);

      // Verify structure
      expect(archive.manifest.version, equals('1.0.0'));
      expect(archive.locations.length, equals(1));
      expect(archive.routes?.length, equals(1));
      expect(archive.sessions?.length, equals(3));
      expect(archive.climbs?.length, equals(2));

      // Verify date conversion - all dates should be in ISO format
      print('Checking date formats...');
      for (final session in archive.sessions!) {
        print('Session ${session.id} date: ${session.date}');
        expect(
          RegExp(r'^\d{4}-\d{2}-\d{2}$').hasMatch(session.date),
          isTrue,
          reason: 'Session date should be in ISO format: ${session.date}',
        );
      }

      for (final climb in archive.climbs!) {
        print('Climb ${climb.id} date: ${climb.date}');
        expect(
          RegExp(r'^\d{4}-\d{2}-\d{2}$').hasMatch(climb.date),
          isTrue,
          reason: 'Climb date should be in ISO format: ${climb.date}',
        );
      }

      // Clean up
      await jsonFile.delete();
      await File(outputPath).delete();

      print('Test completed successfully!');
    });
  });
}
