package io.cldf.globalid

import spock.lang.Specification
import spock.lang.Unroll

class CLIDGeneratorSpec extends Specification {

	def "should generate deterministic location CLID for El Capitan"() {
		given: "El Capitan location data"
		def elCapitan = Location.builder()
				.country("US")
				.state("CA")
				.city("Yosemite Valley")
				.name("El Capitan")
				.coordinates(new Coordinates(37.734000, -119.637700))
				.isIndoor(false)
				.build()

		when: "generating CLID multiple times"
		def clid1 = CLIDGenerator.generateLocationCLID(elCapitan)
		def clid2 = CLIDGenerator.generateLocationCLID(elCapitan)

		then: "the same CLID is generated"
		clid1 != null
		clid1.startsWith("clid:location:")
		clid1 == clid2

		and: "the CLID is valid"
		CLIDGenerator.validate(clid1)

		and: "the CLID can be parsed"
		def parsed = CLIDGenerator.parse(clid1)
		parsed.type() == CLIDGenerator.EntityType.LOCATION
		parsed.namespace() == "clid"
		parsed.url() != null
	}

	def "should generate deterministic route CLID for The Nose"() {
		given: "El Capitan location"
		def elCapitan = Location.builder()
				.country("US")
				.state("CA")
				.city("Yosemite Valley")
				.name("El Capitan")
				.coordinates(new Coordinates(37.734000, -119.637700))
				.isIndoor(false)
				.build()
		def locationCLID = CLIDGenerator.generateLocationCLID(elCapitan)

		and: "The Nose route data"
		def theNose = new RouteModel.Route(
				"The Nose",
				"5.14a",
				RouteModel.RouteType.TRAD,
				new RouteModel.FirstAscent("Warren Harding", 1958),
				900.0)

		when: "generating route CLID"
		def routeCLID = CLIDGenerator.generateRouteCLID(locationCLID, theNose)
		def routeCLID2 = CLIDGenerator.generateRouteCLID(locationCLID, theNose)

		then: "deterministic CLID is generated"
		routeCLID != null
		routeCLID.startsWith("clid:route:")
		routeCLID == routeCLID2

		and: "CLID is valid"
		CLIDGenerator.validate(routeCLID)

		and: "can be parsed"
		def parsed = CLIDGenerator.parse(routeCLID)
		parsed.type() == CLIDGenerator.EntityType.ROUTE
	}

	@Unroll
	def "should generate CLID for #description location"() {
		given: "location data"
		def location = Location.builder()
				.country(country)
				.state(state)
				.city(city)
				.name(name)
				.coordinates(new Coordinates(lat, lon))
				.isIndoor(indoor)
				.build()

		when: "generating CLID"
		def clid = CLIDGenerator.generateLocationCLID(location)

		then: "valid CLID is generated"
		clid.startsWith("clid:location:")
		CLIDGenerator.validate(clid)

		where:
		description       | country | state | city       | name             | lat       | lon        | indoor
		"indoor gym"      | "US"    | "CO"  | "Boulder"  | "The Spot"       | 40.017900 | -105.281600| true
		"outdoor crag"    | "FR"    | null  | null       | "Ceuse"          | 44.506000 | 5.940000   | false
		"Fontainebleau"   | "FR"    | null  | null       | "Fontainebleau"  | 48.404000 | 2.692000   | false
		"Yosemite"        | "US"    | "CA"  | null       | "Yosemite Valley"| 37.720000 | -119.670000| false
	}

	@Unroll
	def "should generate route CLID for #routeType route"() {
		given: "a location"
		def location = Location.builder()
				.country("US")
				.state("CA")
				.name("Test Crag")
				.coordinates(new Coordinates(37.0, -119.0))
				.isIndoor(false)
				.build()
		def locationCLID = CLIDGenerator.generateLocationCLID(location)

		and: "route data"
		def route = new RouteModel.Route(
				routeName,
				grade,
				routeType,
				null,  // no first ascent
				height)

		when: "generating route CLID"
		def routeCLID = CLIDGenerator.generateRouteCLID(locationCLID, route)

		then: "valid CLID is generated"
		routeCLID.startsWith("clid:route:")
		CLIDGenerator.validate(routeCLID)

		where:
		routeName        | grade  | routeType          | height
		"Test Sport"     | "5.12a"| RouteModel.RouteType.SPORT    | 30.0
		"Test Trad"      | "5.10c"| RouteModel.RouteType.TRAD     | 100.0
		"Test Boulder"   | "V8"   | RouteModel.RouteType.BOULDER  | null
		"Ice Route"      | "WI4"  | RouteModel.RouteType.ICE      | 50.0
		"Mixed Route"    | "M7"   | RouteModel.RouteType.MIXED    | 75.0
	}

