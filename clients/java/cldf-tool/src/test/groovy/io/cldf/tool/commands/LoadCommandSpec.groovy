package io.cldf.tool.commands

import io.cldf.api.CLDFArchive
import io.cldf.api.CLDFWriter
import io.cldf.models.*
import io.cldf.tool.models.CommandResult
import io.cldf.tool.services.GraphService
import io.cldf.tool.utils.OutputHandler
import io.cldf.tool.utils.OutputFormat
import picocli.CommandLine
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.time.OffsetDateTime

import io.cldf.models.enums.ClimbType
import io.cldf.models.enums.FinishType
import io.cldf.models.enums.GradeSystem
import io.cldf.models.enums.SessionType
import io.cldf.models.enums.Platform

class LoadCommandSpec extends Specification {
    
    @TempDir
    Path tempDir
    
    GraphService mockGraphService = Mock()
    OutputHandler mockOutput = Mock()
    
    @Subject
    LoadCommand loadCommand
    
    def setup() {
        loadCommand = new LoadCommand(mockGraphService)
        loadCommand.output = mockOutput
        loadCommand.outputFormat = OutputFormat.text
    }
    
    def "should handle non-existent file"() {
        given:
        def nonExistentFile = tempDir.resolve("nonexistent.cldf").toFile()
        loadCommand.archiveFile = nonExistentFile
        
        when:
        def result = loadCommand.execute()
        
        then:
        result.success == false
        result.message == "File not found: " + nonExistentFile.absolutePath
        result.exitCode == 1
    }
    
    def "should handle empty input files"() {
        given:
        def commandLine = new CommandLine(loadCommand)
        
        when:
        def exitCode = commandLine.execute()
        
        then:
        exitCode == 2 // Missing required parameter
    }
    
    def "should output text for failed load"() {
        given:
        def result = CommandResult.builder()
            .success(false)
            .message("Failed to load archive: Test error")
            .exitCode(1)
            .build()
        
        when:
        loadCommand.outputText(result)
        
        then:
        1 * mockOutput.writeError("Failed to load archive: Test error")
    }
    
    def "should output text for successful load without stats"() {
        given:
        loadCommand.showStats = false
        def result = CommandResult.builder()
            .success(true)
            .message("Successfully loaded archive into graph database")
            .build()
        
        when:
        loadCommand.outputText(result)
        
        then:
        1 * mockOutput.write("Successfully loaded archive into graph database")
    }
    
    def "should output text for successful load with stats"() {
        given:
        loadCommand.showStats = true
        def stats = [
            file: "test.cldf",
            fileSize: 1048576L,
            importTimeMs: 500L,
            locations: 5L,
            sessions: 10L,
            climbs: 50L,
            routes: 20L,
            climbers: 3L,
            tags: 15L,
            partnerRelationships: 8L,
            sessionClimbRelationships: 50L,
            routeClimbRelationships: 45L
        ]
        def result = CommandResult.builder()
            .success(true)
            .message("Successfully loaded archive into graph database")
            .data(stats)
            .build()
        
        when:
        loadCommand.outputText(result)
        
        then:
        1 * mockOutput.write("Successfully loaded archive into graph database")
        1 * mockOutput.write({ String text ->
            text.contains("Import Statistics") &&
            text.contains("File: test.cldf") &&
            text.contains("Import time: 500 ms") &&
            text.contains("Locations: 5") &&
            text.contains("Sessions: 10") &&
            text.contains("Climbs: 50") &&
            text.contains("Routes: 20") &&
            text.contains("Climbers: 3") &&
            text.contains("Tags: 15") &&
            text.contains("Partner connections: 8") &&
            text.contains("Session climbs: 50") &&
            text.contains("Route climbs: 45")
        })
        1 * mockOutput.write("\nGraph database ready for queries. Use 'cldf graph-query' to analyze your data.")
    }
    
