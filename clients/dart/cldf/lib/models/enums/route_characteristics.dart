import 'package:json_annotation/json_annotation.dart';

/// Route protection characteristics
@JsonEnum(valueField: 'value')
enum RouteCharacteristics {
  /// Traditional protection (gear placed by climber)
  trad('trad'),

  /// Bolted route (fixed protection)
  bolted('bolted');

  /// Creates a new [RouteCharacteristics] instance
  const RouteCharacteristics(this.value);

  /// The string value of this enum
  final String value;
}
