package io.cldf.tool.services

import io.cldf.api.CLDFArchive
import io.cldf.api.CLDFWriter
import io.cldf.models.*
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Files
import java.time.LocalDate
import java.time.OffsetDateTime

import io.cldf.models.enums.ClimbType
import io.cldf.models.enums.FinishType
import io.cldf.models.enums.Platform

class CLDFServiceSpec extends Specification {

    CLDFService cldfService
    
    @TempDir
    File tempDir
    
    def setup() {
        cldfService = new DefaultCLDFService()
    }

    def "should read CLDF archive from file"() {
        given: "a valid CLDF file"
        def cldfFile = new File(tempDir, "test.cldf")
        def archive = createValidArchive()
        new CLDFWriter(false).write(archive, cldfFile)

        when: "reading the file"
        def readArchive = cldfService.read(cldfFile)

        then: "archive is read correctly"
        readArchive != null
        readArchive.manifest != null
        readArchive.manifest.version == "1.0.0"
        readArchive.locations.size() == 1
        readArchive.sessions.size() == 1
        readArchive.climbs.size() == 1
    }

    def "should write CLDF archive to file without pretty print"() {
        given: "a CLDF archive"
        def archive = createValidArchive()
        def outputFile = new File(tempDir, "output.cldf")

        when: "writing the archive"
        cldfService.write(archive, outputFile, false)

        then: "file is created"
        outputFile.exists()
        outputFile.length() > 0
        
        and: "can be read back"
        def readArchive = cldfService.read(outputFile)
        readArchive.manifest.version == archive.manifest.version
    }

    def "should write CLDF archive to file with pretty print"() {
        given: "a CLDF archive"
        def archive = createValidArchive()
        def outputFile = new File(tempDir, "output-pretty.cldf")

        when: "writing the archive with pretty print"
        cldfService.write(archive, outputFile, true)

        then: "file is created"
        outputFile.exists()
        outputFile.length() > 0
        
        and: "can be read back"
        def readArchive = cldfService.read(outputFile)
        readArchive.manifest.version == archive.manifest.version
        
        and: "file size is larger due to formatting"
        def compactFile = new File(tempDir, "output-compact.cldf")
        cldfService.write(archive, compactFile, false)
        outputFile.length() > compactFile.length()
    }

    def "should handle IOException when reading non-existent file"() {
        given: "a non-existent file"
        def nonExistentFile = new File(tempDir, "does-not-exist.cldf")

        when: "trying to read the file"
        cldfService.read(nonExistentFile)

        then: "IOException is thrown"
        thrown(IOException)
    }

    def "should handle IOException when writing to invalid location"() {
        given: "an archive and an invalid output path"
        def archive = createValidArchive()
        def invalidFile = new File("/non/existent/path/output.cldf")

        when: "trying to write to invalid location"
        cldfService.write(archive, invalidFile, false)

        then: "IOException is thrown"
        thrown(IOException)
    }

    def "should handle reading corrupted CLDF file"() {
        given: "a corrupted CLDF file"
        def corruptedFile = new File(tempDir, "corrupted.cldf")
        Files.write(corruptedFile.toPath(), "not a valid zip file".bytes)

        when: "trying to read the corrupted file"
        cldfService.read(corruptedFile)

        then: "IOException is thrown"
        thrown(IOException)
    }

    def "should write and read complex archive correctly"() {
        given: "a complex CLDF archive"
        def archive = createComplexArchive()
        def outputFile = new File(tempDir, "complex.cldf")

        when: "writing and reading back"
        cldfService.write(archive, outputFile, true)
        def readArchive = cldfService.read(outputFile)

        then: "all data is preserved"
        readArchive.manifest.version == archive.manifest.version
        readArchive.locations.size() == 2
        readArchive.sessions.size() == 3
        readArchive.climbs.size() == 5
        readArchive.climbs.find { it.routeName == "The Classic" } != null
        readArchive.climbs.find { it.type == ClimbType.ROUTE } != null
    }

