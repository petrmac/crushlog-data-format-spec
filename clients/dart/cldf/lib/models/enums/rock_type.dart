import 'package:json_annotation/json_annotation.dart';

@JsonEnum(valueField: 'value')
enum RockType {
  sandstone('sandstone'),
  limestone('limestone'),
  granite('granite'),
  basalt('basalt'),
  gneiss('gneiss'),
  quartzite('quartzite'),
  conglomerate('conglomerate'),
  schist('schist'),
  dolomite('dolomite'),
  slate('slate'),
  rhyolite('rhyolite'),
  gabbro('gabbro'),
  volcanicTuff('volcanicTuff'),
  andesite('andesite'),
  chalk('chalk');

  final String value;
  const RockType(this.value);
}
