/// Validation result for ID generation
class ValidationResult {
  final List<String> errors;
  final List<String> warnings;

  const ValidationResult({this.errors = const [], this.warnings = const []});

  bool get isValid => errors.isEmpty;

  @override
  String toString() {
    if (isValid) {
      return warnings.isEmpty
          ? 'Valid'
          : 'Valid with warnings: ${warnings.join(', ')}';
    }
    return 'Invalid: ${errors.join(', ')}';
  }
}
