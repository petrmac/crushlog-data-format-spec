# Changelog

All notable changes to the CLDF Java client library and CLI tool will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.6] - 2026-01-18

### Added
- **Route CLID in Climb** - Climb model now includes `routeClid` field for globally unique route identification during server synchronization
- **QR Code Support for Routes** - Route model now includes nested `QrCode` class for physical route marking with fields:
  - `data` - Generated QR code data string
  - `url` - Public URL for the route
  - `ipfsHash` - IPFS hash for CLDF archive reference
  - `blockchainTx` - Blockchain transaction hash for permanent record
  - `generatedAt` - Timestamp when the QR code was generated

## [1.0.5] - 2026-01-12

### Added
- **Location CLID in Session** - Session model now includes `locationClid` field for referencing the location where the session took place
- **GitHub Sources & Javadoc** - Added automated publishing of sources and Javadoc to GitHub

### Fixed
- **Boolean Field Marshalling** - Fixed boolean field serialization (`isRepeat`, `isIndoor`) for consistent JSON output
- **Schema Adherence** - Improved marshalling to strictly follow CLDF schema specification
- **CLID Generation** - Removed route type from CLID generation for simplified route identification

### Changed
- Code formatting improvements across all models

## [1.0.4] - 2025-01-11

### Added
- **Unified Media Model** - Complete overhaul of media support across all entities
  - Media can now be attached to Route, Location, and Sector entities (in addition to Climb)
  - New `MediaDesignation` enum for semantic categorization:
    - TOPO - Route diagrams and maps
    - BETA - How-to information and technique videos
    - APPROACH - Access and trail information
    - LOG - Climb documentation and send photos
    - OVERVIEW - General views and panoramas
    - CONDITIONS - Current state and conditions
    - GEAR - Equipment and protection information
    - DESCENT - Down-climb or rappel information
    - OTHER - Unspecified purpose (default)
  - Enhanced MediaItem with new fields:
    - `designation` - Purpose/type of media content
    - `caption` - User-provided description
    - `timestamp` - When the media was created or taken
  - Added EXTERNAL to MediaSource enum for external URLs

### Changed
- Route, Location, and Sector models now include optional `media` field
- MediaSource enum enhanced with EXTERNAL value

## [Unreleased]

### Added
- Initial release of CLDF Java client library
- Core data models for all CLDF entities (Climb, Session, Location, etc.)
- ZIP archive support for reading/writing CLDF files
- JSON serialization/deserialization with Jackson
- Schema validation against JSON schemas
- Comprehensive builder pattern for all models
- Full test coverage with Spock framework

### CLI Tool Features
- `create` - Create CLDF archives from JSON, CSV, or templates
- `validate` - Validate CLDF archives with detailed reports
- `extract` - Extract contents from CLDF archives
- `merge` - Merge multiple CLDF archives
- `convert` - Convert CLDF to CSV, GPX, or JSON formats
- GraalVM native image support for fast startup
- Interactive mode for creating archives
- Template system with demo data

### Technical Details
- Java 21+ required
- Micronaut framework for CLI
- Picocli for command parsing
- GraalVM native image compatible
- Maven Central publishing ready
- Multi-platform support (Linux, macOS, Windows)

## [1.0.0] - TBD

### Added
- First stable release
- Complete implementation of CLDF specification v1.0
- Production-ready API
- Comprehensive documentation
- Example applications

### Changed
- N/A (first release)

### Deprecated
- N/A (first release)

### Removed
- N/A (first release)

### Fixed
- N/A (first release)

### Security
- N/A (first release)

---

## Release Template

<!--
## [X.Y.Z] - YYYY-MM-DD

### Added
- New features or capabilities

### Changed
- Changes in existing functionality
- API changes (breaking changes in MAJOR version)

### Deprecated
- Soon-to-be removed features

### Removed
- Removed features or capabilities

### Fixed
- Bug fixes

### Security
- Security vulnerability fixes
-->

## Version History

- `1.0.0` - Initial stable release (planned)
- `0.9.0` - Beta release (planned)
- `0.1.0` - Alpha release (planned)

[Unreleased]: https://github.com/cldf/cldf-java/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/cldf/cldf-java/releases/tag/v1.0.0