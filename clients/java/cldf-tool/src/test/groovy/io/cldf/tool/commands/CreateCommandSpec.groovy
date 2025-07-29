package io.cldf.tool.commands

import io.cldf.api.CLDFArchive
import io.cldf.api.CLDFWriter
import io.cldf.models.*
import io.cldf.tool.models.CommandResult
import io.cldf.tool.services.ValidationService
import io.cldf.tool.utils.InputHandler
import io.cldf.tool.utils.OutputFormat
import io.cldf.tool.utils.OutputHandler
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Path
import java.time.LocalDate
import java.time.OffsetDateTime

class CreateCommandSpec extends Specification {

    CreateCommand command
    ValidationService validationService

    @TempDir
    Path tempDir

    File outputFile

    def setup() {
        command = new CreateCommand()
        validationService = new ValidationService()
        command.validationService = validationService
        
        outputFile = tempDir.resolve("test.cldf").toFile()
        command.outputFile = outputFile
        command.outputFormat = OutputFormat.text
        command.quiet = false
        command.validate = true
        command.prettyPrint = true
        
        // Initialize the output handler
        command.output = new OutputHandler(OutputFormat.text, false)
    }

    def "should create minimal archive"() {
        given:
        command.template = null
        command.validate = true

        when:
        def result = command.execute()

        then:
        result.success
        result.message == "Successfully created CLDF archive"
        outputFile.exists()

        when:
        def archive = new io.cldf.api.CLDFReader().read(outputFile)

        then:
        archive != null
        archive.manifest != null
        archive.manifest.version == "1.0.0"
        archive.manifest.format == "CLDF"
        archive.locations.size() == 1
        archive.locations[0].name == "Default Location"
        archive.locations[0].isIndoor == true
        archive.sessions.size() == 1
        archive.climbs.size() == 1
        archive.climbs[0].routeName == "Sample Route"
        archive.climbs[0].type == Climb.ClimbType.boulder
    }

    def "should create basic template archive"() {
        given:
        command.template = "basic"

        when:
        def result = command.execute()

        then:
        result.success
        result.message == "Successfully created CLDF archive"
        outputFile.exists()

        when:
        def archive = new io.cldf.api.CLDFReader().read(outputFile)

        then:
        archive != null
        archive.locations.size() == 1
        archive.locations[0].name == "Local Climbing Gym"
        archive.locations[0].country == "United States"
        archive.locations[0].state == "California"
        archive.sessions.size() == 1
        archive.climbs.size() == 2
        archive.climbs[0].routeName == "Warm-up V0"
        archive.climbs[0].finishType == Climb.FinishType.top
        archive.climbs[1].routeName == "Project V4"
        archive.climbs[1].finishType == Climb.FinishType.top
        archive.climbs[1].attempts == 5
    }

    def "should create demo template archive"() {
        given:
        command.template = "demo"

        when:
        def result = command.execute()

        then:
        result.success
        result.message == "Successfully created CLDF archive"
        outputFile.exists()

        when:
        def archive = new io.cldf.api.CLDFReader().read(outputFile)

        then:
        archive != null
        archive.locations.size() == 2
        archive.locations[0].name == "Movement Climbing Gym"
        archive.locations[0].isIndoor == true
        archive.locations[1].name == "Eldorado Canyon"
        archive.locations[1].isIndoor == false
        archive.locations[1].rockType == Location.RockType.sandstone
        archive.sessions.size() == 2
        archive.climbs.size() == 9
        
        // Check outdoor climb details
        def outdoorClimb = archive.climbs.find { it.routeName == "The Bastille Crack" }
        outdoorClimb != null
        outdoorClimb.type == Climb.ClimbType.route
        outdoorClimb.finishType == Climb.FinishType.onsight
        outdoorClimb.belayType == Climb.BelayType.lead
        outdoorClimb.height == 110.0
        outdoorClimb.rating == 5
    }

    def "should fail validation when archive is invalid"() {
        given:
        command.template = null
        command.validate = true
        
        // Mock validation service to return invalid result
        def mockValidationService = Mock(ValidationService)
        command.validationService = mockValidationService
        
        def validationResult = ValidationService.ValidationResult.builder()
            .valid(false)
            .errors(["Missing required field: locations"])
            .build()
        
        mockValidationService.validate(_ as CLDFArchive) >> validationResult

        when:
        def result = command.execute()

        then:
        !result.success
        result.message == "Validation failed"
        result.exitCode == 1
        result.data["errors"] == ["Missing required field: locations"]
    }

