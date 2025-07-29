package io.cldf.tool.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PreDestroy;
import jakarta.inject.Singleton;

import io.cldf.api.CLDFArchive;
import io.cldf.models.*;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.schema.Schema;
import org.neo4j.io.ByteUnit;

import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;

/**
 * Simplified service for managing embedded Neo4j graph database operations. Handles importing CLDF
 * data into graph format and executing queries.
 */
@Slf4j
@Singleton
public class GraphService {

  private DatabaseManagementService managementService;
  private GraphDatabaseService graphDb;
  private Path tempDbPath;

  // Node labels
  public enum NodeLabel implements Label {
    Location,
    Sector,
    Route,
    Session,
    Climb,
    Climber,
    Tag,
    Archive
  }

  // Relationship types
  public enum RelType implements RelationshipType {
    HAS_SECTOR,
    HAS_ROUTE,
    AT_LOCATION,
    INCLUDES_CLIMB,
    ON_ROUTE,
    BY_CLIMBER,
    TAGGED_WITH,
    PARTNERED_WITH,
    IN_SESSION,
    HAS_LOCATION,
    HAS_SESSION,
    HAS_CLIMB
  }

  /** Initialize embedded Neo4j database */
  public void initialize() throws IOException {
    if (graphDb != null) {
      return; // Already initialized
    }

    // Create temporary directory for the database
    tempDbPath = Files.createTempDirectory("cldf-neo4j-");
    tempDbPath.toFile().deleteOnExit();

    log.info("Initializing embedded Neo4j at: {}", tempDbPath);

    managementService =
        new DatabaseManagementServiceBuilder(tempDbPath)
            .setConfig(GraphDatabaseSettings.pagecache_memory, ByteUnit.mebiBytes(64))
            .build();

    graphDb = managementService.database(DEFAULT_DATABASE_NAME);

    // Create indexes
    createIndexes();
  }

  /** Import CLDF archive into Neo4j graph */
  public void importArchive(CLDFArchive archive) {
    log.info("Importing CLDF archive into Neo4j graph");

    try (Transaction tx = graphDb.beginTx()) {
      // Clear existing data
      tx.execute("MATCH (n) DETACH DELETE n");

      // Create archive node
      Node archiveNode = tx.createNode(NodeLabel.Archive);
      archiveNode.setProperty("format", archive.getManifest().getFormat());
      archiveNode.setProperty("version", archive.getManifest().getVersion());
      archiveNode.setProperty("createdAt", archive.getManifest().getCreationDate().toString());

      // Import locations
      Map<Integer, Node> locationNodes = new HashMap<>();
      if (archive.getLocations() != null) {
        for (Location location : archive.getLocations()) {
          Node locNode = createLocationNode(tx, location);
          locationNodes.put(location.getId(), locNode);
          archiveNode.createRelationshipTo(locNode, RelType.HAS_LOCATION);
        }
      }

      // Import sessions
      Map<String, Node> sessionNodes = new HashMap<>();
      if (archive.getSessions() != null) {
        for (Session session : archive.getSessions()) {
          Node sessionNode = createSessionNode(tx, session);
          sessionNodes.put(session.getId(), sessionNode);
          archiveNode.createRelationshipTo(sessionNode, RelType.HAS_SESSION);

          // Link to location
          if (session.getLocationId() != null) {
            try {
              Integer locId = Integer.parseInt(session.getLocationId());
              if (locationNodes.containsKey(locId)) {
                sessionNode.createRelationshipTo(locationNodes.get(locId), RelType.AT_LOCATION);
              }
            } catch (NumberFormatException e) {
              // Handle non-numeric location IDs
            }
          }
        }
      }

      // Import climbs
      if (archive.getClimbs() != null) {
        for (Climb climb : archive.getClimbs()) {
          Node climbNode = createClimbNode(tx, climb);
          archiveNode.createRelationshipTo(climbNode, RelType.HAS_CLIMB);

          // Link to session
          if (climb.getSessionId() != null) {
            String sessionId = "session" + climb.getSessionId();
            if (sessionNodes.containsKey(sessionId)) {
              sessionNodes.get(sessionId).createRelationshipTo(climbNode, RelType.INCLUDES_CLIMB);
            }
          }

          // Create tag relationships
          if (climb.getTags() != null) {
            for (String tagName : climb.getTags()) {
              Node tagNode = tx.findNode(NodeLabel.Tag, "name", tagName);
              if (tagNode == null) {
                tagNode = tx.createNode(NodeLabel.Tag);
                tagNode.setProperty("name", tagName);
              }
              climbNode.createRelationshipTo(tagNode, RelType.TAGGED_WITH);
            }
          }
        }
      }

      tx.commit();
      log.info("Successfully imported archive into Neo4j graph");
    } catch (Exception e) {
      log.error("Failed to import archive into Neo4j", e);
      throw new RuntimeException("Failed to import archive", e);
    }
  }

