package io.cldf.api

import com.fasterxml.jackson.databind.ObjectMapper
import io.cldf.models.*
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Path
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.util.zip.ZipFile

class CLDFWriterSpec extends Specification {

	@TempDir
	Path tempDir

	CLDFWriter writer
	ObjectMapper objectMapper

	def setup() {
		writer = new CLDFWriter()
		objectMapper = new ObjectMapper()
		objectMapper.findAndRegisterModules()
	}

	def "should write valid CLDF archive"() {
		given: "a complete archive"
		def archive = createCompleteArchive()
		def outputFile = tempDir.resolve("output.cldf").toFile()

		when: "writing the archive"
		writer.write(archive, outputFile)

		then: "file is created"
		outputFile.exists()
		outputFile.length() > 0

		and: "archive contains required files"
		def zip = new ZipFile(outputFile)
		zip.getEntry("manifest.json") != null
		zip.getEntry("locations.json") != null
		zip.getEntry("climbs.json") != null
		zip.getEntry("sessions.json") != null
		zip.getEntry("checksums.json") != null
		zip.close()
	}

	def "should write archive to file"() {
		given: "a complete archive"
		def archive = createCompleteArchive()
		def outputFile = tempDir.resolve("output-file.cldf").toFile()

		when: "writing to file"
		writer.write(archive, outputFile)

		then: "file is created"
		outputFile.exists()
		outputFile.length() > 0
	}

	def "should write optional files when present"() {
		given: "an archive with optional data"
		def archive = createArchiveWithOptionalData()
		def outputFile = tempDir.resolve("optional.cldf").toFile()

		when: "writing the archive"
		writer.write(archive, outputFile)

		then: "file contains optional data"
		def zip = new ZipFile(outputFile)
		zip.getEntry("routes.json") != null
		zip.getEntry("tags.json") != null
		zip.getEntry("sectors.json") != null
		zip.getEntry("media-metadata.json") != null
		zip.close()
	}

	def "should validate required files"() {
		given: "an incomplete archive"
		def archive = CLDFArchive.builder()
				.manifest(createManifest())
				.build()
		def outputFile = tempDir.resolve("incomplete.cldf").toFile()

		when: "writing incomplete archive"
		writer.write(archive, outputFile)

		then: "exception is thrown"
		def e = thrown(IllegalArgumentException)
		e.message.contains("required")
	}

	def "should generate checksums"() {
		given: "an archive without checksums"
		def archive = createCompleteArchive()
		archive.checksums = null
		def outputFile = tempDir.resolve("checksums.cldf").toFile()

		when: "writing the archive"
		writer.write(archive, outputFile)

		then: "checksums are generated"
		def zip = new ZipFile(outputFile)
		def checksumEntry = zip.getEntry("checksums.json")
		checksumEntry != null

		and: "checksums file is valid"
		def checksumData = objectMapper.readValue(
				zip.getInputStream(checksumEntry),
				Checksums.class
				)
		checksumData.algorithm == "SHA-256"
		checksumData.files.size() > 0
		zip.close()
	}

	def "should preserve custom fields"() {
		given: "an archive with custom fields"
		def archive = createArchiveWithCustomFields()
		def outputFile = tempDir.resolve("custom.cldf").toFile()

		when: "writing and reading back"
		writer.write(archive, outputFile)
		def reader = new CLDFReader()
		def readArchive = reader.read(outputFile)

		then: "custom fields are preserved"
		readArchive.locations[0].customFields["owner"] == "John Doe"
		readArchive.climbs[0].customFields["temperature"] == 72
	}

	def "should handle large archives"() {
		given: "a large archive"
		def archive = createLargeArchive(100, 1000)
		def outputFile = tempDir.resolve("large.cldf").toFile()

		when: "writing large archive"
		writer.write(archive, outputFile)

		then: "file is created successfully"
		outputFile.exists()
		outputFile.length() > 8000

		and: "can be read back"
		def reader = new CLDFReader()
		def readArchive = reader.read(outputFile)
		readArchive.locations.size() == 100
		readArchive.climbs.size() == 1000
	}

	def "should handle IO errors"() {
		given: "an invalid output path"
		def archive = createCompleteArchive()
		def invalidFile = new File("/invalid/path/test.cldf")

		when: "writing to invalid path"
		writer.write(archive, invalidFile)

		then: "exception is thrown"
		thrown(IOException)
	}

	def "should accept any manifest format"() {
		given: "an archive with custom manifest format"
		def archive = createCompleteArchive()
		archive.manifest.format = "CUSTOM"
		def outputFile = tempDir.resolve("custom-format.cldf").toFile()
		def writerWithoutValidation = new CLDFWriter(true, false)

		when: "writing archive"
		writerWithoutValidation.write(archive, outputFile)

		then: "file is created successfully"
		outputFile.exists()
		outputFile.length() > 0
	}

	def "should include media files when present"() {
		given: "an archive with media references"
		def archive = createArchiveWithMedia()
		def outputFile = tempDir.resolve("media.cldf").toFile()

		// Add actual media files to temp directory
		def mediaDir = tempDir.resolve("media")
		mediaDir.toFile().mkdirs()
		def photo1 = mediaDir.resolve("photo1.jpg").toFile()
		photo1.text = "fake photo data"

		when: "writing archive with media"
		writer.write(archive, outputFile)

		then: "archive contains media metadata"
		def zip = new ZipFile(outputFile)
		zip.getEntry("media-metadata.json") != null
		zip.close()
	}

