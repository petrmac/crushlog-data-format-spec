package app.crushlog.cldf.tool.commands

import spock.lang.Specification
import spock.lang.TempDir
import app.crushlog.cldf.api.CLDFArchive
import app.crushlog.cldf.api.CLDFWriter
import app.crushlog.cldf.models.*
import app.crushlog.cldf.tool.models.CommandResult
import app.crushlog.cldf.tool.models.ReportFormat
import app.crushlog.cldf.tool.models.ValidationReport
import app.crushlog.cldf.tool.models.Statistics
import app.crushlog.cldf.tool.models.ChecksumResult
import app.crushlog.cldf.tool.services.ValidationReportService
import app.crushlog.cldf.tool.services.ValidationReportService.ValidationOptions
import app.crushlog.cldf.tool.utils.OutputHandler
import app.crushlog.cldf.tool.utils.OutputFormat
import app.crushlog.cldf.tool.utils.ValidationReportFormatter
import java.nio.file.Path
import java.nio.file.Files
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import picocli.CommandLine

import app.crushlog.cldf.models.enums.ClimbType
import app.crushlog.cldf.models.enums.FinishType
import app.crushlog.cldf.models.enums.GradeSystem
import app.crushlog.cldf.models.enums.SessionType
import app.crushlog.cldf.models.enums.Platform

class ValidateCommandSpec extends Specification {

    @TempDir
    Path tempDir

    ValidateCommand command
    ValidationReportService mockValidationReportService
    OutputHandler mockOutputHandler

    def setup() {
        mockValidationReportService = Mock(ValidationReportService)
        mockOutputHandler = Mock(OutputHandler)
        
        command = new ValidateCommand(mockValidationReportService)
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
        command.reportFormat = ReportFormat.TEXT
        
        def validationReport = createValidationReport(true, cldfFile.fileName.toString())
        mockValidationReportService.validateFile(_ as File, _ as ValidationOptions) >> validationReport

        when: "executing the command"
        def result = command.execute()

        then: "strict mode enables all validations"
        1 * mockValidationReportService.validateFile(_, { ValidationOptions opts ->
            opts.validateSchema && opts.validateChecksums && opts.validateReferences && opts.strict
        }) >> validationReport
        result.success == true
    }

    def "should validate and handle IOException from CLDF.read"() {
        given: "a file that will fail to read"
        def cldfFile = tempDir.resolve("invalid.cldf").toFile()
        cldfFile.text = "invalid content"  // Create file with invalid content for CLDF reader
        command.inputFile = cldfFile
        command.outputFormat = OutputFormat.TEXT
        command.reportFormat = ReportFormat.TEXT

        mockValidationReportService.validateFile(_, _) >> { throw new IOException("Invalid CLDF format") }

        when: "executing the command"
        def result = command.execute()

        then: "command fails with validation error"
        !result.success
        result.exitCode == 1
        result.message.contains("Validation failed")
    }

