package app.crushlog.cldf.models

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import spock.lang.Specification

import java.time.LocalDate
import java.time.OffsetDateTime
import app.crushlog.cldf.models.enums.Platform
import app.crushlog.cldf.models.enums.MediaStrategy

import app.crushlog.cldf.models.enums.Platform
import app.crushlog.cldf.models.enums.MediaStrategy

class ManifestSpec extends Specification {

	ObjectMapper objectMapper

	def setup() {
		objectMapper = new ObjectMapper()
		objectMapper.registerModule(new JavaTimeModule())
		objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
	}

	def "should build manifest with required fields"() {
		given: "export date"
		def creationDate = OffsetDateTime.now()

		when: "creating a manifest"
		def manifest = Manifest.builder()
				.version("1.0.0")
				.format("CLDF")
				.creationDate(creationDate)
				.appVersion("1.0")
				.platform(Platform.DESKTOP)
				.build()

		then: "required fields are set"
		manifest.version == "1.0.0"
		manifest.format == "CLDF"
		manifest.creationDate == creationDate
		manifest.appVersion == "1.0"
		manifest.platform == Platform.DESKTOP
	}

	def "should build manifest with all fields"() {
		given: "dates and export options"
		def creationDate = OffsetDateTime.now()
		def startDate = LocalDate.of(2023, 1, 1)
		def endDate = LocalDate.of(2024, 12, 31)

		when: "creating a complete manifest"
		def manifest = Manifest.builder()
				.version("1.0.0")
				.format("CLDF")
				.creationDate(creationDate)
				.appVersion("2.5.0")
				.platform(Platform.IOS)
				.stats(Manifest.Stats.builder()
				.climbsCount(150)
				.locationsCount(10)
				.sessionsCount(25)
				.routesCount(75)
				.tagsCount(20)
				.mediaCount(45)
				.build())
				.exportOptions(Manifest.ExportOptions.builder()
				.includeMedia(true)
				.mediaStrategy(MediaStrategy.REFERENCE)
				.dateRange(Manifest.ExportOptions.DateRange.builder()
				.start(startDate)
				.end(endDate)
				.build())
				.build())
				.build()

		then: "all fields are set"
		manifest.version == "1.0.0"
		manifest.format == "CLDF"
		manifest.creationDate == creationDate
		manifest.appVersion == "2.5.0"
		manifest.platform == Platform.IOS

		and: "stats are correct"
		manifest.stats.climbsCount == 150
		manifest.stats.locationsCount == 10
		manifest.stats.sessionsCount == 25
		manifest.stats.routesCount == 75
		manifest.stats.tagsCount == 20
		manifest.stats.mediaCount == 45

		and: "export options are correct"
		manifest.exportOptions.includeMedia
		manifest.exportOptions.mediaStrategy == MediaStrategy.REFERENCE
		manifest.exportOptions.dateRange.start == startDate
		manifest.exportOptions.dateRange.end == endDate
	}

	def "should serialize manifest to JSON"() {
		given: "a manifest"
		def manifest = Manifest.builder()
				.version("1.0.0")
				.format("CLDF")
				.creationDate(OffsetDateTime.parse("2024-01-15T10:00:00Z"))
				.appVersion("2.0.0")
				.platform(Platform.ANDROID)
				.stats(Manifest.Stats.builder()
				.climbsCount(50)
				.locationsCount(5)
				.build())
				.build()

		when: "serializing to JSON"
		def json = objectMapper.writeValueAsString(manifest)

		then: "JSON contains expected fields"
		json.contains('"version":"1.0.0"')
		json.contains('"format":"CLDF"')
		json.contains('"creationDate":"2024-01-15T10:00:00Z"')
		json.contains('"appVersion":"2.0.0"')
		json.contains('"platform":"Android"')
		json.contains('"climbsCount":50')
		json.contains('"locationsCount":5')
	}

	def "should deserialize manifest from JSON"() {
		given: "JSON representation"
		def json = '''
            {
                "version": "1.0.0",
                "format": "CLDF",
                "creationDate": "2024-01-15T10:00:00.000Z",
                "appVersion": "2.5.0",
                "platform": "Desktop",
                "stats": {
                    "climbsCount": 100,
                    "locationsCount": 8,
                    "sessionsCount": 20
                }
            }
        '''

		when: "deserializing from JSON"
		def manifest = objectMapper.readValue(json, Manifest)

		then: "manifest is created correctly"
		manifest.version == "1.0.0"
		manifest.format == "CLDF"
		manifest.creationDate == OffsetDateTime.parse("2024-01-15T10:00:00Z")
		manifest.appVersion == "2.5.0"
		manifest.platform == Platform.DESKTOP

		and: "stats are deserialized"
		manifest.stats != null
		manifest.stats.climbsCount == 100
		manifest.stats.locationsCount == 8
		manifest.stats.sessionsCount == 20
	}

	def "should handle all platforms"() {
		expect: "all platforms are valid"
		Platform.values().size() == 4
		Platform.valueOf(platform) != null

		where:
		platform << [
			"IOS",
			"ANDROID",
			"WEB",
			"DESKTOP"
		]
	}

	def "should validate required manifest fields for CLDFArchive"() {
		when: "creating manifest without required fields"
		def manifest = Manifest.builder().build()

		then: "fields are null"
		manifest.version == null
		manifest.format == null
		manifest.creationDate == null
	}
}
