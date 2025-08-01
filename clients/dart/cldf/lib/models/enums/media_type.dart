import 'package:json_annotation/json_annotation.dart';

@JsonEnum(valueField: 'value')
enum MediaType {
  photo('photo'),
  video('video');

  final String value;
  const MediaType(this.value);
}
