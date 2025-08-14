package app.crushlog.cldf.qr

import app.crushlog.cldf.qr.impl.DefaultQRScanner
import spock.lang.Specification

class QRCodeFactorySpec extends Specification {

	def "should create FunctionalQRScanner instance"() {
		when: "creating scanner"
		def scanner = QRCodeFactory.createScanner()

		then: "scanner is created"
		scanner != null
		scanner instanceof QRScanner
		scanner instanceof DefaultQRScanner
	}

	def "should create QRCodeGenerator instance"() {
		when: "creating generator"
		def generator = QRCodeFactory.createGenerator()

		then: "generator is created"
		generator != null
		generator instanceof QRCodeGenerator
	}

	def "should create new scanner instances"() {
		when: "creating multiple scanners"
		def scanner1 = QRCodeFactory.createScanner()
		def scanner2 = QRCodeFactory.createScanner()

		then: "different instances are returned"
		scanner1 != scanner2
	}

	def "should create new generator instances"() {
		when: "creating multiple generators"
		def generator1 = QRCodeFactory.createGenerator()
		def generator2 = QRCodeFactory.createGenerator()

		then: "different instances are returned"
		generator1 != generator2
	}

	def "should have consistent factory methods"() {
		expect: "all factory methods work"
		QRCodeFactory.createScanner() != null
		QRCodeFactory.createGenerator() != null
	}

	def "scanner should be able to parse data"() {
		given: "a scanner from factory"
		def scanner = QRCodeFactory.createScanner()
		def jsonData = """
        {
            "v": 1,
            "clid": "clid:route:test",
            "route": {
                "name": "Test Route"
            }
        }
        """

		when: "parsing data"
		def result = scanner.parse(jsonData)

		then: "result is successful"
		result.isSuccess()
		result.getSuccess().present
		result.getSuccess().get().clid == "clid:route:test"
	}

	def "should create default options"() {
		when: "creating default options"
		def options = QRCodeFactory.defaultOptions()
		def imageOptions = QRCodeFactory.defaultImageOptions()

		then: "options are created"
		options != null
		imageOptions != null
	}

	def "should create high quality image options"() {
		when: "creating high quality options"
		def options = QRCodeFactory.highQualityImageOptions()

		then: "options have correct settings"
		options.size == 512
		options.errorCorrectionLevel == QRImageOptions.ErrorCorrectionLevel.H
		options.margin == 8
	}

	def "should create compact image options"() {
		when: "creating compact options"
		def options = QRCodeFactory.compactImageOptions()

		then: "options have correct settings"
		options.size == 128
		options.errorCorrectionLevel == QRImageOptions.ErrorCorrectionLevel.L
		options.margin == 2
	}
}
