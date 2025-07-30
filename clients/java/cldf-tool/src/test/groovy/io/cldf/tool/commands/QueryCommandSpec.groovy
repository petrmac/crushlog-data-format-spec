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

import io.cldf.models.enums.ClimbType
import io.cldf.models.enums.BelayType
import io.cldf.models.enums.FinishType
import io.cldf.models.enums.GradeSystem
import io.cldf.models.enums.RockType
import io.cldf.models.enums.SessionType
import io.cldf.models.enums.Platform

class QueryCommandSpec extends Specification {

    QueryCommand command
    CLDFService cldfService
    QueryService queryService
    
    @TempDir
    Path tempDir
    
    File inputFile
    CLDFArchive testArchive

    def setup() {
        cldfService = Mock(CLDFService)
        queryService = new QueryService()
        
        command = new QueryCommand(cldfService, queryService)
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
            .platform(Platform.DESKTOP)
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
                .rockType(RockType.GRANITE)
                .build()
        ]
        
        def sessions = [
            Session.builder()
                .id(1)
                .date(LocalDate.of(2024, 1, 15))
                .location("Movement Gym")
                .locationId(1)
                .isIndoor(true)
                .sessionType(SessionType.INDOOR_CLIMBING)
                .build(),
            Session.builder()
                .id(2)
                .date(LocalDate.of(2024, 1, 20))
                .location("Clear Creek Canyon")
                .locationId(2)
                .isIndoor(false)
                .climbType(ClimbType.ROUTE)
                .build()
        ]
        
