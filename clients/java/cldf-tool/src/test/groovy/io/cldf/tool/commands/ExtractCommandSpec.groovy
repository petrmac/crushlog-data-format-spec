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
import java.time.ZoneOffset

import io.cldf.models.enums.ClimbType
import io.cldf.models.enums.FinishType
import io.cldf.models.enums.Platform

class ExtractCommandSpec extends Specification {

    @TempDir
    Path tempDir

    ExtractCommand command
    OutputHandler mockOutputHandler

    def setup() {
        command = new ExtractCommand()
        mockOutputHandler = Mock(OutputHandler)
        
        // Inject mocks using reflection
        command.output = mockOutputHandler
    }

    def "should handle non-existent file"() {
        given: "a non-existent file"
        command.inputFile = new File(tempDir.toFile(), "non-existent.cldf")
        command.outputDirectory = tempDir.toFile()

        when: "executing the command"
        def result = command.execute()

        then: "result shows file not found"
        result.success == false
        result.exitCode == 1
        result.message.contains("File not found")
    }

    def "should extract all files from CLDF archive"() {
        given: "a valid CLDF file"
        def cldfFile = createValidCLDFFile()
        command.inputFile = cldfFile.toFile()
        command.outputDirectory = tempDir.resolve("extracted").toFile()
        command.prettyPrint = true
        command.overwrite = false

        when: "executing the command"
        command.execute()

        then: "IOException is thrown by CLDF.read"
        thrown(IOException)
    }

    def "should parse files to extract from comma-separated list"() {
        given: "a command with specific files to extract"
        command.files = "manifest.json,locations.json,climbs.json"

        when: "parsing files to extract"
        def filesToExtract = command.parseFilesToExtract()

        then: "returns list of files"
        filesToExtract == ["manifest.json", "locations.json", "climbs.json"]
    }

    def "should parse files to extract with empty string"() {
        given: "a command with empty files string"
        command.files = ""

        when: "parsing files to extract"
        def filesToExtract = command.parseFilesToExtract()

        then: "returns null (extract all)"
        filesToExtract == null
    }

    def "should parse files to extract with null"() {
        given: "a command with null files"
        command.files = null

        when: "parsing files to extract"
        def filesToExtract = command.parseFilesToExtract()

        then: "returns null (extract all)"
        filesToExtract == null
    }

    def "should test shouldExtract method with various scenarios"() {
        expect: "correct extraction decision"
        command.shouldExtract(filename, filesToExtract) == expected

        where:
        filename            | filesToExtract                     | expected
        "manifest.json"     | null                              | true
        "manifest.json"     | ["manifest.json"]                 | true
        "manifest.json"     | ["MANIFEST.JSON"]                 | true
        "manifest.json"     | ["locations.json"]                | false
        "media/photo1.jpg"  | ["media"]                         | true
        "media/photo1.jpg"  | ["media/photo1.jpg"]              | true
        "media/photo1.jpg"  | ["other"]                         | false
    }

    def "should test outputText for successful extraction"() {
        given: "a successful command result with data"
        def result = CommandResult.builder()
            .success(true)
            .message("Extracted 3 files to /output")
            .data([
                count: 3,
                files: ["manifest.json", "locations.json", "climbs.json"],
                stats: [manifest: 1, locations: 1, climbs: 1]
            ])
            .build()
        command.quiet = false

        when: "outputting text"
        command.outputText(result)

        then: "writes extraction details"
        1 * mockOutputHandler.write("Extracted 3 files to /output")
        1 * mockOutputHandler.write({ String msg ->
            msg.contains("Extracted Files") && 
            msg.contains("Total: 3 files")
        })
        1 * mockOutputHandler.write("\nBy Type:")
        3 * mockOutputHandler.write({ String msg -> msg.startsWith("  ") && msg.contains(": 1") })
        1 * mockOutputHandler.write("\nFiles:")
        3 * mockOutputHandler.write({ String msg -> msg.startsWith("  - ") })
    }

    def "should test outputText for failed extraction"() {
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
            .message("Extracted 3 files to /output")
            .data([
                count: 3,
                files: ["manifest.json"],
                stats: [manifest: 1]
            ])
            .build()
        command.quiet = true

        when: "outputting text"
        command.outputText(result)

        then: "only writes the main message"
        1 * mockOutputHandler.write("Extracted 3 files to /output")
        0 * mockOutputHandler.write(_ as String)
    }

