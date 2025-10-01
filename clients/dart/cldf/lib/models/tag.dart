import 'package:json_annotation/json_annotation.dart';

part 'tag.g.dart';

/// A tag for categorization
@JsonSerializable(includeIfNull: false)
class Tag {
  /// Creates a new [Tag] instance
  Tag({
    required this.id,
    required this.name,
    required this.isPredefined,
    this.predefinedTagKey,
    this.category,
    this.description,
    this.color,
  });

  /// Creates a [Tag] from JSON
  factory Tag.fromJson(Map<String, dynamic> json) => _$TagFromJson(json);

  /// Unique identifier
  final int id;

  /// Tag name/value
  final String name;

  /// Whether this is a system-defined tag
  final bool isPredefined;

  /// Key for predefined tags
  final String? predefinedTagKey;

  /// Tag category
  final String? category;

  /// Description
  final String? description;

  /// Associated color (hex format)
  final String? color;

  /// Converts this [Tag] to JSON
  Map<String, dynamic> toJson() => _$TagToJson(this);

  /// Get the full tag reference (category:name)
  String get fullName => category != null ? '$category:$name' : name;
}
