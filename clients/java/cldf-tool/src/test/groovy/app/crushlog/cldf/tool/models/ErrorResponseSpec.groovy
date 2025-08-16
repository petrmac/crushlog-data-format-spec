package app.crushlog.cldf.tool.models

import spock.lang.Specification

class ErrorResponseSpec extends Specification {

    def "should create error response with builder"() {
        given:
        def error = ErrorResponse.Error.builder()
            .code("500")
            .message("Internal server error")
            .type("SERVER_ERROR")
            .build()
            
        when:
        def response = ErrorResponse.builder()
            .success(false)
            .error(error)
            .build()

        then:
        response.success == false
        response.error != null
        response.error.code == "500"
        response.error.message == "Internal server error"
        response.error.type == "SERVER_ERROR"
        response.warnings == null
    }

    def "should create error response with warnings"() {
        given:            
        def warning1 = ErrorResponse.Warning.builder()
            .code("DEPRECATED")
            .message("This API is deprecated")
            .field("apiVersion")
            .build()
            
        def warning2 = ErrorResponse.Warning.builder()
            .code("RATE_LIMIT")
            .message("Approaching rate limit")
            .build()

        when:
        def response = ErrorResponse.builder()
            .success(true)
            .warnings([warning1, warning2])
            .build()

        then:
        response.success == true
        response.error == null
        response.warnings.size() == 2
        response.warnings[0].code == "DEPRECATED"
        response.warnings[0].field == "apiVersion"
        response.warnings[1].code == "RATE_LIMIT"
        response.warnings[1].field == null
    }

    def "should test Error class with all fields"() {
        given:
        def details = [
            "stackTrace": "at line 42",
            "timestamp": "2024-01-01T00:00:00Z"
        ]
        
        when:
        def error = ErrorResponse.Error.builder()
            .code("VALIDATION_ERROR")
            .message("Invalid input")
            .type("VALIDATION")
            .details(details)
            .suggestion("Please check your input")
            .build()

        then:
        error.code == "VALIDATION_ERROR"
        error.message == "Invalid input"
        error.type == "VALIDATION"
        error.details == details
        error.suggestion == "Please check your input"
        
        when:
        def errorString = error.toString()
        
        then:
        errorString.contains("Error")
        errorString.contains("code=VALIDATION_ERROR")
        errorString.contains("message=Invalid input")
        errorString.contains("type=VALIDATION")
        errorString.contains("suggestion=Please check your input")
    }

    def "should test Warning class"() {
        when:
        def warning = ErrorResponse.Warning.builder()
            .code("RATE_LIMIT")
            .message("Approaching rate limit")
            .field("requests")
            .build()

        then:
        warning.code == "RATE_LIMIT"
        warning.message == "Approaching rate limit"
        warning.field == "requests"
        
        when:
        def warningString = warning.toString()
        
        then:
        warningString.contains("Warning")
        warningString.contains("code=RATE_LIMIT")
        warningString.contains("message=Approaching rate limit")
        warningString.contains("field=requests")
    }

    def "should test equals and hashCode for Error"() {
        given:
        def error1 = ErrorResponse.Error.builder()
            .code("ERROR_CODE")
            .message("Error message")
            .type("ERROR_TYPE")
            .build()
            
        def error2 = ErrorResponse.Error.builder()
            .code("ERROR_CODE")
            .message("Error message")
            .type("ERROR_TYPE")
            .build()
            
        def error3 = ErrorResponse.Error.builder()
            .code("DIFFERENT_CODE")
            .message("Error message")
            .type("ERROR_TYPE")
            .build()

        expect:
        error1 == error2
        error1.hashCode() == error2.hashCode()
        error1 != error3
        error1.hashCode() != error3.hashCode()
    }

    def "should test equals and hashCode for Warning"() {
        given:
        def warning1 = ErrorResponse.Warning.builder()
            .code("WARN_CODE")
            .message("Warning message")
            .field("field1")
            .build()
            
        def warning2 = ErrorResponse.Warning.builder()
            .code("WARN_CODE")
            .message("Warning message")
            .field("field1")
            .build()
            
        def warning3 = ErrorResponse.Warning.builder()
            .code("DIFFERENT_CODE")
            .message("Warning message")
            .field("field1")
            .build()

        expect:
        warning1 == warning2
        warning1.hashCode() == warning2.hashCode()
        warning1 != warning3
        warning1.hashCode() != warning3.hashCode()
    }

