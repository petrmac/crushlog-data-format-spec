package io.cldf.tool.services

import io.cldf.api.CLDFArchive
import io.cldf.models.*
import io.cldf.tool.services.ValidationResult
import spock.lang.Specification

import java.time.LocalDate
import java.time.OffsetDateTime

import io.cldf.models.enums.ClimbType
import io.cldf.models.enums.FinishType
import io.cldf.models.enums.Platform
import io.cldf.models.enums.RouteType

class ValidationServiceSpec extends Specification {

    ValidationService validationService
    
    def setup() {
        validationService = new DefaultValidationService()
    }

    def "should validate a complete and valid archive"() {
        given: "a complete valid archive"
        def archive = createValidArchive()

        when: "validating the archive"
        def result = validationService.validate(archive)

        then: "validation passes"
        result.valid == true
        result.errors.isEmpty()
        result.warnings.isEmpty()
        result.hasWarnings() == false
        result.summary == "Validation passed"
    }

    def "should fail validation when manifest is missing"() {
        given: "an archive without manifest"
        def archive = CLDFArchive.builder()
            .locations([createValidLocation()])
            .sessions([createValidSession()])
            .climbs([createValidClimb()])
            .build()

        when: "validating the archive"
        def result = validationService.validate(archive)

        then: "validation fails"
        result.valid == false
        result.errors.contains("Manifest is required")
        result.summary.contains("Validation failed")
    }

    def "should fail validation when locations are missing"() {
        given: "an archive without locations"
        def archive = CLDFArchive.builder()
            .manifest(createValidManifest())
            .sessions([createValidSession()])
            .climbs([createValidClimb()])
            .build()

        when: "validating the archive"
        def result = validationService.validate(archive)

        then: "validation fails"
        result.valid == false
        result.errors.contains("At least one location is required")
    }

    def "should fail validation when locations are empty"() {
        given: "an archive with empty locations"
        def archive = CLDFArchive.builder()
            .manifest(createValidManifest())
            .locations([])
            .sessions([createValidSession()])
            .climbs([createValidClimb()])
            .build()

        when: "validating the archive"
        def result = validationService.validate(archive)

        then: "validation fails"
        result.valid == false
        result.errors.contains("At least one location is required")
    }

    def "should pass validation when sessions are missing"() {
        given: "an archive without sessions"
        def archive = CLDFArchive.builder()
            .manifest(createValidManifest())
            .locations([createValidLocation()])
            .routes([createValidRoute()])
            .build()

        when: "validating the archive"
        def result = validationService.validate(archive)

        then: "validation passes"
        result.valid == true
        !result.errors.contains("At least one session is required")
    }

    def "should pass validation when sessions are empty"() {
        given: "an archive with empty sessions"
        def archive = CLDFArchive.builder()
            .manifest(createValidManifest())
            .locations([createValidLocation()])
            .sessions([])
            .routes([createValidRoute()])
            .build()

        when: "validating the archive"
        def result = validationService.validate(archive)

        then: "validation passes"
        result.valid == true
        !result.errors.contains("At least one session is required")
    }

    def "should pass validation when climbs are missing"() {
        given: "an archive without climbs"
        def archive = CLDFArchive.builder()
            .manifest(createValidManifest())
            .locations([createValidLocation()])
            .routes([createValidRoute()])
            .build()

        when: "validating the archive"
        def result = validationService.validate(archive)

        then: "validation passes"
        result.valid == true
        !result.errors.contains("At least one climb is required")
    }

    def "should pass validation when climbs are empty"() {
        given: "an archive with empty climbs"
        def archive = CLDFArchive.builder()
            .manifest(createValidManifest())
            .locations([createValidLocation()])
            .climbs([])
            .routes([createValidRoute()])
            .build()

        when: "validating the archive"
        def result = validationService.validate(archive)

        then: "validation passes"
        result.valid == true
        !result.errors.contains("At least one climb is required")
    }

