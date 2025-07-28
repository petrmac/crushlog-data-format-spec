package io.cldf.tool.utils

import io.cldf.tool.models.CommandResult
import io.cldf.tool.models.ErrorResponse
import spock.lang.Specification
import spock.lang.Subject

class OutputHandlerSpec extends Specification {
    
    ByteArrayOutputStream outStream
    ByteArrayOutputStream errStream
    PrintStream out
    PrintStream err
    
    @Subject
    OutputHandler handler
    
    def setup() {
        outStream = new ByteArrayOutputStream()
        errStream = new ByteArrayOutputStream()
        out = new PrintStream(outStream)
        err = new PrintStream(errStream)
    }
    
    def "should write text output to stdout"() {
        given:
        handler = new OutputHandler(OutputFormat.text, false, out, err)
        def result = CommandResult.success("Operation completed")
        
        when:
        handler.writeResult(result)
        
        then:
        outStream.toString().trim() == "Operation completed"
        errStream.size() == 0
    }
    
    def "should write JSON output to stdout"() {
        given:
        handler = new OutputHandler(OutputFormat.json, false, out, err)
        def result = CommandResult.success("Operation completed", ["count": 5])
        
        when:
        handler.writeResult(result)
        
        then:
        def output = outStream.toString()
        output.contains('"success" : true')
        output.contains('"message" : "Operation completed"')
        output.contains('"count" : 5')
        errStream.size() == 0
    }
    
    def "should write error to stderr in text mode"() {
        given:
        handler = new OutputHandler(OutputFormat.text, false, out, err)
        def error = ErrorResponse.builder()
            .success(false)
            .error(ErrorResponse.Error.builder()
                .code("TEST_ERROR")
                .message("Something went wrong")
                .suggestion("Try again")
                .build())
            .build()
        
        when:
        handler.writeError(error)
        
        then:
        errStream.toString().contains("Error: Something went wrong")
        errStream.toString().contains("Suggestion: Try again")
        outStream.size() == 0
    }
    
    def "should write error as JSON to stderr in JSON mode"() {
        given:
        handler = new OutputHandler(OutputFormat.json, false, out, err)
        def error = ErrorResponse.builder()
            .success(false)
            .error(ErrorResponse.Error.builder()
                .code("TEST_ERROR")
                .message("Something went wrong")
                .build())
            .build()
        
        when:
        handler.writeError(error)
        
        then:
        def output = errStream.toString()
        output.contains('"success" : false')
        output.contains('"code" : "TEST_ERROR"')
        output.contains('"message" : "Something went wrong"')
        outStream.size() == 0
    }
    
    def "should suppress info messages in quiet mode"() {
        given:
        handler = new OutputHandler(OutputFormat.text, true, out, err)
        
        when:
        handler.writeInfo("This is info")
        handler.writeWarning("This is warning")
        
        then:
        outStream.size() == 0
        errStream.size() == 0
    }
    
    def "should suppress info messages in JSON mode"() {
        given:
        handler = new OutputHandler(OutputFormat.json, false, out, err)
        
        when:
        handler.writeInfo("This is info")
        handler.writeWarning("This is warning")
        
        then:
        outStream.size() == 0
        errStream.size() == 0
    }
    
    def "should write info and warnings to stderr in text mode when not quiet"() {
        given:
        handler = new OutputHandler(OutputFormat.text, false, out, err)
        
        when:
        handler.writeInfo("Processing...")
        handler.writeWarning("Deprecated feature")
        
        then:
        errStream.toString().contains("Processing...")
        errStream.toString().contains("Warning: Deprecated feature")
        outStream.size() == 0
    }
    
    def "should handle complex JSON objects"() {
        given:
        handler = new OutputHandler(OutputFormat.json, false, out, err)
        def data = [
            "stats": [
                "total": 100,
                "processed": 95,
                "failed": 5
            ],
            "items": ["a", "b", "c"]
        ]
        
        when:
        handler.writeJson(data)
        
        then:
        def output = outStream.toString()
        output.contains('"total" : 100')
        output.contains('"processed" : 95')
        output.contains('"items" : [ "a", "b", "c" ]')
    }
    
    def "should handle JSON serialization errors gracefully"() {
        given:
        handler = new OutputHandler(OutputFormat.json, false, out, err)
        def problematicObject = new Object() {
            def cyclicRef = this
        }
        
        when:
        handler.writeJson(problematicObject)
        
        then:
        errStream.toString().contains("Error: Failed to format JSON")
        outStream.size() == 0
    }
}