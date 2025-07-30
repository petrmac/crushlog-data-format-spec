package io.cldf.api

import spock.lang.Specification
import io.cldf.models.Manifest
import io.cldf.models.Location
import io.cldf.models.Climb

class SchemaValidatorResultSpec extends Specification {

	def validator = new SchemaValidator()

	def "should return success for valid manifest"() {
		given:
		// Create a minimal valid manifest with only required fields
		def manifest = [
			version: "1.0.0",
			format: "CLDF",
			creationDate: java.time.OffsetDateTime.now().toString(), // Convert to ISO 8601 string
			appVersion: "1.0.0",
			platform: "Android"
		]

		when:
		def result = validator.validateObjectWithResult("manifest.json", manifest)

		then:
		result.valid
		result.filename == "manifest.json"
		result.errors.isEmpty()
	}

	def "should return failure with errors for invalid manifest"() {
		given:
		def invalidManifest = [:] // Empty object missing all required fields

		when:
		def result = validator.validateObjectWithResult("manifest.json", invalidManifest)

		then:
		!result.valid
		result.filename == "manifest.json"
		!result.errors.isEmpty()
		// Check for required field errors (messages might be localized)
		result.errors.any { it.type == "required" }
	}

	def "should return success for unknown file types"() {
		given:
		def content = '{"some": "data"}'.bytes

		when:
		def result = validator.validateWithResult("unknown.json", content)

		then:
		result.valid
		result.filename == "unknown.json"
		result.errors.isEmpty()
	}

	def "should return failure for invalid JSON"() {
		given:
		def invalidJson = "not valid json".bytes

		when:
		def result = validator.validateWithResult("manifest.json", invalidJson)

		then:
		!result.valid
		result.filename == "manifest.json"
		result.errors.size() == 1
		result.errors[0].type == "parse_error"
		result.errors[0].path == '$'
	}

	def "should return multiple validation errors"() {
		given:
		// locations.json expects an array of locations
		def invalidLocations = [
			[
				// Missing required fields: id, name, latitude, longitude
			]
		]

		when:
		def result = validator.validateObjectWithResult("locations.json", invalidLocations)

		then:
		!result.valid
		result.filename == "locations.json"
		result.errors.size() >= 1  // Should have at least one error
	}

	def "should handle serialization errors gracefully"() {
		given:
		// An object that will fail to serialize
		def unserializable = new Object() {
					def getCircularRef() {
						this
					}
				}

		when:
		def result = validator.validateObjectWithResult("manifest.json", unserializable)

		then:
		!result.valid
		result.filename == "manifest.json"
		result.errors.size() == 1
		result.errors[0].type == "serialization_error"
	}

	def "should provide detailed error information"() {
		given:
		def invalidClimb = [
			id: "climb-1",
			sessionId: "session-1",
			routeId: "route-1",
			timestamp: "not-a-valid-timestamp", // Invalid format
			grade: "V5",
			attempts: 1,
			sendType: "ONSIGHT"
		]

		when:
		def result = validator.validateObjectWithResult("climbs.json", invalidClimb)

		then:
		!result.valid
		result.errors.any { error ->
			error.path != null
			error.message != null
			error.type != null
		}
	}
}
