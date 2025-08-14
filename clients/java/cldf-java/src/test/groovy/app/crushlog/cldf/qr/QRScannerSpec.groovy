package app.crushlog.cldf.qr

import app.crushlog.cldf.models.enums.RouteType
import spock.lang.Specification
import spock.lang.Unroll

class QRScannerSpec extends Specification {

	def "should parse JSON QR data with CLID"() {
		given: "JSON QR data"
		def jsonData = """
        {
            "v": 1,
            "clid": "clid:route:550e8400-e29b-41d4-a716-446655440000",
            "url": "https://crushlog.pro/g/550e8400",
            "cldf": "QmXk9abc123",
            "route": {
                "name": "The Nose",
                "grade": "5.14a",
                "gradeSystem": "yds",
                "type": "route",
                "height": 900.0
            },
            "loc": {
                "clid": "clid:location:660e8400-e29b-41d4-a716-446655440000",
                "name": "El Capitan",
                "country": "US",
                "state": "CA",
                "indoor": false
            },
            "meta": {
                "blockchain": true
            }
        }
        """

		when: "parsing QR data"
		def data = QRScanner.parseString(jsonData)

		then: "data is correctly parsed"
		data.version == 1
		data.clid == "clid:route:550e8400-e29b-41d4-a716-446655440000"
		data.url == "https://crushlog.pro/g/550e8400"
		data.ipfsHash == "QmXk9abc123"
		data.hasOfflineData == true
		data.blockchainVerified == true

		and: "route info is correct"
		data.route.name == "The Nose"
		data.route.grade == "5.14a"
		data.route.type == "route"
		data.route.height == 900.0

		and: "location info is correct"
		data.location.clid == "clid:location:660e8400-e29b-41d4-a716-446655440000"
		data.location.name == "El Capitan"
		data.location.country == "US"
		data.location.state == "CA"
		data.location.indoor == false
	}

	def "should parse URL format QR data"() {
		given: "URL QR data"
		def url = "https://crushlog.pro/g/550e8400"

		when: "parsing URL"
		def data = QRScanner.parseString(url)

		then: "data is correctly parsed"
		data.version == 1
		data.url == url
		data.shortClid == "550e8400"
		data.hasOfflineData == false
	}

	def "should parse custom URI format"() {
		given: "custom URI"
		def uri = "cldf://global/route/550e8400-e29b-41d4-a716-446655440000?v=1&cldf=QmTest&name=Test+Route&grade=V8&gradeSystem=vScale"

		when: "parsing URI"
		def data = QRScanner.parseString(uri)

		then: "data is correctly parsed"
		data.version == 1
		data.clid == "clid:route:550e8400-e29b-41d4-a716-446655440000"
		data.ipfsHash == "QmTest"
		data.hasOfflineData == true
		data.route.name == "Test Route"
		data.route.grade == "V8"
	}

	def "should handle JSON without metadata"() {
		given: "minimal JSON QR data"
		def jsonData = """
        {
            "v": 1,
            "clid": "clid:route:test-uuid",
            "route": {
                "name": "Simple Route"
            }
        }
        """

		when: "parsing QR data"
		def data = QRScanner.parseString(jsonData)

		then: "data is correctly parsed"
		data.version == 1
		data.clid == "clid:route:test-uuid"
		data.route.name == "Simple Route"
		data.hasOfflineData == true
		data.blockchainVerified == false
	}

	def "should convert QR data to Route object"() {
		given: "parsed QR data"
		def qrData = ParsedQRData.builder()
				.clid("clid:route:test-uuid")
				.route(ParsedQRData.RouteInfo.builder()
				.id(1)
				.name("Test Route")
				.grade("5.12a")
				.gradeSystem("yds")
				.type("route")
				.height(30.0)
				.build())
				.location(ParsedQRData.LocationInfo.builder()
				.id(100)
				.build())
				.build()

		when: "converting to Route"
		def routeOpt = QRScanner.toRouteStatic(qrData)

		then: "Route is created correctly"
		routeOpt.isPresent()
		def route = routeOpt.get()
		route.clid == "clid:route:test-uuid"
		route.id == 1
		route.name == "Test Route"
		route.grades.yds == "5.12a"  // Grade is detected as YDS
		route.routeType == RouteType.ROUTE
		route.height == 30.0
		route.locationId == 100
	}

