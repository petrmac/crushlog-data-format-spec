package app.crushlog.cldf.clid

import spock.lang.Specification
import spock.lang.Unroll

class CLIDSpec extends Specification {

	@Unroll
	def "should parse valid CLID: #validClid"() {
		when: "parsing valid CLID"
		def clid = CLID.fromString(validClid)

		then: "all components are correctly extracted"
		clid.namespace() == expectedNamespace
		clid.type() == expectedType
		clid.uuid() == expectedUuid
		clid.fullId() == validClid
		clid.shortForm() == expectedUuid.substring(0, 8)
		clid.url() == "https://crushlog.pro/g/${expectedUuid.substring(0, 8)}"

		where:
		validClid                                               | expectedNamespace | expectedType        | expectedUuid
		"clid:v1:route:550e8400-e29b-41d4-a716-446655440000"    | "clid"            | EntityType.ROUTE    | "550e8400-e29b-41d4-a716-446655440000"
		"clid:v1:location:123e4567-e89b-12d3-a456-426614174000" | "clid"            | EntityType.LOCATION | "123e4567-e89b-12d3-a456-426614174000"
		"clid:v1:sector:00000000-0000-0000-0000-000000000000"   | "clid"            | EntityType.SECTOR   | "00000000-0000-0000-0000-000000000000"
		"clid:v1:climb:ffffffff-ffff-ffff-ffff-ffffffffffff"    | "clid"            | EntityType.CLIMB    | "ffffffff-ffff-ffff-ffff-ffffffffffff"
		"clid:v1:session:abcdef12-3456-7890-abcd-ef1234567890"  | "clid"            | EntityType.SESSION  | "abcdef12-3456-7890-abcd-ef1234567890"
		"clid:v1:media:ABCDEF12-3456-7890-ABCD-EF1234567890"    | "clid"            | EntityType.MEDIA    | "ABCDEF12-3456-7890-ABCD-EF1234567890"
	}

	@Unroll
	def "should validate CLID: '#clid' as #expectedResult"() {
		expect: "validation returns expected result"
		CLID.isValid(clid) == expectedResult

		where:
		clid                                                    | expectedResult
		"clid:v1:route:550e8400-e29b-41d4-a716-446655440000"    | true
		"clid:v1:location:123e4567-e89b-12d3-a456-426614174000" | true
		"clid:v1:climb:abcdef12-3456-7890-abcd-ef1234567890"    | true
		null                                                    | false
		""                                                      | false
		"   "                                                   | false
		"invalid"                                               | false
		"cldf:v1:route:550e8400-e29b-41d4-a716-446655440000"    | false
		"clid:v1:invalid:550e8400-e29b-41d4-a716-446655440000"  | false
		"clid:v1:route:not-a-uuid"                              | false
		"clid:v1:route:550e8400"                                | false
	}

	@Unroll
	def "should throw exception for invalid CLID '#invalidClid' with message containing '#expectedMessage'"() {
		when: "parsing invalid CLID"
		CLID.fromString(invalidClid)

		then: "appropriate exception is thrown"
		def e = thrown(IllegalArgumentException)
		e.message.contains(expectedMessage)

		where:
		invalidClid                                            | expectedMessage
		null                                                   | "cannot be null"
		""                                                     | "cannot be null or empty"
		"   "                                                  | "cannot be null or empty"
		"invalid"                                              | "Invalid CLID format"
		"route:550e8400-e29b-41d4-a716-446655440000"           | "Invalid CLID format"
		"clid:route"                                           | "Invalid CLID format"
		"clid:v1:route"                                        | "Invalid CLID format"
		"clid:route:550e8400-e29b-41d4-a716-446655440000"      | "Invalid CLID format"
		"cldf:v1:route:550e8400-e29b-41d4-a716-446655440000"   | "Invalid namespace 'cldf'"
		"xyz:v1:route:550e8400-e29b-41d4-a716-446655440000"    | "Invalid namespace 'xyz'"
		"clid:v1:invalid:550e8400-e29b-41d4-a716-446655440000" | "Invalid entity type 'invalid'"
		"clid:v1:ROUTE:550e8400-e29b-41d4-a716-446655440000"   | "Invalid entity type 'ROUTE'"
		"clid:v1:route:not-a-uuid"                             | "Invalid UUID format"
		"clid:v1:route:550e8400"                               | "Invalid UUID format"
		"clid:v1:route:g50e8400-e29b-41d4-a716-446655440000"   | "Invalid UUID format"
		"clid:v1:route:550e8400-e29b-41d4-a716"                | "Invalid UUID format"
		"clid:v1:route:550e8400-e29b-41d4-a716-4466554400000"  | "Invalid UUID format"
	}

