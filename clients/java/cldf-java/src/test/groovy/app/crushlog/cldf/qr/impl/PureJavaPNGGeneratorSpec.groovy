package app.crushlog.cldf.qr.impl

import com.google.zxing.BarcodeFormat
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import spock.lang.Specification
import spock.lang.Subject

class PureJavaPNGGeneratorSpec extends Specification {

	@Subject
	PureJavaPNGGenerator generator = new PureJavaPNGGenerator()

	QRCodeWriter qrCodeWriter = new QRCodeWriter()

	def "should generate valid PNG for simple QR code"() {
		given: "A simple QR code bit matrix"
		String data = "Hello World"
		BitMatrix matrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, 100, 100)

		when: "Generating PNG"
		byte[] pngData = generator.generatePNG(matrix, 256, 0x000000, 0xFFFFFF)

		then: "PNG is generated with correct signature"
		pngData != null
		pngData.length > 0
		// Check PNG signature
		pngData[0] == (byte) 0x89
		pngData[1] == (byte) 0x50
		pngData[2] == (byte) 0x4E
		pngData[3] == (byte) 0x47
		pngData[4] == (byte) 0x0D
		pngData[5] == (byte) 0x0A
		pngData[6] == (byte) 0x1A
		pngData[7] == (byte) 0x0A
	}

	def "should generate PNG with custom colors"() {
		given: "A QR code with custom colors"
		String data = "Custom Colors"
		BitMatrix matrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, 50, 50)
		int foreground = 0xFF0000 // Red
		int background = 0x00FF00 // Green

		when: "Generating PNG with custom colors"
		byte[] pngData = generator.generatePNG(matrix, 200, foreground, background)

		then: "PNG is generated"
		pngData != null
		pngData.length > 0
	}

	def "should handle different sizes correctly"() {
		given: "A QR code matrix"
		String data = "Size Test"
		BitMatrix matrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, 25, 25)

		when: "Generating PNGs of different sizes"
		byte[] small = generator.generatePNG(matrix, 100, 0x000000, 0xFFFFFF)
		byte[] medium = generator.generatePNG(matrix, 256, 0x000000, 0xFFFFFF)
		byte[] large = generator.generatePNG(matrix, 512, 0x000000, 0xFFFFFF)

		then: "All sizes are generated and different"
		small != null
		medium != null
		large != null
		small.length < medium.length
		medium.length < large.length
	}

	def "should handle empty matrix gracefully"() {
		given: "An empty bit matrix"
		BitMatrix matrix = new BitMatrix(10, 10)

		when: "Generating PNG from empty matrix"
		byte[] pngData = generator.generatePNG(matrix, 100, 0x000000, 0xFFFFFF)

		then: "PNG is still generated (all background)"
		pngData != null
		pngData.length > 0
	}

	def "should scale properly when size is smaller than matrix"() {
		given: "A large QR code matrix"
		String data = "Large QR Code Data"
		BitMatrix matrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, 200, 200)

		when: "Generating PNG smaller than matrix"
		byte[] pngData = generator.generatePNG(matrix, 100, 0x000000, 0xFFFFFF)

		then: "PNG is generated with minimum scale"
		pngData != null
		pngData.length > 0
	}

	def "should handle null matrix"() {
		when: "Generating PNG from null matrix"
		generator.generatePNG(null, 256, 0x000000, 0xFFFFFF)

		then: "Exception is thrown"
		thrown(NullPointerException)
	}

	def "should generate consistent output for same input"() {
		given: "A QR code matrix"
		String data = "Consistent Test"
		BitMatrix matrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, 50, 50)

		when: "Generating PNG twice with same parameters"
		byte[] png1 = generator.generatePNG(matrix, 200, 0x000000, 0xFFFFFF)
		byte[] png2 = generator.generatePNG(matrix, 200, 0x000000, 0xFFFFFF)

		then: "Output is identical"
		png1 == png2
	}
}
