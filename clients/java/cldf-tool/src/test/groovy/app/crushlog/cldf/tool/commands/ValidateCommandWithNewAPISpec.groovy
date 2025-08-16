package app.crushlog.cldf.tool.commands

import spock.lang.Specification
import app.crushlog.cldf.api.CLDFArchive
import app.crushlog.cldf.tool.models.CommandResult
import app.crushlog.cldf.tool.models.ReportFormat
import app.crushlog.cldf.tool.models.ValidationReport
import app.crushlog.cldf.tool.services.ValidationReportService
import app.crushlog.cldf.tool.services.ValidationService
import app.crushlog.cldf.tool.services.ValidationResult
import app.crushlog.cldf.tool.models.Statistics
import app.crushlog.cldf.models.*
import app.crushlog.cldf.models.enums.*

class ValidateCommandWithNewAPISpec extends Specification {

    def validationReportService = Mock(ValidationReportService)
    def command = new ValidateCommand(validationReportService)

    def setup() {
        // Initialize command properties
        command.outputFormat = app.crushlog.cldf.tool.utils.OutputFormat.TEXT
        command.quiet = false
        command.validateSchema = true
        command.validateChecksums = true
        command.validateReferences = true
        command.reportFormat = ReportFormat.TEXT
        // Initialize output handler
        command.output = new app.crushlog.cldf.tool.utils.OutputHandler(command.outputFormat, command.quiet)
    }

    def "should handle missing input file"() {
        given:
        command.inputFile = new File("non-existent.cldf")

        when:
        def result = command.execute()

        then:
        !result.success
        result.exitCode == 1
        result.message.contains("File not found")
    }

    def "should validate a valid archive successfully"() {
        given:
        def tempFile = File.createTempFile("test", ".cldf")
        tempFile.deleteOnExit()
        createValidArchive(tempFile)
        command.inputFile = tempFile

        and:
        validationReportService.validateFile(_, _) >> ValidationReport.builder()
            .file(tempFile.absolutePath)
            .valid(true)
            .structureValid(true)
            .errors([])
            .warnings([])
            .statistics(new Statistics(0, 0, 0, 0, 0, 0, 0))
            .build()

        when:
        def result = command.execute()

        then:
        result.success
        result.exitCode == 0
        result.message.contains("VALID")
    }

    def "should report validation errors"() {
        given:
        def tempFile = File.createTempFile("test", ".cldf")
        tempFile.deleteOnExit()
        createValidArchive(tempFile)
        command.inputFile = tempFile

        and:
        validationReportService.validateFile(_, _) >> ValidationReport.builder()
            .file(tempFile.absolutePath)
            .valid(false)
            .structureValid(true)
            .errors(["Missing required field: version", "Invalid date format"])
            .warnings(["Route 'Test Route' appears 2 times on 2024-01-01"])
            .statistics(new Statistics(0, 0, 0, 0, 0, 0, 0))
            .build()

        when:
        def result = command.execute()

        then:
        !result.success
        result.exitCode == 1
        result.message.contains("INVALID")
        result.message.contains("Missing required field: version")
        result.message.contains("Invalid date format")
        result.message.contains("Route 'Test Route' appears 2 times")
    }

    def "should handle warnings without failing validation"() {
        given:
        def tempFile = File.createTempFile("test", ".cldf")
        tempFile.deleteOnExit()
        createValidArchive(tempFile)
        command.inputFile = tempFile

        and:
        validationReportService.validateFile(_, _) >> ValidationReport.builder()
            .file(tempFile.absolutePath)
            .valid(true)
            .structureValid(true)
            .errors([])
            .warnings(["2 climbs have dates in the future"])
            .statistics(new Statistics(0, 0, 0, 0, 0, 0, 0))
            .build()

        when:
        def result = command.execute()

        then:
        result.success
        result.exitCode == 0
        result.message.contains("VALID")
        result.message.contains("2 climbs have dates in the future")
    }

    def "should enable all validations in strict mode"() {
        given:
        def tempFile = File.createTempFile("test", ".cldf")
        tempFile.deleteOnExit()
        createValidArchive(tempFile)
        command.inputFile = tempFile
        command.validateSchema = false
        command.validateChecksums = false
        command.validateReferences = false
        command.strict = true

        and:
        validationReportService.validateFile(_, _) >> ValidationReport.builder()
            .file(tempFile.absolutePath)
            .valid(true)
            .structureValid(true)
            .errors([])
            .warnings([])
            .statistics(new Statistics(0, 0, 0, 0, 0, 0, 0))
            .build()

        when:
        command.execute()

        then:
        command.validateSchema
        command.validateChecksums
        command.validateReferences
    }

