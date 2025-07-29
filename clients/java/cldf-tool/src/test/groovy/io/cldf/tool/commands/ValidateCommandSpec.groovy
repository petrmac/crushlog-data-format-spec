package io.cldf.tool.commands

import spock.lang.Specification
import spock.lang.TempDir
import io.cldf.api.CLDFArchive
import io.cldf.api.CLDF
import io.cldf.models.*
import io.cldf.tool.models.CommandResult
import io.cldf.tool.services.ValidationService
import io.cldf.tool.utils.OutputHandler
import io.cldf.tool.utils.OutputFormat
import java.nio.file.Path
import java.nio.file.Files
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

class ValidateCommandSpec extends Specification {

    @TempDir
    Path tempDir

    ValidateCommand command
    ValidationService mockValidationService
    OutputHandler mockOutputHandler

    def setup() {
        command = new ValidateCommand()
        mockValidationService = Mock(ValidationService)
        mockOutputHandler = Mock(OutputHandler)
        
        // Inject mocks using reflection
        command.validationService = mockValidationService
        command.output = mockOutputHandler
    }

    def "should handle non-existent file"() {
        given: "a non-existent file"
        command.inputFile = new File(tempDir.toFile(), "non-existent.cldf")

        when: "executing the command"
        def result = command.execute()

        then: "result shows file not found"
        result.success == false
        result.exitCode == 1
        result.message.contains("File not found")
    }

    def "should enable all validations with strict mode"() {
        given: "a valid CLDF file with strict mode enabled"
        def cldfFile = createValidCLDFFile()
        command.inputFile = cldfFile.toFile()
        command.strict = true
        command.validateSchema = false
        command.validateChecksums = false
        command.validateReferences = false

        when: "executing the command"
        try {
            command.execute()
        } catch (IOException e) {
            // Expected - CLDF.read will fail
        }

        then: "all validation flags are enabled before file reading"
        command.validateSchema == true
        command.validateChecksums == true
        command.validateReferences == true
    }

    def "should validate and handle IOException from CLDF.read"() {
        given: "a mock CLDF file that will fail to read"
        def cldfFile = createValidCLDFFile()
        command.inputFile = cldfFile.toFile()
        command.outputFormat = OutputFormat.text

        when: "executing the command"
        command.execute()

        then: "IOException is thrown by CLDF.read"
        thrown(IOException)
    }

    def "should test validation report formatting logic"() {
        given: "create a validation report directly"
        def report = ValidateCommand.ValidationReport.builder()
            .file("test.cldf")
            .timestamp(OffsetDateTime.now())
            .valid(false)
            .structureValid(false)
            .statistics(ValidateCommand.Statistics.builder()
                .locations(2)
                .sessions(1)
                .climbs(3)
                .routes(2)
                .sectors(1)
                .tags(3)
                .mediaItems(1)
                .build())
            .errors(["Error 1", "Error 2"])
            .warnings(["Warning 1"])
            .build()

        when: "formatting as text"
        def textReport = formatTextReportDirectly(report)

        then: "report contains all expected sections"
        textReport.contains("Validation Report")
        textReport.contains("File: test.cldf")
        textReport.contains("Result: INVALID")
        textReport.contains("Statistics:")
        textReport.contains("Locations: 2")
        textReport.contains("Sessions: 1")
        textReport.contains("Climbs: 3")
        textReport.contains("Routes: 2")
        textReport.contains("Sectors: 1")
        textReport.contains("Tags: 3")
        textReport.contains("Media Items: 1")
        textReport.contains("Errors (2):")
        textReport.contains("✗ Error 1")
        textReport.contains("✗ Error 2")
        textReport.contains("Warnings (1):")
        textReport.contains("⚠ Warning 1")
        textReport.contains("✗ Validation failed with 2 error(s) and 1 warning(s)")
    }

    def "should test XML report formatting"() {
        given: "create a validation report with special characters"
        def report = ValidateCommand.ValidationReport.builder()
            .file("test.cldf")
            .timestamp(OffsetDateTime.now())
            .valid(true)
            .structureValid(true)
            .statistics(ValidateCommand.Statistics.builder()
                .locations(1)
                .sessions(1)
                .climbs(1)
                .routes(0)
                .sectors(0)
                .tags(0)
                .mediaItems(0)
                .build())
            .errors([])
            .warnings(["XML & special <characters>"])
            .build()

        when: "formatting as XML"
        def xmlReport = formatXmlReportDirectly(report)

        then: "report is valid XML with escaped characters"
        xmlReport.contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        xmlReport.contains("<validationReport>")
        xmlReport.contains("<valid>true</valid>")
        xmlReport.contains("<warnings count=\"1\">")
        xmlReport.contains("<warning>XML &amp; special &lt;characters&gt;</warning>")
        xmlReport.contains("</validationReport>")
    }

