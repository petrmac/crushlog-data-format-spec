import 'dart:io';
import 'package:test/test.dart';
import 'package:cldf/qr/qr.dart';

void main() {
  group('Scan Java-generated QR codes', () {
    final testResourcesDir = Directory('test/qr-test-resources');
    final scanner = QRScanner();

    test('Scan Java route QR - The Nose', () async {
      final qrFile = File('${testResourcesDir.path}/java-the-nose.png');
      if (!qrFile.existsSync()) {
        print('Java QR file not found, skipping test');
        return;
      }

      final imageBytes = await qrFile.readAsBytes();
      final parsedData = await scanner.scanImage(imageBytes);

      expect(parsedData, isNotNull);
      expect(parsedData!.version, equals(1)); // Java CLI generates v1 format
      expect(parsedData.clid, isNotNull);
      expect(parsedData.clid, startsWith('clid:route:'));
      
      if (parsedData.route != null) {
        print('Route name: ${parsedData.route!.name}');
        print('Route grade: ${parsedData.route!.grade}');
        print('Route type: ${parsedData.route!.type}');
      }
      
      // Convert to Dart Route object
      final route = scanner.toRoute(parsedData);
      expect(route, isNotNull);
      expect(route!.name, equals('The Nose'));
      expect(route.grades?['yds'], equals('5.14a'));
      
      print('✓ Successfully scanned Java-generated QR for The Nose');
    });

    test('Scan Java route QR - Midnight Lightning', () async {
      final qrFile = File('${testResourcesDir.path}/java-midnight.png');
      if (!qrFile.existsSync()) {
        print('Java QR file not found, skipping test');
        return;
      }

      final imageBytes = await qrFile.readAsBytes();
      final parsedData = await scanner.scanImage(imageBytes);

      expect(parsedData, isNotNull);
      expect(parsedData!.version, equals(1)); // Java CLI generates v1 format
      expect(parsedData.clid, isNotNull);
      
      final route = scanner.toRoute(parsedData);
      expect(route, isNotNull);
      expect(route!.name, equals('Midnight Lightning'));
      expect(route.grades?['vScale'], equals('V8'));
      
      print('✓ Successfully scanned Java-generated QR for Midnight Lightning');
    });

    test('Scan Java location QR - Fontainebleau', () async {
      final qrFile = File('${testResourcesDir.path}/java-font-location.png');
      if (!qrFile.existsSync()) {
        print('Java QR file not found, skipping test');
        return;
      }

      final imageBytes = await qrFile.readAsBytes();
      final parsedData = await scanner.scanImage(imageBytes);

      expect(parsedData, isNotNull);
      expect(parsedData!.version, equals(1)); // Java CLI generates v1 format
      expect(parsedData.clid, isNotNull);
      expect(parsedData.clid, startsWith('clid:location:'));
      
      final location = scanner.toLocation(parsedData);
      expect(location, isNotNull);
      expect(location!.name, equals('Fontainebleau'));
      expect(location.country, equals('FR'));
      
      print('✓ Successfully scanned Java-generated location QR');
    });

    test('Scan Java indoor route QR - Purple Problem', () async {
      final qrFile = File('${testResourcesDir.path}/java-indoor-route.png');
      if (!qrFile.existsSync()) {
        print('Java QR file not found, skipping test');
        return;
      }

      final imageBytes = await qrFile.readAsBytes();
      final parsedData = await scanner.scanImage(imageBytes);

      expect(parsedData, isNotNull);
      expect(parsedData!.version, equals(1)); // Java CLI generates v1 format
      
      final route = scanner.toRoute(parsedData);
      expect(route, isNotNull);
      expect(route!.name, equals('Purple Problem'));
      expect(route.grades?['vScale'], equals('V5'));
      
      // Check location is indoor
      if (parsedData.location != null) {
        print('Location: ${parsedData.location!.name}');
        print('Indoor: ${parsedData.location!.indoor}');
        expect(parsedData.location!.indoor, isTrue);
      }
      
      print('✓ Successfully scanned Java-generated indoor route QR');
    });
  });
}