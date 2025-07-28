package io.cldf.tool.benchmark;

import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.cldf.api.CLDFArchive;
import io.cldf.models.*;
import io.cldf.tool.services.GraphService;
import io.cldf.tool.services.QueryService;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

/** JMH benchmarks comparing Neo4j graph queries vs in-memory filtering */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(
    value = 1,
    jvmArgs = {"-Xms2G", "-Xmx2G"})
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class GraphBenchmark {

  private GraphService graphService;
  private QueryService queryService;
  private CLDFArchive testArchive;

  @Param({"100", "1000", "10000"})
  private int dataSize;

  @Setup(Level.Trial)
  public void setup() throws IOException {
    // Initialize services
    graphService = new GraphService();
    queryService = new QueryService();

    // Create test data
    testArchive = createTestArchive(dataSize);

    // Initialize and load graph
    graphService.initialize();
    graphService.importArchive(testArchive);
  }

  @TearDown(Level.Trial)
  public void teardown() {
    graphService.shutdown();
  }

  @Benchmark
  public void benchmarkGraphGradePyramid(Blackhole blackhole) {
    var results =
        graphService.executeCypher(
            """
            MATCH (c:Climb)
            WHERE c.finishType IN ['redpoint', 'flash', 'onsight']
            WITH c.grade as grade, COUNT(*) as count
            RETURN grade, count
            ORDER BY grade
            """,
            Collections.emptyMap());

    blackhole.consume(results);
  }

  @Benchmark
  public void benchmarkInMemoryGradePyramid(Blackhole blackhole) {
    var successfulClimbs =
        testArchive.getClimbs().stream()
            .filter(c -> Arrays.asList("redpoint", "flash", "onsight").contains(c.getFinishType()))
            .collect(Collectors.toList());

    var gradeCounts =
        successfulClimbs.stream()
            .collect(
                Collectors.groupingBy(
                    c -> c.getGrades() != null ? c.getGrades().getGrade() : "Unknown",
                    Collectors.counting()));

    blackhole.consume(gradeCounts);
  }

  @Benchmark
  public void benchmarkGraphPartnerSearch(Blackhole blackhole) {
    var results =
        graphService.executeCypher(
            """
            MATCH (c1:Climber)-[:PARTNERED_WITH]-(c2:Climber)
            WITH c1.name as climber1, c2.name as climber2, COUNT(*) as sessions
            WHERE climber1 < climber2
            RETURN climber1, climber2, sessions
            ORDER BY sessions DESC
            LIMIT 10
            """,
            Collections.emptyMap());

    blackhole.consume(results);
  }

  @Benchmark
  public void benchmarkInMemoryPartnerSearch(Blackhole blackhole) {
    Map<String, Map<String, Integer>> partnerCounts = new HashMap<>();

    // Count partnerships from sessions
    for (Session session : testArchive.getSessions()) {
      if (session.getPartners() != null && session.getPartners().size() > 1) {
        List<String> partners = session.getPartners();
        for (int i = 0; i < partners.size(); i++) {
          for (int j = i + 1; j < partners.size(); j++) {
            String p1 = partners.get(i);
            String p2 = partners.get(j);
            String key = p1.compareTo(p2) < 0 ? p1 + "-" + p2 : p2 + "-" + p1;
            partnerCounts.computeIfAbsent(p1, k -> new HashMap<>()).merge(p2, 1, Integer::sum);
          }
        }
      }
    }

    // Sort and limit
    var topPartners =
        partnerCounts.entrySet().stream()
            .flatMap(
                e ->
                    e.getValue().entrySet().stream()
                        .map(
                            p ->
                                Map.of(
                                    "climber1",
                                    e.getKey(),
                                    "climber2",
                                    p.getKey(),
                                    "sessions",
                                    p.getValue())))
            .sorted((a, b) -> ((Integer) b.get("sessions")).compareTo((Integer) a.get("sessions")))
            .limit(10)
            .collect(Collectors.toList());

    blackhole.consume(topPartners);
  }

  @Benchmark
  public void benchmarkGraphLocationStats(Blackhole blackhole) {
    var results =
        graphService.executeCypher(
            """
            MATCH (l:Location)<-[:AT_LOCATION]-(s:Session)-[:INCLUDES_CLIMB]->(c:Climb)
            WITH l.name as location,
                 COUNT(DISTINCT s) as sessions,
                 COUNT(c) as climbs,
                 AVG(c.rating) as avgRating
            RETURN location, sessions, climbs, avgRating
            ORDER BY climbs DESC
            """,
            Collections.emptyMap());

    blackhole.consume(results);
  }

  @Benchmark
  public void benchmarkInMemoryLocationStats(Blackhole blackhole) {
    Map<String, LocationStats> locationStats = new HashMap<>();

    // Build session to location map
    Map<String, String> sessionToLocation = new HashMap<>();
    for (Session session : testArchive.getSessions()) {
      sessionToLocation.put(session.getId(), session.getLocation());
    }

    // Calculate stats
    for (Climb climb : testArchive.getClimbs()) {
      String sessionId = String.valueOf(climb.getSessionId());
      String location = sessionToLocation.get(sessionId);
      if (location != null) {
        LocationStats stats = locationStats.computeIfAbsent(location, k -> new LocationStats());
        stats.sessions.add(sessionId);
        stats.climbCount++;
        if (climb.getRating() != null) {
          stats.totalRating += climb.getRating();
          stats.ratedClimbs++;
        }
      }
    }

    // Convert to result format
    var results =
        locationStats.entrySet().stream()
            .map(
                e -> {
                  LocationStats stats = e.getValue();
                  Map<String, Object> result = new HashMap<>();
                  result.put("location", e.getKey());
                  result.put("sessions", stats.sessions.size());
                  result.put("climbs", stats.climbCount);
                  result.put(
                      "avgRating",
                      stats.ratedClimbs > 0
                          ? (double) stats.totalRating / stats.ratedClimbs
                          : null);
                  return result;
                })
            .sorted((a, b) -> ((Integer) b.get("climbs")).compareTo((Integer) a.get("climbs")))
            .collect(Collectors.toList());

    blackhole.consume(results);
  }

  private CLDFArchive createTestArchive(int size) {
    // Create locations
    List<Location> locations =
        IntStream.range(0, 10)
            .mapToObj(
                i ->
                    Location.builder()
                        .id(i + 1)
                        .name("Location " + (i + 1))
                        .country("United States")
                        .isIndoor(i % 2 == 0)
                        .build())
            .collect(Collectors.toList());

    // Create sessions with partners
    List<String> climberNames =
        Arrays.asList(
            "Alice", "Bob", "Charlie", "David", "Eve", "Frank", "Grace", "Henry", "Iris", "Jack");
    Random random = new Random(42);

    List<Session> sessions =
        IntStream.range(0, size)
            .mapToObj(
                i -> {
                  List<String> partners = new ArrayList<>();
                  int partnerCount = random.nextInt(3) + 1;
                  for (int j = 0; j < partnerCount; j++) {
                    partners.add(climberNames.get(random.nextInt(climberNames.size())));
                  }

                  return Session.builder()
                      .id("session" + i)
                      .date(LocalDate.of(2024, 1, 1).plusDays(i % 365))
                      .location(locations.get(i % locations.size()).getName())
                      .locationId(String.valueOf((i % locations.size()) + 1))
                      .partners(partners)
                      .build();
                })
            .collect(Collectors.toList());

    // Create climbs
    String[] grades = {
      "5.8", "5.9", "5.10a", "5.10b", "5.10c", "5.10d", "5.11a", "5.11b", "5.11c", "5.11d", "5.12a"
    };
    String[] finishTypes = {"redpoint", "flash", "onsight", "fall", "hang"};

    List<Climb> climbs =
        IntStream.range(0, size * 5) // 5 climbs per session average
            .mapToObj(
                i -> {
                  Session session = sessions.get(i % sessions.size());
                  return Climb.builder()
                      .id(i + 1)
                      .sessionId((i % sessions.size()) + 1)
                      .date(session.getDate())
                      .routeName("Route " + i)
                      .grades(
                          Climb.GradeInfo.builder()
                              .grade(grades[random.nextInt(grades.length)])
                              .system(Climb.GradeInfo.GradeSystem.yds)
                              .build())
                      .type(Climb.ClimbType.route)
                      .finishType(finishTypes[random.nextInt(finishTypes.length)])
                      .attempts(random.nextInt(5) + 1)
                      .rating(random.nextInt(5) + 1)
                      .partners(session.getPartners())
                      .build();
                })
            .collect(Collectors.toList());

    return CLDFArchive.builder()
        .manifest(
            Manifest.builder()
                .version("1.0.0")
                .format("CLDF")
                .creationDate(OffsetDateTime.now())
                .build())
        .locations(locations)
        .sessions(sessions)
        .climbs(climbs)
        .routes(new ArrayList<>())
        .sectors(new ArrayList<>())
        .tags(new ArrayList<>())
        .mediaItems(new ArrayList<>())
        .mediaFiles(new HashMap<>())
        .checksums(Checksums.builder().algorithm("SHA-256").build())
        .build();
  }

  private static class LocationStats {
    Set<String> sessions = new HashSet<>();
    int climbCount = 0;
    int totalRating = 0;
    int ratedClimbs = 0;
  }
}
