package io.cldf.tool.commands;

import java.io.File;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

import jakarta.inject.Inject;

import io.cldf.api.CLDFArchive;
import io.cldf.api.CLDFWriter;
import io.cldf.models.*;
import io.cldf.tool.services.ValidationService;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Slf4j
@Command(
    name = "create",
    description = "Create a new CLDF archive",
    mixinStandardHelpOptions = true)
public class CreateCommand implements Runnable {

  @Option(
      names = {"-o", "--output"},
      description = "Output CLDF file",
      required = true)
  private File outputFile;

  @Option(
      names = "--template",
      description = "Use template: ${COMPLETION-CANDIDATES}",
      completionCandidates = TemplateType.class)
  private String template;

  @Option(names = "--validate", description = "Validate after creation", defaultValue = "true")
  private boolean validate;

  @Option(
      names = "--pretty-print",
      description = "Pretty print JSON in archive",
      defaultValue = "true")
  private boolean prettyPrint;

  @Inject private ValidationService validationService;

  static class TemplateType extends ArrayList<String> {
    TemplateType() {
      super(Arrays.asList("basic", "demo", "empty"));
    }
  }

  @Override
  public void run() {
    try {
      CLDFArchive archive;

      if (template != null) {
        archive = createFromTemplate(template);
      } else {
        archive = createEmpty();
      }

      if (validate) {
        log.info("Validating archive...");
        var validationResult = validationService.validate(archive);
        if (!validationResult.isValid()) {
          log.error("Validation failed: {}", validationResult.getErrors());
          System.exit(1);
        }
      }

      log.info("Writing archive to {}", outputFile.getAbsolutePath());
      CLDFWriter writer = new CLDFWriter(prettyPrint, validate);
      writer.write(archive, outputFile);
      log.info("Successfully created CLDF archive: {}", outputFile.getName());

    } catch (Exception e) {
      log.error("Failed to create CLDF archive", e);
      System.exit(1);
    }
  }

  private CLDFArchive createFromTemplate(String templateType) {
    log.info("Creating archive from template: {}", templateType);

    switch (templateType.toLowerCase()) {
      case "basic":
        return createBasicTemplate();
      case "demo":
        return createDemoTemplate();
      case "empty":
      default:
        return createEmpty();
    }
  }

  private CLDFArchive createEmpty() {
    // Create minimal required components
    Manifest manifest =
        Manifest.builder()
            .version("1.0.0")
            .format("CLDF")
            .creationDate(OffsetDateTime.now())
            .appVersion("1.0.0")
            .platform(Manifest.Platform.Desktop)
            .build();

    Location location =
        Location.builder()
            .id(1)
            .name("Default Location")
            .isIndoor(true)
            .createdAt(OffsetDateTime.now())
            .build();

    Session session =
        Session.builder()
            .id("sess_1")
            .date(LocalDate.now())
            .location("Default Location")
            .locationId("1")
            .isIndoor(true)
            .build();

    return CLDFArchive.builder()
        .manifest(manifest)
        .locations(List.of(location))
        .sessions(List.of(session))
        .climbs(new ArrayList<>())
        .checksums(Checksums.builder().algorithm("SHA-256").build())
        .build();
  }

  private CLDFArchive createBasicTemplate() {
    Location location =
        Location.builder()
            .id(1)
            .name("Local Climbing Gym")
            .isIndoor(true)
            .country("United States")
            .state("California")
            .createdAt(OffsetDateTime.now())
            .build();

    Session session =
        Session.builder()
            .id("sess_" + System.currentTimeMillis())
            .date(LocalDate.now())
            .location(location.getName())
            .locationId("1")
            .isIndoor(true)
            .build();

    List<Climb> climbs =
        List.of(
            Climb.builder()
                .id(1)
                .sessionId(1)
                .date(session.getDate())
                .routeName("Warm-up V0")
                .type(Climb.ClimbType.boulder)
                .finishType("flash")
                .attempts(1)
                .grades(
                    Climb.GradeInfo.builder()
                        .system(Climb.GradeInfo.GradeSystem.vScale)
                        .grade("V0")
                        .build())
                .isIndoor(true)
                .build(),
            Climb.builder()
                .id(2)
                .sessionId(1)
                .date(session.getDate())
                .routeName("Project V4")
                .type(Climb.ClimbType.boulder)
                .finishType("redpoint")
                .attempts(5)
                .grades(
                    Climb.GradeInfo.builder()
                        .system(Climb.GradeInfo.GradeSystem.vScale)
                        .grade("V4")
                        .build())
                .rating(4)
                .notes("Finally sent it! Crux was the heel hook.")
                .isIndoor(true)
                .build());

    Manifest manifest =
        Manifest.builder()
            .version("1.0.0")
            .format("CLDF")
            .creationDate(OffsetDateTime.now())
            .appVersion("1.0.0")
            .platform(Manifest.Platform.Desktop)
            .stats(
                Manifest.Stats.builder().locationsCount(1).sessionsCount(1).climbsCount(2).build())
            .build();

    return CLDFArchive.builder()
        .manifest(manifest)
        .locations(List.of(location))
        .sessions(List.of(session))
        .climbs(climbs)
        .checksums(Checksums.builder().algorithm("SHA-256").build())
        .build();
  }

