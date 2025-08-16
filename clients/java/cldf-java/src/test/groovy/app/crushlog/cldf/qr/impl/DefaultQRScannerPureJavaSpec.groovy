package app.crushlog.cldf.qr.impl

import app.crushlog.cldf.qr.ParsedQRData
import app.crushlog.cldf.qr.QRImageOptions
import app.crushlog.cldf.qr.QRColor
import app.crushlog.cldf.qr.result.QRError
import app.crushlog.cldf.qr.result.Result
import spock.lang.Specification
import spock.lang.Subject

class DefaultQRScannerPureJavaSpec extends Specification {

	@Subject
	DefaultQRScanner scanner = new DefaultQRScanner()

	DefaultQRCodeGenerator generator = new DefaultQRCodeGenerator()

	def "should scan QR code from PNG bytes without AWT"() {
		given: "A QR code PNG generated with pure Java"
		String testData = '{"v":2,"clid":"clid:v1:route:abc123","url":"https://crushlog.pro/route/abc123"}'
		QRImageOptions options = QRImageOptions.builder()
				.size(256)
				.build()
		byte[] pngBytes = generator.generatePNG(testData, options)

		when: "Scanning the PNG bytes"
		Result<ParsedQRData, QRError> result = scanner.scan(pngBytes)

		then: "QR code is successfully decoded"
		result.isSuccess()
		result.getSuccess().isPresent()

		and: "Data is correctly parsed"
		ParsedQRData parsed = result.getSuccess().get()
		parsed.version == 2
		parsed.clid == "clid:v1:route:abc123"
		parsed.url == "https://crushlog.pro/route/abc123"
	}

	def "should scan QR code with route data"() {
		given: "A QR code containing route information"
		String routeData = '''{
            "v": 2,
            "clid": "clid:v1:route:test-route",
            "route": {
                "id": 123,
                "name": "Test Route",
                "grade": "5.10a",
                "gradeSystem": "YDS",
                "type": "sport",
                "height": 15.5
            }
        }'''
		byte[] pngBytes = generator.generatePNG(routeData, QRImageOptions.builder().size(300).build())

		when: "Scanning the QR code"
		Result<ParsedQRData, QRError> result = scanner.scan(pngBytes)

		then: "Route data is extracted"
		result.isSuccess()
		ParsedQRData parsed = result.getSuccess().get()
		parsed.route != null
		parsed.route.name == "Test Route"
		parsed.route.grade == "5.10a"
		parsed.route.gradeSystem == "YDS"
		parsed.route.height == 15.5
	}

	def "should scan QR code with location data"() {
		given: "A QR code containing location information"
		String locationData = '''{
            "v": 2,
            "clid": "clid:v1:location:test-loc",
            "location": {
                "id": 456,
                "name": "Test Crag",
                "country": "USA",
                "state": "CA",
                "city": "Bishop",
                "indoor": false
            }
        }'''
		byte[] pngBytes = generator.generatePNG(locationData, QRImageOptions.builder().size(256).build())

		when: "Scanning the QR code"
		Result<ParsedQRData, QRError> result = scanner.scan(pngBytes)

		then: "Location data is extracted"
		result.isSuccess()
		ParsedQRData parsed = result.getSuccess().get()
		parsed.location != null
		parsed.location.name == "Test Crag"
		parsed.location.country == "USA"
		parsed.location.state == "CA"
		parsed.location.city == "Bishop"
		!parsed.location.indoor
	}

	def "should handle colored QR codes"() {
		given: "A QR code with custom colors"
		String data = '{"v":1,"clid":"clid:v1:route:colored"}'
		QRImageOptions options = QRImageOptions.builder()
				.size(200)
				.foregroundColor(new QRColor(0, 0, 255))  // Blue
				.backgroundColor(new QRColor(255, 255, 0)) // Yellow
				.build()
		byte[] pngBytes = generator.generatePNG(data, options)

		when: "Scanning the colored QR code"
		Result<ParsedQRData, QRError> result = scanner.scan(pngBytes)

		then: "QR code is decoded despite custom colors"
		result.isSuccess()
		ParsedQRData parsed = result.getSuccess().get()
		parsed.clid == "clid:v1:route:colored"
	}

	def "should handle different QR code sizes"() {
		given: "QR codes of various sizes"
		String data = '{"v":1,"clid":"clid:v1:route:size-test"}'

		when: "Scanning QR codes of different sizes"
		def results = []
		[100, 256, 512, 1024].each { size ->
			QRImageOptions options = QRImageOptions.builder().size(size).build()
			byte[] pngBytes = generator.generatePNG(data, options)
			results << scanner.scan(pngBytes)
		}

		then: "All sizes are successfully scanned"
		results.every { it.isSuccess() }
		results.every { it.getSuccess().get().clid == "clid:v1:route:size-test" }
	}

	def "should handle URLs in QR codes"() {
		given: "A QR code containing just a URL"
		String url = "https://crushlog.pro/g/abc-def-123"
		byte[] pngBytes = generator.generatePNG(url, QRImageOptions.builder().size(200).build())

		when: "Scanning the URL QR code"
		Result<ParsedQRData, QRError> result = scanner.scan(pngBytes)

		then: "URL is parsed correctly"
		result.isSuccess()
		ParsedQRData parsed = result.getSuccess().get()
		parsed.url == url
		parsed.shortClid == "abc-def-123"
	}

