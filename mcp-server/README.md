# CLDF MCP Server

A Model Context Protocol (MCP) server for CLDF (Climbing Log Data Format) tools. This server provides MCP endpoints for creating, validating, querying, and manipulating CLDF archives.

## Architecture

This MCP server is built with NestJS and provides a structured, testable interface to the CLDF command-line tools. It uses:

- **NestJS** - For dependency injection and modular architecture
- **TypeScript** - For type safety
- **Jest** - For unit and e2e testing
- **MCP SDK** - For Model Context Protocol integration

## Prerequisites

- Node.js 18+
- GraalVM 21+ (for building the native binary)
- The CLDF tool native binary built and available

## Installation

1. Build the CLDF tool native binary:
```bash
cd ../clients/java
./gradlew :cldf-tool:nativeCompile
```

2. Install dependencies:
```bash
cd ../../mcp-server
npm install
```

3. Build the MCP server:
```bash
npm run build
```

## Configuration

1. The server will automatically use the `cldf` command if it's in your PATH. You can override this by setting the `CLDF_CLI` environment variable:
```bash
export CLDF_CLI="/absolute/path/to/cldf"
```

2. Add the server to your MCP client configuration (e.g., Claude Desktop):
```json
{
  "mcpServers": {
    "cldf-tools": {
      "command": "node",
      "args": ["/absolute/path/to/mcp-server/dist/src/main.js"],
      "env": {
        "CLDF_CLI": "/absolute/path/to/cldf"
      }
    }
  }
}
```

On macOS, the Claude Desktop config is located at:
`~/Library/Application Support/Claude/claude_desktop_config.json`

## Available Tools

### cldf_schema_info
Get CLDF data structure schema and validation rules to help understand the expected format.

**Parameters:**
- `component`: Specific component schema to retrieve (optional)
  - Options: `all`, `manifest`, `location`, `route`, `sector`, `climb`, `session`, `tag`, `dateFormats`, `enums`, `commonMistakes`, `exampleData`
  - Default: `all`

**Example:**
```javascript
{
  "component": "location"
}
```

### cldf_validate_data
Validate CLDF data structure before creating an archive. Helps identify issues early.

**Parameters:**
- `data`: JSON data to validate against CLDF schema (required)

**Example:**
```javascript
{
  "data": {
    "manifest": {
      "version": "1.0.0",
      "format": "CLDF"
    },
    "locations": [{
      "id": 1,
      "name": "Boulder Canyon",
      "isIndoor": false
    }]
  }
}
```

### cldf_create
Create a new CLDF archive with climbing data.

**Parameters:**
- `template`: Template to use (`basic`, `demo`, `empty`) - required
- `outputPath`: Path where the archive will be saved - required
- `data`: Optional JSON data to include in the archive
  - Must match CLDF schema - use `cldf_schema_info` to understand structure
  - Minimum required: `manifest`, `locations`
  - Sessions and climbs are optional

**Example:**
```javascript
{
  "template": "basic",
  "outputPath": "my-climbs.cldf",
  "data": {
    "manifest": {
      "version": "1.0.0",
      "format": "CLDF",
      "platform": "Desktop",
      "appVersion": "1.0.0",
      "creationDate": "2024-01-01T00:00:00Z"
    },
    "locations": [{
      "id": 1,
      "name": "Boulder Canyon",
      "country": "USA",
      "city": "Boulder",
      "address": "Canyon Dr",
      "isIndoor": false
    }],
    "routes": [{
      "id": 1,
      "locationId": 1,
      "name": "The Sphinx",
      "routeType": "route",
      "grades": {
        "yds": "5.13a"
      }
    }]
  }
}
```

