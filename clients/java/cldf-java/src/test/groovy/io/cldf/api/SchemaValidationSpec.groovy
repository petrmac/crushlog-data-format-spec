package io.cldf.api

import com.fasterxml.jackson.databind.ObjectMapper
import io.cldf.models.*
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Path
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime

import io.cldf.models.enums.ClimbType
import io.cldf.models.enums.BelayType
import io.cldf.models.enums.FinishType
import io.cldf.models.enums.GradeSystem
import io.cldf.models.enums.RouteType
import io.cldf.models.enums.ProtectionRating
import io.cldf.models.enums.Platform

class SchemaValidationSpec extends Specification {

	@TempDir
	Path tempDir

	def "should validate valid archive when schema validation is enabled"() {
		given: "a valid archive"
		def archive = createValidArchive()
		def outputFile = tempDir.resolve("valid.cldf").toFile()
		def writer = new CLDFWriter(true, true) // pretty print and validate schemas

		when: "writing the archive"
		writer.write(archive, outputFile)

		then: "file is created successfully"
		outputFile.exists()
		outputFile.length() > 0

		and: "can be read back with validation"
		def reader = new CLDFReader(true, true)
		def readArchive = reader.read(outputFile)
		readArchive != null
		readArchive.manifest.version == "1.0.0"
	}

	def "should throw exception when writing invalid manifest"() {
		given: "an archive with invalid manifest (missing required field)"
		def archive = createValidArchive()
		archive.manifest = Manifest.builder()
				.format("CLDF")
				// Missing required fields: version, creationDate, appVersion, platform
				.build()
		def outputFile = tempDir.resolve("invalid-manifest.cldf").toFile()
		def writer = new CLDFWriter(true, true)

		when: "writing the archive"
		writer.write(archive, outputFile)

		then: "validation exception is thrown"
		def e = thrown(IOException)
		e.message.contains("Schema validation failed")
		e.message.contains("manifest.json")
	}

	def "should throw exception when reading invalid data with validation enabled"() {
		given: "an archive with invalid data written without validation"
		def archive = createValidArchive()
		// Create climb with missing required fields that would fail schema validation
		archive.climbs[0] = Climb.builder()
				.id(1)
				.sessionId(1)
				// Missing required fields: date, routeName, type, finishType
				.build()

		def outputFile = tempDir.resolve("invalid-climb.cldf").toFile()
		def writer = new CLDFWriter(true, false) // Write without validation
		writer.write(archive, outputFile)

		when: "reading with validation enabled"
		def reader = new CLDFReader(true, true)
		reader.read(outputFile)

		then: "validation exception is thrown"
		def e = thrown(IOException)
		e.message.contains("Schema validation failed")
	}

	def "should allow reading/writing when validation is disabled"() {
		given: "an archive that might not pass strict validation"
		def archive = createValidArchive()
		// Create climb with missing required fields that would fail validation
		archive.climbs[0] = Climb.builder()
				.id(1)
				.sessionId(1)
				// Missing required fields like date, routeName, type, finishType
				.build()
		def outputFile = tempDir.resolve("custom-format.cldf").toFile()
		def writer = new CLDFWriter(true, false) // No validation

		when: "writing and reading without validation"
		writer.write(archive, outputFile)
		def reader = new CLDFReader(true, false) // No validation
		def readArchive = reader.read(outputFile)

		then: "operations succeed"
		outputFile.exists()
		readArchive != null
		readArchive.climbs[0].id == 1
		readArchive.climbs[0].sessionId == 1
		readArchive.climbs[0].date == null
		readArchive.climbs[0].routeName == null
	}

	def "should validate optional files when present"() {
		given: "an archive with invalid optional data"
		def archive = createValidArchive()
		archive.routes = [
			Route.builder()
			.id("route-1")
			.locationId("1")
			.name("Test Route")
			// Missing required routeType field
			.build()
		]
		def outputFile = tempDir.resolve("invalid-route.cldf").toFile()
		def writer = new CLDFWriter(true, true)

		when: "writing the archive"
		writer.write(archive, outputFile)

		then: "validation exception is thrown"
		def e = thrown(IOException)
		e.message.contains("Schema validation failed")
		e.message.contains("routes.json")
	}

