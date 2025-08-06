package io.cldf.api

import io.cldf.models.*
import io.cldf.models.enums.*
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Path
import java.time.LocalDate
import java.time.OffsetDateTime

class CLDFWriterStatsSpec extends Specification {

	@TempDir
	Path tempDir

	def "should automatically calculate stats when not provided"() {
		given: "A CLDF archive without stats"
		def manifest = Manifest.builder()
				.version("1.0.0")
				.format("CLDF")
				.creationDate(OffsetDateTime.now())
				.appVersion("1.0.0")
				.platform(Platform.DESKTOP)
				.build()

		def locations = [
			Location.builder()
			.id(1)
			.name("Test Gym")
			.isIndoor(true)
			.build(),
			Location.builder()
			.id(2)
			.name("Outdoor Crag")
			.isIndoor(false)
			.build()
		]

		def sectors = [
			Sector.builder()
			.id(1)
			.locationId(2)
			.name("Main Wall")
			.build()
		]

		def routes = [
			Route.builder()
			.id(1)
			.locationId(2)
			.sectorId(1)
			.name("Test Route")
			.routeType(RouteType.BOULDER)
			.build(),
			Route.builder()
			.id(2)
			.locationId(2)
			.sectorId(1)
			.name("Another Route")
			.routeType(RouteType.ROUTE)
			.build()
		]

		def sessions = [
			Session.builder()
			.id(1)
			.date(LocalDate.now())
			.location("Test Gym")
			.locationId(1)
			.isIndoor(true)
			.build()
		]

		def climbs = [
			Climb.builder()
			.id(1)
			.sessionId(1)
			.date(LocalDate.now())
			.routeName("Boulder Problem")
			.type(ClimbType.BOULDER)
			.finishType(FinishType.TOP)
			.attempts(3)
			.grades(Climb.GradeInfo.builder()
			.system(GradeSystem.V_SCALE)
			.grade("V4")
			.build())
			.isIndoor(true)
			.build(),
			Climb.builder()
			.id(2)
			.sessionId(1)
			.date(LocalDate.now())
			.routeName("Another Problem")
			.type(ClimbType.BOULDER)
			.finishType(FinishType.FLASH)
			.attempts(1)
			.grades(Climb.GradeInfo.builder()
			.system(GradeSystem.V_SCALE)
			.grade("V3")
			.build())
			.isIndoor(true)
			.build()
		]

		def tags = [
			Tag.builder()
			.id(1)
			.name("project")
			.color("#FF5733")
			.isPredefined(true)
			.build()
		]

		def mediaItems = [
			MediaItem.builder()
			.id(1)
			.climbId(1)
			.type(MediaType.PHOTO)
			.source(MediaSource.LOCAL)
			.filename("climb1.jpg")
			.build(),
			MediaItem.builder()
			.id(2)
			.climbId(2)
			.type(MediaType.VIDEO)
			.source(MediaSource.LOCAL)
			.filename("climb2.mp4")
			.build()
		]

		def archive = CLDFArchive.builder()
				.manifest(manifest)
				.locations(locations)
				.sectors(sectors)
				.routes(routes)
				.sessions(sessions)
				.climbs(climbs)
				.tags(tags)
				.mediaItems(mediaItems)
				.build()

		def outputFile = tempDir.resolve("test_stats.cldf").toFile()
		def writer = new CLDFWriter()

		when: "Writing the archive"
		writer.write(archive, outputFile)

		and: "Reading it back"
		def reader = new CLDFReader()
		def readArchive = reader.read(outputFile)

		then: "Stats should be automatically calculated"
		readArchive.manifest.stats != null
		readArchive.manifest.stats.locationsCount == 2
		readArchive.manifest.stats.sectorsCount == 1
		readArchive.manifest.stats.routesCount == 2
		readArchive.manifest.stats.sessionsCount == 1
		readArchive.manifest.stats.climbsCount == 2
		readArchive.manifest.stats.tagsCount == 1
		readArchive.manifest.stats.mediaCount == 2
	}

	def "should preserve existing stats when provided"() {
		given: "A CLDF archive with custom stats"
		def customStats = Manifest.Stats.builder()
				.locationsCount(100)
				.sectorsCount(50)
				.routesCount(200)
				.sessionsCount(10)
				.climbsCount(500)
				.tagsCount(20)
				.mediaCount(1000)
				.build()

		def manifest = Manifest.builder()
				.version("1.0.0")
				.format("CLDF")
				.creationDate(OffsetDateTime.now())
				.appVersion("2.0.0")
				.platform(Platform.ANDROID)
				.stats(customStats)
				.build()

		def locations = [
			Location.builder()
			.id(1)
			.name("Test Location")
			.isIndoor(true)
			.build()
		]

		def archive = CLDFArchive.builder()
				.manifest(manifest)
				.locations(locations)
				.build()

		def outputFile = tempDir.resolve("test_preserve_stats.cldf").toFile()
		def writer = new CLDFWriter()

		when: "Writing the archive"
		writer.write(archive, outputFile)

		and: "Reading it back"
		def reader = new CLDFReader()
		def readArchive = reader.read(outputFile)

		then: "Custom stats should be preserved"
		readArchive.manifest.stats != null
		readArchive.manifest.stats.locationsCount == 100
		readArchive.manifest.stats.sectorsCount == 50
		readArchive.manifest.stats.routesCount == 200
		readArchive.manifest.stats.sessionsCount == 10
		readArchive.manifest.stats.climbsCount == 500
		readArchive.manifest.stats.tagsCount == 20
		readArchive.manifest.stats.mediaCount == 1000
	}
}
