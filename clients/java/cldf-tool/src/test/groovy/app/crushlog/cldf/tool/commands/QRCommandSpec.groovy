package app.crushlog.cldf.tool.commands

import spock.lang.Specification
import app.crushlog.cldf.tool.services.CLDFService
import app.crushlog.cldf.tool.utils.OutputHandler
import app.crushlog.cldf.api.CLDFArchive
import app.crushlog.cldf.models.Location
import app.crushlog.cldf.models.Route
import java.nio.file.Path
import java.nio.file.Paths

class QRCommandSpec extends Specification {

    def "QRCommand should display help"() {
        given:
        def command = new QRCommand()
        command.parent = Mock(BaseCommand) {
            getSpec() >> Mock(picocli.CommandLine.Model.CommandSpec) {
                commandLine() >> Mock(picocli.CommandLine) {
                    usage(_ as PrintStream) >> {}
                }
            }
        }

        when:
        def result = command.call()

        then:
        result == 0
    }

    def "GenerateCommand should generate QR for route"() {
        given:
        def cldfService = Mock(CLDFService)
        def outputHandler = Mock(OutputHandler)
        def command = new QRCommand.GenerateCommand(cldfService, outputHandler)
        
        def route = new Route().tap {
            clid = "clid:route:test-uuid"
            name = "Test Route"
        }
        
        def archive = Mock(CLDFArchive) {
            getRoutes() >> [route]
            getLocations() >> []
        }
        
        command.archivePath = Paths.get("test.cldf")
        command.clid = "clid:route:test-uuid"
        command.outputPath = Paths.get("test.png")
        command.baseUrl = "https://test.com"
        command.size = 256

        when:
        def result = command.call()

        then:
        1 * cldfService.read(_) >> archive
        1 * outputHandler.writeInfo(_ as String)
        result == 0
    }
    
    def "GenerateCommand should handle entity not found"() {
        given:
        def cldfService = Mock(CLDFService)
        def outputHandler = Mock(OutputHandler)
        def command = new QRCommand.GenerateCommand(cldfService, outputHandler)
        
        def archive = Mock(CLDFArchive) {
            getRoutes() >> []
            getLocations() >> []
        }
        
        command.archivePath = Paths.get("test.cldf")
        command.clid = "clid:route:nonexistent"
        command.outputPath = Paths.get("test.png")

        when:
        def result = command.call()

        then:
        1 * cldfService.read(_) >> archive
        1 * outputHandler.writeError("Entity not found with CLID: clid:route:nonexistent")
        result == 1
    }
    
    def "ScanCommand should display error for null outputHandler"() {
        given:
        def command = new QRCommand.ScanCommand()
        command.imagePath = Paths.get("test.png")

        when:
        def result = command.call()

        then:
        result == 1
    }
}