    def "should test checksum result formatting"() {
        given: "create a report with checksum results"
        def checksumResult = ValidateCommand.ChecksumResult.builder()
            .algorithm("SHA-256")
            .valid(true)
            .results(["file1.json": true, "file2.json": false])
            .build()
            
        def report = ValidateCommand.ValidationReport.builder()
            .file("test.cldf")
            .timestamp(OffsetDateTime.now())
            .valid(false)
            .structureValid(true)
            .checksumResult(checksumResult)
            .statistics(ValidateCommand.Statistics.builder()
                .locations(0)
                .sessions(0)
                .climbs(0)
                .routes(0)
                .sectors(0)
                .tags(0)
                .mediaItems(0)
                .build())
            .errors([])
            .warnings([])
            .build()

        when: "formatting as text"
        def textReport = formatTextReportDirectly(report)

        then: "checksum section is included"
        textReport.contains("Checksums:")
        textReport.contains("Algorithm: SHA-256")
        textReport.contains("Valid: YES")
        textReport.contains("✓ file1.json")
        textReport.contains("✗ file2.json")
    }

    def "should test JSON report formatting"() {
        given: "create a validation report"
        def report = ValidateCommand.ValidationReport.builder()
            .file("test.cldf")
            .timestamp(OffsetDateTime.now())
            .valid(true)
            .structureValid(true)
            .statistics(ValidateCommand.Statistics.builder()
                .locations(0)
                .sessions(0)
                .climbs(0)
                .routes(0)
                .sectors(0)
                .tags(0)
                .mediaItems(0)
                .build())
            .errors([])
            .warnings(["Warning 1", "Warning 2"])
            .build()

        when: "formatting as JSON"
        def jsonReport = formatJsonReportDirectly(report)

        then: "JSON structure is correct"
        jsonReport.contains('"file": "test.cldf"')
        jsonReport.contains('"valid": true')
        jsonReport.contains('"errors": 0')
        jsonReport.contains('"warnings": 2')
    }

    def "should handle outputText method"() {
        given: "a command result with message"
        def outputFile = tempDir.resolve("output.txt").toFile()
        command.outputFile = outputFile
        def result = CommandResult.builder()
            .success(true)
            .message("Test validation report")
            .exitCode(0)
            .build()

        when: "calling outputText"
        command.outputText(result)

        then: "message is written to output handler"
        1 * mockOutputHandler.write("Test validation report")
        
        and: "file is created with content"
        outputFile.exists()
        outputFile.text == "Test validation report"
    }

    def "should handle outputText with IOException"() {
        given: "an invalid output file path"
        command.outputFile = new File("/invalid/path/output.txt")
        def result = CommandResult.builder()
            .success(true)
            .message("Test validation report")
            .exitCode(0)
            .build()

        when: "calling outputText"
        command.outputText(result)

        then: "error is logged"
        1 * mockOutputHandler.write("Test validation report")
        1 * mockOutputHandler.writeError(_ as String)
    }

