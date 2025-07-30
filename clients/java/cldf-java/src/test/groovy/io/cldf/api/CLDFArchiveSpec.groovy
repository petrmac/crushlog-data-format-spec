package io.cldf.api

import io.cldf.models.*
import spock.lang.Specification

import java.time.LocalDate
import java.time.OffsetDateTime

import io.cldf.models.enums.ClimbType
import io.cldf.models.enums.FinishType
import io.cldf.models.enums.GradeSystem
import io.cldf.models.enums.RockType
import io.cldf.models.enums.TerrainType
import io.cldf.models.enums.RouteType
import io.cldf.models.enums.SessionType
import io.cldf.models.enums.MediaType
import io.cldf.models.enums.Platform

class CLDFArchiveSpec extends Specification {

	def "should build archive with required fields"() {
		when: "creating archive with minimum required data"
		def archive = CLDFArchive.builder()
				.manifest(createManifest())
				.locations(createLocations())
				.climbs(createClimbs())
				.sessions(createSessions())
				.build()

		then: "archive has required fields"
		archive.manifest != null
		archive.locations != null
		archive.locations.size() == 1
		archive.climbs != null
		archive.climbs.size() == 1
		archive.sessions != null
		archive.sessions.size() == 1

		and: "optional fields are null"
		!archive.hasRoutes()
		!archive.hasTags()
		!archive.hasSectors()
		!archive.hasMedia()
	}

	def "should build archive with all fields"() {
		when: "creating complete archive"
		def archive = CLDFArchive.builder()
				.manifest(createManifest())
				.locations(createLocations())
				.climbs(createClimbs())
				.sessions(createSessions())
				.routes(createRoutes())
				.tags(createTags())
				.sectors(createSectors())
				.mediaItems(createMediaMetadata())
				.checksums(createChecksums())
				.build()

		then: "all fields are present"
		archive.manifest != null
		archive.locations.size() == 1
		archive.climbs.size() == 1
		archive.sessions.size() == 1
		archive.hasRoutes()
		archive.routes.size() == 1
		archive.hasTags()
		archive.tags.size() == 1
		archive.hasSectors()
		archive.sectors.size() == 1
		archive.hasMedia()
		archive.mediaItems.size() == 1
		archive.checksums != null
	}

	def "should validate manifest is required"() {
		when: "creating archive without manifest"
		CLDFArchive.builder()
				.locations(createLocations())
				.climbs(createClimbs())
				.sessions(createSessions())
				.build()

		then: "build succeeds but manifest is null"
		noExceptionThrown()
	}

	def "should validate locations are required"() {
		when: "creating archive without locations"
		CLDFArchive.builder()
				.manifest(createManifest())
				.climbs(createClimbs())
				.sessions(createSessions())
				.build()

		then: "build succeeds but locations is null"
		noExceptionThrown()
	}

	def "should validate climbs are required"() {
		when: "creating archive without climbs"
		CLDFArchive.builder()
				.manifest(createManifest())
				.locations(createLocations())
				.sessions(createSessions())
				.build()

		then: "build succeeds but climbs is null"
		noExceptionThrown()
	}

	def "should validate sessions are required"() {
		when: "creating archive without sessions"
		CLDFArchive.builder()
				.manifest(createManifest())
				.locations(createLocations())
				.climbs(createClimbs())
				.build()

		then: "build succeeds but sessions is null"
		noExceptionThrown()
	}

	def "should handle empty lists"() {
		when: "creating archive with empty lists"
		def archive = CLDFArchive.builder()
				.manifest(createManifest())
				.locations([])
				.climbs([])
				.sessions([])
				.routes([])
				.tags([])
				.sectors([])
				.mediaItems([])
				.build()

		then: "lists are empty but not null"
		archive.locations != null
		archive.locations.isEmpty()
		archive.climbs != null
		archive.climbs.isEmpty()
		archive.sessions != null
		archive.sessions.isEmpty()
		!archive.hasRoutes()
		!archive.hasTags()
		!archive.hasSectors()
		!archive.hasMedia()
	}

	def "should check optional data presence correctly"() {
		given: "archives with different optional data"
		def emptyArchive = CLDFArchive.builder()
				.manifest(createManifest())
				.locations(createLocations())
				.climbs(createClimbs())
				.sessions(createSessions())
				.build()

		def archiveWithRoutes = CLDFArchive.builder()
				.manifest(createManifest())
				.locations(createLocations())
				.climbs(createClimbs())
				.sessions(createSessions())
				.routes(createRoutes())
				.build()

		def archiveWithEmptyRoutes = CLDFArchive.builder()
				.manifest(createManifest())
				.locations(createLocations())
				.climbs(createClimbs())
				.sessions(createSessions())
				.routes([])
				.build()

		expect: "presence checks work correctly"
		!emptyArchive.hasRoutes()
		archiveWithRoutes.hasRoutes()
		!archiveWithEmptyRoutes.hasRoutes()
	}