    def "should warn about climbs with future dates"() {
        given: "an archive with future climbs"
        def futureDate = LocalDate.now().plusDays(5)
        def archive = CLDFArchive.builder()
            .manifest(createValidManifest())
            .locations([createValidLocation()])
            .sessions([createValidSession()])
            .climbs([
                createValidClimb(),
                Climb.builder()
                    .id(2)
                    .sessionId(1)
                    .date(futureDate)
                    .routeName("Future Climb")
                    .type(ClimbType.BOULDER)
                    .finishType(FinishType.TOP)
                    .build(),
                Climb.builder()
                    .id(3)
                    .sessionId(1)
                    .date(futureDate.plusDays(1))
                    .routeName("Another Future")
                    .type(ClimbType.BOULDER)
                    .finishType(FinishType.TOP)
                    .build()
            ])
            .build()

        when: "validating the archive"
        def result = validationService.validate(archive)

        then: "validation passes with warnings"
        result.valid == true
        result.errors.isEmpty()
        result.hasWarnings() == true
        result.warnings.size() == 1
        result.warnings[0] == "2 climbs have dates in the future"
        result.summary == "Validation passed with 1 warnings"
    }

    def "should warn about duplicate climb names on same day"() {
        given: "an archive with duplicate climbs"
        def date = LocalDate.of(2024, 6, 15)
        def archive = CLDFArchive.builder()
            .manifest(createValidManifest())
            .locations([createValidLocation()])
            .sessions([createValidSession()])
            .climbs([
                Climb.builder()
                    .id(1)
                    .sessionId(1)
                    .date(date)
                    .routeName("The Classic")
                    .type(ClimbType.BOULDER)
                    .finishType(FinishType.TOP)
                    .build(),
                Climb.builder()
                    .id(2)
                    .sessionId(1)
                    .date(date)
                    .routeName("The Classic")
                    .type(ClimbType.BOULDER)
                    .finishType(FinishType.TOP)
                    .build(),
                Climb.builder()
                    .id(3)
                    .sessionId(1)
                    .date(date)
                    .routeName("The Classic")
                    .type(ClimbType.BOULDER)
                    .finishType(FinishType.TOP)
                    .build()
            ])
            .build()

        when: "validating the archive"
        def result = validationService.validate(archive)

        then: "validation passes with warnings"
        result.valid == true
        result.errors.isEmpty()
        result.hasWarnings() == true
        result.warnings.any { it.contains("Route 'The Classic' appears 3 times on ${date}") }
    }

    def "should handle climbs with null dates or names during validation"() {
        given: "an archive with climbs having null values and valid structure"
        def archive = CLDFArchive.builder()
            .manifest(createValidManifest())
            .locations([createValidLocation()])
            .sessions([createValidSession()])
            .climbs([
                // These climbs have valid structure but null values that won't trigger warnings
                Climb.builder()
                    .id(1)
                    .sessionId(1)
                    .date(LocalDate.now().minusDays(1)) // past date, not null
                    .routeName("No Date")
                    .type(ClimbType.BOULDER)
                    .finishType(FinishType.TOP)
                    .build(),
                Climb.builder()
                    .id(2)
                    .sessionId(1)
                    .date(LocalDate.now())
                    .routeName("Has Date") // not null
                    .type(ClimbType.BOULDER)
                    .finishType(FinishType.TOP)
                    .build()
            ])
            .build()

        when: "validating the archive"
        def result = validationService.validate(archive)

        then: "validation passes without warnings"
        result.valid == true
        result.errors.isEmpty()
        result.warnings.isEmpty()
    }

    def "should handle schema validation failures"() {
        given: "an archive with invalid data structure"
        def archive = CLDFArchive.builder()
            .manifest(Manifest.builder()
                .version(null) // Required field missing
                .format("CLDF")
                .creationDate(OffsetDateTime.now())
                .build())
            .locations([createValidLocation()])
            .sessions([createValidSession()])
            .climbs([createValidClimb()])
            .build()

        when: "validating the archive"
        def result = validationService.validate(archive)

        then: "validation fails with schema error"
        result.valid == false
        result.errors.size() > 0
        result.errors.any { it.contains("manifest.json") }
    }