	def "should generate sector CLID"() {
		given: "location and sector"
		def location = Location.builder()
				.country("US")
				.state("CA")
				.name("El Capitan")
				.coordinates(new Coordinates(37.734000, -119.637700))
				.isIndoor(false)
				.build()
		def locationCLID = CLIDGenerator.generateLocationCLID(location)
		def sector = new Sector("Dawn Wall", 1)

		when: "generating sector CLID"
		def sectorCLID = CLIDGenerator.generateSectorCLID(locationCLID, sector)

		then: "valid CLID is generated"
		sectorCLID.startsWith("clid:sector:")
		CLIDGenerator.validate(sectorCLID)
	}

	def "should generate unique random CLIDs"() {
		when: "generating multiple random CLIDs"
		def clid1 = CLIDGenerator.generateRandomCLID(CLIDGenerator.EntityType.CLIMB)
		def clid2 = CLIDGenerator.generateRandomCLID(CLIDGenerator.EntityType.CLIMB)
		def clid3 = CLIDGenerator.generateRandomCLID(CLIDGenerator.EntityType.SESSION)

		then: "all are unique and valid"
		clid1 != clid2
		clid1 != clid3
		clid2 != clid3

		clid1.startsWith("clid:climb:")
		clid2.startsWith("clid:climb:")
		clid3.startsWith("clid:session:")

		CLIDGenerator.validate(clid1)
		CLIDGenerator.validate(clid2)
		CLIDGenerator.validate(clid3)
	}

	@Unroll
	def "should parse valid CLID: #clid"() {
		when: "parsing CLID"
		def parsed = CLIDGenerator.parse(clid)

		then: "correct components are extracted"
		parsed.namespace() == expectedNamespace
		parsed.type() == expectedType
		parsed.uuid() == expectedUuid
		parsed.fullId() == clid
		parsed.shortForm() == expectedUuid.substring(0, 8)
		parsed.url() == "https://crushlog.pro/g/${expectedUuid.substring(0, 8)}"

		where:
		clid                                                    | expectedNamespace | expectedType                      | expectedUuid
		"clid:route:550e8400-e29b-41d4-a716-446655440000"      | "clid"           | CLIDGenerator.EntityType.ROUTE    | "550e8400-e29b-41d4-a716-446655440000"
		"clid:location:660e8400-e29b-41d4-a716-446655440000"   | "clid"           | CLIDGenerator.EntityType.LOCATION | "660e8400-e29b-41d4-a716-446655440000"
		"clid:climb:770e8400-e29b-41d4-a716-446655440000"      | "clid"           | CLIDGenerator.EntityType.CLIMB    | "770e8400-e29b-41d4-a716-446655440000"
	}

	@Unroll
	def "should reject invalid CLID: #invalidClid with reason: #reason"() {
		when: "validating invalid CLID"
		def isValid = CLIDGenerator.validate(invalidClid)

		then: "validation fails"
		!isValid

		when: "parsing invalid CLID"
		CLIDGenerator.parse(invalidClid)

		then: "exception is thrown"
		thrown(IllegalArgumentException)

		where:
		invalidClid                           | reason
		null                                  | "null input"
		""                                    | "empty string"
		"invalid-id"                          | "wrong format"
		"route:123"                           | "missing namespace"
		"cldf:route:123"                      | "wrong namespace"
		"clid:invalid:123"                    | "invalid entity type"
		"clid:route:not-a-uuid"              | "invalid UUID"
		"clid:route"                          | "missing UUID part"
		"clid:route:550e8400:extra"           | "too many parts"
	}