	def "should convert QR data to Location object"() {
		given: "parsed QR data"
		def qrData = ParsedQRData.builder()
				.location(ParsedQRData.LocationInfo.builder()
				.clid("clid:location:test-uuid")
				.id(100)
				.name("Test Crag")
				.country("US")
				.state("CA")
				.city("Yosemite")
				.indoor(false)
				.build())
				.build()

		when: "converting to Location"
		def locationOpt = QRScanner.toLocationStatic(qrData)

		then: "Location is created correctly"
		locationOpt.isPresent()
		def location = locationOpt.get()
		location.clid == "clid:location:test-uuid"
		location.id == 100
		location.name == "Test Crag"
		location.country == "US"
		location.state == "CA"
		location.city == "Yosemite"
		location.isIndoor == false
	}

	def "should return empty Optional when no route data"() {
		given: "QR data without route"
		def qrData = ParsedQRData.builder()
				.clid("clid:location:test")
				.build()

		when: "converting to Route"
		def routeOpt = QRScanner.toRouteStatic(qrData)

		then: "Optional is empty"
		!routeOpt.isPresent()
	}

	def "should return empty Optional when no location data"() {
		given: "QR data without location"
		def qrData = ParsedQRData.builder()
				.clid("clid:route:test")
				.build()

		when: "converting to Location"
		def locationOpt = QRScanner.toLocationStatic(qrData)

		then: "Optional is empty"
		!locationOpt.isPresent()
	}

	def "should throw exception for null QR data"() {
		when: "parsing null data"
		QRScanner.parseString(null)

		then: "exception is thrown"
		thrown(QRScanner.QRParseException)
	}

	def "should throw exception for empty QR data"() {
		when: "parsing empty data"
		QRScanner.parseString("")

		then: "exception is thrown"
		thrown(QRScanner.QRParseException)
	}

	def "should throw exception for unrecognized format"() {
		when: "parsing unrecognized format"
		QRScanner.parseString("random string that is not QR data")

		then: "exception is thrown"
		thrown(QRScanner.QRParseException)
	}

	def "should throw exception for invalid JSON"() {
		when: "parsing invalid JSON"
		QRScanner.parseString("{invalid json}")

		then: "exception is thrown"
		thrown(QRScanner.QRParseException)
	}

	@Unroll
	def "should parse different URL formats: #url"() {
		when: "parsing URL"
		def data = QRScanner.parseString(url)

		then: "data is parsed"
		data.url == url
		data.shortClid == expectedShortClid

		where:
		url                                         | expectedShortClid
		"https://crushlog.pro/g/12345678"          | "12345678"
		"http://example.com/g/abcdef00"            | "abcdef00"
		"https://api.crushlog.pro/g/550e8400"      | "550e8400"
	}

	def "should parse URI with encoded parameters"() {
		given: "URI with encoded spaces and special characters"
		def uri = "cldf://global/route/550e8400-e29b-41d4-a716-446655440000?v=1&name=The+North+Face&grade=5.13%2B"

		when: "parsing URI"
		def data = QRScanner.parseString(uri)

		then: "parameters are correctly decoded"
		data.route.name == "The North Face"
		data.route.grade == "5.13+"
	}

	def "should handle JSON with alternative location key"() {
		given: "JSON with 'location' instead of 'loc'"
		def jsonData = """
        {
            "v": 1,
            "clid": "clid:route:test",
            "location": {
                "name": "Test Location"
            }
        }
        """

		when: "parsing QR data"
		def data = QRScanner.parseString(jsonData)

		then: "location is parsed correctly"
		data.location.name == "Test Location"
	}

	def "should set hasOfflineData when route info is present in URI"() {
		given: "URI with route info"
		def uri = "cldf://global/route/550e8400-e29b-41d4-a716-446655440000?name=Test&grade=V5"

		when: "parsing URI"
		def data = QRScanner.parseString(uri)

		then: "hasOfflineData is true"
		data.hasOfflineData == true
	}

	def "should not set hasOfflineData for simple URI"() {
		given: "simple URI without extra data"
		def uri = "cldf://global/route/550e8400-e29b-41d4-a716-446655440000"

		when: "parsing URI"
		def data = QRScanner.parseString(uri)

		then: "hasOfflineData is false"
		data.hasOfflineData == false
	}
}
