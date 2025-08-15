package app.crushlog.cldf.tool.commands

import picocli.CommandLine
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
        command.spec = Mock(CommandLine.Model.CommandSpec) {
            commandLine() >> Mock(CommandLine) {
                usage(_ as PrintStream) >> {}
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
        def command = new QRCommand.GenerateCommand(cldfService)
        
        def route = new Route().tap {
            clid = "clid:v1:route:test-uuid"
            name = "Test Route"
        }
        
        def archive = Mock(CLDFArchive) {
            getRoutes() >> [route]
            getLocations() >> []
        }
        
        command.archivePath = Paths.get("test.cldf")
        command.archiveClid = "clid:v1:route:test-uuid"
        command.outputPath = Paths.get("test.png")
        command.baseUrl = "https://test.com"
        command.format = "json"
        command.size = 256

        when:
        def result = command.call()

        then:
        1 * cldfService.read(_) >> archive
        result == 0
    }
    
    def "GenerateCommand should handle entity not found"() {
        given:
        def cldfService = Mock(CLDFService)
        def command = new QRCommand.GenerateCommand(cldfService)
        
        def archive = Mock(CLDFArchive) {
            getRoutes() >> []
            getLocations() >> []
        }
        
        command.archivePath = Paths.get("test.cldf")
        command.archiveClid = "clid:v1:route:nonexistent"
        command.outputPath = Paths.get("test.png")
        command.format = "json"

        when:
        def result = command.call()

        then:
        1 * cldfService.read(_) >> archive
        result == 1
    }
    
    def "ScanCommand should handle missing image file"() {
        given:
        def command = new QRCommand.ScanCommand()
        command.imagePath = Paths.get("nonexistent.png")

        when:
        def result = command.call()

        then:
        result == 1
    }
}