  private CLDFArchive createDemoTemplate() {
    // Create multiple locations
    List<Location> locations =
        List.of(
            Location.builder()
                .id(1)
                .name("Movement Climbing Gym")
                .isIndoor(true)
                .country("United States")
                .state("Colorado")
                .starred(true)
                .createdAt(OffsetDateTime.now())
                .build(),
            Location.builder()
                .id(2)
                .name("Eldorado Canyon")
                .isIndoor(false)
                .country("United States")
                .state("Colorado")
                .rockType(Location.RockType.sandstone)
                .coordinates(
                    Location.Coordinates.builder().latitude(39.9308).longitude(-105.2925).build())
                .createdAt(OffsetDateTime.now())
                .build());

    // Create sessions
    List<Session> sessions =
        List.of(
            Session.builder()
                .id("sess_gym")
                .date(LocalDate.now().minusDays(7))
                .location(locations.get(0).getName())
                .locationId("1")
                .isIndoor(true)
                .sessionType(Session.SessionType.indoorClimbing)
                .build(),
            Session.builder()
                .id("sess_outdoor")
                .date(LocalDate.now().minusDays(2))
                .location(locations.get(1).getName())
                .locationId("2")
                .isIndoor(false)
                .climbType(Climb.ClimbType.route)
                .weather(
                    Session.Weather.builder()
                        .conditions("sunny")
                        .temperature(22.0)
                        .humidity(45.0)
                        .build())
                .partners(List.of("Alex", "Sarah"))
                .build());

    // Create climbs
    List<Climb> climbs = new ArrayList<>();

    // Gym session climbs
    for (int i = 0; i < 8; i++) {
      climbs.add(
          Climb.builder()
              .id(i + 1)
              .sessionId(1)
              .date(sessions.get(0).getDate())
              .routeName("Problem " + (i + 1))
              .type(Climb.ClimbType.boulder)
              .finishType(i < 5 ? "flash" : "redpoint")
              .attempts(i < 5 ? 1 : i - 3)
              .grades(
                  Climb.GradeInfo.builder()
                      .system(Climb.GradeInfo.GradeSystem.vScale)
                      .grade("V" + (i / 2))
                      .build())
              .isIndoor(true)
              .rating(i % 2 == 0 ? 4 : 3)
              .build());
    }

    // Outdoor session climb
    climbs.add(
        Climb.builder()
            .id(9)
            .sessionId(2)
            .date(sessions.get(1).getDate())
            .routeName("The Bastille Crack")
            .type(Climb.ClimbType.route)
            .finishType("onsight")
            .attempts(1)
            .grades(
                Climb.GradeInfo.builder()
                    .system(Climb.GradeInfo.GradeSystem.yds)
                    .grade("5.7")
                    .build())
            .belayType(Climb.BelayType.lead)
            .height(110.0)
            .rating(5)
            .notes("Classic route! Perfect hand jams all the way.")
            .isIndoor(false)
            .rockType(Location.RockType.sandstone)
            .partners(sessions.get(1).getPartners())
            .weather("sunny")
            .build());

    Manifest manifest =
        Manifest.builder()
            .version("1.0.0")
            .format("CLDF")
            .creationDate(OffsetDateTime.now())
            .appVersion("1.0.0")
            .platform(Manifest.Platform.Desktop)
            .stats(
                Manifest.Stats.builder()
                    .locationsCount(locations.size())
                    .sessionsCount(sessions.size())
                    .climbsCount(climbs.size())
                    .build())
            .build();

    return CLDFArchive.builder()
        .manifest(manifest)
        .locations(locations)
        .sessions(sessions)
        .climbs(climbs)
        .checksums(Checksums.builder().algorithm("SHA-256").build())
        .build();
  }
}
