/// String utility functions for ID generation
class StringUtils {
  /// Normalize string for consistent ID generation
  static String normalize(String input) {
    return input
        .toLowerCase()
        .trim()
        .replaceAll(RegExp(r'\s+'), '-')
        .replaceAll(RegExp(r'[^\w\-]'), '')
        .replaceAll(RegExp(r'-+'), '-')
        .replaceAll(RegExp(r'^-|-$'), '');
  }
  
  /// Standardize grade format
  static String standardizeGrade(String grade) {
    return grade.replaceAll(RegExp(r'\s'), '').toLowerCase();
  }
}