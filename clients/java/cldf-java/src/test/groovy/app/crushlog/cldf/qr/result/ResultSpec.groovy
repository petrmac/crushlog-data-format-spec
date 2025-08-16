package app.crushlog.cldf.qr.result

import spock.lang.Specification
import spock.lang.Unroll

class ResultSpec extends Specification {

	def "should create successful result"() {
		when:
		def result = Result.success("test value")

		then:
		result.isSuccess()
		!result.isFailure()
		result.getSuccess().isPresent()
		result.getSuccess().get() == "test value"
		result.getError().isEmpty()
	}

	def "should create failed result"() {
		when:
		def result = Result.failure("error message")

		then:
		!result.isSuccess()
		result.isFailure()
		result.getSuccess().isEmpty()
		result.getError().isPresent()
		result.getError().get() == "error message"
	}

	def "should map successful result"() {
		given:
		def result = Result.success(5)

		when:
		def mapped = result.map { v -> v * 2 }

		then:
		mapped.isSuccess()
		mapped.getSuccess().get() == 10
	}

	def "should not map failed result"() {
		given:
		def result = Result.failure("error")

		when:
		def mapped = result.map { v -> v + " mapped" }

		then:
		mapped.isFailure()
		mapped.getError().get() == "error"
	}

	def "should flatMap successful result"() {
		given:
		def result = Result.success(5)

		when:
		def flatMapped = result.flatMap { v -> Result.success(v * 2) }

		then:
		flatMapped.isSuccess()
		flatMapped.getSuccess().get() == 10
	}

	def "should flatMap to failure"() {
		given:
		def result = Result.success(5)

		when:
		def flatMapped = result.flatMap { v -> Result.failure("failed") }

		then:
		flatMapped.isFailure()
		flatMapped.getError().get() == "failed"
	}

	def "should not flatMap failed result"() {
		given:
		def result = Result.failure("error")

		when:
		def flatMapped = result.flatMap { v -> Result.success("new value") }

		then:
		flatMapped.isFailure()
		flatMapped.getError().get() == "error"
	}

	def "should map error"() {
		given:
		def result = Result.failure("error")

		when:
		def mapped = result.mapError { e -> "mapped: " + e }

		then:
		mapped.isFailure()
		mapped.getError().get() == "mapped: error"
	}

	def "should not map error for success"() {
		given:
		def result = Result.success("value")

		when:
		def mapped = result.mapError { e -> "mapped: " + e }

		then:
		mapped.isSuccess()
		mapped.getSuccess().get() == "value"
	}

	def "should use orElse for failure"() {
		given:
		def result = Result.failure("error")

		when:
		def value = result.orElse("default")

		then:
		value == "default"
	}

	def "should not use orElse for success"() {
		given:
		def result = Result.success("value")

		when:
		def value = result.orElse("default")

		then:
		value == "value"
	}

	def "should use orElseGet for failure"() {
		given:
		def result = Result.failure("error")
		def supplierCalled = false

		when:
		def value = result.orElseGet({
			supplierCalled = true
			"supplied"
		})

		then:
		value == "supplied"
		supplierCalled
	}

	def "should not use orElseGet for success"() {
		given:
		def result = Result.success("value")
		def supplierCalled = false

		when:
		def value = result.orElseGet({
			supplierCalled = true
			"supplied"
		})

		then:
		value == "value"
		!supplierCalled
	}

	def "should filter successful result when predicate passes"() {
		given:
		def result = Result.success(10)

		when:
		def filtered = result.filter({ v -> v > 5 }, "too small")

		then:
		filtered.isSuccess()
		filtered.getSuccess().get() == 10
	}

	def "should filter successful result when predicate fails"() {
		given:
		def result = Result.success(3)

		when:
		def filtered = result.filter({ v -> v > 5 }, "too small")

		then:
		filtered.isFailure()
		filtered.getError().get() == "too small"
	}

