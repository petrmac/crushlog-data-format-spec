package io.cldf.tool.commands;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import io.cldf.api.CLDFArchive;
import io.cldf.models.*;
import io.cldf.tool.models.CommandResult;
import io.cldf.tool.services.CLDFService;
import io.cldf.tool.services.QueryService;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Slf4j
@Command(
    name = "query",
    description = "Query data from a CLDF archive",
    mixinStandardHelpOptions = true)
public class QueryCommand extends BaseCommand {

  @Parameters(index = "0", description = "CLDF file to query")
  private File inputFile;

  @Option(
      names = {"--select", "-s"},
      description = "Select specific data type: ${COMPLETION-CANDIDATES}",
      defaultValue = "all")
  private DataType selectType;

  @Option(
      names = {"--filter", "-f"},
      description = "Filter expression (e.g., 'type=boulder', 'grade>=V4', 'location=Gym')")
  private String filter;

  @Option(
      names = {"--limit", "-l"},
      description = "Limit number of results")
  private Integer limit;

  @Option(
      names = {"--offset"},
      description = "Skip first N results")
  private Integer offset;

  @Option(
      names = {"--sort"},
      description = "Sort by field (e.g., 'date', '-grade' for descending)")
  private String sortBy;

  @Option(
      names = {"--count"},
      description = "Return count only")
  private boolean countOnly;

  @Option(
      names = {"--stats"},
      description = "Include statistics")
  private boolean includeStats;

  @Option(
      names = {"--fields"},
      description = "Comma-separated list of fields to include")
  private String fields;

  @Inject private CLDFService cldfService;
  @Inject private QueryService queryService;

  enum DataType {
    all,
    climbs,
    sessions,
    locations,
    routes,
    sectors,
    tags,
    media,
    manifest
  }

  @Override
  protected CommandResult execute() throws Exception {
    if (!inputFile.exists()) {
      return CommandResult.builder()
          .success(false)
          .message("File not found: " + inputFile.getAbsolutePath())
          .exitCode(1)
          .build();
    }

    logInfo("Querying: " + inputFile.getName());

    // Read the archive
    CLDFArchive archive = cldfService.read(inputFile);

    // Perform query
    QueryResult queryResult = performQuery(archive);

    // Build result
    Map<String, Object> resultData = new HashMap<>();

    if (countOnly) {
      resultData.put("count", queryResult.getCount());
    } else {
      resultData.put("results", queryResult.getResults());
      resultData.put("count", queryResult.getCount());

      if (includeStats) {
        resultData.put("stats", queryResult.getStats());
      }
    }

    resultData.put("query", buildQueryInfo());

    return CommandResult.builder()
        .success(true)
        .message("Query completed")
        .data(resultData)
        .build();
  }

  @Override
  protected void outputText(CommandResult result) {
    if (!result.isSuccess()) {
      output.writeError(result.getMessage());
      return;
    }

    Map<String, Object> data = (Map<String, Object>) result.getData();

    if (countOnly) {
      output.write("Count: " + data.get("count"));
      return;
    }

    List<?> results = (List<?>) data.get("results");
    int count = (int) data.get("count");

    if (results.isEmpty()) {
      output.write("No results found.");
      return;
    }

    // Display results based on type
    if (selectType == DataType.climbs) {
      displayClimbs((List<Climb>) results);
    } else if (selectType == DataType.sessions) {
      displaySessions((List<Session>) results);
    } else if (selectType == DataType.locations) {
      displayLocations((List<Location>) results);
    } else {
      // Generic display
      output.write(String.format("Found %d results:", count));
      results.forEach(item -> output.write("  - " + item.toString()));
    }

    if (includeStats && data.containsKey("stats")) {
      output.write(
          """

          Statistics:
          -----------""");
      Map<String, Object> stats = (Map<String, Object>) data.get("stats");
      stats.forEach((key, value) -> output.write("  " + key + ": " + value));
    }
  }