    def "should format report as JSON when requested"() {
        given:
        def tempFile = File.createTempFile("test", ".cldf")
        tempFile.deleteOnExit()
        createValidArchive(tempFile)
        command.inputFile = tempFile
        command.reportFormat = ReportFormat.JSON

        and:
        validationReportService.validateFile(_, _) >> ValidationReport.builder()
            .file(tempFile.absolutePath)
            .valid(true)
            .structureValid(true)
            .errors([])
            .warnings([])
            .statistics(new Statistics(0, 0, 0, 0, 0, 0, 0))
            .build()

        when:
        def result = command.execute()

        then:
        result.success
        result.data != null
        result.data instanceof ValidationReport
    }

    def "should format report as text by default"() {
        given:
        def tempFile = File.createTempFile("test", ".cldf")
        tempFile.deleteOnExit()
        createValidArchive(tempFile)
        command.inputFile = tempFile
        command.reportFormat = ReportFormat.TEXT

        and:
        validationReportService.validateFile(_, _) >> ValidationReport.builder()
            .file(tempFile.absolutePath)
            .valid(true)
            .structureValid(true)
            .errors([])
            .warnings([])
            .statistics(new Statistics(0, 0, 0, 0, 0, 0, 0))
            .build()

        when:
        def result = command.execute()

        then:
        result.success
        result.message.contains("Validation Report")
        result.message.contains("Statistics:")
    }

    def "should format report as XML when requested"() {
        given:
        def tempFile = File.createTempFile("test", ".cldf")
        tempFile.deleteOnExit()
        createValidArchive(tempFile)
        command.inputFile = tempFile
        command.reportFormat = ReportFormat.XML

        and:
        validationReportService.validateFile(_, _) >> ValidationReport.builder()
            .file(tempFile.absolutePath)
            .valid(true)
            .structureValid(true)
            .errors([])
            .warnings([])
            .statistics(new Statistics(0, 0, 0, 0, 0, 0, 0))
            .build()

        when:
        def result = command.execute()

        then:
        result.success
        result.message.contains("<?xml")
        result.message.contains("<validationReport>")
        result.message.contains("</validationReport>")
    }

    def "should include statistics in report"() {
        given:
        def tempFile = File.createTempFile("test", ".cldf")
        tempFile.deleteOnExit()
        createArchiveWithStats(tempFile)
        command.inputFile = tempFile

        and:
        validationReportService.validateFile(_, _) >> ValidationReport.builder()
            .file(tempFile.absolutePath)
            .valid(true)
            .structureValid(true)
            .errors([])
            .warnings([])
            .statistics(new Statistics(2, 3, 5, 4, 0, 0, 0))
            .build()

        when:
        def result = command.execute()

        then:
        result.success
        result.message.contains("Locations: 2")
        result.message.contains("Sessions: 3")
        result.message.contains("Climbs: 5")
        result.message.contains("Routes: 4")
    }

    def "should handle schema validation errors with details"() {
        given:
        def tempFile = File.createTempFile("test", ".cldf")
        tempFile.deleteOnExit()
        createValidArchive(tempFile)
        command.inputFile = tempFile

        and:
        validationReportService.validateFile(_, _) >> ValidationReport.builder()
            .file(tempFile.absolutePath)
            .valid(false)
            .structureValid(true)
            .errors([
                'manifest.json$.version: missing required property',
                'climbs.json$[0].grade: invalid format'
            ])
            .warnings([])
            .statistics(new Statistics(0, 0, 0, 0, 0, 0, 0))
            .build()

        when:
        def result = command.execute()

        then:
        !result.success
        result.message.contains('manifest.json$.version')
        result.message.contains('climbs.json$[0].grade')
    }

    def "should test ValidationService integration with new API"() {
        given: "a test file with basic content"
        def tempFile = File.createTempFile("test", ".cldf")
        tempFile.deleteOnExit()
        createValidArchive(tempFile)
        command.inputFile = tempFile

        and: "mocked validation service returns schema errors from new API"
        validationReportService.validateFile(_, _) >> ValidationReport.builder()
            .file(tempFile.absolutePath)
            .valid(false)
            .structureValid(true)
            .errors([
                'manifest.json$: missing required property \'version\'',
                'manifest.json$.creationDate: invalid date format',
                'locations.json$[0]: missing required property \'id\'',
                'climbs.json$[0].grade: does not match pattern'
            ])
            .warnings([])
            .statistics(new Statistics(0, 0, 0, 0, 0, 0, 0))
            .build()

        when:
        def result = command.execute()

        then:
        !result.success
        result.exitCode == 1
        result.message.contains('manifest.json$: missing required property \'version\'')
        result.message.contains('manifest.json$.creationDate: invalid date format')
        result.message.contains('locations.json$[0]: missing required property \'id\'')
        result.message.contains('climbs.json$[0].grade: does not match pattern')
    }

    // Helper methods

