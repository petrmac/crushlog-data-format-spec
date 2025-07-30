package io.cldf.tool.commands

import spock.lang.Specification
import spock.lang.Unroll
import io.cldf.tool.services.GraphService
import io.cldf.tool.utils.OutputHandler
import io.cldf.tool.models.CommandResult
import io.cldf.tool.utils.OutputFormat

class GraphQueryCommandSpec extends Specification {
    
    GraphQueryCommand command
    GraphService mockGraphService
    OutputHandler mockOutput
    
    def setup() {
        mockGraphService = Mock(GraphService)
        mockOutput = Mock(OutputHandler)
        
        command = new GraphQueryCommand(mockGraphService)
        command.output = mockOutput
        command.outputFormat = OutputFormat.text
        command.limit = 100  // Set default limit
    }
    
    def "should handle missing query and template options"() {
        when:
        def result = command.execute()
        
        then:
        !result.success
        result.message == "Either --query or --template must be specified"
        result.exitCode == 1
    }
    
    def "should execute raw Cypher query"() {
        given:
        command.cypherQuery = "MATCH (n) RETURN n LIMIT 10"
        command.limit = 10
        
        def queryResults = [
            [n: [id: 1, name: "Test"]],
            [n: [id: 2, name: "Test2"]]
        ]
        
        when:
        def result = command.execute()
        
        then:
        1 * mockGraphService.executeCypher("MATCH (n) RETURN n LIMIT 10", [:]) >> queryResults
        
        result.success
        result.message == "Query executed successfully"
        result.data != null
        result.data.query == "MATCH (n) RETURN n LIMIT 10"
        result.data.results == queryResults
        result.data.count == 2
    }
    
    def "should execute query with parameters"() {
        given:
        command.cypherQuery = "MATCH (n:Climb) WHERE n.grade = \$grade RETURN n"
        command.parameters = [grade: "5.10a"]
        command.limit = 100
        
        def queryResults = [[n: [id: 1, grade: "5.10a"]]]
        
        when:
        def result = command.execute()
        
        then:
        1 * mockGraphService.executeCypher("MATCH (n:Climb) WHERE n.grade = \$grade RETURN n LIMIT 100", [grade: "5.10a"]) >> queryResults
        
        result.success
        result.data.parameters == [grade: "5.10a"]
        result.data.results == queryResults
    }
    
    def "should not add LIMIT if query already contains it"() {
        given:
        command.cypherQuery = "MATCH (n) RETURN n LIMIT 5"
        command.limit = 100
        
        when:
        def result = command.execute()
        
        then:
        1 * mockGraphService.executeCypher("MATCH (n) RETURN n LIMIT 5", [:]) >> []
        
        result.success
    }
    
    def "should handle query execution errors"() {
        given:
        command.cypherQuery = "INVALID QUERY"
        
        when:
        def result = command.execute()
        
        then:
        1 * mockGraphService.executeCypher("INVALID QUERY LIMIT 100", [:]) >> { throw new RuntimeException("Invalid syntax") }
        
        !result.success
        result.message == "Query execution failed: Invalid syntax"
        result.exitCode == 1
    }
    
    @Unroll
    def "should execute template query: #templateName"() {
        given:
        command.template = templateName
        command.limit = 50
        
        when:
        def result = command.execute()
        
        then:
        1 * mockGraphService.executeCypher(_, [:]) >> []
        
        result.success
        
        where:
        templateName << ["grade-pyramid", "recent-sends", "project-routes", "climbing-partners", 
                        "location-stats", "progression-analysis", "weakness-finder"]
    }
    
    def "should output text results in table format"() {
        given:
        def queryResults = [
            [name: "Route 1", grade: "5.10a", attempts: 3],
            [name: "Route 2", grade: "5.10b", attempts: 1]
        ]
        
        def result = CommandResult.builder()
            .success(true)
            .message("Query executed successfully")
            .data([
                query: "MATCH (n) RETURN n",
                parameters: [:],
                results: queryResults,
                count: 2
            ])
            .build()
        
        when:
        command.outputText(result)
        
        then:
        1 * mockOutput.write("Found 2 results")
        1 * mockOutput.write("")
        1 * mockOutput.write(_) // header
        1 * mockOutput.write("-" * 80)
        2 * mockOutput.write(_) // rows
    }
    
    def "should handle empty results"() {
        given:
        def result = CommandResult.builder()
            .success(true)
            .message("Query executed successfully")
            .data([
                query: "MATCH (n) RETURN n",
                parameters: [:],
                results: [],
                count: 0
            ])
            .build()
        
        when:
        command.outputText(result)
        
        then:
        1 * mockOutput.write("Found 0 results")
        1 * mockOutput.write("")
        1 * mockOutput.write("No results found")
    }
    
