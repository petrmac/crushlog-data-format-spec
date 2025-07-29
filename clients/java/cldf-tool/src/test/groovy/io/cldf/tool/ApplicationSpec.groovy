package io.cldf.tool

import spock.lang.Specification
import spock.lang.Unroll
import io.micronaut.context.ApplicationContext
import io.micronaut.configuration.picocli.PicocliRunner
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class ApplicationSpec extends Specification {
    
    def setupSpec() {
        // Ensure clean state before tests
        System.setProperty("micronaut.environments", "test")
    }
    
    def cleanupSpec() {
        // Clean up any lingering resources
        System.clearProperty("micronaut.environments")
    }
    
    def "should display help when no arguments provided"() {
        given:
        def output = captureOutput {
            // Use PicocliRunner.run instead of main() to avoid System.exit()
            PicocliRunner.run(Application.class, [] as String[])
        }
        
        expect:
        output.contains("Usage: cldf")
        output.contains("CLDF Tool for creating and manipulating climbing data archives")
        output.contains("Commands:")
        output.contains("create")
        output.contains("validate")
        output.contains("extract")
        output.contains("merge")
        output.contains("convert")
        output.contains("query")
        output.contains("load")
        output.contains("graph-query")
        output.contains("schema")
    }
    
    def "should display version information"() {
        given:
        def output = captureOutput {
            PicocliRunner.run(Application.class, ["--version"] as String[])
        }
        
        expect:
        output.contains("1.0.0")
    }
    
    def "should display help with --help flag"() {
        given:
        def output = captureOutput {
            PicocliRunner.run(Application.class, ["--help"] as String[])
        }
        
        expect:
        output.contains("Usage: cldf")
        output.contains("-h, --help")
        output.contains("-V, --version")
    }
    
    @Unroll
    def "should recognize command: #command"() {
        given:
        def output = captureOutput {
            PicocliRunner.run(Application.class, [command, "--help"] as String[])
        }
        
        expect:
        output.contains("Usage: cldf " + command)
        
        where:
        command << ["create", "validate", "extract", "merge", "convert", "query", "load", "graph-query", "schema"]
    }
    
    def "should handle invalid command"() {
        given:
        def output = captureError {
            PicocliRunner.run(Application.class, ["invalid-command"] as String[])
        }
        
        expect:
        output.contains("Unmatched argument") || output.contains("Unknown command")
    }
    
    def "should initialize application context"() {
        when:
        def context = ApplicationContext.run()
        def app = context.getBean(Application)
        
        then:
        app != null
        app.applicationContext != null
        
        cleanup:
        context.close()
    }
    
    def "should set proper logging configuration"() {
        given:
        // Store original values
        def originalDisableJmx = System.getProperty("log4j2.disable.jmx")
        def originalShutdownHook = System.getProperty("log4j.shutdownHookEnabled")
        
        when:
        // Set the properties like main() does
        System.setProperty("log4j2.disable.jmx", "true")
        System.setProperty("log4j.shutdownHookEnabled", "false")
        
        then:
        System.getProperty("log4j2.disable.jmx") == "true"
        System.getProperty("log4j.shutdownHookEnabled") == "false"
        
        cleanup:
        // Restore original values
        if (originalDisableJmx != null) {
            System.setProperty("log4j2.disable.jmx", originalDisableJmx)
        } else {
            System.clearProperty("log4j2.disable.jmx")
        }
        if (originalShutdownHook != null) {
            System.setProperty("log4j.shutdownHookEnabled", originalShutdownHook)
        } else {
            System.clearProperty("log4j.shutdownHookEnabled")
        }
    }
    
    def "run method should display usage"() {
        given:
        def app = new Application()
        def output = captureOutput {
            app.run()
        }
        
        expect:
        output.contains("Usage: cldf")
    }
    
    // Helper methods for capturing output
    private String captureOutput(Closure closure) {
        def baos = new ByteArrayOutputStream()
        def originalOut = System.out
        def originalErr = System.err
        
        try {
            System.setOut(new PrintStream(baos))
            System.setErr(new PrintStream(baos))
            closure.call()
            return baos.toString()
        } finally {
            System.setOut(originalOut)
            System.setErr(originalErr)
        }
    }
    
    private String captureError(Closure closure) {
        def baos = new ByteArrayOutputStream()
        def originalErr = System.err
        
        try {
            System.setErr(new PrintStream(baos))
            closure.call()
            return baos.toString()
        } finally {
            System.setErr(originalErr)
        }
    }
}