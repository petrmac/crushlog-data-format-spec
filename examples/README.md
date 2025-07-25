# CLDF Examples

This directory contains example CLDF files demonstrating various use cases and features.

## Examples

### minimal.cldf/
A minimal valid CLDF export containing:
- 1 indoor climbing location
- 1 bouldering session
- 3 climbs (flash, send, project)
- Basic predefined tags
- No media files

**Use case**: Basic indoor bouldering session

### outdoor-complete.cldf/ (coming soon)
A comprehensive outdoor climbing export with:
- Multiple locations with GPS coordinates
- Sectors and defined routes
- Mixed boulder and route climbs
- Weather data and partners
- Media references with thumbnails

**Use case**: Complete outdoor climbing trip

### competition.cldf/ (coming soon)
Competition or assessment format featuring:
- Structured session types
- Performance metrics
- Time tracking
- Systematic attempt recording

**Use case**: Competition or climbing assessment

## Viewing Examples

These example directories represent the extracted contents of `.cldf` files. In actual use:

1. These directories would be compressed into a single `.cldf` file
2. The `.cldf` file is a gzipped archive
3. Users would import the entire `.cldf` file

## Creating Your Own Examples

To create a CLDF file from these examples:

```bash
# Navigate to example directory
cd minimal.cldf

# Create gzipped archive
tar -czf ../minimal.cldf .

# Verify the archive
gzip -t ../minimal.cldf
```

## Validation

Each example should validate against the JSON schemas in the `schemas/` directory. You can validate using any JSON Schema validator:

```bash
# Example using ajv-cli
ajv validate -s ../schemas/manifest.schema.json -d manifest.json
```