    private void createValidArchive(File file) {
        def archive = CLDFArchive.builder()
            .manifest(Manifest.builder()
                .version("1.0.0")
                .format("CLDF")
                .creationDate(java.time.OffsetDateTime.now())
                .appVersion("1.0.0")
                .platform(Platform.ANDROID)
                .build())
            .locations([
                Location.builder()
                    .id(1)
                    .name("Test Location")
                    .isIndoor(false)
                    .coordinates(Location.Coordinates.builder()
                        .latitude(40.7128d)
                        .longitude(-74.0060d)
                        .build())
                    .build()
            ])
            .sessions([
                Session.builder()
                    .id(1)
                    .location("Test Location")
                    .locationId(1)
                    .date(java.time.LocalDate.now())
                    .build()
            ])
            .climbs([
                Climb.builder()
                    .id(1)
                    .sessionId(1)
                    .routeId(1)
                    .date(java.time.LocalDate.now())
                    .routeName("Test Route")
                    .attempts(1)
                    .type(ClimbType.BOULDER)
                    .finishType(FinishType.TOP)
                    .build()
            ])
            .build()

        new app.crushlog.cldf.api.CLDFWriter().write(archive, file)
    }

    private void createArchiveWithStats(File file) {
        def archive = CLDFArchive.builder()
            .manifest(Manifest.builder()
                .version("1.0.0")
                .format("CLDF")
                .creationDate(java.time.OffsetDateTime.now())
                .appVersion("1.0.0")
                .platform(Platform.IOS)
                .build())
            .locations([
                Location.builder().id(1).name("Location 1").isIndoor(false)
                    .coordinates(Location.Coordinates.builder().latitude(40.0d).longitude(-74.0d).build()).build(),
                Location.builder().id(2).name("Location 2").isIndoor(false)
                    .coordinates(Location.Coordinates.builder().latitude(41.0d).longitude(-73.0d).build()).build()
            ])
            .sessions([
                Session.builder().id(1).location("Location 1").locationId(1).date(java.time.LocalDate.now()).build(),
                Session.builder().id(2).location("Location 1").locationId(1).date(java.time.LocalDate.now().minusDays(1)).build(),
                Session.builder().id(3).location("Location 2").locationId(2).date(java.time.LocalDate.now().minusDays(2)).build()
            ])
            .climbs([
                Climb.builder().id(1).sessionId(1).routeId(1).date(java.time.LocalDate.now())
                    .routeName("Route 1").attempts(1).type(ClimbType.BOULDER).finishType(FinishType.TOP).build(),
                Climb.builder().id(2).sessionId(1).routeId(2).date(java.time.LocalDate.now())
                    .routeName("Route 2").attempts(2).type(ClimbType.BOULDER).finishType(FinishType.TOP).build(),
                Climb.builder().id(3).sessionId(2).routeId(3).date(java.time.LocalDate.now().minusDays(1))
                    .routeName("Route 3").attempts(1).type(ClimbType.ROUTE).finishType(FinishType.FLASH).build(),
                Climb.builder().id(4).sessionId(2).routeId(4).date(java.time.LocalDate.now().minusDays(1))
                    .routeName("Route 4").attempts(3).type(ClimbType.ROUTE).finishType(FinishType.REDPOINT).build(),
                Climb.builder().id(5).sessionId(3).routeId(1).date(java.time.LocalDate.now().minusDays(2))
                    .routeName("Route 1").attempts(1).type(ClimbType.BOULDER).finishType(FinishType.TOP).build()
            ])
            .routes([
                Route.builder().id(1).name("Route 1").locationId(1).routeType(RouteType.BOULDER)
                    .grades(Route.Grades.builder().vScale("V5").build()).build(),
                Route.builder().id(2).name("Route 2").locationId(1).routeType(RouteType.BOULDER)
                    .grades(Route.Grades.builder().vScale("V6").build()).build(),
                Route.builder().id(3).name("Route 3").locationId(1).routeType(RouteType.ROUTE)
                    .grades(Route.Grades.builder().yds("5.10a").build()).build(),
                Route.builder().id(4).name("Route 4").locationId(2).routeType(RouteType.ROUTE)
                    .grades(Route.Grades.builder().yds("5.11b").build()).build()
            ])
            .build()

        new app.crushlog.cldf.api.CLDFWriter().write(archive, file)
    }
    
    private void createArchiveWithSchemaErrors(File file) {
        // Create an archive that will have schema validation errors
        def archive = CLDFArchive.builder()
            .manifest(Manifest.builder()
                // Missing required fields: version, format, creationDate, appVersion, platform
                .build())
            .locations([
                Location.builder()
                    // Missing required fields: id, name, latitude, longitude
                    .build()
            ])
            .sessions([
                Session.builder()
                    .id(1)
                    // Missing required fields
                    .build()
            ])
            .climbs([
                Climb.builder()
                    .id(1)
                    .sessionId(1)
                    // Missing required fields and invalid data
                    .build()
            ])
            .build()

        // Write without validation to create a file with errors
        new app.crushlog.cldf.api.CLDFWriter(false, false).write(archive, file)
    }
}