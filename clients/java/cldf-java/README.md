# CLDF Java Client

Java client library for reading and writing CrushLog Data Format (CLDF) archives.

## Features

- Read and write CLDF archives (.cldf files)
- Full support for all CLDF data types (climbs, sessions, locations, routes, etc.)
- Checksum validation
- Type-safe POJOs with Lombok
- Support for both Maven and Gradle

## Requirements

- Java 21 or higher
- Maven 3.6+ or Gradle 7+

## Installation

### Maven

```xml
<dependency>
    <groupId>app.crushlog.cldf</groupId>
    <artifactId>cldf-java</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle

```gradle
implementation 'app.crushlog.cldf:cldf-java:1.0.0'
```

## Quick Start

### Reading a CLDF Archive

```java
import app.crushlog.cldf.api.CLDF;
import app.crushlog.cldf.api.CLDFArchive;
import java.io.File;

// Read from file
CLDFArchive archive = CLDF.read(new File("climbing-data.cldf"));

// Access data
System.out.println("Locations: " + archive.getLocations().size());
System.out.println("Climbs: " + archive.getClimbs().size());

// Iterate through climbs
for (Climb climb : archive.getClimbs()) {
    System.out.println(climb.getRouteName() + " - " + climb.getGrades().getGrade());
}
```

### Writing a CLDF Archive

```java
import app.crushlog.cldf.api.CLDF;
import app.crushlog.cldf.api.CLDFArchive;
import app.crushlog.cldf.models.*;
import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.Arrays;

// Create manifest
Manifest manifest = Manifest.builder()
    .version("1.0.0")
    .format("CLDF")
    .exportDate(OffsetDateTime.now())
    .appVersion("1.0")
    .platform(Manifest.Platform.Desktop)
    .build();

// Create location
Location location = Location.builder()
    .id(1)
    .name("Local Climbing Gym")
    .isIndoor(true)
    .country("USA")
    .build();

// Create session
Session session = Session.builder()
    .id(1)
    .date(LocalDate.now())
    .location("Local Climbing Gym")
    .build();

// Create climb
Climb climb = Climb.builder()
    .id(1)
    .sessionId(1)
    .date(LocalDate.now())
    .routeName("Blue Problem")
    .type(Climb.ClimbType.boulder)
    .finishType("flash")
    .grades(Climb.GradeInfo.builder()
        .system(Climb.GradeInfo.GradeSystem.vScale)
        .grade("V4")
        .build())
    .build();

// Build archive
CLDFArchive archive = CLDFArchive.builder()
    .manifest(manifest)
    .locations(Arrays.asList(location))
    .sessions(Arrays.asList(session))
    .climbs(Arrays.asList(climb))
    .build();

// Write to file
CLDF.write(archive, new File("my-climbs.cldf"));
```

## Advanced Usage

### Custom Reader Settings

```java
// Disable checksum validation for faster reading
CLDFReader reader = CLDF.createReader(false, false);
CLDFArchive archive = reader.read(new File("data.cldf"));

// Enable pretty printing for debugging
CLDFWriter writer = CLDF.createWriter(true);
writer.write(archive, new File("formatted.cldf"));
```

### Working with Optional Data

```java
// Check for optional data before accessing
if (archive.hasRoutes()) {
    for (Route route : archive.getRoutes()) {
        System.out.println(route.getName() + " - " + route.getQualityRating() + " stars");
    }
}

if (archive.hasTags()) {
    for (Tag tag : archive.getTags()) {
        System.out.println(tag.getName() + " (" + tag.getCategory() + ")");
    }
}

if (archive.hasEmbeddedMedia()) {
    Map<String, byte[]> mediaFiles = archive.getMediaFiles();
    // Process media files...
}
```

## Data Models

The library provides POJOs for all CLDF data types:

- `Manifest` - Archive metadata
- `Location` - Climbing locations (crags, gyms)
- `Climb` - Individual climb records
- `Session` - Climbing sessions
- `Route` - Route/problem definitions
- `Sector` - Sectors within locations
- `Tag` - Custom and predefined tags
- `MediaItem` - Media metadata
- `Checksums` - File integrity checksums

## Building from Source

```bash
# Using Maven
mvn clean install

# Using Gradle
gradle build

# Run tests
mvn test
# or
gradle test
```

## Example Application

See the complete example in `src/main/java/io/cldf/examples/CLDFExample.java` for a full demonstration of reading and writing CLDF archives.

## License

MIT License

## Support

For questions and support, please contact: info@crushlog.app