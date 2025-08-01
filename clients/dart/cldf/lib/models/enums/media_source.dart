import 'package:json_annotation/json_annotation.dart';

@JsonEnum(valueField: 'value')
enum MediaSource {
  embedded('embedded'),
  external('external'),
  reference('reference');

  final String value;
  const MediaSource(this.value);
}