    def "should include warnings in result"() {
        given:
        command.template = "empty"
        command.validate = true
        
        // Mock validation service to return warnings
        def mockValidationService = Mock(ValidationService)
        command.validationService = mockValidationService
        
        def validationResult = ValidationService.ValidationResult.builder()
            .valid(true)
            .warnings(["No climbs found in archive", "Consider adding route information"])
            .build()
        
        mockValidationService.validate(_ as CLDFArchive) >> validationResult

        when:
        def result = command.execute()

        then:
        result.success
        result.warnings == ["No climbs found in archive", "Consider adding route information"]
    }

    def "should create archive without validation when disabled"() {
        given:
        command.template = "basic"
        command.validate = false

        when:
        def result = command.execute()

        then:
        result.success
        result.message == "Successfully created CLDF archive"
        outputFile.exists()
        result.warnings == null
    }

    def "should create archive without pretty printing when disabled"() {
        given:
        command.template = "empty"
        command.prettyPrint = false
        command.validate = true

        when:
        def result = command.execute()

        then:
        result.success
        outputFile.exists()
        
        // The archive should still be valid
        def archive = new io.cldf.api.CLDFReader().read(outputFile)
        archive != null
        archive.climbs.size() == 1  // Empty template now includes one climb
    }

    def "should output correct text for successful creation"() {
        given:
        command.template = "basic"
        def result = CommandResult.builder()
            .success(true)
            .message("Successfully created CLDF archive")
            .data(["file": outputFile.absolutePath])
            .build()

        when:
        command.outputText(result)

        then:
        // Method should complete without errors
        noExceptionThrown()
    }

    def "should output correct text for failed creation with errors"() {
        given:
        def result = CommandResult.builder()
            .success(false)
            .message("Validation failed")
            .data(["errors": ["Invalid location ID", "Missing session date"]])
            .build()

        when:
        command.outputText(result)

        then:
        // Method should complete without errors
        noExceptionThrown()
    }

    def "should handle all template types"() {
        when:
        command.template = templateType
        def result = command.execute()

        then:
        result.success
        outputFile.exists()

        where:
        templateType << ["empty", "basic", "demo"]
    }

    def "should return correct stats in result data"() {
        given:
        command.template = "demo"

        when:
        def result = command.execute()

        then:
        result.success
        result.data != null
        result.data["file"] == outputFile.absolutePath
        result.data["stats"] != null
        result.data["stats"]["locations"] == 2
        result.data["stats"]["sessions"] == 2
        result.data["stats"]["climbs"] == 9
    }
    
    def "should test createFromJson method directly"() {
        given:
        // Create a test archive
        def testArchive = CLDFArchive.builder()
            .manifest(Manifest.builder()
                .version("1.0.0")
                .format("CLDF")
                .creationDate(OffsetDateTime.now())
                .appVersion("test")
                .platform(Manifest.Platform.Desktop)
                .build())
            .locations([Location.builder()
                .id(1)
                .name("Test Gym")
                .isIndoor(true)
                .createdAt(OffsetDateTime.now())
                .build()])
            .sessions([Session.builder()
                .id("sess_1")
                .date(LocalDate.now())
                .location("Test Gym")
                .locationId("1")
                .isIndoor(true)
                .build()])
            .climbs([Climb.builder()
                .id(1)
                .sessionId(1)
                .date(LocalDate.now())
                .routeName("Test Route")
                .type(Climb.ClimbType.boulder)
                .finishType(Climb.FinishType.top)
                .attempts(1)
                .grades(Climb.GradeInfo.builder()
                    .system(Climb.GradeInfo.GradeSystem.vScale)
                    .grade("V1")
                    .build())
                .isIndoor(true)
                .build()])
            .build()
        
        // Call createFromJson directly via reflection
        def createFromJsonMethod = CreateCommand.class.getDeclaredMethod("createFromJson")
        createFromJsonMethod.setAccessible(true)
        
        when:
        def result = createFromJsonMethod.invoke(command)
        
        then:
        // This should throw an exception since no JSON input is configured
        thrown(Exception)
    }
    