    // Helper methods
    
    private CLDFArchive createValidArchive() {
        return CLDFArchive.builder()
            .manifest(createValidManifest())
            .locations([createValidLocation()])
            .sessions([createValidSession()])
            .climbs([createValidClimb()])
            .build()
    }
    
    private CLDFArchive createComplexArchive() {
        def locations = [
            Location.builder()
                .id(1)
                .name("Outdoor Crag")
                .isIndoor(false)
                .build(),
            Location.builder()
                .id(2)
                .name("Indoor Gym")
                .isIndoor(true)
                .build()
        ]
        
        def sessions = [
            Session.builder()
                .id(1)
                .date(LocalDate.now().minusDays(10))
                .location("Outdoor Crag")
                .locationId(1)
                .isIndoor(false)
                .build(),
            Session.builder()
                .id(2)
                .date(LocalDate.now().minusDays(5))
                .location("Indoor Gym")
                .locationId(2)
                .isIndoor(true)
                .build(),
            Session.builder()
                .id(3)
                .date(LocalDate.now())
                .location("Outdoor Crag")
                .locationId(1)
                .isIndoor(false)
                .build()
        ]
        
        def climbs = [
            Climb.builder()
                .id(1)
                .sessionId(1)
                .date(LocalDate.now().minusDays(10))
                .routeName("The Classic")
                .type(ClimbType.BOULDER)
                .finishType(FinishType.FLASH)
                .build(),
            Climb.builder()
                .id(2)
                .sessionId(1)
                .date(LocalDate.now().minusDays(10))
                .routeName("Hard Boulder")
                .type(ClimbType.BOULDER)
                .finishType(FinishType.PROJECT)
                .build(),
            Climb.builder()
                .id(3)
                .sessionId(2)
                .date(LocalDate.now().minusDays(5))
                .routeName("Gym Route 1")
                .type(ClimbType.ROUTE)
                .finishType(FinishType.ONSIGHT)
                .build(),
            Climb.builder()
                .id(4)
                .sessionId(2)
                .date(LocalDate.now().minusDays(5))
                .routeName("Gym Route 2")
                .type(ClimbType.ROUTE)
                .finishType(FinishType.FLASH)
                .build(),
            Climb.builder()
                .id(5)
                .sessionId(3)
                .date(LocalDate.now())
                .routeName("New Beta")
                .type(ClimbType.BOULDER)
                .finishType(FinishType.TOP)
                .build()
        ]
        
        return CLDFArchive.builder()
            .manifest(Manifest.builder()
                .version("1.0.0")
                .format("CLDF")
                .creationDate(OffsetDateTime.now())
                .appVersion("2.0")
                .platform(Platform.IOS)
                .stats(Manifest.Stats.builder()
                    .climbsCount(5)
                    .locationsCount(2)
                    .sessionsCount(3)
                    .build())
                .build())
            .locations(locations)
            .sessions(sessions)
            .climbs(climbs)
            .build()
    }
    
    private Manifest createValidManifest() {
        return Manifest.builder()
            .version("1.0.0")
            .format("CLDF")
            .creationDate(OffsetDateTime.now())
            .appVersion("1.0")
            .platform(Platform.DESKTOP)
            .stats(Manifest.Stats.builder()
                .climbsCount(1)
                .locationsCount(1)
                .sessionsCount(1)
                .build())
            .build()
    }
    
    private Location createValidLocation() {
        return Location.builder()
            .id(1)
            .name("Test Crag")
            .isIndoor(false)
            .build()
    }
    
    private Session createValidSession() {
        return Session.builder()
            .id(1)
            .date(LocalDate.now())
            .location("Test Crag")
            .locationId(1)
            .isIndoor(false)
            .build()
    }
    
    private Climb createValidClimb() {
        return Climb.builder()
            .id(1)
            .sessionId(1)
            .date(LocalDate.now())
            .routeName("Test Route")
            .type(ClimbType.BOULDER)
            .finishType(FinishType.TOP)
            .build()
    }
}
