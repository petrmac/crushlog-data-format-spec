/// Geographic coordinates for locations
class Coordinates {
  final double lat;
  final double lon;
  
  const Coordinates({
    required this.lat,
    required this.lon,
  });
  
  @override
  String toString() => '${lat.toStringAsFixed(6)},${lon.toStringAsFixed(6)}';
  
  Map<String, dynamic> toJson() => {
    'lat': lat,
    'lon': lon,
  };
  
  factory Coordinates.fromJson(Map<String, dynamic> json) {
    return Coordinates(
      lat: (json['lat'] as num).toDouble(),
      lon: (json['lon'] as num).toDouble(),
    );
  }
  
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is Coordinates &&
          runtimeType == other.runtimeType &&
          lat == other.lat &&
          lon == other.lon;
  
  @override
  int get hashCode => lat.hashCode ^ lon.hashCode;
}