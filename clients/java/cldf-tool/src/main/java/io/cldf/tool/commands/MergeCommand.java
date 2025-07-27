package io.cldf.tool.commands;

import java.io.File;
import java.time.OffsetDateTime;
import java.util.*;

import io.cldf.api.CLDF;
import io.cldf.api.CLDFArchive;
import io.cldf.api.CLDFWriter;
import io.cldf.models.*;
import io.cldf.tool.utils.ConsoleUtils;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Slf4j
@Command(
    name = "merge",
    description = "Merge multiple CLDF archives",
    mixinStandardHelpOptions = true)
public class MergeCommand implements Runnable {

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
  public void run() {
    try {
      ConsoleUtils.printHeader("CLDF Merge");

      // Validate input files
      for (File file : inputFiles) {
        if (!file.exists()) {
          log.error("File not found: {}", file.getAbsolutePath());
          System.exit(1);
        }
      }

      ConsoleUtils.printInfo("Merging " + inputFiles.size() + " archives");

      // Read all archives
      List<CLDFArchive> archives = new ArrayList<>();
      for (int i = 0; i < inputFiles.size(); i++) {
        ConsoleUtils.printProgress(i + 1, inputFiles.size());
        archives.add(CLDF.read(inputFiles.get(i)));
      }

      // Perform merge
      CLDFArchive merged = mergeArchives(archives);

      // Write result
      ConsoleUtils.printInfo("Writing merged archive to " + outputFile.getAbsolutePath());
      CLDFWriter writer = new CLDFWriter(prettyPrint);
      writer.write(merged, outputFile);

      ConsoleUtils.printSuccess("Successfully merged " + inputFiles.size() + " archives");

    } catch (Exception e) {
      log.error("Merge failed", e);
      System.exit(1);
    }
  }

  private CLDFArchive mergeArchives(List<CLDFArchive> archives) {
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

    return CLDFArchive.builder()
        .manifest(manifest)
        .locations(allLocations)
        .sessions(allSessions)
        .climbs(allClimbs)
        .checksums(
            Checksums.builder().algorithm("SHA-256").generatedAt(OffsetDateTime.now()).build())
        .build();
  }
}
