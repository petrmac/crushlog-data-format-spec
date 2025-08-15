package app.crushlog.cldf.qr.impl

import spock.lang.Specification
import spock.lang.Unroll
import app.crushlog.cldf.qr.QRScanner
import app.crushlog.cldf.qr.ParsedQRData
import app.crushlog.cldf.qr.result.QRError
import app.crushlog.cldf.models.enums.RouteType
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import app.crushlog.cldf.qr.QRImageOptions

class QRScannerSpec extends Specification {

	QRScanner scanner = new DefaultQRScanner()

	def "should parse null or empty data as failure"() {
		when:
		def result1 = scanner.parse(null)
		def result2 = scanner.parse("")
		def result3 = scanner.parse("  ")

		then:
		result1.isFailure()
		result1.getError().get().type == QRError.ErrorType.PARSE_ERROR

		result2.isFailure()
		result2.getError().get().type == QRError.ErrorType.PARSE_ERROR

		result3.isFailure()
		result3.getError().get().type == QRError.ErrorType.PARSE_ERROR
	}

	def "should parse JSON QR data successfully"() {
		given:
		def json = '''
    {
      "v": 1,
      "clid": "clid:v1:route:550e8400-e29b-41d4-a716-446655440000",
      "url": "https://crushlog.com/routes/123",
      "route": {
        "id": 123,
        "name": "Test Route",
        "grade": "5.10a",
        "gradeSystem": "yds",
        "type": "sport",
        "height": 15.5
      },
      "loc": {
        "id": 1,
        "name": "Test Gym",
        "country": "US",
        "state": "CA",
        "city": "San Francisco",
        "indoor": true
      }
    }
    '''

		when:
		def result = scanner.parse(json)

		then:
		result.isSuccess()

		def data = result.getSuccess().get()
		data.version == 1
		data.clid == "clid:v1:route:550e8400-e29b-41d4-a716-446655440000"
		data.url == "https://crushlog.com/routes/123"
		data.hasOfflineData

		data.route != null
		data.route.id == 123
		data.route.name == "Test Route"
		data.route.grade == "5.10a"
		data.route.type == "sport"
		data.route.height == 15.5

		data.location != null
		data.location.id == 1
		data.location.name == "Test Gym"
		data.location.country == "US"
		data.location.state == "CA"
		data.location.city == "San Francisco"
		data.location.indoor
	}

	def "should parse JSON with location field instead of loc"() {
		given:
		def json = '''
    {
      "v": 1,
      "location": {
        "id": 2,
        "name": "Outdoor Crag"
      }
    }
    '''

		when:
		def result = scanner.parse(json)

		then:
		result.isSuccess()
		def data = result.getSuccess().get()
		data.location != null
		data.location.id == 2
		data.location.name == "Outdoor Crag"
	}

	def "should parse URL format successfully"() {
		given:
		def url = "https://crushlog.com/g/abc123-def456"

		when:
		def result = scanner.parse(url)

		then:
		result.isSuccess()
		def data = result.getSuccess().get()
		data.version == 1
		data.url == url
		data.shortClid == "abc123-def456"
		!data.hasOfflineData
	}

	def "should parse custom URI format successfully"() {
		given:
		def uri = "cldf://global/route/550e8400-e29b-41d4-a716-446655440000?name=TestRoute&grade=V4&v=2"

		when:
		def result = scanner.parse(uri)

		then:
		result.isSuccess()
		def data = result.getSuccess().get()
		data.version == 2
		data.clid == "clid:v1:route:550e8400-e29b-41d4-a716-446655440000"
		data.hasOfflineData
		data.route != null
		data.route.name == "TestRoute"
		data.route.grade == "V4"
	}

	def "should fail on unrecognized format"() {
		given:
		def badData = "not a valid QR format"

		when:
		def result = scanner.parse(badData)

		then:
		result.isFailure()
		result.getError().get().type == QRError.ErrorType.INVALID_FORMAT
	}

	def "should parse JSON with metadata"() {
		given:
		def json = '''
    {
      "v": 1,
      "clid": "clid:v1:route:123",
      "meta": {
        "blockchain": true,
        "verified": true,
        "timestamp": 1234567890
      }
    }
    '''

		when:
		def result = scanner.parse(json)

		then:
		result.isSuccess()
		def data = result.getSuccess().get()
		data.blockchainVerified == true
	}

