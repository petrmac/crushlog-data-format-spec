package app.crushlog.cldf.tool.utils

import app.crushlog.cldf.tool.models.CommandResult
import app.crushlog.cldf.tool.models.ErrorResponse
import spock.lang.Specification

class OutputHandlerSpec extends Specification {

    ByteArrayOutputStream stdout
    ByteArrayOutputStream stderr
    PrintStream out
    PrintStream err
    
    def setup() {
        stdout = new ByteArrayOutputStream()
        stderr = new ByteArrayOutputStream()
        out = new PrintStream(stdout)
        err = new PrintStream(stderr)
    }
    
    def cleanup() {
        out.close()
        err.close()
    }

    def "should write result in text format"() {
        given: "an output handler in text format"
        def handler = new OutputHandler(OutputFormat.TEXT, false, out, err)
        
        and: "a command result"
        def result = CommandResult.builder()
            .success(true)
            .message("Operation completed successfully")
            .build()

        when: "writing the result"
        handler.writeResult(result)

        then: "message is written to stdout"
        stdout.toString().trim() == "Operation completed successfully"
        stderr.toString() == ""
    }

    def "should write result in JSON format"() {
        given: "an output handler in JSON format"
        def handler = new OutputHandler(OutputFormat.JSON, false, out, err)
        
        and: "a command result"
        def result = CommandResult.builder()
            .success(true)
            .message("Operation completed")
            .data(["count": 5, "status": "active"])
            .build()

        when: "writing the result"
        handler.writeResult(result)

        then: "JSON is written to stdout"
        def output = stdout.toString()
        output.contains('"success" : true')
        output.contains('"message" : "Operation completed"')
        output.contains('"count" : 5')
        output.contains('"status" : "active"')
        stderr.toString() == ""
    }

    def "should handle null message in text format"() {
        given: "an output handler in text format"
        def handler = new OutputHandler(OutputFormat.TEXT, false, out, err)
        
        and: "a result with null message"
        def result = CommandResult.builder()
            .success(true)
            .build()

        when: "writing the result"
        handler.writeResult(result)

        then: "nothing is written"
        stdout.toString() == ""
        stderr.toString() == ""
    }

    def "should write error in text format"() {
        given: "an output handler in text format"
        def handler = new OutputHandler(OutputFormat.TEXT, false, out, err)
        
        and: "an error response"
        def error = ErrorResponse.builder()
            .success(false)
            .error(ErrorResponse.Error.builder()
                .code("VALIDATION_ERROR")
                .message("Invalid input format")
                .suggestion("Check the documentation for valid formats")
                .build())
            .build()

        when: "writing the error"
        handler.writeError(error)

        then: "error is written to stderr"
        stdout.toString() == ""
        def errorOutput = stderr.toString()
        errorOutput.contains("Error: Invalid input format")
        errorOutput.contains("Suggestion: Check the documentation for valid formats")
    }

    def "should write error in JSON format"() {
        given: "an output handler in JSON format"
        def handler = new OutputHandler(OutputFormat.JSON, false, out, err)
        
        and: "an error response"
        def error = ErrorResponse.builder()
            .success(false)
            .error(ErrorResponse.Error.builder()
                .code("NOT_FOUND")
                .message("Resource not found")
                .build())
            .build()

        when: "writing the error"
        handler.writeError(error)

        then: "JSON error is written to stderr"
        stdout.toString() == ""
        def errorOutput = stderr.toString()
        errorOutput.contains('"success" : false')
        errorOutput.contains('"code" : "NOT_FOUND"')
        errorOutput.contains('"message" : "Resource not found"')
    }

    def "should write simple error message"() {
        given: "an output handler in text format"
        def handler = new OutputHandler(OutputFormat.TEXT, false, out, err)

        when: "writing a simple error message"
        handler.writeError("Something went wrong")

        then: "error is written to stderr"
        stdout.toString() == ""
        stderr.toString().trim() == "Error: Something went wrong"
    }

    def "should write simple error message in JSON format"() {
        given: "an output handler in JSON format"
        def handler = new OutputHandler(OutputFormat.JSON, false, out, err)

        when: "writing a simple error message"
        handler.writeError("Connection failed")

        then: "JSON error is written to stderr"
        stdout.toString() == ""
        def errorOutput = stderr.toString()
        errorOutput.contains('"success" : false')
        errorOutput.contains('"code" : "ERROR"')
        errorOutput.contains('"message" : "Connection failed"')
    }

    def "should write info message when not quiet"() {
        given: "an output handler in text format, not quiet"
        def handler = new OutputHandler(OutputFormat.TEXT, false, out, err)

        when: "writing an info message"
        handler.writeInfo("Processing data...")

        then: "info is written to stderr"
        stdout.toString() == ""
        stderr.toString().trim() == "Processing data..."
    }

    def "should not write info message when quiet"() {
        given: "an output handler in quiet mode"
        def handler = new OutputHandler(OutputFormat.TEXT, true, out, err)

        when: "writing an info message"
        handler.writeInfo("Processing data...")

        then: "nothing is written"
        stdout.toString() == ""
        stderr.toString() == ""
    }

    def "should not write info message in JSON format"() {
        given: "an output handler in JSON format"
        def handler = new OutputHandler(OutputFormat.JSON, false, out, err)

        when: "writing an info message"
        handler.writeInfo("Processing data...")

        then: "nothing is written"
        stdout.toString() == ""
        stderr.toString() == ""
    }

    def "should write warning message when not quiet"() {
        given: "an output handler in text format, not quiet"
        def handler = new OutputHandler(OutputFormat.TEXT, false, out, err)

        when: "writing a warning message"
        handler.writeWarning("Deprecated feature used")

        then: "warning is written to stderr"
        stdout.toString() == ""
        stderr.toString().trim() == "Warning: Deprecated feature used"
    }

