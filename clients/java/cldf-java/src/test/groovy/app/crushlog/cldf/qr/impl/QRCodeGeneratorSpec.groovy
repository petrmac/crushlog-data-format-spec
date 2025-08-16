package app.crushlog.cldf.qr.impl

import app.crushlog.cldf.models.Location
import app.crushlog.cldf.models.Route
import app.crushlog.cldf.models.enums.RouteType
import app.crushlog.cldf.qr.QRCodeData
import app.crushlog.cldf.qr.QRCodeFactory
import app.crushlog.cldf.qr.QRCodeGenerator
import app.crushlog.cldf.qr.QROptions
import app.crushlog.cldf.qr.QRImageOptions
import spock.lang.Specification
import spock.lang.Unroll

import java.awt.image.BufferedImage

class QRCodeGeneratorSpec extends Specification {

	def "should create generator from factory"() {
		when:
		def generator = QRCodeFactory.createGenerator()

		then:
		generator != null
		generator instanceof QRCodeGenerator
		generator instanceof DefaultQRCodeGenerator
	}

	def "should generate QR code data for route"() {
		given:
		def generator = QRCodeFactory.createGenerator()
		def route = Route.builder()
				.id(1)
				.locationId(100)
				.clid("clid:v1:route:550e8400-e29b-41d4-a716-446655440000")
				.name("Test Route")
				.grades(Route.Grades.builder()
				.yds("5.14a")
				.build())
				.routeType(RouteType.ROUTE)
				.height(900.0)
				.build()

		def options = QROptions.builder()
				.includeIPFS(true)
				.ipfsHash("QmXk9abc123")
				.build()

		when:
		QRCodeData data = generator.generateData(route, options)

		then:
		data != null
		data.version == 1
		data.clid == "clid:v1:route:550e8400-e29b-41d4-a716-446655440000"
		data.url != null
		data.ipfsHash == "QmXk9abc123"
		data.routeData != null
		data.routeData["name"] == "Test Route"
		data.routeData["grade"] == "5.14a"
		data.routeData["type"] == "route"
		data.routeData["height"] == 900.0
	}

	def "should generate QR code data for location"() {
		given:
		def generator = QRCodeFactory.createGenerator()
		def location = Location.builder()
				.id(1)
				.clid("clid:v1:location:loc12345-e29b-41d4-a716-446655440000")
				.name("El Capitan")
				.country("US")
				.state("CA")
				.city("Yosemite Valley")
				.isIndoor(false)
				.build()

		def options = QROptions.builder().build()

		when:
		QRCodeData data = generator.generateData(location, options)

		then:
		data != null
		data.version == 1
		data.clid == "clid:v1:location:loc12345-e29b-41d4-a716-446655440000"
		data.url != null
		data.locationData != null
		data.locationData["name"] == "El Capitan"
		data.locationData["country"] == "US"
		data.locationData["state"] == "CA"
		data.locationData["city"] == "Yosemite Valley"
		data.locationData["indoor"] == false
	}

	def "should generate QR image for route"() {
		given:
		def generator = QRCodeFactory.createGenerator()
		def route = Route.builder()
				.id(1)
				.clid("clid:v1:route:test-uuid")
				.name("Test Route")
				.build()

		def options = QROptions.builder()
				.format(QROptions.QRDataFormat.URL)
				.build()

		when:
		byte[] pngBytes = generator.generatePNG(route, options, QRImageOptions.builder().size(256).build())

		then:
		pngBytes != null
		pngBytes.length > 0
	}

	def "should generate QR image for location"() {
		given:
		def generator = QRCodeFactory.createGenerator()
		def location = Location.builder()
				.id(1)
				.clid("clid:v1:location:test-uuid")
				.name("Test Location")
				.isIndoor(false)
				.build()

		def options = QROptions.builder()
				.format(QROptions.QRDataFormat.JSON)
				.build()

		when:
		byte[] pngBytes = generator.generatePNG(location, options, QRImageOptions.builder().size(256).build())

		then:
		pngBytes != null
		pngBytes.length > 0
	}

	def "should generate QR image with custom image options"() {
		given:
		def generator = QRCodeFactory.createGenerator()
		def data = "cldf://global/route/test-uuid"
		def imageOptions = QRCodeFactory.highQualityImageOptions()

		when:
		byte[] pngBytes = generator.generatePNG(data, imageOptions)

		then:
		pngBytes != null
		pngBytes.length > 0
	}