    def "should handle missing statistics gracefully"() {
        given:
        loadCommand.showStats = true
        def stats = [
            file: "test.cldf",
            fileSize: 1048576L,
            importTimeMs: 500L
            // No node/relationship counts
        ]
        def result = CommandResult.builder()
            .success(true)
            .message("Successfully loaded archive into graph database")
            .data(stats)
            .build()
        
        when:
        loadCommand.outputText(result)
        
        then:
        1 * mockOutput.write("Successfully loaded archive into graph database")
        1 * mockOutput.write({ String text ->
            text.contains("Import Statistics") &&
            text.contains("File: test.cldf") &&
            text.contains("Import time: 500 ms") &&
            text.contains("Locations: 0") &&
            text.contains("Sessions: 0") &&
            text.contains("Climbs: 0")
        })
    }
    
    def "test collectStatistics method directly"() {
        given:
        mockGraphService.executeCypher("MATCH (n:Location) RETURN COUNT(n) as count", [:]) >> [[count: 5L]]
        mockGraphService.executeCypher("MATCH (n:Session) RETURN COUNT(n) as count", [:]) >> [[count: 10L]]
        mockGraphService.executeCypher("MATCH (n:Climb) RETURN COUNT(n) as count", [:]) >> [[count: 50L]]
        mockGraphService.executeCypher("MATCH (n:Route) RETURN COUNT(n) as count", [:]) >> [[count: 20L]]
        mockGraphService.executeCypher("MATCH (n:Climber) RETURN COUNT(n) as count", [:]) >> [[count: 3L]]
        mockGraphService.executeCypher("MATCH (n:Tag) RETURN COUNT(n) as count", [:]) >> [[count: 15L]]
        mockGraphService.executeCypher("MATCH ()-[r:PARTNERED_WITH]->() RETURN COUNT(r) as count", [:]) >> [[count: 8L]]
        mockGraphService.executeCypher("MATCH ()-[r:INCLUDES_CLIMB]->() RETURN COUNT(r) as count", [:]) >> [[count: 50L]]
        mockGraphService.executeCypher("MATCH ()-[r:ON_ROUTE]->() RETURN COUNT(r) as count", [:]) >> [[count: 45L]]
        
        when:
        def stats = loadCommand.collectStatistics()
        
        then:
        stats["locations"] == 5L
        stats["sessions"] == 10L
        stats["climbs"] == 50L
        stats["routes"] == 20L
        stats["climbers"] == 3L
        stats["tags"] == 15L
        stats["partnerRelationships"] == 8L
        stats["sessionClimbRelationships"] == 50L
        stats["routeClimbRelationships"] == 45L
    }
    
    def "test countNodes method directly"() {
        given:
        mockGraphService.executeCypher("test query", [:]) >> [[count: 42L]]
        
        when:
        def count = loadCommand.countNodes("test query")
        
        then:
        count == 42L
    }
    
    def "test countNodes with empty result"() {
        given:
        mockGraphService.executeCypher("test query", [:]) >> []
        
        when:
        def count = loadCommand.countNodes("test query")
        
        then:
        count == 0L
    }
    
    def "test countNodes with null count"() {
        given:
        mockGraphService.executeCypher("test query", [:]) >> [[count: null]]
        
        when:
        def count = loadCommand.countNodes("test query")
        
        then:
        count == 0L
    }
    
    def "test countNodes with non-numeric count"() {
        given:
        mockGraphService.executeCypher("test query", [:]) >> [[count: "not a number"]]
        
        when:
        def count = loadCommand.countNodes("test query")
        
        then:
        count == 0L
    }
    
    def "test countNodes with missing count key"() {
        given:
        mockGraphService.executeCypher("test query", [:]) >> [[other: 42L]]
        
        when:
        def count = loadCommand.countNodes("test query")
        
        then:
        count == 0L
    }
    
