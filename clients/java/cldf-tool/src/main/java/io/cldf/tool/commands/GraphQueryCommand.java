package io.cldf.tool.commands;

import java.util.*;

import jakarta.inject.Inject;

import io.cldf.tool.models.CommandResult;
import io.cldf.tool.services.GraphService;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Execute Cypher queries on the loaded CLDF data. Provides both raw Cypher queries and predefined
 * query templates.
 */
@Slf4j
@Command(
    name = "graph-query",
    description = "Execute Cypher queries on CLDF data",
    mixinStandardHelpOptions = true)
public class GraphQueryCommand extends BaseCommand {

  @Parameters(index = "0", description = "CLDF archive file", arity = "0..1")
  private String archiveFile;

  @Option(
      names = {"--query"},
      description = "Cypher query to execute")
  private String cypherQuery;

  @Option(
      names = {"-t", "--template"},
      description = "Use predefined query template: ${COMPLETION-CANDIDATES}",
      completionCandidates = QueryTemplate.class)
  private String template;

  @Option(
      names = {"-p", "--param"},
      description = "Query parameters in format key=value")
  private Map<String, String> parameters = new HashMap<>();

  @Option(
      names = {"--limit"},
      description = "Limit number of results",
      defaultValue = "100")
  private int limit;

  private final GraphService graphService;

  @Inject
  public GraphQueryCommand(GraphService graphService) {
    this.graphService = graphService;
  }

  // For PicoCLI framework - it needs a no-arg constructor
  public GraphQueryCommand() {
    this.graphService = null;
  }

  static class QueryTemplate extends ArrayList<String> {
    QueryTemplate() {
      super(
          Arrays.asList(
              "grade-pyramid",
              "recent-sends",
              "project-routes",
              "climbing-partners",
              "location-stats",
              "progression-analysis",
              "weakness-finder"));
    }
  }

  @Override
  protected CommandResult execute() throws Exception {
    if (cypherQuery == null && template == null) {
      return CommandResult.builder()
          .success(false)
          .message("Either --query or --template must be specified")
          .exitCode(1)
          .build();
    }

    // Initialize graph if needed
    if (archiveFile != null) {
      logInfo("Loading archive: " + archiveFile);
      // Load archive into graph
      // This would be handled by a separate command or service
    }

    // Get the query to execute
    String query = cypherQuery != null ? cypherQuery : getTemplateQuery(template);

    // Add limit if not already in query
    if (!query.toLowerCase().contains("limit")) {
      query += " LIMIT " + limit;
    }

    logInfo("Executing Cypher query");

    try {
      // Convert string parameters to proper types
      Map<String, Object> params = new HashMap<>();
      for (Map.Entry<String, String> entry : parameters.entrySet()) {
        params.put(entry.getKey(), parseParameter(entry.getValue()));
      }

      List<Map<String, Object>> results = graphService.executeCypher(query, params);

      Map<String, Object> data = new HashMap<>();
      data.put("query", query);
      data.put("parameters", params);
      data.put("results", results);
      data.put("count", results.size());

      return CommandResult.builder()
          .success(true)
          .message("Query executed successfully")
          .data(data)
          .build();

    } catch (Exception e) {
      log.error("Query execution failed", e);
      return CommandResult.builder()
          .success(false)
          .message("Query execution failed: " + e.getMessage())
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

    Map<String, Object> data = (Map<String, Object>) result.getData();
    List<Map<String, Object>> results = (List<Map<String, Object>>) data.get("results");

    output.write(String.format("Found %d results", results.size()));
    output.write("");

    if (results.isEmpty()) {
      output.write("No results found");
      return;
    }

    // Display results in table format
    Set<String> columns = results.get(0).keySet();

    // Header
    output.write(String.join("\t", columns));
    output.write("-".repeat(80));

    // Rows
    for (Map<String, Object> row : results) {
      List<String> values = new ArrayList<>();
      for (String col : columns) {
        Object value = row.get(col);
        values.add(value != null ? value.toString() : "");
      }
      output.write(String.join("\t", values));
    }
  }

  private String getTemplateQuery(String templateName) {
    switch (templateName) {
      case "grade-pyramid":
        return """
                    MATCH (c:Climb)
                    WHERE c.finishType IN ['redpoint', 'flash', 'onsight']
                    WITH c.grade as grade, COUNT(*) as count
                    RETURN grade, count
                    ORDER BY grade
                    """;

      case "recent-sends":
        return """
                    MATCH (c:Climb)
                    WHERE c.finishType IN ['redpoint', 'flash', 'onsight']
                    AND c.date >= date() - duration('P30D')
                    RETURN c.date, c.routeName, c.grade, c.finishType, c.rating
                    ORDER BY c.date DESC
                    """;

      case "project-routes":
        return """
                    MATCH (c:Climb)
                    WHERE c.finishType NOT IN ['redpoint', 'flash', 'onsight']
                    WITH c.routeName as route, c.grade as grade,
                         COUNT(*) as attempts, MAX(c.date) as lastAttempt
                    WHERE attempts >= 2
                    RETURN route, grade, attempts, lastAttempt
                    ORDER BY attempts DESC, lastAttempt DESC
                    """;

      case "climbing-partners":
        return """
                    MATCH (c1:Climber)-[:PARTNERED_WITH]-(c2:Climber)
                    WITH c1.name as climber1, c2.name as climber2,
                         COUNT(*) as sessions
                    WHERE climber1 < climber2
                    RETURN climber1, climber2, sessions
                    ORDER BY sessions DESC
                    """;

      case "location-stats":
        return """
                    MATCH (l:Location)<-[:AT_LOCATION]-(s:Session)-[:INCLUDES_CLIMB]->(c:Climb)
                    WITH l.name as location,
                         COUNT(DISTINCT s) as sessions,
                         COUNT(c) as climbs,
                         AVG(c.rating) as avgRating
                    RETURN location, sessions, climbs,
                           ROUND(avgRating * 100) / 100 as avgRating
                    ORDER BY climbs DESC
                    """;

      case "progression-analysis":
        return """
                    MATCH (c:Climb)
                    WHERE c.finishType IN ['redpoint', 'flash', 'onsight']
                    WITH date.truncate('month', c.date) as month,
                         c.grade as grade, COUNT(*) as sends
                    RETURN month,
                           COLLECT({grade: grade, count: sends}) as grades
                    ORDER BY month
                    """;

      case "weakness-finder":
        return """
                    MATCH (c:Climb)
                    WHERE c.finishType = 'fall' OR c.attempts > 5
                    WITH c.routeName as route, c.grade as grade,
                         SUM(c.attempts) as totalAttempts,
                         COUNT(*) as sessions
                    RETURN route, grade, totalAttempts, sessions,
                           ROUND(TOFLOAT(totalAttempts) / sessions * 100) / 100 as avgAttempts
                    ORDER BY totalAttempts DESC
                    """;

      default:
        throw new IllegalArgumentException("Unknown template: " + templateName);
    }
  }

  private Object parseParameter(String value) {
    // Try to parse as number
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      // Not an integer
    }

    try {
      return Double.parseDouble(value);
    } catch (NumberFormatException e) {
      // Not a double
    }

    // Try to parse as boolean
    if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
      return Boolean.parseBoolean(value);
    }

    // Return as string
    return value;
  }
}
