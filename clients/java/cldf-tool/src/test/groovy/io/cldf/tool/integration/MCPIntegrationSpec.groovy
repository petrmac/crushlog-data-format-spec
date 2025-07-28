package io.cldf.tool.integration

import groovy.json.JsonSlurper
import io.cldf.tool.Application
import io.micronaut.configuration.picocli.PicocliRunner
import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import spock.lang.Specification
import spock.lang.Stepwise
import spock.lang.TempDir

import java.nio.file.Path

/**
 * Integration tests for MCP (Model Context Protocol) features.
 * Tests JSON I/O, stdin/stdout handling, and agent-friendly features.
 */
@Stepwise
class MCPIntegrationSpec extends Specification {

    @TempDir
    Path tempDir

    ApplicationContext ctx
    
    def setup() {
        ctx = ApplicationContext.run(Environment.CLI, Environment.TEST)
    }

    def cleanup() {
        ctx?.close()
    }

    def "should support JSON output for all commands"() {
        given:
        def archiveFile = tempDir.resolve("test.cldf").toFile()
        
        when: "Create command with JSON output"
        def createResult = runCommand("create", "-o", archiveFile.absolutePath, 
                                    "--template", "basic", "--json")
        
        then:
        createResult.exitCode == 0
        def createJson = parseJson(createResult.output)
        createJson.success == true
        createJson.message == "Successfully created CLDF archive"
        createJson.data.file == archiveFile.absolutePath
        createJson.data.stats.locations == 1
        createJson.data.stats.sessions == 1
        createJson.data.stats.climbs == 2

        when: "Validate command with JSON output"
        def validateResult = runCommand("validate", archiveFile.absolutePath, "--json")
        
        then:
        validateResult.exitCode == 0
        def validateJson = parseJson(validateResult.output)
        validateJson.success == true
        validateJson.data.valid == true
        validateJson.data.errors.isEmpty()
    }

    def "should support stdin/stdout piping"() {
        given:
        def createJson = """
        {
            "manifest": {
                "version": "1.0.0",
                "format": "CLDF",
                "creationDate": "2024-01-01T00:00:00Z"
            },
            "locations": [{
                "id": 1,
                "name": "Test Crag",
                "country": "USA",
                "isIndoor": false
            }],
            "sessions": [{
                "id": "session1",
                "date": "2024-01-15",
                "location": "Test Crag",
                "locationId": "1"
            }],
            "climbs": [{
                "id": 1,
                "sessionId": 1,
                "date": "2024-01-15",
                "routeName": "Test Route",
                "grades": {
                    "grade": "5.10a",
                    "system": "yds"
                },
                "finishType": "redpoint",
                "attempts": 3
            }],
            "checksums": {
                "algorithm": "SHA-256"
            }
        }
        """
        
        when: "Create from stdin"
        def createResult = runCommandWithStdin("create", createJson,
            "-o", tempDir.resolve("stdin-test.cldf").toString(), "--from-json", "-", "--json")
        
        then:
        createResult.exitCode == 0
        def resultJson = parseJson(createResult.output)
        resultJson.success == true
        tempDir.resolve("stdin-test.cldf").toFile().exists()
    }

    def "should support graph queries with JSON output"() {
        given:
        def archiveFile = tempDir.resolve("query-test.cldf").toFile()
        runCommand("create", "-o", archiveFile.absolutePath, "--template", "demo")
        
        when: "Load into graph"
        def loadResult = runCommand("load", archiveFile.absolutePath, "--json")
        
        then:
        loadResult.exitCode == 0
        def loadJson = parseJson(loadResult.output)
        loadJson.success == true
        loadJson.data.locations > 0
        loadJson.data.climbs > 0

        when: "Execute graph query with JSON output"
        def queryResult = runCommand("graph-query", 
            "--template", "grade-pyramid", "--json")
        
        then:
        queryResult.exitCode == 0
        def queryJson = parseJson(queryResult.output)
        queryJson.success == true
        queryJson.data.results.size() > 0
        queryJson.data.results.every { it.grade != null && it.count != null }
    }

