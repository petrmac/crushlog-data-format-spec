package io.cldf.tool.commands

import io.cldf.api.CLDFArchive
import io.cldf.api.CLDFWriter
import io.cldf.models.MediaItem
import io.cldf.models.enums.MediaStrategy
import io.cldf.models.enums.MediaType
import io.cldf.models.enums.MediaSource
import io.cldf.tool.models.CommandResult
import io.cldf.tool.services.ValidationService
import io.cldf.tool.utils.OutputHandler
import io.cldf.tool.utils.OutputFormat
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.Path

class CreateCommandMediaSpec extends Specification {

    @TempDir
    Path tempDir

    def validationService = Mock(ValidationService)
    def command = new CreateCommand(validationService)

    def setup() {
        // Initialize the output handler
        command.output = new OutputHandler(OutputFormat.text, false)
    }

    def "should add media files when media directory is specified"() {
        given: "archive with existing climbs and media directory"
        def outputFile = tempDir.resolve("test-with-media.cldf").toFile()
        def mediaDir = tempDir.resolve("media")
        Files.createDirectories(mediaDir)
        
        // Create test media files named after climb IDs
        def photo1 = mediaDir.resolve("1_photo.jpg")
        def photo2 = mediaDir.resolve("2_photo.png")
        def video1 = mediaDir.resolve("3_video.mp4")
        Files.write(photo1, "fake photo content 1".bytes)
        Files.write(photo2, "fake photo content 2".bytes)
        Files.write(video1, "fake video content".bytes)
        
        // Create a non-media file that should be ignored
        def textFile = mediaDir.resolve("readme.txt")
        Files.write(textFile, "this should be ignored".bytes)
        
        // Create a JSON file with climbs to associate media with
        def jsonFile = tempDir.resolve("climbs.json").toFile()
        def climbsData = [
            manifest: [
                version: "1.0.0",
                format: "CLDF",
                creationDate: "2023-01-01T00:00:00Z",
                appVersion: "1.0.0",
                platform: "Desktop"
            ],
            locations: [
                [id: 1, name: "Test Crag", country: "US", isIndoor: false]
            ],
            routes: [
                [id: 1, locationId: 1, name: "Test Route", routeType: "route"]
            ],
            climbs: [
                [id: 1, date: "2023-01-01", type: "route", finishType: "redpoint", routeId: 1],
                [id: 2, date: "2023-01-02", type: "route", finishType: "flash", routeId: 1],
                [id: 3, date: "2023-01-03", type: "route", finishType: "onsight", routeId: 1]
            ]
        ]
        new com.fasterxml.jackson.databind.ObjectMapper()
            .writerWithDefaultPrettyPrinter()
            .writeValue(jsonFile, climbsData)

        validationService.validate(_) >> { archive ->
            // Verify media was added to archive
            assert archive.mediaItems != null
            assert archive.mediaItems.size() == 3
            assert archive.mediaFiles != null
            assert archive.mediaFiles.size() == 3
            
            io.cldf.tool.services.ValidationResult.builder()
                .valid(true)
                .warnings([])
                .build()
        }

        // Set command properties
        command.jsonInput = jsonFile.absolutePath
        command.outputFile = outputFile
        command.mediaDirectory = mediaDir.toFile()
        command.mediaStrategy = MediaStrategy.FULL

        when:
        def result = command.execute()

        then:
        result.success
        outputFile.exists()
        
        // Verify the archive contains media
        def archive = io.cldf.api.CLDF.read(outputFile)
        archive.mediaItems.size() == 3
        archive.mediaFiles.size() == 3
        
        // Check media metadata
        def photos = archive.mediaItems.findAll { it.type == MediaType.PHOTO }
        def videos = archive.mediaItems.findAll { it.type == MediaType.VIDEO }
        photos.size() == 2
        videos.size() == 1
        
        // Check all media items have correct properties
        archive.mediaItems.each { item ->
            assert item.source == MediaSource.LOCAL
            assert item.embedded == true
            assert item.filename != null
            assert item.climbId != null
        }
        
        // Check media files are stored with correct paths
        assert archive.mediaFiles.containsKey("media/1_photo.jpg")
        assert archive.mediaFiles.containsKey("media/2_photo.png")
        assert archive.mediaFiles.containsKey("media/3_video.mp4")
    }

