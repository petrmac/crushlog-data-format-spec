# Changelog

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