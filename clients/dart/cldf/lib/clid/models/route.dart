/// Represents a climbing route for CLID generation
class Route {
  final String name;
  final String grade;
  final RouteType type;
  final FirstAscent? firstAscent;
  final double? height;
  
  const Route({
    required this.name,
    required this.grade,
    required this.type,
    this.firstAscent,
    this.height,
  });
  
  Map<String, dynamic> toJson() => {
    'name': name,
    'grade': grade,
    'type': type.value,
    if (firstAscent != null) 'firstAscent': firstAscent!.toJson(),
    if (height != null) 'height': height,
  };
  
  factory Route.fromJson(Map<String, dynamic> json) {
    return Route(
      name: json['name'] as String,
      grade: json['grade'] as String,
      type: RouteType.fromString(json['type'] as String),
      firstAscent: json['firstAscent'] != null 
          ? FirstAscent.fromJson(json['firstAscent'] as Map<String, dynamic>)
          : null,
      height: json['height'] as double?,
    );
  }
}

/// Route types
enum RouteType {
  sport('sport'),
  trad('trad'),
  boulder('boulder'),
  ice('ice'),
  mixed('mixed');
  
  final String value;
  const RouteType(this.value);
  
  static RouteType fromString(String value) {
    return RouteType.values.firstWhere(
      (type) => type.value == value,
      orElse: () => throw ArgumentError('Unknown route type: $value'),
    );
  }
}

/// First ascent information
class FirstAscent {
  final int? year;
  final String? name;
  
  const FirstAscent({
    this.year,
    this.name,
  });
  
  Map<String, dynamic> toJson() => {
    if (year != null) 'year': year,
    if (name != null) 'name': name,
  };
  
  factory FirstAscent.fromJson(Map<String, dynamic> json) {
    return FirstAscent(
      year: json['year'] as int?,
      name: json['name'] as String?,
    );
  }
}