	def "should validate author fields in manifest"() {
		given: "an archive with author information"
		def archive = createValidArchive()
		archive.manifest = Manifest.builder()
				.version("1.0.0")
				.format("CLDF")
				.creationDate(OffsetDateTime.now())
				.appVersion("1.0")
				.platform(Platform.DESKTOP)
				.author(Manifest.Author.builder()
				.name("Test Author")
				.email("test@example.com")
				.website("https://example.com")
				.build())
				.source("Test Application")
				.build()

		def outputFile = tempDir.resolve("with-author.cldf").toFile()
		def writer = new CLDFWriter(true, true)

		when: "writing and reading the archive"
		writer.write(archive, outputFile)
		def reader = new CLDFReader(true, true)
		def readArchive = reader.read(outputFile)

		then: "validation passes and author data is preserved"
		readArchive.manifest.author.name == "Test Author"
		readArchive.manifest.author.email == "test@example.com"
		readArchive.manifest.source == "Test Application"
	}

	def "should validate protection rating enum in routes"() {
		given: "a route with protection rating"
		def archive = createValidArchive()
		archive.routes = [
			Route.builder()
			.id("route-1")
			.locationId("1")
			.name("Trad Route")
			.routeType(RouteType.ROUTE)
			.protectionRating(ProtectionRating.ADEQUATE)
			.build()
		]

		def outputFile = tempDir.resolve("route-protection.cldf").toFile()
		def writer = new CLDFWriter(true, true)

		when: "writing and reading the archive"
		writer.write(archive, outputFile)
		def reader = new CLDFReader(true, true)
		def readArchive = reader.read(outputFile)

		then: "validation passes"
		readArchive.routes[0].protectionRating == ProtectionRating.ADEQUATE
	}

	def "should use SchemaValidator directly"() {
		given: "a schema validator"
		def validator = new SchemaValidator()
		def manifest = Manifest.builder()
				.version("1.0.0")
				.format("CLDF")
				.creationDate(OffsetDateTime.now())
				.appVersion("1.0")
				.platform(Platform.DESKTOP)
				.build()

		when: "validating a valid manifest"
		def objectMapper = new ObjectMapper()
				.findAndRegisterModules()
		objectMapper.setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
		objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
		def json = objectMapper.writeValueAsBytes(manifest)
		def result = validator.validate("manifest.json", json)

		then: "validation passes"
		result == true

		when: "validating invalid data"
		def invalidJson = '{"format": "CLDF"}'.bytes // Missing required fields
		validator.validateOrThrow("manifest.json", invalidJson)

		then: "validation fails"
		thrown(IOException)
	}

	private CLDFArchive createValidArchive() {
		return CLDFArchive.builder()
				.manifest(Manifest.builder()
				.version("1.0.0")
				.format("CLDF")
				.creationDate(OffsetDateTime.now())
				.appVersion("1.0")
				.platform(Platform.DESKTOP)
				.stats(Manifest.Stats.builder()
				.climbsCount(1)
				.locationsCount(1)
				.sessionsCount(1)
				.build())
				.build())
				.locations([
					Location.builder()
					.id(1)
					.name("Test Location")
					.isIndoor(false)
					.build()
				])
				.climbs([
					Climb.builder()
					.id(1)
					.sessionId(1)
					.routeId("1")
					.date(LocalDate.now())
					.time(LocalTime.of(14, 30, 0))
					.routeName("Test Route")
					.type(ClimbType.ROUTE)
					.finishType(FinishType.REDPOINT)
					.grades(Climb.GradeInfo.builder()
					.system(GradeSystem.YDS)
					.grade("5.10a")
					.build())
					.belayType(BelayType.LEAD)
					.attempts(1)
					.duration(15)
					.falls(0)
					.height(10.0)
					.rating(4)
					.notes("Test climb")
					.build()
				])
				.sessions([
					Session.builder()
					.id("1")
					.date(LocalDate.now())
					.location("Test Location")
					.locationId("1")
					.isIndoor(false)
					.build()
				])
				.build()
	}
}