    def "should handle natural language style queries"() {
        given:
        def archiveFile = tempDir.resolve("nlp-test.cldf").toFile()
        runCommand("create", "-o", archiveFile.absolutePath, "--template", "demo")
        runCommand("load", archiveFile.absolutePath)
        
        when: "Query climbing partners"
        def result = runCommand("graph-query", 
            "--template", "climbing-partners", 
            "--limit", "5",
            "--json")
        
        then:
        result.exitCode == 0
        def json = parseJson(result.output)
        json.success == true
        json.data.results.size() <= 5
        json.data.results.each { partner ->
            assert partner.climber1 != null
            assert partner.climber2 != null
            assert partner.sessions != null
        }
    }

    def "should support complex analytics queries"() {
        given:
        def archiveFile = tempDir.resolve("analytics-test.cldf").toFile()
        runCommand("create", "-o", archiveFile.absolutePath, "--template", "demo")
        runCommand("load", archiveFile.absolutePath)
        
        when: "Run progression analysis"
        def result = runCommand("graph-query",
            "--template", "progression-analysis",
            "--json")
        
        then:
        result.exitCode == 0
        def json = parseJson(result.output)
        json.success == true
        json.data.results.size() > 0
        json.data.results.each { month ->
            assert month.month != null
            assert month.grades != null
            assert month.grades instanceof List
        }
    }

    def "should support parameter passing for queries"() {
        given:
        def archiveFile = tempDir.resolve("param-test.cldf").toFile()
        runCommand("create", "-o", archiveFile.absolutePath, "--template", "demo")
        runCommand("load", archiveFile.absolutePath)
        
        when: "Custom query with parameters"
        def result = runCommand("graph-query",
            "--query", "MATCH (c:Climb) WHERE c.grade = \$grade RETURN c.routeName as route",
            "--param", "grade=5.7",
            "--json")
        
        then:
        result.exitCode == 0
        def json = parseJson(result.output)
        json.success == true
        json.data.parameters.grade == "5.7"
    }

    def "should provide error messages suitable for agents"() {
        when: "Invalid command"
        def result = runCommand("validate", "nonexistent.cldf", "--json")
        
        then:
        result.exitCode == 1
        def json = parseJson(result.output)
        json.success == false
        json.message.contains("File not found")
        json.exitCode == 1
    }

    def "should handle concurrent operations gracefully"() {
        given:
        def archiveFile = tempDir.resolve("concurrent-test.cldf").toFile()
        runCommand("create", "-o", archiveFile.absolutePath, "--template", "demo")
        runCommand("load", archiveFile.absolutePath)
        
        when: "Multiple concurrent queries"
        def futures = (1..5).collect { i ->
            Thread.start {
                runCommand("graph-query",
                    "--template", "grade-pyramid",
                    "--json")
            }
        }
        def results = futures.collect { it.join() }
        
        then:
        results.every { it.exitCode == 0 }
        results.every { result ->
            def json = parseJson(result.output)
            json.success == true && json.data.results.size() > 0
        }
    }

    // Helper methods
    
    private CommandResult runCommand(String... args) {
        def baos = new ByteArrayOutputStream()
        def ps = new PrintStream(baos)
        def oldOut = System.out
        def oldErr = System.err
        
        try {
            System.setOut(ps)
            System.setErr(ps)
            
            def exitCode = PicocliRunner.execute(Application.class, ctx, args)
            def output = baos.toString()
            
            return new CommandResult(exitCode: exitCode, output: output)
        } finally {
            System.setOut(oldOut)
            System.setErr(oldErr)
        }
    }
    
    private CommandResult runCommandWithStdin(String stdin, String... args) {
        def inputStream = new ByteArrayInputStream(stdin.bytes)
        def oldIn = System.in
        
        try {
            System.setIn(inputStream)
            return runCommand(args)
        } finally {
            System.setIn(oldIn)
        }
    }
    
    private Map parseJson(String output) {
        // Extract JSON from output (might have other text)
        def jsonStart = output.indexOf('{')
        if (jsonStart == -1) {
            throw new IllegalArgumentException("No JSON found in output: $output")
        }
        def jsonEnd = output.lastIndexOf('}')
        def json = output.substring(jsonStart, jsonEnd + 1)
        
        return new JsonSlurper().parseText(json)
    }
    
    static class CommandResult {
        int exitCode
        String output
    }
}