    def "should test equals and hashCode for ErrorResponse"() {
        given:
        def error = ErrorResponse.Error.builder()
            .code("ERR")
            .message("Error")
            .build()
            
        def response1 = ErrorResponse.builder()
            .success(false)
            .error(error)
            .build()
            
        def response2 = ErrorResponse.builder()
            .success(false)
            .error(error)
            .build()
            
        def response3 = ErrorResponse.builder()
            .success(true)
            .build()

        expect:
        response1 == response2
        response1.hashCode() == response2.hashCode()
        response1 != response3
        response1.hashCode() != response3.hashCode()
    }

    def "should test toString for ErrorResponse"() {
        given:
        def error = ErrorResponse.Error.builder()
            .code("CODE1")
            .message("Message1")
            .type("TYPE1")
            .build()
            
        def warning = ErrorResponse.Warning.builder()
            .code("WARN1")
            .message("Warning1")
            .build()
            
        def response = ErrorResponse.builder()
            .success(false)
            .error(error)
            .warnings([warning])
            .build()

        when:
        def string = response.toString()

        then:
        string.contains("ErrorResponse")
        string.contains("success=false")
        string.contains("error=")
        string.contains("warnings=")
    }

    def "should test getters"() {
        given:
        def error = ErrorResponse.Error.builder()
            .code("E1")
            .message("Error 1")
            .build()
            
        def warnings = [
            ErrorResponse.Warning.builder().code("W1").message("WM1").build()
        ]
        
        def response = ErrorResponse.builder()
            .success(false)
            .error(error)
            .warnings(warnings)
            .build()

        expect:
        response.isSuccess() == false
        response.getError() == error
        response.getWarnings() == warnings
    }

    def "should test Error getters"() {
        given:
        def details = ["key": "value"]
        def error = ErrorResponse.Error.builder()
            .code("TEST_CODE")
            .message("Test message")
            .type("TEST_TYPE")
            .details(details)
            .suggestion("Test suggestion")
            .build()

        expect:
        error.getCode() == "TEST_CODE"
        error.getMessage() == "Test message"
        error.getType() == "TEST_TYPE"
        error.getDetails() == details
        error.getSuggestion() == "Test suggestion"
    }

    def "should test Warning getters"() {
        given:
        def warning = ErrorResponse.Warning.builder()
            .code("WARN_CODE")
            .message("Warning message")
            .field("fieldName")
            .build()

        expect:
        warning.getCode() == "WARN_CODE"
        warning.getMessage() == "Warning message"
        warning.getField() == "fieldName"
    }

    def "should handle null values"() {
        when:
        def response = ErrorResponse.builder().build()

        then:
        response.success == false
        response.error == null
        response.warnings == null
    }

    def "should test builder toString methods"() {
        given:
        def errorBuilder = ErrorResponse.Error.builder()
            .code("CODE")
            
        def warningBuilder = ErrorResponse.Warning.builder()
            .code("WARN")
            
        def responseBuilder = ErrorResponse.builder()
            .success(true)

        expect:
        errorBuilder.toString().contains("ErrorBuilder")
        warningBuilder.toString().contains("WarningBuilder")
        responseBuilder.toString().contains("ErrorResponseBuilder")
    }

    def "should create complex error response"() {
        given:
        def error = ErrorResponse.Error.builder()
            .code("MULTI_ERROR")
            .message("Multiple validation errors")
            .type("VALIDATION")
            .details([
                "errors": ["Field 1 invalid", "Field 2 missing"],
                "timestamp": "2024-01-01T00:00:00Z"
            ])
            .suggestion("Please fix all validation errors")
            .build()
            
        def warnings = (1..2).collect { i ->
            ErrorResponse.Warning.builder()
                .code("WARN_${i}")
                .message("Warning message ${i}")
                .field("field${i}")
                .build()
        }

        when:
        def response = ErrorResponse.builder()
            .success(false)
            .error(error)
            .warnings(warnings)
            .build()

        then:
        response.success == false
        response.error.code == "MULTI_ERROR"
        response.error.details["errors"].size() == 2
        response.warnings.size() == 2
        response.warnings[0].field == "field1"
        response.warnings[1].code == "WARN_2"
    }

    def "should test no-args constructor"() {
        when:
        def error = new ErrorResponse.Error()
        error.code = "TEST"
        error.message = "Test message"
        
        def warning = new ErrorResponse.Warning()
        warning.code = "WARN"
        
        def response = new ErrorResponse()
        response.success = true
        response.error = error
        response.warnings = [warning]

        then:
        response.success == true
        response.error.code == "TEST"
        response.warnings[0].code == "WARN"
    }
}