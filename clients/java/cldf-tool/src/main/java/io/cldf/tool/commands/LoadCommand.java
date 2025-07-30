package io.cldf.tool.commands;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Inject;

import io.cldf.api.CLDF;
import io.cldf.api.CLDFArchive;
import io.cldf.tool.models.CommandResult;
import io.cldf.tool.services.GraphService;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Load a CLDF archive into the embedded Neo4j graph database. This enables powerful graph-based
 * queries and analysis.
 */
@Slf4j
@Command(
    name = "load",
    description = "Load CLDF archive into graph database for analysis",
    mixinStandardHelpOptions = true)
public class LoadCommand extends BaseCommand {

  @Parameters(index = "0", description = "CLDF archive file to load")
  private File archiveFile;

  @Option(
      names = {"--stats"},
      description = "Show import statistics",
      defaultValue = "true")
  private boolean showStats;

  @Option(
      names = {"--validate"},
      description = "Validate archive before loading",
      defaultValue = "true")
  private boolean validate;

  private final GraphService graphService;

  @Inject
  public LoadCommand(GraphService graphService) {
    this.graphService = graphService;
  }

  // For PicoCLI framework - it needs a no-arg constructor
  public LoadCommand() {
    this.graphService = null;
  }

  @Override
  protected CommandResult execute() throws Exception {
    if (!archiveFile.exists()) {
      return CommandResult.builder()
          .success(false)
          .message("File not found: " + archiveFile.getAbsolutePath())
          .exitCode(1)
          .build();
    }

    logInfo("Loading CLDF archive: " + archiveFile.getName());

    try {
      // Read the archive
      CLDFArchive archive = CLDF.read(archiveFile);

      // Initialize graph database
      logInfo("Initializing embedded Neo4j database");
      graphService.initialize();

      // Import into Neo4j
      long startTime = System.currentTimeMillis();
      graphService.importArchive(archive);
      long duration = System.currentTimeMillis() - startTime;

      // Collect statistics
      Map<String, Object> stats = new HashMap<>();
      if (showStats) {
        stats = collectStatistics();
      }
      stats.put("importTimeMs", duration);
      stats.put("file", archiveFile.getName());
      stats.put("fileSize", archiveFile.length());

      return CommandResult.builder()
          .success(true)
          .message("Successfully loaded archive into graph database")
          .data(stats)
          .build();

    } catch (Exception e) {
      log.error("Failed to load archive", e);
      return CommandResult.builder()
          .success(false)
          .message("Failed to load archive: " + e.getMessage())
          .exitCode(1)
          .build();
    }
  }

  @Override
  protected void outputText(CommandResult result) {
    if (!result.isSuccess()) {
      output.writeError(result.getMessage());
      return;
    }

    output.write(result.getMessage());

    if (showStats && result.getData() != null) {
      Map<String, Object> stats = (Map<String, Object>) result.getData();

      output.write(
          """

                Import Statistics
                =================
                File: %s (%.2f MB)
                Import time: %d ms

                Graph Contents:
                Locations: %d
                Sessions: %d
                Climbs: %d
                Routes: %d
                Climbers: %d
                Tags: %d

                Relationships:
                Partner connections: %d
                Session climbs: %d
                Route climbs: %d
                """
              .formatted(
                  stats.get("file"),
                  (Long) stats.get("fileSize") / (1024.0 * 1024.0),
                  stats.get("importTimeMs"),
                  stats.getOrDefault("locations", 0),
                  stats.getOrDefault("sessions", 0),
                  stats.getOrDefault("climbs", 0),
                  stats.getOrDefault("routes", 0),
                  stats.getOrDefault("climbers", 0),
                  stats.getOrDefault("tags", 0),
                  stats.getOrDefault("partnerRelationships", 0),
                  stats.getOrDefault("sessionClimbRelationships", 0),
                  stats.getOrDefault("routeClimbRelationships", 0)));

      output.write(
          "\nGraph database ready for queries. Use 'cldf graph-query' to analyze your data.");
    }
  }

  private Map<String, Object> collectStatistics() {
    Map<String, Object> stats = new HashMap<>();

    try {
      // Count nodes
      stats.put("locations", countNodes("MATCH (n:Location) RETURN COUNT(n) as count"));
      stats.put("sessions", countNodes("MATCH (n:Session) RETURN COUNT(n) as count"));
      stats.put("climbs", countNodes("MATCH (n:Climb) RETURN COUNT(n) as count"));
      stats.put("routes", countNodes("MATCH (n:Route) RETURN COUNT(n) as count"));
      stats.put("climbers", countNodes("MATCH (n:Climber) RETURN COUNT(n) as count"));
      stats.put("tags", countNodes("MATCH (n:Tag) RETURN COUNT(n) as count"));

      // Count relationships
      stats.put(
          "partnerRelationships",
          countNodes("MATCH ()-[r:PARTNERED_WITH]->() RETURN COUNT(r) as count"));
      stats.put(
          "sessionClimbRelationships",
          countNodes("MATCH ()-[r:INCLUDES_CLIMB]->() RETURN COUNT(r) as count"));
      stats.put(
          "routeClimbRelationships",
          countNodes("MATCH ()-[r:ON_ROUTE]->() RETURN COUNT(r) as count"));

    } catch (Exception e) {
      log.warn("Failed to collect statistics", e);
    }

    return stats;
  }

  private long countNodes(String query) {
    var results = graphService.executeCypher(query, new HashMap<>());
    if (!results.isEmpty()) {
      Map<String, Object> row = results.get(0);
      Object count = row.get("count");
      if (count instanceof Number) {
        return ((Number) count).longValue();
      }
    }
    return 0;
  }
}
