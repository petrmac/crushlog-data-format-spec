import 'package:json_annotation/json_annotation.dart';

/// Purpose or type of media content
@JsonEnum(valueField: 'value')
enum MediaDesignation {
  topo('topo'), // Route diagram/map
  beta('beta'), // How-to information
  approach('approach'), // Access/approach info
  log('log'), // Climb documentation
  overview('overview'), // General view/panorama
  conditions('conditions'), // Current conditions
  gear('gear'), // Gear placement/requirements
  descent('descent'), // Descent information
  other('other'); // Unspecified purpose

  const MediaDesignation(this.value);
  final String value;

  @override
  String toString() => value;

  /// Parse from string value, defaulting to 'other' if unknown
  static MediaDesignation fromValue(String? value) {
    if (value == null) return MediaDesignation.other;
    return MediaDesignation.values.firstWhere(
      (d) => d.value == value.toLowerCase(),
      orElse: () => MediaDesignation.other,
    );
  }
}