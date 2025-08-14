package app.crushlog.cldf.tool.commands

import spock.lang.Specification
import app.crushlog.cldf.api.CLDFArchive
import app.crushlog.cldf.models.*
import app.crushlog.cldf.models.enums.*
import app.crushlog.cldf.tool.services.ValidationService
import app.crushlog.cldf.tool.utils.OutputFormat
import app.crushlog.cldf.tool.utils.OutputHandler
import io.micronaut.context.ApplicationContext

class ValidateCommandIntegrationSpec extends Specification {

    def applicationContext = ApplicationContext.run()
    def validationService = applicationContext.getBean(ValidationService)
    def command = new ValidateCommand(validationService)

    def setup() {
        command.outputFormat = OutputFormat.text
        command.quiet = false
        command.validateSchema = false // Disable schema validation by default
        command.validateChecksums = false // Disable checksum validation by default  
        command.validateReferences = true
        command.reportFormat = ValidateCommand.ReportFormat.text
        command.output = new OutputHandler(command.outputFormat, command.quiet)
    }

    def cleanup() {
        applicationContext.close()
    }

    def "should validate a valid CLDF archive without schema validation"() {
        given: "a valid CLDF archive"
        def tempFile = File.createTempFile("test", ".cldf")
        tempFile.deleteOnExit()
        createMinimalValidArchive(tempFile)
        command.inputFile = tempFile

        when:
        def result = command.execute()

        then:
        result.success
        result.exitCode == 0
        result.message.contains("VALID")
        result.message.contains("Validation Report")
    }

    def "should detect invalid enum values with schema validation"() {
        given: "schema validation enabled"
        command.validateSchema = true
        
        and: "an archive with invalid enum values"
        def tempFile = File.createTempFile("test", ".cldf")
        tempFile.deleteOnExit()
        createArchiveWithInvalidEnums(tempFile)
        command.inputFile = tempFile

        when:
        def result = command.execute()

        then:
        !result.success
        result.exitCode == 1
        result.message.contains("Schema validation failed")
    }

    def "should detect reference integrity violations"() {
        given: "an archive with broken references"
        def tempFile = File.createTempFile("test", ".cldf")
        tempFile.deleteOnExit()
        createArchiveWithBrokenReferences(tempFile)
        command.inputFile = tempFile

        when:
        def result = command.execute()

        then:
        !result.success
        result.exitCode == 1
        result.message.contains("Reference Error") || result.message.contains("references non-existent")
    }

    def "should detect checksum mismatches when enabled"() {
        given: "checksum validation enabled"
        command.validateChecksums = true
        
        and: "an archive with invalid checksums"
        def tempFile = File.createTempFile("test", ".cldf")
        tempFile.deleteOnExit()
        createArchiveWithInvalidChecksums(tempFile)
        command.inputFile = tempFile

        when:
        def result = command.execute()

        then:
        !result.success
        result.exitCode == 1
        result.message.contains("Checksum")
    }

    def "should report warnings for future dates"() {
        given: "an archive with future dates"
        def tempFile = File.createTempFile("test", ".cldf")
        tempFile.deleteOnExit()
        createArchiveWithWarnings(tempFile)
        command.inputFile = tempFile

        when:
        def result = command.execute()

        then:
        result.success
        result.exitCode == 0
        result.message.contains("VALID")
        result.message.contains("WARNINGS") || result.message.contains("future")
    }

    def "should handle empty archive"() {
        given: "an empty archive"
        def tempFile = File.createTempFile("test", ".cldf")
        tempFile.deleteOnExit()
        createEmptyArchive(tempFile)
        command.inputFile = tempFile

        when:
        def result = command.execute()

        then:
        !result.success
        result.exitCode == 1
        result.message.contains("Missing required file")
    }

    def "should format output as JSON when requested"() {
        given: "JSON output requested"
        command.reportFormat = ValidateCommand.ReportFormat.json
        
        and: "a valid archive"
        def tempFile = File.createTempFile("test", ".cldf")
        tempFile.deleteOnExit()
        createMinimalValidArchive(tempFile)
        command.inputFile = tempFile

        when:
        def result = command.execute()

        then:
        result.success
        result.data != null
        result.data instanceof ValidateCommand.ValidationReport
    }

    def "should enable all validations in strict mode"() {
        given: "strict mode enabled"
        command.strict = true
        
        and: "a valid archive"
        def tempFile = File.createTempFile("test", ".cldf")
        tempFile.deleteOnExit()
        createMinimalValidArchive(tempFile)
        command.inputFile = tempFile

        when:
        command.execute()

        then:
        command.validateSchema
        command.validateChecksums
        command.validateReferences
    }

    // Helper methods to create test archives

