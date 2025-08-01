#!/bin/bash

# Fix belay_type.dart
cat > lib/models/enums/belay_type.dart << 'EOF'
import 'package:json_annotation/json_annotation.dart';

/// Types of belay for rope climbing
@JsonEnum(valueField: 'value')
enum BelayType {
  /// Top rope belay
  topRope('topRope'),
  
  /// Lead belay
  lead('lead'),
  
  /// Auto belay system
  autoBelay('autoBelay');

  /// Creates a new [BelayType] instance
  const BelayType(this.value);
  
  /// The string value of this enum
  final String value;
}
EOF

# Fix climb_type.dart
cat > lib/models/enums/climb_type.dart << 'EOF'
import 'package:json_annotation/json_annotation.dart';

/// Types of climbing
@JsonEnum(valueField: 'value')
enum ClimbType {
  /// Route climbing
  route('route'),
  
  /// Boulder climbing
  boulder('boulder');

  /// Creates a new [ClimbType] instance
  const ClimbType(this.value);
  
  /// The string value of this enum
  final String value;
}
EOF

# Fix finish_type.dart
cat > lib/models/enums/finish_type.dart << 'EOF'
import 'package:json_annotation/json_annotation.dart';

/// How a climb was finished
/// Note: Some types are sport-specific (e.g., top is for boulders, redpoint is for routes)
@JsonEnum(valueField: 'value')
enum FinishType {
  // Boulder-specific
  /// Boulder topped
  top('top'),
  
  /// Boulder flashed
  flash('flash'),
  
  // Route-specific
  /// Route redpointed
  redpoint('redpoint'),
  
  /// Route onsighted
  onsight('onsight'),
  
  // Common
  /// Climb attempted but not completed
  attempt('attempt'),
  
  /// Hang dogging on route
  hangdog('hangdog'),
  
  /// All free moves completed
  allFree('allFree'),
  
  /// Aid climbing used
  aid('aid');

  /// Creates a new [FinishType] instance
  const FinishType(this.value);
  
  /// The string value of this enum
  final String value;
}
EOF

# Fix grade_system.dart
cat > lib/models/enums/grade_system.dart << 'EOF'
import 'package:json_annotation/json_annotation.dart';

/// Climbing grade systems
@JsonEnum(valueField: 'value')
enum GradeSystem {
  /// Font scale (bouldering)
  font('font'),
  
  /// V-scale (bouldering)
  vScale('vScale'),
  
  /// French sport climbing
  french('french'),
  
  /// UIAA scale
  uiaa('uiaa'),
  
  /// YDS (Yosemite Decimal System)
  yds('yds'),
  
  /// UK technical grades
  ukTech('ukTech');

  /// Creates a new [GradeSystem] instance
  const GradeSystem(this.value);
  
  /// The string value of this enum
  final String value;
}
EOF

# Fix media_source.dart
cat > lib/models/enums/media_source.dart << 'EOF'
import 'package:json_annotation/json_annotation.dart';

/// Source of media files
@JsonEnum(valueField: 'value')
enum MediaSource {
  /// File stored in the archive
  archive('archive'),
  
  /// External file reference
  file('file'),
  
  /// External URL reference
  url('url');

  /// Creates a new [MediaSource] instance
  const MediaSource(this.value);
  
  /// The string value of this enum
  final String value;
}
EOF

# Fix media_type.dart
cat > lib/models/enums/media_type.dart << 'EOF'
import 'package:json_annotation/json_annotation.dart';

/// Types of media
@JsonEnum(valueField: 'value')
enum MediaType {
  /// Photo/image
  photo('photo'),
  
  /// Video
  video('video');

  /// Creates a new [MediaType] instance
  const MediaType(this.value);
  
  /// The string value of this enum
  final String value;
}
EOF

# Fix platform.dart
cat > lib/models/enums/platform.dart << 'EOF'
import 'package:json_annotation/json_annotation.dart';

/// Platform that created the archive
@JsonEnum(valueField: 'value')
enum Platform {
  /// Desktop application
  desktop('Desktop'),
  
