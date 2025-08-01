import 'package:json_annotation/json_annotation.dart';

@JsonEnum(valueField: 'value')
enum GradeSystem {
  vScale('vScale'),
  font('font'),
  french('french'),
  yds('yds'),
  uiaa('uiaa');

  final String value;
  const GradeSystem(this.value);
}