	def "should handle malformed JSON gracefully"() {
		given:
		def badJson = '{ "v": 1, "route": { "name": '

		when:
		def result = scanner.parse(badJson)

		then:
		result.isFailure()
		result.getError().get().type == QRError.ErrorType.PARSE_ERROR
		result.getError().get().message.contains("JSON")
	}

	def "should convert parsed data to Route successfully"() {
		given:
		def data = ParsedQRData.builder()
				.clid("clid:v1:route:123")
				.route(ParsedQRData.RouteInfo.builder()
				.id(123)
				.name("Test Route")
				.grade("5.10a")
				.gradeSystem("yds")
				.type("sport")
				.height(15.5)
				.build())
				.location(ParsedQRData.LocationInfo.builder()
				.id(1)
				.build())
				.build()

		when:
		def result = scanner.toRoute(data)

		then:
		result.isSuccess()
		def route = result.getSuccess().get()
		route.clid == "clid:v1:route:123"
		route.id == 123
		route.name == "Test Route"
		route.grades.yds == "5.10a"
		route.routeType == RouteType.ROUTE
		route.height == 15.5
		route.locationId == 1
	}

	def "should convert boulder type correctly"() {
		given:
		def data = ParsedQRData.builder()
				.route(ParsedQRData.RouteInfo.builder()
				.name("Boulder Problem")
				.type("boulder")
				.grade("V4")
				.gradeSystem("vScale")
				.build())
				.build()

		when:
		def result = scanner.toRoute(data)

		then:
		result.isSuccess()
		def route = result.getSuccess().get()
		route.routeType == RouteType.BOULDER
		route.grades.vScale == "V4"
	}

	def "should fail to convert to Route when no route data"() {
		given:
		def data = ParsedQRData.builder()
				.clid("clid:v1:location:123")
				.build()

		when:
		def result = scanner.toRoute(data)

		then:
		result.isFailure()
		result.getError().get().type == QRError.ErrorType.MISSING_DATA
	}

	def "should convert parsed data to Location successfully"() {
		given:
		def data = ParsedQRData.builder()
				.location(ParsedQRData.LocationInfo.builder()
				.clid("clid:v1:location:123")
				.id(1)
				.name("Test Gym")
				.country("US")
				.state("CA")
				.city("San Francisco")
				.indoor(true)
				.build())
				.build()

		when:
		def result = scanner.toLocation(data)

		then:
		result.isSuccess()
		def location = result.getSuccess().get()
		location.clid == "clid:location:123"
		location.id == 1
		location.name == "Test Gym"
		location.country == "US"
		location.state == "CA"
		location.city == "San Francisco"
		location.isIndoor == true
	}

	def "should fail to convert to Location when no location data"() {
		given:
		def data = ParsedQRData.builder()
				.clid("clid:v1:route:123")
				.build()

		when:
		def result = scanner.toLocation(data)

		then:
		result.isFailure()
		result.getError().get().type == QRError.ErrorType.MISSING_DATA
	}

	def "should validate valid QR data"() {
		given:
		def validData = '{"v": 1, "clid": "clid:v1:route:123"}'

		when:
		def result = scanner.validate(validData)

		then:
		result.isSuccess()
		result.getSuccess().get() == validData
	}

	def "should fail validation for invalid data"() {
		given:
		def invalidData = '{"v": 0}'

		when:
		def result = scanner.validate(invalidData)

		then:
		result.isFailure()
	}

	def "should parseToRoute in one operation"() {
		given:
		def json = '''
    {
      "v": 1,
      "route": {
        "name": "Direct Route Test",
        "grade": "5.11a",
        "gradeSystem": "yds"
      }
    }
    '''

		when:
		def result = scanner.parseToRoute(json)

		then:
		result.isSuccess()
		def route = result.getSuccess().get()
		route.name == "Direct Route Test"
		route.grades.yds == "5.11a"
	}

	def "should parseToLocation in one operation"() {
		given:
		def json = '''
    {
      "v": 1,
      "location": {
        "name": "Direct Location Test",
        "country": "US"
      }
    }
    '''

		when:
		def result = scanner.parseToLocation(json)

		then:
		result.isSuccess()
		def location = result.getSuccess().get()
		location.name == "Direct Location Test"
		location.country == "US"
	}

