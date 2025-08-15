package app.crushlog.cldf.tool.utils

import spock.lang.Specification
import spock.lang.Unroll

class CLIDUtilsSpec extends Specification {

    def "should validate v1 CLIDs correctly"() {
        expect:
        CLIDUtils.isValidV1CLID(clid) == isValid

        where:
        clid                           | isValid
        "clid:v1:route:abc-123"       | true
        "clid:v1:location:xyz-456"    | true
        "clid:v1:sector:def-789"      | true
        "clid:route:abc-123"          | false  // legacy format not supported
        "clid:location:xyz-456"       | false  // legacy format not supported
        "clid:v2:route:abc-123"       | false  // wrong version
        "clid:v1"                     | false  // missing parts
        "clid:v1:route"               | false  // missing uuid
        "clid:v1::abc-123"            | false  // empty type
        "clid:v1:route:"              | false  // empty uuid
        "not-a-clid"                  | false
        null                          | false
        ""                            | false
    }

    @Unroll
    def "should extract entity type from valid v1 CLID: #clid -> #expectedType"() {
        expect:
        CLIDUtils.extractEntityType(clid) == Optional.of(expectedType)

        where:
        clid                           | expectedType
        "clid:v1:route:abc-123"       | "route"
        "clid:v1:location:xyz-456"    | "location"
        "clid:v1:sector:def-789"      | "sector"
        "clid:v1:climb:ghi-012"       | "climb"
    }

    def "should return empty Optional for invalid CLIDs when extracting entity type"() {
        expect:
        CLIDUtils.extractEntityType(clid) == Optional.empty()

        where:
        clid << [
            "clid:route:abc-123",     // legacy format
            "clid:v2:route:abc-123",   // wrong version
            "clid:v1",                 // incomplete
            "not-a-clid",
            null,
            ""
        ]
    }

    @Unroll
    def "should extract UUID from valid v1 CLID: #clid -> #expectedUuid"() {
        expect:
        CLIDUtils.extractUuid(clid) == Optional.of(expectedUuid)

        where:
        clid                                       | expectedUuid
        "clid:v1:route:abc-123"                   | "abc-123"
        "clid:v1:location:xyz-456"                | "xyz-456"
        "clid:v1:route:550e8400-e29b-41d4-a716"   | "550e8400-e29b-41d4-a716"
        "clid:v1:climb:test-uuid-123"             | "test-uuid-123"
    }

    def "should return empty Optional for invalid CLIDs when extracting UUID"() {
        expect:
        CLIDUtils.extractUuid(clid) == Optional.empty()

        where:
        clid << [
            "clid:route:abc-123",     // legacy format
            "clid:v2:route:abc-123",   // wrong version
            "clid:v1",                 // incomplete
            "not-a-clid",
            null,
            ""
        ]
    }

    @Unroll
    def "should convert valid v1 CLID to custom URI: #clid -> #expectedUri"() {
        expect:
        CLIDUtils.toCustomUri(clid) == Optional.of(expectedUri)

        where:
        clid                           | expectedUri
        "clid:v1:route:abc-123"       | "cldf://route/abc-123"
        "clid:v1:location:xyz-456"    | "cldf://location/xyz-456"
        "clid:v1:sector:def-789"      | "cldf://sector/def-789"
        "clid:v1:climb:ghi-012"       | "cldf://climb/ghi-012"
    }

    def "should return empty Optional for invalid CLIDs when converting to custom URI"() {
        expect:
        CLIDUtils.toCustomUri(clid) == Optional.empty()

        where:
        clid << [
            "clid:route:abc-123",     // legacy format
            "clid:v2:route:abc-123",   // wrong version
            "clid:v1",                 // incomplete
            "not-a-clid",
            null,
            ""
        ]
    }

    def "should handle CLIDs with complex UUIDs"() {
        given:
        def complexClid = "clid:v1:route:550e8400-e29b-41d4-a716-446655440000"

        expect:
        CLIDUtils.isValidV1CLID(complexClid) == true
        CLIDUtils.extractEntityType(complexClid) == Optional.of("route")
        CLIDUtils.extractUuid(complexClid) == Optional.of("550e8400-e29b-41d4-a716-446655440000")
        CLIDUtils.toCustomUri(complexClid) == Optional.of("cldf://route/550e8400-e29b-41d4-a716-446655440000")
    }

    def "should handle CLIDs with special characters in UUID"() {
        given:
        def specialClid = "clid:v1:location:test_uuid-123.456"

        expect:
        CLIDUtils.isValidV1CLID(specialClid) == true
        CLIDUtils.extractEntityType(specialClid) == Optional.of("location")
        CLIDUtils.extractUuid(specialClid) == Optional.of("test_uuid-123.456")
        CLIDUtils.toCustomUri(specialClid) == Optional.of("cldf://location/test_uuid-123.456")
    }

    def "should reject CLIDs with colons in UUID"() {
        given:
        def invalidClid = "clid:v1:route:uuid:with:colons"

        expect:
        CLIDUtils.isValidV1CLID(invalidClid) == false
        CLIDUtils.extractEntityType(invalidClid) == Optional.empty()
        CLIDUtils.extractUuid(invalidClid) == Optional.empty()
        CLIDUtils.toCustomUri(invalidClid) == Optional.empty()
    }
}