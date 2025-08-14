package app.crushlog.cldf.models

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import spock.lang.Specification

import java.time.LocalDate
import java.time.LocalTime

import app.crushlog.cldf.models.enums.ClimbType
import app.crushlog.cldf.models.enums.RockType
import app.crushlog.cldf.models.enums.TerrainType
import app.crushlog.cldf.models.enums.SessionType

class SessionSpec extends Specification {

	ObjectMapper objectMapper

	def setup() {
		objectMapper = new ObjectMapper()
		objectMapper.registerModule(new JavaTimeModule())
		objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
	}

	def "should build session with required fields"() {
		given: "current date"
		def date = LocalDate.now()

		when: "creating a session"
		def session = Session.builder()
				.id(1)
				.date(date)
				.location("Test Crag")
				.build()

		then: "required fields are set"
		session.id == 1
		session.date == date
		session.location == "Test Crag"

		and: "defaults are applied"
		!session.isOngoing
	}

	def "should build session with all fields"() {
		given: "dates and weather data"
		def date = LocalDate.now()
		def startTime = LocalTime.of(9, 0)
		def endTime = LocalTime.of(17, 30)

		when: "creating a complete session"
		def session = Session.builder()
				.id(1)
				.date(date)
				.startTime(startTime)
				.endTime(endTime)
				.location("Bishop")
				.locationId(1)
				.isIndoor(false)
				.climbType(ClimbType.BOULDER)
				.sessionType(SessionType.BOULDERING)
				.partners(["Alex", "Sam"])
				.weather(Session.Weather.builder()
				.conditions("sunny")
				.temperature(68.0)
				.humidity(45.0)
				.wind("light breeze")
				.build())
				.notes("Great conditions today")
				.rockType(RockType.GRANITE)
				.terrainType(TerrainType.NATURAL)
				.approachTime(15)
				.isOngoing(false)
				.build()

		then: "all fields are set"
		session.id == 1
		session.date == date
		session.startTime == startTime
		session.endTime == endTime
		session.location == "Bishop"
		session.locationId == 1
		!session.isIndoor
		session.climbType == ClimbType.BOULDER
		session.sessionType == SessionType.BOULDERING
		session.partners == ["Alex", "Sam"]
		session.notes == "Great conditions today"
		session.rockType == RockType.GRANITE
		session.terrainType == TerrainType.NATURAL
		session.approachTime == 15
		!session.isOngoing

		and: "weather is set correctly"
		session.weather.conditions == "sunny"
		session.weather.temperature == 68.0
		session.weather.humidity == 45.0
		session.weather.wind == "light breeze"
	}

	def "should serialize session to JSON"() {
		given: "a session"
		def session = Session.builder()
				.id(1)
				.date(LocalDate.of(2024, 1, 15))
				.location("Test Gym")
				.locationId(1)
				.isIndoor(true)
				.sessionType(SessionType.INDOOR_CLIMBING)
				.build()

		when: "serializing to JSON"
		def json = objectMapper.writeValueAsString(session)

		then: "JSON contains expected fields"
		json.contains('"id":1')
		json.contains('"date":"2024-01-15"')
		json.contains('"location":"Test Gym"')
		json.contains('"locationId":1')
		json.contains('"isIndoor":true')
		json.contains('"sessionType":"indoorClimbing"')
		json.contains('"isOngoing":false')
	}

	def "should deserialize session from JSON"() {
		given: "JSON representation"
		def json = '''
            {
                "id": 1,
                "date": "2024-01-15",
                "location": "Bishop",
                "locationId": 1,
                "isIndoor": false,
                "climbType": "boulder",
                "sessionType": "bouldering",
                "weather": {
                    "temperature": 72.0,
                    "conditions": "partly cloudy"
                }
            }
        '''

		when: "deserializing from JSON"
		def session = objectMapper.readValue(json, Session)

		then: "session is created correctly"
		session.id == 1
		session.date == LocalDate.of(2024, 1, 15)
		session.location == "Bishop"
		session.locationId == 1
		!session.isIndoor
		session.climbType == ClimbType.BOULDER
		session.sessionType == SessionType.BOULDERING

		and: "weather is deserialized"
		session.weather != null
		session.weather.temperature == 72.0
		session.weather.conditions == "partly cloudy"
	}

	def "should handle all session types"() {
		expect: "all session types are valid"
		SessionType.values().size() == 7
		SessionType.fromValue(type) != null

		where:
		type << [
			"sportClimbing",
			"multiPitch",
			"tradClimbing",
			"bouldering",
			"indoorClimbing",
			"indoorBouldering",
			"boardSession"
		]
	}

	def "should handle ongoing sessions"() {
		when: "creating an ongoing session"
		def session = Session.builder()
				.id(1)
				.date(LocalDate.now())
				.location("Gym")
				.startTime(LocalTime.now())
				.isOngoing(true)
				.build()

		then: "session is marked as ongoing"
		session.isOngoing
		session.endTime == null
	}

	def "should create SessionsFile with sessions"() {
		given: "a list of sessions"
		def sessions = [
			Session.builder()
			.id(1)
			.date(LocalDate.now())
			.location("Gym")
			.build(),
			Session.builder()
			.id(2)
			.date(LocalDate.now())
			.location("Crag")
			.build()
		]

		when: "creating SessionsFile"
		def file = new SessionsFile()
		file.sessions = sessions

		then: "file contains sessions"
		file.sessions != null
		file.sessions.size() == 2
		file.sessions[0].location == "Gym"
		file.sessions[1].location == "Crag"
	}
}
