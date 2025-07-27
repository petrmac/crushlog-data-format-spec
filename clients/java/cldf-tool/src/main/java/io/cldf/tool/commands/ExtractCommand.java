package io.cldf.tool.commands;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import io.cldf.api.CLDF;
import io.cldf.api.CLDFArchive;
import io.cldf.models.*;
import io.cldf.tool.utils.ConsoleUtils;
import io.cldf.tool.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Slf4j
@Command(
    name = "extract",
    description = "Extract contents from a CLDF archive",
    mixinStandardHelpOptions = true)
public class ExtractCommand implements Runnable {

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
  public void run() {
    try {
      if (!inputFile.exists()) {
        log.error("File not found: {}", inputFile.getAbsolutePath());
        System.exit(1);
      }

      ConsoleUtils.printHeader("CLDF Extract");
      ConsoleUtils.printInfo("Extracting: " + inputFile.getName());

      // Read the archive
      CLDFArchive archive = CLDF.read(inputFile);

      // Create output directory
      Path outputPath = outputDirectory.toPath();
      if (!Files.exists(outputPath)) {
        Files.createDirectories(outputPath);
        ConsoleUtils.printInfo("Created output directory: " + outputPath);
      }

      // Extract files
      List<String> filesToExtract = parseFilesToExtract();
      int extracted = extractFiles(archive, outputPath, filesToExtract);

      ConsoleUtils.printSuccess("Extracted " + extracted + " files to " + outputPath);

    } catch (Exception e) {
      log.error("Extraction failed", e);
      System.exit(1);
    }
  }

  private List<String> parseFilesToExtract() {
    if (files == null || files.trim().isEmpty()) {
      return null; // Extract all files
    }
    return Arrays.asList(files.split(","));
  }

  private int extractFiles(CLDFArchive archive, Path outputPath, List<String> filesToExtract)
      throws IOException {
    int count = 0;

    // Extract manifest
    if (shouldExtract("manifest.json", filesToExtract) && archive.getManifest() != null) {
      writeJsonFile(outputPath, "manifest.json", archive.getManifest());
      count++;
    }

    // Extract locations
    if (shouldExtract("locations.json", filesToExtract) && archive.getLocations() != null) {
      LocationsFile locationsFile =
          LocationsFile.builder().locations(archive.getLocations()).build();
      writeJsonFile(outputPath, "locations.json", locationsFile);
      count++;
    }

    // Extract sessions
    if (shouldExtract("sessions.json", filesToExtract) && archive.getSessions() != null) {
      SessionsFile sessionsFile = SessionsFile.builder().sessions(archive.getSessions()).build();
      writeJsonFile(outputPath, "sessions.json", sessionsFile);
      count++;
    }

    // Extract climbs
    if (shouldExtract("climbs.json", filesToExtract) && archive.getClimbs() != null) {
      ClimbsFile climbsFile = ClimbsFile.builder().climbs(archive.getClimbs()).build();
      writeJsonFile(outputPath, "climbs.json", climbsFile);
      count++;
    }

    // Extract routes
    if (shouldExtract("routes.json", filesToExtract) && archive.hasRoutes()) {
      RoutesFile routesFile = RoutesFile.builder().routes(archive.getRoutes()).build();
      writeJsonFile(outputPath, "routes.json", routesFile);
      count++;
    }

    // Extract sectors
    if (shouldExtract("sectors.json", filesToExtract) && archive.hasSectors()) {
      SectorsFile sectorsFile = SectorsFile.builder().sectors(archive.getSectors()).build();
      writeJsonFile(outputPath, "sectors.json", sectorsFile);
      count++;
    }

    // Extract tags
    if (shouldExtract("tags.json", filesToExtract) && archive.hasTags()) {
      TagsFile tagsFile = TagsFile.builder().tags(archive.getTags()).build();
      writeJsonFile(outputPath, "tags.json", tagsFile);
      count++;
    }

    // Extract media metadata
    if (shouldExtract("media-metadata.json", filesToExtract) && archive.hasMedia()) {
      MediaMetadataFile mediaFile =
          MediaMetadataFile.builder().media(archive.getMediaItems()).build();
      writeJsonFile(outputPath, "media-metadata.json", mediaFile);
      count++;
    }

    // Extract checksums
    if (shouldExtract("checksums.json", filesToExtract) && archive.getChecksums() != null) {
      writeJsonFile(outputPath, "checksums.json", archive.getChecksums());
      count++;
    }

    // Extract embedded media files
    if (archive.hasEmbeddedMedia()) {
      for (var entry : archive.getMediaFiles().entrySet()) {
        if (shouldExtract(entry.getKey(), filesToExtract)) {
          Path mediaPath = outputPath.resolve(entry.getKey());
          Files.createDirectories(mediaPath.getParent());
          Files.write(mediaPath, entry.getValue());
          ConsoleUtils.printInfo("Extracted: " + entry.getKey());
          count++;
        }
      }
    }

    return count;
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
      ConsoleUtils.printWarning("Skipping existing file: " + filename);
      return;
    }

    String json = JsonUtils.toJson(data, prettyPrint);
    Files.writeString(filePath, json);
    ConsoleUtils.printInfo("Extracted: " + filename);
  }
}
