# Flutter Integration Guide

This guide explains how to integrate the CLDF Dart package into your Flutter app.

## Installation

### Option 1: From pub.dev (once published)

Add to your `pubspec.yaml`:

```yaml
dependencies:
  cldf: ^1.0.0
```

### Option 2: From local path (for development)

Add to your `pubspec.yaml`:

```yaml
dependencies:
  cldf:
    path: /Users/petrmacek/git-mirrors/crushlog-data-format-spec/clients/dart/cldf
```

### Option 3: From Git

Add to your `pubspec.yaml`:

```yaml
dependencies:
  cldf:
    git:
      url: https://github.com/crushlog/crushlog-data-format-spec.git
      path: clients/dart/cldf
```

## Migration Guide

The CLDF package provides standardized models that match the CLDF specification. To integrate with your existing Flutter app:

### 1. Update Your Export Service

Replace your current serialization with CLDF models:

```dart
import 'package:cldf/cldf.dart' as cldf;

// Convert your Climb model to CLDF Climb
cldf.Climb toCldfClimb(Climb climb) {
  return cldf.Climb(
    id: climb.id,
    sessionId: climb.sessionId,
    routeId: climb.routeId,
    date: climb.date,
    routeName: climb.routeName,
    type: climb.type == ClimbType.boulder 
        ? cldf.ClimbType.boulder 
        : cldf.ClimbType.route,
    finishType: _mapFinishType(climb.finishType),
    attempts: climb.attempts,
    // ... map other fields
  );
}

// Create CLDF archive
Future<File> exportToCldf(ExportOptions options) async {
  final cldfArchive = cldf.CLDFArchive(
    manifest: cldf.Manifest(
      version: '1.0.0',
      format: 'CLDF',
      creationDate: DateTime.now(),
      platform: cldf.Platform.mobile,
      appVersion: BuildInfo.version,
    ),
    locations: locations.map(toCldfLocation).toList(),
    routes: routes?.map(toCldfRoute).toList(),
    climbs: climbs?.map(toCldfClimb).toList(),
    sessions: sessions?.map(toCldfSession).toList(),
    tags: tags?.map(toCldfTag).toList(),
  );
  
  final writer = cldf.CLDFWriter();
  final outputPath = '${tempDir.path}/export.cldf';
  await writer.writeFile(outputPath, cldfArchive);
  
  return File(outputPath);
}
```

### 2. Update Your Import Service

Use CLDF reader to import archives:

```dart
import 'package:cldf/cldf.dart' as cldf;

Future<void> importCldfArchive(String filePath) async {
  final reader = cldf.CLDFReader();
  final archive = await reader.readFile(filePath);
  
  // Convert CLDF models to your app models
  for (final cldfLocation in archive.locations) {
    final location = fromCldfLocation(cldfLocation);
    await _locationRepository.create(location);
  }
  
  // Import routes if present
  if (archive.hasRoutes) {
    for (final cldfRoute in archive.routes!) {
      final route = fromCldfRoute(cldfRoute);
      await _routeRepository.create(route);
    }
  }
  
  // ... import other entities
}

// Convert CLDF Location to your Location model
Location fromCldfLocation(cldf.Location cldfLocation) {
  return Location(
    id: cldfLocation.id,
    name: cldfLocation.name,
    country: cldfLocation.country,
    isIndoor: cldfLocation.isIndoor ? 1 : 0,
    // ... map other fields
  );
}
```

## Publishing to pub.dev

To publish the package:

1. Ensure you have a pub.dev account
2. Run tests: `dart test`
3. Check package score: `dart pub publish --dry-run`
4. Publish: `dart pub publish`

## Benefits of Using CLDF Package

1. **Type Safety**: All models are strongly typed with proper enums
2. **Validation**: Built-in JSON serialization with validation
3. **Standards Compliance**: Ensures your exports follow the CLDF specification
4. **Interoperability**: Your exports will work with any CLDF-compatible tool
5. **Maintenance**: Updates to the CLDF spec will be reflected in package updates

## Example: Complete Export Flow

```dart
class ExportService {
  final cldf.CLDFWriter _writer = cldf.CLDFWriter();
  
  Future<File> exportUserData({
    required List<Location> locations,
    required List<Route> routes,
    required List<Climb> climbs,
    required String outputPath,
  }) async {
    // Create CLDF archive
    final archive = cldf.CLDFArchive(
      manifest: _createManifest(),
      locations: locations.map(_toCldfLocation).toList(),
      routes: routes.map(_toCldfRoute).toList(),
      climbs: climbs.map(_toCldfClimb).toList(),
    );
    
    // Write to file
    await _writer.writeFile(outputPath, archive);
    
    return File(outputPath);
  }
  
  cldf.Manifest _createManifest() {
    return cldf.Manifest(
      version: '1.0.0',
      format: 'CLDF',
      creationDate: DateTime.now(),
      platform: cldf.Platform.mobile,
      appVersion: '1.0.0',
      description: 'Crushlog climbing data export',
      creator: cldf.Creator(
        name: 'Crushlog App',
      ),
    );
  }
}
```