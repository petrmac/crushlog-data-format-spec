package app.crushlog.cldf.qr.impl

import app.crushlog.cldf.models.Location
import app.crushlog.cldf.models.Route
import app.crushlog.cldf.models.enums.RouteType
import app.crushlog.cldf.qr.QRCodeData
import app.crushlog.cldf.qr.QROptions
import spock.lang.Specification
import spock.lang.Unroll

class QRDataGeneratorSpec extends Specification {

	def dataGenerator = new QRDataGenerator()

	def "should generate route data with JSON format"() {
		given:
		def location = Location.builder()
				.id(100)
				.clid("clid:v1:location:123")
				.name("Test Location")
				.isIndoor(false) // Required for blockchain to be added
				.build()
		def route = Route.builder()
				.id(1)
				.clid("clid:v1:route:550e8400-e29b-41d4-a716-446655440000")
				.name("Test Route")
				.grades(Route.Grades.builder()
				.yds("5.14a")
				.french("9a")
				.font("8C+")
				.build())
				.routeType(RouteType.ROUTE)
				.height(900.0)
				.build()

		def options = QROptions.builder()
				.format(QROptions.QRDataFormat.JSON)
				.includeIPFS(true)
				.ipfsHash("QmXk9abc123")
				.blockchainRecord(true)
				.blockchainNetwork("ethereum")
				.location(location) // Add location for blockchain metadata
				.build()

		when:
		QRCodeData data = dataGenerator.generateRouteData(route, options)

		then:
		data != null
		data.version == 1
		data.clid == "clid:v1:route:550e8400-e29b-41d4-a716-446655440000"
		data.url != null
		data.url.contains("crushlog.pro")
		data.ipfsHash == "QmXk9abc123"
		data.routeData != null
		data.routeData["name"] == "Test Route"
		data.routeData["grade"] == "5.14a"
		data.routeData["gradeSystem"] == "yds"
		data.routeData["type"] == "route"
		data.routeData["height"] == 900.0
		data.metadata != null
		data.metadata["blockchain"] == true
		data.metadata["network"] == "ethereum"
	}

	def "should generate route data with URL format"() {
		given:
		def route = Route.builder()
				.id(1)
				.clid("clid:v1:route:550e8400-e29b-41d4-a716-446655440000")
				.name("Test Route")
				.build()

		def options = QROptions.builder()
				.format(QROptions.QRDataFormat.URL)
				.baseUrl("https://example.com")
				.build()

		when:
		QRCodeData data = dataGenerator.generateRouteData(route, options)

		then:
		data != null
		data.url == "https://example.com/g/550e8400"
		// hasOfflineData() returns true if routeData is not null
		data.hasOfflineData() == true
	}

	def "should generate route data with CUSTOM_URI format"() {
		given:
		def route = Route.builder()
				.id(1)
				.clid("clid:v1:route:550e8400-e29b-41d4-a716-446655440000")
				.name("Test Route")
				.grades(Route.Grades.builder().vScale("V10").build())
				.routeType(RouteType.BOULDER)
				.build()

		def options = QROptions.builder()
				.format(QROptions.QRDataFormat.CUSTOM_URI)
				.ipfsHash("QmTest123")
				.build()

		when:
		QRCodeData data = dataGenerator.generateRouteData(route, options)
		String customUri = dataGenerator.generateCustomUri(route, options)

		then:
		data != null
		data.url != null
		// generateRouteData always creates the standard URL format
		data.url == "https://crushlog.pro/g/550e8400"
		// Check the custom URI separately
		customUri.startsWith("cldf://global/route/550e8400-e29b-41d4-a716-446655440000")
		customUri.contains("v=1")
		customUri.contains("cldf=QmTest123")
		customUri.contains("name=Test+Route") // URLEncoder uses + for spaces
		customUri.contains("grade=V10")
		customUri.contains("gradeSystem=vScale")
	}

	def "should generate route data without CLID and auto-generate one"() {
		given:
		def route = Route.builder()
				.id(1)
				.name("Test Route")
				.build()

		def options = QROptions.builder().build()

		when:
		QRCodeData data = dataGenerator.generateRouteData(route, options)

		then:
		data != null
		data.clid != null
		data.clid.startsWith("clid:v1:route:")
		data.url != null
	}

	def "should generate location data with JSON format"() {
		given:
		def location = Location.builder()
				.id(1)
				.clid("clid:location:loc12345-e29b-41d4-a716-446655440000")
				.name("El Capitan")
				.country("US")
				.state("CA")
				.city("Yosemite Valley")
				.isIndoor(false)
				.build()

		def options = QROptions.builder()
				.format(QROptions.QRDataFormat.JSON)
				.build()

		when:
		QRCodeData data = dataGenerator.generateLocationData(location, options)

		then:
		data != null
		data.version == 1
		data.clid == "clid:location:loc12345-e29b-41d4-a716-446655440000"
		data.url != null
		data.url.contains("/l/loc12345")
		data.locationData != null
		data.locationData["name"] == "El Capitan"
		data.locationData["country"] == "US"
		data.locationData["state"] == "CA"
		data.locationData["city"] == "Yosemite Valley"
		data.locationData["indoor"] == false
	}

