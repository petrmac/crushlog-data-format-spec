import 'dart:convert';
import 'dart:io';

import 'package:archive/archive.dart';

import '../models/checksums.dart';
import '../models/climb.dart';
import '../models/location.dart';
import '../models/manifest.dart';
import '../models/media_item.dart';
import '../models/route.dart';
import '../models/sector.dart';
import '../models/session.dart';
import '../models/tag.dart';
import 'cldf_archive.dart';

/// Reads CLDF archives
class CLDFReader {
  /// Creates a new [CLDFReader] instance
  CLDFReader();
  /// Read a CLDF archive from a file
  Future<CLDFArchive> readFile(String filePath) async {
    final file = File(filePath);
    if (!await file.exists()) {
      throw Exception('File not found: $filePath');
    }

    final bytes = await file.readAsBytes();
    return readBytes(bytes);
  }

  /// Read a CLDF archive from bytes
  CLDFArchive readBytes(List<int> bytes) {
    // Decode the archive
    final archive = ZipDecoder().decodeBytes(bytes);

    // Extract and parse files
    final files = <String, Map<String, dynamic>>{};

    for (final file in archive) {
      if (file.isFile && file.name.endsWith('.json')) {
        final content = utf8.decode(file.content);
        files[file.name] = json.decode(content);
      }
    }

    // Validate required files
    if (!files.containsKey('manifest.json')) {
      throw Exception('Missing required file: manifest.json');
    }
    if (!files.containsKey('locations.json')) {
      throw Exception('Missing required file: locations.json');
    }

    // Parse manifest
    final manifest = Manifest.fromJson(files['manifest.json']!);

    // Parse locations (required)
    final locationsData = files['locations.json']!;
    final locations = (locationsData['locations'] as List)
        .map((json) => Location.fromJson(json))
        .toList();

    // Parse optional files
    List<Sector>? sectors;
    if (files.containsKey('sectors.json')) {
      final sectorsData = files['sectors.json']!;
      sectors = (sectorsData['sectors'] as List?)
          ?.map((json) => Sector.fromJson(json))
          .toList();
    }

    List<Route>? routes;
    if (files.containsKey('routes.json')) {
      final routesData = files['routes.json']!;
      routes = (routesData['routes'] as List?)
          ?.map((json) => Route.fromJson(json))
          .toList();
    }

    List<Climb>? climbs;
    if (files.containsKey('climbs.json')) {
      final climbsData = files['climbs.json']!;
      climbs = (climbsData['climbs'] as List?)
          ?.map((json) => Climb.fromJson(json))
          .toList();
    }

    List<Session>? sessions;
    if (files.containsKey('sessions.json')) {
      final sessionsData = files['sessions.json']!;
      sessions = (sessionsData['sessions'] as List?)
          ?.map((json) => Session.fromJson(json))
          .toList();
    }

    List<Tag>? tags;
    if (files.containsKey('tags.json')) {
      final tagsData = files['tags.json']!;
      tags = (tagsData['tags'] as List?)
          ?.map((json) => Tag.fromJson(json))
          .toList();
    }

    List<MediaItem>? mediaItems;
    if (files.containsKey('media_metadata.json')) {
      final mediaData = files['media_metadata.json']!;
      mediaItems = (mediaData['media'] as List?)
          ?.map((json) => MediaItem.fromJson(json))
          .toList();
    }

    Checksums? checksums;
    if (files.containsKey('checksums.json')) {
      checksums = Checksums.fromJson(files['checksums.json']!);
    }

    return CLDFArchive(
      manifest: manifest,
      locations: locations,
      sectors: sectors,
      routes: routes,
      climbs: climbs,
      sessions: sessions,
      tags: tags,
      mediaItems: mediaItems,
      checksums: checksums,
    );
  }
}
