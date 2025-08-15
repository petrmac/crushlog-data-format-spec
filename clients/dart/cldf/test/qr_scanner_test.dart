import 'package:test/test.dart';
import 'package:cldf/qr/qr.dart';
import 'package:cldf/models/enums/route_type.dart';

void main() {
  group('QRScanner', () {
    late QRScanner scanner;

    setUp(() {
      scanner = QRScanner();
    });

    group('parse', () {
      test('should parse JSON QR data', () {
        const jsonData = '''
        {
          "version": 1,
          "clid": "clid:route:test123",
          "url": "https://crushlog.pro/route/test123",
          "route": {
            "id": 123,
            "name": "Test Route",
            "grade": "5.10a",
            "gradeSystem": "YDS",
            "type": "sport",
            "height": 15.5
          }
        }
        ''';

        final result = scanner.parse(jsonData);

        expect(result, isNotNull);
        expect(result!.version, equals(1));
        expect(result.clid, equals('clid:route:test123'));
        expect(result.url, equals('https://crushlog.pro/route/test123'));
        expect(result.route, isNotNull);
        expect(result.route!.name, equals('Test Route'));
        expect(result.route!.grade, equals('5.10a'));
        expect(result.route!.gradeSystem, equals('YDS'));
        expect(result.route!.type, equals('sport'));
        expect(result.route!.height, equals(15.5));
      });

      test('should parse URL format', () {
        const url = 'https://crushlog.pro/g/abc-def-123';

        final result = scanner.parse(url);

        expect(result, isNotNull);
        expect(result!.url, equals(url));
        expect(result.shortClid, equals('abc-def-123'));
        expect(result.hasOfflineData, isFalse);
      });

      test('should parse custom URI format', () {
        const uri =
            'cldf://global/route/abc-def-123?name=TestRoute&grade=V4&gradeSystem=vscale';

        final result = scanner.parse(uri);

        expect(result, isNotNull);
        expect(result!.clid, equals('clid:route:abc-def-123'));
        expect(result.route, isNotNull);
        expect(result.route!.name, equals('TestRoute'));
        expect(result.route!.grade, equals('V4'));
        expect(result.route!.gradeSystem, equals('vscale'));
        expect(result.hasOfflineData, isTrue);
      });

      test('should handle location data', () {
        const jsonData = '''
        {
          "version": 1,
          "clid": "clid:location:test-loc",
          "location": {
            "id": 456,
            "name": "Test Crag",
            "country": "USA",
            "state": "CA",
            "city": "Bishop",
            "indoor": false
          }
        }
        ''';

        final result = scanner.parse(jsonData);

        expect(result, isNotNull);
        expect(result!.location, isNotNull);
        expect(result.location!.name, equals('Test Crag'));
        expect(result.location!.country, equals('USA'));
        expect(result.location!.state, equals('CA'));
        expect(result.location!.city, equals('Bishop'));
        expect(result.location!.indoor, isFalse);
      });

      test('should handle empty or null data', () {
        expect(scanner.parse(''), isNull);
        expect(scanner.parse('   '), isNull);
      });

      test('should handle blockchain metadata', () {
        const jsonData = '''
        {
          "version": 1,
          "clid": "clid:route:test",
          "meta": {
            "blockchain": true
          }
        }
        ''';

        final result = scanner.parse(jsonData);

        expect(result, isNotNull);
        expect(result!.blockchainVerified, isTrue);
      });
    });

    group('toRoute', () {
      test('should convert parsed data to Route', () {
        const jsonData = '''
        {
          "version": 1,
          "clid": "clid:route:test123",
          "route": {
            "id": 123,
            "name": "Test Route",
            "grade": "V7",
            "gradeSystem": "vScale",
            "type": "boulder",
            "height": 4.5
          },
          "location": {
            "id": 456
          }
        }
        ''';

        final parsed = scanner.parse(jsonData);
        final route = scanner.toRoute(parsed!);

        expect(route, isNotNull);
        expect(route!.id, equals(123));
        expect(route.clid, equals('clid:route:test123'));
        expect(route.name, equals('Test Route'));
        expect(route.grades?['vScale'], equals('V7'));
        expect(route.routeType, equals(RouteType.boulder));
        expect(route.height, equals(4.5));
        expect(route.locationId, equals(456));
      });

      test('should handle different grade systems', () {
        final testCases = [
          ('YDS', 'yds', '5.10a'),
          ('vScale', 'vScale', 'V4'),
          ('font', 'font', '6a'),
          ('french', 'french', '6b'),
          ('uiaa', 'uiaa', 'VII'),
        ];

        for (final (system, field, grade) in testCases) {
          final jsonData =
              '''
          {
            "route": {
              "name": "Test",
              "grade": "$grade",
              "gradeSystem": "$system"
            }
          }
          ''';

          final parsed = scanner.parse(jsonData);
          final route = scanner.toRoute(parsed!);

          expect(route, isNotNull, reason: 'Failed for $system');

          switch (field) {
            case 'yds':
              expect(route!.grades?['yds'], equals(grade));
              break;
            case 'vScale':
              expect(route!.grades?['vScale'], equals(grade));
              break;
            case 'font':
              expect(route!.grades?['font'], equals(grade));
              break;
            case 'french':
              expect(route!.grades?['french'], equals(grade));
              break;
            case 'uiaa':
              expect(route!.grades?['uiaa'], equals(grade));
              break;
          }
        }
      });

      test('should return null if no route data', () {
        const jsonData = '''
        {
          "version": 1,
          "clid": "clid:location:test"
        }
        ''';

        final parsed = scanner.parse(jsonData);
        final route = scanner.toRoute(parsed!);

        expect(route, isNull);
      });
    });

    group('toLocation', () {
      test('should convert parsed data to Location', () {
        const jsonData = '''
        {
          "version": 1,
          "clid": "clid:location:test-loc",
          "location": {
            "id": 456,
            "clid": "clid:location:test-loc",
            "name": "Test Crag",
            "country": "USA",
            "state": "California",
            "city": "Bishop",
            "indoor": true
          }
        }
        ''';

        final parsed = scanner.parse(jsonData);
        final location = scanner.toLocation(parsed!);

        expect(location, isNotNull);
        expect(location!.id, equals(456));
        expect(location.clid, equals('clid:location:test-loc'));
        expect(location.name, equals('Test Crag'));
        expect(location.country, equals('USA'));
        expect(location.state, equals('California'));
        expect(location.city, equals('Bishop'));
        expect(location.isIndoor, isTrue);
      });

      test('should return null if no location data', () {
        const jsonData = '''
        {
          "version": 1,
          "clid": "clid:route:test"
        }
        ''';

        final parsed = scanner.parse(jsonData);
        final location = scanner.toLocation(parsed!);

        expect(location, isNull);
      });

      test('should use root clid if location clid not provided', () {
        const jsonData = '''
        {
          "version": 1,
          "clid": "clid:location:fallback",
          "location": {
            "id": 789,
            "name": "Fallback Location"
          }
        }
        ''';

        final parsed = scanner.parse(jsonData);
        final location = scanner.toLocation(parsed!);

        expect(location, isNotNull);
        expect(location!.clid, equals('clid:location:fallback'));
      });
    });

    group('parseRouteType', () {
      test('should parse all route types correctly', () {
        final routeTypes = {
          'boulder': RouteType.boulder,
          'sport': RouteType.route,
          'trad': RouteType.route,
          'route': RouteType.route,
        };

        for (final entry in routeTypes.entries) {
          final jsonData =
              '''
          {
            "route": {
              "name": "Test",
              "type": "${entry.key}"
            }
          }
          ''';

          final parsed = scanner.parse(jsonData);
          final route = scanner.toRoute(parsed!);

          expect(
            route?.routeType,
            equals(entry.value),
            reason: 'Failed for ${entry.key}',
          );
        }
      });
    });
  });
}