    def "should successfully execute load command with valid file"() {
        given: "a valid CLDF file"
        def cldfFile = createValidCLDFFile().toFile()
        loadCommand.archiveFile = cldfFile
        loadCommand.showStats = true
        
        // Mock statistics queries
        setupMockStatistics()

        when: "executing the command"
        def result = loadCommand.execute()

        then: "graph service methods are called"
        1 * mockGraphService.initialize()
        1 * mockGraphService.importArchive(_ as CLDFArchive) >> { CLDFArchive archive ->
            // Verify the archive has expected data
            assert archive.locations.size() == 5
            assert archive.sessions.size() == 10
            assert archive.climbs.size() == 50
        }
        
        and: "result is successful with data"
        result.success == true
        result.message.contains("Successfully loaded archive")
        result.data != null
        result.data.containsKey("importTimeMs")
        result.data.containsKey("file")
        result.data.containsKey("fileSize")
        result.data.locations == 5L
        result.data.sessions == 10L
    }
    
    def "should execute load command without stats collection"() {
        given: "a valid CLDF file with showStats = false"
        def cldfFile = createValidCLDFFile().toFile()
        loadCommand.archiveFile = cldfFile
        loadCommand.showStats = false

        when: "executing the command"
        def result = loadCommand.execute()

        then: "graph service methods are called but no statistics queries"
        1 * mockGraphService.initialize()
        1 * mockGraphService.importArchive(_ as CLDFArchive) >> { CLDFArchive archive ->
            // Verify the archive has expected data
            assert archive.locations.size() == 5
            assert archive.sessions.size() == 10
            assert archive.climbs.size() == 50
        }
        0 * mockGraphService.executeCypher(_ as String, _ as Map)
        
        and: "result is successful"
        result.success == true
        result.data.containsKey("importTimeMs")
        result.data.containsKey("file")
        result.data.containsKey("fileSize")
    }
    
    def "should handle CLDF read IOException in execute"() {
        given: "a file that will cause read error"
        def invalidFile = tempDir.resolve("invalid.cldf").toFile()
        invalidFile.text = "invalid content" // Create invalid CLDF content
        loadCommand.archiveFile = invalidFile

        when: "executing the command"
        def result = loadCommand.execute()

        then: "error is handled gracefully"
        result.success == false
        result.exitCode == 1
        result.message.contains("Failed to load archive")
    }
    
    def "should handle graph service initialization failure in execute"() {
        given: "a valid file but graph service fails to initialize"
        def cldfFile = createValidCLDFFile().toFile()
        loadCommand.archiveFile = cldfFile
        
        mockGraphService.initialize() >> { throw new RuntimeException("Database initialization failed") }

        when: "executing the command"
        def result = loadCommand.execute()

        then: "error is handled gracefully"
        result.success == false
        result.exitCode == 1
        result.message.contains("Failed to load archive")
        result.message.contains("Database initialization failed")
    }
    
    def "should handle graph service import failure in execute"() {
        given: "a valid file but import fails"
        def cldfFile = createValidCLDFFile().toFile()
        loadCommand.archiveFile = cldfFile
        
        mockGraphService.importArchive(_ as CLDFArchive) >> { throw new RuntimeException("Import failed") }

        when: "executing the command"
        def result = loadCommand.execute()

        then: "error is handled gracefully"
        1 * mockGraphService.initialize()
        result.success == false
        result.exitCode == 1
        result.message.contains("Failed to load archive")
        result.message.contains("Import failed")
    }
    
    def "should handle statistics collection errors during execute"() {
        given: "a valid file but statistics queries fail"
        def cldfFile = createValidCLDFFile().toFile()
        loadCommand.archiveFile = cldfFile
        loadCommand.showStats = true
        
        // Mock failed statistics queries
        mockGraphService.executeCypher(_ as String, _ as Map) >> { throw new RuntimeException("Query failed") }

        when: "executing the command"
        def result = loadCommand.execute()

        then: "command still succeeds but stats are minimal"
        1 * mockGraphService.initialize()
        1 * mockGraphService.importArchive(_ as CLDFArchive)
        result.success == true
        result.data.containsKey("importTimeMs")
        result.data.containsKey("file")
        result.data.containsKey("fileSize")
    }
    
