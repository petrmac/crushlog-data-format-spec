package io.cldf.tool.commands

import io.cldf.tool.models.CommandResult
import io.cldf.tool.utils.OutputFormat
import io.cldf.tool.utils.OutputHandler
import spock.lang.Specification
import spock.lang.Unroll

class SchemaCommandSpec extends Specification {

    SchemaCommand command
    OutputHandler mockOutput

    def setup() {
        command = new SchemaCommand()
        mockOutput = Mock(OutputHandler)
        command.outputFormat = OutputFormat.json
        command.output = mockOutput
    }

    @Unroll
    def "should retrieve schema for component '#component'"() {
        given:
        command.component = component

        when:
        CommandResult result = command.execute()

        then:
        result.success
        result.message == "Schema information retrieved successfully"
        result.data != null
        result.data.containsKey(expectedKey)

        where:
        component   | expectedKey
        "all"       | "manifest"
        "manifest"  | "manifest"
        "location"  | "location"
        "route"     | "route"
        "climb"     | "climb"
        "session"   | "session"
        "tag"       | "tag"
        "enums"     | "enums"
    }

    def "should include correct finish types for boulder and route climbs"() {
        given:
        command.component = "enums"

        when:
        CommandResult result = command.execute()

        then:
        result.success
        def enums = result.data.enums
        def finishTypes = enums.finishType

        // Boulder finish types should include 'attempt'
        finishTypes.boulder.contains("flash")
        finishTypes.boulder.contains("top")
        finishTypes.boulder.contains("repeat")
        finishTypes.boulder.contains("project")
        finishTypes.boulder.contains("attempt")
        !finishTypes.boulder.contains("onsight")
        !finishTypes.boulder.contains("redpoint")

        // Route finish types should include 'attempt'
        finishTypes.route.contains("flash")
        finishTypes.route.contains("top")
        finishTypes.route.contains("repeat")
        finishTypes.route.contains("project")
        finishTypes.route.contains("attempt")
        finishTypes.route.contains("onsight")
        finishTypes.route.contains("redpoint")
    }

    def "should include corrected common mistakes about route IDs"() {
        given:
        command.component = "commonMistakes"

        when:
        CommandResult result = command.execute()

        then:
        result.success
        def mistakes = result.data.commonMistakes
        mistakes.any { it.contains("Route IDs should be strings, and routeId in climbs should also be strings") }
        mistakes.any { it.contains("FinishType values for boulder climbs: flash, top, repeat, project, attempt") }
        mistakes.any { it.contains("FinishType values for route climbs: flash, top, repeat, project, attempt, onsight, redpoint") }
    }

    def "should load actual JSON schema from classpath for manifest component"() {
        given:
        command.component = "manifest"

        when:
        CommandResult result = command.execute()

        then:
        result.success
        def manifestSchema = result.data.manifest
        
        // Verify it's actual JSON schema
        manifestSchema.'$schema' == "http://json-schema.org/draft-07/schema#"
        manifestSchema.'$id' == "https://cldf.io/schemas/manifest.schema.json"
        manifestSchema.title == "CLDF Manifest"
        manifestSchema.required.contains("version")
        manifestSchema.required.contains("format")
        manifestSchema.required.contains("creationDate")
        manifestSchema.required.contains("appVersion")
        manifestSchema.required.contains("platform")
        
        // Verify platform enum values
        manifestSchema.properties.platform.enum == ["iOS", "Android", "Web", "Desktop"]
    }

    def "should load actual JSON schema from classpath for climb component"() {
        given:
        command.component = "climb"

        when:
        CommandResult result = command.execute()

        then:
        result.success
        def climbSchema = result.data.climb
        
        // Verify it's actual JSON schema
        climbSchema.'$schema' == "http://json-schema.org/draft-07/schema#"
        climbSchema.'$id' == "https://cldf.io/schemas/climbs.schema.json"
        climbSchema.title == "CLDF Climbs"
        
        // Verify routeId is now string type (not integer)
        climbSchema.definitions.climb.properties.routeId.type == "string"
        
        // Verify finish types include 'attempt' for both boulder and route
        def boulderFinishTypes = climbSchema.definitions.climb.allOf.find { 
            it.if?.properties?.type?.const == "boulder" 
        }?.then?.properties?.finishType?.enum
        boulderFinishTypes != null
        boulderFinishTypes.contains("attempt")
        
        def routeFinishTypes = climbSchema.definitions.climb.allOf.find { 
            it.if?.properties?.type?.const == "route" 
        }?.then?.properties?.finishType?.enum
        routeFinishTypes != null
        routeFinishTypes.contains("attempt")
    }

    def "should provide example data with correct types"() {
        given:
        command.component = "exampleData"

        when:
        CommandResult result = command.execute()

        then:
        result.success
        def examples = result.data.exampleData
        def minimal = examples.minimal
        
        // Verify route ID is string
        minimal.routes[0].id instanceof String
        
        // Verify climb routeId is string (matching route.id)
        minimal.climbs[0].routeId instanceof String
        minimal.climbs[0].routeId == minimal.routes[0].id
        
        // Verify location ID is integer
        minimal.locations[0].id instanceof Integer
        
        // Verify locationId in route is string
        minimal.routes[0].locationId instanceof String
    }

    def "should throw exception for unknown component"() {
        given:
        command.component = "unknownComponent"

        when:
        command.execute()

        then:
        IllegalArgumentException ex = thrown()
        ex.message.contains("Unknown component: unknownComponent")
    }

    def "should extract enums from actual schema files"() {
        given:
        command.component = "enums"

        when:
        CommandResult result = command.execute()

        then:
        result.success
        def enums = result.data.enums
        
        // Verify platform enum extracted from manifest schema
        enums.platform == ["iOS", "Android", "Web", "Desktop"]
        
        // Verify route type enum
        enums.routeType == ["boulder", "route"]
        
        // Verify grade system enum
        enums.gradeSystem.contains("vScale")
        enums.gradeSystem.contains("font")
        enums.gradeSystem.contains("french")
        enums.gradeSystem.contains("yds")
        enums.gradeSystem.contains("uiaa")
    }

    def "should handle date formats information"() {
        given:
        command.component = "dateFormats"

        when:
        CommandResult result = command.execute()

        then:
        result.success
        def dateFormats = result.data.dateFormats
        dateFormats.description.contains("flexible date parsing")
        dateFormats.supportedFormats.size() > 0
        dateFormats.examples.offsetDateTime == "2024-01-29T12:00:00Z"
        dateFormats.examples.localDate == "2024-01-29"
    }

    def "should provide comprehensive schema for 'all' component"() {
        given:
        command.component = "all"

        when:
        CommandResult result = command.execute()

        then:
        result.success
        def schema = result.data
        
        // Verify all major components are present
        schema.containsKey("manifest")
        schema.containsKey("location")
        schema.containsKey("route")
        schema.containsKey("climb")
        schema.containsKey("session")
        schema.containsKey("tag")
        schema.containsKey("enums")
        schema.containsKey("dateFormats")
        schema.containsKey("commonMistakes")
        schema.containsKey("exampleData")
        
        // Spot check that actual schemas are loaded
        schema.manifest.'$schema' == "http://json-schema.org/draft-07/schema#"
        schema.climb.'$schema' == "http://json-schema.org/draft-07/schema#"
    }
}