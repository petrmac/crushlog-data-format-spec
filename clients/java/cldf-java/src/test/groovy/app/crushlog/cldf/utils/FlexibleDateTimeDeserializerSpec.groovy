package app.crushlog.cldf.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import app.crushlog.cldf.models.Manifest
import spock.lang.Specification
import spock.lang.Unroll

import java.time.OffsetDateTime
import java.time.ZoneOffset

import app.crushlog.cldf.models.enums.Platform
import app.crushlog.cldf.models.enums.MediaStrategy

class FlexibleDateTimeDeserializerSpec extends Specification {

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
            "version": "1.0.0",
            "format": "CLDF",
            "creationDate": "$dateString",
            "appVersion": "1.0.0",
            "platform": "Desktop"
        }
        """

		when: "deserializing the JSON"
		def manifest = objectMapper.readValue(json, Manifest.class)

		then: "the date should be parsed correctly"
		manifest.creationDate != null
		manifest.creationDate == expectedDateTime

		where:
		dateString                      | expectedDateTime
		"2024-01-29T12:00:00.000+00:00" | OffsetDateTime.of(2024, 1, 29, 12, 0, 0, 0, ZoneOffset.UTC)
		"2024-01-29T12:00:00.000Z"      | OffsetDateTime.of(2024, 1, 29, 12, 0, 0, 0, ZoneOffset.UTC)
		"2024-01-29T12:00:00+00:00"     | OffsetDateTime.of(2024, 1, 29, 12, 0, 0, 0, ZoneOffset.UTC)
		"2024-01-29T12:00:00Z"          | OffsetDateTime.of(2024, 1, 29, 12, 0, 0, 0, ZoneOffset.UTC)
		"2024-01-29T12:00:00-05:00"     | OffsetDateTime.of(2024, 1, 29, 12, 0, 0, 0, ZoneOffset.ofHours(-5))
		"2024-01-29T12:00:00+02:00"     | OffsetDateTime.of(2024, 1, 29, 12, 0, 0, 0, ZoneOffset.ofHours(2))
		"2024-01-29T12:00:00.123+00:00" | OffsetDateTime.of(2024, 1, 29, 12, 0, 0, 123_000_000, ZoneOffset.UTC)
		"2024-01-29T12:00:00.123Z"      | OffsetDateTime.of(2024, 1, 29, 12, 0, 0, 123_000_000, ZoneOffset.UTC)
	}

	@Unroll
	def "should handle edge case '#dateString' correctly"() {
		given: "a JSON string with edge case date format"
		def json = """
        {
            "version": "1.0.0",
            "format": "CLDF",
            "creationDate": "$dateString",
            "appVersion": "1.0.0",
            "platform": "Desktop"
        }
        """

		when: "deserializing the JSON"
		def manifest = objectMapper.readValue(json, Manifest.class)

		then: "the date should be parsed correctly"
		manifest.creationDate == expectedDateTime

		where:
		dateString                    | expectedDateTime
		"2024-12-31T23:59:59+14:00"   | OffsetDateTime.of(2024, 12, 31, 23, 59, 59, 0, ZoneOffset.ofHours(14))
		"2024-01-01T00:00:00-12:00"   | OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(-12))
		"2024-06-15T10:30:45.999Z"    | OffsetDateTime.of(2024, 6, 15, 10, 30, 45, 999_000_000, ZoneOffset.UTC)
	}

	def "should handle null and empty date strings"() {
		given: "JSON with null date"
		def jsonWithNull = """
        {
            "version": "1.0.0",
            "format": "CLDF",
            "creationDate": null,
            "appVersion": "1.0.0",
            "platform": "Desktop"
        }
        """

		when: "deserializing JSON with null date"
		def manifest = objectMapper.readValue(jsonWithNull, Manifest.class)

		then: "date should be null"
		manifest.creationDate == null
	}

	def "should throw descriptive error for invalid date formats"() {
		given: "JSON with invalid date format"
		def invalidFormats = [
			"invalid-date",
			"2024-13-45T25:70:80Z",
			// Invalid date/time values
			"2024/01/29 12:00:00",
			// Wrong separators
			"Jan 29, 2024 12:00 PM",
			// Wrong format entirely
			"2024-01-29"             // Date only, no time
		]

		expect: "each invalid format to throw an IOException"
		invalidFormats.each { invalidDate ->
			def json = """
            {
                "version": "1.0.0",
                "format": "CLDF",
                "creationDate": "$invalidDate",
                "appVersion": "1.0.0",
                "platform": "Desktop"
            }
            """

			try {
				objectMapper.readValue(json, Manifest.class)
				// If we get here without exception, the test should fail
				assert false, "Expected IOException for invalid date: $invalidDate"
			} catch (Exception e) {
				// Should throw an exception with descriptive message
				assert e.message.contains("Cannot parse date string") ||
				e.message.contains("Cannot deserialize") ||
				e.message.contains("Invalid value")
			}
		}
	}

	def "should handle empty string as null"() {
		given: "JSON with empty date string"
		def json = """
        {
            "version": "1.0.0",
            "format": "CLDF",
            "creationDate": "",
            "appVersion": "1.0.0",
            "platform": "Desktop"
        }
        """

		when: "deserializing JSON with empty date"
		def manifest = objectMapper.readValue(json, Manifest.class)

		then: "date should be null"
		manifest.creationDate == null
	}

	@Unroll
	def "should preserve timezone information for '#dateString'"() {
		given: "a JSON string with timezone-specific date"
		def json = """
        {
            "version": "1.0.0",
            "format": "CLDF",
            "creationDate": "$dateString",
            "appVersion": "1.0.0",
            "platform": "Desktop"
        }
        """

		when: "deserializing the JSON"
		def manifest = objectMapper.readValue(json, Manifest.class)

		then: "timezone information should be preserved"
		manifest.creationDate.offset == expectedOffset

		where:
		dateString                   | expectedOffset
		"2024-06-15T14:30:00+05:30"  | ZoneOffset.ofHoursMinutes(5, 30) // India Standard Time
		"2024-06-15T14:30:00-08:00"  | ZoneOffset.ofHours(-8)           // Pacific Standard Time
		"2024-06-15T14:30:00+00:00"  | ZoneOffset.UTC                   // UTC
		"2024-06-15T14:30:00Z"       | ZoneOffset.UTC                   // UTC with Z notation
	}

	def "should handle whitespace in date strings"() {
		given: "date string with leading/trailing whitespace"
		def json = """
        {
            "version": "1.0.0",
            "format": "CLDF",
            "creationDate": "  2024-01-29T12:00:00Z  ",
            "appVersion": "1.0.0",
            "platform": "Desktop"
        }
        """

		when: "deserializing the JSON"
		def manifest = objectMapper.readValue(json, Manifest.class)

		then: "the date should be parsed correctly despite whitespace"
		manifest.creationDate == OffsetDateTime.of(2024, 1, 29, 12, 0, 0, 0, ZoneOffset.UTC)
	}

	def "should work with complete Manifest object"() {
		given: "a complete manifest JSON with flexible date"
		def json = """
        {
            "version": "1.0.0",
            "format": "CLDF",
            "creationDate": "2024-01-29T12:00:00Z",
            "appVersion": "2.1.0",
            "platform": "Desktop",
            "author": {
                "name": "Test User",
                "email": "test@example.com",
                "website": "https://example.com"
            },
            "source": "Manual Export",
            "stats": {
                "climbsCount": 150,
                "sessionsCount": 25,
                "locationsCount": 5,
                "routesCount": 75,
                "tagsCount": 20,
                "mediaCount": 100
            },
            "exportOptions": {
                "includeMedia": true,
                "mediaStrategy": "thumbnails",
                "dateRange": {
                    "start": "2024-01-01",
                    "end": "2024-01-31"
                }
            }
        }
        """

		when: "deserializing the complete manifest"
		def manifest = objectMapper.readValue(json, Manifest.class)

		then: "all fields should be parsed correctly"
		manifest.version == "1.0.0"
		manifest.format == "CLDF"
		manifest.creationDate == OffsetDateTime.of(2024, 1, 29, 12, 0, 0, 0, ZoneOffset.UTC)
		manifest.appVersion == "2.1.0"
		manifest.platform == Platform.DESKTOP
		manifest.author.name == "Test User"
		manifest.author.email == "test@example.com"
		manifest.author.website == "https://example.com"
		manifest.source == "Manual Export"
		manifest.stats.climbsCount == 150
		manifest.stats.sessionsCount == 25
		manifest.stats.locationsCount == 5
		manifest.stats.routesCount == 75
		manifest.stats.tagsCount == 20
		manifest.stats.mediaCount == 100
		manifest.exportOptions.includeMedia == true
		manifest.exportOptions.mediaStrategy == MediaStrategy.THUMBNAILS
	}
}
