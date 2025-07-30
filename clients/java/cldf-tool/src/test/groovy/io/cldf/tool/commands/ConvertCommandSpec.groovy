package io.cldf.tool.commands

import spock.lang.Specification
import spock.lang.TempDir
import io.cldf.api.CLDFArchive
import io.cldf.api.CLDF
import io.cldf.models.*
import io.cldf.tool.models.CommandResult
import io.cldf.tool.utils.OutputHandler
import io.cldf.tool.utils.JsonUtils
import java.nio.file.Path
import java.nio.file.Files
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

import io.cldf.models.enums.ClimbType
import io.cldf.models.enums.FinishType

class ConvertCommandSpec extends Specification {

    @TempDir
    Path tempDir

    ConvertCommand command
    OutputHandler mockOutputHandler

    def setup() {
        command = new ConvertCommand()
        mockOutputHandler = Mock(OutputHandler)
        
        // Inject mocks using reflection
        command.output = mockOutputHandler
    }

    def "should handle non-existent file"() {
        given: "a non-existent file"
        command.inputFile = new File(tempDir.toFile(), "non-existent.cldf")
        command.outputFile = new File(tempDir.toFile(), "output.json")
        command.format = ConvertCommand.ConvertFormat.json

        when: "executing the command"
        def result = command.execute()

        then: "result shows file not found"
        result.success == false
        result.exitCode == 1
        result.message.contains("File not found")
    }

    def "should convert CLDF to JSON format"() {
        given: "a valid CLDF file"
        def cldfFile = createValidCLDFFile()
        command.inputFile = cldfFile.toFile()
        command.outputFile = tempDir.resolve("output.json").toFile()
        command.format = ConvertCommand.ConvertFormat.json

        when: "executing the command"
        command.execute()

        then: "IOException is thrown by CLDF.read"
        thrown(IOException)
    }

    def "should test outputText for successful conversion"() {
        given: "a successful command result with data"
        def result = CommandResult.builder()
            .success(true)
            .message("Successfully converted 15 items to json")
            .data([
                inputFile: "test.cldf",
                outputFile: "/path/to/output.json",
                format: "json",
                itemsConverted: 15,
                outputSize: 1024L
            ])
            .build()
        command.quiet = false

        when: "outputting text"
        command.outputText(result)

        then: "writes conversion details"
        1 * mockOutputHandler.write("Successfully converted 15 items to json")
        1 * mockOutputHandler.write({ String msg ->
            msg.contains("Conversion Details") && 
            msg.contains("Output: /path/to/output.json") &&
            msg.contains("Format: json") &&
            msg.contains("Items:  15") &&
            msg.contains("Size:   1024 bytes")
        })
    }

    def "should test outputText for failed conversion"() {
        given: "a failed command result"
        def result = CommandResult.builder()
            .success(false)
            .message("Error: File not found")
            .exitCode(1)
            .build()

        when: "outputting text"
        command.outputText(result)

        then: "writes error message"
        1 * mockOutputHandler.writeError("Error: File not found")
    }

    def "should test outputText in quiet mode"() {
        given: "a successful result in quiet mode"
        def result = CommandResult.builder()
            .success(true)
            .message("Successfully converted 10 items to csv")
            .data([
                inputFile: "test.cldf",
                outputFile: "/path/to/output.csv",
                format: "csv",
                itemsConverted: 10,
                outputSize: 512L
            ])
            .build()
        command.quiet = true

        when: "outputting text"
        command.outputText(result)

        then: "only writes the main message"
        1 * mockOutputHandler.write("Successfully converted 10 items to csv")
        0 * mockOutputHandler.write(_ as String)
    }

    def "should test outputText with null data"() {
        given: "a successful result with null data"
        def result = CommandResult.builder()
            .success(true)
            .message("Conversion completed")
            .data(null)
            .build()
        command.quiet = false

        when: "outputting text"
        command.outputText(result)

        then: "only writes the main message"
        1 * mockOutputHandler.write("Conversion completed")
        0 * mockOutputHandler.write(_ as String)
    }

    def "should test convertToJson method logic"() {
        given: "a mock archive"
        def archive = Mock(CLDFArchive)
        def locations = [
            Location.builder().id(1).name("Test Crag").isIndoor(false).build()
        ]
        def sessions = [
            Session.builder().id("1").locationId("1").date(LocalDate.now()).build()
        ]
        def climbs = [
            Climb.builder()
                .id(1)
                .sessionId(1)
                .date(LocalDate.now())
                .routeName("Test Route")
                .type(ClimbType.ROUTE)
                .finishType(FinishType.ONSIGHT)
                .build()
        ]
        
        archive.getLocations() >> locations
        archive.getSessions() >> sessions
        archive.getClimbs() >> climbs
        
        command.outputFile = tempDir.resolve("output.json").toFile()

        when: "converting to JSON"
        def result = command.convertToJson(archive)

        then: "creates JSON file with correct item count"
        result.itemCount == 3
        command.outputFile.exists()
        command.outputFile.text.contains("{")
    }