    def "should support REFERENCE strategy without embedding files"() {
        given: "archive with existing climb and media directory"
        def outputFile = tempDir.resolve("test-reference-media.cldf").toFile()
        def mediaDir = tempDir.resolve("media")
        Files.createDirectories(mediaDir)
        
        def photo = mediaDir.resolve("1_photo.jpg")
        Files.write(photo, "photo content".bytes)
        
        // Create a JSON file with a climb
        def jsonFile = tempDir.resolve("climb.json").toFile()
        def climbData = [
            manifest: [
                version: "1.0.0",
                format: "CLDF",
                creationDate: "2023-01-01T00:00:00Z",
                appVersion: "1.0.0",
                platform: "Desktop"
            ],
            locations: [
                [id: 1, name: "Test Crag", country: "US", isIndoor: false]
            ],
            routes: [
                [id: 1, locationId: 1, name: "Test Route", routeType: "route"]
            ],
            climbs: [
                [id: 1, date: "2023-01-01", type: "route", finishType: "redpoint", routeId: 1]
            ]
        ]
        new com.fasterxml.jackson.databind.ObjectMapper()
            .writerWithDefaultPrettyPrinter()
            .writeValue(jsonFile, climbData)

        validationService.validate(_) >> { archive ->
            // With REFERENCE strategy, files should not be embedded
            assert archive.mediaItems != null
            assert archive.mediaItems.size() == 1
            assert archive.mediaFiles == null
            
            io.cldf.tool.services.ValidationResult.builder()
                .valid(true)
                .warnings([])
                .build()
        }

        // Set command properties
        command.jsonInput = jsonFile.absolutePath
        command.outputFile = outputFile
        command.mediaDirectory = mediaDir.toFile()
        command.mediaStrategy = MediaStrategy.REFERENCE

        when:
        def result = command.execute()

        then:
        result.success
        outputFile.exists()
        
        def archive = io.cldf.api.CLDF.read(outputFile)
        archive.mediaItems.size() == 1
        archive.mediaFiles == null
        archive.mediaItems[0].embedded == false
        archive.mediaItems[0].climbId == "1"
    }

    def "should handle subdirectories in media folder"() {
        given: "archive with climb and media in subdirectory"
        def outputFile = tempDir.resolve("test-subdirs.cldf").toFile()
        def mediaDir = tempDir.resolve("media")
        def subDir = mediaDir.resolve("climbing-photos")
        Files.createDirectories(subDir)
        
        def photo = subDir.resolve("1_send.jpg")
        Files.write(photo, "photo content".bytes)
        
        // Create a JSON file with a climb
        def jsonFile = tempDir.resolve("climb.json").toFile()
        def climbData = [
            manifest: [
                version: "1.0.0",
                format: "CLDF",
                creationDate: "2023-01-01T00:00:00Z",
                appVersion: "1.0.0",
                platform: "Desktop"
            ],
            locations: [
                [id: 1, name: "Test Crag", country: "US", isIndoor: false]
            ],
            routes: [
                [id: 1, locationId: 1, name: "Test Route", routeType: "route"]
            ],
            climbs: [
                [id: 1, date: "2023-01-01", type: "route", finishType: "redpoint", routeId: 1]
            ]
        ]
        new com.fasterxml.jackson.databind.ObjectMapper()
            .writerWithDefaultPrettyPrinter()
            .writeValue(jsonFile, climbData)

        validationService.validate(_) >> io.cldf.tool.services.ValidationResult.builder()
            .valid(true)
            .warnings([])
            .build()

        // Set command properties
        command.jsonInput = jsonFile.absolutePath
        command.outputFile = outputFile
        command.mediaDirectory = mediaDir.toFile()

        when:
        def result = command.execute()

        then:
        result.success
        
        def archive = io.cldf.api.CLDF.read(outputFile)
        archive.mediaItems.size() == 1
        archive.mediaFiles.containsKey("media/climbing-photos/1_send.jpg")
    }

    def "should fail gracefully when media directory doesn't exist"() {
        given:
        def outputFile = tempDir.resolve("test-no-media.cldf").toFile()
        def nonExistentDir = tempDir.resolve("does-not-exist")

        // Set command properties
        command.template = "basic"
        command.outputFile = outputFile
        command.mediaDirectory = nonExistentDir.toFile()

        when:
        def result = command.execute()

        then:
        !result.success
        result.exitCode == 1
        !outputFile.exists()
    }

    def "should handle empty media directory"() {
        given:
        def outputFile = tempDir.resolve("test-empty-media.cldf").toFile()
        def emptyMediaDir = tempDir.resolve("empty")
        Files.createDirectories(emptyMediaDir)

        validationService.validate(_) >> io.cldf.tool.services.ValidationResult.builder()
            .valid(true)
            .warnings([])
            .build()

        // Set command properties
        command.template = "basic"
        command.outputFile = outputFile
        command.mediaDirectory = emptyMediaDir.toFile()

        when:
        def result = command.execute()

        then:
        result.success
        outputFile.exists()
        
        def archive = io.cldf.api.CLDF.read(outputFile)
        archive.mediaItems == null
        archive.mediaFiles == null
    }

    private List<MediaItem> generateMediaItems(int count) {
        (1..count).collect { i ->
            MediaItem.builder()
                .id(i)
                .climbId("$i")
                .type(i % 2 == 0 ? MediaType.PHOTO : MediaType.VIDEO)
                .source(MediaSource.LOCAL)
                .filename("media${i}.jpg")
                .embedded(true)
                .build()
        }
    }
}