import 'package:json_annotation/json_annotation.dart';

import 'media_item.dart';

part 'media.g.dart';

/// Media references for climbs
@JsonSerializable(includeIfNull: false)
class Media {
  /// Creates a new [Media] instance
  Media({this.items, this.count});

  /// Creates a [Media] from JSON
  factory Media.fromJson(Map<String, dynamic> json) => _$MediaFromJson(json);

  /// List of media items
  final List<MediaItem>? items;

  /// Total media count
  final int? count;

  /// Converts this [Media] to JSON
  Map<String, dynamic> toJson() => _$MediaToJson(this);
}
