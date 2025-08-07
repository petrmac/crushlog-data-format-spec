# CLDF - Crushlog Data Format for Dart

[![Dart CI](https://github.com/petrmac/crushlog-data-format-spec/actions/workflows/dart-ci.yml/badge.svg)](https://github.com/petrmac/crushlog-data-format-spec/actions/workflows/dart-ci.yml)
[![pub package](https://img.shields.io/pub/v/cldf.svg)](https://pub.dev/packages/cldf)
[![Coverage Status](https://sonarcloud.io/api/project_badges/measure?project=petrmac_crushlog-data-format-spec-dart&metric=coverage)](https://sonarcloud.io/summary/new_code?id=petrmac_crushlog-data-format-spec-dart)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=petrmac_crushlog-data-format-spec-dart&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=petrmac_crushlog-data-format-spec-dart)

A Dart implementation of the Crushlog Data Format (CLDF) for climbing data exchange.

## Features

- Read and write CLDF archives (.cldf files)
- JSON serialization/deserialization for all CLDF models
- Type-safe Dart models for all CLDF entities
- Archive creation with automatic checksums
- Automatic statistics calculation when writing archives
- Validation support
- Media file handling (embedded or referenced)

## Platform Support

This package supports all platforms except Web/WASM because CLDF archives require file system access for reading and writing .cldf files. The package works on:
- ✅ Android
- ✅ iOS  
- ✅ macOS
- ✅ Windows
- ✅ Linux
- ❌ Web (file I/O not available)

## Installation

Add this to your package's `pubspec.yaml` file:

```yaml
dependencies:
  cldf: ^1.2.1
```

## Usage

### Reading a CLDF archive

```dart
import 'package:cldf/cldf.dart';

final reader = CLDFReader();
final archive = await reader.readFile('path/to/file.cldf');

// Access data
print('Locations: ${archive.locations.length}');
print('Routes: ${archive.routes.length}');
print('Climbs: ${archive.climbs.length}');
```

### Writing a CLDF archive

```dart
import 'package:cldf/cldf.dart';

final archive = CLDFArchive(
  manifest: Manifest(
    version: '1.0.0',
    format: 'CLDF',
    creationDate: DateTime.now(),
    platform: Platform.iOS,
    appVersion: '1.0.0',
  ),
  locations: [
    Location(
      id: 1,
      name: 'Test Crag',
      country: 'USA',
      isIndoor: false,
    ),
  ],
  routes: [
    Route(
      id: 1,
      locationId: 1,
      name: 'Classic Route',
      routeType: RouteType.route,
      grades: {'french': '6a'},
    ),
  ],
  climbs: [
    Climb(
      id: 1,
      date: '2024-01-15',
      routeName: 'Classic Route',
      type: ClimbType.route,
      finishType: FinishType.redpoint,
      attempts: 2,
    ),
  ],
);

final writer = CLDFWriter();
await writer.writeFile('output.cldf', archive);
```

### Automatic Statistics

When writing an archive, statistics are automatically calculated if not already present in the manifest:

```dart
// Stats will be calculated automatically
final archive = CLDFArchive(
  manifest: Manifest(
    version: '1.0.0',
    format: 'CLDF',
    creationDate: DateTime.now(),
    platform: Platform.android,
    appVersion: '1.0.0',
    // No stats provided - will be calculated automatically
  ),
  locations: locations,
  climbs: climbs,
  // ... other data
);

await writer.writeFile('output.cldf', archive);
// The written archive will have stats populated with counts of all entities
```

### Working with JSON

All models support JSON serialization:

```dart
// Convert to JSON
final climbJson = climb.toJson();

// Parse from JSON
final climb = Climb.fromJson(jsonData);
```

## Models

The package provides models for all CLDF entities:

- `Manifest` - Archive metadata with automatic statistics calculation
- `Location` - Climbing locations
- `Sector` - Sectors within locations
- `Route` - Routes and boulders
- `Climb` - Individual climb records
- `Session` - Climbing sessions
- `Tag` - Tags for categorization
- `MediaItem` - Media references (photos/videos)
- `Author` - Author information for exports
- `Stats` - Statistics about the archive content

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the LICENSE file for details.