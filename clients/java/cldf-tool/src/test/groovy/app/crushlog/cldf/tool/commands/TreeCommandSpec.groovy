package app.crushlog.cldf.tool.commands

import app.crushlog.cldf.api.CLDFArchive
import app.crushlog.cldf.models.*
import app.crushlog.cldf.models.enums.FinishType
import app.crushlog.cldf.tool.services.CLDFService
import app.crushlog.cldf.tool.services.TreeService
import app.crushlog.cldf.tool.utils.OutputHandler
import app.crushlog.cldf.tool.utils.OutputFormat
import spock.lang.Specification
import spock.lang.Subject

import java.time.LocalDate
import java.time.OffsetDateTime

class TreeCommandSpec extends Specification {

    def cldfService = Mock(CLDFService)
    def treeService = new TreeService()

    @Subject
    def command

    def setup() {
        command = new TreeCommand(cldfService, treeService)
        command.output = Mock(OutputHandler)
        command.outputFormat = OutputFormat.text
    }

    def "should display tree structure in text format"() {
        given: "a CLDF archive with hierarchical data"
        def testFile = new File("test.cldf")
        command.inputFile = testFile
        command.outputFormat = OutputFormat.text

        def archive = createTestArchive()

        when: "executing the tree command"
        def result = command.execute()

        then: "should read the archive and display tree structure"
        1 * cldfService.read(testFile) >> archive
        1 * command.output.write({ String text ->
            text.contains("CLDF Archive: test.cldf") &&
                    text.contains("Locations (1)") &&
                    text.contains("LA SKALA") &&
                    text.contains("Sessions (1)") &&
                    text.contains("Tags (1)")
        })
        result.success == true
        result.exitCode == 0
    }

    def "should display tree structure in JSON format"() {
        given: "a CLDF archive and JSON output format"
        def testFile = new File("test.cldf")
        command.inputFile = testFile
        command.outputFormat = OutputFormat.json
        command.output.isJsonFormat() >> true

        def archive = createTestArchive()

        when: "executing the tree command"
        def result = command.execute()

        then: "should read the archive and output JSON"
        1 * cldfService.read(testFile) >> archive
        1 * command.output.writeJson({ Map treeData ->
            treeData.archive == "test.cldf" &&
                    treeData.version == "1.0.0" &&
                    treeData.locations.size() == 1 &&
                    treeData.sessions.size() == 1
        })
        result.success == true
    }

    def "should include details when show-details flag is set"() {
        given: "show-details flag is enabled"
        def testFile = new File("test.cldf")
        command.inputFile = testFile
        command.outputFormat = OutputFormat.json
        command.output.isJsonFormat() >> true
        command.showDetails = true

        def archive = createTestArchive()

        when: "executing the tree command"
        command.execute()

        then: "should include location details in output"
        1 * cldfService.read(testFile) >> archive
        1 * command.output.writeJson({ Map treeData ->
            def location = treeData.locations[0]
            location.country == "Slovakia" &&
                    location.state == "Žilina" &&
                    location.coordinates != null &&
                    location.coordinates.latitude == 49.2234 &&
                    location.coordinates.longitude == 18.7394
        })
    }

    def "should handle empty archive gracefully"() {
        given: "an empty CLDF archive"
        def testFile = new File("empty.cldf")
        command.inputFile = testFile
        command.outputFormat = OutputFormat.json
        command.output.isJsonFormat() >> true

        def emptyArchive = CLDFArchive.builder()
                .manifest(Manifest.builder()
                        .version("1.0.0")
                        .creationDate(OffsetDateTime.now())
                        .build())
                .locations(Collections.emptyList())
                .sessions(Collections.emptyList())
                .climbs(Collections.emptyList())
                .routes(Collections.emptyList())
                .sectors(Collections.emptyList())
                .tags(Collections.emptyList())
                .build()

        when: "executing the tree command"
        command.execute()

        then: "should handle empty collections"
        1 * cldfService.read(testFile) >> emptyArchive
        1 * command.output.writeJson({ Map treeData ->
            treeData.locations.isEmpty() &&
                    treeData.sessions.isEmpty()
        })
    }

    def "should throw exception when archive reading fails"() {
        given: "a file that cannot be read"
        def testFile = new File("invalid.cldf")
        command.inputFile = testFile

        when: "executing the tree command"
        command.execute()

        then: "should throw exception"
        1 * cldfService.read(testFile) >> {
            throw new IOException("Cannot read file")
        }
        thrown(IOException)
    }

