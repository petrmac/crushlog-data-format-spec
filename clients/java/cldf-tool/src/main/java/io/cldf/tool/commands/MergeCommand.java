package io.cldf.tool.commands;

import java.io.File;
import java.time.OffsetDateTime;
import java.util.*;

import io.cldf.api.CLDF;
import io.cldf.api.CLDFArchive;
import io.cldf.api.CLDFWriter;
import io.cldf.models.*;
import io.cldf.tool.models.CommandResult;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Slf4j
@Command(
    name = "merge",
    description = "Merge multiple CLDF archives",
    mixinStandardHelpOptions = true)
public class MergeCommand extends BaseCommand {

  @Parameters(arity = "2..*", description = "CLDF files to merge")
  private List<File> inputFiles;

  @Option(
      names = {"-o", "--output"},
      description = "Output CLDF file",
      required = true)
  private File outputFile;

  @Option(
      names = "--strategy",
      description = "Merge strategy: ${COMPLETION-CANDIDATES}",
      defaultValue = "append")
  private MergeStrategy strategy;

  @Option(
      names = "--pretty-print",
      description = "Pretty print JSON in archive",
      defaultValue = "true")
  private boolean prettyPrint;

  enum MergeStrategy {
    append // Simply append all data
  }

  @Override
  protected CommandResult execute() throws Exception {
    // Validate input files
    for (File file : inputFiles) {
      if (!file.exists()) {
        return CommandResult.builder()
            .success(false)
            .message("File not found: " + file.getAbsolutePath())
            .exitCode(1)
            .build();
      }
    }

    logInfo("Merging " + inputFiles.size() + " archives");

    // Read all archives
    List<CLDFArchive> archives = new ArrayList<>();
    Map<String, Object> sourceStats = new HashMap<>();

    for (int i = 0; i < inputFiles.size(); i++) {
      logInfo(
          "Reading archive "
              + (i + 1)
              + " of "
              + inputFiles.size()
              + ": "
              + inputFiles.get(i).getName());
      CLDFArchive archive = CLDF.read(inputFiles.get(i));
      archives.add(archive);

      // Collect stats from each archive
      Map<String, Object> stats = new HashMap<>();
      stats.put("locations", archive.getLocations() != null ? archive.getLocations().size() : 0);
      stats.put("sessions", archive.getSessions() != null ? archive.getSessions().size() : 0);
      stats.put("climbs", archive.getClimbs() != null ? archive.getClimbs().size() : 0);
      sourceStats.put(inputFiles.get(i).getName(), stats);
    }

    // Perform merge
    MergeResult mergeResult = mergeArchives(archives);

    // Write result
    logInfo("Writing merged archive to " + outputFile.getAbsolutePath());
    CLDFWriter writer = new CLDFWriter(prettyPrint);
    writer.write(mergeResult.archive, outputFile);

    // Build result data
    Map<String, Object> resultData = new HashMap<>();
    resultData.put(
        "inputFiles",
        inputFiles.stream().map(File::getName).collect(java.util.stream.Collectors.toList()));
    resultData.put("outputFile", outputFile.getAbsolutePath());
    resultData.put("strategy", strategy.name());
    resultData.put("sourceStats", sourceStats);
    resultData.put("mergedStats", mergeResult.stats);

    return CommandResult.builder()
        .success(true)
        .message("Successfully merged " + inputFiles.size() + " archives")
        .data(resultData)
        .build();
  }

  @Override
  protected void outputText(CommandResult result) {
    if (!result.isSuccess()) {
      output.writeError(result.getMessage());
      return;
    }

    output.write(result.getMessage());

    var data = (Map<String, Object>) result.getData();
    if (!quiet && data != null) {
      var stats = (Map<String, Object>) data.get("mergedStats");

      output.write(
          """

          Merge Summary
          =============
          Strategy: %s
          Output:   %s

          Merged Archive
          --------------
          Locations: %d
          Sessions:  %d
          Climbs:    %d
          """
              .formatted(
                  data.get("strategy"),
                  data.get("outputFile"),
                  stats.get("locations"),
                  stats.get("sessions"),
                  stats.get("climbs")));
    }
  }

  private MergeResult mergeArchives(List<CLDFArchive> archives) {
    // Simple append strategy
    List<Location> allLocations = new ArrayList<>();
    List<Session> allSessions = new ArrayList<>();
    List<Climb> allClimbs = new ArrayList<>();

    for (CLDFArchive archive : archives) {
      if (archive.getLocations() != null) {
        allLocations.addAll(archive.getLocations());
      }
      if (archive.getSessions() != null) {
        allSessions.addAll(archive.getSessions());
      }
      if (archive.getClimbs() != null) {
        allClimbs.addAll(archive.getClimbs());
      }
    }

    // Create merged manifest
    Manifest manifest =
        Manifest.builder()
            .version("1.0.0")
            .format("CLDF")
            .creationDate(OffsetDateTime.now())
            .appVersion("1.0.0")
            .platform(Manifest.Platform.Desktop)
            .stats(
                Manifest.Stats.builder()
                    .locationsCount(allLocations.size())
                    .sessionsCount(allSessions.size())
                    .climbsCount(allClimbs.size())
                    .build())
            .build();

    CLDFArchive mergedArchive =
        CLDFArchive.builder()
            .manifest(manifest)
            .locations(allLocations)
            .sessions(allSessions)
            .climbs(allClimbs)
            .checksums(
                Checksums.builder().algorithm("SHA-256").generatedAt(OffsetDateTime.now()).build())
            .build();

    Map<String, Object> stats = new HashMap<>();
    stats.put("locations", allLocations.size());
    stats.put("sessions", allSessions.size());
    stats.put("climbs", allClimbs.size());

    return new MergeResult(mergedArchive, stats);
  }

  private static class MergeResult {
    final CLDFArchive archive;
    final Map<String, Object> stats;

    MergeResult(CLDFArchive archive, Map<String, Object> stats) {
      this.archive = archive;
      this.stats = stats;
    }
  }
}
