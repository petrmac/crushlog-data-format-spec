package io.cldf.tool.services

import io.cldf.models.enums.ClimbType
import io.cldf.models.enums.FinishType
import spock.lang.Specification
import spock.lang.Unroll
import io.cldf.models.Climb
import io.cldf.models.Location
import io.cldf.models.Session
import java.time.LocalDate

class QueryServiceSpec extends Specification {

    QueryService queryService = new DefaultQueryService()

    def "should apply single equals filter to climbs"() {
        given: "a list of climbs"
        def climbs = [
            createClimb("Route 1", ClimbType.ROUTE, "5.10a", 3),
            createClimb("Route 2", ClimbType.BOULDER, "V5", 4),
            createClimb("Route 3", ClimbType.ROUTE, "5.11b", 5)
        ]

        when: "filtering by type equals route"
        def result = queryService.applyFilter(climbs, "type=route")

        then: "only route climbs are returned"
        result.size() == 2
        result.every { it.type == ClimbType.ROUTE }
    }

    def "should apply not equals filter"() {
        given: "a list of climbs"
        def climbs = [
            createClimb("Route 1", ClimbType.ROUTE, "5.10a", 3),
            createClimb("Route 2", ClimbType.BOULDER, "V5", 4),
            createClimb("Route 3", ClimbType.ROUTE, "5.11b", 5)
        ]

        when: "filtering by type not equals route"
        def result = queryService.applyFilter(climbs, "type!=route")

        then: "only non-route climbs are returned"
        result.size() == 1
        result[0].type == ClimbType.BOULDER
    }

    def "should apply numeric comparison filters"() {
        given: "a list of climbs with ratings"
        def climbs = [
            createClimb("Route 1", ClimbType.ROUTE, "5.10a", 3),
            createClimb("Route 2", ClimbType.BOULDER, "V5", 4),
            createClimb("Route 3", ClimbType.ROUTE, "5.11b", 5)
        ]

        when: "filtering by rating > 3"
        def result = queryService.applyFilter(climbs, "rating>3")

        then: "only climbs with rating > 3 are returned"
        result.size() == 2
        result.every { it.rating > 3 }
    }

    def "should apply multiple filters with AND"() {
        given: "a list of climbs"
        def climbs = [
            createClimb("Route 1", ClimbType.ROUTE, "5.10a", 3),
            createClimb("Route 2", ClimbType.BOULDER, "V5", 4),
            createClimb("Route 3", ClimbType.ROUTE, "5.11b", 5)
        ]

        when: "filtering by type=route AND rating>=5"
        def result = queryService.applyFilter(climbs, "type=route AND rating>=5")

        then: "only routes with rating >= 5 are returned"
        result.size() == 1
        result[0].routeName == "Route 3"
        result[0].rating == 5
    }

    def "should handle quoted string values"() {
        given: "a list of climbs"
        def climbs = [
            createClimb("The Route", ClimbType.ROUTE, "5.10a", 3),
            createClimb("Boulder Problem", ClimbType.BOULDER, "V5", 4)
        ]

        when: "filtering by routeName with quotes"
        def result = queryService.applyFilter(climbs, "routeName='The Route'")

        then: "climb with matching name is returned"
        result.size() == 1
        result[0].routeName == "The Route"
    }

    def "should handle nested field access"() {
        given: "a climb with grades"
        def climb = createClimb("Route 1", ClimbType.ROUTE, "5.10a", 3)
        
        when: "filtering by nested grade field"
        def result = queryService.applyFilter([climb], "grade=5.10a")

        then: "climb with matching grade is returned"
        result.size() == 1
    }

    def "should handle boolean fields"() {
        given: "climbs with indoor/outdoor settings"
        def climbs = [
            createClimb("Indoor Route", ClimbType.ROUTE, "5.10a", 3, true),
            createClimb("Outdoor Route", ClimbType.ROUTE, "5.10b", 4, false)
        ]

        when: "filtering by isIndoor=true"
        def result = queryService.applyFilter(climbs, "isIndoor=true")

        then: "only indoor climbs are returned"
        result.size() == 1
        result[0].isIndoor == true
    }

    def "should handle date comparison"() {
        given: "climbs with dates"
        def today = LocalDate.now()
        def yesterday = today.minusDays(1)
        def tomorrow = today.plusDays(1)
        
        def climbs = [
            createClimbWithDate("Route 1", yesterday),
            createClimbWithDate("Route 2", today),
            createClimbWithDate("Route 3", tomorrow)
        ]

        when: "filtering by date >= today"
        def result = queryService.applyFilter(climbs, "date>=${today}")

        then: "only climbs from today or later are returned"
        result.size() == 2
        result.every { it.date >= today }
    }

