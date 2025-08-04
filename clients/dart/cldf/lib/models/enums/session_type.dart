import 'package:json_annotation/json_annotation.dart';

@JsonEnum(valueField: 'value')
enum SessionType {
  sportClimbing('sportClimbing'),
  multiPitch('multiPitch'),
  tradClimbing('tradClimbing'),
  bouldering('bouldering'),
  indoorClimbing('indoorClimbing'),
  indoorBouldering('indoorBouldering'),
  boardSession('boardSession');

  final String value;
  const SessionType(this.value);
}
