package io.cldf.tool.models

import spock.lang.Specification

class CommandResultSpec extends Specification {

    def "should create successful result with builder"() {
        when:
        def result = CommandResult.builder()
            .success(true)
            .message("Operation completed")
            .data(["key": "value"])
            .exitCode(0)
            .build()

        then:
        result.success == true
        result.message == "Operation completed"
        result.data == ["key": "value"]
        result.exitCode == 0
        result.warnings == null
    }

    def "should create failed result with warnings"() {
        when:
        def result = CommandResult.builder()
            .success(false)
            .message("Operation failed")
            .warnings(["Warning 1", "Warning 2"])
            .exitCode(1)
            .build()

        then:
        result.success == false
        result.message == "Operation failed"
        result.warnings == ["Warning 1", "Warning 2"]
        result.exitCode == 1
        result.data == null
    }

    def "should handle null values"() {
        when:
        def result = CommandResult.builder()
            .success(true)
            .build()

        then:
        result.success == true
        result.message == null
        result.data == null
        result.warnings == null
        result.metadata == null
        result.exitCode == 0  // has default value
    }

    def "should test toString method"() {
        given:
        def result = CommandResult.builder()
            .success(true)
            .message("Test message")
            .data(["count": 42])
            .warnings(["Minor issue"])
            .exitCode(0)
            .build()

        when:
        def string = result.toString()

        then:
        string.contains("CommandResult")
        string.contains("success=true")
        string.contains("message=Test message")
        string.contains("data={count=42}")
        string.contains("warnings=[Minor issue]")
        string.contains("exitCode=0")
    }

    def "should test equals and hashCode"() {
        given:
        def result1 = CommandResult.builder()
            .success(true)
            .message("Test")
            .exitCode(0)
            .build()
            
        def result2 = CommandResult.builder()
            .success(true)
            .message("Test")
            .exitCode(0)
            .build()
            
        def result3 = CommandResult.builder()
            .success(false)
            .message("Test")
            .exitCode(1)
            .build()

        expect:
        result1 == result2
        result1.hashCode() == result2.hashCode()
        result1 != result3
        result1.hashCode() != result3.hashCode()
    }

    def "should test getters"() {
        given:
        def testData = ["key": "value", "number": 123]
        def testWarnings = ["Warning 1", "Warning 2"]
        
        def result = CommandResult.builder()
            .success(true)
            .message("Success message")
            .data(testData)
            .warnings(testWarnings)
            .exitCode(0)
            .build()

        expect:
        result.isSuccess() == true
        result.getMessage() == "Success message"
        result.getData() == testData
        result.getWarnings() == testWarnings
        result.getExitCode() == 0
    }

    def "should create result with complex data"() {
        given:
        def complexData = [
            "stats": [
                "total": 100,
                "processed": 95,
                "failed": 5
            ],
            "errors": ["Error 1", "Error 2"],
            "metadata": [
                "timestamp": "2024-01-01T00:00:00Z",
                "version": "1.0.0"
            ]
        ]

        when:
        def result = CommandResult.builder()
            .success(false)
            .message("Partial failure")
            .data(complexData)
            .exitCode(2)
            .build()

        then:
        result.data["stats"]["total"] == 100
        result.data["stats"]["processed"] == 95
        result.data["stats"]["failed"] == 5
        result.data["errors"] == ["Error 1", "Error 2"]
        result.data["metadata"]["version"] == "1.0.0"
    }

    def "should test builder with all fields"() {
        when:
        def builder = CommandResult.builder()
        builder.success(true)
        builder.message("Test message")
        builder.data(["test": "data"])
        builder.warnings(["warning"])
        builder.exitCode(0)
        
        def result = builder.build()

        then:
        result != null
        result.success == true
        result.message == "Test message"
        result.data == ["test": "data"]
        result.warnings == ["warning"]
        result.exitCode == 0
    }

    def "should test builder toString"() {
        given:
        def builder = CommandResult.builder()
            .success(true)
            .message("Test")

        when:
        def string = builder.toString()

        then:
        string != null
        string.contains("CommandResultBuilder")
    }

    def "should handle empty collections"() {
        when:
        def result = CommandResult.builder()
            .success(true)
            .data([:])
            .warnings([])
            .build()

        then:
        result.data == [:]
        result.warnings == []
    }

    def "should test static success methods"() {
        when:
        def result1 = CommandResult.success()
        
        then:
        result1.success == true
        result1.message == null
        result1.data == null
        result1.exitCode == 0
        
        when:
        def result2 = CommandResult.success("Operation completed")
        
        then:
        result2.success == true
        result2.message == "Operation completed"
        result2.data == null
        result2.exitCode == 0
        
        when:
        def testData = ["count": 42]
        def result3 = CommandResult.success("Operation completed", testData)
        
        then:
        result3.success == true
        result3.message == "Operation completed"
        result3.data == testData
        result3.exitCode == 0
    }

    def "should test static failure method"() {
        when:
        def result = CommandResult.failure("Operation failed")
        
        then:
        result.success == false
        result.message == "Operation failed"
        result.data == null
        result.exitCode == 1
    }

    def "should test metadata field"() {
        given:
        def metadata = [
            "timestamp": "2024-01-01T00:00:00Z",
            "version": "1.0.0",
            "user": "testuser"
        ]
        
        when:
        def result = CommandResult.builder()
            .success(true)
            .message("Success")
            .metadata(metadata)
            .build()
        
        then:
        result.metadata == metadata
        result.metadata["version"] == "1.0.0"
    }

    def "should test no-args constructor"() {
        when:
        def result = new CommandResult()
        result.success = true
        result.message = "Test"
        result.exitCode = 0
        
        then:
        result.success == true
        result.message == "Test"
        result.exitCode == 0
    }
}