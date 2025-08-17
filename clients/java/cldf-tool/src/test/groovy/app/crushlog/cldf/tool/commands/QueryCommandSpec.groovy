package app.crushlog.cldf.tool.commands

import app.crushlog.cldf.api.CLDFArchive
import app.crushlog.cldf.models.*
import app.crushlog.cldf.tool.models.CommandResult
import app.crushlog.cldf.tool.models.DataType
import app.crushlog.cldf.tool.services.CLDFService
import app.crushlog.cldf.tool.services.QueryService
import app.crushlog.cldf.tool.services.DefaultQueryService
import app.crushlog.cldf.tool.utils.OutputFormat
import app.crushlog.cldf.tool.utils.OutputHandler
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Path
import java.time.LocalDate
import java.time.OffsetDateTime

import app.crushlog.cldf.models.enums.ClimbType
import app.crushlog.cldf.models.enums.BelayType
import app.crushlog.cldf.models.enums.FinishType
import app.crushlog.cldf.models.enums.GradeSystem
import app.crushlog.cldf.models.enums.RockType
import app.crushlog.cldf.models.enums.SessionType
import app.crushlog.cldf.models.enums.Platform

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
        queryService = new DefaultQueryService()
        
        command = new QueryCommand(cldfService, queryService)
        command.outputFormat = OutputFormat.TEXT
        command.quiet = false
        command.output = new OutputHandler(OutputFormat.TEXT, false)
        command.selectType = DataType.ALL // Set default select type
        
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
        def nonExistentFile = new File("/non/existent/file.cldf")
        command.inputFile = nonExistentFile
        command.selectType = DataType.ALL

        when:
        def result = command.execute()

        then:
        !result.success
        result.message == "File not found: ${nonExistentFile.absolutePath}"
        result.exitCode == 1
    }

    def "should query all data"() {
        given:
        command.selectType = DataType.ALL
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
        command.selectType = DataType.CLIMBS
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
        command.selectType = DataType.SESSIONS
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
        command.selectType = DataType.LOCATIONS
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
        command.selectType = DataType.CLIMBS
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
        command.selectType = DataType.CLIMBS
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
        command.selectType = DataType.CLIMBS
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
        command.selectType = DataType.CLIMBS
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
        command.selectType = DataType.CLIMBS
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
        command.selectType = DataType.CLIMBS
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
        command.selectType = DataType.ROUTES  // No routes in test archive
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
        command.selectType = DataType.MANIFEST
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
        command.selectType = DataType.CLIMBS
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
        command.selectType = DataType.CLIMBS
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
        command.selectType = DataType.SESSIONS
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
        command.selectType = DataType.CLIMBS
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
        command.selectType = DataType.ROUTES
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
        command.selectType = DataType.SECTORS
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
        command.selectType = DataType.TAGS
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
        command.selectType = DataType.MEDIA
        def archiveWithMedia = Mock(CLDFArchive)
        def mediaItems = [Mock(MediaMetadataItem)]
        
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
        command.selectType = DataType.CLIMBS
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
        command.selectType = DataType.CLIMBS
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
        command.selectType = DataType.LOCATIONS
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
        command.selectType = DataType.SESSIONS
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
        command.selectType = DataType.CLIMBS
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
    
    def "should output error message for failed result"() {
        given: "a failed result"
        def mockOutput = Mock(OutputHandler)
        command.output = mockOutput
        def result = CommandResult.builder()
            .success(false)
            .message("Failed to process query")
            .build()

        when: "outputting text"
        command.outputText(result)

        then: "error message is written"
        1 * mockOutput.writeError("Failed to process query")
    }
    
    def "should output no results found message"() {
        given: "an empty result set"
        def mockOutput = Mock(OutputHandler)
        command.output = mockOutput
        def result = CommandResult.builder()
            .success(true)
            .message("Query completed")
            .data([
                count: 0,
                results: []
            ])
            .build()

        when: "outputting text"
        command.outputText(result)

        then: "no results message is written"
        1 * mockOutput.write("No results found.")
    }
    
    def "should display sessions with proper formatting"() {
        given: "a result with sessions"
        def mockOutput = Mock(OutputHandler)
        command.output = mockOutput
        command.selectType = DataType.SESSIONS
        def result = CommandResult.builder()
            .success(true)
            .message("Query completed")
            .data([
                count: 2,
                results: testArchive.sessions
            ])
            .build()

        when: "outputting text"
        command.outputText(result)

        then: "sessions are displayed correctly"
        1 * mockOutput.write("Found 2 sessions:")
        1 * mockOutput.write("  - 2024-01-15 at Movement Gym (INDOOR_CLIMBING)")
        1 * mockOutput.write("  - 2024-01-20 at Clear Creek Canyon")
    }
    
    def "should display locations with proper formatting"() {
        given: "a result with locations"
        def mockOutput = Mock(OutputHandler)
        command.output = mockOutput
        command.selectType = DataType.LOCATIONS
        def result = CommandResult.builder()
            .success(true)
            .message("Query completed")
            .data([
                count: 2,
                results: testArchive.locations
            ])
            .build()

        when: "outputting text"
        command.outputText(result)

        then: "locations are displayed correctly"
        1 * mockOutput.write("Found 2 locations:")
        1 * mockOutput.write("  - Movement Gym - USA, Colorado (Indoor)")
        1 * mockOutput.write("  - Clear Creek Canyon - USA, Colorado (Outdoor)")
    }
    
    def "should display generic items for unsupported types"() {
        given: "a result with generic type"
        def mockOutput = Mock(OutputHandler)
        command.output = mockOutput
        command.selectType = DataType.TAGS
        def tags = [Tag.builder().id(1).name("Hard").build()]
        def result = CommandResult.builder()
            .success(true)
            .message("Query completed")
            .data([
                count: 1,
                results: tags
            ])
            .build()

        when: "outputting text"
        command.outputText(result)

        then: "generic format is used"
        1 * mockOutput.write("Found 1 results:")
        1 * mockOutput.write({ it.contains("Tag(id=1, name=Hard") })
    }
    
    def "should display climbs with null values gracefully"() {
        given: "climbs with null values"
        def mockOutput = Mock(OutputHandler)
        command.output = mockOutput
        command.selectType = DataType.CLIMBS
        def climbWithNulls = Climb.builder()
            .id(99)
            .routeName(null)
            .grades(null)
            .type(null)
            .date(null)
            .build()
        def result = CommandResult.builder()
            .success(true)
            .message("Query completed")
            .data([
                count: 1,
                results: [climbWithNulls]
            ])
            .build()

        when: "outputting text"
        command.outputText(result)

        then: "null values are handled gracefully"
        1 * mockOutput.write("Found 1 climbs:")
        1 * mockOutput.write("  - Unnamed")
    }
    
    def "should handle locations without state information"() {
        given: "a location without state"
        def mockOutput = Mock(OutputHandler)
        command.output = mockOutput
        command.selectType = DataType.LOCATIONS
        def locationNoState = Location.builder()
            .id(3)
            .name("International Crag")
            .country("France")
            .state(null)
            .isIndoor(false)
            .build()
        def result = CommandResult.builder()
            .success(true)
            .message("Query completed")
            .data([
                count: 1,
                results: [locationNoState]
            ])
            .build()

        when: "outputting text"
        command.outputText(result)

        then: "location is displayed without state"
        1 * mockOutput.write("Found 1 locations:")
        1 * mockOutput.write("  - International Crag - France (Outdoor)")
    }
    
    def "should handle locations without country information"() {
        given: "a location without country"
        def mockOutput = Mock(OutputHandler)
        command.output = mockOutput
        command.selectType = DataType.LOCATIONS
        def locationNoCountry = Location.builder()
            .id(4)
            .name("Local Crag")
            .country(null)
            .isIndoor(false)
            .build()
        def result = CommandResult.builder()
            .success(true)
            .message("Query completed")
            .data([
                count: 1,
                results: [locationNoCountry]
            ])
            .build()

        when: "outputting text"
        command.outputText(result)

        then: "location is displayed without country"
        1 * mockOutput.write("Found 1 locations:")
        1 * mockOutput.write("  - Local Crag (Outdoor)")
    }
    
    def "should handle sessions without session type"() {
        given: "a session without session type"
        def mockOutput = Mock(OutputHandler)
        command.output = mockOutput
        command.selectType = DataType.SESSIONS
        def sessionNoType = Session.builder()
            .id(3)
            .date(LocalDate.of(2024, 2, 1))
            .location("Unknown Gym")
            .sessionType(null)
            .build()
        def result = CommandResult.builder()
            .success(true)
            .message("Query completed")
            .data([
                count: 1,
                results: [sessionNoType]
            ])
            .build()

        when: "outputting text"
        command.outputText(result)

        then: "session is displayed without type"
        1 * mockOutput.write("Found 1 sessions:")
        1 * mockOutput.write("  - 2024-02-01 at Unknown Gym")
    }
    
    def "should test no-arg constructor"() {
        when: "creating command with no-arg constructor"
        def cmd = new QueryCommand()
        
        then: "services are null"
        cmd.cldfService == null
        cmd.queryService == null
    }
    
    def "should handle query all with null collections in archive"() {
        given: "an archive with all null collections"
        command.selectType = DataType.ALL
        def archiveWithNulls = CLDFArchive.builder()
            .manifest(testArchive.manifest)
            .locations(null)
            .sessions(null)
            .climbs(null)
            .checksums(testArchive.checksums)
            .build()
        cldfService.read(inputFile) >> archiveWithNulls

        when: "executing the command"
        def result = command.execute()

        then: "summary shows zero counts"
        result.success
        def results = result.data["results"] as List
        def summary = results[0] as Map
        summary["locationsCount"] == 0
        summary["sessionsCount"] == 0
        summary["climbsCount"] == 0
    }

    // CLID Search Tests
    def "should find location by CLID"() {
        given: "archive with location having CLID"
        def location = Location.builder()
            .id(1)
            .clid("clid:location:550e8400-e29b-41d4-a716-446655440001")
            .name("Test Crag")
            .country("US")
            .isIndoor(false)
            .build()
        
        def archiveWithCLID = CLDFArchive.builder()
            .manifest(testArchive.manifest)
            .locations([location])
            .build()
        
        command.clid = "clid:location:550e8400-e29b-41d4-a716-446655440001"
        cldfService.read(inputFile) >> archiveWithCLID

        when: "searching by CLID"
        def result = command.execute()

        then: "location is found"
        result.success
        result.data["count"] == 1
        def results = result.data["results"] as List
        results.size() == 1
        results[0] instanceof Location
        (results[0] as Location).clid == "clid:location:550e8400-e29b-41d4-a716-446655440001"
        (results[0] as Location).name == "Test Crag"
    }

    def "should find route by CLID"() {
        given: "archive with route having CLID"
        def route = Route.builder()
            .id(1)
            .clid("clid:route:660e8400-e29b-41d4-a716-446655440002")
            .locationId(1)
            .name("Test Route")
            .routeType(app.crushlog.cldf.models.enums.RouteType.ROUTE)
            .build()
        
        def archiveWithCLID = CLDFArchive.builder()
            .manifest(testArchive.manifest)
            .routes([route])
            .build()
        
        command.clid = "clid:route:660e8400-e29b-41d4-a716-446655440002"
        cldfService.read(inputFile) >> archiveWithCLID

        when: "searching by CLID"
        def result = command.execute()

        then: "route is found"
        result.success
        result.data["count"] == 1
        def results = result.data["results"] as List
        results.size() == 1
        results[0] instanceof Route
        (results[0] as Route).clid == "clid:route:660e8400-e29b-41d4-a716-446655440002"
        (results[0] as Route).name == "Test Route"
    }

    def "should find sector by CLID"() {
        given: "archive with sector having CLID"
        def sector = Sector.builder()
            .id(1)
            .clid("clid:sector:770e8400-e29b-41d4-a716-446655440003")
            .locationId(1)
            .name("Test Sector")
            .build()
        
        def archiveWithCLID = CLDFArchive.builder()
            .manifest(testArchive.manifest)
            .sectors([sector])
            .build()
        
        command.clid = "clid:sector:770e8400-e29b-41d4-a716-446655440003"
        cldfService.read(inputFile) >> archiveWithCLID

        when: "searching by CLID"
        def result = command.execute()

        then: "sector is found"
        result.success
        result.data["count"] == 1
        def results = result.data["results"] as List
        results.size() == 1
        results[0] instanceof Sector
        (results[0] as Sector).clid == "clid:sector:770e8400-e29b-41d4-a716-446655440003"
        (results[0] as Sector).name == "Test Sector"
    }

    def "should find climb by CLID"() {
        given: "archive with climb having CLID"
        def climb = Climb.builder()
            .id(1)
            .clid("clid:climb:880e8400-e29b-41d4-a716-446655440004")
            .routeId(1)
            .date(LocalDate.now())
            .type(ClimbType.ROUTE)
            .finishType(FinishType.REDPOINT)
            .attempts(2)
            .build()
        
        def archiveWithCLID = CLDFArchive.builder()
            .manifest(testArchive.manifest)
            .climbs([climb])
            .build()
        
        command.clid = "clid:climb:880e8400-e29b-41d4-a716-446655440004"
        cldfService.read(inputFile) >> archiveWithCLID

        when: "searching by CLID"
        def result = command.execute()

        then: "climb is found"
        result.success
        result.data["count"] == 1
        def results = result.data["results"] as List
        results.size() == 1
        results[0] instanceof Climb
        (results[0] as Climb).clid == "clid:climb:880e8400-e29b-41d4-a716-446655440004"
        (results[0] as Climb).attempts == 2
    }

    def "should find session by CLID"() {
        given: "archive with session having CLID"
        def session = Session.builder()
            .id(1)
            .clid("clid:session:990e8400-e29b-41d4-a716-446655440005")
            .date(LocalDate.now())
            .location("Test Gym")
            .build()
        
        def archiveWithCLID = CLDFArchive.builder()
            .manifest(testArchive.manifest)
            .sessions([session])
            .build()
        
        command.clid = "clid:session:990e8400-e29b-41d4-a716-446655440005"
        cldfService.read(inputFile) >> archiveWithCLID

        when: "searching by CLID"
        def result = command.execute()

        then: "session is found"
        result.success
        result.data["count"] == 1
        def results = result.data["results"] as List
        results.size() == 1
        results[0] instanceof Session
        (results[0] as Session).clid == "clid:session:990e8400-e29b-41d4-a716-446655440005"
        (results[0] as Session).location == "Test Gym"
    }

    def "should return empty result when CLID not found"() {
        given: "archive without matching CLID"
        command.clid = "clid:route:nonexistent-id"
        cldfService.read(inputFile) >> testArchive

        when: "searching by non-existent CLID"
        def result = command.execute()

        then: "empty result is returned"
        result.success
        result.data["count"] == 0
        def results = result.data["results"] as List
        results.size() == 0
    }

    def "should search across all entity types with CLID"() {
        given: "archive with multiple entities having CLIDs"
        def location = Location.builder()
            .id(1)
            .clid("clid:location:550e8400-e29b-41d4-a716-446655440001")
            .name("Test Location")
            .country("US")
            .isIndoor(false)
            .build()
        
        def route = Route.builder()
            .id(1)
            .clid("clid:route:660e8400-e29b-41d4-a716-446655440002")
            .locationId(1)
            .name("Test Route")
            .routeType(app.crushlog.cldf.models.enums.RouteType.ROUTE)
            .build()
        
        def archiveWithMultiple = CLDFArchive.builder()
            .manifest(testArchive.manifest)
            .locations([location])
            .routes([route])
            .build()
        
        cldfService.read(inputFile) >> archiveWithMultiple

        when: "searching for route CLID"
        command.clid = "clid:route:660e8400-e29b-41d4-a716-446655440002"
        def routeResult = command.execute()

        then: "only route is found"
        routeResult.success
        routeResult.data["count"] == 1
        (routeResult.data["results"] as List)[0] instanceof Route

        when: "searching for location CLID"
        command.clid = "clid:location:550e8400-e29b-41d4-a716-446655440001"
        def locationResult = command.execute()

        then: "only location is found"
        locationResult.success
        locationResult.data["count"] == 1
        (locationResult.data["results"] as List)[0] instanceof Location
    }

    def "should handle null CLID fields gracefully"() {
        given: "archive with entities having null CLIDs"
        def locationWithoutCLID = Location.builder()
            .id(1)
            .clid(null)
            .name("No CLID Location")
            .country("US")
            .isIndoor(false)
            .build()
        
        def locationWithCLID = Location.builder()
            .id(2)
            .clid("clid:location:550e8400-e29b-41d4-a716-446655440001")
            .name("Has CLID Location")
            .country("US")
            .isIndoor(false)
            .build()
        
        def archiveWithMixed = CLDFArchive.builder()
            .manifest(testArchive.manifest)
            .locations([locationWithoutCLID, locationWithCLID])
            .build()
        
        command.clid = "clid:location:550e8400-e29b-41d4-a716-446655440001"
        cldfService.read(inputFile) >> archiveWithMixed

        when: "searching by CLID"
        def result = command.execute()

        then: "only matching entity is found"
        result.success
        result.data["count"] == 1
        def results = result.data["results"] as List
        results.size() == 1
        (results[0] as Location).name == "Has CLID Location"
    }

    def "should include CLID in query info"() {
        given: "a CLID search"
        command.clid = "clid:route:test-id"
        cldfService.read(inputFile) >> testArchive

        when: "executing query"
        def result = command.execute()

        then: "query info includes CLID"
        result.success
        def queryInfo = result.data["query"] as Map
        queryInfo["clid"] == "clid:route:test-id"
    }
}
