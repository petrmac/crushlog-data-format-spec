package app.crushlog.cldf.tool.utils

import spock.lang.Specification
import spock.lang.Unroll

class ConsoleUtilsSpec extends Specification {

    PrintStream originalOut
    PrintStream originalErr
    ByteArrayOutputStream outContent
    ByteArrayOutputStream errContent

    def setup() {
        // Save original streams
        originalOut = System.out
        originalErr = System.err
        
        // Create new streams for capturing output
        outContent = new ByteArrayOutputStream()
        errContent = new ByteArrayOutputStream()
        
        // Redirect System.out and System.err
        System.setOut(new PrintStream(outContent))
        System.setErr(new PrintStream(errContent))
    }

    def cleanup() {
        // Restore original streams
        System.setOut(originalOut)
        System.setErr(originalErr)
    }

    def "should print header with borders"() {
        given: "a header text"
        def text = "Test Header"

        when: "printing header"
        ConsoleUtils.printHeader(text)

        then: "header is formatted correctly"
        def output = outContent.toString()
        output.contains("===============")
        output.contains("| Test Header |")
        output.contains("===============")
    }

    def "should print section with dashes"() {
        given: "a section text"
        def text = "Test Section"

        when: "printing section"
        ConsoleUtils.printSection(text)

        then: "section is formatted correctly"
        outContent.toString().contains("--- Test Section ---")
    }

    def "should print success message with checkmark"() {
        given: "a success message"
        def message = "Operation completed"

        when: "printing success"
        ConsoleUtils.printSuccess(message)

        then: "success message has checkmark"
        outContent.toString().contains("✓ Operation completed")
    }

    def "should print error message to stderr with X mark"() {
        given: "an error message"
        def message = "Operation failed"

        when: "printing error"
        ConsoleUtils.printError(message)

        then: "error message is on stderr with X mark"
        errContent.toString().contains("✗ Operation failed")
        outContent.toString().isEmpty()
    }

    def "should print warning message with warning symbol"() {
        given: "a warning message"
        def message = "Be careful"

        when: "printing warning"
        ConsoleUtils.printWarning(message)

        then: "warning message has warning symbol"
        outContent.toString().contains("⚠ Be careful")
    }

    def "should print info message with info symbol"() {
        given: "an info message"
        def message = "For your information"

        when: "printing info"
        ConsoleUtils.printInfo(message)

        then: "info message has info symbol"
        outContent.toString().contains("ℹ For your information")
    }

    @Unroll
    def "should handle confirm with response '#response' expecting #expected"() {
        given: "a scanner with user input"
        def scanner = new Scanner(new ByteArrayInputStream("${response}\n".bytes))

        when: "confirming action"
        def result = ConsoleUtils.confirm(scanner, "Continue?")

        then: "returns expected result"
        result == expected
        outContent.toString().contains("Continue? (y/n):")

        where:
        response | expected
        "y"      | true
        "Y"      | true
        "yes"    | true
        "YES"    | true
        "n"      | false
        "N"      | false
        "no"     | false
        "maybe"  | false
        ""       | false
    }

    def "should handle prompt with default value"() {
        given: "a scanner with empty input"
        def scanner = new Scanner(new ByteArrayInputStream("\n".bytes))
        def defaultValue = "default"

        when: "prompting with default"
        def result = ConsoleUtils.prompt(scanner, "Enter value", defaultValue)

        then: "returns default value"
        result == defaultValue
        outContent.toString().contains("Enter value [default]:")
    }

    def "should handle prompt without default value"() {
        given: "a scanner with user input"
        def scanner = new Scanner(new ByteArrayInputStream("user input\n".bytes))

        when: "prompting without default"
        def result = ConsoleUtils.prompt(scanner, "Enter value", null)

        then: "returns user input"
        result == "user input"
        outContent.toString().contains("Enter value:")
        !outContent.toString().contains("[")
    }

    def "should handle prompt with empty default value"() {
        given: "a scanner with user input"
        def scanner = new Scanner(new ByteArrayInputStream("test\n".bytes))

        when: "prompting with empty default"
        def result = ConsoleUtils.prompt(scanner, "Enter value", "")

        then: "returns user input"
        result == "test"
        outContent.toString().contains("Enter value:")
        !outContent.toString().contains("[")
    }

    def "should handle promptInt with valid input"() {
        given: "a scanner with valid integer"
        def scanner = new Scanner(new ByteArrayInputStream("42\n".bytes))

        when: "prompting for integer"
        def result = ConsoleUtils.promptInt(scanner, "Enter number", 10)

        then: "returns parsed integer"
        result == 42
        outContent.toString().contains("Enter number [10]:")
    }

    def "should handle promptInt with empty input and default"() {
        given: "a scanner with empty input"
        def scanner = new Scanner(new ByteArrayInputStream("\n".bytes))

        when: "prompting for integer with default"
        def result = ConsoleUtils.promptInt(scanner, "Enter number", 10)

        then: "returns default value"
        result == 10
    }

    def "should handle promptInt with invalid input then valid"() {
        given: "a scanner with invalid then valid input"
        def scanner = new Scanner(new ByteArrayInputStream("abc\n25\n".bytes))

        when: "prompting for integer"
        def result = ConsoleUtils.promptInt(scanner, "Enter number", -1)

        then: "shows error and returns valid input"
        result == 25
        errContent.toString().contains("✗ Invalid number: abc")
    }

    def "should handle promptInt with negative default value"() {
        given: "a scanner with input"
        def scanner = new Scanner(new ByteArrayInputStream("5\n".bytes))

        when: "prompting with negative default"
        def result = ConsoleUtils.promptInt(scanner, "Enter number", -1)

        then: "does not show default in prompt"
        result == 5
        outContent.toString().contains("Enter number:")
        !outContent.toString().contains("[-1]")
    }

    def "should print progress bar at 0%"() {
        when: "printing progress at start"
        ConsoleUtils.printProgress(0, 100)

        then: "shows empty progress bar"
        def output = outContent.toString()
        output.contains("[>                             ] 0% (0/100)")
        output.contains("\r")
    }

    def "should print progress bar at 50%"() {
        when: "printing progress at middle"
        ConsoleUtils.printProgress(50, 100)

        then: "shows half-filled progress bar"
        def output = outContent.toString()
        output.contains("[===============>              ] 50% (50/100)")
    }

    def "should print progress bar at 100%"() {
        when: "printing progress at completion"
        ConsoleUtils.printProgress(100, 100)

        then: "shows complete progress bar with newline"
        def output = outContent.toString()
        output.contains("[==============================] 100% (100/100)")
        output.endsWith("\n")
    }

    def "should handle edge case progress values"() {
        when: "printing progress with edge values"
        ConsoleUtils.printProgress(33, 100)

        then: "calculates percentage correctly"
        def output = outContent.toString()
        output.contains("33% (33/100)")
    }

    def "should handle progress bar for small total"() {
        when: "printing progress with small total"
        ConsoleUtils.printProgress(1, 3)

        then: "shows correct percentage"
        def output = outContent.toString()
        output.contains("33% (1/3)")
    }

    def "should handle progress bar for large numbers"() {
        when: "printing progress with large numbers"
        ConsoleUtils.printProgress(12345, 100000)

        then: "shows correct values"
        def output = outContent.toString()
        output.contains("12% (12345/100000)")
    }
}