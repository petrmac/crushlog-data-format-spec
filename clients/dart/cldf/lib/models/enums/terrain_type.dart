import 'package:json_annotation/json_annotation.dart';

@JsonEnum(valueField: 'value')
enum TerrainType {
  natural('natural'),
  artificial('artificial'),
  mixed('mixed');

  final String value;
  const TerrainType(this.value);
}
