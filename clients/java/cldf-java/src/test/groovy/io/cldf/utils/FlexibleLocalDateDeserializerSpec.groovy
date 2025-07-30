package io.cldf.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.cldf.models.Session
import spock.lang.Specification
import spock.lang.Unroll

import java.time.LocalDate

import io.cldf.models.enums.ClimbType
import io.cldf.models.enums.RockType
import io.cldf.models.enums.TerrainType
import io.cldf.models.enums.SessionType

class FlexibleLocalDateDeserializerSpec extends Specification {

	ObjectMapper objectMapper

	def setup() {
		objectMapper = new ObjectMapper()
		objectMapper.registerModule(new JavaTimeModule())
	}

	@Unroll
	def "should parse date format '#dateString' successfully"() {
		given: "a JSON string with the date format"
		def json = """
        {
            "id": "session-1",
            "date": "$dateString",
            "location": "Test Location"
        }
        """

		when: "deserializing the JSON"
		def session = objectMapper.readValue(json, Session.class)

		then: "the date should be parsed correctly"
		session.date != null
		session.date == expectedDate

		where:
		dateString    | expectedDate
		"2024-01-29"  | LocalDate.of(2024, 1, 29)
		"2024/01/29"  | LocalDate.of(2024, 1, 29)
		"2024.01.29"  | LocalDate.of(2024, 1, 29)
		"01/29/2024"  | LocalDate.of(2024, 1, 29)
		"29/01/2024"  | LocalDate.of(2024, 1, 29)
		"29.01.2024"  | LocalDate.of(2024, 1, 29)
		"01-29-2024"  | LocalDate.of(2024, 1, 29)
		"29-01-2024"  | LocalDate.of(2024, 1, 29)
		"20240129"    | LocalDate.of(2024, 1, 29)
	}

	@Unroll
	def "should handle edge case '#dateString' correctly"() {
		given: "a JSON string with edge case date format"
		def json = """
        {
            "id": "session-1",
            "date": "$dateString",
            "location": "Test Location"
        }
        """

		when: "deserializing the JSON"
		def session = objectMapper.readValue(json, Session.class)

		then: "the date should be parsed correctly"
		session.date == expectedDate

		where:
		dateString    | expectedDate
		"2024-02-29"  | LocalDate.of(2024, 2, 29)  // Leap year
		"2024-01-01"  | LocalDate.of(2024, 1, 1)   // New Year's Day
		"2024-12-31"  | LocalDate.of(2024, 12, 31) // New Year's Eve
		"2024/12/25"  | LocalDate.of(2024, 12, 25) // Slash format
		"12/25/2024"  | LocalDate.of(2024, 12, 25) // US format
		"25/12/2024"  | LocalDate.of(2024, 12, 25) // European format
	}

	def "should handle null and empty date strings"() {
		given: "JSON with null date"
		def jsonWithNull = """
        {
            "id": "session-1",
            "date": null,
            "location": "Test Location"
        }
        """

		when: "deserializing JSON with null date"
		def session = objectMapper.readValue(jsonWithNull, Session.class)

		then: "date should be null"
		session.date == null
	}

	def "should handle empty string as null"() {
		given: "JSON with empty date string"
		def json = """
        {
            "id": "session-1",
            "date": "",
            "location": "Test Location"
        }
        """

		when: "deserializing JSON with empty date"
		def session = objectMapper.readValue(json, Session.class)

		then: "date should be null"
		session.date == null
	}

	@Unroll
	def "should throw descriptive error for invalid date format '#invalidDate'"() {
		given: "a JSON string with invalid date format"
		def json = """
        {
            "id": "session-1",
            "date": "$invalidDate",
            "location": "Test Location"
        }
        """

		when: "trying to deserialize the JSON"
		objectMapper.readValue(json, Session.class)

		then: "should throw an exception with descriptive message"
		Exception e = thrown()
		e.message.contains("Cannot parse date string") ||
				e.message.contains("Cannot deserialize") ||
				e.message.contains("Invalid value") ||
				e.message.contains("DateTimeParseException")

		where:
		invalidDate << [
			"not-a-date-at-all",
			"2024-13-01",
			// Invalid month
			"totally-invalid"
		]
	}

	def "should handle whitespace in date strings"() {
		given: "date string with leading/trailing whitespace"
		def json = """
        {
            "id": "session-1",
            "date": "  2024-01-29  ",
            "location": "Test Location"
        }
        """

		when: "deserializing the JSON"
		def session = objectMapper.readValue(json, Session.class)

		then: "the date should be parsed correctly despite whitespace"
		session.date == LocalDate.of(2024, 1, 29)
	}

	def "should work with complete Session object"() {
		given: "a complete session JSON with flexible date"
		def json = """
        {
            "id": "session-1",
            "date": "2024-01-29",
            "startTime": "09:00:00",
            "endTime": "17:00:00",
            "location": "Boulder Canyon",
            "locationId": "1",
            "isIndoor": false,
            "climbType": "boulder",
            "sessionType": "bouldering",
            "partners": ["Alice", "Bob"],
            "weather": {
                "conditions": "sunny",
                "temperature": 22.5,
                "humidity": 45.0,
                "wind": "light"
            },
            "notes": "Great day out!",
            "rockType": "granite",
            "terrainType": "natural",
            "approachTime": 30,
            "isOngoing": false
        }
        """

		when: "deserializing the complete session"
		def session = objectMapper.readValue(json, Session.class)

		then: "all fields should be parsed correctly"
		session.id == "session-1"
		session.date == LocalDate.of(2024, 1, 29)
		session.location == "Boulder Canyon"
		session.locationId == "1"
		session.isIndoor == false
		session.climbType == ClimbType.BOULDER
		session.sessionType == SessionType.BOULDERING
		session.partners == ["Alice", "Bob"]
		session.weather.conditions == "sunny"
		session.weather.temperature == 22.5
		session.weather.humidity == 45.0
		session.weather.wind == "light"
		session.notes == "Great day out!"
		session.rockType == RockType.GRANITE
		session.terrainType == TerrainType.NATURAL
		session.approachTime == 30
		session.isOngoing == false
	}

	@Unroll
	def "should handle different year format '#dateString' correctly"() {
		given: "a JSON string with different year format"
		def json = """
        {
            "id": "session-1",
            "date": "$dateString",
            "location": "Test Location"
        }
        """

		when: "deserializing the JSON"
		def session = objectMapper.readValue(json, Session.class)

		then: "the date should be parsed correctly"
		session.date == expectedDate

		where:
		dateString    | expectedDate
		"2024-01-29"  | LocalDate.of(2024, 1, 29)
		"1999-12-31"  | LocalDate.of(1999, 12, 31)
		"2000-01-01"  | LocalDate.of(2000, 1, 1)
	}
}