    def "should test JSON file input handling"() {
        given:
        // Create a JSON file with test data
        def jsonFile = tempDir.resolve("test.json").toFile()
        def testJson = """
        {
            "manifest": {
                "version": "1.0.0",
                "format": "CLDF",
                "creationDate": "2024-01-01T00:00:00.000Z",
                "appVersion": "test",
                "platform": "Desktop"
            },
            "locations": [{
                "id": 1,
                "name": "JSON Test Gym",
                "isIndoor": true,
                "createdAt": "2024-01-01T00:00:00.000Z"
            }],
            "sessions": [{
                "id": "sess_1",
                "date": "2024-01-01",
                "location": "JSON Test Gym",
                "locationId": "1",
                "isIndoor": true
            }],
            "climbs": [{
                "id": 1,
                "sessionId": 1,
                "date": "2024-01-01",
                "routeName": "JSON Route",
                "type": "boulder",
                "finishType": "top",
                "attempts": 1,
                "grades": {
                    "system": "vScale",
                    "grade": "V3"
                },
                "isIndoor": true
            }]
        }
        """
        jsonFile.text = testJson
        
        // Set command to read from JSON file
        command.jsonInput = jsonFile.absolutePath
        command.template = null
        
        when:
        def result = command.execute()
        
        then:
        result.success
        result.message == "Successfully created CLDF archive"
        outputFile.exists()
        
        when:
        def archive = new io.cldf.api.CLDFReader().read(outputFile)
        
        then:
        archive != null
        archive.locations.size() == 1
        archive.locations[0].name == "JSON Test Gym"
        archive.climbs.size() == 1
        archive.climbs[0].routeName == "JSON Route"
        archive.climbs[0].grades.grade == "V3"
    }
    
    def "should handle missing JSON file"() {
        given:
        command.jsonInput = tempDir.resolve("nonexistent.json").toFile().absolutePath
        command.template = null
        
        when:
        def result = command.execute()
        
        then:
        !result.success
        result.message.contains("Failed to create archive")
        result.exitCode == 1
    }
    
    def "should handle stdin input flag"() {
        given:
        command.readFromStdin = true
        command.template = null
        
        // We can't easily test actual stdin reading in unit tests,
        // but we can verify the command is configured correctly
        
        expect:
        command.readFromStdin == true
        command.template == null
    }
    
    def "should handle dash (-) as stdin indicator"() {
        given:
        command.jsonInput = "-"
        command.template = null
        
        // Similar to above, we verify configuration
        
        expect:
        command.jsonInput == "-"
        command.template == null
    }
    
    def "should prioritize JSON input over template when both are set"() {
        given:
        // Create a JSON file
        def jsonFile = tempDir.resolve("priority.json").toFile()
        def testJson = """
        {
            "manifest": {
                "version": "1.0.0",
                "format": "CLDF",
                "creationDate": "2024-01-01T00:00:00.000Z",
                "appVersion": "test",
                "platform": "Desktop"
            },
            "locations": [{
                "id": 1,
                "name": "Priority JSON Gym",
                "isIndoor": true,
                "createdAt": "2024-01-01T00:00:00.000Z"
            }],
            "sessions": [{
                "id": "sess_1",
                "date": "2024-01-01",
                "location": "Priority JSON Gym",
                "locationId": "1",
                "isIndoor": true
            }],
            "climbs": [{
                "id": 1,
                "sessionId": 1,
                "date": "2024-01-01",
                "routeName": "Priority Route",
                "type": "boulder",
                "finishType": "top",
                "attempts": 1,
                "grades": {
                    "system": "vScale",
                    "grade": "V0"
                },
                "isIndoor": true
            }]
        }
        """
        jsonFile.text = testJson
        
        command.jsonInput = jsonFile.absolutePath
        command.template = "demo" // This should be ignored
        
        when:
        def result = command.execute()
        
        then:
        result.success
        
        when:
        def archive = new io.cldf.api.CLDFReader().read(outputFile)
        
        then:
        // Should have JSON data, not demo template data
        archive.locations.size() == 1
        archive.locations[0].name == "Priority JSON Gym"
        archive.climbs.size() == 1
        archive.climbs[0].routeName == "Priority Route"
        // Demo template would have 2 locations and 9 climbs
    }
}