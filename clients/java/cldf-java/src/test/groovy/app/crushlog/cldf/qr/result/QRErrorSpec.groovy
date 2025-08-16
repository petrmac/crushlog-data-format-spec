package app.crushlog.cldf.qr.result

import spock.lang.Specification
import spock.lang.Unroll

class QRErrorSpec extends Specification {

	def "should create error with type and message"() {
		when:
		def error = QRError.of(QRError.ErrorType.PARSE_ERROR, "Failed to parse")

		then:
		error.type == QRError.ErrorType.PARSE_ERROR
		error.message == "Failed to parse"
		error.details == null
	}

	def "should create error with full details"() {
		when:
		def error = QRError.of(QRError.ErrorType.SCAN_ERROR, "Scan failed", "No QR code found")

		then:
		error.type == QRError.ErrorType.SCAN_ERROR
		error.message == "Scan failed"
		error.details == "No QR code found"
	}

	def "should create error from exception"() {
		given:
		def exception = new RuntimeException("Test error")

		when:
		def error = QRError.from(QRError.ErrorType.GENERATION_ERROR, exception)

		then:
		error.type == QRError.ErrorType.GENERATION_ERROR
		error.message == "Test error"
		error.details == null
	}

	def "should create error from exception with cause"() {
		given:
		def cause = new IllegalArgumentException("Root cause")
		def exception = new RuntimeException("Test error", cause)

		when:
		def error = QRError.from(QRError.ErrorType.IMAGE_ERROR, exception)

		then:
		error.type == QRError.ErrorType.IMAGE_ERROR
		error.message == "Test error"
		error.details == "Root cause"
	}

	def "should create error from exception without message"() {
		given:
		def exception = new RuntimeException()

		when:
		def error = QRError.from(QRError.ErrorType.VALIDATION_ERROR, exception)

		then:
		error.type == QRError.ErrorType.VALIDATION_ERROR
		error.message == "RuntimeException"
		error.details == null
	}

	def "should create parse error"() {
		when:
		def error = QRError.parseError("Invalid JSON")

		then:
		error.type == QRError.ErrorType.PARSE_ERROR
		error.message == "Invalid JSON"
		error.details == null
	}

	def "should create scan error"() {
		when:
		def error = QRError.scanError("No QR code")

		then:
		error.type == QRError.ErrorType.SCAN_ERROR
		error.message == "No QR code"
		error.details == null
	}

	def "should create validation error"() {
		when:
		def error = QRError.validationError("Missing required field")

		then:
		error.type == QRError.ErrorType.VALIDATION_ERROR
		error.message == "Missing required field"
		error.details == null
	}

	def "should create generation error"() {
		when:
		def error = QRError.generationError("Failed to generate QR")

		then:
		error.type == QRError.ErrorType.GENERATION_ERROR
		error.message == "Failed to generate QR"
		error.details == null
	}

	def "should create image error"() {
		when:
		def error = QRError.imageError("Invalid image format")

		then:
		error.type == QRError.ErrorType.IMAGE_ERROR
		error.message == "Invalid image format"
		error.details == null
	}

	def "should get description without details"() {
		given:
		def error = QRError.of(QRError.ErrorType.PARSE_ERROR, "Test message")

		when:
		def description = error.getDescription()

		then:
		description == "[PARSE_ERROR] Test message"
	}

	def "should get description with details"() {
		given:
		def error = QRError.of(QRError.ErrorType.SCAN_ERROR, "Test message", "Additional details")

		when:
		def description = error.getDescription()

		then:
		description == "[SCAN_ERROR] Test message: Additional details"
	}

	def "should ignore empty details in description"() {
		given:
		def error = QRError.of(QRError.ErrorType.IMAGE_ERROR, "Test message", "")

		when:
		def description = error.getDescription()

		then:
		description == "[IMAGE_ERROR] Test message"
	}

	@Unroll
	def "ErrorType #type should have description #expectedDesc"() {
		expect:
		type.getDescription() == expectedDesc

		where:
		type                                         | expectedDesc
		QRError.ErrorType.PARSE_ERROR              | "Failed to parse QR data"
		QRError.ErrorType.SCAN_ERROR               | "Failed to scan QR code"
		QRError.ErrorType.VALIDATION_ERROR         | "QR data validation failed"
		QRError.ErrorType.GENERATION_ERROR         | "Failed to generate QR code"
		QRError.ErrorType.IMAGE_ERROR              | "Image processing error"
		QRError.ErrorType.INVALID_FORMAT           | "Invalid QR format"
		QRError.ErrorType.MISSING_DATA             | "Required data missing"
		QRError.ErrorType.UNSUPPORTED_VERSION      | "Unsupported QR version"
	}

	def "should be value object with equals and hashCode"() {
		given:
		def error1 = QRError.of(QRError.ErrorType.PARSE_ERROR, "Message", "Details")
		def error2 = QRError.of(QRError.ErrorType.PARSE_ERROR, "Message", "Details")
		def error3 = QRError.of(QRError.ErrorType.SCAN_ERROR, "Message", "Details")

		expect:
		error1 == error2
		error1.hashCode() == error2.hashCode()
		error1 != error3
		error1.hashCode() != error3.hashCode()
	}
}
