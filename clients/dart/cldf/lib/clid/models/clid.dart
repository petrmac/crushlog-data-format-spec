import '../clid_generator.dart';

/// Represents a parsed CLID (CrushLog ID)
class CLID {
  final String namespace;
  final EntityType type;
  final String uuid;
  final String fullId;
  final String shortForm;
  final String url;
  
  const CLID({
    required this.namespace,
    required this.type,
    required this.uuid,
    required this.fullId,
    required this.shortForm,
    required this.url,
  });
  
  @override
  String toString() => fullId;
  
  Map<String, dynamic> toJson() => {
    'namespace': namespace,
    'type': type.value,
    'uuid': uuid,
    'fullId': fullId,
    'shortForm': shortForm,
    'url': url,
  };
  
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is CLID &&
          runtimeType == other.runtimeType &&
          fullId == other.fullId;
  
  @override
  int get hashCode => fullId.hashCode;
}