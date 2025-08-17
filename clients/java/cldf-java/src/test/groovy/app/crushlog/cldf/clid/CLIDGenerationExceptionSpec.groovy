package app.crushlog.cldf.clid

import spock.lang.Specification

class CLIDGenerationExceptionSpec extends Specification {

	def "should create exception with message"() {
		when:
		def exception = new CLIDGenerationException("Test message")

		then:
		exception.message == "Test message"
		exception.cause == null
	}

	def "should create exception with message and cause"() {
		given:
		def cause = new RuntimeException("Cause message")

		when:
		def exception = new CLIDGenerationException("Test message", cause)

		then:
		exception.message == "Test message"
		exception.cause == cause
		exception.cause.message == "Cause message"
	}

	def "should be a RuntimeException"() {
		when:
		def exception = new CLIDGenerationException("Test")

		then:
		exception instanceof RuntimeException
	}
}
