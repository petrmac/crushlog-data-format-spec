package io.cldf.api

import com.fasterxml.jackson.databind.ObjectMapper
import io.cldf.models.*
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class CLDFReaderSpec extends Specification {

	@TempDir
	Path tempDir

	CLDFReader reader
	ObjectMapper objectMapper

	def setup() {
		reader = new CLDFReader()
		objectMapper = new ObjectMapper()
		objectMapper.findAndRegisterModules()
		objectMapper.setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
		objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
	}

	def "should read valid CLDF archive"() {
		given: "a valid CLDF archive"
		def archiveFile = createValidArchive()

		when: "reading the archive"
		def archive = reader.read(archiveFile)

		then: "archive is read correctly"
		archive != null
		archive.manifest != null
		archive.manifest.version == "1.0.0"
		archive.locations != null
		archive.locations.size() == 1
		archive.climbs != null
		archive.climbs.size() == 1
		archive.sessions != null
		archive.sessions.size() == 1
	}

	def "should read archive from file"() {
		given: "a valid CLDF archive"
		def archiveFile = createValidArchive()

		when: "reading from file"
		def archive = reader.read(archiveFile)

		then: "archive is read correctly"
		archive != null
		archive.manifest.format == "CLDF"
	}

	def "should read archive with optional files"() {
		given: "an archive with optional files"
		def archiveFile = createArchiveWithOptionalFiles()

		when: "reading the archive"
		def archive = reader.read(archiveFile)

		then: "required files are read"
		archive.manifest != null
		archive.locations.size() == 1
		archive.climbs.size() == 1
		archive.sessions.size() == 1

		and: "optional files are read"
		archive.hasRoutes()
		archive.routes.size() == 1
		archive.hasTags()
		archive.tags.size() == 1
		archive.hasSectors()
		archive.sectors.size() == 1
	}

	def "should handle missing manifest"() {
		given: "an archive without manifest"
		def archiveFile = createArchiveWithoutManifest()

		when: "reading the archive"
		reader.read(archiveFile)

		then: "exception is thrown"
		def e = thrown(IOException)
		e.message.contains("manifest.json")
	}

	def "should handle invalid JSON format"() {
		given: "an archive with invalid JSON"
		def archiveFile = createArchiveWithInvalidJson()

		when: "reading the archive"
		reader.read(archiveFile)

		then: "exception is thrown"
		thrown(IOException)
	}

	def "should validate checksums when enabled"() {
		given: "a reader with checksum validation"
		def validatingReader = new CLDFReader(true, false)
		def archiveFile = createValidArchive()

		when: "reading with validation"
		def archive = validatingReader.read(archiveFile)

		then: "archive is read and validated"
		archive != null
		archive.checksums != null
	}

	def "should handle empty archive"() {
		given: "an empty zip file"
		def emptyArchive = tempDir.resolve("empty.cldf").toFile()
		new ZipOutputStream(new FileOutputStream(emptyArchive)).close()

		when: "reading empty archive"
		reader.read(emptyArchive)

		then: "exception is thrown"
		thrown(IOException)
	}

	def "should handle corrupted archive"() {
		given: "a corrupted file"
		def corruptedFile = tempDir.resolve("corrupted.cldf").toFile()
		corruptedFile.text = "This is not a valid zip file"

		when: "reading corrupted file"
		reader.read(corruptedFile)

		then: "exception is thrown"
		thrown(IOException)
	}

	def "should read archive with custom fields"() {
		given: "an archive with custom fields"
		def archiveFile = createArchiveWithCustomFields()

		when: "reading the archive"
		def archive = reader.read(archiveFile)

		then: "custom fields are preserved"
		archive.locations[0].customFields != null
		archive.locations[0].customFields["owner"] == "John Doe"
		archive.climbs[0].customFields != null
		archive.climbs[0].customFields["temperature"] == 72
	}

	private File createValidArchive() {
		def file = tempDir.resolve("valid.cldf").toFile()
		def zos = new ZipOutputStream(new FileOutputStream(file))

		try {
			addJsonEntry(zos, "manifest.json", createManifest())
			addJsonEntry(zos, "locations.json", createLocationsFile())
			addJsonEntry(zos, "climbs.json", createClimbsFile())
			addJsonEntry(zos, "sessions.json", createSessionsFile())
			addJsonEntry(zos, "checksums.json", createChecksums())
		} finally {
			zos.close()
		}

		return file
	}

	private File createArchiveWithOptionalFiles() {
		def file = tempDir.resolve("optional.cldf").toFile()
		def zos = new ZipOutputStream(new FileOutputStream(file))

		try {
			addJsonEntry(zos, "manifest.json", createManifest())
			addJsonEntry(zos, "locations.json", createLocationsFile())
			addJsonEntry(zos, "climbs.json", createClimbsFile())
			addJsonEntry(zos, "sessions.json", createSessionsFile())
			addJsonEntry(zos, "routes.json", createRoutesFile())
			addJsonEntry(zos, "tags.json", createTagsFile())
			addJsonEntry(zos, "sectors.json", createSectorsFile())
			addJsonEntry(zos, "checksums.json", createChecksums())
		} finally {
			zos.close()
		}

		return file
	}

	private File createArchiveWithoutManifest() {
		def file = tempDir.resolve("no-manifest.cldf").toFile()
		def zos = new ZipOutputStream(new FileOutputStream(file))

		try {
			addJsonEntry(zos, "locations.json", createLocationsFile())
			addJsonEntry(zos, "climbs.json", createClimbsFile())
			addJsonEntry(zos, "sessions.json", createSessionsFile())
		} finally {
			zos.close()
		}

		return file
	}

	private File createArchiveWithInvalidJson() {
		def file = tempDir.resolve("invalid-json.cldf").toFile()
		def zos = new ZipOutputStream(new FileOutputStream(file))

		try {
			zos.putNextEntry(new ZipEntry("manifest.json"))
			zos.write("{ invalid json }".bytes)
			zos.closeEntry()
		} finally {
			zos.close()
		}

		return file
	}

	private File createArchiveWithCustomFields() {
		def file = tempDir.resolve("custom-fields.cldf").toFile()
		def zos = new ZipOutputStream(new FileOutputStream(file))

		try {
			addJsonEntry(zos, "manifest.json", createManifest())

			def location = Location.builder()
					.id(1)
					.name("Custom Location")
					.isIndoor(false)
					.customFields([owner: "John Doe", yearEstablished: 2020])
					.build()
			def locationsFile = new LocationsFile()
			locationsFile.locations = [location]
			addJsonEntry(zos, "locations.json", locationsFile)

			def climb = Climb.builder()
					.id(1)
					.sessionId(1)
					.date(LocalDate.now())
					.routeName("Custom Route")
					.type(Climb.ClimbType.route)
					.finishType(Climb.FinishType.redpoint)
					.customFields([temperature: 72, humidity: 45])
					.build()
			def climbsFile = new ClimbsFile()
			climbsFile.climbs = [climb]
			addJsonEntry(zos, "climbs.json", climbsFile)

			addJsonEntry(zos, "sessions.json", createSessionsFile())
			addJsonEntry(zos, "checksums.json", createChecksums())
		} finally {
			zos.close()
		}

		return file
	}

	private void addJsonEntry(ZipOutputStream zos, String filename, Object data) {
		zos.putNextEntry(new ZipEntry(filename))
		zos.write(objectMapper.writeValueAsBytes(data))
		zos.closeEntry()
	}

	private Manifest createManifest() {
		return Manifest.builder()
				.version("1.0.0")
				.format("CLDF")
				.creationDate(OffsetDateTime.now())
				.appVersion("1.0")
				.platform(Manifest.Platform.Desktop)
				.stats(Manifest.Stats.builder()
				.climbsCount(1)
				.locationsCount(1)
				.sessionsCount(1)
				.build())
				.build()
	}

	private LocationsFile createLocationsFile() {
		def location = Location.builder()
				.id(1)
				.name("Test Location")
				.isIndoor(false)
				.build()
		def file = new LocationsFile()
		file.locations = [location]
		return file
	}

	private ClimbsFile createClimbsFile() {
		def climb = Climb.builder()
				.id(1)
				.sessionId(1)
				.date(LocalDate.now())
				.routeName("Test Route")
				.type(Climb.ClimbType.route)
				.finishType(Climb.FinishType.redpoint)
				.build()
		def file = new ClimbsFile()
		file.climbs = [climb]
		return file
	}

	private SessionsFile createSessionsFile() {
		def session = Session.builder()
				.id("1")
				.date(LocalDate.now())
				.location("Test Location")
				.locationId("1")
				.isIndoor(false)
				.build()
		def file = new SessionsFile()
		file.sessions = [session]
		return file
	}

	private RoutesFile createRoutesFile() {
		def route = Route.builder()
				.id("1")
				.locationId("1")
				.name("Test Route")
				.routeType(Route.RouteType.route)
				.build()
		def file = new RoutesFile()
		file.routes = [route]
		return file
	}

	private TagsFile createTagsFile() {
		def tag = Tag.builder()
				.id("1")
				.name("crimpy")
				.isPredefined(true)
				.build()
		def file = new TagsFile()
		file.tags = [tag]
		return file
	}

	private SectorsFile createSectorsFile() {
		def sector = Sector.builder()
				.id("1")
				.locationId("1")
				.name("Main Area")
				.build()
		def file = new SectorsFile()
		file.sectors = [sector]
		return file
	}

	private Checksums createChecksums() {
		return Checksums.builder()
				.algorithm("SHA-256")
				.files([:])
				.build()
	}
}
