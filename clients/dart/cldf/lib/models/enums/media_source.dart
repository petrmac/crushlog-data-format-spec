import 'package:json_annotation/json_annotation.dart';

/// Source of media
@JsonEnum(valueField: 'value')
enum MediaSource {
  /// File stored locally on device
  local('local'),

  /// File stored in cloud (iCloud, Google Photos, etc.)
  cloud('cloud'),

  /// Just a reference/URL, not actual file
  reference('reference'),

  /// Embedded in the archive itself
  embedded('embedded'),

  /// Legacy values for backward compatibility
  external('external'),
  photosLibrary('photos_library');

  const MediaSource(this.value);

  final String value;
}
