package io.cldf.tool.commands

import io.cldf.api.CLDF
import io.cldf.api.CLDFArchive
import io.cldf.models.*
import io.cldf.tool.models.CommandResult
import io.cldf.tool.services.GraphService
import io.cldf.tool.utils.OutputHandler
import picocli.CommandLine
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.TempDir

import java.nio.file.Path
import java.time.LocalDate
import java.time.OffsetDateTime

class LoadCommandSpec extends Specification {
    
    @TempDir
    Path tempDir
    
    GraphService mockGraphService = Mock()
    OutputHandler mockOutput = Mock()
    
    @Subject
    LoadCommand loadCommand
    
    def setup() {
        loadCommand = new LoadCommand()
        loadCommand.graphService = mockGraphService
        loadCommand.output = mockOutput
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
}