  /// Mobile application
  mobile('Mobile'),
  
  /// Web application
  web('Web'),
  
  /// Server/API
  server('Server');

  /// Creates a new [Platform] instance
  const Platform(this.value);
  
  /// The string value of this enum
  final String value;
}
EOF

# Fix protection_rating.dart
cat > lib/models/enums/protection_rating.dart << 'EOF'
import 'package:json_annotation/json_annotation.dart';

/// Protection quality ratings
@JsonEnum(valueField: 'value')
enum ProtectionRating {
  /// Well protected
  g('G'),
  
  /// Good protection
  pg('PG'),
  
  /// Adequate protection
  pg13('PG13'),
  
  /// Run-out sections
  r('R'),
  
  /// Very dangerous
  x('X');

  /// Creates a new [ProtectionRating] instance
  const ProtectionRating(this.value);
  
  /// The string value of this enum
  final String value;
}
EOF

# Fix rock_type.dart
cat > lib/models/enums/rock_type.dart << 'EOF'
import 'package:json_annotation/json_annotation.dart';

/// Types of rock
@JsonEnum(valueField: 'value')
enum RockType {
  /// Granite rock
  granite('granite'),
  
  /// Limestone rock
  limestone('limestone'),
  
  /// Sandstone rock
  sandstone('sandstone'),
  
  /// Basalt rock
  basalt('basalt'),
  
  /// Quartzite rock
  quartzite('quartzite'),
  
  /// Gneiss rock
  gneiss('gneiss'),
  
  /// Schist rock
  schist('schist'),
  
  /// Conglomerate rock
  conglomerate('conglomerate'),
  
  /// Rhyolite rock
  rhyolite('rhyolite'),
  
  /// Gritstone rock
  gritstone('gritstone'),
  
  /// Dolomite rock
  dolomite('dolomite'),
  
  /// Other rock type
  other('other');

  /// Creates a new [RockType] instance
  const RockType(this.value);
  
  /// The string value of this enum
  final String value;
}
EOF

# Fix route_type.dart
cat > lib/models/enums/route_type.dart << 'EOF'
import 'package:json_annotation/json_annotation.dart';

/// Types of routes
@JsonEnum(valueField: 'value')
enum RouteType {
  /// Sport/rope route
  route('route'),
  
  /// Boulder problem
  boulder('boulder');

  /// Creates a new [RouteType] instance
  const RouteType(this.value);
  
  /// The string value of this enum
  final String value;
}
EOF

# Fix session_type.dart
cat > lib/models/enums/session_type.dart << 'EOF'
import 'package:json_annotation/json_annotation.dart';

/// Types of climbing sessions
@JsonEnum(valueField: 'value')
enum SessionType {
  /// Training session
  training('training'),
  
  /// Projecting a specific route
  project('project'),
  
  /// Casual climbing
  casual('casual'),
  
  /// Competition
  competition('competition'),
  
  /// Warm-up session
  warmup('warmup'),
  
  /// Recovery session
  recovery('recovery'),
  
  /// Endurance training
  endurance('endurance'),
  
  /// Power training
  power('power'),
  
  /// Technique training
  technique('technique'),
  
  /// Outdoor climbing trip
  outdoor('outdoor'),
  
  /// Indoor climbing session
  indoor('indoor');

  /// Creates a new [SessionType] instance
  const SessionType(this.value);
  
  /// The string value of this enum
  final String value;
}
EOF

# Fix terrain_type.dart
cat > lib/models/enums/terrain_type.dart << 'EOF'
import 'package:json_annotation/json_annotation.dart';

/// Types of climbing terrain
@JsonEnum(valueField: 'value')
enum TerrainType {
  /// Vertical wall
  vertical('vertical'),
  
  /// Overhanging terrain
  overhang('overhang'),
  
  /// Slab climbing
  slab('slab'),
  
  /// Roof climbing
  roof('roof');

  /// Creates a new [TerrainType] instance
  const TerrainType(this.value);
  
  /// The string value of this enum
  final String value;
}
EOF

echo "All enum files have been fixed!"