    def "should handle invalid filter expressions"() {
        given: "a list of climbs"
        def climbs = [createClimb("Route 1", ClimbType.ROUTE, "5.10a", 3)]

        when: "applying invalid filter"
        def result = queryService.applyFilter(climbs, "invalid filter")

        then: "original list is returned"
        result.size() == climbs.size()
    }

    def "should handle null field values"() {
        given: "a climb with null rating"
        def climb = new Climb()
        climb.routeName = "Test"
        climb.rating = null

        when: "filtering by rating"
        def result = queryService.applyFilter([climb], "rating=5")

        then: "no results returned"
        result.isEmpty()
    }

    def "should sort climbs by field ascending"() {
        given: "unsorted climbs"
        def climbs = [
            createClimb("Route C", ClimbType.ROUTE, "5.10a", 3),
            createClimb("Route A", ClimbType.ROUTE, "5.10a", 5),
            createClimb("Route B", ClimbType.ROUTE, "5.10a", 4)
        ]

        when: "sorting by routeName"
        def result = queryService.sort(climbs, "routeName")

        then: "climbs are sorted alphabetically"
        result[0].routeName == "Route A"
        result[1].routeName == "Route B"
        result[2].routeName == "Route C"
    }

    def "should sort climbs by field descending"() {
        given: "unsorted climbs"
        def climbs = [
            createClimb("Route 1", ClimbType.ROUTE, "5.10a", 3),
            createClimb("Route 2", ClimbType.ROUTE, "5.10a", 5),
            createClimb("Route 3", ClimbType.ROUTE, "5.10a", 4)
        ]

        when: "sorting by rating descending"
        def result = queryService.sort(climbs, "-rating")

        then: "climbs are sorted by rating descending"
        result[0].rating == 5
        result[1].rating == 4
        result[2].rating == 3
    }

    def "should handle sorting with null values"() {
        given: "climbs with some null ratings"
        def climbs = [
            createClimb("Route 1", ClimbType.ROUTE, "5.10a", null),
            createClimb("Route 2", ClimbType.ROUTE, "5.10a", 5),
            createClimb("Route 3", ClimbType.ROUTE, "5.10a", 3)
        ]

        when: "sorting by rating"
        def result = queryService.sort(climbs, "rating")

        then: "null values are sorted first"
        result[0].rating == null
        result[1].rating == 3
        result[2].rating == 5
    }

    def "should calculate climb statistics"() {
        given: "a list of climbs"
        def climbs = [
            createClimb("Route 1", ClimbType.ROUTE, "5.10a", 3, true, FinishType.ONSIGHT),
            createClimb("Route 2", ClimbType.BOULDER, "V5", 4, false, FinishType.FLASH),
            createClimb("Route 3", ClimbType.ROUTE, "5.11b", 5, true, FinishType.REDPOINT),
            createClimb("Route 4", ClimbType.BOULDER, "V6", 4, false, FinishType.FLASH)
        ]

        when: "calculating statistics"
        def stats = queryService.calculateStatistics(climbs, "climb")

        then: "statistics are calculated correctly"
        stats.total == 4
        stats.byType == ["route": 2L, "boulder": 2L]
        stats.byFinishType == ["onsight": 1L, "flash": 2L, "redpoint": 1L]
        stats.averageRating == 4.0
        stats.indoorCount == 2L
        stats.outdoorCount == 2L
    }

    def "should calculate session statistics"() {
        given: "a list of sessions"
        def sessions = [
            createSession("Gym A", true),
            createSession("Crag B", false),
            createSession("Gym A", true),
            createSession("Crag C", false)
        ]

        when: "calculating statistics"
        def stats = queryService.calculateStatistics(sessions, "session")

        then: "statistics are calculated correctly"
        stats.total == 4
        stats.byLocation == ["Gym A": 2L, "Crag B": 1L, "Crag C": 1L]
        stats.indoorCount == 2L
        stats.outdoorCount == 2L
    }

    def "should calculate location statistics"() {
        given: "a list of locations"
        def locations = [
            createLocation("Gym A", "USA", true),
            createLocation("Crag B", "USA", false),
            createLocation("Gym C", "Canada", true),
            createLocation("Crag D", "Canada", false)
        ]

        when: "calculating statistics"
        def stats = queryService.calculateStatistics(locations, "location")

        then: "statistics are calculated correctly"
        stats.total == 4
        stats.byCountry == ["USA": 2L, "Canada": 2L]
        stats.indoorCount == 2L
        stats.outdoorCount == 2L
    }

