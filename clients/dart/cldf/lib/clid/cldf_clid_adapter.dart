import '../models/route.dart' as cldf;
import '../models/location.dart' as cldf;
import 'clid_generator.dart';
import 'models/location.dart' as clid;
import 'models/route.dart' as clid;
import 'models/coordinates.dart';

/// Adapter to use CLIDGenerator with main CLDF models
class CLDFClidAdapter {
  /// Generate CLID for a CLDF Location
  static String generateLocationCLID(cldf.Location location) {
    // Adapt CLDF Location to CLID Location
    final clidLocation = clid.Location(
      country: location.country ?? 'XX',
      state: location.state,
      city: location.city,
      name: location.name,
      coordinates: Coordinates(
        lat: location.coordinates?.latitude ?? 0.0,
        lon: location.coordinates?.longitude ?? 0.0,
      ),
      isIndoor: location.isIndoor ?? false,
    );

    return CLIDGenerator.generateLocationCLID(clidLocation);
  }

  /// Generate CLID for a CLDF Route with its location
  static String generateRouteCLID(cldf.Route route, cldf.Location location) {
    // First get or generate location CLID
    final locationCLID = location.clid ?? generateLocationCLID(location);

    // Extract primary grade
    String grade = '';
    if (route.grades != null) {
      grade =
          route.grades!['vScale'] ??
          route.grades!['yds'] ??
          route.grades!['french'] ??
          route.grades!['font'] ??
          route.grades!['uiaa'] ??
          '';
    }

    // Map route type
    clid.RouteType routeType;
    switch (route.routeType.value) {
      case 'boulder':
        routeType = clid.RouteType.boulder;
        break;
      case 'route':
      default:
        routeType = clid.RouteType.sport; // Default to sport for 'route' type
    }

    // Adapt CLDF Route to CLID Route
    final clidRoute = clid.Route(
      name: route.name,
      grade: grade,
      type: routeType,
      firstAscent: null, // FirstAscent info not available in CLDF Route model
      height: route.height,
    );

    return CLIDGenerator.generateRouteCLID(locationCLID, clidRoute);
  }

  /// Generate CLID for a route without location (uses random UUID v4)
  static String generateStandaloneRouteCLID(cldf.Route route) {
    // When no location is available, use the existing CLID or generate a random one
    if (route.clid != null) {
      return route.clid!;
    }

    // Fallback to random UUID when no deterministic data available
    return CLIDGenerator.generateRandomCLID(EntityType.route);
  }
}
