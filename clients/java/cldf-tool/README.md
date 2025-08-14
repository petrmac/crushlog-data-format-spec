# CLDF Tool

A command-line interface (CLI) tool for creating, validating, and manipulating CrushLog Data Format (CLDF) archives. Built with Micronaut, Picocli, and GraalVM native image for fast startup and minimal resource usage.

## Features

- **Create** - Create CLDF archives from various sources (JSON, CSV, templates, interactive)
- **Validate** - Validate CLDF archives against schemas and business rules
- **Extract** - Extract contents from CLDF archives
- **Merge** - Merge multiple CLDF archives with conflict resolution
- **Convert** - Convert CLDF to other formats (CSV, GPX, TCX)

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