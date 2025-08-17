package app.crushlog.cldf.tool.commands

import spock.lang.Specification
import spock.lang.Unroll
import app.crushlog.cldf.models.Route

class QRCommandGradePatternSpec extends Specification {

    def command = new QRCommand.GenerateCommand()

    @Unroll
    def "should correctly identify YDS grade: #grade"() {
        given:
        def grades = new Route.Grades()

        when:
        command.setGradeByPattern(grades, grade)

        then:
        grades.yds == grade
        grades.vScale == null
        grades.french == null

        where:
        grade << ["5.0", "5.9", "5.10", "5.10a", "5.10b", "5.10c", "5.10d", 
                  "5.11a", "5.11b", "5.12", "5.12a", "5.12b+", "5.13a-", "5.14", "5.15"]
    }

    @Unroll
    def "should correctly identify V-Scale grade: #grade"() {
        given:
        def grades = new Route.Grades()

        when:
        command.setGradeByPattern(grades, grade)

        then:
        grades.vScale == grade
        grades.yds == null
        grades.french == null

        where:
        grade << ["V0", "V1", "V2", "V5", "V10", "V11", "V12", "V15", "V16", "V17", "V0+", "V5-"]
    }

    @Unroll
    def "should correctly identify French grade: #grade"() {
        given:
        def grades = new Route.Grades()

        when:
        command.setGradeByPattern(grades, grade)

        then:
        grades.french == grade
        grades.yds == null
        grades.vScale == null

        where:
        grade << ["4", "5a", "5b", "5c", "6a", "6a+", "6b", "6b+", "6c", "7a", "7a+", "8a", "8b", "8c", "9a"]
    }

    @Unroll
    def "should default unknown patterns to YDS: #grade"() {
        given:
        def grades = new Route.Grades()

        when:
        command.setGradeByPattern(grades, grade)

        then:
        grades.yds == grade
        grades.vScale == null
        grades.french == null

        where:
        grade << ["WI4", "M7", "A2", "C1", "5.10a/b", "UIAA VII", "unknown"]
    }

    def "should handle malicious input without ReDoS"() {
        given:
        def grades = new Route.Grades()
        // Create a string that would cause catastrophic backtracking with .* regex
        def maliciousInput = "5." + ("a" * 1000) + "!"

        when:
        def startTime = System.nanoTime()
        command.setGradeByPattern(grades, maliciousInput)
        def endTime = System.nanoTime()
        def durationMs = (endTime - startTime) / 1_000_000

        then:
        // Should complete quickly (under 100ms) even with malicious input
        durationMs < 100
        // Should default to YDS when pattern doesn't match
        grades.yds == maliciousInput
        grades.vScale == null
        grades.french == null
    }

    def "should handle extremely long V-scale input without ReDoS"() {
        given:
        def grades = new Route.Grades()
        // Create a string that would cause issues with V\\d+.*
        def maliciousInput = "V9" + ("9" * 1000)

        when:
        def startTime = System.nanoTime()
        command.setGradeByPattern(grades, maliciousInput)
        def endTime = System.nanoTime()
        def durationMs = (endTime - startTime) / 1_000_000

        then:
        // Should complete quickly
        durationMs < 100
        // Should not match V-scale pattern (only allows 2 digits)
        grades.yds == maliciousInput  // defaults to YDS
        grades.vScale == null
        grades.french == null
    }
}