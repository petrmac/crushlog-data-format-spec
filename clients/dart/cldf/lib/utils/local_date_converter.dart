import 'package:json_annotation/json_annotation.dart';
import 'package:intl/intl.dart';

/// Custom converter for LocalDate fields that handles multiple date formats
class FlexibleLocalDateConverter implements JsonConverter<String?, String?> {
  const FlexibleLocalDateConverter();

  static final List<DateFormat> _dateFormats = [
    DateFormat('yyyy-MM-dd'), // ISO format: 2024-01-29
    DateFormat('yyyy/MM/dd'), // Slash format: 2024/01/29
    DateFormat('yyyy.MM.dd'), // Dot format: 2024.01.29
    DateFormat('MM/dd/yyyy'), // US format: 01/29/2024
    DateFormat('dd/MM/yyyy'), // European format: 29/01/2024
    DateFormat('dd.MM.yyyy'), // European dot format: 29.01.2024
    DateFormat('MM-dd-yyyy'), // US dash format: 01-29-2024
    DateFormat('dd-MM-yyyy'), // European dash format: 29-01-2024
  ];

  @override
  String? fromJson(String? json) {
    if (json == null || json.trim().isEmpty) {
      return null;
    }

    final trimmed = json.trim();

    // Try different date formats in order
    final isoResult = _tryIsoFormat(trimmed, json);
    if (isoResult != null) return isoResult;

    final dateTimeResult = _tryIsoDateTimeFormat(trimmed, json);
    if (dateTimeResult != null) return dateTimeResult;

    final compactResult = _tryCompactFormat(trimmed);
    if (compactResult != null) return compactResult;

    final multiFormatResult = _tryMultipleFormats(trimmed);
    if (multiFormatResult != null) return multiFormatResult;

    throw FormatException('Cannot parse date string: $json');
  }

  /// Try parsing as ISO date format (yyyy-MM-dd)
  String? _tryIsoFormat(String trimmed, String original) {
    if (!RegExp(r'^\d{4}-\d{2}-\d{2}$').hasMatch(trimmed)) {
      return null;
    }

    if (trimmed.isValidIsoDate) {
      return trimmed;
    }
    throw FormatException('Invalid date: $original');
  }

  /// Try parsing as ISO date-time format
  String? _tryIsoDateTimeFormat(String trimmed, String original) {
    if (!RegExp(r'^\d{4}-\d{2}-\d{2}T').hasMatch(trimmed)) {
      return null;
    }

    try {
      final datePart = trimmed.substring(0, 10);
      if (datePart.isValidIsoDate) {
        return datePart;
      }
      throw FormatException('Invalid date in date-time: $original');
    } catch (_) {
      throw FormatException('Invalid date-time format: $original');
    }
  }

  /// Try parsing as compact format (YYYYMMDD)
  String? _tryCompactFormat(String trimmed) {
    if (!RegExp(r'^\d{8}$').hasMatch(trimmed)) {
      return null;
    }

    try {
      final year = int.parse(trimmed.substring(0, 4));
      final month = int.parse(trimmed.substring(4, 6));
      final day = int.parse(trimmed.substring(6, 8));
      final date = DateTime(year, month, day);

      if (date.year == year && date.month == month && date.day == day) {
        return DateFormat('yyyy-MM-dd').format(date);
      }
    } catch (_) {
      // Return null to try other formats
    }
    return null;
  }

  /// Try parsing with multiple date formats
  String? _tryMultipleFormats(String trimmed) {
    for (final format in _dateFormats) {
      try {
        final date = format.parseStrict(trimmed);
        return DateFormat('yyyy-MM-dd').format(date);
      } catch (_) {
        // Try next format
      }
    }
    return null;
  }

  @override
  String? toJson(String? object) {
    // Always output in ISO format
    return object;
  }
}

/// Extension to validate date strings
extension DateStringValidation on String {
  /// Check if the string is a valid ISO date
  bool get isValidIsoDate {
    try {
      final parts = split('-');
      if (parts.length != 3) return false;

      final year = int.tryParse(parts[0]);
      final month = int.tryParse(parts[1]);
      final day = int.tryParse(parts[2]);

      if (year == null || month == null || day == null) return false;
      if (month < 1 || month > 12) return false;
      if (day < 1 || day > 31) return false;

      // More thorough date validation
      try {
        final date = DateTime(year, month, day);
        // Check if the date components match (handles invalid dates like Feb 30)
        return date.year == year && date.month == month && date.day == day;
      } catch (_) {
        return false;
      }
    } catch (_) {
      return false;
    }
  }
}