	def "should not filter failed result"() {
		given:
		def result = Result.failure("error")

		when:
		def filtered = result.filter({ v -> true }, "filter error")

		then:
		filtered.isFailure()
		filtered.getError().get() == "error"
	}

	def "should recover from failure"() {
		given:
		def result = Result.failure("error")

		when:
		def recovered = result.recover { e -> "recovered from " + e }

		then:
		recovered.isSuccess()
		recovered.getSuccess().get() == "recovered from error"
	}

	def "should not recover from success"() {
		given:
		def result = Result.success("value")

		when:
		def recovered = result.recover { e -> "recovered" }

		then:
		recovered.isSuccess()
		recovered.getSuccess().get() == "value"
	}

	def "should recover with new result from failure"() {
		given:
		def result = Result.failure("error")

		when:
		def recovered = result.recoverWith { e -> Result.success("recovered") }

		then:
		recovered.isSuccess()
		recovered.getSuccess().get() == "recovered"
	}

	def "should not recover with new result from success"() {
		given:
		def result = Result.success("value")

		when:
		def recovered = result.recoverWith { e -> Result.success("recovered") }

		then:
		recovered.isSuccess()
		recovered.getSuccess().get() == "value"
	}

	def "should execute consumer if success"() {
		given:
		def result = Result.success("value")
		def consumed = null

		when:
		result.ifSuccess { v -> consumed = v }

		then:
		consumed == "value"
	}

	def "should not execute consumer if failure"() {
		given:
		def result = Result.failure("error")
		def consumed = null

		when:
		result.ifSuccess { v -> consumed = v }

		then:
		consumed == null
	}

	def "should execute consumer if failure"() {
		given:
		def result = Result.failure("error")
		def consumed = null

		when:
		result.ifFailure { e -> consumed = e }

		then:
		consumed == "error"
	}

	def "should not execute consumer if success"() {
		given:
		def result = Result.success("value")
		def consumed = null

		when:
		result.ifFailure { e -> consumed = e }

		then:
		consumed == null
	}

	def "should chain operations fluently"() {
		given:
		def result = Result.success(5)

		when:
		def final_result = result
				.map { v -> v * 2 }
				.filter({ v -> v > 5 }, "too small")
				.map { v -> v + 3 }
				.flatMap { v -> Result.success(v.toString()) }

		then:
		final_result.isSuccess()
		final_result.getSuccess().get() == "13"
	}

	def "should short-circuit chain on failure"() {
		given:
		def result = Result.success(2)

		when:
		def final_result = result
				.map { v -> v * 2 }
				.filter({ v -> v > 5 }, "too small")
				.map { v -> v + 3 }
				.flatMap { v -> Result.success(v.toString()) }

		then:
		final_result.isFailure()
		final_result.getError().get() == "too small"
	}

	def "should tryExecute and capture success"() {
		when:
		def result = Result.tryExecute({ "success" })

		then:
		result.isSuccess()
		result.getSuccess().get() == "success"
	}

	def "should tryExecute and capture exception"() {
		when:
		def result = Result.tryExecute({
			throw new RuntimeException("test error")
		})

		then:
		result.isFailure()
		result.getError().get() == "test error"
	}

	def "should tryExecute with custom error mapper"() {
		when:
		def result = Result.tryExecute(
				{ throw new RuntimeException("test") }, { e ->
					"Mapped: " + e.getMessage()
				}
				)

		then:
		result.isFailure()
		result.getError().get() == "Mapped: test"
	}

	def "should throw on orElseThrow for failure"() {
		given:
		def result = Result.failure("error")

		when:
		result.orElseThrow()

		then:
		thrown(NoSuchElementException)
	}

	def "should not throw on orElseThrow for success"() {
		given:
		def result = Result.success("value")

		when:
		def value = result.orElseThrow()

		then:
		value == "value"
		noExceptionThrown()
	}

	def "should throw custom exception on failure"() {
		given:
		def result = Result.failure("error")

		when:
		result.orElseThrow({ new IllegalStateException("custom") })

		then:
		def e = thrown(IllegalStateException)
		e.message == "custom"
	}
}
