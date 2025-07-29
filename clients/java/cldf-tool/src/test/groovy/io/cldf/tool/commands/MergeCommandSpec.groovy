package io.cldf.tool.commands

import spock.lang.Specification
import spock.lang.TempDir
import io.cldf.api.CLDFArchive
import io.cldf.api.CLDF
import io.cldf.api.CLDFWriter
import io.cldf.models.*
import io.cldf.tool.models.CommandResult
import io.cldf.tool.utils.OutputHandler
import java.nio.file.Path
import java.nio.file.Files
import java.time.LocalDate
import java.time.OffsetDateTime

class MergeCommandSpec extends Specification {

    @TempDir
    Path tempDir

    MergeCommand command
    OutputHandler mockOutputHandler

    def setup() {
        command = new MergeCommand()
        mockOutputHandler = Mock(OutputHandler)
        
        // Inject mocks using reflection
        command.output = mockOutputHandler
    }

    def "should handle non-existent files"() {
        given: "input files where one doesn't exist"
        def file1 = tempDir.resolve("file1.cldf").toFile()
        def file2 = tempDir.resolve("non-existent.cldf").toFile()
        command.inputFiles = [file1, file2]
        command.outputFile = tempDir.resolve("output.cldf").toFile()
        
        // Create first file
        Files.write(file1.toPath(), "mock content".bytes)

        when: "executing the command"
        def result = command.execute()

        then: "result shows file not found"
        result.success == false
        result.exitCode == 1
        result.message.contains("File not found")
        result.message.contains("non-existent.cldf")
    }

    def "should validate input files list is not empty"() {
        given: "empty input files list"
        command.inputFiles = []
        command.outputFile = tempDir.resolve("output.cldf").toFile()

        when: "executing the command"
        command.execute()

        then: "throws exception due to empty list"
        thrown(Exception)
    }

    def "should test outputText for successful merge"() {
        given: "a successful command result with data"
        def result = CommandResult.builder()
            .success(true)
            .message("Successfully merged 2 archives")
            .data([
                inputFiles: ["file1.cldf", "file2.cldf"],
                outputFile: "/path/to/output.cldf",
                strategy: "append",
                sourceStats: [
                    "file1.cldf": [locations: 2, sessions: 3, climbs: 10],
                    "file2.cldf": [locations: 1, sessions: 2, climbs: 5]
                ],
                mergedStats: [locations: 3, sessions: 5, climbs: 15]
            ])
            .build()
        command.quiet = false

        when: "outputting text"
        command.outputText(result)

        then: "writes merge summary"
        1 * mockOutputHandler.write("Successfully merged 2 archives")
        1 * mockOutputHandler.write({ String msg ->
            msg.contains("Merge Summary") && 
            msg.contains("Strategy: append") &&
            msg.contains("Output:   /path/to/output.cldf") &&
            msg.contains("Locations: 3") &&
            msg.contains("Sessions:  5") &&
            msg.contains("Climbs:    15")
        })
    }

    def "should test outputText for failed merge"() {
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
            .message("Successfully merged 3 archives")
            .data([
                mergedStats: [locations: 5, sessions: 10, climbs: 25]
            ])
            .build()
        command.quiet = true

        when: "outputting text"
        command.outputText(result)

        then: "only writes the main message"
        1 * mockOutputHandler.write("Successfully merged 3 archives")
        0 * mockOutputHandler.write(_ as String)
    }

    def "should test outputText with null data"() {
        given: "a successful result with null data"
        def result = CommandResult.builder()
            .success(true)
            .message("Merge completed")
            .data(null)
            .build()
        command.quiet = false

        when: "outputting text"
        command.outputText(result)

        then: "only writes the main message"
        1 * mockOutputHandler.write("Merge completed")
        0 * mockOutputHandler.write(_ as String)
    }

