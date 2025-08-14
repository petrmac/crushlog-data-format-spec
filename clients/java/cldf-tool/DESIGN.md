# CLDF Tool Design Document

## Overview

The CLDF Tool is a command-line interface (CLI) application built with Micronaut, Picocli, and GraalVM native image to create, validate, and manipulate CrushLog Data Format (CLDF) archives. This tool will be used in Model Context Protocol (MCP) environments to enable AI-assisted creation of CLDF files.

## Technology Stack

- **Micronaut 4.x** - Dependency injection and application framework
- **Picocli 4.x** - Command-line parsing and help generation
- **GraalVM Native Image** - Native executable generation for fast startup
- **Jackson** - JSON processing (reused from cldf-java)
- **Java 21** - Target JVM version

## Architecture

### Project Structure

```
cldf-tool/
├── build.gradle
├── gradle.properties
├── micronaut-cli.yml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── app/crushlog/cldf/tool/
│   │   │       ├── Application.java
│   │   │       ├── commands/
│   │   │       │   ├── CLDFCommand.java (main command)
│   │   │       │   ├── CreateCommand.java
│   │   │       │   ├── ValidateCommand.java
│   │   │       │   ├── ExtractCommand.java
│   │   │       │   ├── MergeCommand.java
│   │   │       │   └── ConvertCommand.java
│   │   │       ├── services/
│   │   │       │   ├── CLDFService.java
│   │   │       │   ├── ValidationService.java
│   │   │       │   └── ConversionService.java
│   │   │       ├── models/
│   │   │       │   └── CreateOptions.java
│   │   │       └── utils/
│   │   │           └── FileUtils.java
│   │   └── resources/
│   │       ├── application.yml
│   │       └── META-INF/
│   │           └── native-image/
│   │               └── app.crushlog.cldf/cldf-tool/
│   │                   ├── native-image.properties
│   │                   └── reflect-config.json
│   └── test/
│       └── java/
│           └── app/crushlog/cldf/tool/
│               └── commands/
│                   └── *CommandTest.java
└── README.md
```

### Command Structure

```
cldf [OPTIONS] COMMAND [ARGS]

Commands:
  create    Create a new CLDF archive
  validate  Validate a CLDF archive
  extract   Extract contents from a CLDF archive
  merge     Merge multiple CLDF archives
  convert   Convert between CLDF and other formats

Options:
  -h, --help      Show help message
  -V, --version   Show version information
  -v, --verbose   Enable verbose output
```

### Core Components

#### 1. Main Application Class

```java
@Command(name = "cldf", 
         description = "CLDF Tool for creating and manipulating climbing data archives",
         mixinStandardHelpOptions = true,
         version = "1.0.0",
         subcommands = {
             CreateCommand.class,
             ValidateCommand.class,
             ExtractCommand.class,
             MergeCommand.class,
             ConvertCommand.class
         })
public class CLDFCommand implements Runnable {
    @Override
    public void run() {
        // Show help when no subcommand is provided
    }
}
```

#### 2. Create Command

Creates new CLDF archives from various input sources:

```
cldf create [OPTIONS]

Options:
  -o, --output=<file>        Output CLDF file (required)
  -i, --input=<file>         Input JSON file or directory
  --from-json                Create from JSON input
  --from-csv                 Create from CSV input
  --interactive              Interactive mode
  --template=<type>          Use template (basic, demo, empty)
  --validate                 Validate after creation (default: true)
```

Features:
- Interactive mode for guided creation
- Template-based creation
- JSON/CSV import capabilities
- Automatic validation

#### 3. Validate Command

```
cldf validate [OPTIONS] <file>

Options:
  --schema                   Validate against JSON schemas
  --checksums               Verify file checksums
  --references              Check reference integrity
  --strict                  Enable all validations
  --report-format=<format>  Output format (text, json, xml)
```

#### 4. Extract Command

```
cldf extract [OPTIONS] <file>

Options:
  -o, --output=<directory>   Output directory
  --files=<files>           Specific files to extract
  --preserve-structure      Keep archive structure
  --pretty-print           Format JSON output
```

