package app.crushlog.cldf.models

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import spock.lang.Specification
import spock.lang.Subject

import java.time.OffsetDateTime

import app.crushlog.cldf.models.enums.RockType
import app.crushlog.cldf.models.enums.TerrainType

class LocationSpec extends Specification {

	ObjectMapper objectMapper

	def setup() {
		objectMapper = new ObjectMapper()
		objectMapper.registerModule(new JavaTimeModule())
	}

	def "should build location with all required fields"() {
		when: "creating a location"
		def location = Location.builder()
				.id(1)
				.name("Test Crag")
				.isIndoor(false)
				.build()

		then: "required fields are set correctly"
		location.id == 1
		location.name == "Test Crag"
		!location.isIndoor

		and: "default values are applied"
		!location.starred
	}

	def "should build location with all fields"() {
		given: "current timestamp"
		def now = OffsetDateTime.now()

		and: "custom fields"
		def customFields = [owner: "John Doe", yearEstablished: 2020]

		when: "creating a complete location"
		def location = Location.builder()
				.id(1)
				.name("Test Crag")
				.isIndoor(false)
				.coordinates(Location.Coordinates.builder()
				.latitude(40.0)
				.longitude(-105.0)
				.build())
				.country("USA")
				.state("Colorado")
				.rockType(RockType.GRANITE)
				.terrainType(TerrainType.NATURAL)
				.accessInfo("Park at the main lot")
				.createdAt(now)
				.customFields(customFields)
				.build()

		then: "all fields are set correctly"
		location.id == 1
		location.name == "Test Crag"
		!location.isIndoor
		location.country == "USA"
		location.state == "Colorado"
		location.rockType == RockType.GRANITE
		location.terrainType == TerrainType.NATURAL
		location.accessInfo == "Park at the main lot"
		location.createdAt == now
		location.customFields == customFields

		and: "coordinates are set correctly"
		location.coordinates.latitude == 40.0
		location.coordinates.longitude == -105.0
	}

	def "should serialize location to JSON"() {
		given: "a location with coordinates"
		def location = Location.builder()
				.id(1)
				.name("Test Gym")
				.isIndoor(true)
				.coordinates(Location.Coordinates.builder()
				.latitude(40.0)
				.longitude(-105.0)
				.build())
				.rockType(RockType.GRANITE)
				.build()

		when: "serializing to JSON"
		def json = objectMapper.writeValueAsString(location)

		then: "JSON contains expected fields"
		json.contains('"id":1')
		json.contains('"name":"Test Gym"')
		json.contains('"isIndoor":true')
		json.contains('"latitude":40.0')
		json.contains('"longitude":-105.0')
		json.contains('"starred":false')
		json.contains('"rockType":"granite"')
	}

	def "should deserialize location from JSON"() {
		given: "JSON representation"
		def json = '''
            {
                "id": 1,
                "name": "Test Crag",
                "isIndoor": false,
                "coordinates": {
                    "latitude": 40.0,
                    "longitude": -105.0
                },
                "country": "USA",
                "rockType": "granite",
                "terrainType": "natural"
            }
        '''

		when: "deserializing from JSON"
		def location = objectMapper.readValue(json, Location)

		then: "location is created correctly"
		location.id == 1
		location.name == "Test Crag"
		!location.isIndoor
		location.country == "USA"
		location.rockType == RockType.GRANITE
		location.terrainType == TerrainType.NATURAL

		and: "coordinates are deserialized"
		location.coordinates != null
		location.coordinates.latitude == 40.0
		location.coordinates.longitude == -105.0
	}

	def "should handle all rock types"() {
		expect: "all rock types are valid"
		RockType.values().size() == 15
		RockType.fromValue(rockType) != null

		where:
		rockType << [
			"sandstone",
			"limestone",
			"granite",
			"basalt",
			"gneiss",
			"quartzite",
			"conglomerate",
			"schist",
			"dolomite",
			"slate",
			"rhyolite",
			"gabbro",
			"volcanicTuff",
			"andesite",
			"chalk"
		]
	}

	def "should handle all terrain types"() {
		expect: "all terrain types are valid"
		TerrainType.values().size() == 2
		TerrainType.fromValue(terrainType) != null

		where:
		terrainType << ["natural", "artificial"]
	}

	def "should create LocationsFile with locations"() {
		given: "a list of locations"
		def locations = [
			Location.builder().id(1).name("Crag 1").isIndoor(false).build(),
			Location.builder().id(2).name("Gym 1").isIndoor(true).build()
		]

		when: "creating LocationsFile"
		def file = new LocationsFile()
		file.locations = locations

		then: "file contains locations"
		file.locations.size() == 2
		file.locations[0].name == "Crag 1"
		file.locations[1].name == "Gym 1"
	}
}
