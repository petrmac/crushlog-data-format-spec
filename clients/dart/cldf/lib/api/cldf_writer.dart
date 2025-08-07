import 'dart:convert';
import 'dart:io';

import 'package:archive/archive.dart';
import 'package:crypto/crypto.dart';
import 'package:logging/logging.dart';

import '../models/checksums.dart';
import '../models/manifest.dart';
import 'cldf_archive.dart';

/// Writes CLDF archives
class CLDFWriter {
  /// Creates a new [CLDFWriter] instance
  CLDFWriter();

  final _logger = Logger('CLDFWriter');

  /// Write a CLDF archive to a file
  Future<void> writeFile(String filePath, CLDFArchive archive) async {
    _logger.info('Starting CLDF export to file: $filePath');

    try {
      final bytes = await writeBytes(archive);
      final file = File(filePath);

      _logger.fine('Writing ${bytes.length} bytes to $filePath');
      await file.writeAsBytes(bytes);

      _logger.info('Export completed successfully to $filePath');
    } catch (e, stackTrace) {
      _logger.severe('Export failed: Error writing file', e, stackTrace);
      rethrow;
    }
  }

  /// Write a CLDF archive to bytes
  Future<List<int>> writeBytes(CLDFArchive archive) async {
    _logger.info('Starting CLDF export to bytes');
    _logger.fine(
      'Archive info: version=${archive.manifest.version}, '
      'format=${archive.manifest.format}, platform=${archive.manifest.platform}',
    );

    final zipArchive = Archive();
    final checksums = <String, String>{};

    try {
      // Calculate and set stats if not already present
      final manifestData = archive.manifest.toJson();
      if (archive.manifest.stats == null) {
        _logger.fine('Calculating archive statistics');
        manifestData['stats'] = _calculateStats(archive).toJson();
      }

      // Add manifest
      _logger.fine('Adding manifest.json');
      await _addJsonFile(zipArchive, 'manifest.json', manifestData, checksums);

      // Add locations (required)
      _logger.fine(
        'Adding locations.json with ${archive.locations.length} locations',
      );
      await _addJsonFile(zipArchive, 'locations.json', {
        'locations': archive.locations.map((l) => l.toJson()).toList(),
      }, checksums);

      // Add optional files
      if (archive.hasSectors) {
        _logger.fine(
          'Adding sectors.json with ${archive.sectors!.length} sectors',
        );
        await _addJsonFile(zipArchive, 'sectors.json', {
          'sectors': archive.sectors!.map((s) => s.toJson()).toList(),
        }, checksums);
      }

      if (archive.hasRoutes) {
        _logger.fine(
          'Adding routes.json with ${archive.routes!.length} routes',
        );
        await _addJsonFile(zipArchive, 'routes.json', {
          'routes': archive.routes!.map((r) => r.toJson()).toList(),
        }, checksums);
      }

      if (archive.hasClimbs) {
        _logger.fine(
          'Adding climbs.json with ${archive.climbs!.length} climbs',
        );
        await _addJsonFile(zipArchive, 'climbs.json', {
          'climbs': archive.climbs!.map((c) => c.toJson()).toList(),
        }, checksums);
      }

      if (archive.hasSessions) {
        _logger.fine(
          'Adding sessions.json with ${archive.sessions!.length} sessions',
        );
        await _addJsonFile(zipArchive, 'sessions.json', {
          'sessions': archive.sessions!.map((s) => s.toJson()).toList(),
        }, checksums);
      }

      if (archive.hasTags) {
        _logger.fine('Adding tags.json with ${archive.tags!.length} tags');
        await _addJsonFile(zipArchive, 'tags.json', {
          'tags': archive.tags!.map((t) => t.toJson()).toList(),
        }, checksums);
      }

      if (archive.hasMedia) {
        _logger.fine(
          'Adding media_metadata.json with ${archive.mediaItems!.length} media items',
        );
        await _addJsonFile(zipArchive, 'media_metadata.json', {
          'media': archive.mediaItems!.map((m) => m.toJson()).toList(),
        }, checksums);
      }

      // Add embedded media files
      if (archive.hasEmbeddedMedia) {
        _logger.fine(
          'Adding ${archive.mediaFiles!.length} embedded media files',
        );
        for (final entry in archive.mediaFiles!.entries) {
          final path = entry.key;
          final bytes = entry.value;

          _logger.finer('Adding media file: $path (${bytes.length} bytes)');

          // Calculate checksum
          final digest = sha256.convert(bytes);
          checksums[path] = digest.toString();

          // Add to archive
          zipArchive.addFile(ArchiveFile(path, bytes.length, bytes));
        }
      }

      // Add checksums file
      _logger.fine('Adding checksums.json');
      final checksumsData = Checksums(algorithm: 'SHA-256', files: checksums);

      final checksumsJson = json.encode(checksumsData.toJson());
      zipArchive.addFile(
        ArchiveFile(
          'checksums.json',
          checksumsJson.length,
          utf8.encode(checksumsJson),
        ),
      );

      // Encode to zip
      _logger.fine('Encoding ZIP archive');
      final encodedBytes = ZipEncoder().encode(zipArchive);

      _logger.info('Export completed successfully. Summary:');
      _logger.info('  - Archive size: ${encodedBytes.length} bytes');
      _logger.info('  - Locations: ${archive.locations.length}');
      _logger.info('  - Sectors: ${archive.sectors?.length ?? 0}');
      _logger.info('  - Routes: ${archive.routes?.length ?? 0}');
      _logger.info('  - Sessions: ${archive.sessions?.length ?? 0}');
      _logger.info('  - Climbs: ${archive.climbs?.length ?? 0}');
      _logger.info('  - Tags: ${archive.tags?.length ?? 0}');
      _logger.info('  - Media items: ${archive.mediaItems?.length ?? 0}');
      _logger.info('  - Media files: ${archive.mediaFiles?.length ?? 0}');

      return encodedBytes;
    } catch (e, stackTrace) {
      _logger.severe('Export failed with error', e, stackTrace);
      rethrow;
    }
  }

  /// Calculate statistics for the archive
  Stats _calculateStats(CLDFArchive archive) {
    return Stats(
      climbsCount: archive.climbs?.length ?? 0,
      sessionsCount: archive.sessions?.length ?? 0,
      locationsCount: archive.locations.length,
      routesCount: archive.routes?.length ?? 0,
      sectorsCount: archive.sectors?.length ?? 0,
      tagsCount: archive.tags?.length ?? 0,
      mediaCount: archive.mediaItems?.length ?? 0,
    );
  }

  /// Add a JSON file to the archive and calculate checksum
  Future<void> _addJsonFile(
    Archive archive,
    String filename,
    Map<String, dynamic> data,
    Map<String, String> checksums,
  ) async {
    try {
      final jsonString = const JsonEncoder.withIndent('  ').convert(data);
      final bytes = utf8.encode(jsonString);

      _logger.finer('Encoded $filename: ${bytes.length} bytes');

      // Calculate checksum
      final digest = sha256.convert(bytes);
      checksums[filename] = digest.toString();
      _logger.finer('Checksum for $filename: ${checksums[filename]}');

      // Add to archive
      archive.addFile(ArchiveFile(filename, bytes.length, bytes));
    } catch (e) {
      _logger.severe('Failed to add $filename to archive', e);
      throw Exception('Failed to add $filename to archive: $e');
    }
  }
}
