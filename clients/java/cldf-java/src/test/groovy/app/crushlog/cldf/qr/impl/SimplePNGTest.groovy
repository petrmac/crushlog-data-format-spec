package app.crushlog.cldf.qr.impl

import spock.lang.Specification
import com.google.zxing.common.BitMatrix

class SimplePNGTest extends Specification {

	def "test simple PNG generation and reading"() {
		given:
		PureJavaPNGGenerator generator = new PureJavaPNGGenerator()
		PureJavaImageReader reader = new PureJavaImageReader()

		// Create a simple 10x10 matrix
		BitMatrix matrix = new BitMatrix(10, 10)
		matrix.set(0, 0)
		matrix.set(5, 5)

		when:
		// Generate PNG at 100x100 size
		byte[] png = generator.generatePNG(matrix, 100, 0x000000, 0xFFFFFF)
		println "Generated PNG size: ${png.length} bytes"

		// Read it back
		def source = reader.createLuminanceSource(png)

		then:
		source != null
		println "Read dimensions: ${source.getWidth()}x${source.getHeight()}"
		source.getWidth() == 100
		source.getHeight() == 100
	}
}