	@Unroll
	def "should throw exception for invalid location data: #scenario"() {
		given: "invalid location"
		def location = Location.builder()
				.country(country)
				.name(name)
				.coordinates(new Coordinates(lat, lon))
				.isIndoor(false)
				.build()

		when: "generating CLID"
		CLIDGenerator.generateLocationCLID(location)

		then: "exception is thrown"
		def e = thrown(IllegalArgumentException)
		e.message.contains(expectedMessage)

		where:
		scenario               | country | name        | lat    | lon     | expectedMessage
		"invalid country code" | "USA"   | "Test"      | 40.0   | -105.0  | "Country must be ISO 3166-1 alpha-2"
		"empty name"           | "US"    | ""          | 40.0   | -105.0  | "Location name is required"
		"invalid latitude"     | "US"    | "Test"      | 91.0   | -105.0  | "Latitude must be between -90 and 90"
		"invalid longitude"    | "US"    | "Test"      | 40.0   | 181.0   | "Longitude must be between -180 and 180"
	}

	@Unroll
	def "should throw exception for invalid route data: #scenario"() {
		given: "valid location"
		def location = Location.builder()
				.country("US")
				.name("Test")
				.coordinates(new Coordinates(40.0, -105.0))
				.isIndoor(false)
				.build()
		def locationCLID = CLIDGenerator.generateLocationCLID(location)

		and: "invalid route"
		def route = new RouteModel.Route(
				name,
				grade,
				type,
				null,  // no first ascent
				null)  // no height

		when: "generating CLID"
		CLIDGenerator.generateRouteCLID(locationCLID, route)

		then: "exception is thrown"
		def e = thrown(IllegalArgumentException)
		e.message.contains(expectedMessage)

		where:
		scenario       | name     | grade    | type              | expectedMessage
		"empty name"   | ""       | "5.10"   | RouteModel.RouteType.SPORT   | "Route name is required"
		"null name"    | null     | "5.10"   | RouteModel.RouteType.SPORT   | "Route name is required"
		"empty grade"  | "Test"   | ""       | RouteModel.RouteType.SPORT   | "Route grade is required"
		"null grade"   | "Test"   | null     | RouteModel.RouteType.SPORT   | "Route grade is required"
		"null type"    | "Test"   | "5.10"   | null              | "Route type is required"
	}

	def "should generate short form of CLID"() {
		given: "a location"
		def location = Location.builder()
				.country("FR")
				.name("Ceuse")
				.coordinates(new Coordinates(44.506000, 5.940000))
				.isIndoor(false)
				.build()

		when: "generating CLID and short form"
		def clid = CLIDGenerator.generateLocationCLID(location)
		def shortForm = CLIDGenerator.toShortForm(clid)

		then: "short form is 8 characters"
		shortForm.length() == 8
		clid.contains(shortForm)
	}

	def "boulder problems should not require height"() {
		given: "location and boulder problem"
		def location = Location.builder()
				.country("FR")
				.name("Fontainebleau")
				.coordinates(new Coordinates(48.404000, 2.692000))
				.isIndoor(false)
				.build()
		def locationCLID = CLIDGenerator.generateLocationCLID(location)

		def boulder = new RouteModel.Route(
				"Rainbow Rocket",
				"8A",
				RouteModel.RouteType.BOULDER,
				null,  // no first ascent
				null)  // no height

		when: "generating CLID"
		def routeCLID = CLIDGenerator.generateRouteCLID(locationCLID, boulder)

		then: "CLID is generated successfully"
		routeCLID != null
		CLIDGenerator.validate(routeCLID)
	}

	def "different locations should generate different CLIDs"() {
		given: "two different locations"
		def location1 = Location.builder()
				.country("US")
				.name("Yosemite")
				.coordinates(new Coordinates(37.720000, -119.670000))
				.isIndoor(false)
				.build()

		def location2 = Location.builder()
				.country("US")
				.name("Joshua Tree")
				.coordinates(new Coordinates(33.873415, -115.900992))
				.isIndoor(false)
				.build()

		when: "generating CLIDs"
		def clid1 = CLIDGenerator.generateLocationCLID(location1)
		def clid2 = CLIDGenerator.generateLocationCLID(location2)

		then: "CLIDs are different"
		clid1 != clid2
	}
}
