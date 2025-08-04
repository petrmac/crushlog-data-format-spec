import 'package:json_annotation/json_annotation.dart';

/// Protection rating indicating safety level
@JsonEnum(valueField: 'value')
enum ProtectionRating {
  /// Bombproof protection (extremely safe)
  bombproof('bombproof'),

  /// Good protection throughout
  good('good'),

  /// Adequate protection but some runouts
  adequate('adequate'),

  /// Runout sections with spaced protection
  runout('runout'),

  /// Serious consequences for falls
  serious('serious'),

  /// Extremely dangerous with potential for ground falls
  x('x');

  const ProtectionRating(this.value);

  final String value;
}
