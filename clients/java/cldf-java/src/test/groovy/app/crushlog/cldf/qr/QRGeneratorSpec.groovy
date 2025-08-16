package app.crushlog.cldf.qr

import com.fasterxml.jackson.databind.ObjectMapper
import app.crushlog.cldf.clid.CLIDGenerator
import app.crushlog.cldf.models.Location
import app.crushlog.cldf.models.Route
import app.crushlog.cldf.models.enums.RouteType
import spock.lang.Specification
import spock.lang.Unroll

class QRGeneratorSpec extends Specification {

	def objectMapper = new ObjectMapper()

	def "should generate hybrid QR code for route with CLID"() {
		given: "a route with CLID"
		def route = Route.builder()
				.id(1)
				.locationId(100)
				.clid("clid:v1:route:550e8400-e29b-41d4-a716-446655440000")
				.name("The Nose")
				.grades(Route.Grades.builder()
				.yds("5.14a")
				.build())
				.routeType(RouteType.ROUTE)
				.height(900.0)
				.build()

		def options = QRGenerator.QROptions.builder()
				.includeIPFS(true)
				.ipfsHash("QmXk9abc123")
				.build()

		when: "generating hybrid QR code"
		def qrData = QRGenerator.generateHybrid(route, "https://crushlog.pro", options)
		def json = objectMapper.readTree(qrData)

		then: "QR data contains all expected fields"
		json.get("v").asInt() == 1
		json.get("clid").asText() == "clid:v1:route:550e8400-e29b-41d4-a716-446655440000"
		json.get("url").asText() == "https://crushlog.pro/g/550e8400"
		json.get("cldf").asText() == "QmXk9abc123"
		json.get("route").get("name").asText() == "The Nose"
		json.get("route").get("grade").asText() == "5.14a"
		json.get("route").get("type").asText() == "route"
		json.get("route").get("height").asDouble() == 900.0
	}

	def "should generate QR code for route without CLID"() {
		given: "a route without CLID"
		def route = Route.builder()
				.id(1)
				.locationId(100)
				.name("Test Boulder")
				.grades(Route.Grades.builder()
				.vScale("V8")
				.build())
				.routeType(RouteType.BOULDER)
				.build()

		def options = QRGenerator.QROptions.builder().build()

		when: "generating hybrid QR code"
		def qrData = QRGenerator.generateHybrid(route, null, options)
		def json = objectMapper.readTree(qrData)

		then: "QR data contains generated CLID"
		json.get("v").asInt() == 1
		json.get("clid").asText().startsWith("clid:v1:route:")
		json.get("url").asText().startsWith("https://crushlog.pro/g/")
		json.get("route").get("name").asText() == "Test Boulder"
		json.get("route").get("grade").asText() == "V8"
		json.get("route").get("type").asText() == "boulder"
	}

	def "should generate simple URL QR code"() {
		given: "a route with CLID"
		def route = Route.builder()
				.clid("clid:v1:route:12345678-e29b-41d4-a716-446655440000")
				.name("Test Route")
				.build()

		when: "generating simple QR code"
		def url = QRGenerator.generateSimple(route, "https://example.com")

		then: "URL uses short CLID"
		url == "https://example.com/g/12345678"
	}

	def "should throw exception for route without CLID in simple generation"() {
		given: "a route without CLID"
		def route = Route.builder()
				.id(456)
				.locationId(123)
				.name("No CLID Route")
				.build()

		when: "generating simple QR code"
		QRGenerator.generateSimple(route, "https://example.com")

		then: "exception is thrown"
		thrown(QRGenerator.QRGenerationException)
	}

	def "should generate custom URI for mobile apps"() {
		given: "a route with CLID"
		def route = Route.builder()
				.clid("clid:v1:route:abc12345-e29b-41d4-a716-446655440000")
				.name("Test Route")
				.grades(Route.Grades.builder()
				.french("7a")
				.build())
				.build()

		def options = QRGenerator.QROptions.builder()
				.ipfsHash("QmTest123")
				.build()

		when: "generating custom URI"
		def uri = QRGenerator.generateCustomURI(route, options)

		then: "URI contains expected parameters"
		uri.startsWith("cldf://global/route/abc12345-e29b-41d4-a716-446655440000")
		uri.contains("v=1")
		uri.contains("cldf=QmTest123")
		uri.contains("name=Test+Route")
		uri.contains("grade=7a")
	}

	def "should generate QR code for location"() {
		given: "a location with CLID"
		def location = Location.builder()
				.id(1)
				.clid("clid:v1:location:loc12345-e29b-41d4-a716-446655440000")
				.name("El Capitan")
				.country("US")
				.state("CA")
				.city("Yosemite Valley")
				.isIndoor(false)
				.build()

		when: "generating location QR code"
		def qrData = QRGenerator.generateLocationQR(location, "https://crushlog.pro")
		def json = objectMapper.readTree(qrData)

		then: "QR data contains location information"
		json.get("v").asInt() == 1
		json.get("clid").asText() == "clid:v1:location:loc12345-e29b-41d4-a716-446655440000"
		json.get("url").asText() == "https://crushlog.pro/l/loc12345"
		json.get("location").get("name").asText() == "El Capitan"
		json.get("location").get("country").asText() == "US"
		json.get("location").get("state").asText() == "CA"
		json.get("location").get("city").asText() == "Yosemite Valley"
		json.get("location").get("indoor").asBoolean() == false
	}

