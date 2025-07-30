package io.cldf.api

import io.cldf.models.*
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Path
import java.time.LocalDate
import java.time.OffsetDateTime

import io.cldf.models.enums.ClimbType
import io.cldf.models.enums.FinishType
import io.cldf.models.enums.GradeSystem
import io.cldf.models.enums.RockType
import io.cldf.models.enums.TerrainType
import io.cldf.models.enums.RouteType
import io.cldf.models.enums.SessionType
import io.cldf.models.enums.PredefinedTagKey
import io.cldf.models.enums.Platform

class CLDFReaderWriterSpec extends Specification {

	@TempDir
	Path tempDir

	def "should write and read a CLDF archive"() {
		given: "a complete CLDF archive"
		def archive = createTestArchive()

		and: "a temporary file"
		def file = tempDir.resolve("test.cldf").toFile()

		when: "writing the archive"
		CLDF.write(archive, file)

		then: "file should exist"
		file.exists()
		file.length() > 0

		when: "reading the archive back"
		def readArchive = CLDF.read(file)

		then: "manifest should match"
		readArchive.manifest.version == archive.manifest.version
		readArchive.manifest.format == "CLDF"
		readArchive.manifest.appVersion == archive.manifest.appVersion
		readArchive.manifest.platform == archive.manifest.platform

		and: "locations should match"
		readArchive.locations.size() == 1
		readArchive.locations[0].name == "Test Crag"
		readArchive.locations[0].isIndoor == false

		and: "climbs should match"
		readArchive.climbs.size() == 1
		readArchive.climbs[0].routeName == "Test Route"
		readArchive.climbs[0].type == ClimbType.BOULDER

		and: "sessions should match"
		readArchive.sessions.size() == 1
		readArchive.sessions[0].location == "Test Crag"

		and: "checksums should be present"
		readArchive.checksums != null
		readArchive.checksums.algorithm == "SHA-256"
		readArchive.checksums.files.size() > 0
	}

	def "should handle optional fields correctly"() {
		given: "an archive with optional data"
		def archive = createTestArchive()
		archive.routes = [createTestRoute()]
		archive.tags = [createTestTag()]
		archive.sectors = [createTestSector()]

		and: "a temporary file"
		def file = tempDir.resolve("test-optional.cldf").toFile()

		when: "writing and reading the archive"
		CLDF.write(archive, file)
		def readArchive = CLDF.read(file)

		then: "optional data should be preserved"
		readArchive.hasRoutes()
		readArchive.routes.size() == 1
		readArchive.routes[0].name == "Test Boulder"

		and: "tags should be preserved"
		readArchive.hasTags()
		readArchive.tags.size() == 1
		readArchive.tags[0].name == "crimpy"

		and: "sectors should be preserved"
		readArchive.hasSectors()
		readArchive.sectors.size() == 1
		readArchive.sectors[0].name == "Main Area"
	}

	def "should validate required fields"() {
		given: "an incomplete archive"
		def archive = CLDFArchive.builder()
				.manifest(createTestManifest())
				.build()

		and: "a temporary file"
		def file = tempDir.resolve("invalid.cldf").toFile()

		when: "trying to write the archive"
		CLDF.write(archive, file)

		then: "should throw an exception"
		thrown(IllegalArgumentException)
	}

	def "should handle checksum validation"() {
		given: "a valid archive"
		def archive = createTestArchive()
		def file = tempDir.resolve("test-checksum.cldf").toFile()
		CLDF.write(archive, file)

		when: "reading with checksum validation enabled"
		def reader = CLDF.createReader(true, false)
		def readArchive = reader.read(file)

		then: "should succeed"
		readArchive != null
		readArchive.climbs.size() == 1
	}

	private CLDFArchive createTestArchive() {
		return CLDFArchive.builder()
				.manifest(createTestManifest())
				.locations([createTestLocation()])
				.climbs([createTestClimb()])
				.sessions([createTestSession()])
				.build()
	}

	private Manifest createTestManifest() {
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

	private Location createTestLocation() {
		return Location.builder()
				.id(1)
				.name("Test Crag")
				.isIndoor(false)
				.country("USA")
				.state("Colorado")
				.coordinates(Location.Coordinates.builder()
				.latitude(40.0)
				.longitude(-105.0)
				.build())
				.rockType(RockType.GRANITE)
				.terrainType(TerrainType.NATURAL)
				.build()
	}

	private Climb createTestClimb() {
		return Climb.builder()
				.id(1)
				.sessionId(1)
				.date(LocalDate.now())
				.routeName("Test Route")
				.type(ClimbType.BOULDER)
				.finishType(FinishType.TOP)
				.grades(Climb.GradeInfo.builder()
				.system(GradeSystem.V_SCALE)
				.grade("V5")
				.build())
				.attempts(1)
				.rating(4)
				.notes("Great climb!")
				.build()
	}

	private Session createTestSession() {
		return Session.builder()
				.id(1)
				.date(LocalDate.now())
				.location("Test Crag")
				.locationId(1)
				.isIndoor(false)
				.climbType(ClimbType.BOULDER)
				.sessionType(SessionType.BOULDERING)
				.notes("Good session")
				.build()
	}

	private Route createTestRoute() {
		return Route.builder()
				.id(1)
				.locationId(1)
				.name("Test Boulder")
				.routeType(RouteType.BOULDER)
				.grades(Route.Grades.builder()
				.vScale("V5")
				.build())
				.qualityRating(4)
				.beta("Start on crimps, big move to jug")
				.build()
	}

	private Tag createTestTag() {
		return Tag.builder()
				.id(1)
				.name("crimpy")
				.isPredefined(true)
				.predefinedTagKey(PredefinedTagKey.CRIMPY)
				.color("#FF0000")
				.category("holds")
				.build()
	}

	private Sector createTestSector() {
		return Sector.builder()
				.id(1)
				.locationId(1)
				.name("Main Area")
				.isDefault(true)
				.description("The main bouldering area")
				.coordinates(Sector.Coordinates.builder()
				.latitude(40.001)
				.longitude(-105.001)
				.build())
				.build()
	}
}