    def "should properly collect all statistics during execute"() {
        given: "a valid file with all statistics available"
        def cldfFile = createValidCLDFFile().toFile()
        loadCommand.archiveFile = cldfFile
        loadCommand.showStats = true
        
        setupMockStatistics()

        when: "executing the command"
        def result = loadCommand.execute()

        then: "all statistics are collected"
        1 * mockGraphService.initialize()
        1 * mockGraphService.importArchive(_ as CLDFArchive)
        result.success == true
        result.data.locations == 5L
        result.data.sessions == 10L
        result.data.climbs == 50L
        result.data.routes == 20L
        result.data.climbers == 3L
        result.data.tags == 15L
        result.data.partnerRelationships == 8L
        result.data.sessionClimbRelationships == 50L
        result.data.routeClimbRelationships == 45L
    }
    
    private void setupMockStatistics() {
        mockGraphService.executeCypher("MATCH (n:Location) RETURN COUNT(n) as count", [:]) >> [[count: 5L]]
        mockGraphService.executeCypher("MATCH (n:Session) RETURN COUNT(n) as count", [:]) >> [[count: 10L]]
        mockGraphService.executeCypher("MATCH (n:Climb) RETURN COUNT(n) as count", [:]) >> [[count: 50L]]
        mockGraphService.executeCypher("MATCH (n:Route) RETURN COUNT(n) as count", [:]) >> [[count: 20L]]
        mockGraphService.executeCypher("MATCH (n:Climber) RETURN COUNT(n) as count", [:]) >> [[count: 3L]]
        mockGraphService.executeCypher("MATCH (n:Tag) RETURN COUNT(n) as count", [:]) >> [[count: 15L]]
        mockGraphService.executeCypher("MATCH ()-[r:PARTNERED_WITH]->() RETURN COUNT(r) as count", [:]) >> [[count: 8L]]
        mockGraphService.executeCypher("MATCH ()-[r:INCLUDES_CLIMB]->() RETURN COUNT(r) as count", [:]) >> [[count: 50L]]
        mockGraphService.executeCypher("MATCH ()-[r:ON_ROUTE]->() RETURN COUNT(r) as count", [:]) >> [[count: 45L]]
    }
    
    private Path createValidCLDFFile() {
        def archive = createTestArchive()
        def file = tempDir.resolve("test.cldf")
        new CLDFWriter(false).write(archive, file.toFile())
        return file
    }
    
    private CLDFArchive createTestArchive(int locationCount = 5, int sessionCount = 10, int climbCount = 50) {
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
                .id("sess_$i")
                .date(LocalDate.of(2024, 1, Math.min(i, 28)))
                .location("Location ${(i % locationCount) + 1}")
                .locationId("${(i % locationCount) + 1}")
                .isIndoor(i % 2 == 0)
                .sessionType(SessionType.INDOOR_CLIMBING)
                .build()
        }
        
        def climbs = (1..climbCount).collect { i ->
            Climb.builder()
                .id(i)
                .sessionId((i % sessionCount) + 1)
                .date(LocalDate.of(2024, 1, 15))
                .routeName("Route $i")
                .type(ClimbType.BOULDER)
                .finishType(FinishType.TOP)
                .attempts(1)
                .grades(Climb.GradeInfo.builder()
                    .system(GradeSystem.V_SCALE)
                    .grade("V${i % 10}")
                    .build())
                .isIndoor(true)
                .rating(4)
                .build()
        }
        
        return CLDFArchive.builder()
            .manifest(manifest)
            .locations(locations)
            .sessions(sessions)
            .climbs(climbs)
            .checksums(Checksums.builder().algorithm("SHA-256").build())
            .build()
    }
}
