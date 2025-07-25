# CLDF Enumeration Reference

This document defines all enumerated types used in the CrushLog Data Format.

## Climb Types

### ClimbType
Defines the type of climbing activity.

- `boulder` - Bouldering (no rope)
- `route` - Rope climbing

## Finish Types

### BoulderFinishType
Completion styles specific to bouldering.

- `flash` - First attempt with beta/knowledge
- `top` - Successful completion (send)
- `repeat` - Subsequent successful completion
- `project` - Working on but not yet completed

### RouteFinishType
Completion styles specific to rope climbing.

- `onsight` - First attempt with no prior knowledge
- `flash` - First attempt with beta/knowledge
- `redpoint` - Clean ascent after practice
- `repeat` - Subsequent successful completion
- `project` - Working on but not yet completed

## Grade Systems

### BoulderGradeSystem
Grading systems for boulder problems.

- `vScale` - V-Scale (V0-V17, VB)
- `font` - Fontainebleau (3-9A)

### RouteGradeSystem
Grading systems for rope climbs.

- `french` - French Sport (3a-9c)
- `yds` - Yosemite Decimal System (5.0-5.15d)
- `uiaa` - UIAA (I-XII)

## Location and Terrain

### TerrainType
Classification of climbing terrain.

- `natural` - Natural rock formations
- `artificial` - Artificial/indoor climbing walls

### RockType
Geological rock types.

- `sandstone` - Sandstone
- `limestone` - Limestone
- `granite` - Granite
- `basalt` - Basalt
- `gneiss` - Gneiss
- `quartzite` - Quartzite
- `conglomerate` - Conglomerate
- `schist` - Schist
- `dolomite` - Dolomite
- `slate` - Slate
- `rhyolite` - Rhyolite
- `gabbro` - Gabbro
- `volcanicTuff` - Volcanic Tuff
- `andesite` - Andesite
- `chalk` - Chalk

## Session Types

### SessionType
Types of climbing sessions.

- `sportClimbing` - Sport climbing session
- `multiPitch` - Multi-pitch climbing
- `tradClimbing` - Traditional climbing
- `bouldering` - Outdoor bouldering
- `indoorClimbing` - Indoor rope climbing
- `indoorBouldering` - Indoor bouldering
- `boardSession` - Training board session

## Route Characteristics

### RouteCharacteristics
Protection style for routes.

- `trad` - Traditional (gear protected)
- `bolted` - Sport (bolt protected)

### RouteType
Classification of routes (same as ClimbType).

- `boulder` - Boulder problem
- `route` - Rope route

## Belay Types

### BelayType
Belay methods for rope climbing.

- `topRope` - Top rope belay
- `lead` - Lead climbing
- `autoBelay` - Auto-belay device

## Predefined Tags

### PredefinedTag
System-defined tags for categorizing climbs.

#### Angle/Wall Type
- `overhang` - Overhanging wall
- `slab` - Slab (less than vertical)
- `vertical` - Vertical wall
- `roof` - Roof section

#### Features
- `crack` - Crack climbing
- `corner` - Corner/dihedral
- `arete` - Arete/outside corner

#### Movement
- `dyno` - Dynamic movement

#### Hold Types
- `crimpy` - Small edges/crimps
- `slopers` - Sloping holds
- `jugs` - Large holds
- `pockets` - Pocket holds

#### Climbing Style
- `technical` - Technical/delicate climbing
- `powerful` - Power-based climbing
- `endurance` - Endurance-based climbing

## Media

### MediaType
Types of media files.

- `photo` - Photograph
- `video` - Video recording

### MediaSource
Source of media files.

- `photos_library` - Device photo library
- `local` - Local file system
- `embedded` - Embedded in archive

### MediaExportStrategy
Strategy for exporting media.

- `reference` - Reference only (IDs)
- `thumbnails` - Include thumbnails
- `full` - Include full media files

## Platform

### Platform
Supported platforms.

- `iOS` - Apple iOS
- `Android` - Google Android
- `Web` - Web browser
- `Desktop` - Desktop application

## Import/Export

### ConflictResolution
How to handle conflicts during import.

- `skip` - Skip conflicting records
- `overwrite` - Replace existing records
- `duplicate` - Create new records
- `merge` - Intelligent merge