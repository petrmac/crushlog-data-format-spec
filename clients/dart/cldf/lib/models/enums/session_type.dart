import 'package:json_annotation/json_annotation.dart';

@JsonEnum(valueField: 'value')
enum SessionType {
  training('training'),
  bouldering('bouldering'),
  sportClimbing('sportClimbing'),
  trad('trad'),
  alpine('alpine'),
  ice('ice'),
  mixed('mixed'),
  deepWaterSolo('deepWaterSolo'),
  indoorClimbing('indoorClimbing'),
  indoorBouldering('indoorBouldering');

  final String value;
  const SessionType(this.value);
}