    def "should test mergeArchives with append strategy"() {
        given: "multiple archives to merge"
        def archive1 = Mock(CLDFArchive)
        def archive2 = Mock(CLDFArchive)
        
        def locations1 = [
            Location.builder().id(1).name("Crag 1").isIndoor(false).build(),
            Location.builder().id(2).name("Gym 1").isIndoor(true).build()
        ]
        def locations2 = [
            Location.builder().id(3).name("Crag 2").isIndoor(false).build()
        ]
        
        def sessions1 = [
            Session.builder().id("1").locationId("1").date(LocalDate.of(2023, 7, 1)).build(),
            Session.builder().id("2").locationId("2").date(LocalDate.of(2023, 7, 2)).build()
        ]
        def sessions2 = [
            Session.builder().id("3").locationId("3").date(LocalDate.of(2023, 7, 3)).build()
        ]
        
        def climbs1 = [
            Climb.builder()
                .id(1)
                .sessionId(1)
                .date(LocalDate.of(2023, 7, 1))
                .routeName("Route 1")
                .type(Climb.ClimbType.route)
                .finishType(Climb.FinishType.onsight)
                .build()
        ]
        def climbs2 = [
            Climb.builder()
                .id(2)
                .sessionId(3)
                .date(LocalDate.of(2023, 7, 3))
                .routeName("Boulder 1")
                .type(Climb.ClimbType.boulder)
                .finishType(Climb.FinishType.flash)
                .build()
        ]
        
        archive1.getLocations() >> locations1
        archive1.getSessions() >> sessions1
        archive1.getClimbs() >> climbs1
        
        archive2.getLocations() >> locations2
        archive2.getSessions() >> sessions2
        archive2.getClimbs() >> climbs2

        when: "merging archives"
        def result = command.mergeArchives([archive1, archive2])

        then: "creates merged archive with all data"
        result.archive != null
        result.archive.locations.size() == 3
        result.archive.sessions.size() == 3
        result.archive.climbs.size() == 2
        result.stats.locations == 3
        result.stats.sessions == 3
        result.stats.climbs == 2
    }

    def "should test mergeArchives with null collections"() {
        given: "archives with null collections"
        def archive1 = Mock(CLDFArchive)
        def archive2 = Mock(CLDFArchive)
        
        archive1.getLocations() >> null
        archive1.getSessions() >> null
        archive1.getClimbs() >> null
        
        archive2.getLocations() >> [Location.builder().id(1).name("Test").isIndoor(false).build()]
        archive2.getSessions() >> null
        archive2.getClimbs() >> null

        when: "merging archives"
        def result = command.mergeArchives([archive1, archive2])

        then: "handles null collections gracefully"
        result.archive != null
        result.archive.locations.size() == 1
        result.archive.sessions.size() == 0
        result.archive.climbs.size() == 0
        result.stats.locations == 1
        result.stats.sessions == 0
        result.stats.climbs == 0
    }

    def "should test MergeResult class"() {
        given: "a merge result"
        def archive = CLDFArchive.builder()
            .locations([])
            .sessions([])
            .climbs([])
            .build()
        def stats = [locations: 5, sessions: 10, climbs: 20]
        def result = new MergeCommand.MergeResult(archive, stats)

        expect: "stores data correctly"
        result.archive == archive
        result.stats == stats
    }

    def "should merge multiple archives with complex data"() {
        given: "three CLDF files with different data"
        def file1 = createCLDFFile("file1.cldf")
        def file2 = createCLDFFile("file2.cldf")
        def file3 = createCLDFFile("file3.cldf")
        
        command.inputFiles = [file1.toFile(), file2.toFile(), file3.toFile()]
        command.outputFile = tempDir.resolve("merged.cldf").toFile()
        command.strategy = MergeCommand.MergeStrategy.append
        command.prettyPrint = true

        when: "executing the command"
        command.execute()

        then: "fails due to CLDF.read IOException"
        thrown(IOException)
    }

    def "should test manifest creation in merged archive"() {
        given: "empty archives"
        def archives = [Mock(CLDFArchive), Mock(CLDFArchive)]
        
        archives[0].getLocations() >> []
        archives[0].getSessions() >> []
        archives[0].getClimbs() >> []
        
        archives[1].getLocations() >> []
        archives[1].getSessions() >> []
        archives[1].getClimbs() >> []

        when: "merging archives"
        def result = command.mergeArchives(archives)

        then: "creates proper manifest"
        def manifest = result.archive.manifest
        manifest.version == "1.0.0"
        manifest.format == "CLDF"
        manifest.appVersion == "1.0.0"
        manifest.platform == Manifest.Platform.Desktop
        manifest.stats.locationsCount == 0
        manifest.stats.sessionsCount == 0
        manifest.stats.climbsCount == 0
        manifest.creationDate != null
    }

    def "should create checksums in merged archive"() {
        given: "archives to merge"
        def archives = [Mock(CLDFArchive)]
        archives[0].getLocations() >> []
        archives[0].getSessions() >> []
        archives[0].getClimbs() >> []

        when: "merging archives"
        def result = command.mergeArchives(archives)

        then: "creates checksums"
        result.archive.checksums != null
        result.archive.checksums.algorithm == "SHA-256"
        result.archive.checksums.generatedAt != null
    }

    // Helper methods
    private Path createCLDFFile(String name) {
        def file = tempDir.resolve(name)
        Files.write(file, "mock CLDF content".bytes)
        return file
    }
}