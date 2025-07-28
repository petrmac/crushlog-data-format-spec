package io.cldf.tool.commands

import io.cldf.api.CLDFArchive
import io.cldf.models.*
import io.cldf.tool.models.CommandResult
import io.cldf.tool.services.CLDFService
import io.cldf.tool.services.QueryService
import io.cldf.tool.utils.OutputFormat
import io.cldf.tool.utils.OutputHandler
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Path
import java.time.LocalDate
import java.time.OffsetDateTime

class QueryCommandSpec extends Specification {

    QueryCommand command
    CLDFService cldfService
    QueryService queryService
    
    @TempDir
    Path tempDir
    
    File inputFile
    CLDFArchive testArchive

    def setup() {
        command = new QueryCommand()
        cldfService = Mock(CLDFService)
        queryService = new QueryService()
        
        command.cldfService = cldfService
        command.queryService = queryService
        command.outputFormat = OutputFormat.text
        command.quiet = false
        command.output = new OutputHandler(OutputFormat.text, false)
        
        inputFile = tempDir.resolve("test.cldf").toFile()
        inputFile.createNewFile()
        command.inputFile = inputFile
        
        // Create test archive with sample data
        testArchive = createTestArchive()
    }
    
    def createTestArchive() {
        def manifest = Manifest.builder()
            .version("1.0.0")
            .format("CLDF")
            .creationDate(OffsetDateTime.now())
            .appVersion("1.0.0")
            .platform(Manifest.Platform.Desktop)
            .build()
            
        def locations = [
            Location.builder()
                .id(1)
                .name("Movement Gym")
                .isIndoor(true)
                .country("USA")
                .state("Colorado")
                .build(),
            Location.builder()
                .id(2)
                .name("Clear Creek Canyon")
                .isIndoor(false)
                .country("USA")
                .state("Colorado")
                .rockType(Location.RockType.granite)
                .build()
        ]
        
        def sessions = [
            Session.builder()
                .id("sess_1")
                .date(LocalDate.of(2024, 1, 15))
                .location("Movement Gym")
                .locationId("1")
                .isIndoor(true)
                .sessionType(Session.SessionType.indoorClimbing)
                .build(),
            Session.builder()
                .id("sess_2")
                .date(LocalDate.of(2024, 1, 20))
                .location("Clear Creek Canyon")
                .locationId("2")
                .isIndoor(false)
                .climbType(Climb.ClimbType.route)
                .build()
        ]
        
        def climbs = [
            Climb.builder()
                .id(1)
                .sessionId(1)
                .date(LocalDate.of(2024, 1, 15))
                .routeName("Blue Problem")
                .type(Climb.ClimbType.boulder)
                .finishType(Climb.FinishType.top)
                .attempts(3)
                .grades(Climb.GradeInfo.builder()
                    .system(Climb.GradeInfo.GradeSystem.vScale)
                    .grade("V3")
                    .build())
                .isIndoor(true)
                .rating(4)
                .build(),
            Climb.builder()
                .id(2)
                .sessionId(1)
                .date(LocalDate.of(2024, 1, 15))
                .routeName("Yellow Slab")
                .type(Climb.ClimbType.boulder)
                .finishType(Climb.FinishType.top)
                .attempts(1)
                .grades(Climb.GradeInfo.builder()
                    .system(Climb.GradeInfo.GradeSystem.vScale)
                    .grade("V2")
                    .build())
                .isIndoor(true)
                .rating(3)
                .build(),
            Climb.builder()
                .id(3)
                .sessionId(2)
                .date(LocalDate.of(2024, 1, 20))
                .routeName("The Bulge")
                .type(Climb.ClimbType.route)
                .finishType(Climb.FinishType.onsight)
                .attempts(1)
                .grades(Climb.GradeInfo.builder()
                    .system(Climb.GradeInfo.GradeSystem.yds)
                    .grade("5.10a")
                    .build())
                .belayType(Climb.BelayType.lead)
                .height(25.0)
                .isIndoor(false)
                .rockType(Location.RockType.granite)
                .rating(5)
                .build()
        ]
        
        return CLDFArchive.builder()
            .manifest(manifest)
            .locations(locations)
            .sessions(sessions)
            .climbs(climbs)
            .checksums(Checksums.builder().algorithm("SHA-256").build())
            .build()
    }

    def "should fail when input file does not exist"() {
        given:
        command.inputFile = new File("/non/existent/file.cldf")
        command.selectType = QueryCommand.DataType.all

        when:
        def result = command.execute()

        then:
        !result.success
        result.message == "File not found: /non/existent/file.cldf"
        result.exitCode == 1
    }

    def "should query all data"() {
        given:
        command.selectType = QueryCommand.DataType.all
        cldfService.read(inputFile) >> testArchive

        when:
        def result = command.execute()

        then:
        result.success
        result.message == "Query completed"
        result.data["count"] == 1
        def results = result.data["results"] as List
        results.size() == 1
        def summary = results[0] as Map
        summary["locationsCount"] == 2
        summary["sessionsCount"] == 2
        summary["climbsCount"] == 3
    }