	def "should handle custom URI format"() {
		given: "A QR code with custom URI"
		String uri = "cldf://global/route/test-123?name=TestRoute&grade=V4&gradeSystem=vscale"
		byte[] pngBytes = generator.generatePNG(uri, QRImageOptions.builder()
				.size(400)
				.errorCorrectionLevel(QRImageOptions.ErrorCorrectionLevel.H)
				.build())

		when: "Scanning the custom URI QR code"
		Result<ParsedQRData, QRError> result = scanner.scan(pngBytes)

		then: "URI is parsed with query parameters"
		result.isSuccess()
		ParsedQRData parsed = result.getSuccess().get()
		parsed.clid == "clid:v1:route:test-123"
		parsed.route != null
		parsed.route.name == "TestRoute"
		parsed.route.grade == "V4"
		parsed.route.gradeSystem == "vscale"
	}

	def "should fail gracefully when no QR code in image"() {
		given: "A PNG without a QR code (solid color)"
		// Create a simple solid color PNG
		PureJavaPNGGenerator pngGen = new PureJavaPNGGenerator()
		com.google.zxing.common.BitMatrix emptyMatrix = new com.google.zxing.common.BitMatrix(100, 100)
		byte[] pngBytes = pngGen.generatePNG(emptyMatrix, 200, 0x000000, 0xFFFFFF)

		when: "Attempting to scan"
		Result<ParsedQRData, QRError> result = scanner.scan(pngBytes)

		then: "Scan fails with appropriate error"
		result.isFailure()
		result.getError().get().message == "No QR code found in image"
	}

	def "should handle invalid PNG data"() {
		given: "Invalid PNG data"
		byte[] invalidPng = "Not a PNG".getBytes()

		when: "Attempting to scan"
		Result<ParsedQRData, QRError> result = scanner.scan(invalidPng)

		then: "Scan fails with error"
		result.isFailure()
		result.getError().get().message.contains("Failed to read image")
	}

	def "should handle null or empty image bytes"() {
		when: "Scanning null bytes"
		Result<ParsedQRData, QRError> resultNull = scanner.scan(null as byte[])

		then: "Returns appropriate error"
		resultNull.isFailure()
		resultNull.getError().get().message == "Image bytes cannot be null or empty"

		when: "Scanning empty bytes"
		Result<ParsedQRData, QRError> resultEmpty = scanner.scan(new byte[0])

		then: "Returns appropriate error"
		resultEmpty.isFailure()
		resultEmpty.getError().get().message == "Image bytes cannot be null or empty"
	}

	def "should handle complex JSON data in QR code"() {
		given: "Complex nested JSON structure"
		String complexData = '''{
            "v": 3,
            "clid": "clid:v1:route:complex",
            "url": "https://crushlog.pro/route/complex",
            "cldf": "QmTest123",
            "route": {
                "id": 999,
                "name": "Complex Route",
                "grade": "7a",
                "gradeSystem": "french",
                "type": "boulder",
                "height": 4.5
            },
            "location": {
                "id": 888,
                "name": "Complex Area",
                "country": "France",
                "state": null,
                "city": "Fontainebleau",
                "indoor": false
            },
            "meta": {
                "blockchain": true
            }
        }'''
		byte[] pngBytes = generator.generatePNG(complexData, QRImageOptions.builder()
				.size(600)
				.errorCorrectionLevel(QRImageOptions.ErrorCorrectionLevel.H)
				.build())

		when: "Scanning complex QR code"
		Result<ParsedQRData, QRError> result = scanner.scan(pngBytes)

		then: "All data is correctly parsed"
		result.isSuccess()
		ParsedQRData parsed = result.getSuccess().get()
		parsed.version == 3
		parsed.clid == "clid:v1:route:complex"
		parsed.ipfsHash == "QmTest123"
		parsed.blockchainVerified
		parsed.route != null
		parsed.location != null
		parsed.hasOfflineData
	}

	def "should maintain data integrity through encode-decode cycle"() {
		given: "Original data to encode"
		String originalData = '''{
            "v": 2,
            "clid": "clid:v1:route:integrity-test",
            "route": {
                "id": 12345,
                "name": "Integrity Test Route",
                "grade": "5.12d",
                "gradeSystem": "YDS"
            }
        }'''

		when: "Encoding to QR and decoding back"
		byte[] pngBytes = generator.generatePNG(originalData, QRImageOptions.builder().size(300).build())
		Result<ParsedQRData, QRError> result = scanner.scan(pngBytes)

		then: "Data remains intact"
		result.isSuccess()
		ParsedQRData parsed = result.getSuccess().get()
		parsed.clid == "clid:v1:route:integrity-test"
		parsed.route.name == "Integrity Test Route"
		parsed.route.grade == "5.12d"
		parsed.route.gradeSystem == "YDS"
		parsed.route.id == 12345
	}
}
