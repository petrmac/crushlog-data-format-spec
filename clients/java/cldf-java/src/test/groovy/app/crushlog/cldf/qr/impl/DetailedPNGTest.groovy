package app.crushlog.cldf.qr.impl

import spock.lang.Specification
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.BarcodeFormat

class DetailedPNGTest extends Specification {

	def "test QR code PNG generation and reading with details"() {
		given:
		PureJavaPNGGenerator generator = new PureJavaPNGGenerator()
		PureJavaImageReader reader = new PureJavaImageReader()
		QRCodeWriter qrWriter = new QRCodeWriter()

		// Generate a QR code like in the failing test
		String data = "Test QR Code"
		BitMatrix matrix = qrWriter.encode(data, BarcodeFormat.QR_CODE, 100, 100)

		when:
		// Generate PNG at 256x256 size
		byte[] png = generator.generatePNG(matrix, 256, 0x000000, 0xFFFFFF)
		println "Matrix dimensions: ${matrix.getWidth()}x${matrix.getHeight()}"
		println "Generated PNG size: ${png.length} bytes"

		// Read it back
		def source = reader.createLuminanceSource(png)
		println "Read dimensions: ${source.getWidth()}x${source.getHeight()}"

		then:
		source != null
		source.getWidth() == 256
		source.getHeight() == 256
	}
}
