package io.cldf.tool.commands;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import jakarta.inject.Inject;

import io.cldf.api.CLDFArchive;
import io.cldf.api.CLDFWriter;
import io.cldf.models.*;
import io.cldf.models.enums.*;
import io.cldf.tool.models.CommandResult;
import io.cldf.tool.services.ValidationService;
import io.cldf.tool.utils.InputHandler;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Slf4j
@Command(
    name = "create",
    description = "Create a new CLDF archive",
    mixinStandardHelpOptions = true)
public class CreateCommand extends BaseCommand {

  public static final String ALGORITHM = "SHA-256";
  public static final String ERRORS = "errors";

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

  @Option(
      names = {"--from-json"},
      description = "Create from JSON input (use - for stdin)")
  private String jsonInput;

  @Option(
      names = {"--stdin"},
      description = "Read JSON data from stdin")
  private boolean readFromStdin;

  @Option(
      names = {"--media-dir"},
      description = "Directory containing media files to include")
  private File mediaDirectory;

  @Option(
      names = {"--media-strategy"},
      description = "Media handling strategy: ${COMPLETION-CANDIDATES}",
      defaultValue = "FULL")
  private MediaStrategy mediaStrategy = MediaStrategy.FULL;

  private final ValidationService validationService;

  @Inject
  public CreateCommand(ValidationService validationService) {
    this.validationService = validationService;
  }

  // For PicoCLI framework - it needs a no-arg constructor
  public CreateCommand() {
    this.validationService = null;
  }

  static class TemplateType extends ArrayList<String> {
    TemplateType() {
      super(Arrays.asList("basic", "demo", "empty"));
    }
  }

  @Override
  protected CommandResult execute() throws Exception {
    CLDFArchive archive;

    try {
      if (jsonInput != null || readFromStdin) {
        logInfo("Creating archive from JSON input");
        archive = createFromJson();
      } else if (template != null) {
        logInfo("Creating archive from template: " + template);
        archive = createFromTemplate(template);
      } else {
        logInfo("Creating empty archive");
        archive = createEmpty();
      }
    } catch (IOException e) {
      log.error("Failed to create archive", e);
      return CommandResult.builder()
          .success(false)
          .message("Failed to create archive: " + e.getMessage())
          .exitCode(1)
          .build();
    }

    List<String> warnings = new ArrayList<>();

    // Process media files if directory is specified
    if (mediaDirectory != null) {
      try {
        processMediaFiles(archive);
      } catch (IOException e) {
        log.error("Failed to process media files", e);
        return CommandResult.builder()
            .success(false)
            .message("Failed to process media files: " + e.getMessage())
            .exitCode(1)
            .build();
      }
    }

    if (validate) {
      logInfo("Validating archive...");
      var validationResult = validationService.validate(archive);
      if (!validationResult.isValid()) {
        return CommandResult.builder()
            .success(false)
            .message("Validation failed")
            .data(Map.of(ERRORS, validationResult.getErrors()))
            .exitCode(1)
            .build();
      }
      if (!validationResult.getWarnings().isEmpty()) {
        warnings.addAll(validationResult.getWarnings());
      }
    }

    logInfo("Writing archive to " + outputFile.getAbsolutePath());
    CLDFWriter writer = new CLDFWriter(prettyPrint, validate);
    writer.write(archive, outputFile);

    Map<String, Object> stats = new HashMap<>();
    stats.put("locations", Optional.ofNullable(archive.getLocations()).map(List::size).orElse(0));
    stats.put("sessions", Optional.ofNullable(archive.getSessions()).map(List::size).orElse(0));
    stats.put("climbs", Optional.ofNullable(archive.getClimbs()).map(List::size).orElse(0));
    stats.put("media", Optional.ofNullable(archive.getMediaItems()).map(List::size).orElse(0));

    Map<String, Object> resultData = Map.of("file", outputFile.getAbsolutePath(), "stats", stats);

    return CommandResult.builder()
        .success(true)
        .message("Successfully created CLDF archive")
        .data(resultData)
        .warnings(warnings.isEmpty() ? null : warnings)
        .build();
  }

