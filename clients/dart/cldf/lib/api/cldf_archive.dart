import '../models/manifest.dart';
import '../models/location.dart';
import '../models/sector.dart';
import '../models/route.dart';
import '../models/climb.dart';
import '../models/session.dart';
import '../models/tag.dart';
import '../models/media_item.dart';
import '../models/checksums.dart';

/// Represents a complete CLDF archive
class CLDFArchive {

  /// Creates a new [CLDFArchive] instance
  CLDFArchive({
    required this.manifest,
    required this.locations,
    this.sectors,
    this.routes,
    this.climbs,
    this.sessions,
    this.tags,
    this.mediaItems,
    this.checksums,
  });
  /// Archive metadata
  final Manifest manifest;

  /// Locations
  final List<Location> locations;

  /// Sectors
  final List<Sector>? sectors;

  /// Routes
  final List<Route>? routes;

  /// Climbs
  final List<Climb>? climbs;

  /// Sessions
  final List<Session>? sessions;

  /// Tags
  final List<Tag>? tags;

  /// Media items
  final List<MediaItem>? mediaItems;

  /// Checksums for files
  final Checksums? checksums;

  /// Check if the archive has routes
  bool get hasRoutes => routes != null && routes!.isNotEmpty;

  /// Check if the archive has climbs
  bool get hasClimbs => climbs != null && climbs!.isNotEmpty;

  /// Check if the archive has sessions
  bool get hasSessions => sessions != null && sessions!.isNotEmpty;

  /// Check if the archive has sectors
  bool get hasSectors => sectors != null && sectors!.isNotEmpty;

  /// Check if the archive has tags
  bool get hasTags => tags != null && tags!.isNotEmpty;

  /// Check if the archive has media
  bool get hasMedia => mediaItems != null && mediaItems!.isNotEmpty;
}
