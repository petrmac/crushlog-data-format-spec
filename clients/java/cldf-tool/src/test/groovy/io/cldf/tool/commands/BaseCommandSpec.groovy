package io.cldf.tool.commands

import io.cldf.tool.models.CommandResult
import io.cldf.tool.utils.OutputFormat
import io.cldf.tool.utils.OutputHandler
import spock.lang.Specification

class BaseCommandSpec extends Specification {
    
    BaseCommand command
    ByteArrayOutputStream outStream
    ByteArrayOutputStream errStream
    
    def setup() {
        outStream = new ByteArrayOutputStream()
        errStream = new ByteArrayOutputStream()
        
        // Create a test implementation of BaseCommand
        command = new BaseCommand() {
            @Override
            protected CommandResult execute() throws Exception {
                return CommandResult.success("Test passed", ["data": "value"])
            }
            
            @Override
            protected void outputText(CommandResult result) {
                output.write(result.getMessage())
            }
        }
    }
    
    def "should handle successful execution in text mode"() {
        given:
        command.outputFormat = OutputFormat.text
        command.quiet = false
        
        when:
        command.run()
        
        then:
        noExceptionThrown()
    }
    
    def "should handle successful execution in JSON mode"() {
        given:
        command.outputFormat = OutputFormat.json
        command.quiet = false
        command.output = new OutputHandler(OutputFormat.json, false,
            new PrintStream(outStream), new PrintStream(errStream))
        
        // Override execute to avoid System.exit
        command = new BaseCommand() {
            @Override
            protected CommandResult execute() throws Exception {
                return CommandResult.success("Test passed", ["data": "value"])
            }
            
            @Override
            protected void outputText(CommandResult result) {
                // Not used in JSON mode
            }
            
            @Override
            protected void handleResult(CommandResult result) {
                output.writeResult(result)
                // Don't call System.exit in tests
            }
        }
        command.outputFormat = OutputFormat.json
        command.output = new OutputHandler(OutputFormat.json, false,
            new PrintStream(outStream), new PrintStream(errStream))
        
        when:
        command.run()
        
        then:
        def output = outStream.toString()
        output.contains('"success" : true')
        output.contains('"message" : "Test passed"')
        output.contains('"data"')
    }
    
    def "should handle exceptions properly"() {
        given:
        command = new BaseCommand() {
            @Override
            protected CommandResult execute() throws Exception {
                throw new RuntimeException("Test error")
            }
            
            @Override
            protected void outputText(CommandResult result) {
                // Not reached
            }
            
            @Override
            protected void handleError(Exception e) {
                // Override to avoid System.exit
                output.writeError(e.getMessage())
            }
        }
        command.outputFormat = OutputFormat.text
        command.output = new OutputHandler(OutputFormat.text, false,
            new PrintStream(outStream), new PrintStream(errStream))
        
        when:
        command.run()
        
        then:
        errStream.toString().contains("Error: Test error")
    }
    
    def "should handle exceptions in JSON mode"() {
        given:
        command = new BaseCommand() {
            @Override
            protected CommandResult execute() throws Exception {
                throw new IllegalArgumentException("Invalid input")
            }
            
            @Override
            protected void outputText(CommandResult result) {
                // Not reached
            }
            
            @Override
            protected void handleError(Exception e) {
                // Override to avoid System.exit
                def error = io.cldf.tool.models.ErrorResponse.builder()
                    .success(false)
                    .error(io.cldf.tool.models.ErrorResponse.Error.builder()
                        .code("COMMAND_FAILED")
                        .message(e.getMessage())
                        .type(e.getClass().getSimpleName())
                        .build())
                    .build()
                output.writeError(error)
            }
        }
        command.outputFormat = OutputFormat.json
        command.output = new OutputHandler(OutputFormat.json, false,
            new PrintStream(outStream), new PrintStream(errStream))
        
        when:
        command.run()
        
        then:
        def error = errStream.toString()
        error.contains('"success" : false')
        error.contains('"code" : "COMMAND_FAILED"')
        error.contains('"message" : "Invalid input"')
        error.contains('"type" : "IllegalArgumentException"')
    }
    
    def "should respect quiet mode"() {
        given:
        command = new BaseCommand() {
            @Override
            protected CommandResult execute() throws Exception {
                logInfo("This should not appear")
                logWarning("This warning should not appear")
                return CommandResult.success("Done")
            }
            
            @Override
            protected void outputText(CommandResult result) {
                output.write(result.getMessage())
            }
            
            @Override
            protected void handleResult(CommandResult result) {
                outputText(result)
                // Don't call System.exit in tests
            }
        }
        command.outputFormat = OutputFormat.text
        command.quiet = true
        command.output = new OutputHandler(OutputFormat.text, true,
            new PrintStream(outStream), new PrintStream(errStream))
        
        when:
        command.run()
        
        then:
        outStream.toString().trim() == "Done"
        errStream.size() == 0  // No info/warning output
    }
}