  /** Execute a Cypher query and return consumed results */
  public List<Map<String, Object>> executeCypher(String query, Map<String, Object> parameters) {
    try (Transaction tx = graphDb.beginTx()) {
      Result result = tx.execute(query, parameters);
      List<Map<String, Object>> results = new ArrayList<>();
      while (result.hasNext()) {
        results.add(result.next());
      }
      tx.commit();
      return results;
    }
  }

  /** Export graph back to CLDF archive format */
  public CLDFArchive exportToArchive() {
    try (Transaction tx = graphDb.beginTx()) {
      CLDFArchive.CLDFArchiveBuilder builder = CLDFArchive.builder();

      // Export archive metadata
      Node archiveNode = tx.findNode(NodeLabel.Archive, "format", "CLDF");
      if (archiveNode != null) {
        // Build manifest from archive node
        Manifest manifest =
            Manifest.builder()
                .format((String) archiveNode.getProperty("format"))
                .version((String) archiveNode.getProperty("version"))
                .creationDate(java.time.OffsetDateTime.now())
                .build();
        builder.manifest(manifest);
      }

      // Export locations
      List<Location> locations = new ArrayList<>();
      tx.findNodes(NodeLabel.Location)
          .forEachRemaining(
              node -> {
                locations.add(nodeToLocation(node));
              });
      builder.locations(locations);

      // Export sessions
      List<Session> sessions = new ArrayList<>();
      tx.findNodes(NodeLabel.Session)
          .forEachRemaining(
              node -> {
                sessions.add(nodeToSession(node));
              });
      builder.sessions(sessions);

      // Export climbs
      List<Climb> climbs = new ArrayList<>();
      tx.findNodes(NodeLabel.Climb)
          .forEachRemaining(
              node -> {
                climbs.add(nodeToClimb(node));
              });
      builder.climbs(climbs);

      // Add other collections as empty lists for now
      builder.routes(new ArrayList<>());
      builder.sectors(new ArrayList<>());
      builder.tags(new ArrayList<>());
      builder.mediaItems(new ArrayList<>());
      builder.mediaFiles(new HashMap<>());
      builder.checksums(Checksums.builder().algorithm("SHA-256").build());

      tx.commit();
      return builder.build();
    }
  }

  /** Shutdown the database */
  @PreDestroy
  public void shutdown() {
    log.info("Shutting down Neo4j database");
    if (managementService != null) {
      try {
        managementService.shutdown();
        managementService = null;
        graphDb = null;
      } catch (Exception e) {
        log.warn("Error during Neo4j shutdown: {}", e.getMessage());
      }
    }

    // Clean up temp directory
    if (tempDbPath != null) {
      try {
        Files.walk(tempDbPath)
            .sorted(Comparator.reverseOrder())
            .forEach(
                path -> {
                  try {
                    Files.deleteIfExists(path);
                  } catch (IOException e) {
                    log.warn("Failed to delete: {}", path);
                  }
                });
      } catch (IOException e) {
        log.warn("Failed to clean up temp directory", e);
      }
    }
  }

  // Helper methods for node creation

