package io.cldf.tool.commands

import io.cldf.api.CLDFArchive
import io.cldf.tool.models.CommandResult
import io.cldf.tool.services.ValidationService
import io.cldf.tool.utils.OutputFormat
import io.cldf.tool.utils.OutputHandler
import io.micronaut.context.ApplicationContext
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Path

@MicronautTest
class CreateCommandSpec extends Specification {
    
    @Inject
    ApplicationContext context
    
    @TempDir
    Path tempDir
    
    CreateCommand command
    ValidationService validationService
    ByteArrayOutputStream outStream
    ByteArrayOutputStream errStream
    
    def setup() {
        validationService = Mock(ValidationService)
        command = new CreateCommand()
        command.validationService = validationService
        
        // Setup output streams
        outStream = new ByteArrayOutputStream()
        errStream = new ByteArrayOutputStream()
        command.output = new OutputHandler(OutputFormat.text, false, 
            new PrintStream(outStream), new PrintStream(errStream))
    }
    
    def "should create empty CLDF archive"() {
        given:
        def outputFile = tempDir.resolve("test.cldf").toFile()
        command.outputFile = outputFile
        command.template = null
        command.validate = false
        
        when:
        def result = command.execute()
        
        then:
        result.success
        result.message == "Successfully created CLDF archive"
        outputFile.exists()
        
        and: "stats are included"
        def data = result.data as Map
        data.file == outputFile.absolutePath
        data.stats.locations == 1
        data.stats.sessions == 1
        data.stats.climbs == 0
    }
    
    def "should create archive from basic template"() {
        given:
        def outputFile = tempDir.resolve("basic.cldf").toFile()
        command.outputFile = outputFile
        command.template = "basic"
        command.validate = false
        
        when:
        def result = command.execute()
        
        then:
        result.success
        outputFile.exists()
        
        and: "basic template has expected content"
        def data = result.data as Map
        data.stats.locations == 1
        data.stats.sessions == 1
        data.stats.climbs == 2
    }
    
    def "should create archive from demo template"() {
        given:
        def outputFile = tempDir.resolve("demo.cldf").toFile()
        command.outputFile = outputFile
        command.template = "demo"
        command.validate = false
        
        when:
        def result = command.execute()
        
        then:
        result.success
        outputFile.exists()
        
        and: "demo template has rich content"
        def data = result.data as Map
        data.stats.locations == 2
        data.stats.sessions == 2
        data.stats.climbs == 9
    }
    
    def "should validate archive when validate flag is true"() {
        given:
        def outputFile = tempDir.resolve("validated.cldf").toFile()
        command.outputFile = outputFile
        command.validate = true
        validationService.validate(_ as CLDFArchive) >> new ValidationService.ValidationResult(
            valid: true,
            errors: [],
            warnings: ["Minor issue"]
        )
        
        when:
        def result = command.execute()
        
        then:
        result.success
        result.warnings == ["Minor issue"]
        1 * validationService.validate(_ as CLDFArchive)
    }
    
    def "should fail when validation fails"() {
        given:
        def outputFile = tempDir.resolve("invalid.cldf").toFile()
        command.outputFile = outputFile
        command.validate = true
        validationService.validate(_ as CLDFArchive) >> new ValidationService.ValidationResult(
            valid: false,
            errors: ["Critical error"],
            warnings: []
        )
        
        when:
        def result = command.execute()
        
        then:
        !result.success
        result.message == "Validation failed"
        result.exitCode == 1
        
        and: "errors are included in data"
        def data = result.data as Map
        data.errors == ["Critical error"]
    }
    
    def "should create archive from JSON file"() {
        given:
        def jsonFile = tempDir.resolve("input.json").toFile()
        jsonFile.text = '''{
            "manifest": {
                "version": "1.0.0",
                "format": "CLDF",
                "creationDate": "2024-01-01T00:00:00Z"
            },
            "locations": [{
                "id": 1,
                "name": "Test Location",
                "isIndoor": true
            }],
            "sessions": [],
            "climbs": []
        }'''
        
        def outputFile = tempDir.resolve("from-json.cldf").toFile()
        command.outputFile = outputFile
        command.jsonInput = jsonFile.absolutePath
        command.validate = false
        
        when:
        def result = command.execute()
        
        then:
        result.success
        outputFile.exists()
        
        and: "archive contains data from JSON"
        def data = result.data as Map
        data.stats.locations == 1
    }
    
    def "should create archive from stdin"() {
        given:
        def originalStdin = System.in
        def jsonInput = '''{
            "manifest": {
                "version": "1.0.0",
                "format": "CLDF",
                "creationDate": "2024-01-01T00:00:00Z"
            },
            "locations": [],
            "sessions": [],
            "climbs": []
        }'''
        System.in = new ByteArrayInputStream(jsonInput.bytes)
        
        def outputFile = tempDir.resolve("from-stdin.cldf").toFile()
        command.outputFile = outputFile
        command.readFromStdin = true
        command.validate = false
        
        when:
        def result = command.execute()
        
        then:
        result.success
        outputFile.exists()
        
        cleanup:
        System.in = originalStdin
    }
    
    def "should support JSON output format"() {
        given:
        def outputFile = tempDir.resolve("json-output.cldf").toFile()
        command.outputFile = outputFile
        command.template = "empty"
        command.validate = false
        command.outputFormat = OutputFormat.json
        command.output = new OutputHandler(OutputFormat.json, false,
            new PrintStream(outStream), new PrintStream(errStream))
        
        when:
        command.run()
        
        then:
        def jsonOutput = outStream.toString()
        jsonOutput.contains('"success" : true')
        jsonOutput.contains('"message" : "Successfully created CLDF archive"')
        jsonOutput.contains('"stats"')
    }
    
    def "should handle missing output file gracefully"() {
        given:
        command.outputFile = null
        
        when:
        command.execute()
        
        then:
        thrown(NullPointerException)
    }
    
    def "should prefer JSON input over template"() {
        given:
        def jsonFile = tempDir.resolve("priority.json").toFile()
        jsonFile.text = '''{
            "manifest": {"version": "1.0.0", "format": "CLDF"},
            "locations": [],
            "sessions": [],
            "climbs": []
        }'''
        
        def outputFile = tempDir.resolve("priority.cldf").toFile()
        command.outputFile = outputFile
        command.jsonInput = jsonFile.absolutePath
        command.template = "basic"  // This should be ignored
        command.validate = false
        
        when:
        def result = command.execute()
        
        then:
        result.success
        
        and: "JSON input was used, not template"
        def data = result.data as Map
        data.stats.locations == 0  // Empty from JSON, not 1 from basic template
    }
    
    def "should handle dash as stdin indicator"() {
        given:
        def originalStdin = System.in
        def jsonInput = '''{
            "manifest": {"version": "1.0.0", "format": "CLDF"},
            "locations": [{"id": 2, "name": "From Stdin"}],
            "sessions": [],
            "climbs": []
        }'''
        System.in = new ByteArrayInputStream(jsonInput.bytes)
        
        def outputFile = tempDir.resolve("dash-stdin.cldf").toFile()
        command.outputFile = outputFile
        command.jsonInput = "-"
        command.validate = false
        
        when:
        def result = command.execute()
        
        then:
        result.success
        
        and: "data came from stdin"
        def data = result.data as Map
        data.stats.locations == 1
        
        cleanup:
        System.in = originalStdin
    }
}