import 'package:json_annotation/json_annotation.dart';

@JsonEnum(valueField: 'value')
enum Platform {
  iOS('iOS'),
  android('Android'),
  web('Web'),
  desktop('Desktop');

  final String value;
  const Platform(this.value);
}