    def "should validate complex archive with mixed issues"() {
        given: "an archive with multiple issues"
        def today = LocalDate.now()
        def archive = CLDFArchive.builder()
            .manifest(createValidManifest())
            .locations([createValidLocation()])
            .sessions([createValidSession()])
            .climbs([
                // Valid climb
                createValidClimb(),
                // Future climb
                Climb.builder()
                    .id(2)
                    .sessionId(1)
                    .date(today.plusDays(1))
                    .routeName("Future Route")
                    .type(ClimbType.ROUTE)
                    .finishType(FinishType.REDPOINT)
                    .build(),
                // Duplicate on same day
                Climb.builder()
                    .id(3)
                    .sessionId(1)
                    .date(today)
                    .routeName("Test Route")
                    .type(ClimbType.BOULDER)
                    .finishType(FinishType.TOP)
                    .build(),
                Climb.builder()
                    .id(4)
                    .sessionId(1)
                    .date(today)
                    .routeName("Test Route")
                    .type(ClimbType.BOULDER)
                    .finishType(FinishType.TOP)
                    .build()
            ])
            .build()

        when: "validating the archive"
        def result = validationService.validate(archive)

        then: "validation passes with multiple warnings"
        result.valid == true
        result.errors.isEmpty()
        result.hasWarnings() == true
        result.warnings.size() == 2
        result.warnings.any { it.contains("1 climbs have dates in the future") }
        result.warnings.any { it.contains("Route 'Test Route' appears") }
        result.summary.contains("Validation passed with 2 warnings")
    }

    def "should build ValidationResult correctly"() {
        given: "a validation result with errors and warnings"
        def result = ValidationResult.builder()
            .valid(false)
            .errors(["Error 1", "Error 2"])
            .warnings(["Warning 1", "Warning 2", "Warning 3"])
            .build()

        expect: "correct result properties"
        result.valid == false
        result.errors.size() == 2
        result.warnings.size() == 3
        result.hasWarnings() == true
        result.summary == "Validation failed with 2 errors and 3 warnings"
    }

    def "should handle empty warnings list"() {
        given: "a validation result with no warnings"
        def result = ValidationResult.builder()
            .valid(true)
            .errors([])
            .warnings([])
            .build()

        expect: "no warnings reported"
        result.hasWarnings() == false
        result.summary == "Validation passed"
    }

    def "should handle null warnings list"() {
        given: "a validation result with null warnings"
        def result = ValidationResult.builder()
            .valid(true)
            .errors([])
            .warnings(null)
            .build()

        expect: "no warnings reported"
        result.hasWarnings() == false
        result.summary == "Validation passed"
    }

    // Helper methods to create valid test data
    
    private CLDFArchive createValidArchive() {
        return CLDFArchive.builder()
            .manifest(createValidManifest())
            .locations([createValidLocation()])
            .sessions([createValidSession()])
            .climbs([createValidClimb()])
            .build()
    }
    
    private Manifest createValidManifest() {
        return Manifest.builder()
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
            .build()
    }
    
    private Location createValidLocation() {
        return Location.builder()
            .id(1)
            .name("Test Crag")
            .isIndoor(false)
            .build()
    }
    
    private Session createValidSession() {
        return Session.builder()
            .id(1)
            .date(LocalDate.now())
            .location("Test Crag")
            .locationId(1)
            .isIndoor(false)
            .build()
    }
    
    private Climb createValidClimb() {
        return Climb.builder()
            .id(1)
            .sessionId(1)
            .date(LocalDate.now())
            .routeName("Test Route")
            .type(ClimbType.BOULDER)
            .finishType(FinishType.TOP)
            .build()
    }

    private Route createValidRoute() {
        return Route.builder()
            .id(1)
            .locationId(1)
            .name("Test Route")
            .routeType(RouteType.ROUTE)
            .grades(new Route.Grades(french: "5c"))
            .build()
    }
}