    def "should correctly build hierarchical relationships"() {
        given: "a complex archive with multiple levels"
        def testFile = new File("complex.cldf")
        command.inputFile = testFile
        command.outputFormat = OutputFormat.json
        command.output.isJsonFormat() >> true

        def archive = createComplexArchive()

        when: "executing the tree command"
        command.execute()

        then: "should correctly nest sectors under locations and routes under sectors"
        1 * cldfService.read(testFile) >> archive
        1 * command.output.writeJson({ Map treeData ->
            def location = treeData.locations[0]
            def session = treeData.sessions[0]
            location.sectors.size() == 2 &&
                    location.sectors[0].routes.size() == 2 &&
                    location.sectors[1].routes.size() == 1 &&
                    session.climbs.size() == 2 &&
                    session.climbs[0].route == "Test Route 1" &&
                    session.climbs[0].finishType == "REDPOINT"
        })
    }

    def "should format text output with proper tree structure"() {
        given: "text output format"
        def testFile = new File("test.cldf")
        command.inputFile = testFile
        command.outputFormat = OutputFormat.text
        command.showDetails = true

        def archive = createTestArchive()

        when: "executing the tree command"
        command.execute()

        then: "should create properly formatted tree"
        1 * cldfService.read(testFile) >> archive
        1 * command.output.write({ String text ->
            text.contains("│   ") &&  // Tree branches
                    text.contains("└── ") &&  // Tree end nodes
                    text.contains("├── ") &&  // Tree mid nodes
                    text.contains("coordinates: 49.2234, 18.7394") // Details shown
        })
    }

    private CLDFArchive createTestArchive() {
        def location = Location.builder()
                .id(1)
                .name("LA SKALA")
                .isIndoor(true)
                .country("Slovakia")
                .state("Žilina")
                .coordinates(Location.Coordinates.builder()
                        .latitude(49.2234)
                        .longitude(18.7394)
                        .build())
                .build()

        def sector = Sector.builder()
                .id(1)
                .locationId(1)
                .name("Main Wall")
                .build()

        def route = Route.builder()
                .id(1)
                .locationId(1)
                .sectorId(1)
                .name("Test Route")
                .grades(Route.Grades.builder().french("6a").build())
                .color("#FF0000")
                .build()

        def session = Session.builder()
                .id(1)
                .locationId(1)
                .date(LocalDate.now())
                .build()

        def climb = Climb.builder()
                .id(1)
                .sessionId(1)
                .routeId(1)
                .finishType(FinishType.REDPOINT)
                .build()

        def tag = Tag.builder()
                .id(1)
                .name("difficulty")
                .category("general")
                .isPredefined(false)
                .build()

        return CLDFArchive.builder()
                .manifest(Manifest.builder()
                        .version("1.0.0")
                        .creationDate(OffsetDateTime.now())
                        .build())
                .locations([location])
                .sectors([sector])
                .routes([route])
                .sessions([session])
                .climbs([climb])
                .tags([tag])
                .build()
    }

    private CLDFArchive createComplexArchive() {
        def location = Location.builder()
                .id(1)
                .name("Test Crag")
                .isIndoor(false)
                .build()

        def sector1 = Sector.builder()
                .id(1)
                .locationId(1)
                .name("North Face")
                .build()

        def sector2 = Sector.builder()
                .id(2)
                .locationId(1)
                .name("South Face")
                .build()

        def routes = [
                Route.builder().id(1).locationId(1).sectorId(1)
                        .name("Test Route 1").grades(Route.Grades.builder().french("5c").build()).build(),
                Route.builder().id(2).locationId(1).sectorId(1)
                        .name("Test Route 2").grades(Route.Grades.builder().french("6a").build()).build(),
                Route.builder().id(3).locationId(1).sectorId(2)
                        .name("Test Route 3").grades(Route.Grades.builder().french("7a").build()).build()
        ]

        def session = Session.builder()
                .id(1)
                .locationId(1)
                .date(LocalDate.now())
                .build()

        def climbs = [
                Climb.builder().id(1).sessionId(1).routeId(1)
                        .finishType(FinishType.REDPOINT).build(),
                Climb.builder().id(2).sessionId(1).routeId(2)
                        .finishType(FinishType.ONSIGHT).build()
        ]

        return CLDFArchive.builder()
                .manifest(Manifest.builder()
                        .version("1.0.0")
                        .creationDate(OffsetDateTime.now())
                        .build())
                .locations([location])
                .sectors([sector1, sector2])
                .routes(routes)
                .sessions([session])
                .climbs(climbs)
                .tags([])
                .build()
    }

    def "no-arg constructor should work for PicoCLI"() {
        when: "creating command with no-arg constructor"
        def cmd = new TreeCommand()

        then: "should create instance with null services"
        cmd != null
        // Services will be null, which is expected for PicoCLI instantiation
    }
}