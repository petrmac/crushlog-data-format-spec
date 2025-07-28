package io.cldf.tool.utils

import spock.lang.Specification
import spock.lang.Subject
import spock.lang.TempDir

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class InputHandlerSpec extends Specification {
    
    @TempDir
    Path tempDir
    
    @Subject
    InputHandler handler = new InputHandler()
    
    def "should read JSON from input stream"() {
        given:
        def json = '{"name": "test", "value": 42}'
        def inputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))
        
        when:
        def result = handler.readJson(inputStream, Map.class)
        
        then:
        result.name == "test"
        result.value == 42
    }
    
    def "should read JSON from file"() {
        given:
        def json = '{"locations": [{"name": "Gym A"}], "climbs": []}'
        def file = tempDir.resolve("test.json").toFile()
        file.text = json
        
        when:
        def result = handler.readJsonFromFile(file, Map.class)
        
        then:
        result.locations.size() == 1
        result.locations[0].name == "Gym A"
        result.climbs.isEmpty()
    }
    
    def "should read JSON from file when filename provided"() {
        given:
        def json = '{"test": true}'
        def file = tempDir.resolve("input.json")
        Files.write(file, json.getBytes(StandardCharsets.UTF_8))
        
        when:
        def result = handler.readJson(file.toString(), Map.class)
        
        then:
        result.test == true
    }
    
    def "should read text from input stream"() {
        given:
        def text = """Line 1
Line 2
Line 3"""
        def inputStream = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8))
        
        when:
        def result = handler.readText(inputStream)
        
        then:
        result == text
    }
    
    def "should handle empty input stream"() {
        given:
        def inputStream = new ByteArrayInputStream(new byte[0])
        
        when:
        def result = handler.readText(inputStream)
        
        then:
        result == ""
    }
    
    def "should throw IOException for invalid JSON"() {
        given:
        def invalidJson = '{"invalid": json}'
        def inputStream = new ByteArrayInputStream(invalidJson.getBytes(StandardCharsets.UTF_8))
        
        when:
        handler.readJson(inputStream, Map.class)
        
        then:
        thrown(IOException)
    }
    
    def "should throw FileNotFoundException for non-existent file"() {
        given:
        def nonExistentFile = new File("/non/existent/file.json")
        
        when:
        handler.readJsonFromFile(nonExistentFile, Map.class)
        
        then:
        thrown(FileNotFoundException)
    }
    
    def "should handle complex JSON structures"() {
        given:
        def complexJson = '''
        {
            "manifest": {
                "version": "1.0.0",
                "format": "CLDF"
            },
            "locations": [
                {
                    "id": 1,
                    "name": "Test Location",
                    "isIndoor": true,
                    "coordinates": {
                        "latitude": 40.0,
                        "longitude": -105.0
                    }
                }
            ],
            "climbs": [
                {
                    "id": 1,
                    "routeName": "Test Route",
                    "grades": {
                        "system": "vScale",
                        "grade": "V5"
                    }
                }
            ]
        }
        '''
        def inputStream = new ByteArrayInputStream(complexJson.getBytes(StandardCharsets.UTF_8))
        
        when:
        def result = handler.readJson(inputStream, Map.class)
        
        then:
        result.manifest.version == "1.0.0"
        result.locations[0].coordinates.latitude == 40.0
        result.climbs[0].grades.grade == "V5"
    }
    
    def "should read from stdin when filename is dash"() {
        given:
        def originalStdin = System.in
        def json = '{"stdin": true}'
        System.in = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))
        
        when:
        def result = handler.readJson("-", Map.class)
        
        then:
        result.stdin == true
        
        cleanup:
        System.in = originalStdin
    }
}