	def "should include location data in route QR when provided"() {
		given: "a route and location"
		def location = Location.builder()
				.id(100)
				.clid("clid:v1:location:loc12345-e29b-41d4-a716-446655440000")
				.name("Test Crag")
				.country("US")
				.isIndoor(false)
				.build()

		def route = Route.builder()
				.id(1)
				.locationId(100)
				.clid("clid:v1:route:550e8400-e29b-41d4-a716-446655440000")
				.name("Test Route")
				.routeType(RouteType.ROUTE)
				.build()

		def options = QRGenerator.QROptions.builder()
				.location(location)
				.blockchainRecord(true)
				.blockchainNetwork("ethereum")
				.build()

		when: "generating hybrid QR code"
		def qrData = QRGenerator.generateHybrid(route, null, options)
		def json = objectMapper.readTree(qrData)

		then: "QR data contains location information"
		json.get("loc").get("clid").asText() == "clid:v1:location:loc12345-e29b-41d4-a716-446655440000"
		json.get("loc").get("name").asText() == "Test Crag"
		json.get("loc").get("country").asText() == "US"
		json.get("loc").get("indoor").asBoolean() == false
		json.get("meta").get("blockchain").asBoolean() == true
		json.get("meta").get("network").asText() == "ethereum"
	}

	def "should throw exception for null route"() {
		when: "generating QR code with null route"
		QRGenerator.generateHybrid(null, "https://example.com", QRGenerator.QROptions.builder().build())

		then: "exception is thrown"
		thrown(NullPointerException)
	}

	def "should throw exception for route without IDs in simple generation"() {
		given: "a route without CLID or IDs"
		def route = Route.builder()
				.name("Test Route")
				.build()

		when: "generating simple QR code"
		QRGenerator.generateSimple(route, "https://example.com")

		then: "exception is thrown"
		thrown(QRGenerator.QRGenerationException)
	}

	@Unroll
	def "should extract primary grade for #routeType route"() {
		given: "a route with grades"
		def grades = Route.Grades.builder()
		if (boulderGrade) grades.vScale(boulderGrade)
		if (routeGrade) grades.yds(routeGrade)
		if (frenchGrade) grades.french(frenchGrade)

		def route = Route.builder()
				.id(1)
				.clid("clid:v1:route:test")
				.name("Test")
				.grades(grades.build())
				.routeType(routeType)
				.build()

		def options = QRGenerator.QROptions.builder().build()

		when: "generating QR code"
		def qrData = QRGenerator.generateHybrid(route, null, options)
		def json = objectMapper.readTree(qrData)

		then: "correct grade is selected"
		json.get("route").get("grade")?.asText() == expectedGrade

		where:
		routeType         | boulderGrade | routeGrade | frenchGrade | expectedGrade
		RouteType.BOULDER | "V8"         | null       | null        | "V8"
		RouteType.BOULDER | null         | "5.12a"    | null        | "5.12a"
		RouteType.ROUTE   | null         | "5.12a"    | null        | "5.12a"
		RouteType.ROUTE   | null         | null       | "7a"        | "7a"
		RouteType.ROUTE   | "V8"         | null       | null        | "V8"
	}

	def "should handle blockchain options correctly"() {
		given: "an outdoor location and route"
		def location = Location.builder()
				.id(1)
				.isIndoor(false)
				.build()

		def route = Route.builder()
				.id(1)
				.clid("clid:v1:route:test")
				.name("Outdoor Route")
				.build()

		def options = QRGenerator.QROptions.builder()
				.location(location)
				.blockchainRecord(true)
				.build()

		when: "generating QR code"
		def qrData = QRGenerator.generateHybrid(route, null, options)
		def json = objectMapper.readTree(qrData)

		then: "blockchain metadata is included"
		json.get("meta").get("blockchain").asBoolean() == true
	}

	def "should not include blockchain for indoor routes"() {
		given: "an indoor location and route"
		def location = Location.builder()
				.id(1)
				.isIndoor(true)
				.build()

		def route = Route.builder()
				.id(1)
				.clid("clid:v1:route:test")
				.name("Indoor Route")
				.build()

		def options = QRGenerator.QROptions.builder()
				.location(location)
				.blockchainRecord(true)
				.build()

		when: "generating QR code"
		def qrData = QRGenerator.generateHybrid(route, null, options)
		def json = objectMapper.readTree(qrData)

		then: "blockchain metadata is not included"
		!json.get("meta").has("blockchain")
	}
}
