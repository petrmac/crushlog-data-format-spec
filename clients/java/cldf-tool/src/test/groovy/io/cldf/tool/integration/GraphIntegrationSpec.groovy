package io.cldf.tool.integration

import io.cldf.api.CLDFArchive
import io.cldf.models.*
import io.cldf.tool.services.GraphService
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise

import java.time.LocalDate
import java.time.OffsetDateTime

/**
 * Integration tests for Neo4j graph functionality
 */
@MicronautTest
@Stepwise
class GraphIntegrationSpec extends Specification {

    @Shared
    @Inject
    GraphService graphService

    def setupSpec() {
        // Ensure clean state
    }

    def "should initialize embedded Neo4j database"() {
        when:
        graphService.initialize()

        then:
        noExceptionThrown()
    }

    def "should import CLDF archive into graph"() {
        given:
        def archive = createTestArchive()

        when:
        graphService.importArchive(archive)

        then:
        noExceptionThrown()
    }

    def "should query locations from graph"() {
        when:
        def locations = graphService.executeCypher(
            "MATCH (l:Location) RETURN l.name as name, l.country as country ORDER BY l.name",
            [:])

        then:
        locations.size() == 2
        locations[0].name == "Boulder Canyon"
        locations[0].country == "United States"
        locations[1].name == "Clear Creek Canyon"
        locations[1].country == "United States"
    }

    def "should query climbs by grade"() {
        when:
        def climbs = graphService.executeCypher(
            "MATCH (c:Climb) WHERE c.grade = \$grade RETURN c.routeName as route, c.finishType as finish",
            [grade: "5.10a"])

        then:
        climbs.size() == 1
        climbs[0].route == "Classic Route"
        climbs[0].finish == "redpoint"
    }

    def "should find climbing partners"() {
        when:
        def partners = graphService.executeCypher("""
            MATCH (c1:Climber)-[:PARTNERED_WITH]-(c2:Climber)
            WHERE c1.name < c2.name
            RETURN c1.name as climber1, c2.name as climber2
            ORDER BY climber1, climber2
        """, [:])

        then:
        partners.size() == 1
        partners[0].climber1 == "Alice"
        partners[0].climber2 == "Bob"
    }

    def "should calculate grade pyramid"() {
        when:
        def pyramid = graphService.executeCypher("""
            MATCH (c:Climb)
            WHERE c.finishType IN ['redpoint', 'flash', 'onsight']
            WITH c.grade as grade, COUNT(*) as count
            RETURN grade, count
            ORDER BY grade
        """, [:])

        then:
        pyramid.size() == 3
        pyramid.find { it.grade == "5.9" }?.count == 1
        pyramid.find { it.grade == "5.10a" }?.count == 1
        pyramid.find { it.grade == "5.11a" }?.count == 1
    }

    def "should find routes by location"() {
        when:
        def routes = graphService.executeCypher("""
            MATCH (l:Location {name: \$location})<-[:AT_LOCATION]-(s:Session)-[:INCLUDES_CLIMB]->(c:Climb)
            RETURN DISTINCT c.routeName as route, c.grade as grade
            ORDER BY route
        """, [location: "Boulder Canyon"])

        then:
        routes.size() == 2
        routes[0].route == "Classic Route"
        routes[0].grade == "5.10a"
    }

    def "should export graph back to CLDF archive"() {
        when:
        def exportedArchive = graphService.exportToArchive()

        then:
        exportedArchive != null
        exportedArchive.locations.size() == 2
        exportedArchive.sessions.size() == 2
        exportedArchive.climbs.size() == 3
    }

    def "performance test: should handle large dataset efficiently"() {
        given:
        def largeArchive = createLargeTestArchive(1000, 5000) // 1000 sessions, 5000 climbs

        when:
        def startTime = System.currentTimeMillis()
        graphService.importArchive(largeArchive)
        def importTime = System.currentTimeMillis() - startTime

        and:
        startTime = System.currentTimeMillis()
        graphService.executeCypher("""
            MATCH (c:Climb)
            WHERE c.finishType = 'redpoint'
            WITH c.grade as grade, COUNT(*) as count
            RETURN grade, count
            ORDER BY count DESC
            LIMIT 10
        """, [:])
        def queryTime = System.currentTimeMillis() - startTime

        then:
        importTime < 5000 // Should import in under 5 seconds
        queryTime < 100   // Query should execute in under 100ms
    }

    def cleanupSpec() {
        graphService?.shutdown()
    }

    // Helper methods