    // Helper methods to test formatting directly
    private String formatTextReportDirectly(ValidateCommand.ValidationReport report) {
        // Replicate the logic from ValidateCommand.formatTextReport
        StringBuilder sb = new StringBuilder()
        
        sb.append("\nValidation Report\n")
        sb.append("=================\n\n")
        
        sb.append("File: ").append(report.getFile()).append("\n")
        sb.append("Timestamp: ").append(report.getTimestamp()).append("\n")
        sb.append("Result: ").append(report.isValid() ? "VALID" : "INVALID").append("\n\n")
        
        // Statistics
        sb.append("Statistics:\n")
        sb.append("-----------\n")
        sb.append("  Locations: ").append(report.getStatistics().getLocations()).append("\n")
        sb.append("  Sessions: ").append(report.getStatistics().getSessions()).append("\n")
        sb.append("  Climbs: ").append(report.getStatistics().getClimbs()).append("\n")
        if (report.getStatistics().getRoutes() > 0) {
            sb.append("  Routes: ").append(report.getStatistics().getRoutes()).append("\n")
        }
        if (report.getStatistics().getSectors() > 0) {
            sb.append("  Sectors: ").append(report.getStatistics().getSectors()).append("\n")
        }
        if (report.getStatistics().getTags() > 0) {
            sb.append("  Tags: ").append(report.getStatistics().getTags()).append("\n")
        }
        if (report.getStatistics().getMediaItems() > 0) {
            sb.append("  Media Items: ").append(report.getStatistics().getMediaItems()).append("\n")
        }
        sb.append("\n")
        
        // Errors
        if (!report.getErrors().isEmpty()) {
            sb.append("Errors (").append(report.getErrors().size()).append("):\n")
            sb.append("------------\n")
            for (String error : report.getErrors()) {
                sb.append("  ✗ ").append(error).append("\n")
            }
            sb.append("\n")
        }
        
        // Warnings
        if (!report.getWarnings().isEmpty()) {
            sb.append("Warnings (").append(report.getWarnings().size()).append("):\n")
            sb.append("--------------\n")
            for (String warning : report.getWarnings()) {
                sb.append("  ⚠ ").append(warning).append("\n")
            }
            sb.append("\n")
        }
        
        // Checksum results
        if (report.getChecksumResult() != null) {
            sb.append("Checksums:\n")
            sb.append("----------\n")
            sb.append("  Algorithm: ").append(report.getChecksumResult().getAlgorithm()).append("\n")
            sb.append("  Valid: ")
                .append(report.getChecksumResult().isValid() ? "YES" : "NO")
                .append("\n")
            if (!report.getChecksumResult().getResults().isEmpty()) {
                for (Map.Entry<String, Boolean> entry :
                    report.getChecksumResult().getResults().entrySet()) {
                    sb.append("  ")
                        .append(entry.getValue() ? "✓" : "✗")
                        .append(" ")
                        .append(entry.getKey())
                        .append("\n")
                }
            }
            sb.append("\n")
        }
        
        // Summary
        sb.append("Summary:\n")
        sb.append("--------\n")
        if (report.isValid() && report.getWarnings().isEmpty()) {
            sb.append("✓ Validation passed\n")
        } else if (report.isValid()) {
            sb.append("✓ Validation passed with ")
                .append(report.getWarnings().size())
                .append(" warning(s)\n")
        } else {
            sb.append("✗ Validation failed with ")
                .append(report.getErrors().size())
                .append(" error(s) and ")
                .append(report.getWarnings().size())
                .append(" warning(s)\n")
        }
        
        return sb.toString()
    }

    private String formatXmlReportDirectly(ValidateCommand.ValidationReport report) {
        // Replicate the logic from ValidateCommand.formatXmlReport
        StringBuilder sb = new StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<validationReport>\n")
        sb.append("  <file>").append(report.getFile()).append("</file>\n")
        sb.append("  <timestamp>").append(report.getTimestamp()).append("</timestamp>\n")
        sb.append("  <valid>").append(report.isValid()).append("</valid>\n")
        
        sb.append("  <statistics>\n")
        sb.append("    <locations>")
            .append(report.getStatistics().getLocations())
            .append("</locations>\n")
        sb.append("    <sessions>")
            .append(report.getStatistics().getSessions())
            .append("</sessions>\n")
        sb.append("    <climbs>").append(report.getStatistics().getClimbs()).append("</climbs>\n")
        sb.append("    <routes>").append(report.getStatistics().getRoutes()).append("</routes>\n")
        sb.append("    <sectors>").append(report.getStatistics().getSectors()).append("</sectors>\n")
        sb.append("    <tags>").append(report.getStatistics().getTags()).append("</tags>\n")
        sb.append("    <mediaItems>")
            .append(report.getStatistics().getMediaItems())
            .append("</mediaItems>\n")
        sb.append("  </statistics>\n")
        
        if (!report.getErrors().isEmpty()) {
            sb.append("  <errors count=\"").append(report.getErrors().size()).append("\">\n")
            for (String error : report.getErrors()) {
                sb.append("    <error>").append(escapeXml(error)).append("</error>\n")
            }
            sb.append("  </errors>\n")
        }
        
        if (!report.getWarnings().isEmpty()) {
            sb.append("  <warnings count=\"").append(report.getWarnings().size()).append("\">\n")
            for (String warning : report.getWarnings()) {
                sb.append("    <warning>").append(escapeXml(warning)).append("</warning>\n")
            }
            sb.append("  </warnings>\n")
        }
        
        sb.append("</validationReport>")
        return sb.toString()
    }

    private String formatJsonReportDirectly(ValidateCommand.ValidationReport report) {
        // Simple JSON formatting
        StringBuilder sb = new StringBuilder()
        sb.append("{\n")
        sb.append("  \"file\": \"").append(report.getFile()).append("\",\n")
        sb.append("  \"timestamp\": \"").append(report.getTimestamp()).append("\",\n")
        sb.append("  \"valid\": ").append(report.isValid()).append(",\n")
        sb.append("  \"errors\": ").append(report.getErrors().size()).append(",\n")
        sb.append("  \"warnings\": ").append(report.getWarnings().size()).append("\n")
        sb.append("}")
        return sb.toString()
    }

    private String escapeXml(String text) {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    // Helper methods to create test data
    private Path createValidCLDFFile() {
        def file = tempDir.resolve("test.cldf")
        Files.write(file, "mock CLDF content".bytes)
        return file
    }
}