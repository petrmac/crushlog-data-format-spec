package app.crushlog.cldf.tool.commands;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.Arrays;
import java.util.List;

import app.crushlog.cldf.api.CLDF;
import app.crushlog.cldf.api.CLDFArchive;
import app.crushlog.cldf.models.*;
import app.crushlog.cldf.tool.models.CommandResult;
import app.crushlog.cldf.tool.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Slf4j
@Command(
    name = "extract",
    description = "Extract contents from a CLDF archive",
    mixinStandardHelpOptions = true)
public class ExtractCommand extends BaseCommand {

  @Parameters(index = "0", description = "CLDF file to extract")
  private File inputFile;

  @Option(
      names = {"-o", "--output"},
      description = "Output directory",
      defaultValue = ".")
  private File outputDirectory;

  @Option(names = "--files", description = "Specific files to extract (comma-separated)")
  private String files;

  @Option(
      names = "--preserve-structure",
      description = "Keep archive structure",
      defaultValue = "true")
  private boolean preserveStructure;

  @Option(names = "--pretty-print", description = "Format JSON output", defaultValue = "true")
  private boolean prettyPrint;

  @Option(names = "--overwrite", description = "Overwrite existing files", defaultValue = "false")
  private boolean overwrite;

  // No injection needed, using static CLDF methods

  @Override
  protected CommandResult execute() throws Exception {
    if (!inputFile.exists()) {
      return CommandResult.builder()
          .success(false)
          .message("File not found: " + inputFile.getAbsolutePath())
          .exitCode(1)
          .build();
    }

    logInfo("Extracting: " + inputFile.getName());

    // Read the archive
    CLDFArchive archive = CLDF.read(inputFile);

    // Create output directory
    Path outputPath = outputDirectory.toPath();
    if (!Files.exists(outputPath)) {
      Files.createDirectories(outputPath);
      logInfo("Created output directory: " + outputPath);
    }

    // Extract files
    List<String> filesToExtract = parseFilesToExtract();
    ExtractResult result = extractFiles(archive, outputPath, filesToExtract);

    return CommandResult.builder()
        .success(true)
        .message("Extracted " + result.count + " files to " + outputPath)
        .data(result.toMap())
        .build();
  }

  @Override
  protected void outputText(CommandResult result) {
    if (!result.isSuccess()) {
      output.writeError(result.getMessage());
      return;
    }

    output.write(result.getMessage());

    var data = (java.util.Map<String, Object>) result.getData();
    if (!quiet && data.containsKey("files")) {
      var files = (java.util.List<String>) data.get("files");
      var stats = (java.util.Map<String, Integer>) data.get("stats");

      output.write(
          """

          Extracted Files
          ===============
          Total: %d files
          """
              .formatted(files.size()));

      if (!stats.isEmpty()) {
        output.write("\nBy Type:");
        stats.forEach((type, count) -> output.write("  %s: %d".formatted(type, count)));
      }

      output.write("\nFiles:");
      files.forEach(f -> output.write("  - " + f));
    }
  }

  private List<String> parseFilesToExtract() {
    if (files == null || files.trim().isEmpty()) {
      return null; // Extract all files
    }
    return Arrays.asList(files.split(","));
  }

