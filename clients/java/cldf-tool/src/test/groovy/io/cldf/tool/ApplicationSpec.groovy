package io.cldf.tool

import spock.lang.Specification
import spock.lang.Unroll
import io.micronaut.context.ApplicationContext
import io.micronaut.configuration.picocli.PicocliRunner
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class ApplicationSpec extends Specification {
    
    def "should display help when no arguments provided"() {
        given:
        def baos = new ByteArrayOutputStream()
        def originalOut = System.out
        System.setOut(new PrintStream(baos))
        
        when:
        Application.main([] as String[])
        
        then:
        def output = baos.toString()
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
        
        cleanup:
        System.setOut(originalOut)
    }
    
    def "should display version information"() {
        given:
        def baos = new ByteArrayOutputStream()
        def originalOut = System.out
        System.setOut(new PrintStream(baos))
        
        when:
        Application.main(["--version"] as String[])
        
        then:
        def output = baos.toString()
        output.contains("1.0.0")
        
        cleanup:
        System.setOut(originalOut)
    }
    
    def "should display help with --help flag"() {
        given:
        def baos = new ByteArrayOutputStream()
        def originalOut = System.out
        System.setOut(new PrintStream(baos))
        
        when:
        Application.main(["--help"] as String[])
        
        then:
        def output = baos.toString()
        output.contains("Usage: cldf")
        output.contains("-h, --help")
        output.contains("-V, --version")
        
        cleanup:
        System.setOut(originalOut)
    }
    
    @Unroll
    def "should recognize command: #command"() {
        given:
        def baos = new ByteArrayOutputStream()
        def originalOut = System.out
        System.setOut(new PrintStream(baos))
        
        when:
        Application.main([command, "--help"] as String[])
        
        then:
        def output = baos.toString()
        output.contains("Usage: cldf " + command)
        
        cleanup:
        System.setOut(originalOut)
        
        where:
        command << ["create", "validate", "extract", "merge", "convert", "query", "load", "graph-query"]
    }
    
    def "should handle invalid command"() {
        given:
        def baos = new ByteArrayOutputStream()
        def originalErr = System.err
        System.setErr(new PrintStream(baos))
        
        when:
        Application.main(["invalid-command"] as String[])
        
        then:
        def output = baos.toString()
        output.contains("Unmatched argument") || output.contains("Unknown command")
        
        cleanup:
        System.setErr(originalErr)
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
    
    def "should have proper logging configuration"() {
        expect:
        System.getProperty("log4j2.disable.jmx") == "true"
        System.getProperty("log4j.shutdownHookEnabled") == "false"
    }
    
    def "run method should display usage"() {
        given:
        def app = new Application()
        def baos = new ByteArrayOutputStream()
        def originalOut = System.out
        System.setOut(new PrintStream(baos))
        
        when:
        app.run()
        
        then:
        def output = baos.toString()
        output.contains("Usage: cldf")
        
        cleanup:
        System.setOut(originalOut)
    }
}