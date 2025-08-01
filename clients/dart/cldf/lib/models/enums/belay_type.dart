import 'package:json_annotation/json_annotation.dart';

@JsonEnum(valueField: 'value')
enum BelayType {
  topRope('topRope'),
  lead('lead'),
  autoBelay('autoBelay');

  final String value;
  const BelayType(this.value);
}
