package app.crushlog.cldf.tool.commands

import app.crushlog.cldf.schema.SchemaService
import app.crushlog.cldf.tool.models.CommandResult
import app.crushlog.cldf.tool.utils.OutputFormat
import app.crushlog.cldf.tool.utils.OutputHandler
import spock.lang.Specification
import spock.lang.Unroll

class SchemaCommandSpec extends Specification {

    SchemaCommand command
    SchemaService mockSchemaService
    OutputHandler mockOutput

    def setup() {
        mockSchemaService = Mock(SchemaService)
        command = new SchemaCommand(mockSchemaService)
        mockOutput = Mock(OutputHandler)
        command.outputFormat = OutputFormat.json
        command.output = mockOutput
    }

    @Unroll
    def "should retrieve schema for component '#component'"() {
        given:
        command.component = component
        mockSchemaService.getSchemaInfo(component) >> [(expectedKey): [:]]

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
        def mockEnums = [
            finishType: [
                boulder: ["flash", "top", "repeat", "project", "attempt"],
                route: ["flash", "top", "repeat", "project", "attempt", "onsight", "redpoint"]
            ]
        ]
        mockSchemaService.getSchemaInfo("enums") >> [enums: mockEnums]

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
        def mockMistakes = [
            "Route IDs should be strings, and routeId in climbs should also be strings (both must match)",
            "FinishType values for boulder climbs: flash, top, repeat, project, attempt (NOT onsight, redpoint)",
            "FinishType values for route climbs: flash, top, repeat, project, attempt, onsight, redpoint"
        ]
        mockSchemaService.getSchemaInfo("commonMistakes") >> [commonMistakes: mockMistakes]

        when:
        CommandResult result = command.execute()

        then:
        result.success
        def mistakes = result.data.commonMistakes
        mistakes.any { it.contains("Route IDs should be strings, and routeId in climbs should also be strings") }
        mistakes.any { it.contains("FinishType values for boulder climbs: flash, top, repeat, project, attempt") }
        mistakes.any { it.contains("FinishType values for route climbs: flash, top, repeat, project, attempt, onsight, redpoint") }
    }

    def "should handle IOException when loading schema"() {
        given:
        command.component = "manifest"
        mockSchemaService.getSchemaInfo("manifest") >> { throw new IOException("Failed to load schema") }

        when:
        def result = command.execute()

        then:
        !result.success
        result.exitCode == 1
        result.message.contains("Failed to retrieve schema information")
    }

    def "should output text format correctly"() {
        given:
        command.component = "enums"
        command.outputFormat = OutputFormat.text
        command.output = mockOutput
        def mockData = [
            enums: [
                platform: ["iOS", "Android"],
                climbType: ["boulder", "route"]
            ]
        ]
        mockSchemaService.getSchemaInfo("enums") >> mockData

        when:
        def result = command.execute()
        command.outputText(result)

        then:
        result.success
        1 * mockOutput.write("ENUMS SCHEMA:")
        _ * mockOutput.write(_) // Allow any number of writes for nested structure output
    }

    def "should provide example data with correct types"() {
        given:
        command.component = "exampleData"
        def mockExamples = [
            exampleData: [
                minimal: [
                    routes: [[id: "1", locationId: "1"]],
                    climbs: [[routeId: "1"]],
                    locations: [[id: 1]]
                ]
            ]
        ]
        mockSchemaService.getSchemaInfo("exampleData") >> mockExamples

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

    def "should handle exception for unknown component"() {
        given:
        command.component = "unknownComponent"
        mockSchemaService.getSchemaInfo("unknownComponent") >> { throw new IllegalArgumentException("Unknown component: unknownComponent") }

        when:
        def result = command.execute()

        then:
        !result.success
        result.exitCode == 1
        result.message == "Unknown component: unknownComponent"
    }

    def "should extract enums from schema service"() {
        given:
        command.component = "enums"
        def mockEnums = [
            enums: [
                platform: ["iOS", "Android", "Web", "Desktop"],
                routeType: ["boulder", "route"],
                gradeSystem: ["vScale", "font", "french", "yds", "uiaa"]
            ]
        ]
        mockSchemaService.getSchemaInfo("enums") >> mockEnums

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
        def mockDateFormats = [
            dateFormats: [
                description: "CLDF supports flexible date parsing with multiple formats",
                supportedFormats: ["ISO-8601", "yyyy-MM-dd"],
                examples: [
                    offsetDateTime: "2024-01-29T12:00:00Z",
                    localDate: "2024-01-29"
                ]
            ]
        ]
        mockSchemaService.getSchemaInfo("dateFormats") >> mockDateFormats

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
        def mockAllSchema = [
            manifest: ['$schema': "http://json-schema.org/draft-07/schema#"],
            location: [:],
            route: [:],
            climb: ['$schema': "http://json-schema.org/draft-07/schema#"],
            session: [:],
            tag: [:],
            enums: [:],
            dateFormats: [:],
            commonMistakes: [],
            exampleData: [:]
        ]
        mockSchemaService.getSchemaInfo("all") >> mockAllSchema

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