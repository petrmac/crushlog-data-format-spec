package io.cldf.models.enums

import spock.lang.Specification
import com.fasterxml.jackson.databind.ObjectMapper

class MediaDesignationSpec extends Specification {

	def objectMapper = new ObjectMapper()

	def "should have all designation values"() {
		expect:
		MediaDesignation.values().length == 9
		MediaDesignation.TOPO.value == "topo"
		MediaDesignation.BETA.value == "beta"
		MediaDesignation.APPROACH.value == "approach"
		MediaDesignation.LOG.value == "log"
		MediaDesignation.OVERVIEW.value == "overview"
		MediaDesignation.CONDITIONS.value == "conditions"
		MediaDesignation.GEAR.value == "gear"
		MediaDesignation.DESCENT.value == "descent"
		MediaDesignation.OTHER.value == "other"
	}

	def "should parse from string value"() {
		expect:
		MediaDesignation.fromValue("topo") == MediaDesignation.TOPO
		MediaDesignation.fromValue("beta") == MediaDesignation.BETA
		MediaDesignation.fromValue("approach") == MediaDesignation.APPROACH
		MediaDesignation.fromValue("TOPO") == MediaDesignation.TOPO // case insensitive
	}

	def "should default to OTHER for unknown values"() {
		expect:
		MediaDesignation.fromValue("unknown") == MediaDesignation.OTHER
		MediaDesignation.fromValue(null) == MediaDesignation.OTHER
	}

	def "should serialize to JSON correctly"() {
		given:
		def designation = MediaDesignation.TOPO

		when:
		def json = objectMapper.writeValueAsString(designation)

		then:
		json == '"topo"'
	}

	def "should deserialize from JSON correctly"() {
		given:
		def json = '"beta"'

		when:
		def designation = objectMapper.readValue(json, MediaDesignation.class)

		then:
		designation == MediaDesignation.BETA
	}

	def "should handle all designation types in serialization"() {
		given:
		def testData = [
			designation: designation
		]

		when:
		def json = objectMapper.writeValueAsString(testData)
		def result = objectMapper.readValue(json, Map.class)

		then:
		result.designation == expectedValue

		where:
		designation                 | expectedValue
		MediaDesignation.TOPO       | "topo"
		MediaDesignation.BETA       | "beta"
		MediaDesignation.APPROACH   | "approach"
		MediaDesignation.LOG        | "log"
		MediaDesignation.OVERVIEW   | "overview"
		MediaDesignation.CONDITIONS | "conditions"
		MediaDesignation.GEAR       | "gear"
		MediaDesignation.DESCENT    | "descent"
		MediaDesignation.OTHER      | "other"
	}
}
