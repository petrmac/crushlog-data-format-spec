import 'package:json_annotation/json_annotation.dart';

@JsonEnum(valueField: 'value')
enum ProtectionRating {
  g('G'),
  pg('PG'),
  pg13('PG-13'),
  r('R'),
  x('X');

  final String value;
  const ProtectionRating(this.value);
}