  @Override
  protected void outputText(CommandResult result) {
    if (result.isSuccess()) {
      output.write("Successfully created CLDF archive: " + outputFile.getName());
      Optional.ofNullable(result.getWarnings())
          .filter(warnings -> !warnings.isEmpty())
          .ifPresent(
              warnings -> {
                output.write("\nWarnings:");
                warnings.forEach(w -> output.write("  - " + w));
              });
    } else {
      output.writeError("Failed to create CLDF archive: " + result.getMessage());
      Optional.ofNullable(result.getData())
          .filter(Map.class::isInstance)
          .map(Map.class::cast)
          .filter(data -> data.containsKey(ERRORS))
          .ifPresent(
              data -> {
                output.writeError("\nErrors:", true);
                Optional.ofNullable(data.get(ERRORS))
                    .filter(List.class::isInstance)
                    .map(List.class::cast)
                    .ifPresent(errors -> errors.forEach(e -> output.writeError("  - " + e, true)));
              });
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
            .platform(Platform.DESKTOP)
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
            .id(1)
            .date(LocalDate.now())
            .location("Default Location")
            .locationId(1)
            .isIndoor(true)
            .build();

    // Create a minimal climb to satisfy validation
    Climb minimalClimb =
        Climb.builder()
            .id(1)
            .sessionId(1)
            .date(session.getDate())
            .routeName("Sample Route")
            .type(ClimbType.BOULDER)
            .finishType(FinishType.TOP) // Use enum value
            .attempts(1)
            .grades(Climb.GradeInfo.builder().system(GradeSystem.V_SCALE).grade("V0").build())
            .isIndoor(true)
            .build();

    return CLDFArchive.builder()
        .manifest(manifest)
        .locations(List.of(location))
        .sessions(List.of(session))
        .climbs(List.of(minimalClimb))
        .checksums(Checksums.builder().algorithm(ALGORITHM).build())
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
            .id(1)
            .date(LocalDate.now())
            .location(location.getName())
            .locationId(1)
            .isIndoor(true)
            .build();

    List<Climb> climbs =
        List.of(
            Climb.builder()
                .id(1)
                .sessionId(1)
                .date(session.getDate())
                .routeName("Warm-up V0")
                .type(ClimbType.BOULDER)
                .finishType(FinishType.TOP) // Use enum value
                .attempts(1)
                .grades(Climb.GradeInfo.builder().system(GradeSystem.V_SCALE).grade("V0").build())
                .isIndoor(true)
                .build(),
            Climb.builder()
                .id(2)
                .sessionId(1)
                .date(session.getDate())
                .routeName("Project V4")
                .type(ClimbType.BOULDER)
                .finishType(FinishType.TOP) // Use enum value
                .attempts(5)
                .grades(Climb.GradeInfo.builder().system(GradeSystem.V_SCALE).grade("V4").build())
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
            .platform(Platform.DESKTOP)
            .stats(
                Manifest.Stats.builder().locationsCount(1).sessionsCount(1).climbsCount(2).build())
            .build();

    return CLDFArchive.builder()
        .manifest(manifest)
        .locations(List.of(location))
        .sessions(List.of(session))
        .climbs(climbs)
        .checksums(Checksums.builder().algorithm(ALGORITHM).build())
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
                .rockType(RockType.SANDSTONE)
                .coordinates(
                    Location.Coordinates.builder().latitude(39.9308).longitude(-105.2925).build())
                .createdAt(OffsetDateTime.now())
                .build());

    // Create sessions
    List<Session> sessions =
        List.of(
            Session.builder()
                .id(1)
                .date(LocalDate.now().minusDays(7))
                .location(locations.get(0).getName())
                .locationId(1)
                .isIndoor(true)
                .sessionType(SessionType.INDOOR_CLIMBING)
                .build(),
            Session.builder()
                .id(2)
                .date(LocalDate.now().minusDays(2))
                .location(locations.get(1).getName())
                .locationId(2)
                .isIndoor(false)
                .climbType(ClimbType.ROUTE)
                .weather(
                    Session.Weather.builder()
                        .conditions("sunny")
                        .temperature(22.0)
                        .humidity(45.0)
                        .build())
                .partners(List.of("Alex", "Sarah"))
                .build());

    // Create climbs using streams
    List<Climb> gymClimbs =
        IntStream.range(0, 8)
            .mapToObj(
                i ->
                    Climb.builder()
                        .id(i + 1)
                        .sessionId(1)
                        .date(sessions.get(0).getDate())
                        .routeName("Problem " + (i + 1))
                        .type(ClimbType.BOULDER)
                        .finishType(FinishType.TOP) // Use enum value
                        .attempts(i < 5 ? 1 : i - 3)
                        .grades(
                            Climb.GradeInfo.builder()
                                .system(GradeSystem.V_SCALE)
                                .grade("V" + (i / 2))
                                .build())
                        .isIndoor(true)
                        .rating(i % 2 == 0 ? 4 : 3)
                        .build())
            .toList();

    // Outdoor session climb
    Climb outdoorClimb =
        Climb.builder()
            .id(9)
            .sessionId(2)
            .date(sessions.get(1).getDate())
            .routeName("The Bastille Crack")
            .type(ClimbType.ROUTE)
            .finishType(FinishType.ONSIGHT) // Use enum value
            .attempts(1)
            .grades(Climb.GradeInfo.builder().system(GradeSystem.YDS).grade("5.7").build())
            .belayType(BelayType.LEAD)
            .height(110.0)
            .rating(5)
            .notes("Classic route! Perfect hand jams all the way.")
            .isIndoor(false)
            .rockType(RockType.SANDSTONE)
            .partners(sessions.get(1).getPartners())
            .weather("sunny")
            .build();

    // Combine gym and outdoor climbs
    List<Climb> climbs =
        Stream.concat(gymClimbs.stream(), Stream.of(outdoorClimb)).collect(Collectors.toList());

    Manifest manifest =
        Manifest.builder()
            .version("1.0.0")
            .format("CLDF")
            .creationDate(OffsetDateTime.now())
            .appVersion("1.0.0")
            .platform(Platform.DESKTOP)
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
        .checksums(Checksums.builder().algorithm(ALGORITHM).build())
        .build();
  }

  private CLDFArchive createFromJson() throws IOException {
    InputHandler inputHandler = new InputHandler();

    if (readFromStdin || "-".equals(jsonInput)) {
      logInfo("Reading JSON from stdin...");
      return inputHandler.readJsonFromStdin(CLDFArchive.class);
    } else if (jsonInput != null) {
      logInfo("Reading JSON from file: " + jsonInput);
      return inputHandler.readJsonFromFile(new File(jsonInput), CLDFArchive.class);
    } else {
      throw new IllegalStateException("No JSON input source specified");
    }
  }

  private void processMediaFiles(CLDFArchive archive) throws IOException {
    if (!mediaDirectory.exists() || !mediaDirectory.isDirectory()) {
      throw new IOException("Media directory does not exist: " + mediaDirectory);
    }

    logInfo("Processing media files from: " + mediaDirectory);

    // Supported media file extensions
    Set<String> supportedExtensions =
        Set.of(
            ".jpg",
            ".jpeg",
            ".png",
            ".gif",
            ".bmp",
            ".webp", // Images
            ".mp4",
            ".mov",
            ".avi",
            ".webm",
            ".mkv" // Videos
            );

    // Build a map of climb IDs for matching
    Set<String> climbIds = new HashSet<>();
    if (archive.getClimbs() != null) {
      archive.getClimbs().forEach(climb -> climbIds.add(climb.getId().toString()));
    }

    // Scan for media files
    Map<String, byte[]> mediaFiles = new HashMap<>();
    List<MediaItem> mediaItems = new ArrayList<>();

    try (Stream<Path> paths = Files.walk(mediaDirectory.toPath())) {
      List<Path> mediaFilePaths =
          paths
              .filter(Files::isRegularFile)
              .filter(
                  path -> {
                    String name = path.getFileName().toString().toLowerCase();
                    return supportedExtensions.stream().anyMatch(name::endsWith);
                  })
              .collect(Collectors.toList());

      int mediaId = 1;
      for (Path filePath : mediaFilePaths) {
        String filename = filePath.getFileName().toString();

        // Extract climb ID from filename (e.g., "climb1_photo.jpg" -> "climb1")
        String climbId = extractClimbIdFromFilename(filename, climbIds);
        if (climbId == null) {
          logWarning("Skipping media file without matching climb ID: " + filename);
          continue;
        }

        String relativePath = mediaDirectory.toPath().relativize(filePath).toString();
        String mediaPath = "media/" + relativePath.replace(File.separatorChar, '/');

        // Read file content for embedding
        if (mediaStrategy == MediaStrategy.FULL) {
          byte[] content = Files.readAllBytes(filePath);
          mediaFiles.put(mediaPath, content);
        }

        // Create media metadata
        MediaType type = determineMediaType(filename);

        MediaItem item =
            MediaItem.builder()
                .id(mediaId++)
                .climbId(climbId)
                .type(type)
                .source(MediaSource.LOCAL)
                .filename(filename)
                .embedded(mediaStrategy == MediaStrategy.FULL)
                .build();

        mediaItems.add(item);
        logInfo("Added media: " + filename + " -> " + mediaPath);
      }
    }

    // Update archive with media
    if (!mediaItems.isEmpty()) {
      archive.setMediaItems(mediaItems);
      if (mediaStrategy == MediaStrategy.FULL && !mediaFiles.isEmpty()) {
        archive.setMediaFiles(mediaFiles);
      }
      logInfo("Added " + mediaItems.size() + " media files to archive");
    } else {
      logInfo("No media files found in directory");
    }
  }

  private String extractClimbIdFromFilename(String filename, Set<String> climbIds) {
    // Try to match climb ID at the beginning of the filename
    for (String climbId : climbIds) {
      if (filename.startsWith(climbId + "_") || filename.startsWith(climbId + ".")) {
        return climbId;
      }
    }

    // Try to match climb ID anywhere in the filename
    for (String climbId : climbIds) {
      if (filename.contains(climbId)) {
        return climbId;
      }
    }

    return null;
  }

  private MediaType determineMediaType(String filename) {
    String lower = filename.toLowerCase();
    if (lower.matches(".*\\.(mp4|mov|avi|webm|mkv)$")) {
      return MediaType.VIDEO;
    }
    return MediaType.PHOTO;
  }
}
