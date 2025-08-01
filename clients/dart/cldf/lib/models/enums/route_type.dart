import 'package:json_annotation/json_annotation.dart';

@JsonEnum(valueField: 'value')
enum RouteType {
  boulder('boulder'),
  route('route');

  final String value;
  const RouteType(this.value);
}
