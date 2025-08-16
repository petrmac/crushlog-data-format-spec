package app.crushlog.cldf.tool.services;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import app.crushlog.cldf.api.CLDFArchive;

/**
 * Interface for graph database operations with CLDF data. Provides methods to import CLDF archives
 * into a graph database, execute graph queries, and export results back to CLDF format.
 */
public interface GraphService {

  /**
   * Initializes the embedded graph database.
   *
   * @throws IOException if database initialization fails
   */
  void initialize() throws IOException;

  /**
   * Imports a CLDF archive into the graph database.
   *
   * @param archive the CLDF archive to import
   */
  void importArchive(CLDFArchive archive);

  /**
   * Executes a Cypher query and returns the results.
   *
   * @param query the Cypher query to execute
   * @param parameters query parameters
   * @return list of result maps
   */
  List<Map<String, Object>> executeCypher(String query, Map<String, Object> parameters);

  /**
   * Exports the graph database content back to CLDF archive format.
   *
   * @return CLDF archive containing the exported data
   */
  CLDFArchive exportToArchive();

  /** Shuts down the graph database. */
  void shutdown();
}
