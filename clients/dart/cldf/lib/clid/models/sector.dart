/// Represents a sector within a climbing location
class Sector {
  final String name;
  final int? order;
  
  const Sector({
    required this.name,
    this.order,
  });
  
  Map<String, dynamic> toJson() => {
    'name': name,
    if (order != null) 'order': order,
  };
  
  factory Sector.fromJson(Map<String, dynamic> json) {
    return Sector(
      name: json['name'] as String,
      order: json['order'] as int?,
    );
  }
}