import 'package:json_annotation/json_annotation.dart';

import '../utils/date_time_converter.dart';
import 'enums/platform.dart';

part 'manifest.g.dart';

/// Metadata about the CLDF archive
@JsonSerializable()
class Manifest {

  /// Creates a new [Manifest] instance
  Manifest({
    required this.version,
    this.format = 'CLDF',
    required this.creationDate,
    required this.platform,
    required this.appVersion,
    this.description,
    this.creator,
    this.exportConfig,
  });

  /// Creates a [Manifest] from JSON
  factory Manifest.fromJson(Map<String, dynamic> json) =>
      _$ManifestFromJson(json);
  /// CLDF specification version
  final String version;

  /// Format identifier (always "CLDF")
  final String format;

  /// When the archive was created
  @FlexibleDateTimeConverter()
  final DateTime creationDate;

  /// Platform that created the archive
  final Platform platform;

  /// Version of the app that created the archive
  final String appVersion;

  /// Optional description
  final String? description;

  /// Creator information
  final Creator? creator;

  /// Export configuration
  final ExportConfig? exportConfig;

  /// Converts this [Manifest] to JSON
  Map<String, dynamic> toJson() => _$ManifestToJson(this);
}

/// Creator information
@JsonSerializable()
class Creator {

  /// Creates a new [Creator] instance
  Creator({
    this.name,
    this.email,
    this.userId,
  });

  /// Creates a [Creator] from JSON
  factory Creator.fromJson(Map<String, dynamic> json) =>
      _$CreatorFromJson(json);
  /// Name of the creator
  final String? name;
  
  /// Email of the creator
  final String? email;
  
  /// User ID of the creator
  final String? userId;

  /// Converts this [Creator] to JSON
  Map<String, dynamic> toJson() => _$CreatorToJson(this);
}

/// Export configuration
@JsonSerializable()
class ExportConfig {

  /// Creates a new [ExportConfig] instance
  ExportConfig({
    this.includeMedia = false,
    this.mediaStrategy,
    this.mediaQuality,
    this.anonymized,
  });

  /// Creates a [ExportConfig] from JSON
  factory ExportConfig.fromJson(Map<String, dynamic> json) =>
      _$ExportConfigFromJson(json);
  /// Whether to include media files
  final bool includeMedia;
  
  /// Media export strategy
  final String? mediaStrategy;
  
  /// Media quality (0-100)
  final int? mediaQuality;
  
  /// Whether data is anonymized
  final bool? anonymized;

  /// Converts this [ExportConfig] to JSON
  Map<String, dynamic> toJson() => _$ExportConfigToJson(this);
}
