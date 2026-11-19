package app.crushlog.cldf.models

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import spock.lang.Specification

import java.nio.file.Files
import java.time.OffsetDateTime

import app.crushlog.cldf.api.CLDFArchive
import app.crushlog.cldf.api.CLDFReader
import app.crushlog.cldf.api.CLDFWriter
import app.crushlog.cldf.models.enums.ClimbType
import app.crushlog.cldf.models.enums.FinishType
import app.crushlog.cldf.models.enums.GradeScaleSpread
import app.crushlog.cldf.models.enums.GradeSystem
import app.crushlog.cldf.models.enums.Platform
import app.crushlog.cldf.utils.GradeAxis

class GradeScalesSpec extends Specification {

	// Configured exactly as CLDFReader configures its mapper.
	ObjectMapper readerMapper

	def setup() {
		readerMapper = new ObjectMapper()
		readerMapper.registerModule(new JavaTimeModule())
	}

	def "a level climb's grades are read"() {
		given: "grades as an app writes them for a climb logged on a level"
		def json = '''
			{
				"id": 1,
				"date": "2026-11-19",
				"type": "boulder",
				"finishType": "top",
				"grades": {
					"system": "font",
					"grade": "6A",
					"numeric": 400,
					"estimated": true,
					"scale": { "id": "6f1c2a9e-0b7d-4c55-9a3e-2d8f4b1c7e10", "level": 3 }
				}
			}
		'''

		when:
		def climb = readerMapper.readValue(json, Climb)

		then:
		climb.grades.grade == "6A"
		climb.grades.numeric == 400
		climb.grades.estimated
		climb.grades.scale.id == "6f1c2a9e-0b7d-4c55-9a3e-2d8f4b1c7e10"
		climb.grades.scale.level == 3
	}

	def "grades written before these fields read as before"() {
		when:
		def grades = readerMapper.readValue('{"system": "font", "grade": "6A"}', Climb.GradeInfo)

		then:
		grades.numeric == null
		grades.estimated == null
		grades.scale == null

		and: "the value is computed when the file does not carry it"
		grades.effectiveNumeric() == 400
	}

	def "the stored value wins over the computed one"() {
		expect:
		Climb.GradeInfo.builder().system(GradeSystem.FONT).grade("6A").numeric(425).build().effectiveNumeric() == 425
		Climb.GradeInfo.builder().system(GradeSystem.FONT).grade("blue").build().effectiveNumeric() == null
	}

	def "the difficulty axis matches the specification"() {
		expect:
		GradeAxis.numericFor(system, grade) == value

		where:
		system              | grade   | value
		GradeSystem.FONT    | "4"     | 0
		GradeSystem.FONT    | "4A"    | 25
		GradeSystem.FONT    | "5A+"   | 150
		GradeSystem.FONT    | "6A"    | 400
		GradeSystem.FONT    | "6A+"   | 450
		GradeSystem.FONT    | "7A+"   | 800
		GradeSystem.FONT    | "7B+"   | 950
		GradeSystem.FONT    | "9A"    | 1800
		GradeSystem.V_SCALE | "VB"    | 0
		GradeSystem.V_SCALE | "V3"    | 400
		GradeSystem.V_SCALE | "V17"   | 1800
		GradeSystem.FRENCH  | "4a"    | 0
		GradeSystem.FRENCH  | "6a"    | 600
		GradeSystem.FRENCH  | "9c+"   | 2900
		GradeSystem.YDS     | "5.10a" | 400
		GradeSystem.YDS     | "5.16b" | 2900
		GradeSystem.UIAA    | "VII"   | 900
		GradeSystem.UIAA    | "XIV-"  | 2900
		GradeSystem.FONT    | "6A/B"  | null
		GradeSystem.V_SCALE | "V3-4"  | null
	}

	def "every ladder is strictly increasing and has the specified length"() {
		expect:
		def ladder = GradeAxis.ladder(system)
		ladder.size() == size
		(1..<ladder.size()).every {
			GradeAxis.numericFor(system, ladder[it]) > GradeAxis.numericFor(system, ladder[it - 1])
		}

		where:
		system              | size
		GradeSystem.FONT    | 29
		GradeSystem.V_SCALE | 19
		GradeSystem.FRENCH  | 30
		GradeSystem.YDS     | 30
		GradeSystem.UIAA    | 30
	}

	def "a value becomes the nearest grade, ties going to the easier"() {
		expect:
		GradeAxis.gradeFor(system, value) == grade

		where:
		system              | value | grade
		GradeSystem.FONT    | 400   | "6A"
		GradeSystem.FONT    | 450   | "6A+"
		GradeSystem.V_SCALE | 450   | "V3"
		GradeSystem.V_SCALE | 451   | "V4"
		GradeSystem.V_SCALE | -10   | "VB"
		GradeSystem.V_SCALE | 99999 | "V17"
	}