	// Grade pattern detection tests removed - we now require explicit gradeSystem
	def "should not set grade without gradeSystem"() {
		given:
		def data = ParsedQRData.builder()
				.route(ParsedQRData.RouteInfo.builder()
				.name("Test")
				.grade("5.10a")
				// No gradeSystem provided
				.build())
				.build()

		when:
		def result = scanner.toRoute(data)

		then:
		result.isSuccess()
		def route = result.getSuccess().get()
		route.grades == null  // Grade not set without gradeSystem
	}

	def "should scan QR code from image bytes"() {
		given:
		def testData = '{"v": 1, "clid": "clid:v1:route:123", "route": {"name": "Image Test"}}'
		def imageBytes = generateQRImageBytes(testData)

		when:
		def result = scanner.scan(imageBytes)

		then:
		result.isSuccess()
		def data = result.getSuccess().get()
		data.clid == "clid:route:123"
		data.route.name == "Image Test"
	}

	def "should fail scanning image without QR code"() {
		given:
		// Create a blank PNG using our pure Java generator
		def generator = new PureJavaPNGGenerator()
		def emptyMatrix = new com.google.zxing.common.BitMatrix(100, 100)
		def blankImageBytes = generator.generatePNG(emptyMatrix, 100, 0x000000, 0xFFFFFF)

		when:
		def result = scanner.scan(blankImageBytes)

		then:
		result.isFailure()
		result.getError().get().type == QRError.ErrorType.SCAN_ERROR
		result.getError().get().message.contains("No QR code found")
	}

	def "should scan QR code from byte array"() {
		given:
		def testData = '{"v": 1, "route": {"name": "Byte Test"}}'
		def bytes = generateQRImageBytes(testData)

		when:
		def result = scanner.scan(bytes)

		then:
		result.isSuccess()
		def data = result.getSuccess().get()
		data.route.name == "Byte Test"
	}

	def "should fail scanning invalid byte array"() {
		given:
		def invalidBytes = "not an image".getBytes()

		when:
		def result = scanner.scan(invalidBytes)

		then:
		result.isFailure()
		result.getError().get().type == QRError.ErrorType.IMAGE_ERROR
	}

	def "should scanToRoute from image bytes directly"() {
		given:
		def testData = '{"v": 1, "route": {"name": "Direct Scan", "grade": "V7", "gradeSystem": "vScale"}}'
		def imageBytes = generateQRImageBytes(testData)

		when:
		def result = scanner.scanToRoute(imageBytes)

		then:
		result.isSuccess()
		def route = result.getSuccess().get()
		route.name == "Direct Scan"
		route.grades.vScale == "V7"
	}

	def "should handle parse URL with query params"() {
		given:
		def uri = "cldf://global/route/123?cldf=QmXyz&name=Test%20Route&grade=5.10a&gradeSystem=yds"

		when:
		def result = scanner.parse(uri)

		then:
		result.isSuccess()
		def data = result.getSuccess().get()
		data.ipfsHash == "QmXyz"
		data.route.name == "Test Route"
		data.route.grade == "5.10a"
		data.route.gradeSystem == "yds"
		data.hasOfflineData
	}

	def "should handle empty JSON objects gracefully"() {
		given:
		def json = '{}'

		when:
		def result = scanner.parse(json)

		then:
		result.isSuccess()
		def data = result.getSuccess().get()
		data.version == 1
		!data.hasOfflineData
	}

	def "should handle route without location ID"() {
		given:
		def data = ParsedQRData.builder()
				.route(ParsedQRData.RouteInfo.builder()
				.name("No Location Route")
				.build())
				.build()

		when:
		def result = scanner.toRoute(data)

		then:
		result.isSuccess()
		def route = result.getSuccess().get()
		route.name == "No Location Route"
		route.locationId == null
	}

	// Helper methods

	private byte[] generateQRImageBytes(String data) {
		def generator = new DefaultQRCodeGenerator()
		def options = QRImageOptions.builder().size(200).build()
		return generator.generatePNG(data, options)
	}
}
