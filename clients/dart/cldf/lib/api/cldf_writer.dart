import 'dart:convert';
import 'dart:io';

import 'package:archive/archive.dart';
import 'package:crypto/crypto.dart';

import '../models/checksums.dart';
import 'cldf_archive.dart';

/// Writes CLDF archives
class CLDFWriter {
  /// Creates a new [CLDFWriter] instance
  CLDFWriter();
  /// Write a CLDF archive to a file
  Future<void> writeFile(String filePath, CLDFArchive archive) async {
    final bytes = await writeBytes(archive);
    final file = File(filePath);
    await file.writeAsBytes(bytes);
  }

  /// Write a CLDF archive to bytes
  Future<List<int>> writeBytes(CLDFArchive archive) async {
    final zipArchive = Archive();
    final checksums = <String, String>{};

    // Add manifest
    await _addJsonFile(
      zipArchive,
      'manifest.json',
      archive.manifest.toJson(),
      checksums,
    );

    // Add locations (required)
    await _addJsonFile(
      zipArchive,
      'locations.json',
      {
        'locations': archive.locations.map((l) => l.toJson()).toList(),
      },
      checksums,
    );

    // Add optional files
    if (archive.hasSectors) {
      await _addJsonFile(
        zipArchive,
        'sectors.json',
        {
          'sectors': archive.sectors!.map((s) => s.toJson()).toList(),
        },
        checksums,
      );
    }

    if (archive.hasRoutes) {
      await _addJsonFile(
        zipArchive,
        'routes.json',
        {
          'routes': archive.routes!.map((r) => r.toJson()).toList(),
        },
        checksums,
      );
    }

    if (archive.hasClimbs) {
      await _addJsonFile(
        zipArchive,
        'climbs.json',
        {
          'climbs': archive.climbs!.map((c) => c.toJson()).toList(),
        },
        checksums,
      );
    }

    if (archive.hasSessions) {
      await _addJsonFile(
        zipArchive,
        'sessions.json',
        {
          'sessions': archive.sessions!.map((s) => s.toJson()).toList(),
        },
        checksums,
      );
    }

    if (archive.hasTags) {
      await _addJsonFile(
        zipArchive,
        'tags.json',
        {
          'tags': archive.tags!.map((t) => t.toJson()).toList(),
        },
        checksums,
      );
    }

    if (archive.hasMedia) {
      await _addJsonFile(
        zipArchive,
        'media_metadata.json',
        {
          'media': archive.mediaItems!.map((m) => m.toJson()).toList(),
        },
        checksums,
      );
    }

    // Add checksums file
    final checksumsData = Checksums(
      algorithm: 'SHA-256',
      files: checksums,
    );

    final checksumsJson = json.encode(checksumsData.toJson());
    zipArchive.addFile(ArchiveFile(
      'checksums.json',
      checksumsJson.length,
      utf8.encode(checksumsJson),
    ));

    // Encode to zip
    return ZipEncoder().encode(zipArchive)!;
  }

  /// Add a JSON file to the archive and calculate checksum
  Future<void> _addJsonFile(
    Archive archive,
    String filename,
    Map<String, dynamic> data,
    Map<String, String> checksums,
  ) async {
    final jsonString = const JsonEncoder.withIndent('  ').convert(data);
    final bytes = utf8.encode(jsonString);

    // Calculate checksum
    final digest = sha256.convert(bytes);
    checksums[filename] = digest.toString();

    // Add to archive
    archive.addFile(ArchiveFile(
      filename,
      bytes.length,
      bytes,
    ));
  }
}
