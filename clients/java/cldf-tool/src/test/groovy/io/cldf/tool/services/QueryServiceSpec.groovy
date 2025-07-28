package io.cldf.tool.services

import io.cldf.models.Climb
import io.cldf.models.Location
import io.cldf.models.Session
import spock.lang.Specification
import spock.lang.Subject

import java.time.LocalDate

class QueryServiceSpec extends Specification {
    
    @Subject
    QueryService queryService = new QueryService()
    
    def "should apply equals filter"() {
        given:
        def climbs = [
            createClimb("Boulder 1", Climb.ClimbType.boulder, "V3"),
            createClimb("Route 1", Climb.ClimbType.route, "5.10a"),
            createClimb("Boulder 2", Climb.ClimbType.boulder, "V5")
        ]
        
        when:
        def result = queryService.applyFilter(climbs, "type=boulder")
        
        then:
        result.size() == 2
        result.every { (it as Climb).type == Climb.ClimbType.boulder }
    }
    
    def "should apply not equals filter"() {
        given:
        def climbs = [
            createClimb("Boulder 1", Climb.ClimbType.boulder, "V3"),
            createClimb("Route 1", Climb.ClimbType.route, "5.10a"),
            createClimb("Boulder 2", Climb.ClimbType.boulder, "V5")
        ]
        
        when:
        def result = queryService.applyFilter(climbs, "type!=boulder")
        
        then:
        result.size() == 1
        (result[0] as Climb).type == Climb.ClimbType.route
    }
    
    def "should apply greater than filter for numbers"() {
        given:
        def climbs = [
            createClimb("C1", Climb.ClimbType.boulder, "V1", 3),
            createClimb("C2", Climb.ClimbType.boulder, "V2", 4),
            createClimb("C3", Climb.ClimbType.boulder, "V3", 5)
        ]
        
        when:
        def result = queryService.applyFilter(climbs, "rating>3")
        
        then:
        result.size() == 2
        result.every { (it as Climb).rating > 3 }
    }
    
    def "should apply greater than or equals filter"() {
        given:
        def climbs = [
            createClimb("C1", Climb.ClimbType.boulder, "V1", 3),
            createClimb("C2", Climb.ClimbType.boulder, "V2", 4),
            createClimb("C3", Climb.ClimbType.boulder, "V3", 5)
        ]
        
        when:
        def result = queryService.applyFilter(climbs, "rating>=4")
        
        then:
        result.size() == 2
        result.every { (it as Climb).rating >= 4 }
    }
    
    def "should apply less than filter"() {
        given:
        def climbs = [
            createClimb("C1", Climb.ClimbType.boulder, "V1", 3),
            createClimb("C2", Climb.ClimbType.boulder, "V2", 4),
            createClimb("C3", Climb.ClimbType.boulder, "V3", 5)
        ]
        
        when:
        def result = queryService.applyFilter(climbs, "rating<4")
        
        then:
        result.size() == 1
        (result[0] as Climb).rating == 3
    }
    
    def "should apply grade comparison filters"() {
        given:
        def climbs = [
            createClimb("C1", Climb.ClimbType.boulder, "V1"),
            createClimb("C2", Climb.ClimbType.boulder, "V3"),
            createClimb("C3", Climb.ClimbType.boulder, "V5")
        ]
        
        when:
        def result = queryService.applyFilter(climbs, "grade>=V3")
        
        then:
        result.size() == 2
        (result[0] as Climb).grades.grade == "V3"
        (result[1] as Climb).grades.grade == "V5"
    }
    
    def "should apply multiple AND filters"() {
        given:
        def climbs = [
            createClimb("C1", Climb.ClimbType.boulder, "V1", 3),
            createClimb("C2", Climb.ClimbType.boulder, "V3", 4),
            createClimb("C3", Climb.ClimbType.route, "5.10a", 4)
        ]
        
        when:
        def result = queryService.applyFilter(climbs, "type=boulder AND rating>=4")
        
        then:
        result.size() == 1
        def climb = result[0] as Climb
        climb.type == Climb.ClimbType.boulder
        climb.rating == 4
    }
    
