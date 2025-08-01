import 'package:json_annotation/json_annotation.dart';

@JsonEnum(valueField: 'value')
enum ClimbType {
  boulder('boulder'),
  route('route');

  final String value;
  const ClimbType(this.value);
}
