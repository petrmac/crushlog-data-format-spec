package app.crushlog.cldf.models

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import spock.lang.Specification

import java.time.OffsetDateTime

class SectorSpec extends Specification {

	ObjectMapper objectMapper

	def setup() {
		objectMapper = new ObjectMapper()
		objectMapper.registerModule(new JavaTimeModule())
	}

	def "should build sector with required fields"() {
		when: "creating a sector"
		def sector = Sector.builder()
				.id(1)
				.locationId(1)
				.name("Main Boulder Field")
				.build()

		then: "required fields are set"
		sector.id == 1
		sector.locationId == 1
		sector.name == "Main Boulder Field"

		and: "defaults are applied"
		!sector.isDefault
	}

	def "should build sector with all fields"() {
		given: "timestamps"
		def createdAt = OffsetDateTime.now()

		when: "creating a complete sector"
		def sector = Sector.builder()
				.id(1)
				.locationId(1)
				.name("Warm Up Area")
				.isDefault(true)
				.description("Easy access area with V0-V5 problems")
				.coordinates(Sector.Coordinates.builder()
				.latitude(40.0150)
				.longitude(-105.2705)
				.build())
				.approach("Follow main trail for 200m")
				.createdAt(createdAt)
				.build()

		then: "all fields are set"
		sector.id == 1
		sector.locationId == 1
		sector.name == "Warm Up Area"
		sector.isDefault
		sector.description == "Easy access area with V0-V5 problems"
		sector.approach == "Follow main trail for 200m"
		sector.createdAt == createdAt

		and: "coordinates are set"
		sector.coordinates.latitude == 40.0150
		sector.coordinates.longitude == -105.2705
	}

	def "should serialize sector to JSON"() {
		given: "a sector"
		def sector = Sector.builder()
				.id(1)
				.locationId(1)
				.name("North Face")
				.isDefault(false)
				.description("Shady area, good for summer")
				.coordinates(Sector.Coordinates.builder()
				.latitude(37.4167)
				.longitude(-118.5833)
				.build())
				.build()

		when: "serializing to JSON"
		def json = objectMapper.writeValueAsString(sector)

		then: "JSON contains expected fields"
		json.contains('"id":1')
		json.contains('"locationId":1')
		json.contains('"name":"North Face"')
		json.contains('"isDefault":false')
		json.contains('"description":"Shady area, good for summer"')
		json.contains('"latitude":37.4167')
		json.contains('"longitude":-118.5833')
	}

	def "should deserialize sector from JSON"() {
		given: "JSON representation"
		def json = '''
            {
                "id": 1,
                "locationId": 1,
                "name": "Cave Area",
                "isDefault": true,
                "description": "Protected from rain",
                "coordinates": {
                    "latitude": 40.0,
                    "longitude": -105.0
                }
            }
        '''

		when: "deserializing from JSON"
		def sector = objectMapper.readValue(json, Sector)

		then: "sector is created correctly"
		sector.id == 1
		sector.locationId == 1
		sector.name == "Cave Area"
		sector.isDefault
		sector.description == "Protected from rain"

		and: "coordinates are deserialized"
		sector.coordinates != null
		sector.coordinates.latitude == 40.0
		sector.coordinates.longitude == -105.0
	}

	def "should handle multiple sectors per location"() {
		when: "creating multiple sectors for one location"
		def sectors = [
			Sector.builder()
			.id(1)
			.locationId(1)
			.name("Lower Tier")
			.isDefault(true)
			.description("Main area with easy access")
			.build(),
			Sector.builder()
			.id(2)
			.locationId(1)
			.name("Upper Tier")
			.isDefault(false)
			.description("Requires 15 min approach")
			.build(),
			Sector.builder()
			.id(3)
			.locationId(1)
			.name("Secret Garden")
			.isDefault(false)
			.description("Hidden area with classic problems")
			.build()
		]

		then: "only one is default"
		sectors.count { it.isDefault } == 1

		and: "all belong to same location"
		sectors.every { it.locationId == 1 }

		and: "names are unique"
		sectors.collect { it.name }.unique().size() == 3
	}

	def "should create SectorsFile with sectors"() {
		given: "a list of sectors"
		def sectors = [
			Sector.builder()
			.id(1)
			.locationId(1)
			.name("Main Area")
			.isDefault(true)
			.build(),
			Sector.builder()
			.id(2)
			.locationId(1)
			.name("Back Area")
			.build()
		]

		when: "creating SectorsFile"
		def file = new SectorsFile()
		file.sectors = sectors

		then: "file contains sectors"
		file.sectors != null
		file.sectors.size() == 2
		file.sectors[0].name == "Main Area"
		file.sectors[1].name == "Back Area"
	}
}
