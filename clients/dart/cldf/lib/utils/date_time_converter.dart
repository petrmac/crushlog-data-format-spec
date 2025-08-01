import 'package:json_annotation/json_annotation.dart';

/// Custom converter for DateTime fields that handles multiple formats
class FlexibleDateTimeConverter implements JsonConverter<DateTime?, String?> {
  const FlexibleDateTimeConverter();

  @override
  DateTime? fromJson(String? json) {
    if (json == null || json.trim().isEmpty) {
      return null;
    }

    final trimmed = json.trim();

    // Check if it looks like a datetime (must have T separator)
    if (!trimmed.contains('T')) {
      throw FormatException(
        'Cannot parse date string: $json - missing time component',
      );
    }

    // Try parsing as ISO 8601
    try {
      return DateTime.parse(trimmed);
    } catch (e) {
      throw FormatException('Cannot parse date string: $json');
    }
  }

  @override
  String? toJson(DateTime? object) {
    return object?.toUtc().toIso8601String();
  }
}