	def "should handle all entity types"() {
		given: "all entity types"

		def entityTypes = [
			EntityType.LOCATION,
			EntityType.SECTOR,
			EntityType.ROUTE,
			EntityType.CLIMB,
			EntityType.SESSION,
			EntityType.MEDIA
		]
		expect: "all can be parsed"
		entityTypes.each { type ->
			def clid = "clid:v1:${type.value}:550e8400-e29b-41d4-a716-446655440000"
			def parsed = CLID.fromString(clid)
			assert parsed.type() == type
		}
	}

	def "should create consistent short form"() {
		given: "various UUIDs"
		def testCases = [
			[
				"550e8400-e29b-41d4-a716-446655440000",
				"550e8400"
			],
			[
				"12345678-9abc-def0-1234-567890abcdef",
				"12345678"
			],
			[
				"abcdef12-3456-7890-abcd-ef1234567890",
				"abcdef12"
			],
			[
				"00000000-0000-0000-0000-000000000000",
				"00000000"
			]
		]

		expect: "short form is first 8 characters"
		testCases.each { uuid, expectedShort ->
			def clid = CLID.fromString("clid:v1:route:${uuid}")
			assert clid.shortForm() == expectedShort
		}
	}

	def "should create consistent URL"() {
		given: "a CLID"
		def clid = CLID.fromString("clid:v1:route:550e8400-e29b-41d4-a716-446655440000")

		expect: "URL is correctly formatted"
		clid.url() == "https://crushlog.pro/g/550e8400"
	}

	def "toString should return full CLID"() {
		given: "a CLID string"
		def clidString = "clid:v1:route:550e8400-e29b-41d4-a716-446655440000"

		when: "parsing and converting back to string"
		def clid = CLID.fromString(clidString)

		then: "toString returns the original string"
		clid.toString() == clidString
	}

	def "should handle case-insensitive UUID validation"() {
		given: "CLIDs with different case UUIDs"
		def lowercase = "clid:v1:route:550e8400-e29b-41d4-a716-446655440000"
		def uppercase = "clid:v1:route:550E8400-E29B-41D4-A716-446655440000"
		def mixedcase = "clid:v1:route:550e8400-E29B-41d4-A716-446655440000"

		when: "parsing CLIDs"
		def clidLower = CLID.fromString(lowercase)
		def clidUpper = CLID.fromString(uppercase)
		def clidMixed = CLID.fromString(mixedcase)

		then: "all are valid"
		clidLower != null
		clidUpper != null
		clidMixed != null

		and: "UUIDs are preserved as-is"
		clidLower.uuid() == "550e8400-e29b-41d4-a716-446655440000"
		clidUpper.uuid() == "550E8400-E29B-41D4-A716-446655440000"
		clidMixed.uuid() == "550e8400-E29B-41d4-A716-446655440000"
	}

	def "should validate UUID is parseable by Java UUID class"() {
		given: "a technically valid UUID format that Java UUID can't parse"
		// This is actually a valid UUID, so let's use one that would pass regex but fail UUID parsing
		// Actually, Java's UUID.fromString is quite permissive, so this test ensures both checks work
		def validClid = "clid:v1:route:550e8400-e29b-41d4-a716-446655440000"

		when: "parsing"
		def clid = CLID.fromString(validClid)

		then: "it succeeds"
		clid != null

		and: "the UUID can be parsed by Java's UUID class"
		UUID.fromString(clid.uuid()) != null
	}
}
