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
    
    @TempDir
    Path tempDir
    
    QueryCommand command
    CLDFService cldfService
    QueryService queryService
    ByteArrayOutputStream outStream
    ByteArrayOutputStream errStream
    
    CLDFArchive testArchive
    
    def setup() {
        cldfService = Mock(CLDFService)
        queryService = new QueryService() // Use real implementation for testing
        
        command = new QueryCommand()
        command.cldfService = cldfService
        command.queryService = queryService
        
        outStream = new ByteArrayOutputStream()
        errStream = new ByteArrayOutputStream()
        command.output = new OutputHandler(OutputFormat.text, false,
            new PrintStream(outStream), new PrintStream(errStream))
        
        // Create test archive
        testArchive = createTestArchive()
    }
    
    def "should query all data when no filters applied"() {
        given:
        def inputFile = tempDir.resolve("test.cldf").toFile()
        inputFile.createNewFile()
        command.inputFile = inputFile
        command.selectType = QueryCommand.DataType.climbs
        cldfService.read(inputFile) >> testArchive
        
        when:
        def result = command.execute()
        
        then:
        result.success
        def data = result.data as Map
        data.count == 5
        (data.results as List).size() == 5
    }
    
    def "should filter climbs by type"() {
        given:
        def inputFile = tempDir.resolve("test.cldf").toFile()
        inputFile.createNewFile()
        command.inputFile = inputFile
        command.selectType = QueryCommand.DataType.climbs
        command.filter = "type=boulder"
        cldfService.read(inputFile) >> testArchive
        
        when:
        def result = command.execute()
        
        then:
        result.success
        def data = result.data as Map
        data.count == 3 // Only boulder climbs
        (data.results as List).every { climb ->
            (climb as Climb).type == Climb.ClimbType.boulder
        }
    }
    
    def "should filter climbs by grade range"() {
        given:
        def inputFile = tempDir.resolve("test.cldf").toFile()
        inputFile.createNewFile()
        command.inputFile = inputFile
        command.selectType = QueryCommand.DataType.climbs
        command.filter = "grade>=V3"
        cldfService.read(inputFile) >> testArchive
        
        when:
        def result = command.execute()
        
        then:
        result.success
        def data = result.data as Map
        data.count == 2 // V3 and V4
    }
    
    def "should support AND filters"() {
        given:
        def inputFile = tempDir.resolve("test.cldf").toFile()
        inputFile.createNewFile()
        command.inputFile = inputFile
        command.selectType = QueryCommand.DataType.climbs
        command.filter = "type=boulder AND rating>=4"
        cldfService.read(inputFile) >> testArchive
        
        when:
        def result = command.execute()
        
        then:
        result.success
        def data = result.data as Map
        data.count == 1 // Only V3 boulder with 4 stars
    }
    
    def "should sort results"() {
        given:
        def inputFile = tempDir.resolve("test.cldf").toFile()
        inputFile.createNewFile()
        command.inputFile = inputFile
        command.selectType = QueryCommand.DataType.climbs
        command.sortBy = "grade"
        cldfService.read(inputFile) >> testArchive
        
        when:
        def result = command.execute()
        
        then:
        result.success
        def climbs = (result.data as Map).results as List<Climb>
        climbs[0].grades.grade == "5.8"
        climbs[1].grades.grade == "5.10a"
        climbs[2].grades.grade == "V1"
        climbs[3].grades.grade == "V3"
        climbs[4].grades.grade == "V4"
    }
    
    def "should sort results in descending order"() {
        given:
        def inputFile = tempDir.resolve("test.cldf").toFile()
        inputFile.createNewFile()
        command.inputFile = inputFile
        command.selectType = QueryCommand.DataType.climbs
        command.sortBy = "-rating"
        cldfService.read(inputFile) >> testArchive
        
        when:
        def result = command.execute()
        
        then:
        result.success
        def climbs = (result.data as Map).results as List<Climb>
        climbs[0].rating == 5
        climbs[1].rating == 4
        climbs[2].rating == 3
    }
    
    def "should apply limit and offset"() {
        given:
        def inputFile = tempDir.resolve("test.cldf").toFile()
        inputFile.createNewFile()
        command.inputFile = inputFile
        command.selectType = QueryCommand.DataType.climbs
        command.sortBy = "grade"
        command.offset = 1
        command.limit = 2
        cldfService.read(inputFile) >> testArchive
        
        when:
        def result = command.execute()
        
        then:
        result.success
        def data = result.data as Map
        data.count == 2
        def climbs = data.results as List<Climb>
        climbs[0].grades.grade == "5.10a"
        climbs[1].grades.grade == "V1"
    }
    
    def "should return count only when requested"() {
        given:
        def inputFile = tempDir.resolve("test.cldf").toFile()
        inputFile.createNewFile()
        command.inputFile = inputFile
        command.selectType = QueryCommand.DataType.climbs
        command.countOnly = true
        cldfService.read(inputFile) >> testArchive
        
        when:
        def result = command.execute()
        
        then:
        result.success
        def data = result.data as Map
        data.count == 5
        !data.containsKey("results")
    }
    
    def "should include statistics when requested"() {
        given:
        def inputFile = tempDir.resolve("test.cldf").toFile()
        inputFile.createNewFile()
        command.inputFile = inputFile
        command.selectType = QueryCommand.DataType.climbs
        command.includeStats = true
        cldfService.read(inputFile) >> testArchive
        
        when:
        def result = command.execute()
        
        then:
        result.success
        def data = result.data as Map
        data.stats != null
        def stats = data.stats as Map
        stats.total == 5
        stats.byType != null
        stats.averageRating == 4.0
    }
    
    def "should query sessions"() {
        given:
        def inputFile = tempDir.resolve("test.cldf").toFile()
        inputFile.createNewFile()
        command.inputFile = inputFile
        command.selectType = QueryCommand.DataType.sessions
        cldfService.read(inputFile) >> testArchive
        
        when:
        def result = command.execute()
        
        then:
        result.success
        def data = result.data as Map
        data.count == 2
    }
    
    def "should query locations"() {
        given:
        def inputFile = tempDir.resolve("test.cldf").toFile()
        inputFile.createNewFile()
        command.inputFile = inputFile
        command.selectType = QueryCommand.DataType.locations
        command.filter = "isIndoor=true"
        cldfService.read(inputFile) >> testArchive
        
        when:
        def result = command.execute()
        
        then:
        result.success
        def data = result.data as Map
        data.count == 1
        def location = (data.results as List<Location>)[0]
        location.name == "Local Gym"
    }
    
    def "should output JSON format"() {
        given:
        def inputFile = tempDir.resolve("test.cldf").toFile()
        inputFile.createNewFile()
        command.inputFile = inputFile
        command.selectType = QueryCommand.DataType.climbs
        command.filter = "type=boulder"
        command.outputFormat = OutputFormat.json
        command.output = new OutputHandler(OutputFormat.json, false,
            new PrintStream(outStream), new PrintStream(errStream))
        cldfService.read(inputFile) >> testArchive
        
        when:
        command.run()
        
        then:
        def jsonOutput = outStream.toString()
        jsonOutput.contains('"success" : true')
        jsonOutput.contains('"count" : 3')
        jsonOutput.contains('"query"')
        jsonOutput.contains('"filter" : "type=boulder"')
    }
    
    def "should handle file not found"() {
        given:
        def inputFile = new File("/non/existent/file.cldf")
        command.inputFile = inputFile
        
        when:
        def result = command.execute()
        
        then:
        !result.success
        result.message.contains("File not found")
        result.exitCode == 1
    }
    
    def "should display results in text format"() {
        given:
        def inputFile = tempDir.resolve("test.cldf").toFile()
        inputFile.createNewFile()
        command.inputFile = inputFile
        command.selectType = QueryCommand.DataType.climbs
        command.filter = "type=boulder"
        cldfService.read(inputFile) >> testArchive
        
        when:
        command.run()
        
        then:
        def output = outStream.toString()
        output.contains("Found 3 climbs:")
        output.contains("Problem 1 (V1) - boulder")
        output.contains("Project (V4) - boulder")
    }
    
    private CLDFArchive createTestArchive() {
        def locations = [
            Location.builder()
                .id(1)
                .name("Local Gym")
                .isIndoor(true)
                .country("USA")
                .build(),
            Location.builder()
                .id(2)
                .name("Boulder Canyon")
                .isIndoor(false)
                .country("USA")
                .build()
        ]
        
        def sessions = [
            Session.builder()
                .id("sess_1")
                .date(LocalDate.now().minusDays(7))
                .location("Local Gym")
                .locationId("1")
                .isIndoor(true)
                .build(),
            Session.builder()
                .id("sess_2")
                .date(LocalDate.now().minusDays(1))
                .location("Boulder Canyon")
                .locationId("2")
                .isIndoor(false)
                .build()
        ]
        
        def climbs = [
            Climb.builder()
                .id(1)
                .sessionId(1)
                .routeName("Warm-up Route")
                .type(Climb.ClimbType.route)
                .grades(Climb.GradeInfo.builder()
                    .system(Climb.GradeInfo.GradeSystem.yds)
                    .grade("5.8")
                    .build())
                .rating(3)
                .isIndoor(true)
                .build(),
            Climb.builder()
                .id(2)
                .sessionId(1)
                .routeName("Problem 1")
                .type(Climb.ClimbType.boulder)
                .grades(Climb.GradeInfo.builder()
                    .system(Climb.GradeInfo.GradeSystem.vScale)
                    .grade("V1")
                    .build())
                .rating(3)
                .isIndoor(true)
                .build(),
            Climb.builder()
                .id(3)
                .sessionId(1)
                .routeName("Hard Route")
                .type(Climb.ClimbType.route)
                .grades(Climb.GradeInfo.builder()
                    .system(Climb.GradeInfo.GradeSystem.yds)
                    .grade("5.10a")
                    .build())
                .rating(5)
                .isIndoor(true)
                .build(),
            Climb.builder()
                .id(4)
                .sessionId(2)
                .routeName("Outdoor Boulder")
                .type(Climb.ClimbType.boulder)
                .grades(Climb.GradeInfo.builder()
                    .system(Climb.GradeInfo.GradeSystem.vScale)
                    .grade("V3")
                    .build())
                .rating(4)
                .isIndoor(false)
                .build(),
            Climb.builder()
                .id(5)
                .sessionId(2)
                .routeName("Project")
                .type(Climb.ClimbType.boulder)
                .grades(Climb.GradeInfo.builder()
                    .system(Climb.GradeInfo.GradeSystem.vScale)
                    .grade("V4")
                    .build())
                .rating(5)
                .isIndoor(false)
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
            .build()
    }
}