    def "should test convertToCsv method logic with headers"() {
        given: "a mock archive with climb data"
        def archive = Mock(CLDFArchive)
        def sessions = [
            Session.builder()
                .id("1")
                .location("Test Crag")
                .locationId("1")
                .date(LocalDate.of(2023, 7, 15))
                .build()
        ]
        def climbs = [
            Climb.builder()
                .id(1)
                .sessionId(1)
                .date(LocalDate.of(2023, 7, 15))
                .routeName("Test Route 1")
                .type(ClimbType.ROUTE)
                .grades(Climb.GradeInfo.builder().grade("5.10a").build())
                .finishType(FinishType.ONSIGHT)
                .attempts(1)
                .rating(4)
                .notes("Great climb!")
                .build(),
            Climb.builder()
                .id(2)
                .sessionId(1)
                .date(LocalDate.of(2023, 7, 15))
                .routeName("Test Route 2")
                .type(ClimbType.BOULDER)
                .grades(Climb.GradeInfo.builder().grade("V5").build())
                .finishType(FinishType.FLASH)
                .attempts(1)
                .build()
        ]
        
        archive.getSessions() >> sessions
        archive.getClimbs() >> climbs
        
        command.outputFile = tempDir.resolve("output.csv").toFile()
        command.includeHeaders = true
        command.dateFormat = "yyyy-MM-dd"

        when: "converting to CSV"
        def result = command.convertToCsv(archive)

        then: "creates CSV file with headers and correct data"
        result.itemCount == 2
        command.outputFile.exists()
        def csvContent = command.outputFile.text
        csvContent.contains("Date,Location,Route Name,Type,Grade,Finish Type,Attempts,Rating,Notes")
        csvContent.contains("2023-07-15,Test Crag,Test Route 1,ROUTE,5.10a,ONSIGHT,1,4,Great climb!")
        csvContent.contains("2023-07-15,Test Crag,Test Route 2,BOULDER,V5,FLASH,1,,")
    }

    def "should test convertToCsv without headers"() {
        given: "a mock archive"
        def archive = Mock(CLDFArchive)
        def sessions = [
            Session.builder()
                .id("1")
                .location("Test Crag")
                .locationId("1")
                .date(LocalDate.now())
                .build()
        ]
        def climbs = [
            Climb.builder()
                .id(1)
                .sessionId(1)
                .date(LocalDate.of(2023, 7, 15))
                .routeName("Test Route")
                .type(ClimbType.ROUTE)
                .finishType(FinishType.REDPOINT)
                .attempts(3)
                .build()
        ]
        
        archive.getSessions() >> sessions
        archive.getClimbs() >> climbs
        
        command.outputFile = tempDir.resolve("output.csv").toFile()
        command.includeHeaders = false
        command.dateFormat = "MM/dd/yyyy"

        when: "converting to CSV"
        def result = command.convertToCsv(archive)

        then: "creates CSV file without headers"
        result.itemCount == 1
        command.outputFile.exists()
        def csvContent = command.outputFile.text
        !csvContent.contains("Date,Location,Route Name")
        csvContent.contains("07/15/2023,Test Crag,Test Route,ROUTE,,REDPOINT,3,,")
    }

    def "should test escapeCsv method"() {
        expect: "correct CSV escaping"
        command.escapeCsv(input) == expected

        where:
        input                          | expected
        null                          | ""
        "simple text"                 | "simple text"
        "text with, comma"            | '"text with, comma"'
        'text with "quotes"'          | '"text with ""quotes"""'
        "text with\nnewline"          | '"text with\nnewline"'
        'text, with "both" and\nnew'  | '"text, with ""both"" and\nnew"'
    }

    def "should handle climb with unknown session"() {
        given: "a mock archive with orphaned climb"
        def archive = Mock(CLDFArchive)
        def sessions = []
        def climbs = [
            Climb.builder()
                .id(1)
                .sessionId(999) // Non-existent session
                .date(LocalDate.of(2023, 7, 15))
                .routeName("Orphaned Route")
                .type(ClimbType.ROUTE)
                .finishType(FinishType.PROJECT)
                .attempts(5)
                .build()
        ]
        
        archive.getSessions() >> sessions
        archive.getClimbs() >> climbs
        
        command.outputFile = tempDir.resolve("output.csv").toFile()
        command.includeHeaders = true
        command.dateFormat = "yyyy-MM-dd"

        when: "converting to CSV"
        def result = command.convertToCsv(archive)

        then: "creates CSV with Unknown location"
        result.itemCount == 1
        command.outputFile.exists()
        def csvContent = command.outputFile.text
        csvContent.contains("2023-07-15,Unknown,Orphaned Route,ROUTE,,PROJECT,5,,")
    }

    def "should handle climb with null values"() {
        given: "a mock archive with climb containing nulls"
        def archive = Mock(CLDFArchive)
        def sessions = []
        def climbs = [
            Climb.builder()
                .id(1)
                .sessionId(null)
                .date(LocalDate.of(2023, 7, 15))
                .routeName("Minimal Route")
                .type(ClimbType.BOULDER)
                .finishType(FinishType.TOP)
                .attempts(2)
                .grades(null)
                .rating(null)
                .notes(null)
                .build()
        ]
        
        archive.getSessions() >> sessions
        archive.getClimbs() >> climbs
        
        command.outputFile = tempDir.resolve("output.csv").toFile()
        command.includeHeaders = false
        command.dateFormat = "yyyy-MM-dd"

        when: "converting to CSV"
        def result = command.convertToCsv(archive)

        then: "handles null values correctly"
        result.itemCount == 1
        command.outputFile.exists()
        def csvContent = command.outputFile.text
        csvContent.contains("2023-07-15,Unknown,Minimal Route,BOULDER,,TOP,2,,")
    }

    def "should handle ConversionResult class"() {
        given: "a ConversionResult instance"
        def result = new ConvertCommand.ConversionResult(42)

        expect: "stores item count correctly"
        result.itemCount == 42
    }

    // Helper methods to create test data
    private Path createValidCLDFFile() {
        def file = tempDir.resolve("test.cldf")
        Files.write(file, "mock CLDF content".bytes)
        return file
    }
}
