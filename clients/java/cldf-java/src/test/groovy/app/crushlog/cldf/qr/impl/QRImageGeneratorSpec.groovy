package app.crushlog.cldf.qr.impl

import app.crushlog.cldf.qr.QRImageOptions
import spock.lang.Specification
import spock.lang.Unroll

import javax.imageio.ImageIO
import java.awt.*
import java.awt.image.BufferedImage

class QRImageGeneratorSpec extends Specification {

	def imageGenerator = new QRImageGenerator()

	def "should generate QR code image with default options"() {
		given:
		def data = "https://crushlog.pro/test"
		def options = QRImageOptions.builder().build()

		when:
		BufferedImage image = imageGenerator.generateImage(data, options)

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
		BufferedImage image = imageGenerator.generateImage(data, options)

		then:
		image != null
		image.getWidth() == 512
		image.getHeight() == 512
	}

	def "should generate QR code image with custom colors"() {
		given:
		def data = "Custom Colors"
		def options = QRImageOptions.builder()
				.foregroundColor(Color.BLUE)
				.backgroundColor(Color.YELLOW)
				.build()

		when:
		BufferedImage image = imageGenerator.generateImage(data, options)

		then:
		image != null
		// Verify colors were applied (check a sample pixel)
		def rgb = image.getRGB(0, 0)
		def color = new Color(rgb)
		// Background should be yellow
		color == Color.YELLOW || color == Color.BLUE
	}

	@Unroll
	def "should generate QR code with error correction level: #level"() {
		given:
		def data = "Error Correction Test"
		def options = QRImageOptions.builder()
				.errorCorrectionLevel(level)
				.build()

		when:
		BufferedImage image = imageGenerator.generateImage(data, options)

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
				.foregroundColor(Color.BLACK)
				.backgroundColor(Color.WHITE)
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
		svg.contains("fill=\"#000000\"") // Black foreground
		svg.contains("fill=\"#ffffff\"") // White background
	}

	def "should generate SVG with custom colors"() {
		given:
		def data = "Custom SVG colors"
		def options = QRImageOptions.builder()
				.foregroundColor(new Color(255, 0, 0)) // Red
				.backgroundColor(new Color(0, 255, 0)) // Green
				.build()

		when:
		String svg = imageGenerator.generateSVG(data, options)

		then:
		svg != null
		svg.contains("fill=\"#ff0000\"") // Red in hex
		svg.contains("fill=\"#00ff00\"") // Green in hex
	}

	def "should generate QR code with different margins"() {
		given:
		def data = "Margin test"
		def options = QRImageOptions.builder()
				.margin(10)
				.size(256)
				.build()

		when:
		BufferedImage image = imageGenerator.generateImage(data, options)

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
		imageGenerator.generateImage(data, options)

		then:
		thrown(IllegalArgumentException)
	}

	def "should handle very long data"() {
		given:
		def data = "A" * 1000 // 1000 characters
		def options = QRImageOptions.builder()
				.errorCorrectionLevel(QRImageOptions.ErrorCorrectionLevel.L)
				.build()

		when:
		BufferedImage image = imageGenerator.generateImage(data, options)

		then:
		image != null
		image.getWidth() == 256
	}

	def "should handle special characters in data"() {
		given:
		def data = "Special chars: !@#\$%^&*()_+-=[]{}|;':\",./<>?`~"
		def options = QRImageOptions.builder().build()

		when:
		BufferedImage image = imageGenerator.generateImage(data, options)

		then:
		image != null
		image.getWidth() == 256
	}

	def "should handle unicode characters"() {
		given:
		def data = "Unicode: 你好世界 🌍 Ñoño"
		def options = QRImageOptions.builder().build()

		when:
		BufferedImage image = imageGenerator.generateImage(data, options)

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
		def data = "https://crushlog.pro/g/550e8400?param=value&other=test"
		def options = QRImageOptions.builder().build()

		when:
		BufferedImage image = imageGenerator.generateImage(data, options)

		then:
		image != null
		image.getWidth() == 256
	}

	def "should generate QR code with JSON data"() {
		given:
		def data = '{"v":1,"clid":"clid:route:test","url":"https://crushlog.pro/g/test","route":{"name":"Test Route","grade":"5.11a"}}'
		def options = QRImageOptions.builder()
				.errorCorrectionLevel(QRImageOptions.ErrorCorrectionLevel.M)
				.build()

		when:
		BufferedImage image = imageGenerator.generateImage(data, options)

		then:
		image != null
		image.getWidth() == 256
	}
}
