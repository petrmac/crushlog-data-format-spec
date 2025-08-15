# CLDF Tool

A command-line interface (CLI) tool for creating, validating, and manipulating CrushLog Data Format (CLDF) archives. Built with Micronaut, Picocli, and GraalVM native image for fast startup and minimal resource usage.

## Features

- **Create** - Create CLDF archives from various sources (JSON, CSV, templates, interactive)
- **Validate** - Validate CLDF archives against schemas and business rules
- **Extract** - Extract contents from CLDF archives
- **Merge** - Merge multiple CLDF archives with conflict resolution
- **Convert** - Convert CLDF to other formats (CSV, GPX, TCX)
- **QR Code** - Generate QR codes for routes/locations directly or from archives, scan and parse QR codes

## Installation

### From Source

```bash
# Clone the repository
git clone https://github.com/cldf/cldf-java.git
cd cldf-java/cldf-tool

# Build native executable
./gradlew nativeCompile

# The executable will be at build/native/nativeCompile/cldf
```

### Pre-built Binaries

Download pre-built binaries from the [releases page](https://github.com/cldf/cldf-java/releases).

## Usage

```bash
cldf [OPTIONS] COMMAND [ARGS]
```

### Commands

#### Create

Create a new CLDF archive:

```bash
# Create from template
cldf create --template demo -o my-climbs.cldf

# Create interactively
cldf create --interactive -o my-climbs.cldf

# Create from JSON
cldf create --from-json -i data.json -o output.cldf

# Create from directory of JSON files
cldf create --from-json -i ./json-data/ -o output.cldf
```

#### Validate

Validate a CLDF archive:

```bash
# Basic validation
cldf validate my-climbs.cldf

# Strict validation with all checks
cldf validate --strict my-climbs.cldf

# Output JSON report
cldf validate --report-format json my-climbs.cldf > report.json
```

#### Extract

Extract contents from a CLDF archive:

```bash
# Extract all files
cldf extract my-climbs.cldf -o extracted/

# Extract specific files
cldf extract my-climbs.cldf --files "climbs.json,sessions.json" -o extracted/

# Extract without pretty printing
cldf extract my-climbs.cldf --pretty-print false -o extracted/
```

#### Merge

Merge multiple CLDF archives:

```bash
# Simple append merge
cldf merge file1.cldf file2.cldf file3.cldf -o merged.cldf

# Merge with deduplication
cldf merge --strategy dedupe *.cldf -o merged.cldf

# Smart merge with conflict resolution
cldf merge --strategy smart --resolve-conflicts *.cldf -o merged.cldf
```

#### Convert

Convert CLDF to other formats:

```bash
# Convert to CSV
cldf convert my-climbs.cldf --format csv -o climbs.csv

# Convert to CSV with filter
cldf convert my-climbs.cldf --format csv --filter "type=boulder AND rating>=4" -o hard-boulders.csv

# Convert to GPX (locations as waypoints)
cldf convert my-climbs.cldf --format gpx -o locations.gpx

# Convert to JSON (pretty printed)
cldf convert my-climbs.cldf --format json -o climbs.json
```

#### QR Code

Generate, scan, and parse QR codes for routes and locations:

##### Generate QR Code

Three modes are available for QR code generation:

**1. Archive Mode** - Generate from existing CLDF archive:
```bash
# Generate QR for a route from archive
cldf qr generate my-climbs.cldf clid:v1:route:550e8400... -o route-qr.png

# Generate as SVG with custom size
cldf qr generate my-climbs.cldf clid:v1:location:123abc... -o location-qr.svg --size 512
```

**2. Direct Mode with Auto-Generated CLID** - Generate without archive:
```bash
# Generate QR for route (minimal)
cldf qr generate --type route --name "Midnight Lightning" --grade "V8" \
  --location-clid clid:v1:location:abc123... -o qr.png

# Generate QR for route with full details
cldf qr generate --type route --name "The Nose" --grade "5.14a" \
  --route-type sport --height 900 \
  --location-clid clid:v1:location:abc123... \
  --first-ascent-name "Lynn Hill" --first-ascent-year 1993 \
  -o nose-qr.png

# Generate QR for location
cldf qr generate --type location --name "Yosemite Valley" \
  --country "USA" --state "California" --city "Yosemite" \
  --latitude 37.7456 --longitude -119.5936 \
  -o yosemite-qr.png
```

**3. Direct Mode with Explicit CLID** - Provide your own CLID:
```bash
# Generate with specific CLID
cldf qr generate --clid clid:v1:route:custom-uuid-here \
  --type route --name "Dawn Wall" --grade "5.14d" \
  -o dawn-wall-qr.png
```

Options:
- `-o, --output` - Output file path (PNG or SVG)
- `-s, --size` - QR code size in pixels (default: 256)
- `--base-url` - Base URL for QR links (default: https://crushlog.pro)
- `--include-ipfs` - Include IPFS hash if available
- `--ipfs-hash` - Specific IPFS hash to include

##### Scan QR Code

Extract data from QR code images:

```bash
# Scan QR code from image
cldf qr scan qr-code.png

# Output as JSON
cldf qr scan qr-code.png -o json

# Extract route data
cldf qr scan qr-code.png --extract-route

# Extract location data
cldf qr scan qr-code.png --extract-location
```

##### Parse QR Code

Parse QR code data from text:

```bash
# Parse QR data string
cldf qr parse "cldf://route/550e8400..."

# Parse from file
cldf qr parse qr-data.txt

# Extract entities
cldf qr parse qr-data.txt --extract-route --extract-location
```

## Templates

The create command supports several built-in templates:

- **empty** - Minimal valid CLDF archive
- **basic** - Simple climbing session with a few climbs
- **demo** - Comprehensive example with multiple locations, routes, and sessions

## Filter Expressions

The convert command supports simple filter expressions:

- `type=boulder` - Only boulder problems
- `type=route` - Only routes
- `rating>=4` - Climbs rated 4 or higher
- `attempts<=3` - Climbs done in 3 attempts or less

Combine with AND:
- `type=boulder AND rating>=4` - Hard boulder problems

## MCP Integration

The tool is designed for Model Context Protocol (MCP) integration:

```bash
# Create from structured input
echo '{"locations": [...], "climbs": [...]}' | cldf create --from-json -o output.cldf

# Validate and output JSON report
cldf validate input.cldf --report-format json --quiet

# Convert with filters
cldf convert input.cldf --format csv --filter "rating>=4" -o filtered.csv
```

## Performance

- **Startup time**: < 50ms (native image)
- **Memory usage**: < 50MB typical
- **Processing speed**: > 1000 climbs/second

## Building

### Requirements

- Java 21+
- GraalVM 21+ (for native image)
- Gradle 8+

### Build Commands

```bash
# Build JAR
./gradlew build

# Build native executable
./gradlew nativeCompile

# Run tests
./gradlew test

# Build all (including parent project)
cd .. && ./gradlew build
```

### Native Image Tips

If you encounter issues building the native image:

1. Ensure GraalVM is installed and `JAVA_HOME` points to it
2. Install native-image component: `gu install native-image`
3. Check reflection configuration in `src/main/resources/META-INF/native-image/`

## Development

### Project Structure

```
cldf-tool/
├── src/main/java/app/crushlog/cldf/tool/
│   ├── Application.java           # Main entry point
│   ├── commands/                  # CLI commands
│   ├── services/                  # Business logic
│   └── utils/                     # Utilities
└── src/main/resources/
    ├── application.yml            # Micronaut config
    └── META-INF/native-image/     # GraalVM config
```

### Adding New Commands

1. Create a new command class in `commands/` package
2. Implement `Runnable` interface
3. Add `@Command` annotation
4. Register in `Application.java` subcommands

### Testing

```bash
# Run all tests
./gradlew test

# Run with coverage
./gradlew test jacocoTestReport
```

## License

MIT License - see LICENSE file for details