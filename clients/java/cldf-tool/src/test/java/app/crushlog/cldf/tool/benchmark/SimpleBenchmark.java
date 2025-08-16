package app.crushlog.cldf.tool.benchmark;

import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import app.crushlog.cldf.api.CLDFArchive;
import app.crushlog.cldf.models.Climb;
import app.crushlog.cldf.models.Location;
import app.crushlog.cldf.models.Session;
import app.crushlog.cldf.models.enums.FinishType;
import app.crushlog.cldf.models.enums.GradeSystem;
import app.crushlog.cldf.tool.services.DefaultGraphService;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
public class SimpleBenchmark {

  private CLDFArchive archive;
  private DefaultGraphService graphService;

  @Param({"100", "1000"})
  private int dataSize;

  @Setup
  public void setup() {
    // Create test data
    archive = createTestArchive(dataSize);

    // Initialize graph service
    graphService = new DefaultGraphService();
    try {
      graphService.initialize();
      graphService.importArchive(archive);
    } catch (IOException e) {
      throw new RuntimeException("Failed to initialize graph service", e);
    }
  }

  @TearDown
  public void tearDown() {
    if (graphService != null) {
      graphService.shutdown();
    }
  }

  @Benchmark
  public Map<String, Object> benchmarkGraphGradePyramid() {
    String query =
        """
            MATCH (c:Climb)
            WHERE c.grade IS NOT NULL
            RETURN c.grade as grade, count(*) as count
            ORDER BY grade
            """;

    List<Map<String, Object>> results = graphService.executeCypher(query, Map.of());
    return Map.of("results", results);
  }

  @Benchmark
  public Map<String, Object> benchmarkInMemoryGradePyramid() {
    Map<String, Long> gradeCounts =
        archive.getClimbs().stream()
            .filter(c -> c.getGrades() != null && c.getGrades().getGrade() != null)
            .collect(Collectors.groupingBy(c -> c.getGrades().getGrade(), Collectors.counting()));

    List<Map<String, Object>> results =
        gradeCounts.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(e -> Map.<String, Object>of("grade", e.getKey(), "count", e.getValue()))
            .collect(Collectors.toList());

    return Map.of("results", results);
  }

  private CLDFArchive createTestArchive(int size) {
    List<Location> locations = new ArrayList<>();
    List<Session> sessions = new ArrayList<>();
    List<Climb> climbs = new ArrayList<>();

    // Create locations
    for (int i = 0; i < 5; i++) {
      locations.add(
          Location.builder()
              .id(i + 1)
              .name("Location " + (i + 1))
              .country("USA")
              .isIndoor(i % 2 == 0)
              .build());
    }

    // Create sessions and climbs
    String[] grades = {"5.6", "5.7", "5.8", "5.9", "5.10a", "5.10b", "5.11a", "5.11b"};
    Random random = new Random(42);

    for (int i = 0; i < size; i++) {
      if (i % 10 == 0) {
        sessions.add(
            Session.builder()
                .id(sessions.size() + 1)
                .date(LocalDate.now().minusDays(random.nextInt(365)))
                .locationId(random.nextInt(5) + 1)
                .build());
      }

      climbs.add(
          Climb.builder()
              .id(i + 1)
              .sessionId(sessions.size())
              .date(LocalDate.now().minusDays(random.nextInt(365)))
              .routeName("Route " + (i + 1))
              .grades(
                  Climb.GradeInfo.builder()
                      .grade(grades[random.nextInt(grades.length)])
                      .system(GradeSystem.YDS)
                      .build())
              .finishType(random.nextBoolean() ? FinishType.REDPOINT : FinishType.ONSIGHT)
              .build());
    }

    return CLDFArchive.builder()
        .manifest(
            app.crushlog.cldf.models.Manifest.builder()
                .version("1.0.0")
                .format("CLDF")
                .creationDate(OffsetDateTime.now())
                .build())
        .locations(locations)
        .sessions(sessions)
        .climbs(climbs)
        .build();
  }

  public static void main(String[] args) throws RunnerException {
    Options opt =
        new OptionsBuilder().include(SimpleBenchmark.class.getSimpleName()).forks(1).build();

    new Runner(opt).run();
  }
}
