package io.cldf.models

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import spock.lang.Specification

import io.cldf.models.enums.PredefinedTagKey

class TagSpec extends Specification {

	ObjectMapper objectMapper

	def setup() {
		objectMapper = new ObjectMapper()
		objectMapper.registerModule(new JavaTimeModule())
	}

	def "should build tag with required fields"() {
		when: "creating a tag"
		def tag = Tag.builder()
				.id("tag-1")
				.name("crimpy")
				.build()

		then: "required fields are set"
		tag.id == "tag-1"
		tag.name == "crimpy"

		and: "defaults are applied"
		!tag.isPredefined
	}

	def "should build predefined tag"() {
		when: "creating a predefined tag"
		def tag = Tag.builder()
				.id("tag-1")
				.name("crimpy")
				.isPredefined(true)
				.predefinedTagKey(PredefinedTagKey.CRIMPY)
				.category("holds")
				.color("#FF5722")
				.build()

		then: "all fields are set"
		tag.id == "tag-1"
		tag.name == "crimpy"
		tag.isPredefined
		tag.predefinedTagKey == PredefinedTagKey.CRIMPY
		tag.category == "holds"
		tag.color == "#FF5722"
	}

	def "should serialize tag to JSON"() {
		given: "a tag"
		def tag = Tag.builder()
				.id("tag-1")
				.name("slopers")
				.isPredefined(true)
				.predefinedTagKey(PredefinedTagKey.SLOPERS)
				.category("holds")
				.color("#4CAF50")
				.build()

		when: "serializing to JSON"
		def json = objectMapper.writeValueAsString(tag)

		then: "JSON contains expected fields"
		json.contains('"id":"tag-1"')
		json.contains('"name":"slopers"')
		json.contains('"isPredefined":true')
		json.contains('"predefinedTagKey":"slopers"')
		json.contains('"category":"holds"')
		json.contains('"color":"#4CAF50"')
	}

	def "should deserialize tag from JSON"() {
		given: "JSON representation"
		def json = '''
            {
                "id": "tag-1",
                "name": "overhang",
                "isPredefined": true,
                "predefinedTagKey": "overhang",
                "category": "angle",
                "color": "#9C27B0"
            }
        '''

		when: "deserializing from JSON"
		def tag = objectMapper.readValue(json, Tag)

		then: "tag is created correctly"
		tag.id == "tag-1"
		tag.name == "overhang"
		tag.isPredefined
		tag.predefinedTagKey == PredefinedTagKey.OVERHANG
		tag.category == "angle"
		tag.color == "#9C27B0"
	}

	def "should handle all predefined tag keys"() {
		expect: "all predefined keys are valid"
		PredefinedTagKey.fromValue(key) != null

		where:
		key << [
			"overhang",
			"slab",
			"vertical",
			"roof",
			"crack",
			"corner",
			"arete",
			"dyno",
			"crimpy",
			"slopers",
			"jugs",
			"pockets",
			"technical",
			"powerful",
			"endurance"
		]
	}

	def "should create custom tag"() {
		when: "creating a custom tag"
		def tag = Tag.builder()
				.id("custom-1")
				.name("My Custom Tag")
				.isPredefined(false)
				.category("custom")
				.color("#000000")
				.build()

		then: "custom tag is created"
		tag.id == "custom-1"
		tag.name == "My Custom Tag"
		!tag.isPredefined
		tag.predefinedTagKey == null
		tag.category == "custom"
	}

	def "should create TagsFile with tags"() {
		given: "a mix of predefined and custom tags"
		def tags = [
			Tag.builder()
			.id("1")
			.name("crimpy")
			.isPredefined(true)
			.predefinedTagKey(PredefinedTagKey.CRIMPY)
			.build(),
			Tag.builder()
			.id("2")
			.name("My Project")
			.isPredefined(false)
			.build()
		]

		when: "creating TagsFile"
		def file = new TagsFile()
		file.tags = tags

		then: "file contains tags"
		file.tags.size() == 2
		file.tags[0].isPredefined
		!file.tags[1].isPredefined
	}
}