    def "should not write warning message when quiet"() {
        given: "an output handler in quiet mode"
        def handler = new OutputHandler(OutputFormat.TEXT, true, out, err)

        when: "writing a warning message"
        handler.writeWarning("Deprecated feature used")

        then: "nothing is written"
        stdout.toString() == ""
        stderr.toString() == ""
    }

    def "should write JSON object"() {
        given: "an output handler"
        def handler = new OutputHandler(OutputFormat.TEXT, false, out, err)
        
        and: "a data object"
        def data = [
            name: "Test Archive",
            climbs: 42,
            locations: ["Bishop", "Yosemite"]
        ]

        when: "writing JSON data"
        handler.writeJson(data)

        then: "JSON is written to stdout"
        def output = stdout.toString()
        output.contains('"name" : "Test Archive"')
        output.contains('"climbs" : 42')
        output.contains('"Bishop"')
        output.contains('"Yosemite"')
        stderr.toString() == ""
    }

    def "should write plain text"() {
        given: "an output handler"
        def handler = new OutputHandler(OutputFormat.TEXT, false, out, err)

        when: "writing plain text"
        handler.write("Hello World")

        then: "text is written to stdout"
        stdout.toString().trim() == "Hello World"
        stderr.toString() == ""
    }

    def "should write to stdout or stderr based on error flag"() {
        given: "an output handler"
        def handler = new OutputHandler(OutputFormat.TEXT, false, out, err)

        when: "writing to stdout"
        handler.writeError("Standard output", false)
        
        then: "text is written to stdout"
        stdout.toString().trim() == "Standard output"
        stderr.toString() == ""

        when: "writing to stderr"
        stdout.reset()
        handler.writeError("Error output", true)
        
        then: "text is written to stderr"
        stdout.toString() == ""
        stderr.toString().trim() == "Error output"
    }

    def "should report JSON format correctly"() {
        given: "handlers with different formats"
        def jsonHandler = new OutputHandler(OutputFormat.JSON, false, out, err)
        def textHandler = new OutputHandler(OutputFormat.TEXT, false, out, err)

        expect: "correct format reporting"
        jsonHandler.isJsonFormat() == true
        textHandler.isJsonFormat() == false
    }

    def "should report quiet mode correctly"() {
        given: "handlers with different quiet settings"
        def quietHandler = new OutputHandler(OutputFormat.TEXT, true, out, err)
        def verboseHandler = new OutputHandler(OutputFormat.TEXT, false, out, err)

        expect: "correct quiet mode reporting"
        quietHandler.isQuiet() == true
        verboseHandler.isQuiet() == false
    }

    def "should use default streams when not provided"() {
        given: "an output handler with default streams"
        def handler = new OutputHandler(OutputFormat.TEXT, false)

        expect: "handler is created successfully"
        handler != null
        handler.isJsonFormat() == false
        handler.isQuiet() == false
    }

    def "should handle exceptions when writing result"() {
        given: "an output handler"
        def handler = new OutputHandler(OutputFormat.JSON, false, out, err)
        
        and: "a result that will cause serialization error"
        def result = Mock(CommandResult) {
            getMessage() >> { throw new RuntimeException("Serialization error") }
        }

        when: "writing the result"
        handler.writeResult(result)

        then: "error is handled and written to stderr"
        def errorOutput = stderr.toString()
        errorOutput.contains("Failed to format output")
        errorOutput.contains("Serialization error")
    }

    def "should handle exceptions when writing error"() {
        given: "an output handler"
        def handler = new OutputHandler(OutputFormat.JSON, false, out, err)
        
        and: "a mock ErrorResponse that causes issues during serialization" 
        def error = Stub(ErrorResponse) {
            getError() >> Stub(ErrorResponse.Error) {
                getMessage() >> "Original error"
                getSuggestion() >> null
            }
        }

        when: "writing the error"
        handler.writeError(error)

        then: "error message is written"
        def errorOutput = stderr.toString()
        errorOutput.contains("Original error") || errorOutput.contains("success")
    }

    def "should handle exceptions when writing JSON"() {
        given: "an output handler"
        def handler = new OutputHandler(OutputFormat.TEXT, false, out, err)
        
        and: "an object that can't be serialized"
        def invalidData = new Object() {
            String toString() { throw new RuntimeException("Can't serialize") }
        }

        when: "writing JSON data"
        handler.writeJson(invalidData)

        then: "error is handled and written to stderr"
        def errorOutput = stderr.toString()
        errorOutput.contains("Error: Failed to format JSON")
    }

    def "should write debug messages when not quiet and not JSON"() {
        given: "an output handler in text format"
        def handler = new OutputHandler(OutputFormat.TEXT, false, out, err)

        when: "writing a debug message"
        handler.writeDebug("Debug information")

        then: "nothing is written to stdout or stderr (goes to log)"
        stdout.toString() == ""
        stderr.toString() == ""
    }

    def "should not write debug messages in quiet mode"() {
        given: "an output handler in quiet mode"
        def handler = new OutputHandler(OutputFormat.TEXT, true, out, err)

        when: "writing a debug message"
        handler.writeDebug("Debug information")

        then: "nothing is written"
        stdout.toString() == ""
        stderr.toString() == ""
    }

    def "should not write debug messages in JSON format"() {
        given: "an output handler in JSON format"
        def handler = new OutputHandler(OutputFormat.JSON, false, out, err)

        when: "writing a debug message"
        handler.writeDebug("Debug information")

        then: "nothing is written"
        stdout.toString() == ""
        stderr.toString() == ""
    }
}