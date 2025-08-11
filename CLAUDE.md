# CLAUDE.md - Project Context

## MCP Server Information

The MCP (Model Context Protocol) server for CLDF operations has been moved to:
- **Location**: `/Users/petrmacek/git-mirrors/crushlog-data-format-spec/mcp-server/`
- **Source**: `/Users/petrmacek/git-mirrors/crushlog-data-format-spec/mcp-server/src/index.ts`
- **CLDF CLI Tool**: `/Users/petrmacek/git-mirrors/crushlog-data-format-spec/clients/java/cldf-tool/build/native/nativeCompile/cldf`

## Available MCP Tools

1. `cldf_schema_info` - Get CLDF data structure schema and validation rules
2. `cldf_validate_data` - Validate CLDF data structure before creating an archive
3. `cldf_create` - Create a new CLDF archive with climbing data
4. `cldf_validate` - Validate a CLDF archive file
5. `cldf_query` - Query data from a CLDF archive (now includes media support)
6. `cldf_merge` - Merge multiple CLDF archives
7. `cldf_convert` - Convert CLDF archive to different formats
8. `cldf_extract` - Extract specific data from a CLDF archive
9. `cldf_query_media` - Query media information from archives
10. `cldf_extract_media` - Extract media files to a directory

## Commands

When working with CLDF data:
- Always validate data structure before creating archives
- Use `cldf_schema_info` to understand expected data formats
- Run lint/typecheck commands after changes (if available)

## Creating Valid CLDF Data

### Step 1: Get Examples
```
cldf_schema_info(component="exampleData")
```

### Step 2: Check Common Mistakes
```
cldf_schema_info(component="commonMistakes")
```

### Step 3: Quick Field Reference
```
cldf_schema_info(component="fieldReference")
```

### Critical Rules:
1. **ID Types**: Location, Route, and Sector IDs MUST be integers (not strings)
2. **belayType**: Only valid for climbs, NOT routes
3. **No Custom Fields**: Only schema-defined fields are allowed
4. **Grade Patterns**: Must match exact regex patterns (e.g., V4 not v4)
5. **Colors**: Must be hex format #RRGGBB

### Validation Workflow:
1. Build data incrementally
2. Validate with `cldf_validate_data(data={...})`
3. Fix errors based on messages
4. Create archive only after validation passes

## Media Support

CLDF now supports media files (photos and videos):

### Creating Archives with Media
Use the cldf-tool CLI with media options:
```bash
cldf create --template basic --output my-climbs.cldf --media-dir ./photos --media-strategy FULL
```

### Querying Media via MCP
```
cldf_query_media(filePath="archive.cldf", mediaType="photo")
cldf_extract_media(filePath="archive.cldf", outputDir="./media-out")
```

See MEDIA_SUPPORT.md for comprehensive documentation.