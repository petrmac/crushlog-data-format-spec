package io.cldf.tool.commands

import spock.lang.Specification
import spock.lang.TempDir
import io.cldf.api.CLDFArchive
import io.cldf.api.CLDFWriter
import io.cldf.models.*
import io.cldf.tool.models.CommandResult
import io.cldf.tool.services.ValidationService
import io.cldf.tool.services.ValidationResult
import io.cldf.tool.utils.OutputHandler
import io.cldf.tool.utils.OutputFormat
import io.cldf.tool.utils.ValidationReportFormatter
import java.nio.file.Path
import java.nio.file.Files
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import picocli.CommandLine

import io.cldf.models.enums.ClimbType
import io.cldf.models.enums.FinishType
import io.cldf.models.enums.GradeSystem
import io.cldf.models.enums.SessionType
import io.cldf.models.enums.Platform

class ValidateCommandSpec extends Specification {

    @TempDir
    Path tempDir

    ValidateCommand command
    ValidationService mockValidationService
    OutputHandler mockOutputHandler

    def setup() {
        mockValidationService = Mock(ValidationService)
        mockOutputHandler = Mock(OutputHandler)
        
        command = new ValidateCommand(mockValidationService)
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
        command.reportFormat = ValidateCommand.ReportFormat.text
        
        def validationResult = ValidationResult.builder()
            .valid(true)
            .errors([])
            .warnings([])
            .build()
        mockValidationService.validate(_ as CLDFArchive) >> validationResult

        when: "executing the command"
        def result = command.execute()

        then: "all validation flags are enabled before file reading"
        command.validateSchema == true
        command.validateChecksums == true
        command.validateReferences == true
        result.success == true
    }

    def "should validate and handle IOException from CLDF.read"() {
        given: "a file that will fail to read"
        def cldfFile = tempDir.resolve("invalid.cldf").toFile()
        cldfFile.text = "invalid content"  // Create file with invalid content for CLDF reader
        command.inputFile = cldfFile
        command.outputFormat = OutputFormat.text
        command.reportFormat = ValidateCommand.ReportFormat.text

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
        given: "create a validation report with special characters"
        def formatter = new ValidationReportFormatter()
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
        def xmlReport = formatter.formatXmlReport(report)

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
        def formatter = new ValidationReportFormatter()
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
        def textReport = formatter.formatTextReport(report)

        then: "checksum section is included"
        textReport.contains("Checksums:")
        textReport.contains("Algorithm: SHA-256")
        textReport.contains("Valid: YES")
        textReport.contains("✓ file1.json")
        textReport.contains("✗ file2.json")
    }

