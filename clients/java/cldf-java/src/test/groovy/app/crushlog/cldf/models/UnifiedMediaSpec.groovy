package app.crushlog.cldf.models

import spock.lang.Specification
import com.fasterxml.jackson.databind.ObjectMapper
import app.crushlog.cldf.models.media.Media
import app.crushlog.cldf.models.media.MediaItem
import app.crushlog.cldf.models.enums.*

class UnifiedMediaSpec extends Specification {

	def objectMapper = new ObjectMapper()

	def "Route should support media attachment"() {
		given:
		def media = Media.builder()
				.items([
					MediaItem.builder()
					.type(MediaType.PHOTO)
					.path("media/dawn_wall_topo.jpg")
					.designation(MediaDesignation.TOPO)
					.caption("Full route topo with pitch breakdown")
					.source(MediaSource.EMBEDDED)
					.build(),
					MediaItem.builder()
					.type(MediaType.VIDEO)
					.path("https://youtube.com/watch?v=example")
					.designation(MediaDesignation.BETA)
					.caption("Beta video from Tommy Caldwell")
					.source(MediaSource.EXTERNAL)
					.build()
				])
				.count(2)
				.build()

		when:
		def route = Route.builder()
				.id(101)
				.locationId(1)
				.name("The Dawn Wall")
				.routeType(RouteType.ROUTE)
				.media(media)
				.build()

		then:
		route.media != null
		route.media.count == 2
		route.media.items.size() == 2
		route.media.items[0].designation == MediaDesignation.TOPO
		route.media.items[1].designation == MediaDesignation.BETA
	}

	def "Route with media should serialize to JSON correctly"() {
		given:
		def route = Route.builder()
				.id(102)
				.locationId(1)
				.name("Test Route")
				.routeType(RouteType.BOULDER)
				.media(Media.builder()
				.items([
					MediaItem.builder()
					.type(MediaType.PHOTO)
					.path("media/boulder_topo.jpg")
					.designation(MediaDesignation.TOPO)
					.source(MediaSource.EMBEDDED)
					.build()
				])
				.count(1)
				.build())
				.build()

		when:
		def json = objectMapper.writeValueAsString(route)
		def result = objectMapper.readValue(json, Map.class)

		then:
		result.media != null
		result.media.count == 1
		result.media.items[0].designation == "topo"
		result.media.items[0].type == "photo"
	}

	def "Location should support media attachment"() {
		given:
		def media = Media.builder()
				.items([
					MediaItem.builder()
					.type(MediaType.PHOTO)
					.path("media/el_cap_overview.jpg")
					.designation(MediaDesignation.OVERVIEW)
					.caption("View from the valley floor")
					.source(MediaSource.EMBEDDED)
					.build(),
					MediaItem.builder()
					.type(MediaType.PHOTO)
					.path("media/el_cap_approach.jpg")
					.designation(MediaDesignation.APPROACH)
					.caption("Approach trail map")
					.source(MediaSource.EMBEDDED)
					.metadata([
						coordinates: [
							latitude: 37.7340,
							longitude: -119.6378
						]
					])
					.build()
				])
				.count(2)
				.build()

		when:
		def location = Location.builder()
				.id(10)
				.name("El Capitan")
				.isIndoor(false)
				.media(media)
				.build()

		then:
		location.media != null
		location.media.count == 2
		location.media.items[0].designation == MediaDesignation.OVERVIEW
		location.media.items[1].designation == MediaDesignation.APPROACH
		location.media.items[1].metadata.coordinates.latitude == 37.7340
	}

	def "Location with media should serialize and deserialize correctly"() {
		given:
		def location = Location.builder()
				.id(11)
				.name("Test Crag")
				.isIndoor(false)
				.media(Media.builder()
				.items([
					MediaItem.builder()
					.type(MediaType.PHOTO)
					.path("media/crag_overview.jpg")
					.designation(MediaDesignation.OVERVIEW)
					.caption("Main wall overview")
					.timestamp("2024-01-15T10:00:00Z")
					.source(MediaSource.EMBEDDED)
					.build()
				])
				.count(1)
				.build())
				.build()

		when:
		def json = objectMapper.writeValueAsString(location)
		def result = objectMapper.readValue(json, Location.class)

		then:
		result.media != null
		result.media.count == 1
		result.media.items[0].designation == MediaDesignation.OVERVIEW
		result.media.items[0].caption == "Main wall overview"
		result.media.items[0].timestamp == "2024-01-15T10:00:00Z"
	}

