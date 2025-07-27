package io.cldf.models

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import spock.lang.Specification

import java.time.LocalDate
import java.time.OffsetDateTime

class RouteSpec extends Specification {

	ObjectMapper objectMapper

	def setup() {
		objectMapper = new ObjectMapper()
		objectMapper.registerModule(new JavaTimeModule())
	}

	def "should build route with required fields"() {
		when: "creating a route"
		def route = Route.builder()
				.id("route-1")
				.locationId("location-1")
				.name("Test Route")
				.routeType(Route.RouteType.route)
				.build()

		then: "required fields are set"
		route.id == "route-1"
		route.locationId == "location-1"
		route.name == "Test Route"
		route.routeType == Route.RouteType.route
	}

	def "should build route with all fields"() {
		given: "date"
		def createdAt = OffsetDateTime.now()

		when: "creating a complete route"
		def route = Route.builder()
				.id("route-1")
				.locationId("location-1")
				.sectorId("sector-1")
				.name("Biographie")
				.routeType(Route.RouteType.route)
				.grades(Route.Grades.builder()
				.yds("5.15a")
				.french("9a+")
				.build())
				.qualityRating(5)
				.beta("Start with undercling, big move to crimp")
				.height(35)
				.firstAscent(Route.FirstAscent.builder()
				.name("Chris Sharma")
				.date(LocalDate.of(2001, 7, 18))
				.info("First ascent")
				.build())
				.tags([
					"overhang",
					"endurance",
					"crimpy"
				])
				.createdAt(createdAt)
				.build()

		then: "all fields are set"
		route.id == "route-1"
		route.locationId == "location-1"
		route.sectorId == "sector-1"
		route.name == "Biographie"
		route.routeType == Route.RouteType.route
		route.qualityRating == 5
		route.beta == "Start with undercling, big move to crimp"
		route.height == 35
		route.tags == [
			"overhang",
			"endurance",
			"crimpy"
		]
		route.createdAt == createdAt

		and: "grades are correct"
		route.grades.yds == "5.15a"
		route.grades.french == "9a+"

		and: "first ascent is correct"
		route.firstAscent.name == "Chris Sharma"
		route.firstAscent.date == LocalDate.of(2001, 7, 18)
		route.firstAscent.info == "First ascent"
	}

	def "should serialize route to JSON"() {
		given: "a route"
		def route = Route.builder()
				.id("route-1")
				.locationId("location-1")
				.name("Test Route")
				.routeType(Route.RouteType.boulder)
				.grades(Route.Grades.builder()
				.vScale("V10")
				.font("7C+")
				.build())
				.qualityRating(4)
				.build()

		when: "serializing to JSON"
		def json = objectMapper.writeValueAsString(route)

		then: "JSON contains expected fields"
		json.contains('"id":"route-1"')
		json.contains('"locationId":"location-1"')
		json.contains('"name":"Test Route"')
		json.contains('"routeType":"boulder"')
		json.contains('"grades"')
		json.contains('"vScale":"V10"')
		json.contains('"font":"7C+"')
		json.contains('"qualityRating":4')
	}

	def "should deserialize route from JSON"() {
		given: "JSON representation"
		def json = '''
            {
                "id": "route-1",
                "locationId": "location-1",
                "name": "The Mandala",
                "routeType": "boulder",
                "grades": {
                    "vScale": "V12",
                    "font": "8A+"
                },
                "qualityRating": 5,
                "firstAscent": {
                    "name": "Chris Sharma",
                    "date": "2000-02-01"
                },
                "tags": ["highball", "slopers"]
            }
        '''

		when: "deserializing from JSON"
		def route = objectMapper.readValue(json, Route)

		then: "route is created correctly"
		route.id == "route-1"
		route.locationId == "location-1"
		route.name == "The Mandala"
		route.routeType == Route.RouteType.boulder
		route.qualityRating == 5
		route.tags == ["highball", "slopers"]

		and: "grades are deserialized"
		route.grades != null
		route.grades.vScale == "V12"
		route.grades.font == "8A+"

		and: "first ascent is deserialized"
		route.firstAscent != null
		route.firstAscent.name == "Chris Sharma"
		route.firstAscent.date == LocalDate.of(2000, 2, 1)
	}

	def "should handle all route types"() {
		expect: "all route types are valid"
		Route.RouteType.values().size() == 2
		Route.RouteType.valueOf(type) != null

		where:
		type << ["boulder", "route"]
	}

	def "should build grades with multiple systems"() {
		when: "creating grades"
		def grades = Route.Grades.builder()
				.yds("5.12a")
				.french("7a+")
				.uiaa("IX-")
				.vScale("V4")
				.font("6C")
				.build()

		then: "all grades are set"
		grades.yds == "5.12a"
		grades.french == "7a+"
		grades.uiaa == "IX-"
		grades.vScale == "V4"
		grades.font == "6C"
	}

	def "should create RoutesFile with routes"() {
		given: "a list of routes"
		def routes = [
			Route.builder()
			.id("route-1")
			.locationId("loc-1")
			.name("Route 1")
			.routeType(Route.RouteType.route)
			.build(),
			Route.builder()
			.id("route-2")
			.locationId("loc-1")
			.name("Boulder 1")
			.routeType(Route.RouteType.boulder)
			.build()
		]

		when: "creating RoutesFile"
		def file = new RoutesFile()
		file.routes = routes

		then: "file contains routes"
		file.routes.size() == 2
		file.routes[0].name == "Route 1"
		file.routes[1].name == "Boulder 1"
	}
}
