import 'package:json_annotation/json_annotation.dart';

import '../enums/media_type.dart';
import '../enums/media_source.dart';
import '../enums/media_designation.dart';

part 'media_item.g.dart';

/// Media item for inline use within climbs
/// Matches the CLDF media.schema.json structure
@JsonSerializable(includeIfNull: false)
class MediaItem {
  /// Creates a new [MediaItem] instance
  MediaItem({
    required this.type,
    required this.path,
    this.assetId,
    this.thumbnailPath,
    this.source,
    this.designation,
    this.caption,
    this.timestamp,
    this.metadata,
  });

  /// Creates a [MediaItem] from JSON
  factory MediaItem.fromJson(Map<String, dynamic> json) =>
      _$MediaItemFromJson(json);

  /// Media type
  final MediaType type;

  /// Primary path/URL to the media file
  final String path;

  /// Asset ID for cloud/library stored media
  final String? assetId;

  /// Path to thumbnail (for performance)
  final String? thumbnailPath;

  /// Source of the media
  final MediaSource? source;

  /// Purpose or type of media content
  final MediaDesignation? designation;

  /// User-provided description or caption
  final String? caption;

  /// When the media was created or taken
  final String? timestamp;

  /// Additional metadata
  final Map<String, dynamic>? metadata;

  /// Converts this [MediaItem] to JSON
  Map<String, dynamic> toJson() => _$MediaItemToJson(this);
}