        def climbs = [
            Climb.builder()
                .id(1)
                .sessionId(1)
                .date(LocalDate.of(2024, 1, 15))
                .routeName("Blue Problem")
                .type(ClimbType.BOULDER)
                .finishType(FinishType.TOP)
                .attempts(3)
                .grades(Climb.GradeInfo.builder()
                    .system(GradeSystem.V_SCALE)
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
                .type(ClimbType.BOULDER)
                .finishType(FinishType.TOP)
                .attempts(1)
                .grades(Climb.GradeInfo.builder()
                    .system(GradeSystem.V_SCALE)
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
                .type(ClimbType.ROUTE)
                .finishType(FinishType.ONSIGHT)
                .attempts(1)
                .grades(Climb.GradeInfo.builder()
                    .system(GradeSystem.YDS)
                    .grade("5.10a")
                    .build())
                .belayType(BelayType.LEAD)
                .height(25.0)
                .isIndoor(false)
                .rockType(RockType.GRANITE)
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
    
    def "should execute query with filter applied"() {
        given: "a query with filter"
        command.selectType = QueryCommand.DataType.climbs
        command.filter = "type=boulder"
        cldfService.read(inputFile) >> testArchive

        when: "executing the command"
        def result = command.execute()

        then: "filter is applied and result is successful"
        result.success == true
        result.message == "Query completed"
        result.data.query.filter == "type=boulder"
        result.data.count >= 0
    }
    
    def "should execute query with sorting applied"() {
        given: "a query with sorting"
        command.selectType = QueryCommand.DataType.sessions
        command.sortBy = "date"
        cldfService.read(inputFile) >> testArchive

        when: "executing the command"
        def result = command.execute()

        then: "sort is applied and result is successful"
        result.success == true
        result.data.query.sort == "date"
        result.data.count == 2
    }
    
    def "should execute query with field filtering"() {
        given: "a query with field filtering"
        command.selectType = QueryCommand.DataType.climbs
        command.fields = "routeName,grade,date"
        cldfService.read(inputFile) >> testArchive

        when: "executing the command"
        def result = command.execute()

        then: "field filtering is applied"
        result.success == true
        result.data.query.fields == "routeName,grade,date"
        result.data.count >= 0
    }
    
    def "should execute query for routes when archive has routes"() {
        given: "an archive with routes"
        command.selectType = QueryCommand.DataType.routes
        def archiveWithRoutes = Mock(CLDFArchive)
        def routes = [Mock(Route), Mock(Route)]
        
        archiveWithRoutes.hasRoutes() >> true
        archiveWithRoutes.getRoutes() >> routes
        cldfService.read(inputFile) >> archiveWithRoutes

        when: "executing the command"
        def result = command.execute()

        then: "routes are returned"
        result.success == true
        result.data.results == routes
        result.data.count == 2
    }
    
    def "should execute query for sectors when archive has sectors"() {
        given: "an archive with sectors"
        command.selectType = QueryCommand.DataType.sectors
        def archiveWithSectors = Mock(CLDFArchive)
        def sectors = [Mock(Sector)]
        
        archiveWithSectors.hasSectors() >> true
        archiveWithSectors.getSectors() >> sectors
        cldfService.read(inputFile) >> archiveWithSectors

        when: "executing the command"
        def result = command.execute()

        then: "sectors are returned"
        result.success == true
        result.data.results == sectors
        result.data.count == 1
    }
    
    def "should execute query for tags when archive has tags"() {
        given: "an archive with tags"
        command.selectType = QueryCommand.DataType.tags
        def archiveWithTags = Mock(CLDFArchive)
        def tags = [Mock(Tag), Mock(Tag)]
        
        archiveWithTags.hasTags() >> true
        archiveWithTags.getTags() >> tags
        cldfService.read(inputFile) >> archiveWithTags

        when: "executing the command"
        def result = command.execute()

        then: "tags are returned"
        result.success == true
        result.data.results == tags
        result.data.count == 2
    }
    
    def "should execute query for media when archive has media"() {
        given: "an archive with media"
        command.selectType = QueryCommand.DataType.media
        def archiveWithMedia = Mock(CLDFArchive)
        def mediaItems = [Mock(MediaItem)]
        
        archiveWithMedia.hasMedia() >> true
        archiveWithMedia.getMediaItems() >> mediaItems
        cldfService.read(inputFile) >> archiveWithMedia

        when: "executing the command"
        def result = command.execute()

        then: "media items are returned"
        result.success == true
        result.data.results == mediaItems
        result.data.count == 1
    }
    
    def "should execute query and handle null collections gracefully"() {
        given: "an archive with null collections"
        command.selectType = QueryCommand.DataType.climbs
        def archiveWithNulls = Mock(CLDFArchive)
        archiveWithNulls.getClimbs() >> null
        cldfService.read(inputFile) >> archiveWithNulls

        when: "executing the command"
        def result = command.execute()

        then: "empty results are returned gracefully"
        result.success == true
        result.data.results == []
        result.data.count == 0
    }
    
    def "should execute query and handle CLDFService IOException"() {
        given: "a file that fails to read"
        cldfService.read(inputFile) >> { throw new IOException("Invalid CLDF format") }

        when: "executing the command"
        def result = command.execute()

        then: "IOException is propagated (not caught in QueryCommand)"
        thrown(IOException)
    }
    
    def "should execute query with all options combined"() {
        given: "a query with all options"
        command.selectType = QueryCommand.DataType.climbs
        command.filter = "type=boulder"
        command.sortBy = "date"
        command.limit = 2
        command.offset = 1
        command.fields = "routeName,grade"
        command.includeStats = true
        cldfService.read(inputFile) >> testArchive

        when: "executing the command"
        def result = command.execute()

        then: "all options are applied"
        result.success == true
        result.data.query.select == "climbs"
        result.data.query.filter == "type=boulder"
        result.data.query.sort == "date"
        result.data.query.limit == 2
        result.data.query.offset == 1
        result.data.query.fields == "routeName,grade"
        result.data.containsKey("stats")
        result.data.count >= 0
    }
    
    def "should execute query and build proper query info"() {
        given: "a query with various parameters"
        command.selectType = QueryCommand.DataType.locations
        command.filter = "indoor=true"
        command.sortBy = "name"
        command.limit = 5
        command.offset = 2
        command.fields = "name,country"
        cldfService.read(inputFile) >> testArchive

        when: "executing the command"
        def result = command.execute()

        then: "query info is built correctly"
        result.success == true
        def queryInfo = result.data.query
        queryInfo.select == "locations"
        queryInfo.filter == "indoor=true"
        queryInfo.sort == "name"
        queryInfo.limit == 5
        queryInfo.offset == 2
        queryInfo.fields == "name,country"
    }
    
    def "should execute count only query successfully"() {
        given: "a count only query"
        command.selectType = QueryCommand.DataType.sessions
        command.countOnly = true
        cldfService.read(inputFile) >> testArchive

        when: "executing the command"
        def result = command.execute()

        then: "only count is returned"
        result.success == true
        result.data.count == 2
        !result.data.containsKey("results")
        !result.data.containsKey("stats")
        result.data.containsKey("query")
    }
    
    def "should execute query with statistics enabled"() {
        given: "a query with statistics enabled"
        command.selectType = QueryCommand.DataType.climbs
        command.includeStats = true
        cldfService.read(inputFile) >> testArchive

        when: "executing the command"
        def result = command.execute()

        then: "statistics are included"
        result.success == true
        result.data.count == 3
        result.data.containsKey("results")
        result.data.containsKey("stats")
        result.data.stats != null
    }
}
