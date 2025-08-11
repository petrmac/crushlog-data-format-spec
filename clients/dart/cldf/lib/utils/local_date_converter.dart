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

    // If already in ISO format, validate and return as-is
    if (RegExp(r'^\d{4}-\d{2}-\d{2}$').hasMatch(trimmed)) {
      if (trimmed.isValidIsoDate) {
        return trimmed;
      }
      throw FormatException('Invalid date: $json');
    }

    // Handle ISO date-time format (extract date part)
    if (RegExp(r'^\d{4}-\d{2}-\d{2}T').hasMatch(trimmed)) {
      try {
        // Extract just the date part from the string
        final datePart = trimmed.substring(0, 10);
        if (datePart.isValidIsoDate) {
          return datePart;
        }
        throw FormatException('Invalid date in date-time: $json');
      } catch (_) {
        throw FormatException('Invalid date-time format: $json');
      }
    }

    // Handle compact format (YYYYMMDD) specially
    if (RegExp(r'^\d{8}$').hasMatch(trimmed)) {
      try {
        final year = int.parse(trimmed.substring(0, 4));
        final month = int.parse(trimmed.substring(4, 6));
        final day = int.parse(trimmed.substring(6, 8));
        final date = DateTime(year, month, day);
        if (date.year == year && date.month == month && date.day == day) {
          return DateFormat('yyyy-MM-dd').format(date);
        }
      } catch (_) {
        // Fall through to error
      }
    }

    // Try each format
    for (final format in _dateFormats) {
      try {
        final date = format.parseStrict(trimmed);
        // Always return in ISO format
        return DateFormat('yyyy-MM-dd').format(date);
      } catch (_) {
        // Try next format
      }
    }

    throw FormatException('Cannot parse date string: $json');
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
