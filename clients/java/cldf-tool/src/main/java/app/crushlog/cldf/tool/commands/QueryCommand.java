package app.crushlog.cldf.tool.commands;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import app.crushlog.cldf.api.CLDFArchive;
import app.crushlog.cldf.models.*;
import app.crushlog.cldf.tool.converters.DataTypeConverter;
import app.crushlog.cldf.tool.models.CommandResult;
import app.crushlog.cldf.tool.models.DataType;
import app.crushlog.cldf.tool.services.CLDFService;
import app.crushlog.cldf.tool.services.QueryService;
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
      description =
          "Select specific data type: locations, routes, sectors, climbs, sessions, tags, media, all (case-insensitive)",
      defaultValue = "all",
      converter = DataTypeConverter.class)
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

  @Option(
      names = {"--clid"},
      description = "Search for a specific CLID (CrushLog ID)")
  private String clid;

  private final CLDFService cldfService;
  private final QueryService queryService;

  @Inject
  public QueryCommand(CLDFService cldfService, QueryService queryService) {
    this.cldfService = cldfService;
    this.queryService = queryService;
  }

  // For PicoCLI framework - it needs a no-arg constructor
  public QueryCommand() {
    this.cldfService = null;
    this.queryService = null;
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
    if (selectType == DataType.CLIMBS) {
      displayClimbs((List<Climb>) results);
    } else if (selectType == DataType.SESSIONS) {
      displaySessions((List<Session>) results);
    } else if (selectType == DataType.LOCATIONS) {
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
    List<Object> allItems = collectInitialItems(archive);
    List<Object> processedItems = applyQueryProcessing(allItems);
    Map<String, Object> stats = calculateStatsIfRequested(processedItems);

    return QueryResult.builder()
        .results(processedItems)
        .count(processedItems.size())
        .totalCount(allItems.size())
        .stats(stats)
        .build();
  }

  /**
   * Collects initial items based on CLID search or data type selection.
   *
   * @param archive the archive to search
   * @return list of initial items
   */
  private List<Object> collectInitialItems(CLDFArchive archive) {
    if (clid != null && !clid.isEmpty()) {
      return searchByCLID(archive);
    } else {
      return selectDataByType(archive);
    }
  }

  /**
   * Searches for a specific item by CLID.
   *
   * @param archive the archive to search
   * @return list containing the found item or empty list
   */
  private List<Object> searchByCLID(CLDFArchive archive) {
    List<Object> items = new ArrayList<>();
    Object item = findByCLID(archive, clid);
    if (item != null) {
      items.add(item);
    }
    return items;
  }

  /**
   * Selects data based on the specified type.
   *
   * @param archive the archive to select from
   * @return list of selected items
   */
  private List<Object> selectDataByType(CLDFArchive archive) {
    List<Object> allItems = new ArrayList<>();

    switch (selectType) {
      case CLIMBS:
        addIfNotNull(allItems, archive.getClimbs());
        break;
      case SESSIONS:
        addIfNotNull(allItems, archive.getSessions());
        break;
      case LOCATIONS:
        addIfNotNull(allItems, archive.getLocations());
        break;
      case ROUTES:
        if (archive.hasRoutes()) {
          allItems.addAll(archive.getRoutes());
        }
        break;
      case SECTORS:
        if (archive.hasSectors()) {
          allItems.addAll(archive.getSectors());
        }
        break;
      case TAGS:
        if (archive.hasTags()) {
          allItems.addAll(archive.getTags());
        }
        break;
      case MEDIA:
        if (archive.hasMedia()) {
          allItems.addAll(archive.getMediaItems());
        }
        break;
      case MANIFEST:
        allItems.add(archive.getManifest());
        break;
      case ALL:
      default:
        allItems.add(createArchiveSummary(archive));
        break;
    }

    return allItems;
  }

  /**
   * Helper method to safely add non-null collections to the results.
   *
   * @param target the target list
   * @param source the source collection
   */
  private void addIfNotNull(List<Object> target, List<?> source) {
    if (source != null) {
      target.addAll(source);
    }
  }

  /**
   * Creates a summary object for 'all' data type queries.
   *
   * @param archive the archive to summarize
   * @return summary map
   */
  private Map<String, Object> createArchiveSummary(CLDFArchive archive) {
    Map<String, Object> summary = new HashMap<>();
    summary.put("manifest", archive.getManifest());
    summary.put("locationsCount", getCollectionSize(archive.getLocations()));
    summary.put("sessionsCount", getCollectionSize(archive.getSessions()));
    summary.put("climbsCount", getCollectionSize(archive.getClimbs()));
    return summary;
  }

  /**
   * Helper method to safely get collection size.
   *
   * @param collection the collection
   * @return size or 0 if null
   */
  private int getCollectionSize(List<?> collection) {
    return collection != null ? collection.size() : 0;
  }

  /**
   * Applies all query processing steps: filtering, sorting, pagination, and field selection.
   *
   * @param items the initial items
   * @return processed items
   */
  private List<Object> applyQueryProcessing(List<Object> items) {
    List<Object> processedItems = applyFilter(items);
    processedItems = applySorting(processedItems);
    processedItems = applyPagination(processedItems);
    processedItems = applyFieldSelection(processedItems);
    return processedItems;
  }

  /**
   * Applies filter if specified.
   *
   * @param items the items to filter
   * @return filtered items
   */
  private List<Object> applyFilter(List<Object> items) {
    if (filter != null && !filter.isEmpty()) {
      return queryService.applyFilter(items, filter);
    }
    return new ArrayList<>(items);
  }

  /**
   * Applies sorting if specified.
   *
   * @param items the items to sort
   * @return sorted items
   */
  private List<Object> applySorting(List<Object> items) {
    if (sortBy != null && !sortBy.isEmpty()) {
      return queryService.sort(items, sortBy);
    }
    return items;
  }

  /**
   * Applies pagination (offset and limit) if specified.
   *
   * @param items the items to paginate
   * @return paginated items
   */
  private List<Object> applyPagination(List<Object> items) {
    List<Object> paginatedItems = items;

    if (offset != null && offset > 0) {
      paginatedItems = paginatedItems.stream().skip(offset).collect(Collectors.toList());
    }

    if (limit != null && limit > 0) {
      paginatedItems = paginatedItems.stream().limit(limit).collect(Collectors.toList());
    }

    return paginatedItems;
  }

  /**
   * Applies field selection if specified.
   *
   * @param items the items to filter fields from
   * @return items with selected fields only
   */
  private List<Object> applyFieldSelection(List<Object> items) {
    if (fields != null && !fields.isEmpty()) {
      return queryService.filterFields(items, Arrays.asList(fields.split(",")));
    }
    return items;
  }

  /**
   * Calculates statistics if requested.
   *
   * @param items the items to calculate statistics for
   * @return statistics map or null if not requested
   */
  private Map<String, Object> calculateStatsIfRequested(List<Object> items) {
    if (includeStats) {
      return queryService.calculateStatistics(items, selectType.name());
    }
    return null;
  }

  private Map<String, Object> buildQueryInfo() {
    Map<String, Object> queryInfo = new HashMap<>();
    queryInfo.put("select", selectType.name().toLowerCase());
    if (clid != null) queryInfo.put("clid", clid);
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

  private Object findByCLID(CLDFArchive archive, String clid) {
    // Search in locations
    Optional<Location> location =
        Optional.ofNullable(archive.getLocations()).orElse(Collections.emptyList()).stream()
            .filter(l -> clid.equals(l.getClid()))
            .findFirst();
    if (location.isPresent()) return location.get();

    // Search in routes
    Optional<Route> route =
        archive.hasRoutes()
            ? archive.getRoutes().stream().filter(r -> clid.equals(r.getClid())).findFirst()
            : Optional.empty();
    if (route.isPresent()) return route.get();

    // Search in sectors
    Optional<Sector> sector =
        archive.hasSectors()
            ? archive.getSectors().stream().filter(s -> clid.equals(s.getClid())).findFirst()
            : Optional.empty();
    if (sector.isPresent()) return sector.get();

    // Search in climbs
    Optional<Climb> climb =
        Optional.ofNullable(archive.getClimbs()).orElse(Collections.emptyList()).stream()
            .filter(c -> clid.equals(c.getClid()))
            .findFirst();
    if (climb.isPresent()) return climb.get();

    // Search in sessions
    Optional<Session> session =
        Optional.ofNullable(archive.getSessions()).orElse(Collections.emptyList()).stream()
            .filter(s -> clid.equals(s.getClid()))
            .findFirst();
    if (session.isPresent()) return session.get();

    return null;
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
