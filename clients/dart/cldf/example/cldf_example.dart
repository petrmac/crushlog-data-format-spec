import 'package:cldf/cldf.dart';

void main() async {
  // Create a sample CLDF archive
  final archive = CLDFArchive(
    manifest: Manifest(
      version: '1.0.0',
      creationDate: DateTime.now(),
      platform: Platform.iOS,
      appVersion: '1.0.0',
      description: 'Sample climbing data export',
      author: Author(name: 'John Climber', email: 'john@example.com'),
    ),
    locations: [
      Location(
        id: 1,
        name: 'Sample Crag',
        country: 'USA',
        isIndoor: false,
        state: 'Colorado',
        city: 'Boulder',
        coordinates: Coordinates(latitude: 40.0150, longitude: -105.2705),
        rockType: RockType.granite,
        terrainType: TerrainType.natural,
      ),
    ],
    routes: [
      Route(
        id: 1,
        locationId: 1,
        name: 'Classic Line',
        routeType: RouteType.route,
        grades: {'french': '6a', 'yds': '5.10a'},
        height: 20,
        bolts: 8,
        qualityRating: 4,
        tags: ['classic', 'well-protected'],
      ),
      Route(
        id: 2,
        locationId: 1,
        name: 'Power Problem',
        routeType: RouteType.boulder,
        grades: {'vScale': 'V4', 'font': '6A'},
        color: '#FF0000',
        tags: ['crimpy', 'overhang'],
      ),
    ],
    climbs: [
      Climb(
        id: 1,
        date: '2024-01-15',
        routeId: 1,
        type: ClimbType.route,
        finishType: FinishType.redpoint,
        attempts: 3,
        belayType: BelayType.lead,
        rating: 4,
        notes: 'Great climb, tricky crux in the middle',
        grades: GradeInfo(
          system: GradeSystem.french,
          grade: '6a',
          conversions: {'yds': '5.10a'},
        ),
      ),
    ],
    sessions: [
      Session(
        id: 1,
        date: '2024-01-15',
        locationId: 1,
        sessionType: SessionType.sportClimbing,
        partners: ['Jane Doe', 'Bob Smith'],
        weather: Weather(conditions: 'Sunny', temperature: 18, humidity: 45),
        notes: 'Perfect conditions for climbing',
      ),
    ],
    tags: [
      Tag(
        id: 1,
        category: 'style',
        isPredefined: false,
        name: 'crimpy',
        description: 'Routes with small crimpy holds',
      ),
      Tag(
        id: 2,
        category: 'style',
        isPredefined: false,
        name: 'overhang',
        description: 'Overhanging routes',
      ),
    ],
  );

  // Write to file
  final writer = CLDFWriter();
  await writer.writeFile('sample.cldf', archive);
  print('Archive written to sample.cldf');

  // Read back the file
  final reader = CLDFReader();
  final loadedArchive = await reader.readFile('sample.cldf');

  print('Loaded archive:');
  print('- Locations: ${loadedArchive.locations.length}');
  print('- Routes: ${loadedArchive.routes?.length ?? 0}');
  print('- Climbs: ${loadedArchive.climbs?.length ?? 0}');
  print('- Sessions: ${loadedArchive.sessions?.length ?? 0}');
  print('- Tags: ${loadedArchive.tags?.length ?? 0}');
}