**Notes:**
- Archives can contain just locations and routes (sessions/climbs optional)
- Location model now supports `city` and `address` fields (both optional)
- Route grades must match pattern (e.g., French: 5c, 6a+, 7b)
- Colors must be hex format (#RRGGBB)

### cldf_validate
Validate a CLDF archive file.

**Parameters:**
- `filePath`: Path to the CLDF archive to validate
- `strict`: Use strict validation (optional, default: false)

**Example:**
```javascript
{
  "filePath": "my-climbs.cldf",
  "strict": true
}
```

### cldf_query
Query data from a CLDF archive.

**Parameters:**
- `filePath`: Path to the CLDF archive
- `dataType`: Type of data to query (locations, climbs, sessions, all)
- `filter`: Optional filter expression

**Example:**
```javascript
{
  "filePath": "my-climbs.cldf",
  "dataType": "climbs",
  "filter": "grade >= '5.10'"
}
```

### cldf_merge
Merge multiple CLDF archives.

**Parameters:**
- `files`: List of CLDF archive paths to merge
- `outputPath`: Path for the merged archive
- `strategy`: Merge strategy (replace, merge, append)

**Example:**
```javascript
{
  "files": ["climbs1.cldf", "climbs2.cldf"],
  "outputPath": "merged.cldf",
  "strategy": "merge"
}
```

### cldf_convert
Convert CLDF archive to different formats.

**Parameters:**
- `filePath`: Path to the CLDF archive
- `format`: Output format (json, csv, yaml)
- `outputPath`: Path for the converted file

**Example:**
```javascript
{
  "filePath": "my-climbs.cldf",
  "format": "json",
  "outputPath": "my-climbs.json"
}
```

### cldf_extract
Extract specific data from a CLDF archive.

**Parameters:**
- `filePath`: Path to the CLDF archive
- `dataType`: Type of data to extract (manifest, locations, climbs, sessions, media)
- `outputDir`: Directory to extract data to (optional)

**Example:**
```javascript
{
  "filePath": "my-climbs.cldf",
  "dataType": "climbs",
  "outputDir": "./extracted"
}
```

## Development

Run in development mode:
```bash
npm run start:dev
```

Run tests:
```bash
# Unit tests
npm test

# Unit tests with coverage
npm run test:cov

# E2E tests
npm run test:e2e

# Watch mode
npm run test:watch
```

Lint and format:
```bash
npm run lint
npm run format
```

## Logging

The MCP server includes comprehensive logging that outputs to stderr (stdout is reserved for MCP protocol communication).

### Log Levels

- **Production mode** (`NODE_ENV=production`): Only errors and warnings
- **Development mode** (`NODE_ENV=development`): All log levels including debug and verbose
- **Debug mode** (`DEBUG=true`): Enable verbose logging in any environment

### Running with Different Log Levels

```bash
# Production mode (minimal logging)
npm run start:prod

# Development mode (verbose logging)
npm run start:dev

# Production with debug logging
DEBUG=true npm run start:prod
```

### Log Output Example

```
[Nest] 12345  - 2024/01/20, 10:00:00 AM     LOG [MCPServer] ═══════════════════════════════════════════════════════════════
[Nest] 12345  - 2024/01/20, 10:00:00 AM     LOG [MCPServer] CLDF MCP server is ready!
[Nest] 12345  - 2024/01/20, 10:00:00 AM     LOG [MCPServer] ═══════════════════════════════════════════════════════════════
[Nest] 12345  - 2024/01/20, 10:00:00 AM     LOG [MCPServer] Communication: stdio (Model Context Protocol)
[Nest] 12345  - 2024/01/20, 10:00:00 AM     LOG [MCPServer] Status: Waiting for MCP requests...
```

### What Gets Logged

- Server startup information (environment, CLI path, process ID)
- Tool registration and initialization
- Incoming MCP requests and tool calls
- Command execution with timing information
- Success/failure of operations
- In development mode: detailed arguments and debug information

## Project Structure

```
src/
├── main.ts                      # Application entry point
├── app.module.ts               # Root application module
└── mcp/                        # MCP module
    ├── mcp.module.ts           # MCP module definition
    ├── mcp-server.service.ts   # MCP server implementation
    ├── cldf.service.ts         # CLDF CLI wrapper service
    ├── tool-handlers.service.ts # Tool implementation handlers
    └── tools.constants.ts      # Tool definitions
```

## Key Changes from Original Implementation

1. **NestJS Architecture**: Migrated from a simple Node.js script to a full NestJS application with dependency injection
2. **Improved Testing**: Added comprehensive unit tests and e2e tests using Jest
3. **Better Error Handling**: Enhanced error messages with helpful suggestions for common issues
4. **Schema Updates**: Added support for optional sessions/climbs and new location fields (city, address)
5. **Type Safety**: Full TypeScript implementation with proper typing

## Future Enhancements

Based on the MCP integration plan, future enhancements will include:

1. **Web Scraping Support**: Import command with URL support and HTML extraction
2. **Streaming Operations**: Handle large datasets without loading all into memory
3. **Progress Reporting**: Real-time progress updates for long-running operations
4. **Batch Operations**: Process multiple sources in a single operation
5. **Custom Extractors**: Plugin system for site-specific data extraction

## Testing with Claude

Once configured, you can test the tools in Claude:

```
Can you create a basic CLDF archive at test.cldf?

Can you validate the test.cldf file?

Can you query all climbs from test.cldf?
```

## Troubleshooting

1. **Binary not found**: Ensure the CLDF binary is in your PATH or update CLDF_CLI environment variable
2. **Permission denied**: Ensure the binary has execute permissions (`chmod +x cldf`)
3. **JSON parsing errors**: Check that all commands include the --json flag
4. **GraalVM issues**: Ensure you're using GraalVM 21+ for building the native binary

## License

Same as the CLDF project.