    def "should output error message for failed result"() {
        given:
        def result = CommandResult.builder()
            .success(false)
            .message("Query failed")
            .build()
        
        when:
        command.outputText(result)
        
        then:
        1 * mockOutput.writeError("Query failed")
    }
    
    def "should execute query and return results in JSON format"() {
        given:
        command.outputFormat = OutputFormat.json
        command.cypherQuery = "MATCH (n) RETURN n"
        def queryResults = [[n: [id: 1, name: "Test"]]]
        
        when:
        def result = command.execute()
        
        then:
        1 * mockGraphService.executeCypher("MATCH (n) RETURN n LIMIT 100", [:]) >> queryResults
        
        result.success
        result.data != null
        result.data.results == queryResults
        // The actual JSON output would be handled by handleResult() which calls System.exit()
        // so we verify the result structure is correct for JSON formatting
    }
    
    def "should test parseParameter method for different types"() {
        when:
        def intResult = command.parseParameter("123")
        def floatResult = command.parseParameter("123.45")
        def boolTrue = command.parseParameter("true")
        def boolFalse = command.parseParameter("false")
        def stringResult = command.parseParameter("hello")
        
        then:
        intResult == 123
        floatResult == 123.45
        boolTrue == true
        boolFalse == false
        stringResult == "hello"
    }
    
    def "should get template query for grade-pyramid"() {
        when:
        def query = command.getTemplateQuery("grade-pyramid")
        
        then:
        query.contains("MATCH (c:Climb)")
        query.contains("WITH c.grade as grade, COUNT(*) as count")
    }
    
    def "should get template query for recent-sends"() {
        when:
        def query = command.getTemplateQuery("recent-sends")
        
        then:
        query.contains("MATCH (c:Climb)")
        query.contains("ORDER BY c.date DESC")
    }
    
    def "should handle unknown template"() {
        when:
        command.getTemplateQuery("unknown-template")
        
        then:
        thrown(IllegalArgumentException)
    }
    
    def "should test QueryTemplate completion candidates"() {
        when:
        def templates = new GraphQueryCommand.QueryTemplate()
        
        then:
        templates.size() == 7
        templates.contains("grade-pyramid")
        templates.contains("recent-sends")
        templates.contains("project-routes")
        templates.contains("climbing-partners")
        templates.contains("location-stats")
        templates.contains("progression-analysis")
        templates.contains("weakness-finder")
    }
    
    def "should log info when archive file is provided"() {
        given:
        command.archiveFile = "test.cldf"
        command.cypherQuery = "MATCH (n) RETURN n"
        
        when:
        def result = command.execute()
        
        then:
        1 * mockGraphService.executeCypher(_, _) >> []
        result.success
        // Note: We can't verify logInfo calls without Spy, but the test still validates the core functionality
    }
    
    def "should parse numeric parameters correctly"() {
        given:
        command.cypherQuery = "MATCH (c:Climb) WHERE c.attempts = \$attempts AND c.rating = \$rating RETURN c"
        command.parameters = [attempts: "5", rating: "4.5"]
        
        when:
        def result = command.execute()
        
        then:
        1 * mockGraphService.executeCypher(_, [attempts: 5, rating: 4.5]) >> []
        
        result.success
    }
    
    def "should get all template queries correctly"() {
        expect:
        command.getTemplateQuery("grade-pyramid").contains("WHERE c.finishType IN ['redpoint', 'flash', 'onsight']")
        command.getTemplateQuery("recent-sends").contains("AND c.date >= date() - duration('P30D')")
        command.getTemplateQuery("project-routes").contains("WHERE c.finishType NOT IN ['redpoint', 'flash', 'onsight']")
        command.getTemplateQuery("climbing-partners").contains("MATCH (c1:Climber)-[:PARTNERED_WITH]-(c2:Climber)")
        command.getTemplateQuery("location-stats").contains("MATCH (l:Location)<-[:AT_LOCATION]-(s:Session)")
        command.getTemplateQuery("progression-analysis").contains("date.truncate('month', c.date)")
        command.getTemplateQuery("weakness-finder").contains("WHERE c.finishType = 'fall' OR c.attempts > 5")
    }
    
    def "should handle case-insensitive boolean parsing"() {
        expect:
        command.parseParameter("TRUE") == true
        command.parseParameter("FALSE") == false
        command.parseParameter("True") == true
        command.parseParameter("False") == false
    }
}