package io.cldf.api

import spock.lang.Specification
import io.cldf.models.*
import io.cldf.models.media.*
import io.cldf.models.enums.*
import java.nio.file.Files
import java.time.LocalDate
import java.time.OffsetDateTime

class MediaIntegrationSpec extends Specification {

	def "should create and read CLDF archive with unified media model"() {
		given: "A complete CLDF archive with media on all entity types"
		def tempFile = Files.createTempFile("test-media", ".cldf")

		// Create location with media
		def location = Location.builder()
				.id(1)
				.name("Yosemite Valley")
				.isIndoor(false)
				.country("USA")
				.media(Media.builder()
				.items([
					MediaItem.builder()
					.type(MediaType.PHOTO)
					.path("media/yosemite_overview.jpg")
					.designation(MediaDesignation.OVERVIEW)
					.caption("Valley overview from Tunnel View")
					.source(MediaSource.EMBEDDED)
					.build()
				])
				.count(1)
				.build())
				.build()

		// Create sector with media
		def sector = Sector.builder()
				.id(1)
				.locationId(1)
				.name("El Capitan")
				.media(Media.builder()
				.items([
					MediaItem.builder()
					.type(MediaType.PHOTO)
					.path("media/el_cap_approach.jpg")
					.designation(MediaDesignation.APPROACH)
					.caption("Approach trail from parking")
					.source(MediaSource.EMBEDDED)
					.build()
				])
				.count(1)
				.build())
				.build()

		// Create route with multiple media items
		def route = Route.builder()
				.id(1)
				.locationId(1)
				.sectorId(1)
				.name("The Nose")
				.routeType(RouteType.ROUTE)
				.grades(Route.Grades.builder()
				.yds("5.9")
				.build())
				.media(Media.builder()
				.items([
					MediaItem.builder()
					.type(MediaType.PHOTO)
					.path("media/nose_topo.jpg")
					.designation(MediaDesignation.TOPO)
					.caption("Complete route topo")
					.source(MediaSource.EMBEDDED)
					.build(),
					MediaItem.builder()
					.type(MediaType.VIDEO)
					.path("https://youtube.com/watch?v=nose_beta")
					.designation(MediaDesignation.BETA)
					.caption("Alex Honnold beta video")
					.source(MediaSource.EXTERNAL)
					.build(),
					MediaItem.builder()
					.type(MediaType.PHOTO)
					.path("media/nose_gear.jpg")
					.designation(MediaDesignation.GEAR)
					.caption("Essential rack for The Nose")
					.source(MediaSource.EMBEDDED)
					.build()
				])
				.count(3)
				.build())
				.build()

		// Create climb with media
		def climb = Climb.builder()
				.id(1)
				.date(LocalDate.of(2024, 1, 15))
				.type(ClimbType.ROUTE)
				.finishType(FinishType.REDPOINT)
				.routeId(1)
				.routeName("The Nose")
				.attempts(2)
				.media(Media.builder()
				.items([
					MediaItem.builder()
					.type(MediaType.PHOTO)
					.path("media/nose_send.jpg")
					.designation(MediaDesignation.LOG)
					.caption("Summit photo after sending!")
					.timestamp("2024-01-15T16:30:00Z")
					.source(MediaSource.EMBEDDED)
					.build()
				])
				.count(1)
				.build())
				.build()

		// Create session
		def session = Session.builder()
				.id(1)
				.date(LocalDate.of(2024, 1, 15))
				.sessionType(SessionType.SPORT_CLIMBING)
				.locationId(1)
				.location("Yosemite Valley")
				.build()

		// Create manifest
		def manifest = Manifest.builder()
				.version("1.2.3")
				.format("CLDF")
				.creationDate(OffsetDateTime.now())
				.appVersion("2.0.0")
				.platform(Platform.DESKTOP)
				.stats(Manifest.Stats.builder()
				.climbsCount(1)
				.sessionsCount(1)
				.locationsCount(1)
				.routesCount(1)
				.sectorsCount(1)
				.mediaCount(6) // Total media items across all entities
				.build())
				.build()

		// Create archive
		def archive = CLDFArchive.builder()
				.manifest(manifest)
				.locations([location])
				.sectors([sector])
				.routes([route])
				.sessions([session])
				.climbs([climb])
				.build()

		when: "Writing and reading the archive"
		CLDF.write(archive, tempFile.toFile())
		def readArchive = CLDF.read(tempFile.toFile())

		then: "All media is correctly preserved"
		// Location media
		readArchive.locations[0].media != null
		readArchive.locations[0].media.count == 1
		readArchive.locations[0].media.items[0].designation == MediaDesignation.OVERVIEW
		readArchive.locations[0].media.items[0].caption == "Valley overview from Tunnel View"

		// Sector media
		readArchive.sectors[0].media != null
		readArchive.sectors[0].media.count == 1
		readArchive.sectors[0].media.items[0].designation == MediaDesignation.APPROACH

		// Route media
		readArchive.routes[0].media != null
		readArchive.routes[0].media.count == 3
		def routeMediaTypes = readArchive.routes[0].media.items.collect { it.designation }
		routeMediaTypes.contains(MediaDesignation.TOPO)
		routeMediaTypes.contains(MediaDesignation.BETA)
		routeMediaTypes.contains(MediaDesignation.GEAR)

		// Check external vs embedded sources
		def externalMedia = readArchive.routes[0].media.items.find { it.source == MediaSource.EXTERNAL }
		externalMedia != null
		externalMedia.path.startsWith("https://")

		// Climb media
		readArchive.climbs[0].media != null
		readArchive.climbs[0].media.count == 1
		readArchive.climbs[0].media.items[0].designation == MediaDesignation.LOG
		readArchive.climbs[0].media.items[0].timestamp == "2024-01-15T16:30:00Z"

		cleanup:
		Files.deleteIfExists(tempFile)
	}

	def "should handle entities without media gracefully"() {
		given: "Entities without media"
		def tempFile = Files.createTempFile("test-no-media", ".cldf")

		def location = Location.builder()
				.id(1)
				.name("Test Location")
				.isIndoor(true)
				.build()

		def route = Route.builder()
				.id(1)
				.locationId(1)
				.name("Test Route")
				.routeType(RouteType.BOULDER)
				.build()

		def manifest = Manifest.builder()
				.version("1.2.3")
				.format("CLDF")
				.creationDate(OffsetDateTime.now())
				.appVersion("1.0.0")
				.platform(Platform.IOS)
				.build()

		def archive = CLDFArchive.builder()
				.manifest(manifest)
				.locations([location])
				.routes([route])
				.build()

		when:
		CLDF.write(archive, tempFile.toFile())
		def readArchive = CLDF.read(tempFile.toFile())

		then:
		readArchive.locations[0].media == null
		readArchive.routes[0].media == null

		cleanup:
		Files.deleteIfExists(tempFile)
	}
}
