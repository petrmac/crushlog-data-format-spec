# Changelog

## 1.2.0

### Added
- `Stats` class to Manifest for export statistics (matching schema and Java implementation)
  - Includes counts for: climbs, sessions, locations, routes, sectors, tags, media
- `DateRange` class for ExportConfig to support date-filtered exports
- `source` field to Manifest for tracking the source application
- `website` field to Author class (previously Creator)
- Comprehensive Platform enum tests with validation

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