import 'package:json_annotation/json_annotation.dart';

import '../enums/media_type.dart';
import '../enums/media_source.dart';

part 'flexible_media_item.g.dart';

/// Individual media item for flexible media model
@JsonSerializable()
class FlexibleMediaItem {
  /// Creates a new [FlexibleMediaItem] instance
  FlexibleMediaItem({
    required this.type,
    required this.path,
    this.assetId,
    this.thumbnailPath,
    this.source,
    this.metadata,
  });

  /// Creates a [FlexibleMediaItem] from JSON
  factory FlexibleMediaItem.fromJson(Map<String, dynamic> json) =>
      _$FlexibleMediaItemFromJson(json);

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

  /// Additional metadata
  final Map<String, dynamic>? metadata;

  /// Converts this [FlexibleMediaItem] to JSON
  Map<String, dynamic> toJson() => _$FlexibleMediaItemToJson(this);
}
