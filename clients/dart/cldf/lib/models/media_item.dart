import 'package:json_annotation/json_annotation.dart';

import '../utils/date_time_converter.dart';
import 'enums/media_source.dart';
import 'enums/media_type.dart';

part 'media_item.g.dart';

/// A media item (photo or video)
@JsonSerializable()
class MediaItem {
  /// Creates a new [MediaItem] instance
  MediaItem({
    required this.id,
    required this.type,
    required this.source,
    required this.path,
    this.filename,
    this.size,
    this.mimeType,
    this.width,
    this.height,
    this.duration,
    this.createdAt,
    this.coordinates,
    this.climbId,
    this.routeId,
    this.locationId,
    this.caption,
    this.tags,
  });

  /// Creates a [MediaItem] from JSON
  factory MediaItem.fromJson(Map<String, dynamic> json) =>
      _$MediaItemFromJson(json);

  /// Unique identifier
  final String id;

  /// Type of media
  final MediaType type;

  /// Source of the media
  final MediaSource source;

  /// File path or URL
  final String path;

  /// Original filename
  final String? filename;

  /// File size in bytes
  final int? size;

  /// MIME type
  final String? mimeType;

  /// Width in pixels
  final int? width;

  /// Height in pixels
  final int? height;

  /// Duration in seconds (for videos)
  final int? duration;

  /// Creation date
  @FlexibleDateTimeConverter()
  final DateTime? createdAt;

  /// GPS coordinates
  final MediaCoordinates? coordinates;

  /// Associated climb ID
  final int? climbId;

  /// Associated route ID
  final int? routeId;

  /// Associated location ID
  final int? locationId;

  /// Caption
  final String? caption;

  /// Associated tags
  final List<String>? tags;

  /// Converts this [MediaItem] to JSON
  Map<String, dynamic> toJson() => _$MediaItemToJson(this);
}

/// GPS coordinates for media
@JsonSerializable()
class MediaCoordinates {
  /// Creates a new [MediaCoordinates] instance
  MediaCoordinates({
    required this.latitude,
    required this.longitude,
    this.altitude,
  });

  /// Creates a [MediaCoordinates] from JSON
  factory MediaCoordinates.fromJson(Map<String, dynamic> json) =>
      _$MediaCoordinatesFromJson(json);

  /// Latitude coordinate
  final double latitude;

  /// Longitude coordinate
  final double longitude;

  /// Altitude in meters
  final double? altitude;

  /// Converts this [MediaCoordinates] to JSON
  Map<String, dynamic> toJson() => _$MediaCoordinatesToJson(this);
}