    def "should handle empty list for statistics"() {
        when: "calculating statistics on empty list"
        def stats = queryService.calculateStatistics([], "climb")

        then: "only total is returned"
        stats.total == 0
        stats.size() == 1
    }

    def "should compare grades correctly"() {
        given: "climbs with V-scale grades"
        def climbs = [
            createClimb("Boulder 1", ClimbType.BOULDER, "V5", 3),
            createClimb("Boulder 2", ClimbType.BOULDER, "V3", 3),
            createClimb("Boulder 3", ClimbType.BOULDER, "V10", 3)
        ]

        when: "filtering by grade > V4"
        def result = queryService.applyFilter(climbs, "grade>V4")

        then: "only V5 and V10 are returned"
        result.size() == 2
        result.any { it.grades.grade == "V5" }
        result.any { it.grades.grade == "V10" }
    }

    @Unroll
    def "should handle different operator types: #operator"() {
        given: "a climb with rating 4"
        def climbs = [createClimb("Route", ClimbType.ROUTE, "5.10a", 4)]

        when: "applying filter"
        def result = queryService.applyFilter(climbs, "rating${operator}${value}")

        then: "result matches expectation"
        result.size() == expectedSize

        where:
        operator | value | expectedSize
        "="      | "4"   | 1
        "!="     | "4"   | 0
        ">"      | "3"   | 1
        ">"      | "4"   | 0
        ">="     | "4"   | 1
        "<"      | "5"   | 1
        "<"      | "4"   | 0
        "<="     | "4"   | 1
    }

    def "should handle reflection-based field access"() {
        given: "a climb with attempts field"
        def climb = createClimb("Route", ClimbType.ROUTE, "5.10a", 4)
        climb.attempts = 3

        when: "filtering by attempts"
        def result = queryService.applyFilter([climb], "attempts=3")

        then: "climb is found"
        result.size() == 1
    }

    def "should handle enum comparison"() {
        given: "climbs with finish types"
        def climbs = [
            createClimb("Route 1", ClimbType.ROUTE, "5.10a", 3, true, FinishType.ONSIGHT),
            createClimb("Route 2", ClimbType.ROUTE, "5.10a", 3, true, FinishType.FLASH)
        ]

        when: "filtering by finishType"
        def result = queryService.applyFilter(climbs, "finishType=onsight")

        then: "only onsight climb is returned"
        result.size() == 1
        result[0].finishType == FinishType.ONSIGHT
    }

    def "should return original list for null or empty filter"() {
        given: "a list of climbs"
        def climbs = [createClimb("Route", ClimbType.ROUTE, "5.10a", 3)]

        when: "applying null filter"
        def result1 = queryService.applyFilter(climbs, null)

        and: "applying empty filter"
        def result2 = queryService.applyFilter(climbs, "")

        then: "original list is returned"
        result1 == climbs
        result2 == climbs
    }

    def "should return original list for null or empty sort"() {
        given: "a list of climbs"
        def climbs = [createClimb("Route", ClimbType.ROUTE, "5.10a", 3)]

        when: "applying null sort"
        def result1 = queryService.sort(climbs, null)

        and: "applying empty sort"
        def result2 = queryService.sort(climbs, "")

        then: "original list is returned"
        result1 == climbs
        result2 == climbs
    }

    def "should handle field access for sessions"() {
        given: "sessions with different locations"
        def session1 = new Session()
        session1.location = "Gym A"
        session1.isIndoor = true
        def session2 = new Session()
        session2.location = "Crag B"
        session2.isIndoor = false

        when: "filtering by location"
        def result = queryService.applyFilter([session1, session2], 
            "location='Gym A'")

        then: "only gym session is returned"
        result.size() == 1
        result[0].location == "Gym A"
    }

    // Helper methods
    private Climb createClimb(String name, ClimbType type, String grade, Integer rating,
                              Boolean isIndoor = true, FinishType finishType = null) {
        def climb = new Climb()
        climb.routeName = name
        climb.type = type
        climb.grades = new Climb.GradeInfo()
        climb.grades.grade = grade
        climb.rating = rating
        climb.isIndoor = isIndoor
        climb.finishType = finishType
        climb.date = LocalDate.now()
        return climb
    }

    private Climb createClimbWithDate(String name, LocalDate date) {
        def climb = createClimb(name, ClimbType.ROUTE, "5.10a", 3)
        climb.date = date
        return climb
    }

    private Session createSession(String location, Boolean isIndoor) {
        def session = new Session()
        session.location = location
        session.isIndoor = isIndoor
        session.date = LocalDate.now()
        return session
    }

    private Location createLocation(String name, String country, Boolean isIndoor) {
        def location = new Location()
        location.name = name
        location.country = country
        location.isIndoor = isIndoor
        return location
    }
}