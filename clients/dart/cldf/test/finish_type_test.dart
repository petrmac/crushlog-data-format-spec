import 'package:test/test.dart';
import 'package:cldf/cldf.dart';

void main() {
  group('FinishType Enum Tests', () {
    test('should identify boulder finish types', () {
      expect(FinishType.flash.isBoulderFinish, isTrue);
      expect(FinishType.top.isBoulderFinish, isTrue);

      expect(FinishType.onsight.isBoulderFinish, isFalse);
      expect(FinishType.redpoint.isBoulderFinish, isFalse);
      expect(FinishType.repeat.isBoulderFinish, isFalse);
      expect(FinishType.project.isBoulderFinish, isFalse);
      expect(FinishType.attempt.isBoulderFinish, isFalse);
    });

    test('should identify route finish types', () {
      expect(FinishType.onsight.isRouteFinish, isTrue);
      expect(FinishType.redpoint.isRouteFinish, isTrue);

      expect(FinishType.flash.isRouteFinish, isFalse);
      expect(FinishType.top.isRouteFinish, isFalse);
      expect(FinishType.repeat.isRouteFinish, isFalse);
      expect(FinishType.project.isRouteFinish, isFalse);
      expect(FinishType.attempt.isRouteFinish, isFalse);
    });

    test('should validate finish types for boulder climbs', () {
      // Boulder-specific finish types
      expect(FinishType.flash.isValidForClimbType(ClimbType.boulder), isTrue);
      expect(FinishType.top.isValidForClimbType(ClimbType.boulder), isTrue);

      // Common finish types
      expect(FinishType.repeat.isValidForClimbType(ClimbType.boulder), isTrue);
      expect(FinishType.project.isValidForClimbType(ClimbType.boulder), isTrue);
      expect(FinishType.attempt.isValidForClimbType(ClimbType.boulder), isTrue);

      // Route-specific finish types (not valid for boulder)
      expect(
        FinishType.onsight.isValidForClimbType(ClimbType.boulder),
        isFalse,
      );
      expect(
        FinishType.redpoint.isValidForClimbType(ClimbType.boulder),
        isFalse,
      );
    });

    test('should validate finish types for route climbs', () {
      // Route-specific finish types
      expect(FinishType.onsight.isValidForClimbType(ClimbType.route), isTrue);
      expect(FinishType.redpoint.isValidForClimbType(ClimbType.route), isTrue);

      // Common finish types
      expect(FinishType.flash.isValidForClimbType(ClimbType.route), isTrue);
      expect(FinishType.repeat.isValidForClimbType(ClimbType.route), isTrue);
      expect(FinishType.project.isValidForClimbType(ClimbType.route), isTrue);
      expect(FinishType.attempt.isValidForClimbType(ClimbType.route), isTrue);

      // Boulder-specific finish type (not valid for route)
      expect(FinishType.top.isValidForClimbType(ClimbType.route), isFalse);
    });

    test('should have correct string values', () {
      expect(FinishType.flash.value, equals('flash'));
      expect(FinishType.top.value, equals('top'));
      expect(FinishType.onsight.value, equals('onsight'));
      expect(FinishType.redpoint.value, equals('redpoint'));
      expect(FinishType.repeat.value, equals('repeat'));
      expect(FinishType.project.value, equals('project'));
      expect(FinishType.attempt.value, equals('attempt'));
    });

    test('should have all enum values', () {
      expect(FinishType.values.length, equals(7));
      expect(FinishType.values, contains(FinishType.flash));
      expect(FinishType.values, contains(FinishType.top));
      expect(FinishType.values, contains(FinishType.onsight));
      expect(FinishType.values, contains(FinishType.redpoint));
      expect(FinishType.values, contains(FinishType.repeat));
      expect(FinishType.values, contains(FinishType.project));
      expect(FinishType.values, contains(FinishType.attempt));
    });
  });
}