  private ExtractResult extractFiles(
      CLDFArchive archive, Path outputPath, List<String> filesToExtract) throws IOException {
    ExtractResult result = new ExtractResult();
    int count = 0;

    // Extract manifest
    if (shouldExtract("manifest.json", filesToExtract) && archive.getManifest() != null) {
      writeJsonFile(outputPath, "manifest.json", archive.getManifest());
      result.addFile("manifest.json");
      count++;
    }

    // Extract locations
    if (shouldExtract("locations.json", filesToExtract) && archive.getLocations() != null) {
      LocationsFile locationsFile =
          LocationsFile.builder().locations(archive.getLocations()).build();
      writeJsonFile(outputPath, "locations.json", locationsFile);
      result.addFile("locations.json");
      count++;
    }

    // Extract sessions
    if (shouldExtract("sessions.json", filesToExtract) && archive.getSessions() != null) {
      SessionsFile sessionsFile = SessionsFile.builder().sessions(archive.getSessions()).build();
      writeJsonFile(outputPath, "sessions.json", sessionsFile);
      result.addFile("sessions.json");
      count++;
    }

    // Extract climbs
    if (shouldExtract("climbs.json", filesToExtract) && archive.getClimbs() != null) {
      ClimbsFile climbsFile = ClimbsFile.builder().climbs(archive.getClimbs()).build();
      writeJsonFile(outputPath, "climbs.json", climbsFile);
      result.addFile("climbs.json");
      count++;
    }

    // Extract routes
    if (shouldExtract("routes.json", filesToExtract) && archive.hasRoutes()) {
      RoutesFile routesFile = RoutesFile.builder().routes(archive.getRoutes()).build();
      writeJsonFile(outputPath, "routes.json", routesFile);
      result.addFile("routes.json");
      count++;
    }

    // Extract sectors
    if (shouldExtract("sectors.json", filesToExtract) && archive.hasSectors()) {
      SectorsFile sectorsFile = SectorsFile.builder().sectors(archive.getSectors()).build();
      writeJsonFile(outputPath, "sectors.json", sectorsFile);
      result.addFile("sectors.json");
      count++;
    }

    // Extract tags
    if (shouldExtract("tags.json", filesToExtract) && archive.hasTags()) {
      TagsFile tagsFile = TagsFile.builder().tags(archive.getTags()).build();
      writeJsonFile(outputPath, "tags.json", tagsFile);
      result.addFile("tags.json");
      count++;
    }

    // Extract media metadata
    if (shouldExtract("media-metadata.json", filesToExtract) && archive.hasMedia()) {
      MediaMetadataFile mediaFile =
          MediaMetadataFile.builder().media(archive.getMediaItems()).build();
      writeJsonFile(outputPath, "media-metadata.json", mediaFile);
      result.addFile("media-metadata.json");
      count++;
    }

    // Extract checksums
    if (shouldExtract("checksums.json", filesToExtract) && archive.getChecksums() != null) {
      writeJsonFile(outputPath, "checksums.json", archive.getChecksums());
      result.addFile("checksums.json");
      count++;
    }

    // Extract embedded media files
    if (archive.hasEmbeddedMedia()) {
      for (var entry : archive.getMediaFiles().entrySet()) {
        if (shouldExtract(entry.getKey(), filesToExtract)) {
          Path mediaPath = outputPath.resolve(entry.getKey());
          Files.createDirectories(mediaPath.getParent());
          Files.write(mediaPath, entry.getValue());
          logInfo("Extracted: " + entry.getKey());
          result.addFile(entry.getKey());
          count++;
        }
      }
    }

    result.count = count;
    return result;
  }

  private boolean shouldExtract(String filename, List<String> filesToExtract) {
    if (filesToExtract == null) {
      return true; // Extract all files
    }
    return filesToExtract.stream()
        .anyMatch(f -> f.trim().equalsIgnoreCase(filename) || filename.startsWith(f.trim() + "/"));
  }

  private void writeJsonFile(Path outputPath, String filename, Object data) throws IOException {
    Path filePath = outputPath.resolve(filename);

    if (Files.exists(filePath) && !overwrite) {
      logWarning("Skipping existing file: " + filename);
      return;
    }

    String json = JsonUtils.toJson(data, prettyPrint);
    Files.writeString(filePath, json);
    logInfo("Extracted: " + filename);
  }

  private static class ExtractResult {
    int count = 0;
    List<String> files = new ArrayList<>();
    Map<String, Integer> stats = new HashMap<>();

    void addFile(String filename) {
      files.add(filename);
      // Track stats by type
      String type = filename.replace(".json", "");
      stats.put(type, stats.getOrDefault(type, 0) + 1);
    }

    Map<String, Object> toMap() {
      Map<String, Object> map = new HashMap<>();
      map.put("count", count);
      map.put("files", files);
      map.put("stats", stats);
      return map;
    }
  }
}
