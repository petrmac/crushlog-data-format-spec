import 'dart:convert';
import 'dart:io';

import 'package:archive/archive.dart';
import 'package:logging/logging.dart';

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

  final _logger = Logger('CLDFReader');

  /// Read a CLDF archive from a file
  Future<CLDFArchive> readFile(String filePath) async {
    _logger.info('Starting CLDF import from file: $filePath');

    final file = File(filePath);
    if (!await file.exists()) {
      _logger.severe('Import failed: File not found at $filePath');
      throw Exception('File not found: $filePath');
    }

    try {
      _logger.fine('Reading file bytes from $filePath');
      final bytes = await file.readAsBytes();
      _logger.fine('File size: ${bytes.length} bytes');
      return readBytes(bytes);
    } catch (e, stackTrace) {
      _logger.severe('Import failed: Error reading file', e, stackTrace);
      rethrow;
    }
  }

  /// Read a CLDF archive from bytes
  CLDFArchive readBytes(List<int> bytes) {
    _logger.info('Starting CLDF import from bytes (${bytes.length} bytes)');

    try {
      // Decode the archive
      _logger.fine('Decoding ZIP archive');
      final archive = ZipDecoder().decodeBytes(bytes);
      _logger.fine('ZIP archive decoded, ${archive.length} entries found');

      // Extract and parse files
      final files = <String, Map<String, dynamic>>{};
      final mediaFiles = <String, List<int>>{};

      for (final file in archive) {
        if (file.isFile) {
          if (file.name.endsWith('.json')) {
            try {
              _logger.finer('Parsing JSON file: ${file.name}');
              final content = utf8.decode(file.content);
              files[file.name] = json.decode(content);
              _logger.finer('Successfully parsed ${file.name}');
            } catch (e) {
              _logger.severe('Failed to parse JSON file: ${file.name}', e);
              throw Exception('Invalid JSON in ${file.name}: $e');
            }
          } else if (file.name.startsWith('media/')) {
            // Store media files as bytes
            mediaFiles[file.name] = file.content;
            _logger.finer(
              'Found media file: ${file.name} (${file.content.length} bytes)',
            );
          }
        }
      }

      _logger.fine(
        'Extracted ${files.length} JSON files and ${mediaFiles.length} media files',
      );

      // Validate required files
      if (!files.containsKey('manifest.json')) {
        _logger.severe('Import failed: Missing required file manifest.json');
        throw Exception('Missing required file: manifest.json');
      }
      if (!files.containsKey('locations.json')) {
        _logger.severe('Import failed: Missing required file locations.json');
        throw Exception('Missing required file: locations.json');
      }

      // Parse manifest
      _logger.fine('Parsing manifest');
      final manifest = Manifest.fromJson(files['manifest.json']!);
      _logger.info(
        'Archive info: version=${manifest.version}, format=${manifest.format}, '
        'platform=${manifest.platform}, appVersion=${manifest.appVersion}',
      );

      // Parse locations (required)
      _logger.fine('Parsing locations');
      final locationsData = files['locations.json']!;
      final locationsList = locationsData['locations'] as List?;
      if (locationsList == null || locationsList.isEmpty) {
        _logger.warning('No locations found in locations.json');
      }

      final locations = <Location>[];
      for (var i = 0; i < (locationsList?.length ?? 0); i++) {
        try {
          locations.add(Location.fromJson(locationsList![i]));
        } catch (e) {
          _logger.severe('Failed to parse location at index $i', e);
          throw Exception('Invalid location data at index $i: $e');
        }
      }
      _logger.fine('Parsed ${locations.length} locations');

      // Parse optional files
      List<Sector>? sectors;
      if (files.containsKey('sectors.json')) {
        _logger.fine('Parsing sectors');
        try {
          final sectorsData = files['sectors.json']!;
          final sectorsList = sectorsData['sectors'] as List?;
          sectors = <Sector>[];
          for (var i = 0; i < (sectorsList?.length ?? 0); i++) {
            try {
              sectors.add(Sector.fromJson(sectorsList![i]));
            } catch (e) {
              _logger.severe('Failed to parse sector at index $i', e);
              throw Exception('Invalid sector data at index $i: $e');
            }
          }
          _logger.fine('Parsed ${sectors.length} sectors');
        } catch (e) {
          _logger.severe('Failed to parse sectors.json', e);
          rethrow;
        }
      }

      // Parse routes
      final routes = _parseModelList(
        files,
        'routes.json',
        'routes',
        Route.fromJson,
      );

      // Parse climbs
      final climbs = _parseModelList(
        files,
        'climbs.json',
        'climbs',
        Climb.fromJson,
      );

      // Parse sessions
      final sessions = _parseModelList(
        files,
        'sessions.json',
        'sessions',
        Session.fromJson,
      );

      // Parse tags
      final tags = _parseModelList(files, 'tags.json', 'tags', Tag.fromJson);

      // Parse media items
      final mediaItems = _parseModelList(
        files,
        'media_metadata.json',
        'media',
        MediaItem.fromJson,
      );

      Checksums? checksums;
      if (files.containsKey('checksums.json')) {
        checksums = Checksums.fromJson(files['checksums.json']!);
      }

      final cldfArchive = CLDFArchive(
        manifest: manifest,
        locations: locations,
        sectors: sectors,
        routes: routes,
        climbs: climbs,
        sessions: sessions,
        tags: tags,
        mediaItems: mediaItems,
        mediaFiles: mediaFiles.isEmpty ? null : mediaFiles,
        checksums: checksums,
      );

      _logger.info('Import completed successfully. Summary:');
      _logger.info('  - Locations: ${locations.length}');
      _logger.info('  - Sectors: ${sectors?.length ?? 0}');
      _logger.info('  - Routes: ${routes?.length ?? 0}');
      _logger.info('  - Sessions: ${sessions?.length ?? 0}');
      _logger.info('  - Climbs: ${climbs?.length ?? 0}');
      _logger.info('  - Tags: ${tags?.length ?? 0}');
      _logger.info('  - Media items: ${mediaItems?.length ?? 0}');
      _logger.info('  - Media files: ${mediaFiles.length}');

      if (manifest.stats != null) {
        _logger.fine('Archive stats from manifest:');
        _logger.fine('  - climbsCount: ${manifest.stats!.climbsCount}');
        _logger.fine('  - sessionsCount: ${manifest.stats!.sessionsCount}');
        _logger.fine('  - locationsCount: ${manifest.stats!.locationsCount}');
        _logger.fine('  - routesCount: ${manifest.stats!.routesCount}');
        _logger.fine('  - sectorsCount: ${manifest.stats!.sectorsCount}');
        _logger.fine('  - tagsCount: ${manifest.stats!.tagsCount}');
        _logger.fine('  - mediaCount: ${manifest.stats!.mediaCount}');
      }

      return cldfArchive;
    } catch (e, stackTrace) {
      _logger.severe('Import failed with error', e, stackTrace);
      rethrow;
    }
  }

  /// Helper method to parse a list of models with error handling
  List<T>? _parseModelList<T>(
    Map<String, Map<String, dynamic>> files,
    String filename,
    String listKey,
    T Function(Map<String, dynamic>) fromJson,
  ) {
    if (!files.containsKey(filename)) {
      return null;
    }

    _logger.fine('Parsing $filename');
    try {
      final data = files[filename]!;
      final list = data[listKey] as List?;

      if (list == null || list.isEmpty) {
        _logger.fine('No items found in $filename');
        return [];
      }

      final results = <T>[];
      for (var i = 0; i < list.length; i++) {
        try {
          results.add(fromJson(list[i]));
        } catch (e) {
          _logger.severe('Failed to parse item at index $i in $filename', e);
          throw Exception('Invalid data at index $i in $filename: $e');
        }
      }

      _logger.fine('Parsed ${results.length} items from $filename');
      return results;
    } catch (e) {
      if (e is! Exception) {
        _logger.severe('Failed to parse $filename', e);
        throw Exception('Failed to parse $filename: $e');
      }
      rethrow;
    }
  }
}
