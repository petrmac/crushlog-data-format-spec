package io.cldf.models

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import spock.lang.Specification

import java.time.LocalDate
import java.time.LocalTime

import io.cldf.models.enums.ClimbType
import io.cldf.models.enums.BelayType
import io.cldf.models.enums.FinishType
import io.cldf.models.enums.GradeSystem
import io.cldf.models.enums.RockType
import io.cldf.models.enums.TerrainType

class ClimbSpec extends Specification {

	ObjectMapper objectMapper

	def setup() {
		objectMapper = new ObjectMapper()
		objectMapper.registerModule(new JavaTimeModule())
		objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
	}

	def "should build climb with required fields"() {
		given: "current date"
		def date = LocalDate.now()

		when: "creating a climb"
		def climb = Climb.builder()
				.id(1)
				.date(date)
				.routeName("Midnight Lightning")
				.type(ClimbType.BOULDER)
				.finishType(FinishType.FLASH)
				.build()

		then: "required fields are set"
		climb.id == 1
		climb.date == date
		climb.routeName == "Midnight Lightning"
		climb.type == ClimbType.BOULDER
		climb.finishType == FinishType.FLASH

		and: "defaults are applied"
		climb.attempts == 1
		climb.repeats == 0
		!climb.isRepeat
	}

	def "should build climb with all fields"() {
		given: "dates and custom data"
		def date = LocalDate.now()
		def time = LocalTime.of(14, 30)
		def customFields = [temperature: 72, humidity: 65]

		when: "creating a complete climb"
		def climb = Climb.builder()
				.id(1)
				.sessionId(1)
				.routeId("123")
				.date(date)
				.time(time)
				.routeName("The Nose")
				.type(ClimbType.ROUTE)
				.finishType(FinishType.REDPOINT)
				.grades(Climb.GradeInfo.builder()
				.system(GradeSystem.YDS)
				.grade("5.14a")
				.build())
				.attempts(3)
				.repeats(0)
				.isRepeat(false)
				.belayType(BelayType.LEAD)
				.duration(180)
				.falls(2)
				.height(35.5)
				.rating(5)
				.notes("Epic send!")
				.tags(["overhang", "endurance"])
				.beta("Start with undercling")
				.media(Climb.Media.builder()
				.photos(["send1.jpg", "send2.jpg"])
				.videos(["climb.mp4"])
				.count(3)
				.build())
				.color("red")
				.rockType(RockType.GRANITE)
				.terrainType(TerrainType.NATURAL)
				.isIndoor(false)
				.partners(["Alex", "Sam"])
				.weather("sunny")
				.customFields(customFields)
				.build()

		then: "all fields are set"
		climb.id == 1
		climb.sessionId == 1
		climb.routeId == "123"
		climb.date == date
		climb.time == time
		climb.routeName == "The Nose"
		climb.type == ClimbType.ROUTE
		climb.finishType == FinishType.REDPOINT
		climb.attempts == 3
		climb.repeats == 0
		!climb.isRepeat
		climb.belayType == BelayType.LEAD
		climb.duration == 180
		climb.falls == 2
		climb.height == 35.5
		climb.rating == 5
		climb.notes == "Epic send!"
		climb.tags == ["overhang", "endurance"]
		climb.beta == "Start with undercling"
		climb.color == "red"
		climb.rockType == RockType.GRANITE
		climb.terrainType == TerrainType.NATURAL
		!climb.isIndoor
		climb.partners == ["Alex", "Sam"]
		climb.weather == "sunny"
		climb.customFields == customFields

		and: "grade info is correct"
		climb.grades.system == GradeSystem.YDS
		climb.grades.grade == "5.14a"

		and: "media is correct"
		climb.media.photos.size() == 2
		climb.media.videos.size() == 1
		climb.media.count == 3
	}

	def "should serialize climb to JSON"() {
		given: "a climb"
		def climb = Climb.builder()
				.id(1)
				.date(LocalDate.of(2024, 1, 15))
				.routeName("Test Route")
				.type(ClimbType.BOULDER)
				.finishType(FinishType.FLASH)
				.attempts(1)
				.rating(4)
				.build()

		when: "serializing to JSON"
		def json = objectMapper.writeValueAsString(climb)

		then: "JSON contains expected fields"
		json.contains('"id":1')
		json.contains('"date":"2024-01-15"')
		json.contains('"routeName":"Test Route"')
		json.contains('"type":"boulder"')
		json.contains('"finishType":"flash"')
		json.contains('"attempts":1')
		json.contains('"rating":4')
	}

	def "should deserialize climb from JSON"() {
		given: "JSON representation"
		def json = '''
            {
                "id": 1,
                "sessionId": 1,
                "date": "2024-01-15",
                "routeName": "Test Boulder",
                "type": "boulder",
                "finishType": "flash",
                "grades": {
                    "system": "vScale",
                    "grade": "V5"
                },
                "attempts": 2,
                "rating": 4
            }
        '''

		when: "deserializing from JSON"
		def climb = objectMapper.readValue(json, Climb)

		then: "climb is created correctly"
		climb.id == 1
		climb.sessionId == 1
		climb.date == LocalDate.of(2024, 1, 15)
		climb.routeName == "Test Boulder"
		climb.type == ClimbType.BOULDER
		climb.finishType == FinishType.FLASH
		climb.attempts == 2
		climb.rating == 4

		and: "grade info is deserialized"
		climb.grades != null
		climb.grades.system == GradeSystem.V_SCALE
		climb.grades.grade == "V5"
	}

	def "should handle all climb types"() {
		expect: "all climb types are valid"
		ClimbType.values().size() == 2
		ClimbType.valueOf(climbType) != null

		where:
		climbType << ["BOULDER", "ROUTE"]
	}

	def "should handle all grade systems"() {
		expect: "all grade systems are valid"
		GradeSystem.values().size() == 5
		GradeSystem.valueOf(system) != null

		where:
		system << [
			"V_SCALE",
			"FONT",
			"FRENCH",
			"YDS",
			"UIAA"
		]
	}

	def "should handle all belay types"() {
		expect: "all belay types are valid"
		BelayType.values().size() == 3
		BelayType.valueOf(belayType) != null

		where:
		belayType << [
			"TOP_ROPE",
			"LEAD",
			"AUTO_BELAY"
		]
	}

	def "should handle default values correctly"() {
		when: "creating climb with minimal fields"
		def climb = Climb.builder()
				.id(1)
				.date(LocalDate.now())
				.routeName("Test")
				.type(ClimbType.ROUTE)
				.finishType(FinishType.REDPOINT)
				.build()

		then: "defaults are set"
		climb.attempts == 1
		climb.repeats == 0
		!climb.isRepeat
		climb.sessionId == null
		climb.routeId == null
		climb.time == null
		climb.belayType == null
	}

	def "should create ClimbsFile with climbs"() {
		given: "a list of climbs"
		def climbs = [
			Climb.builder()
			.id(1)
			.date(LocalDate.now())
			.routeName("Route 1")
			.type(ClimbType.ROUTE)
			.finishType(FinishType.REDPOINT)
			.build(),
			Climb.builder()
			.id(2)
			.date(LocalDate.now())
			.routeName("Boulder 1")
			.type(ClimbType.BOULDER)
			.finishType(FinishType.FLASH)
			.build()
		]

		when: "creating ClimbsFile"
		def file = new ClimbsFile()
		file.climbs = climbs

		then: "file contains climbs"
		file.climbs != null
		file.climbs.size() == 2
		file.climbs[0].routeName == "Route 1"
		file.climbs[1].routeName == "Boulder 1"
	}
}