    def "should query climbs only"() {
        given:
        command.selectType = QueryCommand.DataType.climbs
        cldfService.read(inputFile) >> testArchive

        when:
        def result = command.execute()

        then:
        result.success
        result.data["count"] == 3
        def results = result.data["results"] as List
        results.size() == 3
        results.every { it instanceof Climb }
    }

    def "should query sessions only"() {
        given:
        command.selectType = QueryCommand.DataType.sessions
        cldfService.read(inputFile) >> testArchive

        when:
        def result = command.execute()

        then:
        result.success
        result.data["count"] == 2
        def results = result.data["results"] as List
        results.size() == 2
        results.every { it instanceof Session }
    }

    def "should query locations only"() {
        given:
        command.selectType = QueryCommand.DataType.locations
        cldfService.read(inputFile) >> testArchive

        when:
        def result = command.execute()

        then:
        result.success
        result.data["count"] == 2
        def results = result.data["results"] as List
        results.size() == 2
        results.every { it instanceof Location }
    }

    def "should apply limit"() {
        given:
        command.selectType = QueryCommand.DataType.climbs
        command.limit = 2
        cldfService.read(inputFile) >> testArchive

        when:
        def result = command.execute()

        then:
        result.success
        result.data["count"] == 2
        def results = result.data["results"] as List
        results.size() == 2
    }

    def "should apply offset"() {
        given:
        command.selectType = QueryCommand.DataType.climbs
        command.offset = 1
        cldfService.read(inputFile) >> testArchive

        when:
        def result = command.execute()

        then:
        result.success
        result.data["count"] == 2  // 3 total - 1 offset = 2
        def results = result.data["results"] as List
        results.size() == 2
    }

    def "should apply limit and offset together"() {
        given:
        command.selectType = QueryCommand.DataType.climbs
        command.limit = 1
        command.offset = 1
        cldfService.read(inputFile) >> testArchive

        when:
        def result = command.execute()

        then:
        result.success
        result.data["count"] == 1
        def results = result.data["results"] as List
        results.size() == 1
    }

    def "should return count only when requested"() {
        given:
        command.selectType = QueryCommand.DataType.climbs
        command.countOnly = true
        cldfService.read(inputFile) >> testArchive

        when:
        def result = command.execute()

        then:
        result.success
        result.data["count"] == 3
        !result.data.containsKey("results")
    }

    def "should include statistics when requested"() {
        given:
        command.selectType = QueryCommand.DataType.climbs
        command.includeStats = true
        cldfService.read(inputFile) >> testArchive

        when:
        def result = command.execute()

        then:
        result.success
        result.data["count"] == 3
        result.data["stats"] != null
    }

    def "should include query info in result"() {
        given:
        command.selectType = QueryCommand.DataType.climbs
        command.filter = "type=boulder"
        command.sortBy = "date"
        command.limit = 10
        command.offset = 5
        command.fields = "id,routeName,grade"
        cldfService.read(inputFile) >> testArchive

        when:
        def result = command.execute()

        then:
        result.success
        def queryInfo = result.data["query"] as Map
        queryInfo["select"] == "climbs"
        queryInfo["filter"] == "type=boulder"
        queryInfo["sort"] == "date"
        queryInfo["limit"] == 10
        queryInfo["offset"] == 5
        queryInfo["fields"] == "id,routeName,grade"
    }

    def "should handle empty results"() {
        given:
        command.selectType = QueryCommand.DataType.routes  // No routes in test archive
        cldfService.read(inputFile) >> testArchive

        when:
        def result = command.execute()

        then:
        result.success
        result.data["count"] == 0
        def results = result.data["results"] as List
        results.isEmpty()
    }

    def "should query manifest"() {
        given:
        command.selectType = QueryCommand.DataType.manifest
        cldfService.read(inputFile) >> testArchive

        when:
        def result = command.execute()

        then:
        result.success
        result.data["count"] == 1
        def results = result.data["results"] as List
        results.size() == 1
        results[0] instanceof Manifest
    }

    def "should format text output for climbs"() {
        given:
        command.selectType = QueryCommand.DataType.climbs
        cldfService.read(inputFile) >> testArchive
        def result = CommandResult.builder()
            .success(true)
            .message("Query completed")
            .data([
                count: 3,
                results: testArchive.climbs
            ])
            .build()

        when:
        command.outputText(result)

        then:
        // Should complete without errors
        noExceptionThrown()
    }

    def "should format text output for count only"() {
        given:
        command.countOnly = true
        def result = CommandResult.builder()
            .success(true)
            .message("Query completed")
            .data([count: 5])
            .build()

        when:
        command.outputText(result)

        then:
        // Should complete without errors
        noExceptionThrown()
    }

    def "should format text output with statistics"() {
        given:
        command.includeStats = true
        def result = CommandResult.builder()
            .success(true)
            .message("Query completed")
            .data([
                count: 3,
                results: testArchive.climbs,
                stats: [
                    averageGrade: "V2.3",
                    totalAttempts: 5,
                    successRate: "100%"
                ]
            ])
            .build()

        when:
        command.outputText(result)

        then:
        // Should complete without errors
        noExceptionThrown()
    }
}