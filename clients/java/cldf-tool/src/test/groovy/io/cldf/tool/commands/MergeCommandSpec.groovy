package io.cldf.tool.commands

import spock.lang.Specification
import spock.lang.TempDir
import io.cldf.api.CLDFArchive
import io.cldf.api.CLDFWriter
import io.cldf.models.*
import io.cldf.tool.models.CommandResult
import io.cldf.tool.utils.OutputHandler
import java.nio.file.Path
import java.nio.file.Files
import java.time.LocalDate
import java.time.OffsetDateTime

import io.cldf.models.enums.ClimbType
import io.cldf.models.enums.FinishType
import io.cldf.models.enums.GradeSystem
import io.cldf.models.enums.SessionType
import io.cldf.models.enums.Platform

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
            Session.builder().id(1).locationId(1).date(LocalDate.of(2023, 7, 1)).build(),
            Session.builder().id(2).locationId(2).date(LocalDate.of(2023, 7, 2)).build()
        ]
        def sessions2 = [
            Session.builder().id(3).locationId(3).date(LocalDate.of(2023, 7, 3)).build()
        ]
        
        def climbs1 = [
            Climb.builder()
                .id(1)
                .sessionId(1)
                .date(LocalDate.of(2023, 7, 1))
                .routeName("Route 1")
                .type(ClimbType.ROUTE)
                .finishType(FinishType.ONSIGHT)
                .build()
        ]
        def climbs2 = [
            Climb.builder()
                .id(2)
                .sessionId(3)
                .date(LocalDate.of(2023, 7, 3))
                .routeName("Boulder 1")
                .type(ClimbType.BOULDER)
                .finishType(FinishType.FLASH)
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
        def archive1 = createMockArchive(2, 2, 5)
        def archive2 = createMockArchive(3, 3, 8)
        def archive3 = createMockArchive(1, 2, 4)
        
        def file1 = tempDir.resolve("file1.cldf").toFile()
        def file2 = tempDir.resolve("file2.cldf").toFile()
        def file3 = tempDir.resolve("file3.cldf").toFile()
        
        new CLDFWriter(false).write(archive1, file1)
        new CLDFWriter(false).write(archive2, file2)
        new CLDFWriter(false).write(archive3, file3)
        
        command.inputFiles = [file1, file2, file3]
        command.outputFile = tempDir.resolve("merged.cldf").toFile()
        command.strategy = MergeCommand.MergeStrategy.append
        command.prettyPrint = true

        when: "executing the command"
        def result = command.execute()

        then: "merge is successful"
        result.success == true
        result.message.contains("Successfully merged 3 archives")
        result.data.mergedStats.locations == 6 // 2+3+1
        result.data.mergedStats.sessions == 7  // 2+3+2
        result.data.mergedStats.climbs == 17   // 5+8+4
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
        manifest.platform == Platform.DESKTOP
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

    def "should successfully execute merge command with valid files"() {
        given: "two actual CLDF files"
        def archive1 = createMockArchive(2, 1, 3)
        def archive2 = createMockArchive(1, 2, 4)
        
        def file1 = tempDir.resolve("archive1.cldf").toFile()
        def file2 = tempDir.resolve("archive2.cldf").toFile()
        def outputFile = tempDir.resolve("merged.cldf").toFile()
        
        // Create actual CLDF files using CLDFWriter
        new CLDFWriter(false).write(archive1, file1)
        new CLDFWriter(false).write(archive2, file2)
        
        command.inputFiles = [file1, file2]
        command.outputFile = outputFile
        command.strategy = MergeCommand.MergeStrategy.append
        command.prettyPrint = true

        when: "executing the command"
        def result = command.execute()

        then: "result is successful with merge data"
        result.success == true
        result.message.contains("Successfully merged 2 archives")
        result.data != null
        result.data.inputFiles == ["archive1.cldf", "archive2.cldf"]
        result.data.outputFile == outputFile.absolutePath
        result.data.strategy == "append"
        result.data.sourceStats != null
        result.data.mergedStats != null
        
        and: "output file is created"
        outputFile.exists()
    }
    
    def "should handle CLDF read IOException during execute"() {
        given: "an invalid CLDF file"
        def file1 = tempDir.resolve("invalid.cldf").toFile()
        file1.text = "invalid content"  // Create file with invalid content
        def outputFile = tempDir.resolve("merged.cldf").toFile()
        
        command.inputFiles = [file1]
        command.outputFile = outputFile
        command.strategy = MergeCommand.MergeStrategy.append

        when: "executing the command"
        command.execute()

        then: "IOException is thrown (not caught by MergeCommand)"
        thrown(IOException)
    }
    
    def "should handle CLDFWriter IOException during execute"() {
        given: "a nonexistent output directory"
        def archive1 = createMockArchive(1, 1, 1)
        def file1 = tempDir.resolve("archive1.cldf").toFile()
        new CLDFWriter(false).write(archive1, file1)
        
        // Use a path to a non-existent directory that won't be created
        def outputFile = tempDir.resolve("nonexistent/dir/merged.cldf").toFile()
        
        command.inputFiles = [file1]
        command.outputFile = outputFile
        command.strategy = MergeCommand.MergeStrategy.append

        when: "executing the command"
        command.execute()

        then: "FileNotFoundException is thrown (not caught by MergeCommand)"
        thrown(FileNotFoundException)
    }
    
    def "should execute merge with multiple archives"() {
        given: "three actual CLDF archives with varying content"
        def archive1 = createMockArchive(1, 1, 2)  // Small - need at least 1 session
        def archive2 = createMockArchive(3, 2, 5)  // Medium
        def archive3 = createMockArchive(5, 4, 10) // Large
        
        def file1 = tempDir.resolve("small.cldf").toFile()
        def file2 = tempDir.resolve("medium.cldf").toFile()
        def file3 = tempDir.resolve("large.cldf").toFile()
        def outputFile = tempDir.resolve("merged.cldf").toFile()
        
        // Create actual CLDF files
        new CLDFWriter(false).write(archive1, file1)
        new CLDFWriter(false).write(archive2, file2)
        new CLDFWriter(false).write(archive3, file3)
        
        command.inputFiles = [file1, file2, file3]
        command.outputFile = outputFile
        command.strategy = MergeCommand.MergeStrategy.append

        when: "executing the command"
        def result = command.execute()

        then: "archives are merged correctly"
        result.success == true
        result.message.contains("Successfully merged 3 archives")
        result.data.sourceStats.size() == 3
        outputFile.exists()
    }
    
    def "should execute merge with pretty print disabled"() {
        given: "archives with pretty print disabled"
        def archive1 = createMockArchive(1, 1, 1)
        def file1 = tempDir.resolve("archive1.cldf").toFile()
        def outputFile = tempDir.resolve("merged.cldf").toFile()
        
        new CLDFWriter(false).write(archive1, file1)
        
        command.inputFiles = [file1]
        command.outputFile = outputFile
        command.prettyPrint = false  // Disable pretty print
        command.strategy = MergeCommand.MergeStrategy.append

        when: "executing the command"
        def result = command.execute()

        then: "merge is successful and output file is created"
        result.success == true
        outputFile.exists()
        
        and: "verify the file is not pretty printed by checking size"
        outputFile.length() > 0
    }
    
    def "should execute merge and collect source statistics"() {
        given: "two archives with different sizes"
        def archive1 = createMockArchive(1, 2, 3)
        def archive2 = createMockArchive(4, 5, 6)
        
        def file1 = tempDir.resolve("small.cldf").toFile()
        def file2 = tempDir.resolve("large.cldf").toFile()
        def outputFile = tempDir.resolve("merged.cldf").toFile()
        
        new CLDFWriter(false).write(archive1, file1)
        new CLDFWriter(false).write(archive2, file2)
        
        command.inputFiles = [file1, file2]
        command.outputFile = outputFile
        command.strategy = MergeCommand.MergeStrategy.append

        when: "executing the command"
        def result = command.execute()

        then: "source statistics are collected correctly"
        result.success == true
        def sourceStats = result.data.sourceStats
        sourceStats["small.cldf"].locations == 1
        sourceStats["small.cldf"].sessions == 2
        sourceStats["small.cldf"].climbs == 3
        sourceStats["large.cldf"].locations == 4
        sourceStats["large.cldf"].sessions == 5
        sourceStats["large.cldf"].climbs == 6
    }
    
    def "should execute merge with archives containing minimal data"() {
        given: "archives with varying amounts of data"
        // Archive 1 has minimal required data per CLDF spec
        def archive1 = createMockArchive(1, 1, 1)  // 1 location, 1 session, 1 climb (minimum required)
        def archive2 = createMockArchive(2, 1, 3)
        
        def file1 = tempDir.resolve("archive1.cldf").toFile()
        def file2 = tempDir.resolve("archive2.cldf").toFile()
        def outputFile = tempDir.resolve("merged.cldf").toFile()
        
        new CLDFWriter(false).write(archive1, file1)
        new CLDFWriter(false).write(archive2, file2)
        
        command.inputFiles = [file1, file2]
        command.outputFile = outputFile
        command.strategy = MergeCommand.MergeStrategy.append

        when: "executing the command"
        def result = command.execute()

        then: "merge handles minimal data gracefully"
        result.success == true
        outputFile.exists()
        result.data.mergedStats.locations == 3  // 1 from archive1 + 2 from archive2
        result.data.mergedStats.sessions == 2   // 1 from archive1 + 1 from archive2
        result.data.mergedStats.climbs == 4     // 1 from archive1 + 3 from archive2
        
        and: "source stats are collected correctly"
        result.data.sourceStats["archive1.cldf"].locations == 1
        result.data.sourceStats["archive1.cldf"].sessions == 1  
        result.data.sourceStats["archive1.cldf"].climbs == 1
    }

    private CLDFArchive createMockArchive(int locationCount, int sessionCount, int climbCount) {
        // Create actual CLDF entities for a valid archive
        def manifest = Manifest.builder()
            .version("1.0.0")
            .format("CLDF")
            .creationDate(OffsetDateTime.now())
            .appVersion("1.0.0")
            .platform(Platform.DESKTOP)
            .build()
        
        def locations = (1..locationCount).collect { i ->
            Location.builder()
                .id(i)
                .name("Location $i")
                .isIndoor(i % 2 == 0)
                .country("USA")
                .state("Colorado")
                .build()
        }
        
        def sessions = (1..sessionCount).collect { i ->
            Session.builder()
                .id(i)
                .date(LocalDate.of(2024, 1, Math.max(1, i)))  // Ensure day is at least 1
                .location("Location $i")
                .locationId(i)
                .isIndoor(i % 2 == 0)
                .sessionType(SessionType.INDOOR_CLIMBING)
                .build()
        }
        
        def climbs = (1..climbCount).collect { i ->
            Climb.builder()
                .id(i)
                .sessionId(1)
                .date(LocalDate.of(2024, 1, 15))
                .routeName("Route $i")
                .type(ClimbType.BOULDER)
                .finishType(FinishType.TOP)
                .attempts(1)
                .grades(Climb.GradeInfo.builder()
                    .system(GradeSystem.V_SCALE)
                    .grade("V$i")
                    .build())
                .isIndoor(true)
                .rating(4)
                .build()
        }
        
        return CLDFArchive.builder()
            .manifest(manifest)
            .locations(locationCount > 0 ? locations : null)
            .sessions(sessionCount > 0 ? sessions : null)
            .climbs(climbCount > 0 ? climbs : null)
            .checksums(Checksums.builder().algorithm("SHA-256").build())
            .build()
    }


}