	def "Sector should support media attachment"() {
		given:
		def media = Media.builder()
				.items([
					MediaItem.builder()
					.type(MediaType.PHOTO)
					.path("media/sector_map.jpg")
					.designation(MediaDesignation.OVERVIEW)
					.caption("Sector map with route locations")
					.source(MediaSource.EMBEDDED)
					.build()
				])
				.count(1)
				.build()

		when:
		def sector = Sector.builder()
				.id(20)
				.locationId(10)
				.name("Main Wall")
				.media(media)
				.build()

		then:
		sector.media != null
		sector.media.count == 1
		sector.media.items[0].designation == MediaDesignation.OVERVIEW
		sector.media.items[0].caption == "Sector map with route locations"
	}

	def "should filter media by designation"() {
		given:
		def items = [
			MediaItem.builder()
			.type(MediaType.PHOTO)
			.path("topo1.jpg")
			.designation(MediaDesignation.TOPO)
			.build(),
			MediaItem.builder()
			.type(MediaType.VIDEO)
			.path("beta1.mp4")
			.designation(MediaDesignation.BETA)
			.build(),
			MediaItem.builder()
			.type(MediaType.PHOTO)
			.path("topo2.jpg")
			.designation(MediaDesignation.TOPO)
			.build(),
			MediaItem.builder()
			.type(MediaType.PHOTO)
			.path("overview.jpg")
			.designation(MediaDesignation.OVERVIEW)
			.build()
		]

		when:
		def topos = items.findAll { it.designation == MediaDesignation.TOPO }
		def betas = items.findAll { it.designation == MediaDesignation.BETA }
		def overviews = items.findAll { it.designation == MediaDesignation.OVERVIEW }

		then:
		topos.size() == 2
		topos[0].path == "topo1.jpg"
		topos[1].path == "topo2.jpg"

		betas.size() == 1
		betas[0].path == "beta1.mp4"

		overviews.size() == 1
		overviews[0].path == "overview.jpg"
	}

	def "should handle complex media scenarios"() {
		given:
		def route = Route.builder()
				.id(103)
				.locationId(1)
				.name("Complex Route")
				.routeType(RouteType.ROUTE)
				.media(Media.builder()
				.items([
					MediaItem.builder()
					.type(MediaType.PHOTO)
					.path("media/route_topo.jpg")
					.designation(MediaDesignation.TOPO)
					.caption("Main route topo")
					.source(MediaSource.EMBEDDED)
					.build(),
					MediaItem.builder()
					.type(MediaType.VIDEO)
					.path("https://vimeo.com/123456")
					.designation(MediaDesignation.BETA)
					.caption("Crux beta")
					.source(MediaSource.EXTERNAL)
					.build(),
					MediaItem.builder()
					.type(MediaType.PHOTO)
					.path("media/gear_placement.jpg")
					.designation(MediaDesignation.GEAR)
					.caption("Key gear placements")
					.source(MediaSource.EMBEDDED)
					.build(),
					MediaItem.builder()
					.type(MediaType.PHOTO)
					.path("media/descent.jpg")
					.designation(MediaDesignation.DESCENT)
					.caption("Rappel stations")
					.source(MediaSource.EMBEDDED)
					.build()
				])
				.count(4)
				.build())
				.build()

		when:
		def json = objectMapper.writeValueAsString(route)
		def deserialized = objectMapper.readValue(json, Route.class)

		then:
		deserialized.media.items.size() == 4
		deserialized.media.items.findAll { it.designation == MediaDesignation.TOPO }.size() == 1
		deserialized.media.items.findAll { it.designation == MediaDesignation.BETA }.size() == 1
		deserialized.media.items.findAll { it.designation == MediaDesignation.GEAR }.size() == 1
		deserialized.media.items.findAll { it.designation == MediaDesignation.DESCENT }.size() == 1
		deserialized.media.items.findAll { it.source == MediaSource.EXTERNAL }.size() == 1
		deserialized.media.items.findAll { it.source == MediaSource.EMBEDDED }.size() == 3
	}

	def "should handle null media gracefully"() {
		given:
		def route = Route.builder()
				.id(104)
				.locationId(1)
				.name("Route without media")
				.routeType(RouteType.ROUTE)
				.media(null)
				.build()

		when:
		def json = objectMapper.writeValueAsString(route)
		def result = objectMapper.readValue(json, Route.class)

		then:
		result.media == null
	}
}