    private CLDFArchive createTestArchive() {
        def locations = [
            Location.builder()
                .id(1)
                .name("Boulder Canyon")
                .country("United States")
                .state("Colorado")
                .isIndoor(false)
                .build(),
            Location.builder()
                .id(2)
                .name("Clear Creek Canyon")
                .country("United States")
                .state("Colorado")
                .isIndoor(false)
                .build()
        ]

        def sessions = [
            Session.builder()
                .id("session1")
                .date(LocalDate.of(2024, 1, 15))
                .location("Boulder Canyon")
                .locationId("1")
                .partners(["Alice", "Bob"])
                .build(),
            Session.builder()
                .id("session2")
                .date(LocalDate.of(2024, 1, 20))
                .location("Clear Creek Canyon")
                .locationId("2")
                .build()
        ]

        def climbs = [
            Climb.builder()
                .id(1)
                .sessionId(1)
                .date(LocalDate.of(2024, 1, 15))
                .routeName("Classic Route")
                .grades(Climb.GradeInfo.builder()
                    .grade("5.10a")
                    .system(Climb.GradeInfo.GradeSystem.yds)
                    .build())
                .type(Climb.ClimbType.route)
                .finishType("redpoint")
                .attempts(3)
                .rating(4)
                .partners(["Alice", "Bob"])
                .build(),
            Climb.builder()
                .id(2)
                .sessionId(1)
                .date(LocalDate.of(2024, 1, 15))
                .routeName("Warm-up Route")
                .grades(Climb.GradeInfo.builder()
                    .grade("5.9")
                    .system(Climb.GradeInfo.GradeSystem.yds)
                    .build())
                .type(Climb.ClimbType.route)
                .finishType("flash")
                .attempts(1)
                .partners(["Alice", "Bob"])
                .build(),
            Climb.builder()
                .id(3)
                .sessionId(2)
                .date(LocalDate.of(2024, 1, 20))
                .routeName("Project Route")
                .grades(Climb.GradeInfo.builder()
                    .grade("5.11a")
                    .system(Climb.GradeInfo.GradeSystem.yds)
                    .build())
                .type(Climb.ClimbType.route)
                .finishType("redpoint")
                .attempts(5)
                .rating(5)
                .build()
        ]

        return CLDFArchive.builder()
            .manifest(Manifest.builder()
                .version("1.0.0")
                .format("CLDF")
                .creationDate(OffsetDateTime.now())
                .build())
            .locations(locations)
            .sessions(sessions)
            .climbs(climbs)
            .routes([])
            .sectors([])
            .tags([])
            .mediaItems([])
            .mediaFiles([:])
            .checksums(Checksums.builder().algorithm("SHA-256").build())
            .build()
    }

    private CLDFArchive createLargeTestArchive(int sessionCount, int climbCount) {
        def locations = (1..10).collect { i ->
            Location.builder()
                .id(i)
                .name("Location $i")
                .country("United States")
                .isIndoor(i % 2 == 0)
                .build()
        }

        def sessions = (1..sessionCount).collect { i ->
            Session.builder()
                .id("session$i")
                .date(LocalDate.of(2024, 1, 1).plusDays(i % 365))
                .location(locations[i % locations.size()].name)
                .locationId(String.valueOf((i % locations.size()) + 1))
                .build()
        }

        def grades = ["5.8", "5.9", "5.10a", "5.10b", "5.10c", "5.10d", 
                      "5.11a", "5.11b", "5.11c", "5.11d", "5.12a"]
        def finishTypes = ["redpoint", "flash", "onsight", "fall", "hang"]

        def climbs = (1..climbCount).collect { i ->
            Climb.builder()
                .id(i)
                .sessionId((i % sessionCount) + 1)
                .date(sessions[i % sessionCount].date)
                .routeName("Route $i")
                .grades(Climb.GradeInfo.builder()
                    .grade(grades[i % grades.size()])
                    .system(Climb.GradeInfo.GradeSystem.yds)
                    .build())
                .type(Climb.ClimbType.route)
                .finishType(finishTypes[i % finishTypes.size()])
                .attempts((i % 5) + 1)
                .rating((i % 5) + 1)
                .build()
        }

        return CLDFArchive.builder()
            .manifest(Manifest.builder()
                .version("1.0.0")
                .format("CLDF")
                .creationDate(OffsetDateTime.now())
                .build())
            .locations(locations)
            .sessions(sessions)
            .climbs(climbs)
            .routes([])
            .sectors([])
            .tags([])
            .mediaItems([])
            .mediaFiles([:])
            .checksums(Checksums.builder().algorithm("SHA-256").build())
            .build()
    }
}