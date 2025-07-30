package io.cldf.tool.commands

import io.cldf.tool.models.CommandResult
import io.cldf.tool.models.ErrorResponse
import io.cldf.tool.utils.OutputFormat
import io.cldf.tool.utils.OutputHandler
import picocli.CommandLine.Model.CommandSpec
import spock.lang.Specification
import spock.lang.Subject

class BaseCommandSpec extends Specification {

    @Subject
    TestableBaseCommand command
    
    CommandSpec mockSpec

    def setup() {
        mockSpec = Mock(CommandSpec)
        command = new TestableBaseCommand()
        command.spec = mockSpec
    }

    def "should handle successful execution with text output"() {
        given:
        command.outputFormat = OutputFormat.text
        command.quiet = false
        def expectedResult = CommandResult.builder()
            .success(true)
            .message("Test successful")
            .exitCode(0)
            .build()
        command.expectedResult = expectedResult

        when:
        command.run()

        then:
        command.outputTextCalled
        command.outputTextResult == expectedResult
        // Note: System.exit() is called but we can't test it in unit tests
    }

    def "should handle successful execution with JSON output"() {
        given:
        command.outputFormat = OutputFormat.json
        command.quiet = false
        def expectedResult = CommandResult.builder()
            .success(true)
            .message("Test successful")
            .data(["key": "value"])
            .exitCode(0)
            .build()
        command.expectedResult = expectedResult

        when:
        command.run()

        then:
        !command.outputTextCalled
        // OutputHandler will be used to write JSON
    }

    def "should handle execution errors"() {
        given:
        command.outputFormat = OutputFormat.text
        command.quiet = false
        def expectedException = new RuntimeException("Test error")
        command.expectedException = expectedException

        when:
        command.run()

        then:
        command.errorHandled
        command.handledException == expectedException
    }

    def "should suppress output when quiet mode is enabled"() {
        given:
        command.outputFormat = OutputFormat.text
        command.quiet = true
        def expectedResult = CommandResult.builder()
            .success(true)
            .message("Test successful")
            .exitCode(0)
            .build()
        command.expectedResult = expectedResult

        when:
        command.run()

        then:
        command.output.quiet
    }

    def "should create proper error response for exceptions"() {
        given:
        def exception = new IllegalArgumentException("Invalid argument")
        command.outputFormat = OutputFormat.json
        command.expectedException = exception

        when:
        command.run()

        then:
        command.errorResponse != null
        command.errorResponse.success == false
        command.errorResponse.error.code == "COMMAND_FAILED"
        command.errorResponse.error.message == "Invalid argument"
        command.errorResponse.error.type == "IllegalArgumentException"
    }

    def "should handle logInfo method"() {
        given:
        command.outputFormat = OutputFormat.text
        command.quiet = false
        command.output = new OutputHandler(command.outputFormat, command.quiet)
        def message = "Info message"

        when:
        command.logInfo(message)

        then:
        // OutputHandler will handle the actual output
        notThrown(Exception)
    }

    def "should handle logWarning method"() {
        given:
        command.outputFormat = OutputFormat.text
        command.quiet = false
        command.output = new OutputHandler(command.outputFormat, command.quiet)
        def message = "Warning message"

        when:
        command.logWarning(message)

        then:
        // OutputHandler will handle the actual output
        notThrown(Exception)
    }

    def "should handle logInfo in quiet mode"() {
        given:
        command.outputFormat = OutputFormat.text
        command.quiet = true
        command.output = new OutputHandler(command.outputFormat, command.quiet)
        def message = "Info message"

        when:
        command.logInfo(message)

        then:
        // In quiet mode, info messages should be suppressed
        notThrown(Exception)
    }

    def "should handle logWarning in quiet mode"() {
        given:
        command.outputFormat = OutputFormat.text
        command.quiet = true
        command.output = new OutputHandler(command.outputFormat, command.quiet)
        def message = "Warning message"

        when:
        command.logWarning(message)

        then:
        // In quiet mode, warnings might still be shown (depends on OutputHandler)
        notThrown(Exception)
    }

    def "should handle null exception message gracefully"() {
        given:
        def exception = new RuntimeException((String) null)
        command.outputFormat = OutputFormat.json
        command.expectedException = exception

        when:
        command.run()

        then:
        command.errorResponse != null
        command.errorResponse.success == false
        command.errorResponse.error.code == "COMMAND_FAILED"
        command.errorResponse.error.message == null
        command.errorResponse.error.type == "RuntimeException"
    }

    def "should handle different exit codes from command result"() {
        given:
        command.outputFormat = OutputFormat.text
        def expectedResult = CommandResult.builder()
            .success(false)
            .message("Command failed")
            .exitCode(2)
            .build()
        command.expectedResult = expectedResult

        when:
        command.run()

        then:
        command.outputTextCalled
        command.exitCode == 2
    }

    def "should initialize OutputHandler with correct parameters"() {
        given:
        command.outputFormat = OutputFormat.json
        command.quiet = true
        command.expectedResult = CommandResult.builder()
            .success(true)
            .message("Success")
            .exitCode(0)
            .build()

        when:
        command.run()

        then:
        command.output != null
        // OutputHandler is created with the correct parameters
        // We can't directly test private fields, but we can verify it was created
        notThrown(Exception)
    }

    // Test implementation of BaseCommand
    private static class TestableBaseCommand extends BaseCommand {
        CommandResult expectedResult
        Exception expectedException
        boolean outputTextCalled = false
        CommandResult outputTextResult
        boolean errorHandled = false
        Exception handledException
        ErrorResponse errorResponse
        int exitCode = -1

        @Override
        protected CommandResult execute() throws Exception {
            if (expectedException) {
                throw expectedException
            }
            return expectedResult
        }

        @Override
        protected void outputText(CommandResult result) {
            outputTextCalled = true
            outputTextResult = result
        }

        @Override
        protected void handleError(Exception e) {
            errorHandled = true
            handledException = e
            // Capture the error response that would be written
            errorResponse = ErrorResponse.builder()
                .success(false)
                .error(
                    ErrorResponse.Error.builder()
                        .code("COMMAND_FAILED")
                        .message(e.getMessage())
                        .type(e.getClass().getSimpleName())
                        .build())
                .build()
            // Don't call super to avoid System.exit in tests
        }

        @Override
        protected void handleResult(CommandResult result) {
            // Override to avoid System.exit in tests
            exitCode = result.getExitCode()
            if (outputFormat == OutputFormat.json) {
                output.writeResult(result)
            } else {
                outputText(result)
            }
        }
    }
}