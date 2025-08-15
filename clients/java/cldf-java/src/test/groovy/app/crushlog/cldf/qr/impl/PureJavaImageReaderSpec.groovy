package app.crushlog.cldf.qr.impl

import com.google.zxing.BarcodeFormat
import com.google.zxing.LuminanceSource
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import spock.lang.Specification
import spock.lang.Subject

class PureJavaImageReaderSpec extends Specification {

	@Subject
	PureJavaImageReader reader = new PureJavaImageReader()

	PureJavaPNGGenerator pngGenerator = new PureJavaPNGGenerator()
	QRCodeWriter qrCodeWriter = new QRCodeWriter()

	def "should create LuminanceSource from valid PNG"() {
		given: "A PNG containing a QR code"
		String data = "Test QR Code"
		BitMatrix matrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, 100, 100)
		byte[] pngData = pngGenerator.generatePNG(matrix, 256, 0x000000, 0xFFFFFF)

		when: "Creating LuminanceSource from PNG"
		LuminanceSource source = reader.createLuminanceSource(pngData)

		then: "LuminanceSource is created successfully"
		source != null
		source.getWidth() == 256
		source.getHeight() == 256
	}

	def "should handle grayscale PNG correctly"() {
		given: "A grayscale QR code PNG"
		String data = "Grayscale Test"
		BitMatrix matrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, 50, 50)
		byte[] pngData = pngGenerator.generatePNG(matrix, 128, 0x000000, 0xFFFFFF)

		when: "Creating LuminanceSource"
		LuminanceSource source = reader.createLuminanceSource(pngData)

		then: "Source dimensions match"
		source.getWidth() == 128
		source.getHeight() == 128
	}

	def "should reject invalid PNG data"() {
		given: "Invalid PNG data"
		byte[] invalidData = "Not a PNG".getBytes()

		when: "Attempting to create LuminanceSource"
		reader.createLuminanceSource(invalidData)

		then: "IOException is thrown"
		IOException e = thrown()
		e.message == "Not a valid PNG file"
	}

	def "should handle empty byte array"() {
		given: "Empty byte array"
		byte[] emptyData = new byte[0]

		when: "Attempting to create LuminanceSource"
		reader.createLuminanceSource(emptyData)

		then: "IOException is thrown"
		IOException e = thrown()
		e.message == "Not a valid PNG file"
	}

	def "should handle PNG with multiple IDAT chunks"() {
		given: "A complex QR code that might generate multiple IDAT chunks"
		String data = "This is a longer text that will create a more complex QR code " +
				"with potentially multiple IDAT chunks in the PNG representation"
		BitMatrix matrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, 200, 200)
		byte[] pngData = pngGenerator.generatePNG(matrix, 512, 0x000000, 0xFFFFFF)

		when: "Creating LuminanceSource"
		LuminanceSource source = reader.createLuminanceSource(pngData)

		then: "Source is created with correct dimensions"
		source != null
		source.getWidth() == 512
		source.getHeight() == 512
	}

	def "should extract correct luminance values"() {
		given: "A simple black and white pattern"
		BitMatrix matrix = new BitMatrix(10, 10)
		// Create a checkerboard pattern
		for (int i = 0; i < 10; i++) {
			for (int j = 0; j < 10; j++) {
				if ((i + j) % 2 == 0) {
					matrix.set(i, j)
				}
			}
		}
		byte[] pngData = pngGenerator.generatePNG(matrix, 100, 0x000000, 0xFFFFFF)

		when: "Creating LuminanceSource and checking values"
		LuminanceSource source = reader.createLuminanceSource(pngData)
		byte[] row = source.getRow(0, null)

		then: "Luminance values are extracted"
		source != null
		row != null
		row.length == 100
	}

	def "should handle colored QR codes"() {
		given: "A QR code with custom colors"
		String data = "Colored QR"
		BitMatrix matrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, 50, 50)
		int foreground = 0xFF0000 // Red
		int background = 0x00FF00 // Green
		byte[] pngData = pngGenerator.generatePNG(matrix, 150, foreground, background)

		when: "Creating LuminanceSource from colored PNG"
		LuminanceSource source = reader.createLuminanceSource(pngData)

		then: "Source is created (colors converted to grayscale)"
		source != null
		source.getWidth() == 150
		source.getHeight() == 150
	}

	def "should handle PNG with wrong dimensions in header"() {
		given: "A malformed PNG with incorrect header"
		byte[] malformedPNG = createMalformedPNG()

		when: "Attempting to create LuminanceSource"
		reader.createLuminanceSource(malformedPNG)

		then: "IOException is thrown"
		thrown(IOException)
	}

	def "should process PNG filters correctly"() {
		given: "A PNG with various filter types"
		String data = "Filter Test"
		BitMatrix matrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, 75, 75)
		byte[] pngData = pngGenerator.generatePNG(matrix, 200, 0x333333, 0xCCCCCC)

		when: "Creating LuminanceSource"
		LuminanceSource source = reader.createLuminanceSource(pngData)

		then: "Source is created successfully despite filters"
		source != null
		source.getWidth() == 200
		source.getHeight() == 200
	}

	private byte[] createMalformedPNG() {
		// Create a minimal PNG with invalid structure
		byte[] png = new byte[100]
		// PNG signature
		png[0] = (byte) 0x89
		png[1] = (byte) 0x50
		png[2] = (byte) 0x4E
		png[3] = (byte) 0x47
		png[4] = (byte) 0x0D
		png[5] = (byte) 0x0A
		png[6] = (byte) 0x1A
		png[7] = (byte) 0x0A
		// Invalid chunk data
		return png
	}
}