    private void createMinimalValidArchive(File file) {
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
                    .name("Test Crag")
                    .isIndoor(false)
                    .build()
            ])
            .sessions([
                Session.builder()
                    .id(1)
                    .location("Test Crag")
                    .locationId(1)
                    .date(java.time.LocalDate.now())
                    .build()
            ])
            .climbs([
                Climb.builder()
                    .id(1)
                    .sessionId(1)
                    .date(java.time.LocalDate.now())
                    .routeName("Easy Boulder")
                    .attempts(1)
                    .type(ClimbType.BOULDER)
                    .finishType(FinishType.TOP)
                    .build()
            ])
            .build()

        // Write without validation to avoid schema issues
        new app.crushlog.cldf.api.CLDFWriter(false, false).write(archive, file)
    }

    private void createArchiveWithInvalidEnums(File file) {
        // Manually create a ZIP with invalid enum values
        def baos = new ByteArrayOutputStream()
        def zos = new java.util.zip.ZipOutputStream(baos)
        
        // Add manifest.json with invalid platform
        zos.putNextEntry(new java.util.zip.ZipEntry("manifest.json"))
        zos.write('''
        {
            "version": "1.0.0",
            "format": "CLDF",
            "creationDate": "2024-01-01T00:00:00Z",
            "appVersion": "1.0.0",
            "platform": "invalid_platform"
        }
        '''.bytes)
        zos.closeEntry()
        
        // Add minimal required files
        zos.putNextEntry(new java.util.zip.ZipEntry("locations.json"))
        zos.write('{"locations": []}'.bytes)
        zos.closeEntry()
        
        zos.putNextEntry(new java.util.zip.ZipEntry("sessions.json"))
        zos.write('{"sessions": []}'.bytes)
        zos.closeEntry()
        
        zos.putNextEntry(new java.util.zip.ZipEntry("climbs.json"))
        zos.write('{"climbs": []}'.bytes)
        zos.closeEntry()
        
        zos.putNextEntry(new java.util.zip.ZipEntry("checksums.json"))
        zos.write('{"algorithm": "SHA-256", "files": {}, "generatedAt": "2024-01-01T00:00:00Z"}'.bytes)
        zos.closeEntry()
        
        zos.close()
        file.bytes = baos.toByteArray()
    }

    private void createArchiveWithBrokenReferences(File file) {
        def archive = CLDFArchive.builder()
            .manifest(Manifest.builder()
                .version("1.0.0")
                .format("CLDF")
                .creationDate(java.time.OffsetDateTime.now())
                .appVersion("1.0.0")
                .platform(Platform.IOS)
                .build())
            .locations([
                Location.builder()
                    .id(1)
                    .name("Test Location")
                    .isIndoor(false)
                    .build()
            ])
            .sessions([
                Session.builder()
                    .id(1)
                    .location("Test Location")
                    .locationId(999) // Non-existent location
                    .date(java.time.LocalDate.now())
                    .build()
            ])
            .climbs([
                Climb.builder()
                    .id(1)
                    .sessionId(999) // Non-existent session
                    .date(java.time.LocalDate.now())
                    .routeName("Ghost Route")
                    .attempts(1)
                    .type(ClimbType.BOULDER)
                    .finishType(FinishType.TOP)
                    .build()
            ])
            .build()

        new app.crushlog.cldf.api.CLDFWriter(false, false).write(archive, file)
    }

    private void createArchiveWithInvalidChecksums(File file) {
        // Create a valid archive first
        createMinimalValidArchive(file)
        
        // Then modify the checksums to be invalid
        def tempDir = File.createTempDir()
        def zipFile = new java.util.zip.ZipFile(file)
        def entries = zipFile.entries()
        
        // Extract all files
        def extractedFiles = [:]
        while (entries.hasMoreElements()) {
            def entry = entries.nextElement()
            def extractedFile = new File(tempDir, entry.name)
            if (!entry.isDirectory()) {
                extractedFile.parentFile.mkdirs()
                extractedFile.bytes = zipFile.getInputStream(entry).bytes
                extractedFiles[entry.name] = extractedFile
            }
        }
        zipFile.close()
        
        // Modify checksums.json with invalid hashes
        def checksumsFile = extractedFiles["checksums.json"]
        checksumsFile.text = '''
        {
            "algorithm": "SHA-256",
            "files": {
                "manifest.json": "0000000000000000000000000000000000000000000000000000000000000000",
                "locations.json": "1111111111111111111111111111111111111111111111111111111111111111",
                "sessions.json": "2222222222222222222222222222222222222222222222222222222222222222",
                "climbs.json": "3333333333333333333333333333333333333333333333333333333333333333"
            },
            "generatedAt": "2024-01-01T00:00:00Z"
        }
        '''
        
        // Repackage the archive
        def baos = new ByteArrayOutputStream()
        def zos = new java.util.zip.ZipOutputStream(baos)
        extractedFiles.each { name, extractedFile ->
            zos.putNextEntry(new java.util.zip.ZipEntry(name))
            zos.write(extractedFile.bytes)
            zos.closeEntry()
        }
        zos.close()
        file.bytes = baos.toByteArray()
        
        // Clean up
        tempDir.deleteDir()
    }

    private void createArchiveWithWarnings(File file) {
        def futureDate = java.time.LocalDate.now().plusDays(30)
        
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
                    .name("Future Gym")
                    .isIndoor(true)
                    .build()
            ])
            .sessions([
                Session.builder()
                    .id(1)
                    .location("Future Gym")
                    .locationId(1)
                    .date(futureDate)
                    .build()
            ])
            .climbs([
                Climb.builder()
                    .id(1)
                    .sessionId(1)
                    .date(futureDate)
                    .routeName("Time Travel V5")
                    .attempts(1)
                    .type(ClimbType.BOULDER)
                    .finishType(FinishType.FLASH)
                    .build(),
                Climb.builder()
                    .id(2)
                    .sessionId(1)
                    .date(futureDate)
                    .routeName("Future Problem")
                    .attempts(2)
                    .type(ClimbType.BOULDER)
                    .finishType(FinishType.TOP)
                    .build()
            ])
            .build()

        new app.crushlog.cldf.api.CLDFWriter(false, false).write(archive, file)
    }

    private void createEmptyArchive(File file) {
        // Create an empty ZIP file
        def baos = new ByteArrayOutputStream()
        def zos = new java.util.zip.ZipOutputStream(baos)
        zos.close()
        file.bytes = baos.toByteArray()
    }
}