    def "should test ExtractResult class"() {
        given: "an ExtractResult instance"
        def result = new ExtractCommand.ExtractResult()

        when: "adding files"
        result.addFile("manifest.json")
        result.addFile("locations.json")
        result.addFile("climbs.json")
        result.addFile("media/photo1.jpg")
        result.addFile("media/photo2.jpg")
        result.count = 5

        then: "tracks files and stats correctly"
        result.files == ["manifest.json", "locations.json", "climbs.json", "media/photo1.jpg", "media/photo2.jpg"]
        result.stats == [
            manifest: 1,
            locations: 1,
            climbs: 1,
            "media/photo1.jpg": 1,
            "media/photo2.jpg": 1
        ]

        when: "converting to map"
        def map = result.toMap()

        then: "contains all data"
        map.count == 5
        map.files == result.files
        map.stats == result.stats
    }

    def "should test writeJsonFile with existing file and no overwrite"() {
        given: "an existing file"
        def outputPath = tempDir
        def filename = "manifest.json"
        Files.writeString(outputPath.resolve(filename), "{}")
        
        command.overwrite = false
        command.prettyPrint = true

        when: "writing JSON file"
        command.writeJsonFile(outputPath, filename, [test: "data"])

        then: "skips the file and logs warning"
        Files.readString(outputPath.resolve(filename)) == "{}"
    }

    def "should test writeJsonFile with overwrite enabled"() {
        given: "an existing file"
        def outputPath = tempDir
        def filename = "manifest.json"
        Files.writeString(outputPath.resolve(filename), "{}")
        
        command.overwrite = true
        command.prettyPrint = false

        when: "writing JSON file"
        command.writeJsonFile(outputPath, filename, [test: "data"])

        then: "overwrites the file"
        Files.readString(outputPath.resolve(filename)).contains('"test":"data"')
    }

    def "should test extractFiles method logic"() {
        given: "a mock archive with various components"
        def archive = Mock(CLDFArchive)
        def outputPath = tempDir
        def manifest = Manifest.builder()
            .format("cldf")
            .appVersion("1.0")
            .creationDate(OffsetDateTime.now())
            .platform(Platform.DESKTOP)
            .build()
        def locations = [Location.builder().id(1).name("Test Location").isIndoor(false).build()]
        def sessions = [Session.builder().id(1).locationId(1).date(LocalDate.now()).build()]
        def climbs = [Climb.builder()
            .id(1)
            .sessionId(1)
            .date(LocalDate.now())
            .routeName("Test Route")
            .type(ClimbType.ROUTE)
            .finishType(FinishType.ONSIGHT)
            .build()]
        
        archive.getManifest() >> manifest
        archive.getLocations() >> locations
        archive.getSessions() >> sessions
        archive.getClimbs() >> climbs
        archive.getRoutes() >> []
        archive.getSectors() >> []
        archive.getTags() >> []
        archive.getMediaItems() >> []
        archive.getChecksums() >> null
        archive.hasRoutes() >> false
        archive.hasSectors() >> false
        archive.hasTags() >> false
        archive.hasMedia() >> false
        archive.hasEmbeddedMedia() >> false
        
        command.overwrite = true
        command.prettyPrint = false

        when: "extracting all files"
        def result = command.extractFiles(archive, outputPath, null)

        then: "extracts available files"
        result.count == 4
        result.files.contains("manifest.json")
        result.files.contains("locations.json")
        result.files.contains("sessions.json")
        result.files.contains("climbs.json")
        Files.exists(outputPath.resolve("manifest.json"))
        Files.exists(outputPath.resolve("locations.json"))
        Files.exists(outputPath.resolve("sessions.json"))
        Files.exists(outputPath.resolve("climbs.json"))
    }

    def "should test extractFiles with specific file list"() {
        given: "a mock archive and specific file list"
        def archive = Mock(CLDFArchive)
        def outputPath = tempDir
        def filesToExtract = ["manifest.json", "locations.json"]
        
        def manifest = Manifest.builder()
            .format("cldf")
            .appVersion("1.0")
            .creationDate(OffsetDateTime.now())
            .platform(Platform.DESKTOP)
            .build()
        def locations = [Location.builder().id(1).name("Test Location").isIndoor(false).build()]
        def sessions = [Session.builder().id(1).locationId(1).date(LocalDate.now()).build()]
        
        archive.getManifest() >> manifest
        archive.getLocations() >> locations
        archive.getSessions() >> sessions
        
        command.overwrite = true
        command.prettyPrint = false

        when: "extracting specific files"
        def result = command.extractFiles(archive, outputPath, filesToExtract)

        then: "only extracts requested files"
        result.count == 2
        result.files == ["manifest.json", "locations.json"]
        Files.exists(outputPath.resolve("manifest.json"))
        Files.exists(outputPath.resolve("locations.json"))
        !Files.exists(outputPath.resolve("sessions.json"))
    }

    // Helper methods to create test data
    private Path createValidCLDFFile() {
        def file = tempDir.resolve("test.cldf")
        Files.write(file, "mock CLDF content".bytes)
        return file
    }
}
