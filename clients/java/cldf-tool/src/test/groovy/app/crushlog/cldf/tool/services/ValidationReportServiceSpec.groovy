package app.crushlog.cldf.tool.services

import app.crushlog.cldf.api.CLDFArchive
import app.crushlog.cldf.api.CLDFWriter
import app.crushlog.cldf.models.*
import app.crushlog.cldf.models.media.MediaItem
import app.crushlog.cldf.models.enums.MediaType
import app.crushlog.cldf.models.enums.Platform
import app.crushlog.cldf.tool.services.ValidationReportService.ValidationOptions
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Path
import java.security.MessageDigest
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class ValidationReportServiceSpec extends Specification {

	@TempDir
	Path tempDir

	ValidationService mockValidationService = Mock()
	DefaultValidationReportService service = new DefaultValidationReportService(mockValidationService)

	def "should validate archive with all checks enabled"() {
		given: "a CLDF archive with valid data"
		def archive = createTestArchive()
		def fileName = "test.cldf"
		def options = new ValidationOptions(true, true, true)

		and: "mock validation service returns success"
		mockValidationService.validate(_) >> new ValidationResult(true, [], [])

		when: "validating the archive"
		def report = service.validateArchive(archive, fileName, options)

		then: "report shows validation passed"
		report.valid
		report.structureValid
		report.file == fileName
		report.errors.isEmpty()
		report.warnings.isEmpty()
		report.statistics.locations == 2
		report.statistics.routes == 3
		report.statistics.climbs == 1
		report.statistics.sessions == 1
	}

	def "should handle validation failures"() {
		given: "a CLDF archive with invalid data"
		def archive = createTestArchive()
		def fileName = "test.cldf"
		def options = new ValidationOptions()

		and: "mock validation service returns errors"
		def errors = ["Missing required field: name", "Invalid CLID format"]
		def warnings = ["Deprecated field used: old_field"]
		mockValidationService.validate(_) >> new ValidationResult(false, errors, warnings)

		when: "validating the archive"
		def report = service.validateArchive(archive, fileName, options)

		then: "report shows validation failed"
		!report.valid
		!report.structureValid
		report.errors == errors
		report.warnings == warnings
	}

	def "should validate checksums when file is provided"() {
		given: "a CLDF archive file with checksums"
		def fixedTime = OffsetDateTime.now()
		def archiveFile = createArchiveFileWithChecksumsAndTime(fixedTime)
		def archive = createArchiveWithChecksumsAndTime(fixedTime)
		def options = new ValidationOptions(false, true, false)

		when: "validating with checksum verification"
		def report = service.validateArchive(archive, archiveFile.name, archiveFile, options)

		then: "checksums are validated"
		report.checksumResult != null
		report.checksumResult.algorithm == "SHA-256"
		report.checksumResult.valid
		report.checksumResult.results.size() > 0
	}

	def "should detect checksum mismatches"() {
		given: "a CLDF archive with incorrect checksums"
		def archiveFile = createArchiveFileWithChecksums()
		def archive = createArchiveWithWrongChecksums()
		def options = new ValidationOptions(false, true, false)

		when: "validating with checksum verification"
		def report = service.validateArchive(archive, archiveFile.name, archiveFile, options)

		then: "checksum mismatch is detected"
		!report.valid
		report.checksumResult != null
		!report.checksumResult.valid
		report.errors.any { it.contains("Checksum mismatch") }
	}

	def "should skip checksum validation when disabled"() {
		given: "validation options with checksums disabled"
		def archive = createArchiveWithChecksums()
		def fileName = "test.cldf"
		def options = new ValidationOptions(true, false, true)

		and: "mock validation service"
		mockValidationService.validate(_) >> new ValidationResult(true, [], [])

		when: "validating without checksums"
		def report = service.validateArchive(archive, fileName, options)

		then: "no checksum validation is performed"
		report.checksumResult == null
		report.valid
	}

	def "should handle strict mode"() {
		given: "strict validation options"
		def options = ValidationOptions.fromFlags(false, false, false, true)

		expect: "all validations are enabled"
		options.validateSchema
		options.validateChecksums
		options.validateReferences
		options.strict
	}

	def "should validate file directly"() {
		given: "a CLDF archive file"
		def archiveFile = createArchiveFileWithChecksums()
		def options = new ValidationOptions()

		and: "mock validation service"
		mockValidationService.validate(_) >> new ValidationResult(true, [], [])

		when: "validating the file"
		def report = service.validateFile(archiveFile, options)

		then: "validation is performed"
		report != null
		report.file == archiveFile.name
		report.checksumResult != null
	}

	def "should handle file not found"() {
		given: "a non-existent file"
		def file = new File(tempDir.toFile(), "non-existent.cldf")

		when: "validating the file"
		def report = service.validateFile(file)

		then: "error report is generated"
		!report.valid
		report.errors.any { it.contains("File not found") }
	}

	def "should gather correct statistics"() {
		given: "an archive with various content"
		def archive = createComplexArchive()
		def fileName = "complex.cldf"

		and: "mock validation service"
		mockValidationService.validate(_) >> new ValidationResult(true, [], [])

		when: "validating the archive"
		def report = service.validateArchive(archive, fileName)

		then: "statistics are correct"
		report.statistics.locations == 3
		report.statistics.routes == 5
		report.statistics.sectors == 2
		report.statistics.climbs == 10
		report.statistics.sessions == 4
		report.statistics.tags == 6
		report.statistics.mediaItems == 8
	}

	// Helper methods

	private CLDFArchive createTestArchive() {
		def archive = new CLDFArchive()
		archive.manifest = createManifest()
		archive.locations = createLocations()
		archive.routes = createRoutes()
		archive.climbs = [createClimb()]
		archive.sessions = [createSession()]
		return archive
	}

	private CLDFArchive createArchiveWithChecksums() {
		def archive = createTestArchive()
		
		// Calculate checksums for the actual JSON content that will be in the file
		def manifestData = [
			version: "1.0.0",
			format: "CLDF",
			creationDate: OffsetDateTime.now().toString(),
			appVersion: "1.0.0",
			platform: "Desktop"
		]
		def manifestJson = new groovy.json.JsonBuilder(manifestData).toString()
		
		def locationsData = [
			locations: [
				[id: 1, name: "Test Location", country: "US", state: "CA", city: "Test", isIndoor: false]
			]
		]
		def locationsJson = new groovy.json.JsonBuilder(locationsData).toString()
		
		archive.checksums = new Checksums(
			algorithm: "SHA-256",
			files: [
				"manifest.json": calculateSHA256(manifestJson),
				"locations.json": calculateSHA256(locationsJson)
			],
			generatedAt: OffsetDateTime.now()
		)
		return archive
	}

	private CLDFArchive createArchiveWithWrongChecksums() {
		def archive = createTestArchive()
		archive.checksums = new Checksums(
			algorithm: "SHA-256",
			files: [
				"manifest.json": "wrong_checksum_value",
				"locations.json": "another_wrong_checksum"
			],
			generatedAt: OffsetDateTime.now()
		)
		return archive
	}

	private CLDFArchive createComplexArchive() {
		def archive = new CLDFArchive()
		archive.manifest = createManifest()
		archive.locations = (1..3).collect { createLocation(it) }
		archive.routes = (1..5).collect { createRoute(it) }
		archive.sectors = (1..2).collect { createSector(it) }
		archive.climbs = (1..10).collect { createClimb(it) }
		archive.sessions = (1..4).collect { createSession(it) }
		archive.tags = (1..6).collect { createTag(it) }
		archive.mediaItems = (1..8).collect { createMediaItem(it) }
		return archive
	}

	private File createArchiveFileWithChecksums() {
		def file = new File(tempDir.toFile(), "test.cldf")
		
		// Create valid JSON content for manifest
		def manifestData = [
			version: "1.0.0",
			format: "CLDF",
			creationDate: OffsetDateTime.now().toString(),
			appVersion: "1.0.0",
			platform: "Desktop"
		]
		def manifestJson = new groovy.json.JsonBuilder(manifestData).toString()
		
		// Create valid JSON content for locations
		def locationsData = [
			locations: [
				[id: 1, name: "Test Location", country: "US", state: "CA", city: "Test", isIndoor: false]
			]
		]
		def locationsJson = new groovy.json.JsonBuilder(locationsData).toString()
		
		file.withOutputStream { fos ->
			def zos = new ZipOutputStream(fos)
			
			// Add manifest.json
			zos.putNextEntry(new ZipEntry("manifest.json"))
			zos.write(manifestJson.bytes)
			zos.closeEntry()
			
			// Add locations.json
			zos.putNextEntry(new ZipEntry("locations.json"))
			zos.write(locationsJson.bytes)
			zos.closeEntry()
			
			// Add checksums.json (not included in checksum calculation)
			def checksums = [
				algorithm: "SHA-256",
				files: [
					"manifest.json": calculateSHA256(manifestJson),
					"locations.json": calculateSHA256(locationsJson)
				],
				generatedAt: OffsetDateTime.now().toString()
			]
			zos.putNextEntry(new ZipEntry("checksums.json"))
			zos.write(new groovy.json.JsonBuilder(checksums).toString().bytes)
			zos.closeEntry()
			
			zos.close()
		}
		return file
	}

	private File createArchiveFileWithChecksumsAndTime(OffsetDateTime fixedTime) {
		def file = new File(tempDir.toFile(), "test.cldf")
		
		// Create valid JSON content for manifest with fixed time
		def manifestData = [
			version: "1.0.0",
			format: "CLDF",
			creationDate: fixedTime.toString(),
			appVersion: "1.0.0",
			platform: "Desktop"
		]
		def manifestJson = new groovy.json.JsonBuilder(manifestData).toString()
		
		// Create valid JSON content for locations
		def locationsData = [
			locations: [
				[id: 1, name: "Test Location", country: "US", state: "CA", city: "Test", isIndoor: false]
			]
		]
		def locationsJson = new groovy.json.JsonBuilder(locationsData).toString()
		
		file.withOutputStream { fos ->
			def zos = new ZipOutputStream(fos)
			
			// Add manifest.json
			zos.putNextEntry(new ZipEntry("manifest.json"))
			zos.write(manifestJson.bytes)
			zos.closeEntry()
			
			// Add locations.json
			zos.putNextEntry(new ZipEntry("locations.json"))
			zos.write(locationsJson.bytes)
			zos.closeEntry()
			
			// Add checksums.json (not included in checksum calculation)
			def checksums = [
				algorithm: "SHA-256",
				files: [
					"manifest.json": calculateSHA256(manifestJson),
					"locations.json": calculateSHA256(locationsJson)
				],
				generatedAt: fixedTime.toString()
			]
			zos.putNextEntry(new ZipEntry("checksums.json"))
			zos.write(new groovy.json.JsonBuilder(checksums).toString().bytes)
			zos.closeEntry()
			
			zos.close()
		}
		return file
	}
	
	private CLDFArchive createArchiveWithChecksumsAndTime(OffsetDateTime fixedTime) {
		def archive = createTestArchive()
		
		// Calculate checksums for the actual JSON content with fixed time
		def manifestData = [
			version: "1.0.0",
			format: "CLDF",
			creationDate: fixedTime.toString(),
			appVersion: "1.0.0",
			platform: "Desktop"
		]
		def manifestJson = new groovy.json.JsonBuilder(manifestData).toString()
		
		def locationsData = [
			locations: [
				[id: 1, name: "Test Location", country: "US", state: "CA", city: "Test", isIndoor: false]
			]
		]
		def locationsJson = new groovy.json.JsonBuilder(locationsData).toString()
		
		archive.checksums = new Checksums(
			algorithm: "SHA-256",
			files: [
				"manifest.json": calculateSHA256(manifestJson),
				"locations.json": calculateSHA256(locationsJson)
			],
			generatedAt: fixedTime
		)
		return archive
	}

	private String calculateSHA256(String data) {
		def digest = MessageDigest.getInstance("SHA-256")
		def hash = digest.digest(data.bytes)
		return hash.encodeHex().toString()
	}

	private Manifest createManifest() {
		def manifest = new Manifest()
		manifest.version = "1.0.0"
		manifest.format = "CLDF"
		manifest.creationDate = OffsetDateTime.now()
		manifest.appVersion = "1.0.0"
		manifest.platform = Platform.DESKTOP
		return manifest
	}

	private List<Location> createLocations() {
		return [
			createLocation(1),
			createLocation(2)
		]
	}

	private Location createLocation(int id = 1) {
		return new Location(
			id: id,
			name: "Test Location $id",
			country: "US",
			state: "CA",
			city: "Test City",
			isIndoor: false
		)
	}

	private List<Route> createRoutes() {
		return (1..3).collect { createRoute(it) }
	}

	private Route createRoute(int id) {
		return new Route(
			id: id,
			name: "Test Route $id",
			locationId: 1,
			grades: new Route.Grades(yds: "5.10a")
		)
	}

	private Sector createSector(int id) {
		return new Sector(
			id: id,
			name: "Test Sector $id",
			locationId: 1
		)
	}

	private Climb createClimb(int id = 1) {
		def climb = new Climb()
		climb.id = id
		climb.routeId = 1
		climb.sessionId = 1
		climb.date = LocalDate.now()
		climb.type = app.crushlog.cldf.models.enums.ClimbType.ROUTE
		climb.finishType = app.crushlog.cldf.models.enums.FinishType.REDPOINT
		climb.attempts = 1
		// No sends field in Climb model
		return climb
	}

	private Session createSession(int id = 1) {
		return new Session(
			id: id,
			date: LocalDate.now(),
			locationId: 1
		)
	}

	private Tag createTag(int id) {
		return new Tag(
			id: id,
			name: "Tag $id",
			category: "test"
		)
	}

	private MediaItem createMediaItem(int id) {
		def media = new MediaItem()
		media.type = MediaType.PHOTO
		media.path = "/photos/photo${id}.jpg"
		media.assetId = "asset-${id}"
		media.caption = "Test photo ${id}"
		return media
	}
}
