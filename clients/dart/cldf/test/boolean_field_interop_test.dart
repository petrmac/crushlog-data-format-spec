import 'dart:convert';
import 'dart:io';

import 'package:cldf/cldf.dart';
import 'package:test/test.dart';

void main() {
  group('Boolean Field Serialization Interoperability', () {
    test(
      'Dart-generated archive with isRepeat and isIndoor should be readable by Java',
      () async {
        final outputPath = 'test/boolean-field-test.cldf';

        // Create archive using Dart library with boolean fields
        final manifest = Manifest(
          version: '1.0.0',
          format: 'CLDF',
          creationDate: DateTime.now().toUtc(),
          platform: Platform.iOS,
          appVersion: '1.4.5',
        );

        final location = Location(
          id: 1,
          name: 'Indoor Gym',
          isIndoor: true, // Testing isIndoor field
        );

        final session = Session(
          id: 1,
          date: '2024-01-15',
          locationId: 1,
          isIndoor: true, // Testing isIndoor field in Session
        );

        final climb = Climb(
          id: 1,
          date: '2024-01-15',
          sessionId: 1,
          routeId: 1,
          type: ClimbType.route,
          finishType: FinishType.redpoint,
          isRepeat: true, // Testing isRepeat field
          isIndoor: true, // Testing isIndoor field in Climb
        );

        final route = Route(
          id: 1,
          locationId: 1,
          name: 'Test Route',
          routeType: RouteType.route,
        );

        final archive = CLDFArchive(
          manifest: manifest,
          locations: [location],
          sessions: [session],
          climbs: [climb],
          routes: [route],
        );

        // Write archive
        print('Creating CLDF archive with Dart library...');
        final writer = CLDFWriter();
        await writer.writeFile(outputPath, archive);

        // Verify the raw JSON content to ensure fields are correctly serialized
        print('Verifying JSON serialization...');
        final reader = CLDFReader();
        final readArchive = await reader.readFile(outputPath);

        // Verify Session isIndoor field
        expect(readArchive.sessions, isNotNull);
        expect(readArchive.sessions!.length, equals(1));
        expect(readArchive.sessions!.first.isIndoor, isTrue);
        print('✓ Session isIndoor field correctly serialized');

        // Verify Climb isRepeat field
        expect(readArchive.climbs, isNotNull);
        expect(readArchive.climbs!.length, equals(1));
        expect(readArchive.climbs!.first.isRepeat, isTrue);
        expect(readArchive.climbs!.first.isIndoor, isTrue);
        print('✓ Climb isRepeat and isIndoor fields correctly serialized');

        // Verify Location isIndoor field
        expect(readArchive.locations.length, equals(1));
        expect(readArchive.locations.first.isIndoor, isTrue);
        print('✓ Location isIndoor field correctly serialized');

        // Verify with Java CLI if available
        final cliPath =
            '/Users/petrmacek/git-mirrors/crushlog-data-format-spec/clients/java/cldf-tool/build/native/nativeCompile/cldf';

        if (File(cliPath).existsSync()) {
          print('\nVerifying with Java CLI...');

          // Query sessions
          final sessionsResult = await Process.run(cliPath, [
            'query',
            outputPath,
            '--select',
            'sessions',
            '--json',
            'json',
          ], workingDirectory: Directory.current.path);

          if (sessionsResult.exitCode != 0) {
            print('Java CLI query failed: ${sessionsResult.stderr}');
            fail(
              'Java CLI could not read Dart-generated archive with boolean fields',
            );
          }

          final sessionsOutput =
              jsonDecode(sessionsResult.stdout as String)
                  as Map<String, dynamic>;
          final sessions =
              (sessionsOutput['data'] as Map<String, dynamic>)['results']
                  as List;
          expect(sessions.length, equals(1));
          final session = sessions.first as Map<String, dynamic>;
          expect(
            session.containsKey('isIndoor'),
            isTrue,
            reason: 'Session JSON should contain isIndoor field',
          );
          expect(
            session['isIndoor'],
            isTrue,
            reason: 'Session isIndoor should be true',
          );
          print('✓ Java correctly read Session.isIndoor = true');

          // Query climbs
          final climbsResult = await Process.run(cliPath, [
            'query',
            outputPath,
            '--select',
            'climbs',
            '--json',
            'json',
          ], workingDirectory: Directory.current.path);

          final climbsOutput =
              jsonDecode(climbsResult.stdout as String) as Map<String, dynamic>;
          final climbs =
              (climbsOutput['data'] as Map<String, dynamic>)['results'] as List;
          expect(climbs.length, equals(1));
          final climb = climbs.first as Map<String, dynamic>;
          expect(
            climb.containsKey('isRepeat'),
            isTrue,
            reason: 'Climb JSON should contain isRepeat field',
          );
          expect(
            climb['isRepeat'],
            isTrue,
            reason: 'Climb isRepeat should be true',
          );
          expect(
            climb.containsKey('isIndoor'),
            isTrue,
            reason: 'Climb JSON should contain isIndoor field',
          );
          expect(
            climb['isIndoor'],
            isTrue,
            reason: 'Climb isIndoor should be true',
          );
          print('✓ Java correctly read Climb.isRepeat = true');
          print('✓ Java correctly read Climb.isIndoor = true');

          // Query locations
          final locationsResult = await Process.run(cliPath, [
            'query',
            outputPath,
            '--select',
            'locations',
            '--json',
            'json',
          ], workingDirectory: Directory.current.path);

          final locationsOutput =
              jsonDecode(locationsResult.stdout as String)
                  as Map<String, dynamic>;
          final locations =
              (locationsOutput['data'] as Map<String, dynamic>)['results']
                  as List;
          expect(locations.length, equals(1));
          final location = locations.first as Map<String, dynamic>;
          expect(
            location.containsKey('isIndoor'),
            isTrue,
            reason: 'Location JSON should contain isIndoor field',
          );
          expect(
            location['isIndoor'],
            isTrue,
            reason: 'Location isIndoor should be true',
          );
          print('✓ Java correctly read Location.isIndoor = true');

          print(
            '\n✅ All boolean fields correctly serialized and readable by both Dart and Java',
          );
        } else {
          print(
            'Java CLI not available at $cliPath, skipping Java verification',
          );
          print(
            'Note: Run this test after building the Java CLI to verify full interoperability',
          );
        }

        // Clean up
        await File(outputPath).delete();

        print('\nTest completed successfully!');
      },
    );

    test(
      'Java-generated archive with isRepeat and isIndoor should be readable by Dart',
      () async {
        // Check if Java CLI is available
        final cliPath =
            '/Users/petrmacek/git-mirrors/crushlog-data-format-spec/clients/java/cldf-tool/build/native/nativeCompile/cldf';

        if (!File(cliPath).existsSync()) {
          print(
            'Java CLI not available, skipping Java->Dart interoperability test',
          );
          print('Expected CLI path: $cliPath');
          return;
        }

        // Test data with boolean fields
        final testData = {
          'manifest': {
            'version': '1.0.0',
            'format': 'CLDF',
            'creationDate': '2024-01-15T10:00:00Z',
            'platform': 'Desktop',
            'appVersion': '1.4.5',
          },
          'locations': [
            {
              'id': 1,
              'name': 'Indoor Gym',
              'isIndoor': true, // Testing isIndoor field
            },
          ],
          'sessions': [
            {
              'id': 1,
              'date': '2024-01-15',
              'locationId': 1,
              'isIndoor': true, // Testing isIndoor field in Session
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
          'climbs': [
            {
              'id': 1,
              'date': '2024-01-15',
              'sessionId': 1,
              'routeId': '1',
              'type': 'route',
              'finishType': 'redpoint',
              'isRepeat': true, // Testing isRepeat field
              'isIndoor': true, // Testing isIndoor field in Climb
            },
          ],
          'tags': [],
          'checksums': {'algorithm': 'SHA-256'},
        };

        // Write test data to JSON file
        final jsonFile = File('test/boolean-field-test-data.json');
        await jsonFile.writeAsString(jsonEncode(testData));

        // Create CLDF using Java CLI
        final outputPath = 'test/java-boolean-field-test.cldf';

        print('Creating CLDF file with boolean fields using Java CLI...');
        final createResult = await Process.run(cliPath, [
          'create',
          '--template',
          'empty',
          '--output',
          outputPath,
          '--from-json',
          jsonFile.path,
        ], workingDirectory: Directory.current.path);

        if (createResult.exitCode != 0) {
          print('Failed to create CLDF file: ${createResult.stderr}');
          await jsonFile.delete();
          fail('Java CLI failed to create CLDF file');
        }

        print('✓ Java CLI created archive with boolean fields');

        // Read with Dart library
        print('\nReading CLDF file with Dart library...');
        final reader = CLDFReader();
        final archive = await reader.readFile(outputPath);

        // Verify boolean fields
        expect(archive.locations.length, equals(1));
        expect(archive.locations.first.isIndoor, isTrue);
        print('✓ Dart correctly read Location.isIndoor = true');

        expect(archive.sessions, isNotNull);
        expect(archive.sessions!.length, equals(1));
        expect(archive.sessions!.first.isIndoor, isTrue);
        print('✓ Dart correctly read Session.isIndoor = true');

        expect(archive.climbs, isNotNull);
        expect(archive.climbs!.length, equals(1));
        expect(archive.climbs!.first.isRepeat, isTrue);
        expect(archive.climbs!.first.isIndoor, isTrue);
        print('✓ Dart correctly read Climb.isRepeat = true');
        print('✓ Dart correctly read Climb.isIndoor = true');

        print(
          '\n✅ All boolean fields correctly serialized by Java and readable by Dart',
        );

        // Clean up
        await jsonFile.delete();
        await File(outputPath).delete();

        print('\nTest completed successfully!');
      },
    );
  });
}