#### 5. Merge Command

```
cldf merge [OPTIONS] <files...>

Options:
  -o, --output=<file>       Output CLDF file
  --strategy=<strategy>     Merge strategy (append, dedupe, smart)
  --resolve-conflicts       Interactive conflict resolution
```

#### 6. Convert Command

```
cldf convert [OPTIONS] <file>

Options:
  -o, --output=<file>       Output file
  --format=<format>         Target format (json, csv, gpx, tcx)
  --filter=<expression>     Filter expression
```

### Services Architecture

#### CLDFService
- Wraps cldf-java library functionality
- Provides high-level operations
- Handles file I/O and compression

#### ValidationService
- Schema validation
- Checksum verification
- Reference integrity checks
- Custom business rule validation

#### ConversionService
- Format converters (CSV, GPX, TCX)
- Data transformers
- Filter/query engine

### Native Image Configuration

#### Reflection Configuration
- All CLDF model classes
- Picocli command classes
- Jackson type information

#### Resource Configuration
- JSON schemas
- Application properties
- Template files

#### Build-time Initialization
- Jackson ObjectMapper
- Schema validators
- Static resources

### MCP Integration

The tool will be designed for seamless MCP integration:

1. **Structured Output**: JSON output mode for all commands
2. **Exit Codes**: Consistent exit codes for automation
3. **Streaming**: Support for stdin/stdout operations
4. **Minimal Dependencies**: Fast startup time
5. **Error Handling**: Machine-readable error messages

### Example Workflows

#### 1. AI-Assisted Creation
```bash
# Create from structured input
echo '{"locations": [...], "climbs": [...]}' | cldf create --from-json -o output.cldf

# Interactive creation with prompts
cldf create --interactive -o my-climbs.cldf
```

#### 2. Validation Pipeline
```bash
# Validate and output JSON report
cldf validate input.cldf --strict --report-format=json
```

#### 3. Data Transformation
```bash
# Convert and filter
cldf convert input.cldf --format=csv --filter="type=boulder AND rating>=4" -o boulders.csv
```

### Performance Targets

- **Startup Time**: < 50ms (native image)
- **Memory Usage**: < 50MB for typical operations
- **Processing Speed**: > 1000 climbs/second

### Testing Strategy

1. **Unit Tests**: Service layer testing
2. **Integration Tests**: Command execution tests
3. **Native Image Tests**: GraalVM-specific testing
4. **Performance Tests**: Benchmark suite

### Build Configuration

```gradle
plugins {
    id "io.micronaut.application" version "4.3.5"
    id "io.micronaut.graalvm" version "4.3.5"
}

dependencies {
    implementation("io.micronaut:micronaut-runtime")
    implementation("io.micronaut.picocli:micronaut-picocli")
    implementation("info.picocli:picocli")
    implementation("app.crushlog.cldf:cldf-java:1.0.0")
    
    annotationProcessor("io.micronaut:micronaut-inject-java")
    annotationProcessor("info.picocli:picocli-codegen")
}

graalvmNative {
    binaries {
        main {
            imageName = "cldf"
            buildArgs.add("--no-fallback")
            buildArgs.add("--enable-http")
            buildArgs.add("--enable-https")
        }
    }
}
```

### Future Enhancements

1. **Plugin System**: Extensible command architecture
2. **Cloud Integration**: S3/GCS support
3. **Database Export**: Direct database connections
4. **Web Service**: REST API mode
5. **Watch Mode**: File system monitoring

### Security Considerations

1. **Input Validation**: Sanitize all user inputs
2. **File Access**: Restrict file system access
3. **Memory Limits**: Prevent OOM attacks
4. **Checksum Verification**: Ensure data integrity

### Deployment

1. **Native Binaries**: Pre-built for major platforms
   - Linux (x64, ARM64)
   - macOS (x64, ARM64)
   - Windows (x64)

2. **Distribution**:
   - GitHub Releases
   - Homebrew (macOS)
   - Package managers (apt, yum)
   - Docker images

3. **Documentation**:
   - Man pages
   - Built-in help
   - Online documentation
   - MCP integration guide