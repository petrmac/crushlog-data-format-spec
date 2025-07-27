package io.cldf.models

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import spock.lang.Specification

import java.time.LocalDate
import java.time.LocalTime

class ClimbSpec extends Specification {

	ObjectMapper objectMapper

	def setup() {
		objectMapper = new ObjectMapper()
		objectMapper.registerModule(new JavaTimeModule())
	}

	def "should build climb with required fields"() {
		given: "current date"
		def date = LocalDate.now()

		when: "creating a climb"
		def climb = Climb.builder()
				.id(1)
				.date(date)
				.routeName("Midnight Lightning")
				.type(Climb.ClimbType.boulder)
				.finishType("flash")
				.build()

		then: "required fields are set"
		climb.id == 1
		climb.date == date
		climb.routeName == "Midnight Lightning"
		climb.type == Climb.ClimbType.boulder
		climb.finishType == "flash"

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
				.routeId(123)
				.date(date)
				.time(time)
				.routeName("The Nose")
				.type(Climb.ClimbType.route)
				.finishType("redpoint")
				.grades(Climb.GradeInfo.builder()
				.system(Climb.GradeInfo.GradeSystem.yds)
				.grade("5.14a")
				.build())
				.attempts(3)
				.repeats(0)
				.isRepeat(false)
				.belayType(Climb.BelayType.lead)
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
				.rockType(Location.RockType.granite)
				.terrainType(Location.TerrainType.natural)
				.isIndoor(false)
				.partners(["Alex", "Sam"])
				.weather("sunny")
				.customFields(customFields)
				.build()

		then: "all fields are set"
		climb.id == 1
		climb.sessionId == 1
		climb.routeId == 123
		climb.date == date
		climb.time == time
		climb.routeName == "The Nose"
		climb.type == Climb.ClimbType.route
		climb.finishType == "redpoint"
		climb.attempts == 3
		climb.repeats == 0
		!climb.isRepeat
		climb.belayType == Climb.BelayType.lead
		climb.duration == 180
		climb.falls == 2
		climb.height == 35.5
		climb.rating == 5
		climb.notes == "Epic send!"
		climb.tags == ["overhang", "endurance"]
		climb.beta == "Start with undercling"
		climb.color == "red"
		climb.rockType == Location.RockType.granite
		climb.terrainType == Location.TerrainType.natural
		!climb.isIndoor
		climb.partners == ["Alex", "Sam"]
		climb.weather == "sunny"
		climb.customFields == customFields

		and: "grade info is correct"
		climb.grades.system == Climb.GradeInfo.GradeSystem.yds
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
				.type(Climb.ClimbType.boulder)
				.finishType("onsight")
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
		json.contains('"finishType":"onsight"')
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
		climb.type == Climb.ClimbType.boulder
		climb.finishType == "flash"
		climb.attempts == 2
		climb.rating == 4

		and: "grade info is deserialized"
		climb.grades != null
		climb.grades.system == Climb.GradeInfo.GradeSystem.vScale
		climb.grades.grade == "V5"
	}

	def "should handle all climb types"() {
		expect: "all climb types are valid"
		Climb.ClimbType.values().size() == 2
		Climb.ClimbType.valueOf(climbType) != null

		where:
		climbType << ["boulder", "route"]
	}

	def "should handle all grade systems"() {
		expect: "all grade systems are valid"
		Climb.GradeInfo.GradeSystem.values().size() == 5
		Climb.GradeInfo.GradeSystem.valueOf(system) != null

		where:
		system << [
			"vScale",
			"font",
			"french",
			"yds",
			"uiaa"
		]
	}

	def "should handle all belay types"() {
		expect: "all belay types are valid"
		Climb.BelayType.values().size() == 3
		Climb.BelayType.valueOf(belayType) != null

		where:
		belayType << [
			"topRope",
			"lead",
			"autoBelay"
		]
	}

	def "should handle default values correctly"() {
		when: "creating climb with minimal fields"
		def climb = Climb.builder()
				.id(1)
				.date(LocalDate.now())
				.routeName("Test")
				.type(Climb.ClimbType.route)
				.finishType("redpoint")
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
			.type(Climb.ClimbType.route)
			.finishType("redpoint")
			.build(),
			Climb.builder()
			.id(2)
			.date(LocalDate.now())
			.routeName("Boulder 1")
			.type(Climb.ClimbType.boulder)
			.finishType("flash")
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