	def "should generate SVG for route"() {
		given:
		def generator = QRCodeFactory.createGenerator()
		def data = "https://example.com/route/123"
		def imageOptions = QRCodeFactory.defaultImageOptions()

		when:
		String svg = generator.generateSVG(data, imageOptions)

		then:
		svg != null
		svg.contains("<svg")
		svg.contains("</svg>")
	}

	def "should handle different QR data formats"() {
		given:
		def generator = new DefaultQRCodeGenerator()
		def route = Route.builder()
				.id(1)
				.clid("clid:v1:route:550e8400-e29b-41d4-a716-446655440000")
				.name("Test Route")
				.build()

		when: "generating with JSON format"
		def jsonOptions = QROptions.builder()
				.format(QROptions.QRDataFormat.JSON)
				.build()
		def jsonPng = generator.generatePNG(route, jsonOptions, QRImageOptions.builder().size(256).build())

		then:
		jsonPng != null
		jsonPng.length > 0

		when: "generating with URL format"
		def urlOptions = QROptions.builder()
				.format(QROptions.QRDataFormat.URL)
				.baseUrl("https://example.com")
				.build()
		def urlPng = generator.generatePNG(route, urlOptions, QRImageOptions.builder().size(256).build())

		then:
		urlPng != null
		urlPng.length > 0

		when: "generating with CUSTOM_URI format"
		def customOptions = QROptions.builder()
				.format(QROptions.QRDataFormat.CUSTOM_URI)
				.build()
		def customPng = generator.generatePNG(route, customOptions, QRImageOptions.builder().size(256).build())

		then:
		customPng != null
		customPng.length > 0
	}

	def "should check hasOfflineData correctly"() {
		given:
		def generator = QRCodeFactory.createGenerator()

		when: "route data is present"
		def routeData = QRCodeData.builder()
				.version(1)
				.clid("clid:v1:route:test")
				.url("https://example.com")
				.routeData(["name": "Test"])
				.build()

		then:
		routeData.hasOfflineData() == true

		when: "location data is present"
		def locationData = QRCodeData.builder()
				.version(1)
				.clid("clid:v1:location:test")
				.url("https://example.com")
				.locationData(["name": "Test"])
				.build()

		then:
		locationData.hasOfflineData() == true

		when: "no offline data"
		def noData = QRCodeData.builder()
				.version(1)
				.clid("clid:v1:route:test")
				.url("https://example.com")
				.build()

		then:
		noData.hasOfflineData() == false
	}

	def "should get primary URL from QRCodeData"() {
		given:
		def data = QRCodeData.builder()
				.version(1)
				.clid("clid:v1:route:test")
				.url("https://example.com/route/123")
				.build()

		when:
		def url = data.getPrimaryUrl()

		then:
		url == "https://example.com/route/123"
	}

	@Unroll
	def "should handle route without CLID for #format format"() {
		given:
		def generator = new DefaultQRCodeGenerator()
		def route = Route.builder()
				.id(1)
				.locationId(100)
				.name("Test Route")
				.build()

		def options = QROptions.builder()
				.format(format)
				.build()

		when:
		def data = generator.generateData(route, options)

		then:
		data != null
		data.clid != null
		data.clid.startsWith("clid:v1:route:")

		where:
		format << [
			QROptions.QRDataFormat.JSON,
			QROptions.QRDataFormat.URL,
			QROptions.QRDataFormat.CUSTOM_URI
		]
	}

	def "should handle location without CLID"() {
		given:
		def generator = new DefaultQRCodeGenerator()
		def location = Location.builder()
				.id(1)
				.name("Test Location")
				.isIndoor(true)
				.build()

		def options = QROptions.builder().build()

		when:
		def data = generator.generateData(location, options)

		then:
		data != null
		data.clid != null
		data.clid.startsWith("clid:v1:location:")
	}

	def "should include location data with route when provided"() {
		given:
		def generator = new DefaultQRCodeGenerator()
		def location = Location.builder()
				.id(100)
				.clid("clid:v1:location:loc12345")
				.name("Test Crag")
				.country("US")
				.isIndoor(false)
				.build()

		def route = Route.builder()
				.id(1)
				.locationId(100)
				.clid("clid:v1:route:route12345")
				.name("Test Route")
				.build()

		def options = QROptions.builder()
				.location(location)
				.build()

		when:
		def data = generator.generateData(route, options)

		then:
		data != null
		data.locationData != null
		data.locationData["clid"] == "clid:v1:location:loc12345"
		data.locationData["name"] == "Test Crag"
		data.locationData["country"] == "US"
	}
}
