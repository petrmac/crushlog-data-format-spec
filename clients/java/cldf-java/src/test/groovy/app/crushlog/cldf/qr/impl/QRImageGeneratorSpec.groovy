package app.crushlog.cldf.qr.impl

import app.crushlog.cldf.qr.QRImageOptions
import app.crushlog.cldf.qr.QRColor
import spock.lang.Specification
import spock.lang.Unroll

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

class QRImageGeneratorSpec extends Specification {

	def imageGenerator = new QRImageGenerator()

	def "should generate QR code image with default options"() {
		given:
		def data = "https://crushlog.pro/test"
		def options = QRImageOptions.builder().build()

		when:
		byte[] pngBytes = imageGenerator.generatePNG(data, options)
		BufferedImage image = ImageIO.read(new ByteArrayInputStream(pngBytes))

		then:
		image != null
		image.getWidth() == 256
		image.getHeight() == 256
	}

	def "should generate QR code image with custom size"() {
		given:
		def data = "Test QR Code"
		def options = QRImageOptions.builder()
				.size(512)
				.build()

		when:
		byte[] pngBytes = imageGenerator.generatePNG(data, options)
		BufferedImage image = ImageIO.read(new ByteArrayInputStream(pngBytes))

		then:
		image != null
		image.getWidth() == 512
		image.getHeight() == 512
	}

	def "should generate QR code image with custom colors"() {
		given:
		def data = "Custom Colors"
		def options = QRImageOptions.builder()
				.foregroundColor(new QRColor(0, 0, 255)) // Blue
				.backgroundColor(new QRColor(255, 255, 0)) // Yellow
				.build()

		when:
		byte[] pngBytes = imageGenerator.generatePNG(data, options)
		BufferedImage image = ImageIO.read(new ByteArrayInputStream(pngBytes))

		then:
		image != null
		// PNG was generated successfully
		pngBytes.length > 0
	}

	@Unroll
	def "should generate QR code with error correction level: #level"() {
		given:
		def data = "Error Correction Test"
		def options = QRImageOptions.builder()
				.errorCorrectionLevel(level)
				.build()

		when:
		byte[] pngBytes = imageGenerator.generatePNG(data, options)
		BufferedImage image = ImageIO.read(new ByteArrayInputStream(pngBytes))

		then:
		image != null
		image.getWidth() == 256

		where:
		level << [
			QRImageOptions.ErrorCorrectionLevel.L,
			QRImageOptions.ErrorCorrectionLevel.M,
			QRImageOptions.ErrorCorrectionLevel.Q,
			QRImageOptions.ErrorCorrectionLevel.H
		]
	}

	def "should generate QR code as PNG byte array"() {
		given:
		def data = "PNG byte array test"
		def options = QRImageOptions.builder()
				.size(128)
				.build()

		when:
		byte[] pngBytes = imageGenerator.generatePNG(data, options)

		then:
		pngBytes != null
		pngBytes.length > 0

		// Verify it's a valid PNG by reading it back
		when:
		ByteArrayInputStream bais = new ByteArrayInputStream(pngBytes)
		BufferedImage image = ImageIO.read(bais)

		then:
		image != null
		image.getWidth() == 128
		image.getHeight() == 128
	}

	def "should generate QR code as SVG string"() {
		given:
		def data = "SVG test data"
		def options = QRImageOptions.builder()
				.size(200)
				.foregroundColor(QRColor.BLACK)
				.backgroundColor(QRColor.WHITE)
				.build()

		when:
		String svg = imageGenerator.generateSVG(data, options)

		then:
		svg != null
		svg.contains("<svg")
		svg.contains("width=\"200\"")
		svg.contains("height=\"200\"")
		svg.contains("<rect")
		svg.contains("</svg>")
		svg.contains("fill=\"#000000\"") // Black foreground - toHex() returns uppercase
		svg.contains("fill=\"#FFFFFF\"") // White background - toHex() returns uppercase
	}

