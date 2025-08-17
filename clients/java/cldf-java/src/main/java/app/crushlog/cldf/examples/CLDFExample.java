package app.crushlog.cldf.examples;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import app.crushlog.cldf.api.CLDF;
import app.crushlog.cldf.api.CLDFArchive;
import app.crushlog.cldf.models.*;
import app.crushlog.cldf.models.enums.*;

/** Example usage of the CLDF Java client library. */
public class CLDFExample {

  public static void main(String[] args) {
    try {
      // Create a new CLDF archive
      CLDFArchive archive = createSampleArchive();

      // Write to file
      File outputFile = new File("example.cldf");
      CLDF.write(archive, outputFile);
      System.out.println("CLDF archive written to: " + outputFile.getAbsolutePath());

      // Read from file
      CLDFArchive readArchive = CLDF.read(outputFile);
      System.out.println("\nRead CLDF archive:");
      System.out.println("Version: " + readArchive.getManifest().getVersion());
      System.out.println("Locations: " + readArchive.getLocations().size());
      System.out.println("Climbs: " + readArchive.getClimbs().size());
      System.out.println("Sessions: " + readArchive.getSessions().size());

      // Display some climb data
      System.out.println("\nClimbs:");
      for (Climb climb : readArchive.getClimbs()) {
        System.out.println(
            "- "
                + climb.getRouteName()
                + " ("
                + climb.getGrades().getGrade()
                + ") - "
                + climb.getFinishType());
      }

    } catch (IOException e) {
      System.err.println("Error: " + e.getMessage());
      // In production code, use proper logging instead of printStackTrace
      // e.printStackTrace();
    }
  }

  private static CLDFArchive createSampleArchive() {
    // Create manifest
    Manifest manifest =
        Manifest.builder()
            .version("1.0.0")
            .format("CLDF")
            .creationDate(OffsetDateTime.now())
            .appVersion("1.0.0")
            .platform(Platform.DESKTOP)
            .stats(
                Manifest.Stats.builder().locationsCount(2).climbsCount(5).sessionsCount(2).build())
            .build();

    // Create locations
    List<Location> locations =
        Arrays.asList(
            Location.builder()
                .id(1)
                .name("The Spot Bouldering Gym")
                .isIndoor(true)
                .country("US")
                .state("Colorado")
                .coordinates(
                    Location.Coordinates.builder().latitude(40.0170).longitude(-105.2830).build())
                .terrainType(TerrainType.ARTIFICIAL)
                .starred(true)
                .build(),
            Location.builder()
                .id(2)
                .name("Clear Creek Canyon")
                .isIndoor(false)
                .country("US")
                .state("Colorado")
                .coordinates(
                    Location.Coordinates.builder().latitude(39.7416).longitude(-105.5080).build())
                .rockType(RockType.GRANITE)
                .terrainType(TerrainType.NATURAL)
                .accessInfo("Park at pullout mile marker 269")
                .build());

    // Create sessions
    List<Session> sessions =
        Arrays.asList(
            Session.builder()
                .id(1)
                .date(LocalDate.now().minusDays(7))
                .location("The Spot Bouldering Gym")
                .locationId(1)
                .isIndoor(true)
                .climbType(ClimbType.BOULDER)
                .sessionType(SessionType.INDOOR_BOULDERING)
                .partners(Arrays.asList("Alice", "Bob"))
                .notes("Great session, worked on crimpy problems")
                .build(),
            Session.builder()
                .id(2)
                .date(LocalDate.now().minusDays(1))
                .location("Clear Creek Canyon")
                .locationId(2)
                .isIndoor(false)
                .climbType(ClimbType.BOULDER)
                .sessionType(SessionType.BOULDERING)
                .weather(
                    Session.Weather.builder()
                        .conditions("Sunny")
                        .temperature(22.0)
                        .humidity(30.0)
                        .build())
                .approachTime(15)
                .build());

    // Create climbs
    List<Climb> climbs =
        Arrays.asList(
            Climb.builder()
                .id(1)
                .sessionId(1)
                .date(LocalDate.now().minusDays(7))
                .routeName("Purple Crimps")
                .type(ClimbType.BOULDER)
                .finishType(FinishType.FLASH)
                .grades(Climb.GradeInfo.builder().system(GradeSystem.V_SCALE).grade("V4").build())
                .attempts(1)
                .rating(4)
                .color("#800080")
                .isIndoor(true)
                .tags(Arrays.asList("crimpy", "technical"))
                .build(),
            Climb.builder()
                .id(2)
                .sessionId(1)
                .date(LocalDate.now().minusDays(7))
                .routeName("Yellow Overhang")
                .type(ClimbType.BOULDER)
                .finishType(FinishType.TOP)
                .grades(Climb.GradeInfo.builder().system(GradeSystem.V_SCALE).grade("V5").build())
                .attempts(3)
                .rating(5)
                .color("#FFFF00")
                .isIndoor(true)
                .tags(Arrays.asList("overhang", "powerful"))
                .notes("Finally got the heel hook beta!")
                .build(),
            Climb.builder()
                .id(3)
                .sessionId(2)
                .date(LocalDate.now().minusDays(1))
                .routeName("Warmup Traverse")
                .type(ClimbType.BOULDER)
                .finishType(FinishType.TOP)
                .grades(Climb.GradeInfo.builder().system(GradeSystem.V_SCALE).grade("V2").build())
                .attempts(1)
                .isIndoor(false)
                .rockType(RockType.GRANITE)
                .build(),
            Climb.builder()
                .id(4)
                .sessionId(2)
                .date(LocalDate.now().minusDays(1))
                .routeName("The Egg")
                .type(ClimbType.BOULDER)
                .finishType(FinishType.PROJECT)
                .grades(Climb.GradeInfo.builder().system(GradeSystem.V_SCALE).grade("V7").build())
                .attempts(5)
                .falls(4)
                .rating(5)
                .isIndoor(false)
                .rockType(RockType.GRANITE)
                .beta("Start matched on sloper, big move to crimp rail")
                .notes("So close! Got to the top hold but couldn't match")
                .build(),
            Climb.builder()
                .id(5)
                .sessionId(2)
                .date(LocalDate.now().minusDays(1))
                .routeName("Classic Arete")
                .type(ClimbType.BOULDER)
                .finishType(FinishType.REPEAT)
                .grades(Climb.GradeInfo.builder().system(GradeSystem.V_SCALE).grade("V3").build())
                .attempts(1)
                .isRepeat(true)
                .rating(4)
                .isIndoor(false)
                .rockType(RockType.GRANITE)
                .tags(Arrays.asList("arete", "technical"))
                .build());

    // Create tags
    List<Tag> tags =
        Arrays.asList(
            Tag.builder()
                .id(1)
                .name("crimpy")
                .isPredefined(true)
                .predefinedTagKey(PredefinedTagKey.CRIMPY)
                .color("#FF6B6B")
                .category("holds")
                .build(),
            Tag.builder()
                .id(2)
                .name("overhang")
                .isPredefined(true)
                .predefinedTagKey(PredefinedTagKey.OVERHANG)
                .color("#4ECDC4")
                .category("angle")
                .build(),
            Tag.builder()
                .id(3)
                .name("project")
                .isPredefined(false)
                .color("#FFD93D")
                .category("custom")
                .build());

    return CLDFArchive.builder()
        .manifest(manifest)
        .locations(locations)
        .sessions(sessions)
        .climbs(climbs)
        .tags(tags)
        .build();
  }
}
