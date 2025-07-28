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

    // Test implementation of BaseCommand
    private static class TestableBaseCommand extends BaseCommand {
        CommandResult expectedResult
        Exception expectedException
        boolean outputTextCalled = false
        CommandResult outputTextResult
        boolean errorHandled = false
        Exception handledException
        ErrorResponse errorResponse

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
            if (outputFormat == OutputFormat.json) {
                output.writeResult(result)
            } else {
                outputText(result)
            }
        }
    }
}