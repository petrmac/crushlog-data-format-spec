package app.crushlog.cldf.qr.impl

import spock.lang.Specification
import app.crushlog.cldf.qr.QRImageOptions

class IntegrationTest extends Specification {

	def "test complete QR generation and scanning flow"() {
		given:
		DefaultQRCodeGenerator generator = new DefaultQRCodeGenerator()
		DefaultQRScanner scanner = new DefaultQRScanner()

		String testData = '{"v": 1, "clid": "clid:v1:route:test", "route": {"name": "Test Route"}}'

		when:
		// Generate QR code PNG
		byte[] png = generator.generatePNG(testData, QRImageOptions.builder().size(256).build())

		// Scan it back
		def result = scanner.scan(png)

		then:
		result.isSuccess()
		def parsed = result.getSuccess().get()
		parsed.clid == "clid:v1:route:test"
		parsed.route.name == "Test Route"
	}
}
