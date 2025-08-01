import 'package:json_annotation/json_annotation.dart';

@JsonEnum(valueField: 'value')
enum Platform {
  desktop('Desktop'),
  mobile('Mobile'),
  web('Web'),
  api('API');

  final String value;
  const Platform(this.value);
}
