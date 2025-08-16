package app.crushlog.cldf.schema

import spock.lang.Specification
import spock.lang.Unroll

class SchemaServiceSpec extends Specification {

	def schemaService = new DefaultSchemaService()

	def "should get schema info for all components"() {
		when: "getting schema info for all components"
		def result = schemaService.getSchemaInfo("all")

		then: "result contains all expected sections"
		result.containsKey("dateFormats")
		result.containsKey("enums")
		result.containsKey("commonMistakes")
		result.containsKey("exampleData")
		result.containsKey("manifest")
		result.containsKey("location")
		result.containsKey("route")
		result.containsKey("climb")
		result.containsKey("session")
		result.containsKey("tag")

		and: "each section has content"
		result.size() == 10
		result.values().each { value ->
			assert value != null
			assert !(value instanceof Map && value.isEmpty())
		}
	}

	@Unroll
	def "should get schema info for #component component"() {
		when: "getting schema info for specific component"
		def result = schemaService.getSchemaInfo(component)

		then: "result contains expected key"
		result.containsKey(expectedKey)
		result.size() == 1
		result[expectedKey] != null

		where:
		component        | expectedKey
		"manifest"       | "manifest"
		"location"       | "location"
		"route"          | "route"
		"climb"          | "climb"
		"session"        | "session"
		"tag"            | "tag"
		"dateFormats"    | "dateFormats"
		"enums"          | "enums"
		"commonMistakes" | "commonMistakes"
		"exampleData"    | "exampleData"
	}

	def "should throw exception for unknown component"() {
		when: "getting schema info for unknown component"
		schemaService.getSchemaInfo("unknown")

		then: "IllegalArgumentException is thrown"
		def e = thrown(IllegalArgumentException)
		e.message == "Unknown component: unknown"
	}

	def "should build complete schema with all components"() {
		when: "building complete schema"
		def result = schemaService.buildCompleteSchema()

		then: "all components are present"
		result.size() == 10
		result.containsKey("dateFormats")
		result.containsKey("enums")
		result.containsKey("commonMistakes")
		result.containsKey("exampleData")
		result.containsKey("manifest")
		result.containsKey("location")
		result.containsKey("route")
		result.containsKey("climb")
		result.containsKey("session")
		result.containsKey("tag")
	}

	def "should build enums from schemas"() {
		when: "building enums"
		def enums = schemaService.buildEnumsFromSchemas()
		println "Extracted enums: ${enums.keySet()}"

		then: "all expected enums are present"
		enums.containsKey("platform")
		enums.containsKey("climbType")
		enums.containsKey("belayType")
		enums.containsKey("rockType")
		enums.containsKey("terrainType")
		enums.containsKey("routeType")
		enums.containsKey("finishType")
		enums.containsKey("gradeSystem")
		enums.containsKey("sessionType")

		and: "platform enum has expected values"
		def platforms = enums["platform"] as List
		platforms.containsAll([
			"iOS",
			"Android",
			"Web",
			"Desktop"
		])

		and: "climbType enum has expected values"
		def climbTypes = enums["climbType"] as List
		climbTypes.containsAll(["boulder", "route"])

		and: "finishType is context-dependent"
		def finishTypes = enums["finishType"] as Map
		finishTypes.containsKey("boulder")
		finishTypes.containsKey("route")
		(finishTypes["boulder"] as List).containsAll([
			"flash",
			"top",
			"repeat",
			"project",
			"attempt"
		])
		(finishTypes["route"] as List).containsAll([
			"flash",
			"top",
			"repeat",
			"project",
			"attempt",
			"onsight",
			"redpoint"
		])

		and: "gradeSystem has expected values"
		def gradeSystems = enums["gradeSystem"] as List
		gradeSystems.containsAll([
			"vScale",
			"font",
			"french",
			"yds",
			"uiaa"
		])
	}

	def "should build date formats info"() {
		when: "building date formats info"
		def dateInfo = schemaService.buildDateFormatsInfo()

		then: "description is present"
		dateInfo["description"] == "CLDF supports flexible date parsing with multiple formats"

		and: "supported formats are listed"
		def formats = dateInfo["supportedFormats"] as List
		formats.size() == 6
		formats.any { it.contains("ISO-8601") }

		and: "examples are provided"
		def examples = dateInfo["examples"] as Map
		examples["offsetDateTime"] == "2024-01-29T12:00:00Z"
		examples["localDate"] == "2024-01-29"
	}

