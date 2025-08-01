import 'package:json_annotation/json_annotation.dart';

part 'tag.g.dart';

/// A tag for categorization
@JsonSerializable()
class Tag {

  /// Creates a new [Tag] instance
  Tag({
    required this.id,
    required this.category,
    required this.name,
    this.description,
    this.color,
  });

  /// Creates a [Tag] from JSON
  factory Tag.fromJson(Map<String, dynamic> json) => _$TagFromJson(json);
  /// Unique identifier
  final int id;

  /// Tag category/key
  final String category;

  /// Tag name/value
  final String name;

  /// Description
  final String? description;

  /// Associated color
  final String? color;

  /// Converts this [Tag] to JSON
  Map<String, dynamic> toJson() => _$TagToJson(this);

  /// Get the full tag reference (category:name)
  String get fullName => '$category:$name';
}