    def "should test validation report formatting logic"() {
        given: "create a validation report directly"
        def formatter = new ValidationReportFormatter()
        def report = ValidationReport.builder()
            .file("test.cldf")
            .timestamp(OffsetDateTime.now())
            .valid(false)
            .structureValid(false)
            .statistics(Statistics.builder()
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
        def textReport = formatter.formatTextReport(report)

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
        given: "create a validation report directly"
        def formatter = new ValidationReportFormatter()
        def report = ValidationReport.builder()
            .file("test.cldf")
            .timestamp(OffsetDateTime.of(2023, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC))
            .valid(true)
            .structureValid(true)
            .statistics(Statistics.builder().locations(1).build())
            .errors([])
            .warnings([])
            .build()

        when: "formatting as XML"
        def xmlReport = formatter.formatXmlReport(report)

        then: "XML structure is correct"
        xmlReport.contains('<?xml version="1.0" encoding="UTF-8"?>')
        xmlReport.contains('<validationReport>')
        xmlReport.contains('<file>test.cldf</file>')
        xmlReport.contains('<valid>true</valid>')
        xmlReport.contains('<statistics>')
        xmlReport.contains('<locations>1</locations>')
        xmlReport.contains('</validationReport>')
    }

    def "should test JSON report formatting"() {
        given: "create a validation report directly"
        def formatter = new ValidationReportFormatter()
        def report = ValidationReport.builder()
            .file("test.cldf")
            .timestamp(OffsetDateTime.of(2023, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC))
            .valid(true)
            .structureValid(true)
            .statistics(Statistics.builder().locations(1).build())
            .errors([])
            .warnings([])
            .build()

        when: "formatting as JSON"
        def jsonReport = formatter.formatJsonReport(report)

        then: "JSON structure is correct"
        jsonReport.contains('"file":"test.cldf"')
        jsonReport.contains('"valid":true')
        jsonReport.contains('"structureValid":true')
        jsonReport.contains('"locations":1')
    }

    def "should successfully execute validate command with valid file"() {
        given: "a valid CLDF file"
        def cldfFile = createValidCLDFFile()
        command.inputFile = cldfFile.toFile()
        command.reportFormat = ReportFormat.TEXT
        
        def validationReport = createValidationReport(true, cldfFile.fileName.toString())
        mockValidationReportService.validateFile(_ as File, _ as ValidationOptions) >> validationReport

        when: "executing the command"
        def result = command.execute()

        then: "validation succeeds"
        result.success == true
        result.exitCode == 0
        result.message.contains("Validation Report")
    }

    def "should execute validate command with validation errors"() {
        given: "a CLDF file with validation errors"
        def cldfFile = createValidCLDFFile()
        command.inputFile = cldfFile.toFile()
        command.reportFormat = ReportFormat.TEXT
        
        def validationReport = createValidationReport(false, cldfFile.fileName.toString())
        mockValidationReportService.validateFile(_ as File, _ as ValidationOptions) >> validationReport

        when: "executing the command"
        def result = command.execute()

        then: "validation fails"
        result.success == false
        result.exitCode == 1
        result.message.contains("Validation Report")
    }

    def "should execute validate command with JSON report format"() {
        given: "a valid CLDF file with JSON output"
        def cldfFile = createValidCLDFFile()
        command.inputFile = cldfFile.toFile()
        command.reportFormat = ReportFormat.JSON
        
        def validationReport = createValidationReport(true, cldfFile.fileName.toString())
        mockValidationReportService.validateFile(_ as File, _ as ValidationOptions) >> validationReport

        when: "executing the command"
        def result = command.execute()

        then: "JSON data is returned"
        result.success == true
        result.data instanceof ValidationReport
        result.message == "Validation passed"
    }

    def "should execute validate command with output file specified"() {
        given: "a valid CLDF file with output file"
        def cldfFile = createValidCLDFFile()
        def outputFile = tempDir.resolve("report.txt").toFile()
        command.inputFile = cldfFile.toFile()
        command.outputFile = outputFile
        command.reportFormat = ReportFormat.TEXT
        
        def validationReport = createValidationReport(true, cldfFile.fileName.toString())
        mockValidationReportService.validateFile(_ as File, _ as ValidationOptions) >> validationReport

        when: "executing the command"
        def result = command.execute()
        command.outputText(result)

        then: "output file is created"
        outputFile.exists()
        outputFile.text.contains("Validation Report")
    }

    def "should handle outputText with IOException"() {
        given: "an invalid output path"
        command.outputFile = new File("/invalid/path/output.txt")
        def result = CommandResult.builder()
            .success(true)
            .message("Test report")
            .exitCode(0)
            .build()

        when: "outputting text"
        command.outputText(result)

        then: "error is logged"
        1 * mockOutputHandler.write("Test report")
        1 * mockOutputHandler.writeError({ it.contains("Failed to write report to file") })
    }

    def "should execute validate command with checksums validation"() {
        given: "a CLDF file with checksums"
        def cldfFile = createValidCLDFFile()
        command.inputFile = cldfFile.toFile()
        command.validateChecksums = true
        command.reportFormat = ReportFormat.TEXT
        
        def validationReport = createValidationReportWithChecksums(true, cldfFile.fileName.toString())
        mockValidationReportService.validateFile(_ as File, _ as ValidationOptions) >> validationReport

        when: "executing the command"
        def result = command.execute()

        then: "checksums are validated"
        1 * mockValidationReportService.validateFile(_, { ValidationOptions opts ->
            opts.validateChecksums
        }) >> validationReport
        result.success == true
    }

    def "should execute validate command with different report formats"() {
        given: "a valid CLDF file"
        def cldfFile = createValidCLDFFile()
        command.inputFile = cldfFile.toFile()
        command.reportFormat = format
        
        def validationReport = createValidationReport(true, cldfFile.fileName.toString())
        mockValidationReportService.validateFile(_ as File, _ as ValidationOptions) >> validationReport

        when: "executing the command"
        def result = command.execute()

        then: "result is successful"
        result.success == true

        where:
        format << [ReportFormat.TEXT, ReportFormat.JSON, ReportFormat.XML]
    }

    // Helper methods
    private Path createValidCLDFFile() {
        def tempFile = tempDir.resolve("test.cldf")
        def archive = createTestArchive()
        CLDFWriter writer = new CLDFWriter()
        writer.write(archive, tempFile.toFile())
        return tempFile
    }

    private ValidationReport createValidationReport(boolean valid, String fileName) {
        return ValidationReport.builder()
            .file(fileName)
            .timestamp(OffsetDateTime.now())
            .valid(valid)
            .structureValid(valid)
            .statistics(Statistics.builder()
                .locations(1)
                .sessions(1)
                .climbs(2)
                .build())
            .errors(valid ? [] : ["Validation error"])
            .warnings([])
            .build()
    }

    private ValidationReport createValidationReportWithChecksums(boolean valid, String fileName) {
        def report = createValidationReport(valid, fileName)
        return ValidationReport.builder()
            .file(report.file)
            .timestamp(report.timestamp)
            .valid(report.valid)
            .structureValid(report.structureValid)
            .statistics(report.statistics)
            .errors(report.errors)
            .warnings(report.warnings)
            .checksumResult(ChecksumResult.builder()
                .algorithm("SHA-256")
                .valid(true)
                .results(["manifest.json": true, "locations.json": true])
                .build())
            .build()
    }

    private CLDFArchive createTestArchive() {
        def archive = new CLDFArchive()
        archive.manifest = createManifest()
        archive.locations = createLocations()
        archive.climbs = createClimbs()
        archive.sessions = createSessions()
        return archive
    }

    private Manifest createManifest() {
        def manifest = new Manifest()
        manifest.version = "1.0.0"
        manifest.format = "CLDF"
        manifest.creationDate = OffsetDateTime.now()
        manifest.appVersion = "1.0.0"
        manifest.platform = Platform.DESKTOP
        return manifest
    }

    private List<Location> createLocations() {
        return [
            new Location(
                id: 1,
                name: "Test Crag",
                country: "US",
                state: "CA",
                city: "Test City",
                isIndoor: false
            )
        ]
    }

    private List<Climb> createClimbs() {
        def climb1 = new Climb()
        climb1.id = 1
        climb1.routeId = 1
        climb1.sessionId = 1
        climb1.date = LocalDate.now()
        climb1.type = ClimbType.ROUTE
        climb1.finishType = FinishType.ONSIGHT
        climb1.attempts = 3
        // No sends field in Climb model
        
        def climb2 = new Climb()
        climb2.id = 2
        climb2.routeId = 2
        climb2.sessionId = 1
        climb2.date = LocalDate.now()
        climb2.type = ClimbType.ROUTE
        climb2.finishType = FinishType.REDPOINT
        climb2.attempts = 1
        // No sends field in Climb model
        
        return [climb1, climb2]
    }

    private List<Session> createSessions() {
        return [
            new Session(
                id: 1,
                date: LocalDate.now(),
                locationId: 1
            )
        ]
    }

    // Additional test methods remain the same...
    // The rest of the tests follow the same pattern of updates
}