# Claude Desktop MCP Integration Guide

## Quick Setup

1. **Run the setup script** from the project root:
   ```bash
   ./setup-claude-mcp.sh
   ```

2. **Restart Claude Desktop**

3. **Verify the connection** - In Claude, you should see "cldf-tools" in the MCP servers list

## Manual Setup

If the automatic setup doesn't work, follow these steps:

### 1. Build Prerequisites

```bash
# Build CLDF native binary
cd clients/java
./gradlew :cldf-tool:nativeCompile

# Build MCP server
cd ../../mcp-server
npm install
npm run build
```

### 2. Configure Claude Desktop

Create or edit `~/Library/Application Support/Claude/claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "cldf-tools": {
      "command": "node",
      "args": [
        "/absolute/path/to/mcp-server/dist/src/main.js"
      ],
      "env": {
        "CLDF_CLI": "/absolute/path/to/cldf",
        "NODE_ENV": "production"
      }
    }
  }
}
```

### 3. Restart Claude Desktop

## Using CLDF Tools in Claude

Once connected, you can ask Claude to:

### Get Schema Information
- "Show me the CLDF schema for locations"
- "What fields are required for a route?"
- "Explain the CLDF manifest structure"

### Validate Data
- "Validate this CLDF data: {manifest: {...}, locations: [...]}"
- "Check if this location data is valid for CLDF"

### Create Archives
- "Create a basic CLDF archive at test.cldf"
- "Create a CLDF file with this climbing data: ..."

### Query Archives
- "Query all locations from my-climbs.cldf"
- "Show me all climbs from yesterday in archive.cldf"

### Other Operations
- "Merge climb1.cldf and climb2.cldf into combined.cldf"
- "Convert archive.cldf to JSON format"
- "Extract just the routes from my-archive.cldf"

## Troubleshooting

### MCP Server Not Showing Up

1. Check Claude Desktop logs:
   ```bash
   tail -f ~/Library/Logs/Claude/mcp.log
   ```

2. Verify the config file:
   ```bash
   cat ~/Library/Application\ Support/Claude/claude_desktop_config.json
   ```

3. Test the MCP server manually:
   ```bash
   cd mcp-server
   npm run start:prod
   ```

### Enable Debug Logging

For more verbose logging, edit the config file and set:
```json
"env": {
  "NODE_ENV": "development"
}
```

### Common Issues

1. **"Command not found"** - Check that Node.js is in your PATH
2. **"CLDF CLI not found"** - Verify the CLDF_CLI path in the config
3. **"Permission denied"** - Ensure the CLDF binary has execute permissions:
   ```bash
   chmod +x /path/to/cldf
   ```

## Development Mode

For development with hot reload:

1. Update config to use tsx:
   ```json
   {
     "mcpServers": {
       "cldf-tools-dev": {
         "command": "npx",
         "args": ["tsx", "watch", "src/main.ts"],
         "cwd": "/path/to/mcp-server",
         "env": {
           "NODE_ENV": "development"
         }
       }
     }
   }
   ```

2. Run in development:
   ```bash
   cd mcp-server
   npm run start:dev
   ```

## Logs Location

- **MCP logs**: `~/Library/Logs/Claude/mcp.log`
- **Server logs**: Output to stderr, visible in MCP logs
- **Tool execution logs**: Included in server logs (development mode)