	def "should create valid archive for real-world scenario"() {
		given: "a climbing trip data"
		def tripDate = LocalDate.of(2024, 6, 15)
		def creationDate = OffsetDateTime.now()

		when: "creating archive for trip"
		def archive = CLDFArchive.builder()
				.manifest(Manifest.builder()
				.version("1.0.0")
				.format("CLDF")
				.creationDate(creationDate)
				.appVersion("2.5.0")
				.platform(Platform.IOS)
				.stats(Manifest.Stats.builder()
				.climbsCount(5)
				.locationsCount(1)
				.sessionsCount(1)
				.routesCount(3)
				.build())
				.build())
				.locations([
					Location.builder()
					.id(1)
					.name("Bishop - Buttermilks")
					.isIndoor(false)
					.country("USA")
					.state("California")
					.coordinates(Location.Coordinates.builder()
					.latitude(37.4167)
					.longitude(-118.5833)
					.build())
					.rockType(RockType.GRANITE)
					.terrainType(TerrainType.NATURAL)
					.build()
				])
				.sessions([
					Session.builder()
					.id(1)
					.date(tripDate)
					.location("Bishop - Buttermilks")
					.locationId(1)
					.isIndoor(false)
					.sessionType(SessionType.BOULDERING)
					.weather(Session.Weather.builder()
					.temperature(68.0)
					.conditions("sunny")
					.build())
					.notes("Perfect conditions!")
					.build()
				])
				.climbs([
					Climb.builder()
					.id(1)
					.sessionId(1)
					.date(tripDate)
					.routeName("High Plains Drifter")
					.routeId(1)
					.type(ClimbType.BOULDER)
					.finishType(FinishType.FLASH)
					.grades(Climb.GradeInfo.builder()
					.system(GradeSystem.V_SCALE)
					.grade("V7")
					.build())
					.attempts(1)
					.rating(5)
					.build(),
					Climb.builder()
					.id(2)
					.sessionId(1)
					.date(tripDate)
					.routeName("Cave Route")
					.routeId(2)
					.type(ClimbType.BOULDER)
					.finishType(FinishType.REDPOINT)
					.grades(Climb.GradeInfo.builder()
					.system(GradeSystem.V_SCALE)
					.grade("V6")
					.build())
					.attempts(3)
					.rating(4)
					.notes("Tricky heel hook")
					.build()
				])
				.routes([
					Route.builder()
					.id(1)
					.locationId(1)
					.name("High Plains Drifter")
					.routeType(RouteType.BOULDER)
					.grades(Route.Grades.builder()
					.vScale("V7")
					.build())
					.qualityRating(5)
					.build(),
					Route.builder()
					.id(2)
					.locationId(1)
					.name("Cave Route")
					.routeType(RouteType.BOULDER)
					.grades(Route.Grades.builder()
					.vScale("V6")
					.build())
					.qualityRating(4)
					.beta("Start with heel hook")
					.build()
				])
				.build()

		then: "archive represents complete trip data"
		archive.manifest.stats.climbsCount == 5
		archive.locations[0].name == "Bishop - Buttermilks"
		archive.sessions[0].weather.temperature == 68
		archive.climbs.size() == 2
		archive.climbs[0].finishType == FinishType.FLASH
		archive.climbs[1].notes == "Tricky heel hook"
		archive.routes.size() == 2
	}

	private Manifest createManifest() {
		return Manifest.builder()
				.version("1.0.0")
				.format("CLDF")
				.creationDate(OffsetDateTime.now())
				.appVersion("1.0")
				.platform(Platform.DESKTOP)
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
			.type(ClimbType.ROUTE)
			.finishType(FinishType.REDPOINT)
			.build()
		]
	}

	private List<Session> createSessions() {
		return [
			Session.builder()
			.id(1)
			.date(LocalDate.now())
			.location("Test Location")
			.locationId(1)
			.isIndoor(false)
			.build()
		]
	}

	private List<Route> createRoutes() {
		return [
			Route.builder()
			.id(1)
			.locationId(1)
			.name("Test Route")
			.routeType(RouteType.ROUTE)
			.build()
		]
	}

	private List<Tag> createTags() {
		return [
			Tag.builder()
			.id(1)
			.name("crimpy")
			.isPredefined(true)
			.build()
		]
	}

	private List<Sector> createSectors() {
		return [
			Sector.builder()
			.id(1)
			.locationId(1)
			.name("Main Area")
			.build()
		]
	}

	private List<MediaItem> createMediaMetadata() {
		return [
			MediaItem.builder()
			.id(1)
			.climbId("1")
			.filename("test.jpg")
			.type(MediaType.PHOTO)
			.createdAt(OffsetDateTime.now())
			.build()
		]
	}

	private Checksums createChecksums() {
		return Checksums.builder()
				.algorithm("SHA-256")
				.files([:])
				.build()
	}
}