	def "should generate SVG with custom colors"() {
		given:
		def data = "Custom SVG colors"
		def options = QRImageOptions.builder()
				.foregroundColor(new QRColor(255, 0, 0)) // Red
				.backgroundColor(new QRColor(0, 255, 0)) // Green
				.build()

		when:
		String svg = imageGenerator.generateSVG(data, options)

		then:
		svg != null
		svg.contains("fill=\"#FF0000\"") // Red in hex - toHex() returns uppercase
		svg.contains("fill=\"#00FF00\"") // Green in hex - toHex() returns uppercase
	}

	def "should generate QR code with different margins"() {
		given:
		def data = "Margin test"
		def options = QRImageOptions.builder()
				.margin(10)
				.size(256)
				.build()

		when:
		byte[] pngBytes = imageGenerator.generatePNG(data, options)
		BufferedImage image = ImageIO.read(new ByteArrayInputStream(pngBytes))

		then:
		image != null
		image.getWidth() == 256
		image.getHeight() == 256
	}

	def "should handle empty data"() {
		given:
		def data = ""
		def options = QRImageOptions.builder().build()

		when:
		imageGenerator.generatePNG(data, options)

		then:
		thrown(RuntimeException)
	}

	def "should handle very long data"() {
		given:
		def data = "A" * 1000 // 1000 characters
		def options = QRImageOptions.builder()
				.errorCorrectionLevel(QRImageOptions.ErrorCorrectionLevel.L)
				.build()

		when:
		byte[] pngBytes = imageGenerator.generatePNG(data, options)
		BufferedImage image = ImageIO.read(new ByteArrayInputStream(pngBytes))

		then:
		image != null
		image.getWidth() == 256
	}

	def "should handle special characters in data"() {
		given:
		def data = "Special chars: !@#\$%^&*()_+-=[]{}|;':\",./<>?`~"
		def options = QRImageOptions.builder().build()

		when:
		byte[] pngBytes = imageGenerator.generatePNG(data, options)
		BufferedImage image = ImageIO.read(new ByteArrayInputStream(pngBytes))

		then:
		image != null
		image.getWidth() == 256
	}

	def "should handle unicode characters"() {
		given:
		def data = "Unicode: 你好世界 🌍 Ñoño"
		def options = QRImageOptions.builder().build()

		when:
		byte[] pngBytes = imageGenerator.generatePNG(data, options)
		BufferedImage image = ImageIO.read(new ByteArrayInputStream(pngBytes))

		then:
		image != null
		image.getWidth() == 256
	}

	def "should generate PNG with large size"() {
		given:
		def data = "Large QR code"
		def options = QRImageOptions.builder()
				.size(1024)
				.build()

		when:
		byte[] pngBytes = imageGenerator.generatePNG(data, options)

		then:
		pngBytes != null
		pngBytes.length > 500 // PNG file should be at least 500 bytes
	}

	def "should generate SVG with small size"() {
		given:
		def data = "Small QR"
		def options = QRImageOptions.builder()
				.size(64)
				.build()

		when:
		String svg = imageGenerator.generateSVG(data, options)

		then:
		svg != null
		svg.contains("width=\"64\"")
		svg.contains("height=\"64\"")
	}

	def "should handle URL data"() {
		given:
		def data = "https://crushlog.pro/route/123?param=value&other=test#anchor"
		def options = QRImageOptions.builder().build()

		when:
		byte[] pngBytes = imageGenerator.generatePNG(data, options)
		BufferedImage image = ImageIO.read(new ByteArrayInputStream(pngBytes))

		then:
		image != null
		image.getWidth() == 256
		image.getHeight() == 256
	}

	def "should generate QR code with JSON data"() {
		given:
		def data = """{"route": {"name": "The Nose", "grade": "5.14a"}}"""
		def options = QRImageOptions.builder()
				.errorCorrectionLevel(QRImageOptions.ErrorCorrectionLevel.M)
				.build()

		when:
		byte[] pngBytes = imageGenerator.generatePNG(data, options)
		BufferedImage image = ImageIO.read(new ByteArrayInputStream(pngBytes))

		then:
		image != null
		image.getWidth() == 256
		image.getHeight() == 256
	}
}