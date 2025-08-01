import 'package:json_annotation/json_annotation.dart';

@JsonEnum(valueField: 'value')
enum RockType {
  limestone('limestone'),
  granite('granite'),
  sandstone('sandstone'),
  basalt('basalt'),
  conglomerate('conglomerate'),
  gneiss('gneiss'),
  quartzite('quartzite'),
  rhyolite('rhyolite'),
  schist('schist'),
  tuff('tuff'),
  other('other');

  final String value;
  const RockType(this.value);
}
