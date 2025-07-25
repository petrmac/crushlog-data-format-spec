# CLDF Media Handling Specification

## Overview

The CrushLog Data Format supports flexible media handling to accommodate different use cases and storage constraints. Media files (photos and videos) can be handled in three different ways during export/import.

## Media Export Strategies

### 1. Reference Strategy (`reference`)

**Description**: Stores only media identifiers and metadata without the actual files.

**Use Cases**:
- Minimal file size exports
- When media files remain accessible in the device's photo library
- Quick data backups without media

**Structure**:
```json
{
  "media": {
    "photos": ["photo_id_1", "photo_id_2"],
    "videos": ["video_id_1"],
    "count": 3
  }
}
```

**Metadata Storage**:
- Platform-specific asset IDs (e.g., PHAsset for iOS)
- Original filenames
- Creation timestamps
- Basic metadata (dimensions, size, duration)

### 2. Thumbnail Strategy (`thumbnails`)

**Description**: Includes compressed thumbnail images while excluding full-resolution media.

**Use Cases**:
- Visual reference without large file sizes
- Browsing climb history with preview images
- Sharing data with visual context

**Specifications**:
- Maximum thumbnail size: 200x200 pixels
- Format: JPEG (quality 80%)
- Video thumbnails: First frame or poster frame
- File naming: `{climb_id}_{index}_thumb.jpg`

**Structure**:
```
media/
└── thumbnails/
    ├── climb_123_1_thumb.jpg
    ├── climb_123_2_thumb.jpg
    └── climb_456_1_thumb.jpg
```

### 3. Full Export Strategy (`full`)

**Description**: Embeds complete media files within the archive.

**Use Cases**:
- Complete data portability
- Archival purposes
- Sharing complete climbing history
- Device migration

**Specifications**:
- Photos: Converted to JPEG (quality 90%) for compatibility
- Videos: MP4 format with H.264 codec
- Original metadata preserved in sidecar JSON files
- File naming: `{climb_id}_{index}.{ext}`

**Structure**:
```
media/
├── photos/
│   ├── climb_123_1.jpg
│   ├── climb_123_2.jpg
│   └── metadata/
│       ├── climb_123_1.json
│       └── climb_123_2.json
└── videos/
    ├── climb_456_1.mp4
    └── metadata/
        └── climb_456_1.json
```

## Media Metadata Schema

Each media file can have associated metadata stored in `media-metadata.json`:

```json
{
  "id": "photo_id_1",
  "climbId": "climb_123",
  "type": "photo",
  "source": "photos_library",
  "assetId": "PHAsset_12345",
  "filename": "IMG_1234.HEIC",
  "thumbnailPath": "media/thumbnails/climb_123_1_thumb.jpg",
  "embedded": true,
  "createdAt": "2024-01-20T10:30:00Z",
  "metadata": {
    "width": 4032,
    "height": 3024,
    "size": 2456789,
    "location": {
      "latitude": 40.0150,
      "longitude": -105.3967
    },
    "exif": {
      "camera": "iPhone 15 Pro",
      "focalLength": 24,
      "aperture": 1.78,
      "iso": 100
    }
  }
}
```

## Import Behavior

### Media Matching

During import, the system attempts to match media references:

1. **By Asset ID**: Platform-specific identifiers (highest priority)
2. **By Filename**: Original filename matching
3. **By Metadata**: Creation date, size, and location matching
4. **By Hash**: SHA-256 hash comparison (if available)

### Import Options

```json
{
  "importMedia": true,
  "mediaImportStrategy": "smart",
  "skipMissingMedia": false,
  "importToLibrary": true
}
```

**Strategies**:
- `smart`: Import only if not already present
- `always`: Import all media files
- `never`: Skip media import
- `missing`: Import only missing media

### Conflict Resolution

When media already exists:
1. Compare SHA-256 hashes if available
2. Check file size and dimensions
3. Use creation timestamp as tiebreaker
4. Prompt user for large conflicts

## Privacy Considerations

### Location Data
- GPS coordinates can be stripped from EXIF data
- Precision can be reduced (e.g., to city level)
- User preference: `includeMediaLocation: boolean`

### Personal Information
- Camera serial numbers removed by default
- Owner information stripped from EXIF
- Face detection data not exported

## Performance Guidelines

### Compression
- Use streaming compression for large media sets
- Process media in batches of 10-20 files
- Show progress indicators for operations > 5 seconds

### Memory Management
- Stream large video files instead of loading into memory
- Use temporary files for processing
- Clean up temporary files after completion

### Size Limits
- Warn users when export > 100MB
- Suggest thumbnail strategy for exports > 500MB
- Maximum single file size: 2GB (filesystem limit)

## Platform-Specific Considerations

### iOS
- Request photo library permissions before export
- Use PHAsset identifiers for reference
- Support HEIC to JPEG conversion
- Handle Live Photos as static images

### Android
- Use MediaStore URIs for references
- Request storage permissions
- Support various manufacturer formats
- Handle motion photos appropriately

## Error Handling

### Common Errors
1. **Media Not Found**: Original file deleted from library
2. **Permission Denied**: No access to photo library
3. **Corrupt Media**: File damaged or incomplete
4. **Unsupported Format**: Proprietary or unknown format

### Error Response
```json
{
  "mediaId": "photo_123",
  "error": "MEDIA_NOT_FOUND",
  "message": "Original media file no longer exists in photo library",
  "fallback": "thumbnail"
}
```

## Best Practices

1. **Always include thumbnails** when using reference strategy
2. **Validate media access** before starting export
3. **Provide clear size estimates** before export
4. **Allow selective media export** for large collections
5. **Implement resume capability** for interrupted exports
6. **Cache converted media** to speed up repeated exports