  private Node createLocationNode(Transaction tx, Location location) {
    Node node = tx.createNode(NodeLabel.Location);
    node.setProperty("locationId", location.getId());
    node.setProperty("name", location.getName());
    if (location.getCountry() != null) node.setProperty("country", location.getCountry());
    if (location.getState() != null) node.setProperty("state", location.getState());
    if (location.getIsIndoor() != null) node.setProperty("isIndoor", location.getIsIndoor());
    if (location.getCoordinates() != null) {
      node.setProperty("latitude", location.getCoordinates().getLatitude());
      node.setProperty("longitude", location.getCoordinates().getLongitude());
    }
    return node;
  }

  private Node createSessionNode(Transaction tx, Session session) {
    Node node = tx.createNode(NodeLabel.Session);
    node.setProperty("sessionId", session.getId());
    node.setProperty("date", session.getDate().toString());
    if (session.getLocation() != null) node.setProperty("locationName", session.getLocation());
    return node;
  }

  private Node createClimbNode(Transaction tx, Climb climb) {
    Node node = tx.createNode(NodeLabel.Climb);
    node.setProperty("climbId", climb.getId());
    node.setProperty("date", climb.getDate().toString());
    if (climb.getRouteName() != null) node.setProperty("routeName", climb.getRouteName());
    if (climb.getGrades() != null && climb.getGrades().getGrade() != null) {
      node.setProperty("grade", climb.getGrades().getGrade());
    }
    if (climb.getFinishType() != null) node.setProperty("finishType", climb.getFinishType().name());
    node.setProperty("attempts", climb.getAttempts());
    if (climb.getRating() != null) node.setProperty("rating", climb.getRating());
    return node;
  }

  private void createIndexes() {
    try (Transaction tx = graphDb.beginTx()) {
      Schema schema = tx.schema();

      // Create indexes for faster lookups
      schema.indexFor(NodeLabel.Location).on("locationId").create();
      schema.indexFor(NodeLabel.Location).on("name").create();
      schema.indexFor(NodeLabel.Session).on("sessionId").create();
      schema.indexFor(NodeLabel.Climb).on("climbId").create();
      schema.indexFor(NodeLabel.Climb).on("date").create();
      schema.indexFor(NodeLabel.Tag).on("name").create();

      tx.commit();
    }

    // Wait for indexes to come online
    try (Transaction tx = graphDb.beginTx()) {
      tx.schema().awaitIndexesOnline(2, TimeUnit.MINUTES);
      tx.commit();
    }
  }

  // Helper methods for converting nodes back to domain objects

  private Location nodeToLocation(Node node) {
    return Location.builder()
        .id((Integer) node.getProperty("locationId"))
        .name((String) node.getProperty("name"))
        .country((String) node.getProperty("country", null))
        .state((String) node.getProperty("state", null))
        .isIndoor((Boolean) node.getProperty("isIndoor", false))
        .build();
  }

  private Session nodeToSession(Node node) {
    return Session.builder()
        .id((String) node.getProperty("sessionId"))
        .date(java.time.LocalDate.parse((String) node.getProperty("date")))
        .location((String) node.getProperty("locationName", null))
        .build();
  }

  private Climb nodeToClimb(Node node) {
    Climb.ClimbBuilder builder =
        Climb.builder()
            .id((Integer) node.getProperty("climbId"))
            .date(java.time.LocalDate.parse((String) node.getProperty("date")))
            .routeName((String) node.getProperty("routeName", null))
            .finishType(parseFinishType((String) node.getProperty("finishType", null)))
            .attempts((Integer) node.getProperty("attempts", 1));

    String grade = (String) node.getProperty("grade", null);
    if (grade != null) {
      builder.grades(Climb.GradeInfo.builder().grade(grade).build());
    }

    Integer rating = (Integer) node.getProperty("rating", null);
    if (rating != null) {
      builder.rating(rating);
    }

    return builder.build();
  }

  private Climb.FinishType parseFinishType(String finishTypeStr) {
    if (finishTypeStr == null || finishTypeStr.isEmpty()) {
      return null;
    }
    try {
      return Climb.FinishType.valueOf(finishTypeStr);
    } catch (IllegalArgumentException e) {
      log.warn("Unknown finish type: {}", finishTypeStr);
      return null;
    }
  }
}