    def "should test JSON report formatting"() {
        given: "create a validation report"
        def formatter = new ValidationReportFormatter()
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
        def jsonReport = formatter.formatJsonReport(report)

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


    // Helper methods to create test data
    private Path createValidCLDFFile() {
        def archive = createTestArchive()
        def file = tempDir.resolve("test.cldf")
        new CLDFWriter(false).write(archive, file.toFile())
        return file
    }
    
    private CLDFArchive createTestArchive(int locationCount = 2, int sessionCount = 1, int climbCount = 3) {
        def manifest = Manifest.builder()
            .version("1.0.0")
            .format("CLDF")
            .creationDate(OffsetDateTime.now())
            .appVersion("1.0.0")
            .platform(Platform.DESKTOP)
            .build()
        
        def locations = (1..locationCount).collect { i ->
            Location.builder()
                .id(i)
                .name("Location $i")
                .isIndoor(i % 2 == 0)
                .country("USA")
                .state("Colorado")
                .build()
        }
        
        def sessions = (1..sessionCount).collect { i ->
            Session.builder()
                .id(i)
                .date(LocalDate.of(2024, 1, Math.min(i, 28)))
                .location("Location ${(i % locationCount) + 1}")
                .locationId((i % locationCount) + 1)
                .isIndoor(i % 2 == 0)
                .sessionType(SessionType.INDOOR_CLIMBING)
                .build()
        }
        
        def climbs = (1..climbCount).collect { i ->
            Climb.builder()
                .id(i)
                .sessionId(1)
                .date(LocalDate.of(2024, 1, 15))
                .routeName("Route $i")
                .type(ClimbType.BOULDER)
                .finishType(FinishType.TOP)
                .attempts(1)
                .grades(Climb.GradeInfo.builder()
                    .system(GradeSystem.V_SCALE)
                    .grade("V${i % 10}")
                    .build())
                .isIndoor(true)
                .rating(4)
                .build()
        }
        
        return CLDFArchive.builder()
            .manifest(manifest)
            .locations(locations)
            .sessions(sessions)
            .climbs(climbs)
            .checksums(Checksums.builder().algorithm("SHA-256").build())
            .build()
    }
    
    def "should test performValidation method with full validation"() {
        given: "a mock archive with full data"
        def archive = Mock(CLDFArchive)
        def checksums = Mock(Checksums)
        checksums.getAlgorithm() >> "SHA-256"
        archive.getChecksums() >> checksums
        archive.getLocations() >> [Mock(Location), Mock(Location)]
        archive.getSessions() >> [Mock(Session)]
        archive.getClimbs() >> [Mock(Climb), Mock(Climb), Mock(Climb)]
        archive.hasRoutes() >> true
        archive.getRoutes() >> [Mock(Route), Mock(Route)]
        archive.hasSectors() >> true
        archive.getSectors() >> [Mock(Sector)]
        archive.hasTags() >> true
        archive.getTags() >> [Mock(Tag), Mock(Tag), Mock(Tag)]
        archive.hasMedia() >> true
        archive.getMediaItems() >> [Mock(MediaMetadataItem)]
        
        def validationResult = ValidationResult.builder()
            .valid(true)
            .errors(["Error 1"])
            .warnings(["Warning 1", "Warning 2"])
            .build()
        mockValidationService.validate(archive) >> validationResult
        
        command.validateChecksums = true
        command.inputFile = new File("test.cldf")
        
        when: "performing validation"
        def report = command.performValidation(archive)
        
        then: "report contains all expected data"
        report.file == "test.cldf"
        report.timestamp != null
        report.structureValid == true
        report.errors == ["Error 1"]
        report.warnings == ["Warning 1", "Warning 2"]
        report.statistics.locations == 2
        report.statistics.sessions == 1
        report.statistics.climbs == 3
        report.statistics.routes == 2
        report.statistics.sectors == 1
        report.statistics.tags == 3
        report.statistics.mediaItems == 1
        report.checksumResult != null
        report.checksumResult.algorithm == "SHA-256"
        report.checksumResult.valid == true
        report.valid == true
    }
    
    def "should test performValidation without checksums"() {
        given: "a mock archive without checksums"
        def archive = Mock(CLDFArchive)
        archive.getChecksums() >> null
        archive.getLocations() >> []
        archive.getSessions() >> null
        archive.getClimbs() >> null
        archive.hasRoutes() >> false
        archive.hasSectors() >> false
        archive.hasTags() >> false
        archive.hasMedia() >> false
        
        def validationResult = ValidationResult.builder()
            .valid(false)
            .errors(["Critical error"])
            .warnings([])
            .build()
        mockValidationService.validate(archive) >> validationResult
        
        command.validateChecksums = true
        command.inputFile = new File("test.cldf")
        
        when: "performing validation"
        def report = command.performValidation(archive)
        
        then: "report reflects failure and no checksums"
        report.structureValid == false
        report.errors == ["Critical error"]
        report.warnings == []
        report.statistics.locations == 0
        report.statistics.sessions == 0
        report.statistics.climbs == 0
        report.checksumResult == null
        report.valid == false
    }
    
    
    def "should test formatReport method for coverage"() {
        given: "a validation report and validate full text formatting"
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
            .warnings([])
            .build()
            
        when: "testing private formatReport method using performValidation approach"
        command.inputFile = new File("test.cldf")
        command.validateChecksums = false
        
        def archive = Mock(CLDFArchive)
        archive.getChecksums() >> null
        archive.getLocations() >> [Mock(Location)]
        archive.getSessions() >> [Mock(Session)]
        archive.getClimbs() >> [Mock(Climb)]
        archive.hasRoutes() >> false
        archive.hasSectors() >> false
        archive.hasTags() >> false
        archive.hasMedia() >> false
        
        def validationResult = ValidationResult.builder()
            .valid(true)
            .errors([])
            .warnings([])
            .build()
        mockValidationService.validate(archive) >> validationResult
        
        def performedReport = command.performValidation(archive)
        
        then: "all report validation is done via performValidation"
        performedReport.file == "test.cldf"
        performedReport.valid == true
        performedReport.errors == []
        performedReport.warnings == []
        performedReport.statistics.locations == 1
        performedReport.statistics.sessions == 1
        performedReport.statistics.climbs == 1
    }
    
    def "should test escapeXml method"() {
        given: "a formatter instance"
        def formatter = new ValidationReportFormatter()
        
        when: "escaping various XML entities"
        def result1 = formatter.escapeXml("Test & <tag> with \"quotes\" and 'apostrophes'")
        def result2 = formatter.escapeXml("Normal text")
        
        then: "XML entities are properly escaped"
        result1 == "Test &amp; &lt;tag&gt; with &quot;quotes&quot; and &apos;apostrophes&apos;"
        result2 == "Normal text"
    }
    
    def "should test validateChecksums method"() {
        given: "an archive with checksums"
        def archive = Mock(CLDFArchive)
        def checksums = Mock(Checksums)
        checksums.getAlgorithm() >> "MD5"
        archive.getChecksums() >> checksums
        
        command.quiet = false
        
        when: "validating checksums"
        def result = command.validateChecksums(archive)
        
        then: "checksum result is created"
        result.algorithm == "MD5"
        result.valid == true
        result.results != null
        result.results.isEmpty()
    }
    
    def "should test validateChecksums in quiet mode"() {
        given: "an archive with checksums in quiet mode"
        def archive = Mock(CLDFArchive)
        def checksums = Mock(Checksums)
        checksums.getAlgorithm() >> "SHA-256"
        archive.getChecksums() >> checksums
        
        command.quiet = true
        
        when: "validating checksums"
        def result = command.validateChecksums(archive)
        
        then: "checksum result is created without console output"
        result.algorithm == "SHA-256"
        result.valid == true
    }
    
    def "should test gatherStatistics with null collections"() {
        given: "an archive with null collections"
        def archive = Mock(CLDFArchive)
        archive.getLocations() >> null
        archive.getSessions() >> null
        archive.getClimbs() >> null
        archive.hasRoutes() >> false
        archive.hasSectors() >> false
        archive.hasTags() >> false
        archive.hasMedia() >> false
        
        when: "gathering statistics"
        def stats = command.gatherStatistics(archive)
        
        then: "all counts are zero"
        stats.locations == 0
        stats.sessions == 0
        stats.climbs == 0
        stats.routes == 0
        stats.sectors == 0
        stats.tags == 0
        stats.mediaItems == 0
    }
    
    def "should test CLI integration"() {
        given: "a command line with validate command"
        def commandLine = new CommandLine(new ValidateCommand())
        
        when: "parsing help"
        def sw = new StringWriter()
        commandLine.setOut(new PrintWriter(sw))
        commandLine.execute("--help")
        def helpText = sw.toString()
        
        then: "help text contains expected options"
        helpText.contains("validate")
        helpText.contains("--schema")
        helpText.contains("--checksums")
        helpText.contains("--references")
        helpText.contains("--strict")
        helpText.contains("--report-format")
        helpText.contains("--output")
    }
    
    def "should successfully execute validate command with valid file"() {
        given: "a valid CLDF file"
        def cldfFile = createValidCLDFFile().toFile()
        command.inputFile = cldfFile
        command.validateSchema = true
        command.validateChecksums = true
        command.validateReferences = true
        command.outputFormat = OutputFormat.text
        command.reportFormat = ValidateCommand.ReportFormat.text
        
        def validationResult = ValidationResult.builder()
            .valid(true)
            .errors([])
            .warnings([])
            .build()
        mockValidationService.validate(_ as CLDFArchive) >> validationResult

        when: "executing the command"
        def result = command.execute()

        then: "validation is performed and result is successful"
        result.success == true
        result.message.contains("Validation Report")
        result.message.contains("Result: VALID")
        
        and: "validation service was called with the loaded archive"
        1 * mockValidationService.validate(_ as CLDFArchive) >> validationResult
    }
    
    def "should execute validate command and handle CLDF read IOException"() {
        given: "an invalid CLDF file"
        def cldfFile = tempDir.resolve("invalid.cldf").toFile()
        cldfFile.text = "invalid content"  // Create file with invalid content
        command.inputFile = cldfFile
        command.reportFormat = ValidateCommand.ReportFormat.text

        when: "executing the command"
        def result = command.execute()

        then: "command fails with validation error"
        !result.success
        result.exitCode == 1
        result.message.contains("Validation failed")
    }
    
    def "should execute validate command with strict mode"() {
        given: "a valid CLDF file with strict mode enabled"
        def cldfFile = createValidCLDFFile().toFile()
        command.inputFile = cldfFile
        command.strict = true
        command.validateSchema = false
        command.validateChecksums = false
        command.validateReferences = false
        command.reportFormat = ValidateCommand.ReportFormat.text
        
        def mockArchive = Mock(CLDFArchive)
        mockArchive.getChecksums() >> null
        mockArchive.getLocations() >> []
        mockArchive.getSessions() >> []
        mockArchive.getClimbs() >> []
        mockArchive.hasRoutes() >> false
        mockArchive.hasSectors() >> false
        mockArchive.hasTags() >> false
        mockArchive.hasMedia() >> false
        
        def validationResult = ValidationResult.builder()
            .valid(true)
            .errors([])
            .warnings([])
            .build()
        mockValidationService.validate(_ as CLDFArchive) >> validationResult

        when: "executing the command"
        def result = command.execute()

        then: "strict mode enables all validation flags"
        command.validateSchema == true
        command.validateChecksums == true
        command.validateReferences == true
        result.success == true
        
        and: "validation service was called"
        1 * mockValidationService.validate(_ as CLDFArchive) >> validationResult
    }
    
    def "should execute validate command with validation errors"() {
        given: "a CLDF file with validation errors"
        def cldfFile = createValidCLDFFile().toFile()
        command.inputFile = cldfFile
        command.validateSchema = true
        command.reportFormat = ValidateCommand.ReportFormat.text
        
        def mockArchive = Mock(CLDFArchive)
        mockArchive.getChecksums() >> null
        mockArchive.getLocations() >> []
        mockArchive.getSessions() >> []
        mockArchive.getClimbs() >> []
        mockArchive.hasRoutes() >> false
        mockArchive.hasSectors() >> false
        mockArchive.hasTags() >> false
        mockArchive.hasMedia() >> false
        
        def validationResult = ValidationResult.builder()
            .valid(false)
            .errors(["Schema validation failed", "Invalid climb data"])
            .warnings(["Deprecated field used"])
            .build()
        mockValidationService.validate(_ as CLDFArchive) >> validationResult

        when: "executing the command"
        def result = command.execute()

        then: "validation errors are reported"
        result.success == false  // Command fails when validation errors occur
        result.exitCode == 1
        result.message.contains("Schema validation failed")
        result.message.contains("Invalid climb data")
        result.message.contains("Deprecated field used")
        
        and: "validation service was called"
        1 * mockValidationService.validate(_ as CLDFArchive) >> validationResult
    }
    
    def "should execute validate command with checksums validation"() {
        given: "a CLDF file with checksums"
        def cldfFile = createValidCLDFFile().toFile()
        command.inputFile = cldfFile
        command.validateChecksums = true
        command.reportFormat = ValidateCommand.ReportFormat.text
        
        def mockArchive = Mock(CLDFArchive)
        def mockChecksums = Mock(Checksums)
        mockChecksums.getAlgorithm() >> "SHA-256"
        mockArchive.getChecksums() >> mockChecksums
        mockArchive.getLocations() >> []
        mockArchive.getSessions() >> []
        mockArchive.getClimbs() >> []
        mockArchive.hasRoutes() >> false
        mockArchive.hasSectors() >> false
        mockArchive.hasTags() >> false
        mockArchive.hasMedia() >> false
        
        def validationResult = ValidationResult.builder()
            .valid(true)
            .errors([])
            .warnings([])
            .build()
        mockValidationService.validate(_ as CLDFArchive) >> validationResult

        when: "executing the command"
        def result = command.execute()

        then: "checksums are validated"
        result.success == true
        result.message.contains("Checksums:")
        result.message.contains("Algorithm: SHA-256")
        result.message.contains("Valid: YES")
        
        and: "validation service was called"
        1 * mockValidationService.validate(_ as CLDFArchive) >> validationResult
    }
    
    def "should execute validate command with different report formats"() {
        given: "a valid CLDF file with XML report format"
        def cldfFile = createValidCLDFFile().toFile()
        command.inputFile = cldfFile
        command.reportFormat = ValidateCommand.ReportFormat.xml
        
        def mockArchive = Mock(CLDFArchive)
        mockArchive.getChecksums() >> null
        mockArchive.getLocations() >> []
        mockArchive.getSessions() >> []
        mockArchive.getClimbs() >> []
        mockArchive.hasRoutes() >> false
        mockArchive.hasSectors() >> false
        mockArchive.hasTags() >> false
        mockArchive.hasMedia() >> false
        
        def validationResult = ValidationResult.builder()
            .valid(true)
            .errors([])
            .warnings([])
            .build()
        mockValidationService.validate(_ as CLDFArchive) >> validationResult

        when: "executing the command"
        def result = command.execute()

        then: "XML format is used"
        result.success == true
        result.message.contains("<?xml")  // XML format output
        
        and: "validation service was called"
        1 * mockValidationService.validate(_ as CLDFArchive) >> validationResult
    }
    
    def "should execute validate command with JSON report format"() {
        given: "a valid CLDF file with JSON report format"
        def cldfFile = createValidCLDFFile().toFile()
        command.inputFile = cldfFile
        command.reportFormat = ValidateCommand.ReportFormat.json
        
        def mockArchive = Mock(CLDFArchive)
        mockArchive.getChecksums() >> null
        mockArchive.getLocations() >> []
        mockArchive.getSessions() >> []
        mockArchive.getClimbs() >> []
        mockArchive.hasRoutes() >> false
        mockArchive.hasSectors() >> false
        mockArchive.hasTags() >> false
        mockArchive.hasMedia() >> false
        
        def validationResult = ValidationResult.builder()
            .valid(true)
            .errors([])
            .warnings([])
            .build()
        mockValidationService.validate(_ as CLDFArchive) >> validationResult

        when: "executing the command"
        def result = command.execute()

        then: "JSON format is used"
        result.success == true
        result.data != null  // JSON format output
        result.message == "Validation passed"
        
        and: "validation service was called"
        1 * mockValidationService.validate(_ as CLDFArchive) >> validationResult
    }
    
    def "should execute validate command with output file specified"() {
        given: "a valid CLDF file with output file"
        def cldfFile = createValidCLDFFile().toFile()
        def outputFile = tempDir.resolve("validation-report.txt").toFile()
        command.inputFile = cldfFile
        command.outputFile = outputFile
        command.reportFormat = ValidateCommand.ReportFormat.text
        
        def mockArchive = Mock(CLDFArchive)
        mockArchive.getChecksums() >> null
        mockArchive.getLocations() >> []
        mockArchive.getSessions() >> []
        mockArchive.getClimbs() >> []
        mockArchive.hasRoutes() >> false
        mockArchive.hasSectors() >> false
        mockArchive.hasTags() >> false
        mockArchive.hasMedia() >> false
        
        def validationResult = ValidationResult.builder()
            .valid(true)
            .errors([])
            .warnings([])
            .build()
        mockValidationService.validate(_ as CLDFArchive) >> validationResult

        when: "executing the command"
        def result = command.execute()
        
        then: "command executes successfully"
        result.success == true
        1 * mockValidationService.validate(_ as CLDFArchive) >> validationResult
        
        when: "outputText is called"
        command.outputText(result)
        
        then: "report is written to output file"
        outputFile.exists()
        outputFile.text.contains("Validation Report")
    }
    
    def "should execute validate command with complex archive data"() {
        given: "a complex CLDF archive"
        // Create archive with specific counts matching our expectations
        def archive = createTestArchive(3, 2, 4)  // 3 locations, 2 sessions, 4 climbs
        def file = tempDir.resolve("complex.cldf")
        new CLDFWriter(false).write(archive, file.toFile())
        
        command.inputFile = file.toFile()
        command.validateSchema = true
        command.validateChecksums = true
        command.reportFormat = ValidateCommand.ReportFormat.text
        
        def validationResult = ValidationResult.builder()
            .valid(true)
            .errors([])
            .warnings(["Minor formatting issue"])
            .build()
        mockValidationService.validate(_ as CLDFArchive) >> validationResult

        when: "executing the command"
        def result = command.execute()

        then: "all data is processed correctly"
        result.success == true
        result.message.contains("Validation Report")
        result.message.contains("Locations: 3")
        result.message.contains("Sessions: 2")
        result.message.contains("Climbs: 4")
        result.message.contains("Algorithm: SHA-256")
        result.message.contains("Minor formatting issue")
        
        and: "validation service was called"
        1 * mockValidationService.validate(_ as CLDFArchive) >> validationResult
    }
}
