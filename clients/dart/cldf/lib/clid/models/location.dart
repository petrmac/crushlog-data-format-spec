import 'coordinates.dart';

/// Represents a climbing location for CLID generation
class Location {
  final String country;
  final String? state;
  final String? city;
  final String name;
  final Coordinates coordinates;
  final bool isIndoor;

  const Location({
    required this.country,
    this.state,
    this.city,
    required this.name,
    required this.coordinates,
    required this.isIndoor,
  });

  factory Location.outdoor({
    required String country,
    String? state,
    String? city,
    required String name,
    required double lat,
    required double lon,
  }) {
    return Location(
      country: country,
      state: state,
      city: city,
      name: name,
      coordinates: Coordinates(lat: lat, lon: lon),
      isIndoor: false,
    );
  }

  factory Location.indoor({
    required String country,
    String? state,
    String? city,
    required String name,
    required double lat,
    required double lon,
  }) {
    return Location(
      country: country,
      state: state,
      city: city,
      name: name,
      coordinates: Coordinates(lat: lat, lon: lon),
      isIndoor: true,
    );
  }

  Map<String, dynamic> toJson() => {
    'country': country,
    if (state != null) 'state': state,
    if (city != null) 'city': city,
    'name': name,
    'coordinates': coordinates.toJson(),
    'isIndoor': isIndoor,
  };

  factory Location.fromJson(Map<String, dynamic> json) {
    return Location(
      country: json['country'] as String,
      state: json['state'] as String?,
      city: json['city'] as String?,
      name: json['name'] as String,
      coordinates: Coordinates.fromJson(
        json['coordinates'] as Map<String, dynamic>,
      ),
      isIndoor: json['isIndoor'] as bool,
    );
  }
}