    def "should handle quoted string values"() {
        given:
        def locations = [
            Location.builder().name("Local Gym").build(),
            Location.builder().name("Boulder Canyon").build()
        ]
        
        when:
        def result1 = queryService.applyFilter(locations, "name='Local Gym'")
        def result2 = queryService.applyFilter(locations, 'name="Boulder Canyon"')
        
        then:
        result1.size() == 1
        (result1[0] as Location).name == "Local Gym"
        result2.size() == 1
        (result2[0] as Location).name == "Boulder Canyon"
    }
    
    def "should sort ascending"() {
        given:
        def climbs = [
            createClimb("C1", Climb.ClimbType.boulder, "V3", 5),
            createClimb("C2", Climb.ClimbType.boulder, "V1", 3),
            createClimb("C3", Climb.ClimbType.boulder, "V2", 4)
        ]
        
        when:
        def result = queryService.sort(climbs, "rating")
        
        then:
        (result[0] as Climb).rating == 3
        (result[1] as Climb).rating == 4
        (result[2] as Climb).rating == 5
    }
    
    def "should sort descending"() {
        given:
        def climbs = [
            createClimb("C1", Climb.ClimbType.boulder, "V1", 3),
            createClimb("C2", Climb.ClimbType.boulder, "V3", 5),
            createClimb("C3", Climb.ClimbType.boulder, "V2", 4)
        ]
        
        when:
        def result = queryService.sort(climbs, "-rating")
        
        then:
        (result[0] as Climb).rating == 5
        (result[1] as Climb).rating == 4
        (result[2] as Climb).rating == 3
    }
    
    def "should sort by date"() {
        given:
        def sessions = [
            Session.builder().id("1").date(LocalDate.now()).build(),
            Session.builder().id("2").date(LocalDate.now().minusDays(7)).build(),
            Session.builder().id("3").date(LocalDate.now().minusDays(3)).build()
        ]
        
        when:
        def result = queryService.sort(sessions, "date")
        
        then:
        (result[0] as Session).id == "2"
        (result[1] as Session).id == "3"
        (result[2] as Session).id == "1"
    }
    
    def "should handle null values in sorting"() {
        given:
        def climbs = [
            createClimb("C1", Climb.ClimbType.boulder, "V1", null),
            createClimb("C2", Climb.ClimbType.boulder, "V2", 4),
            createClimb("C3", Climb.ClimbType.boulder, "V3", 5)
        ]
        
        when:
        def result = queryService.sort(climbs, "rating")
        
        then:
        (result[0] as Climb).rating == null
        (result[1] as Climb).rating == 4
        (result[2] as Climb).rating == 5
    }
    
    def "should calculate climb statistics"() {
        given:
        def climbs = [
            createClimb("C1", Climb.ClimbType.boulder, "V1", 3, true),
            createClimb("C2", Climb.ClimbType.boulder, "V2", 4, true),
            createClimb("C3", Climb.ClimbType.route, "5.10a", 5, false),
            createClimb("C4", Climb.ClimbType.route, "5.11a", 4, false)
        ]
        
        when:
        def stats = queryService.calculateStatistics(climbs, "climbs")
        
        then:
        stats.total == 4
        stats.byType == [boulder: 2L, route: 2L]
        stats.averageRating == 4.0
        stats.indoorCount == 2
        stats.outdoorCount == 2
    }
    
    def "should handle invalid filter expressions gracefully"() {
        given:
        def climbs = [createClimb("C1", Climb.ClimbType.boulder, "V1")]
        
        when:
        def result = queryService.applyFilter(climbs, "invalid filter")
        
        then:
        result == climbs // Returns original list
    }
    
    def "should handle empty lists"() {
        when:
        def filtered = queryService.applyFilter([], "type=boulder")
        def sorted = queryService.sort([], "rating")
        def stats = queryService.calculateStatistics([], "climbs")
        
        then:
        filtered.isEmpty()
        sorted.isEmpty()
        stats.total == 0
    }
    
    private Climb createClimb(String name, Climb.ClimbType type, String grade, Integer rating = null, Boolean isIndoor = true) {
        Climb.builder()
            .routeName(name)
            .type(type)
            .grades(Climb.GradeInfo.builder()
                .system(grade.startsWith("V") ? Climb.GradeInfo.GradeSystem.vScale : Climb.GradeInfo.GradeSystem.yds)
                .grade(grade)
                .build())
            .rating(rating)
            .isIndoor(isIndoor)
            .finishType(rating != null && rating >= 4 ? "flash" : "redpoint")
            .build()
    }
}