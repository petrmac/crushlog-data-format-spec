import 'package:json_annotation/json_annotation.dart';

import '../utils/local_date_converter.dart';
import 'enums/session_type.dart';

part 'session.g.dart';

// Helper functions for date conversion
String _dateFromJson(String? json) =>
    const FlexibleLocalDateConverter().fromJson(json) ?? '';
String? _dateToJson(String? date) =>
    const FlexibleLocalDateConverter().toJson(date);

/// A climbing session
@JsonSerializable()
class Session {
  /// Creates a new [Session] instance
  Session({
    required this.id,
    required this.date,
    this.startTime,
    this.endTime,
    this.location,
    this.locationId,
    this.isIndoor,
    this.climbType,
    this.sessionType,
    this.partners,
    this.weather,
    this.notes,
    this.rockType,
    this.terrainType,
    this.approachTime,
    this.departureTime,
    this.isOngoing = false,
    this.tags,
    this.customFields,
  });

  /// Creates a [Session] from JSON
  factory Session.fromJson(Map<String, dynamic> json) =>
      _$SessionFromJson(json);

  /// Unique identifier
  final int id;

  /// Date of the session (YYYY-MM-DD)
  @JsonKey(fromJson: _dateFromJson, toJson: _dateToJson)
  final String date;

  /// Start time (HH:MM:SS)
  final String? startTime;

  /// End time (HH:MM:SS)
  final String? endTime;

  /// Location name (when location is not in database)
  final String? location;

  /// Reference to location ID
  final int? locationId;

  /// Whether this is an indoor session
  final bool? isIndoor;

  /// Primary type of climbing
  final String? climbType;

  /// Session type
  final SessionType? sessionType;

  /// Climbing partners
  final List<String>? partners;

  /// Weather information
  final Weather? weather;

  /// Notes
  final String? notes;

  /// Rock type
  final String? rockType;

  /// Terrain type
  final String? terrainType;

  /// Approach time in minutes
  final int? approachTime;

  /// Departure time in minutes
  final int? departureTime;

  /// Whether the session is currently ongoing
  final bool isOngoing;

  /// Associated tags
  final List<String>? tags;

  /// Custom fields
  final Map<String, dynamic>? customFields;

  /// Converts this [Session] to JSON
  Map<String, dynamic> toJson() => _$SessionToJson(this);
}

/// Weather information
@JsonSerializable()
class Weather {
  /// Creates a new [Weather] instance
  Weather({this.conditions, this.temperature, this.humidity, this.wind});

  /// Creates a [Weather] from JSON
  factory Weather.fromJson(Map<String, dynamic> json) =>
      _$WeatherFromJson(json);

  /// Weather conditions description
  final String? conditions;

  /// Temperature in Celsius
  final double? temperature;

  /// Humidity percentage
  final double? humidity;

  /// Wind conditions
  final String? wind;

  /// Converts this [Weather] to JSON
  Map<String, dynamic> toJson() => _$WeatherToJson(this);
}