	def "a location's scales are read, removed ones included"() {
		given:
		def json = '''
			{
				"id": 1, "name": "Gym", "isIndoor": true,
				"gradeScales": [{
					"id": "6f1c2a9e-0b7d-4c55-9a3e-2d8f4b1c7e10",
					"climbType": "boulder",
					"spread": "widerTop",
					"updatedAt": "2026-11-19T10:00:00.000Z",
					"deletedAt": "2026-11-20T08:00:00.000Z",
					"levels": [
						{ "level": 1, "startGrade": "4", "startNumeric": 0 },
						{ "level": 2, "startGrade": "5C", "startNumeric": 300, "color": "blue" }
					]
				}]
			}
		'''

		when:
		def location = readerMapper.readValue(json, Location)
		def scale = location.gradeScales[0]

		then:
		scale.id == "6f1c2a9e-0b7d-4c55-9a3e-2d8f4b1c7e10"
		scale.climbType == ClimbType.BOULDER
		scale.spread == GradeScaleSpread.WIDER_TOP
		scale.updatedAt.toInstant() == OffsetDateTime.parse("2026-11-19T10:00:00Z").toInstant()
		scale.deleted
		scale.levels.size() == 2
		scale.levelNumbered(2).startGrade == "5C"
		scale.levelNumbered(2).startNumeric == 300
		scale.levelNumbered(2).color == "blue"
		scale.levelNumbered(9) == null
	}

	def "a location without scales reads as before"() {
		expect:
		readerMapper.readValue('{"id": 1, "name": "Gym", "isIndoor": true}', Location).gradeScales == null
	}

	def "scales and level climbs survive writing and reading an archive"() {
		given:
		def file = Files.createTempFile("grade-scales", ".cldf").toFile()
		def updatedAt = OffsetDateTime.parse("2026-11-19T10:00:00Z")
		def archive = CLDFArchive.builder()
				.manifest(Manifest.builder()
				.version("1.0.0")
				.format("CLDF")
				.creationDate(OffsetDateTime.parse("2026-11-19T10:30:00Z"))
				.appVersion("1.3.0")
				.platform(Platform.IOS)
				.build())
				.locations([
					Location.builder()
					.id(1)
					.name("Gym")
					.isIndoor(true)
					.gradeScales([
						GradeScale.builder()
						.id("6f1c2a9e-0b7d-4c55-9a3e-2d8f4b1c7e10")
						.climbType(ClimbType.BOULDER)
						.spread(GradeScaleSpread.EVEN)
						.updatedAt(updatedAt)
						.levels([
							GradeScale.Level.builder().level(1).startGrade("4").startNumeric(0).build(),
							GradeScale.Level.builder().level(2).startGrade("6A").startNumeric(400).color("red").build()
						])
						.build()
					])
					.build()
				])
				.sessions([
					Session.builder().id(1).date(java.time.LocalDate.parse("2026-11-19")).location("Gym").locationId(1).build()
				])
				.climbs([
					Climb.builder()
					.id(1)
					.sessionId(1)
					.date(java.time.LocalDate.parse("2026-11-19"))
					.type(ClimbType.BOULDER)
					.finishType(FinishType.TOP)
					.grades(Climb.GradeInfo.builder()
					.system(GradeSystem.FONT)
					.grade("6A")
					.numeric(400)
					.estimated(true)
					.scale(Climb.ScaleRef.builder().id("6f1c2a9e-0b7d-4c55-9a3e-2d8f4b1c7e10").level(2).build())
					.build())
					.build()
				])
				.build()

		when: "written and read with schema validation on"
		new CLDFWriter(false, true).write(archive, file)
		def read = new CLDFReader(true, true).read(file)

		then:
		def scale = read.locations[0].gradeScales[0]
		scale.id == "6f1c2a9e-0b7d-4c55-9a3e-2d8f4b1c7e10"
		scale.spread == GradeScaleSpread.EVEN
		scale.updatedAt.toInstant() == updatedAt.toInstant()
		scale.deletedAt == null
		scale.levels*.startNumeric == [0, 400]
		scale.levels[1].color == "red"

		and:
		def grades = read.climbs[0].grades
		grades.grade == "6A"
		grades.numeric == 400
		grades.estimated
		grades.scale.id == "6f1c2a9e-0b7d-4c55-9a3e-2d8f4b1c7e10"
		grades.scale.level == 2

		cleanup:
		file?.delete()
	}

	def "a field from a later version of the format does not stop an archive being read"() {
		given: "a mapper configured as CLDFReader configures its own"
		def reader = new CLDFReader(false, false)
		def field = CLDFReader.getDeclaredField("objectMapper")
		field.accessible = true
		def mapper = field.get(reader) as ObjectMapper

		when:
		def climb = mapper.readValue('''
			{
				"id": 1, "date": "2026-11-19", "type": "boulder", "finishType": "top",
				"somethingNew": 1,
				"grades": { "system": "font", "grade": "6A", "addedLater": true }
			}
		''', Climb)

		then:
		climb.grades.grade == "6A"
	}
}
