import 'package:json_annotation/json_annotation.dart';
import 'climb_type.dart';

@JsonEnum(valueField: 'value')
enum FinishType {
  // Boulder finish types
  flash('flash'),
  top('top'),

  // Route finish types
  onsight('onsight'),
  redpoint('redpoint'),

  // Common finish types
  repeat('repeat'),
  project('project'),
  attempt('attempt');

  final String value;
  const FinishType(this.value);

  /// Check if this is a boulder-specific finish type
  bool get isBoulderFinish => this == flash || this == top;

  /// Check if this is a route-specific finish type
  bool get isRouteFinish => this == onsight || this == redpoint;

  /// Check if this finish type is valid for the given climb type
  bool isValidForClimbType(ClimbType climbType) {
    if (climbType == ClimbType.boulder) {
      return isBoulderFinish || [repeat, project, attempt].contains(this);
    } else {
      return isRouteFinish || [flash, repeat, project, attempt].contains(this);
    }
  }
}
