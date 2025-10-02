import 'package:json_annotation/json_annotation.dart';

import 'enums/platform.dart';

part 'manifest.g.dart';

/// Metadata about the CLDF archive
@JsonSerializable(includeIfNull: false)
class Manifest {
  /// Creates a new [Manifest] instance
  Manifest({
    required this.version,
    this.format = 'CLDF',
    required this.creationDate,
    required this.platform,
    required this.appVersion,
    this.author,
    this.source,
    this.stats,
    this.exportOptions,
  });

  /// Creates a [Manifest] from JSON
  factory Manifest.fromJson(Map<String, dynamic> json) =>
      _$ManifestFromJson(json);

  /// CLDF specification version
  final String version;

  /// Format identifier (always "CLDF")
  final String format;

  /// When the archive was created
  @JsonKey(toJson: _dateTimeToJson, fromJson: _dateTimeFromJson)
  final DateTime creationDate;

  /// Platform that created the archive
  final Platform platform;

  /// Version of the app that created the archive
  final String appVersion;

  /// Author information
  final Author? author;

  /// Source application or system that created the export
  final String? source;

  /// Statistics about exported data
  final Stats? stats;

  /// Export configuration
  @JsonKey(name: 'exportOptions')
  final ExportConfig? exportOptions;

  /// Converts this [Manifest] to JSON
  Map<String, dynamic> toJson() => _$ManifestToJson(this);
}

/// Author information
@JsonSerializable(includeIfNull: false)
class Author {
  /// Creates a new [Author] instance
  Author({this.name, this.email, this.website});

  /// Creates an [Author] from JSON
  factory Author.fromJson(Map<String, dynamic> json) => _$AuthorFromJson(json);

  /// Name of the author
  final String? name;

  /// Email of the author
  final String? email;

  /// Website URL of the author
  final String? website;

  /// Converts this [Author] to JSON
  Map<String, dynamic> toJson() => _$AuthorToJson(this);
}

/// Export configuration
@JsonSerializable(includeIfNull: false)
class ExportConfig {
  /// Creates a new [ExportConfig] instance
  ExportConfig({
    this.includeMedia = false,
    this.mediaStrategy,
    this.dateRange,
  });

  /// Creates a [ExportConfig] from JSON
  factory ExportConfig.fromJson(Map<String, dynamic> json) =>
      _$ExportConfigFromJson(json);

  /// Whether to include media files
  final bool includeMedia;

  /// Media export strategy
  final String? mediaStrategy;

  /// Date range for filtered exports
  final DateRange? dateRange;

  /// Converts this [ExportConfig] to JSON
  Map<String, dynamic> toJson() => _$ExportConfigToJson(this);
}

/// Date range for filtered exports
@JsonSerializable()
class DateRange {
  /// Creates a new [DateRange] instance
  DateRange({required this.start, required this.end});

  /// Creates a [DateRange] from JSON
  factory DateRange.fromJson(Map<String, dynamic> json) =>
      _$DateRangeFromJson(json);

  /// Start date
  final String start;

  /// End date
  final String end;

  /// Converts this [DateRange] to JSON
  Map<String, dynamic> toJson() => _$DateRangeToJson(this);
}

/// Statistics about exported data
@JsonSerializable(includeIfNull: false)
class Stats {
  /// Creates a new [Stats] instance
  Stats({
    this.climbsCount,
    this.sessionsCount,
    this.locationsCount,
    this.routesCount,
    this.sectorsCount,
    this.tagsCount,
    this.mediaCount,
  });

  /// Creates a [Stats] from JSON
  factory Stats.fromJson(Map<String, dynamic> json) => _$StatsFromJson(json);

  /// Number of climbs in the export
  final int? climbsCount;

  /// Number of sessions in the export
  final int? sessionsCount;

  /// Number of locations in the export
  final int? locationsCount;

  /// Number of routes in the export
  final int? routesCount;

  /// Number of sectors in the export
  final int? sectorsCount;

  /// Number of tags in the export
  final int? tagsCount;

  /// Number of media items in the export
  final int? mediaCount;

  /// Converts this [Stats] to JSON
  Map<String, dynamic> toJson() => _$StatsToJson(this);
}

// Helper functions for DateTime serialization
String _dateTimeToJson(DateTime dateTime) => dateTime.toUtc().toIso8601String();
DateTime _dateTimeFromJson(String json) => DateTime.parse(json);