	def "should build common mistakes list"() {
		when: "building common mistakes"
		def mistakes = schemaService.buildCommonMistakes()

		then: "list contains expected items"
		mistakes.size() == 8
		mistakes.any { it.contains("Route IDs should be strings") }
		mistakes.any { it.contains("Location IDs should be integers") }
		mistakes.any { it.contains("FinishType values for boulder climbs") }
		mistakes.any { it.contains("Grades object requires both") }
		mistakes.any { it.contains("Manifest requires appVersion") }
		mistakes.any { it.contains("Date formats are flexible") }
	}

	def "should build example data"() {
		when: "building example data"
		def examples = schemaService.buildExampleData()

		then: "minimal example is present"
		examples.containsKey("minimal")
		def minimal = examples["minimal"] as Map

		and: "manifest example is correct"
		def manifest = minimal["manifest"] as Map
		manifest["format"] == "CLDF"
		manifest["version"] == "1.0.0"
		manifest["appVersion"] == "1.0.0"
		manifest["platform"] == "Desktop"
		manifest.containsKey("creationDate")

		and: "locations example is correct"
		def locations = minimal["locations"] as List
		locations.size() == 1
		def location = locations[0] as Map
		location["id"] == 1
		location["name"] == "Test Location"
		location["country"] == "USA"
		location["isIndoor"] == false
		def coordinates = location["coordinates"] as Map
		coordinates["latitude"] == 40.0
		coordinates["longitude"] == -105.0

		and: "routes example is correct"
		def routes = minimal["routes"] as List
		routes.size() == 1
		def route = routes[0] as Map
		route["id"] == "1"
		route["name"] == "Test Route"
		route["locationId"] == "1"
		route["routeType"] == "boulder"
		(route["grades"] as Map)["vScale"] == "V4"

		and: "climbs example is correct"
		def climbs = minimal["climbs"] as List
		climbs.size() == 1
		def climb = climbs[0] as Map
		climb["id"] == 1
		climb["routeId"] == "1"
		climb["type"] == "boulder"
		climb["finishType"] == "top"
		climb["attempts"] == 1
		def grades = climb["grades"] as Map
		grades["system"] == "vScale"
		grades["grade"] == "V4"

		and: "tags example is correct"
		def tags = minimal["tags"] as List
		tags.size() == 1
		def tag = tags[0] as Map
		tag["id"] == "1"
		tag["name"] == "test"
		tag["category"] == "style"

		and: "checksums example is correct"
		def checksums = minimal["checksums"] as Map
		checksums["algorithm"] == "SHA-256"
	}

	def "should handle case insensitive component names"() {
		when: "getting schema info with different case"
		def result1 = schemaService.getSchemaInfo("MANIFEST")
		def result2 = schemaService.getSchemaInfo("Manifest")
		def result3 = schemaService.getSchemaInfo("manifest")

		then: "all return the same result"
		result1.containsKey("manifest")
		result2.containsKey("manifest")
		result3.containsKey("manifest")
	}

	def "should handle dateFormats as single word or camelCase"() {
		when: "getting date formats with different casings"
		def result1 = schemaService.getSchemaInfo("dateFormats")
		def result2 = schemaService.getSchemaInfo("dateformats")

		then: "both return date formats"
		result1.containsKey("dateFormats")
		result2.containsKey("dateFormats")
	}

	def "should return proper structure for enum definitions"() {
		when: "getting enums"
		def result = schemaService.getSchemaInfo("enums")
		def enums = result["enums"] as Map

		then: "enums have proper structure"
		enums.each { key, value ->
			assert key instanceof String
			assert value instanceof List || value instanceof Map
			if (value instanceof List) {
				value.each { item ->
					assert item instanceof String
				}
			}
		}
	}

	def "should load actual schema files"() {
		when: "getting manifest schema"
		def result = schemaService.getSchemaInfo("manifest")
		def schema = result["manifest"] as Map

		then: "schema has JSON Schema structure"
		schema.containsKey("\$schema") || schema.containsKey("type") || schema.containsKey("properties")
	}

	def "should handle missing schema file gracefully in complete schema"() {
		given: "a service that might fail to load some schemas"
		def service = new DefaultSchemaService()

		when: "building complete schema"
		def result = service.buildCompleteSchema()

		then: "result still contains other components"
		result.containsKey("dateFormats")
		result.containsKey("enums")
		result.containsKey("commonMistakes")
		result.containsKey("exampleData")
		// Individual schema loading might fail, but shouldn't crash
		notThrown(Exception)
	}
}
