package io.cldf.tool.commands

import spock.lang.Specification
import io.cldf.tool.services.ValidationService
import io.cldf.tool.services.ValidationResult
import io.cldf.tool.models.CommandResult

class ValidateCommandSimpleSpec extends Specification {

    def validationService = Mock(ValidationService)
    def command = new ValidateCommand(validationService)

    def setup() {
        // Initialize command properties
        command.outputFormat = io.cldf.tool.utils.OutputFormat.text
        command.quiet = false
        command.validateSchema = true
        command.validateChecksums = false // Disable checksum validation for tests
        command.validateReferences = true
        command.reportFormat = ValidateCommand.ReportFormat.text
        // Initialize output handler
        command.output = new io.cldf.tool.utils.OutputHandler(command.outputFormat, command.quiet)
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

    def "validation service now returns detailed schema errors from new API"() {
        given:
        def tempFile = File.createTempFile("test", ".cldf")
        tempFile.deleteOnExit()
        // Use CLDFWriter to create a valid CLDF file
        createValidCLDF(tempFile)
        command.inputFile = tempFile

        and: "mocked validation service returns schema errors"
        validationService.validate(_) >> ValidationResult.builder()
            .valid(false)
            .errors([
                'manifest.json$: missing required property \'version\'',
                'manifest.json$.creationDate: invalid date format',
                'locations.json$[0]: missing required property \'id\'',
                'climbs.json$[0].grade: does not match pattern'
            ])
            .warnings(['2 climbs have dates in the future'])
            .build()

        when:
        def result = command.execute()

        then:
        !result.success
        result.exitCode == 1
        // Verify all error messages are preserved in the report
        result.message.contains('manifest.json$: missing required property \'version\'')
        result.message.contains('manifest.json$.creationDate: invalid date format')
        result.message.contains('locations.json$[0]: missing required property \'id\'')
        result.message.contains('climbs.json$[0].grade: does not match pattern')
        result.message.contains('2 climbs have dates in the future')
    }

    def "should format successful validation with new API"() {
        given:
        def tempFile = File.createTempFile("test", ".cldf")
        tempFile.deleteOnExit()
        createValidCLDF(tempFile)
        command.inputFile = tempFile

        and:
        validationService.validate(_) >> ValidationResult.builder()
            .valid(true)
            .errors([])
            .warnings([])
            .build()

        when:
        def result = command.execute()

        then:
        result.success
        result.exitCode == 0
        result.message.contains("VALID")
    }

    def "should format JSON report with validation errors"() {
        given:
        def tempFile = File.createTempFile("test", ".cldf")
        tempFile.deleteOnExit()
        createValidCLDF(tempFile)
        command.inputFile = tempFile
        command.reportFormat = ValidateCommand.ReportFormat.json

        and:
        validationService.validate(_) >> ValidationResult.builder()
            .valid(false)
            .errors(['Error 1', 'Error 2'])
            .warnings(['Warning 1'])
            .build()

        when:
        def result = command.execute()

        then:
        !result.success
        result.data != null
        result.data instanceof ValidateCommand.ValidationReport
        result.data.errors == ['Error 1', 'Error 2']
        result.data.warnings == ['Warning 1']
    }

    def "should enable all validations in strict mode"() {
        given:
        def tempFile = File.createTempFile("test", ".cldf")
        tempFile.deleteOnExit()
        createValidCLDF(tempFile)
        command.inputFile = tempFile
        command.validateSchema = false
        command.validateChecksums = false
        command.validateReferences = false
        command.strict = true

        and:
        validationService.validate(_) >> ValidationResult.builder()
            .valid(true)
            .errors([])
            .warnings([])
            .build()

        when:
        def result = command.execute()
        
        then:
        result.success
        result.exitCode == 0
        // The execute method should have enabled all validations when strict=true
        command.validateSchema
        command.validateChecksums  
        command.validateReferences
    }

    // Helper to create valid CLDF archive using CLDFWriter
    private void createValidCLDF(File file) {
        def archive = io.cldf.api.CLDFArchive.builder()
            .manifest(io.cldf.models.Manifest.builder()
                .version("1.0.0")
                .format("CLDF")
                .creationDate(java.time.OffsetDateTime.now())
                .appVersion("1.0.0")
                .platform(io.cldf.models.enums.Platform.ANDROID)
                .build())
            .locations([
                io.cldf.models.Location.builder()
                    .id(1)
                    .name("Test Location")
                    .isIndoor(false)
                    .coordinates(io.cldf.models.Location.Coordinates.builder()
                        .latitude(40.7128d)
                        .longitude(-74.0060d)
                        .build())
                    .build()
            ])
            .sessions([
                io.cldf.models.Session.builder()
                    .id(1)
                    .location("Test Location")
                    .locationId(1)
                    .date(java.time.LocalDate.now())
                    .build()
            ])
            .climbs([
                io.cldf.models.Climb.builder()
                    .id(1)
                    .sessionId(1)
                    .routeId(1)
                    .date(java.time.LocalDate.now())
                    .routeName("Test Route")
                    .attempts(1)
                    .type(io.cldf.models.enums.ClimbType.BOULDER)
                    .finishType(io.cldf.models.enums.FinishType.TOP)
                    .build()
            ])
            .build()

        new io.cldf.api.CLDFWriter().write(archive, file)
    }
    
    // Helper to create minimal valid CLDF archive
    private byte[] createMinimalCLDF() {
        // Create a minimal ZIP with just manifest
        def baos = new ByteArrayOutputStream()
        def zos = new java.util.zip.ZipOutputStream(baos)
        
        // Add manifest.json
        zos.putNextEntry(new java.util.zip.ZipEntry("manifest.json"))
        zos.write('''
        {
            "version": "1.0.0",
            "format": "CLDF",
            "creationDate": "2024-01-01T00:00:00Z",
            "appVersion": "1.0.0",
            "platform": "Android"
        }
        '''.bytes)
        zos.closeEntry()
        
        // Add minimal locations.json
        zos.putNextEntry(new java.util.zip.ZipEntry("locations.json"))
        zos.write('{"locations": [{"id": 1, "name": "Test", "isIndoor": false}]}'.bytes)
        zos.closeEntry()
        
        // Add minimal sessions.json
        zos.putNextEntry(new java.util.zip.ZipEntry("sessions.json"))
        zos.write('{"sessions": [{"id": 1, "location": "Test Location", "locationId": 1, "date": "2024-01-01"}]}'.bytes)
        zos.closeEntry()
        
        // Add minimal climbs.json
        zos.putNextEntry(new java.util.zip.ZipEntry("climbs.json"))
        zos.write('{"climbs": [{"id": 1, "sessionId": 1, "date": "2024-01-01", "routeName": "Test Route", "type": "boulder", "finishType": "top"}]}'.bytes)
        zos.closeEntry()
        
        // Add checksums.json (required)
        zos.putNextEntry(new java.util.zip.ZipEntry("checksums.json"))
        zos.write('''
        {
            "algorithm": "SHA-256",
            "files": {
                "manifest.json": "a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3",
                "locations.json": "b3b3b7e08bb86c366e6e5d11cb2c7e55ad6dc9da6c6e4f00a1f9da1a0a9f8234",
                "sessions.json": "c3c3c7e08bb86c366e6e5d11cb2c7e55ad6dc9da6c6e4f00a1f9da1a0a9f8234",
                "climbs.json": "d4d4d7e08bb86c366e6e5d11cb2c7e55ad6dc9da6c6e4f00a1f9da1a0a9f8234"
            },
            "generatedAt": "2024-01-01T00:00:00Z"
        }
        '''.bytes)
        zos.closeEntry()
        
        zos.close()
        return baos.toByteArray()
    }
}