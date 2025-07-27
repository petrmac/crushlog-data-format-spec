package io.cldf.api

import io.cldf.models.*
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Path
import java.time.LocalDate
import java.time.OffsetDateTime

class CLDFSpec extends Specification {

	@TempDir
	Path tempDir

	def "should create reader with custom settings"() {
		when: "creating a reader with validation enabled"
		def reader = CLDF.createReader(true, false)

		then: "reader is created"
		reader != null
		reader instanceof CLDFReader
	}

	def "should create reader with validation disabled"() {
		when: "creating a reader with validation disabled"
		def reader = CLDF.createReader(false, false)

		then: "reader is created"
		reader != null
		reader instanceof CLDFReader
	}

	def "should create writer with pretty print"() {
		when: "creating a writer with pretty print"
		def writer = CLDF.createWriter(true)

		then: "writer is created"
		writer != null
		writer instanceof CLDFWriter
	}

	def "should create writer without pretty print"() {
		when: "creating a writer without pretty print"
		def writer = CLDF.createWriter(false)

		then: "writer is created"
		writer != null
		writer instanceof CLDFWriter
	}

	def "should write and read archive using static methods"() {
		given: "a test archive"
		def archive = createMinimalArchive()
		def file = tempDir.resolve("test.cldf").toFile()

		when: "writing archive"
		CLDF.write(archive, file)

		then: "file is created"
		file.exists()
		file.length() > 0

		when: "reading archive"
		def readArchive = CLDF.read(file)

		then: "archive is read correctly"
		readArchive != null
		readArchive.manifest.version == "1.0.0"
		readArchive.locations.size() == 1
		readArchive.climbs.size() == 1
		readArchive.sessions.size() == 1
	}

	def "should read archive from file"() {
		given: "a test archive written to file"
		def archive = createMinimalArchive()
		def file = tempDir.resolve("test.cldf").toFile()
		CLDF.write(archive, file)

		when: "reading from file"
		def readArchive = CLDF.read(file)

		then: "archive is read correctly"
		readArchive != null
		readArchive.manifest.format == "CLDF"
	}

	def "should handle write errors gracefully"() {
		given: "an invalid path"
		def archive = createMinimalArchive()
		def invalidFile = new File("/invalid/path/test.cldf")

		when: "trying to write to invalid path"
		CLDF.write(archive, invalidFile)

		then: "exception is thrown"
		thrown(IOException)
	}

	def "should handle read errors gracefully"() {
		given: "a non-existent file"
		def nonExistentFile = tempDir.resolve("nonexistent.cldf").toFile()

		when: "trying to read non-existent file"
		CLDF.read(nonExistentFile)

		then: "exception is thrown"
		thrown(IOException)
	}

	def "should validate archive has required files"() {
		given: "an incomplete archive"
		def archive = CLDFArchive.builder()
				.manifest(createManifest())
				.locations([])
				.climbs([])
				.build()

		when: "writing archive"
		def file = tempDir.resolve("incomplete.cldf").toFile()
		CLDF.write(archive, file)

		then: "exception is thrown"
		thrown(IllegalArgumentException)
	}

	private CLDFArchive createMinimalArchive() {
		return CLDFArchive.builder()
				.manifest(createManifest())
				.locations([createLocation()])
				.climbs([createClimb()])
				.sessions([createSession()])
				.build()
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

	private Location createLocation() {
		return Location.builder()
				.id(1)
				.name("Test Location")
				.isIndoor(false)
				.build()
	}

	private Climb createClimb() {
		return Climb.builder()
				.id(1)
				.sessionId(1)
				.date(LocalDate.now())
				.routeName("Test Route")
				.type(Climb.ClimbType.route)
				.finishType("redpoint")
				.build()
	}

	private Session createSession() {
		return Session.builder()
				.id("1")
				.date(LocalDate.now())
				.location("Test Location")
				.locationId("1")
				.isIndoor(false)
				.build()
	}
}
