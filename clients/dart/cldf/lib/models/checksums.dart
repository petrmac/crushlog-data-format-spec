import 'package:json_annotation/json_annotation.dart';

part 'checksums.g.dart';

/// Checksums for archive files
@JsonSerializable()
class Checksums {
  /// Creates a new [Checksums] instance
  Checksums({required this.algorithm, this.files});

  /// Creates a [Checksums] from JSON
  factory Checksums.fromJson(Map<String, dynamic> json) =>
      _$ChecksumsFromJson(json);

  /// Algorithm used (e.g., SHA-256)
  final String algorithm;

  /// Checksums for each file
  final Map<String, String>? files;

  /// Converts this [Checksums] to JSON
  Map<String, dynamic> toJson() => _$ChecksumsToJson(this);
}