	def "should generate location data with URL format"() {
		given:
		def location = Location.builder()
				.id(1)
				.clid("clid:location:loc12345-e29b-41d4-a716-446655440000")
				.name("Test Gym")
				.isIndoor(true)
				.build()

		def options = QROptions.builder()
				.format(QROptions.QRDataFormat.URL)
				.baseUrl("https://mysite.com")
				.build()

		when:
		QRCodeData data = dataGenerator.generateLocationData(location, options)

		then:
		data != null
		data.url == "https://mysite.com/l/loc12345"
	}

	def "should handle location with blockchain but outdoor"() {
		given:
		def location = Location.builder()
				.id(1)
				.clid("clid:location:test")
				.name("Outdoor Crag")
				.isIndoor(false)
				.build()

		def options = QROptions.builder()
				.location(location)
				.blockchainRecord(true)
				.blockchainNetwork("solana")
				.build()

		when:
		QRCodeData data = dataGenerator.generateLocationData(location, options)

		then:
		data != null
		data.metadata != null
		data.metadata["blockchain"] == true
		data.metadata["network"] == "solana"
	}

	def "should handle location without blockchain if indoor"() {
		given:
		def location = Location.builder()
				.id(1)
				.clid("clid:location:test")
				.name("Indoor Gym")
				.isIndoor(true)
				.build()

		def options = QROptions.builder()
				.location(location)
				.blockchainRecord(true)
				.build()

		when:
		QRCodeData data = dataGenerator.generateLocationData(location, options)

		then:
		data != null
		data.metadata != null
		data.metadata["blockchain"] == null // blockchain not set for indoor locations
	}

	@Unroll
	def "should select correct grade based on route type: #routeType"() {
		given:
		def grades = Route.Grades.builder()
				.yds("5.11a")
				.french("6c")
				.vScale("V5")
				.font("6B+")
				.uiaa("VII+")
				.build()

		def route = Route.builder()
				.id(1)
				.clid("clid:route:test")
				.name("Test")
				.grades(grades)
				.routeType(routeType)
				.build()

		def options = QROptions.builder().build()

		when:
		QRCodeData data = dataGenerator.generateRouteData(route, options)

		then:
		data.routeData["grade"] == expectedGrade
		data.routeData["gradeSystem"] == expectedGradeSystem

		where:
		routeType             | expectedGrade | expectedGradeSystem
		RouteType.ROUTE       | "5.11a"       | "yds"
		RouteType.BOULDER     | "V5"          | "vScale"
	}

	def "should handle route with only specific grade system"() {
		given:
		def route = Route.builder()
				.id(1)
				.clid("clid:route:test")
				.name("Test")
				.grades(Route.Grades.builder()
				.font("7A")
				.build())
				.routeType(RouteType.BOULDER)
				.build()

		def options = QROptions.builder().build()

		when:
		QRCodeData data = dataGenerator.generateRouteData(route, options)

		then:
		data.routeData["grade"] == "7A"
		data.routeData["gradeSystem"] == "font"
	}

	def "should handle route with no grades"() {
		given:
		def route = Route.builder()
				.id(1)
				.clid("clid:route:test")
				.name("Test")
				.routeType(RouteType.ROUTE)
				.build()

		def options = QROptions.builder().build()

		when:
		QRCodeData data = dataGenerator.generateRouteData(route, options)

		then:
		data.routeData["grade"] == null
		data.routeData["gradeSystem"] == null
	}

	def "should include location data in route QR when location provided in options"() {
		given:
		def route = Route.builder()
				.id(1)
				.clid("clid:route:test")
				.name("Test Route")
				.build()

		def location = Location.builder()
				.id(100)
				.clid("clid:location:test")
				.name("Test Crag")
				.country("US")
				.build()

		def options = QROptions.builder()
				.location(location)
				.build()

		when:
		QRCodeData data = dataGenerator.generateRouteData(route, options)

		then:
		data.locationData != null
		data.locationData["name"] == "Test Crag"
		data.locationData["country"] == "US"
	}

	def "should generate valid JSON from QRCodeData"() {
		given:
		def route = Route.builder()
				.id(1)
				.clid("clid:route:test")
				.name("Test Route")
				.build()

		def options = QROptions.builder().build()

		when:
		QRCodeData data = dataGenerator.generateRouteData(route, options)
		String json = dataGenerator.toJson(data)

		then:
		json != null
		json.contains("\"v\":1")
		json.contains("\"clid\":\"clid:route:test\"")
		json.contains("\"name\":\"Test Route\"")
	}
}