	private CLDFArchive createCompleteArchive() {
		return CLDFArchive.builder()
				.manifest(createManifest())
				.locations(createLocations())
				.climbs(createClimbs())
				.sessions(createSessions())
				.build()
	}

	private CLDFArchive createArchiveWithOptionalData() {
		def archive = createCompleteArchive()
		archive.routes = createRoutes()
		archive.tags = createTags()
		archive.sectors = createSectors()
		archive.mediaItems = createMediaMetadata()
		return archive
	}

	private CLDFArchive createArchiveWithCustomFields() {
		def location = Location.builder()
				.id(1)
				.name("Custom Location")
				.isIndoor(false)
				.customFields([owner: "John Doe", yearEstablished: 2020])
				.build()

		def climb = Climb.builder()
				.id(1)
				.sessionId(1)
				.date(LocalDate.now())
				.routeName("Custom Route")
				.type(Climb.ClimbType.route)
				.finishType("redpoint")
				.customFields([temperature: 72, humidity: 45])
				.build()

		return CLDFArchive.builder()
				.manifest(createManifest())
				.locations([location])
				.climbs([climb])
				.sessions(createSessions())
				.build()
	}

	private CLDFArchive createArchiveWithMedia() {
		def climb = Climb.builder()
				.id(1)
				.sessionId(1)
				.date(LocalDate.now())
				.routeName("Photo Route")
				.type(Climb.ClimbType.route)
				.finishType("redpoint")
				.media(Climb.Media.builder()
				.photos(["photo1.jpg", "photo2.jpg"])
				.build())
				.build()

		def mediaItem = MediaItem.builder()
				.id("media-1")
				.climbId("1")
				.filename("photo1.jpg")
				.type(MediaItem.MediaType.photo)
				.createdAt(OffsetDateTime.now())
				.metadata(MediaItem.Metadata.builder()
				.width(1920)
				.height(1080)
				.size(1024000)
				.build())
				.build()

		return CLDFArchive.builder()
				.manifest(createManifest())
				.locations(createLocations())
				.climbs([climb])
				.sessions(createSessions())
				.mediaItems([mediaItem])
				.build()
	}

	private CLDFArchive createLargeArchive(int locationCount, int climbCount) {
		def locations = (1..locationCount).collect { i ->
			Location.builder()
					.id(i)
					.name("Location $i")
					.isIndoor(i % 2 == 0)
					.build()
		}

		def climbs = (1..climbCount).collect { i ->
			Climb.builder()
					.id(i)
					.sessionId(1)
					.date(LocalDate.now())
					.routeName("Route $i")
					.type(Climb.ClimbType.route)
					.finishType("redpoint")
					.build()
		}

		def manifest = Manifest.builder()
				.version("1.0.0")
				.format("CLDF")
				.creationDate(OffsetDateTime.now())
				.appVersion("1.0")
				.platform(Manifest.Platform.Desktop)
				.stats(Manifest.Stats.builder()
				.locationsCount(locationCount)
				.climbsCount(climbCount)
				.sessionsCount(1)
				.build())
				.build()

		return CLDFArchive.builder()
				.manifest(manifest)
				.locations(locations)
				.climbs(climbs)
				.sessions(createSessions())
				.build()
	}

	private Manifest createManifest() {
		return Manifest.builder()
				.version("1.0.0")
				.format("CLDF")
				.creationDate(OffsetDateTime.now())
				.appVersion("1.0")
				.platform(Manifest.Platform.Desktop)
				.build()
	}

	private List<Location> createLocations() {
		return [
			Location.builder()
			.id(1)
			.name("Test Location")
			.isIndoor(false)
			.build()
		]
	}

	private List<Climb> createClimbs() {
		return [
			Climb.builder()
			.id(1)
			.sessionId(1)
			.date(LocalDate.now())
			.routeName("Test Route")
			.type(Climb.ClimbType.route)
			.finishType("redpoint")
			.build()
		]
	}

	private List<Session> createSessions() {
		return [
			Session.builder()
			.id("1")
			.date(LocalDate.now())
			.location("Test Location")
			.locationId("1")
			.isIndoor(false)
			.build()
		]
	}

	private List<Route> createRoutes() {
		return [
			Route.builder()
			.id("1")
			.locationId("1")
			.name("Test Route")
			.routeType(Route.RouteType.route)
			.build()
		]
	}

	private List<Tag> createTags() {
		return [
			Tag.builder()
			.id("1")
			.name("crimpy")
			.isPredefined(true)
			.build()
		]
	}

	private List<Sector> createSectors() {
		return [
			Sector.builder()
			.id("1")
			.locationId("1")
			.name("Main Area")
			.build()
		]
	}

	private List<MediaItem> createMediaMetadata() {
		return [
			MediaItem.builder()
			.id("1")
			.climbId("1")
			.filename("test.jpg")
			.type(MediaItem.MediaType.photo)
			.createdAt(OffsetDateTime.now())
			.build()
		]
	}
}
