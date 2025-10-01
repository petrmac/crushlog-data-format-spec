# Changelog

## 1.4.1

### Fixed
- **Java Schema Compatibility** - Fixed manifest serialization to comply with Java schema validation
  - `creationDate` now includes timezone suffix (RFC 3339 format) - converts to UTC with 'Z' suffix
  - Removed `description` field from manifest (not defined in schema)
  - Null fields (`author`, `exportOptions`, etc.) are now omitted from JSON instead of being included as `null`
  - Added `includeIfNull: false` to all `@JsonSerializable` annotations in manifest models

### Changed
- Manifest `creationDate` field now always serializes to UTC timezone format
- Test expectations updated to compare UTC timestamps for consistency

## 1.4.0

### Changed
- **CLID Generation Alignment** - Aligned with Java implementation for consistency
  - Removed `RouteType` enum from CLID route model (simplified to name, grade, firstAscent, height)
  - Route type no longer affects CLID generation - routes are uniquely identified by location + name + grade
  - Updated `CLIDGenerator` to exclude route type from deterministic components
  - Simplified route validation by removing type-specific checks
  - Updated `CLDFClidAdapter` to work without route type mapping
  
### Improved
- Better separation between CLID generation models and main data models
- Reduced coupling between route metadata and CLID generation
- Improved maintainability through model simplification

## 1.3.1

### Fixed
- **Code Quality Improvements** - Resolved SonarCloud analysis issues
  - Fixed grade systems to match schema specification - removed unsupported systems
  - Now only supports the 5 official grade systems: vScale, font, french, yds, uiaa

### Changed
- Refactored QR generation code for better maintainability
- Improved date parsing with clearer method separation

## 1.3.0

### Added
- **CLID Support** - Initial implementation of Crushlog IDs (CLIDs)
  - CLIDs use versioned format `clid:v1:type:uuid` for unique entity identification
  - New `CLIDGenerator` class for generating v1 CLIDs
  - Full CLID validation and parsing support
  - QR code generation and cross-platform QR code scanning between Java and Dart implementations


## 1.2.3

### Added
- **Unified Media Model** - Complete overhaul of media support across all entities
  - Media can now be attached to routes, locations, and sectors (in addition to climbs)
  - New `MediaDesignation` enum for semantic categorization of media purpose:
    - `topo` - Route diagrams and maps
    - `beta` - How-to information and technique videos
    - `approach` - Access and trail information
    - `log` - Climb documentation and send photos
    - `overview` - General views and panoramas
    - `conditions` - Current state and conditions
    - `gear` - Equipment and protection information
    - `descent` - Down-climb or rappel information
    - `other` - Unspecified purpose (default)
  - New fields in `MediaItem`:
    - `designation` - Purpose/type of media content
    - `caption` - User-provided description
    - `timestamp` - When the media was created or taken
  - Added `external` to `MediaSource` enum for external URLs (YouTube, etc.)
  - Enhanced metadata structure with coordinate support

### Changed
- Renamed `FlexibleMediaItem` to `MediaItem` for consistency with Java implementation
- Renamed old `MediaItem` to `MediaMetadataItem` to clarify its use for standalone media files
- Media model is now consistent across Java and Dart implementations

### Enhanced
- Route, Location, and Sector models now include optional `media` field
- Schema validation updated to support media on all entity types
- Comprehensive test coverage for new media functionality

## 1.2.2

### Added
- Comprehensive logging for import and export operations
  - Detailed progress tracking during CLDF archive reading
  - Error logging with specific failure reasons for each parsing step
  - Export operation logging with file sizes and checksums
  - Summary statistics on successful import/export
  - Fine-grained logging levels for debugging
- Round-trip integration tests to ensure data integrity
  - Tests for minimal archives with required data only
  - Tests for complete archives with all optional data
  - Tests for media file handling
  - Tests for custom field preservation
  - Tests for empty collection handling
- Support for date-time strings in date fields
  - `FlexibleLocalDateConverter` now accepts ISO date-time strings (e.g., "2024-01-29T12:00:00Z")
  - Extracts the date part from date-time strings for fields that require date-only values
  - Maintains compatibility with all existing date formats

## 1.2.1

### Fixed
- Session model now uses strongly typed enums instead of strings
  - `climbType` now uses `ClimbType` enum
  - `rockType` now uses `RockType` enum
  - `terrainType` now uses `TerrainType` enum

## 1.2.0

### Added
- `Stats` class to Manifest for export statistics (matching schema and Java implementation)
  - Includes counts for: climbs, sessions, locations, routes, sectors, tags, media
- `DateRange` class for ExportConfig to support date-filtered exports
- `source` field to Manifest for tracking the source application
- `website` field to Author class (previously Creator)
- Comprehensive Platform enum tests with validation

### Enhanced
- CLDFWriter now automatically calculates stats when writing archives if stats are not already provided
  - Counts for: climbs, sessions, locations, routes, sectors, tags, media
  - Preserves existing stats if already present in the manifest

### Changed
- Renamed `Creator` class to `Author` to match schema and Java implementation
- `ExportConfig` now includes `dateRange` field for date-filtered exports
- `exportConfig` field in Manifest now maps to JSON property `exportOptions` to match schema

### Fixed
- Platform enum values updated to match schema specification exactly
  - Changed from: `desktop`, `mobile`, `web`, `api`  
  - Changed to: `iOS`, `Android`, `Web`, `Desktop`
- All references to `Platform.mobile` updated to `Platform.iOS` in tests and examples

### Breaking Changes
- **Major version bump due to breaking changes in Platform enum and Creator/Author rename**
- `Creator` class renamed to `Author` - update all references
- `Platform.mobile` has been removed - use `Platform.iOS` or `Platform.android` instead
- `Platform.api` has been removed - not part of the schema specification
- Author class no longer has `userId` field (not in schema)

## 1.1.1

### Added
- Example demonstrating flexible media model usage
- Tests for enum helper methods (FinishType validation)
- Tests for all model classes including Manifest, Route, and Session

### Fixed
- Flutter static analysis warnings in test files
- Test coverage improved from 86.3% to 96.3%

## 1.1.0

### Added
- Flexible media model support with new `Media` and `FlexibleMediaItem` classes
- `MediaSource` enum extended with: `local`, `cloud`, `reference` values
- `RouteCharacteristics` enum for route protection types (trad/bolted)
- New fields in models:
  - `Tag`: Added required `isPredefined` field
  - `Location`: Added `starred` (bool) and `createdAt` (DateTime?) fields
  - `Sector`: Added `isDefault` (bool), `createdAt` (DateTime?), and `approach` (String?) fields
  - `Route`: Added `routeCharacteristics` (RouteCharacteristics?) and `gearNotes` (String?) fields
  - `Session`: Added `isOngoing` (bool) field with default value of false
- Comprehensive test coverage for all model updates

### Changed
- `MediaItem.id` changed from String to int for consistency
- `Tag.category` changed from required to optional
- `Location.country` changed from required to optional
- `ProtectionRating` enum updated to match schema values: bombproof, good, adequate, runout, serious, x
- `RockType` enum updates:
  - Changed `tuff` to `volcanicTuff`
  - Added: dolomite, slate, gabbro, andesite, chalk
- `SessionType` enum added: multiPitch, boardSession
- `MediaSource` enum maintains backward compatibility with legacy values

### Fixed
- Schema synchronization issues between Dart and Java implementations
- Type consistency across all CLDF components

## 1.0.0

- Initial release
- Full support for CLDF 1.0.0 specification
- Read and write CLDF archives
- JSON serialization/deserialization for all models
- Type-safe Dart models
- Archive creation with automatic checksums