  private QueryResult performQuery(CLDFArchive archive) {
    List<Object> allItems = new ArrayList<>();
    List<Object> filteredItems;

    // Select data based on type
    switch (selectType) {
      case climbs:
        allItems.addAll(
            archive.getClimbs() != null ? archive.getClimbs() : Collections.emptyList());
        break;
      case sessions:
        allItems.addAll(
            archive.getSessions() != null ? archive.getSessions() : Collections.emptyList());
        break;
      case locations:
        allItems.addAll(
            archive.getLocations() != null ? archive.getLocations() : Collections.emptyList());
        break;
      case routes:
        if (archive.hasRoutes()) {
          allItems.addAll(archive.getRoutes());
        }
        break;
      case sectors:
        if (archive.hasSectors()) {
          allItems.addAll(archive.getSectors());
        }
        break;
      case tags:
        if (archive.hasTags()) {
          allItems.addAll(archive.getTags());
        }
        break;
      case media:
        if (archive.hasMedia()) {
          allItems.addAll(archive.getMediaItems());
        }
        break;
      case manifest:
        allItems.add(archive.getManifest());
        break;
      case all:
      default:
        // Return summary of all data
        Map<String, Object> summary = new HashMap<>();
        summary.put("manifest", archive.getManifest());
        summary.put(
            "locationsCount", archive.getLocations() != null ? archive.getLocations().size() : 0);
        summary.put(
            "sessionsCount", archive.getSessions() != null ? archive.getSessions().size() : 0);
        summary.put("climbsCount", archive.getClimbs() != null ? archive.getClimbs().size() : 0);
        allItems.add(summary);
        break;
    }

    // Apply filter if provided
    if (filter != null && !filter.isEmpty()) {
      filteredItems = queryService.applyFilter(allItems, filter);
    } else {
      filteredItems = new ArrayList<>(allItems);
    }

    // Apply sorting if provided
    if (sortBy != null && !sortBy.isEmpty()) {
      filteredItems = queryService.sort(filteredItems, sortBy);
    }

    // Apply offset
    if (offset != null && offset > 0) {
      filteredItems = filteredItems.stream().skip(offset).collect(Collectors.toList());
    }

    // Apply limit
    if (limit != null && limit > 0) {
      filteredItems = filteredItems.stream().limit(limit).collect(Collectors.toList());
    }

    // Filter fields if specified
    if (fields != null && !fields.isEmpty()) {
      filteredItems = queryService.filterFields(filteredItems, Arrays.asList(fields.split(",")));
    }

    // Calculate statistics if requested
    Map<String, Object> stats = null;
    if (includeStats) {
      stats = queryService.calculateStatistics(filteredItems, selectType.name());
    }

    return QueryResult.builder()
        .results(filteredItems)
        .count(filteredItems.size())
        .totalCount(allItems.size())
        .stats(stats)
        .build();
  }

  private Map<String, Object> buildQueryInfo() {
    Map<String, Object> queryInfo = new HashMap<>();
    queryInfo.put("select", selectType.name());
    if (filter != null) queryInfo.put("filter", filter);
    if (sortBy != null) queryInfo.put("sort", sortBy);
    if (limit != null) queryInfo.put("limit", limit);
    if (offset != null) queryInfo.put("offset", offset);
    if (fields != null) queryInfo.put("fields", fields);
    return queryInfo;
  }

  private void displayClimbs(List<Climb> climbs) {
    output.write(String.format("Found %d climbs:", climbs.size()));
    for (Climb climb : climbs) {
      StringBuilder sb = new StringBuilder();
      sb.append("  - ").append(climb.getRouteName() != null ? climb.getRouteName() : "Unnamed");
      if (climb.getGrades() != null) {
        sb.append(" (").append(climb.getGrades().getGrade()).append(")");
      }
      if (climb.getType() != null) {
        sb.append(" - ").append(climb.getType());
      }
      if (climb.getDate() != null) {
        sb.append(" - ").append(climb.getDate());
      }
      output.write(sb.toString());
    }
  }

  private void displaySessions(List<Session> sessions) {
    output.write(String.format("Found %d sessions:", sessions.size()));
    for (Session session : sessions) {
      StringBuilder sb = new StringBuilder();
      sb.append("  - ").append(session.getDate());
      sb.append(" at ").append(session.getLocation());
      if (session.getSessionType() != null) {
        sb.append(" (").append(session.getSessionType()).append(")");
      }
      output.write(sb.toString());
    }
  }

  private void displayLocations(List<Location> locations) {
    output.write(String.format("Found %d locations:", locations.size()));
    for (Location location : locations) {
      StringBuilder sb = new StringBuilder();
      sb.append("  - ").append(location.getName());
      if (location.getCountry() != null) {
        sb.append(" - ").append(location.getCountry());
        if (location.getState() != null) {
          sb.append(", ").append(location.getState());
        }
      }
      sb.append(location.getIsIndoor() ? " (Indoor)" : " (Outdoor)");
      output.write(sb.toString());
    }
  }

  @lombok.Data
  @lombok.Builder
  private static class QueryResult {
    private List<Object> results;
    private int count;
    private int totalCount;
    private Map<String, Object> stats;
  }
}
