package io.cldf.tool.services

import spock.lang.Specification
import spock.lang.TempDir
import io.cldf.api.CLDFArchive
import io.cldf.models.*
import org.neo4j.graphdb.*
import org.neo4j.graphdb.schema.IndexCreator
import org.neo4j.graphdb.schema.IndexDefinition
import org.neo4j.graphdb.schema.Schema
import org.neo4j.dbms.api.DatabaseManagementService
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder
import java.nio.file.Path
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

import io.cldf.models.enums.FinishType

class GraphServiceSpec extends Specification {

    @TempDir
    Path tempDir

    GraphService graphService
    DatabaseManagementService mockManagementService
    GraphDatabaseService mockGraphDb
    Transaction mockTransaction

    def setup() {
        graphService = new GraphService()
        mockManagementService = Mock(DatabaseManagementService)
        mockGraphDb = Mock(GraphDatabaseService)
        mockTransaction = Mock(Transaction)
    }

    def "should initialize embedded Neo4j database"() {
        when: "initializing the graph service"
        graphService.initialize()

        then: "database is initialized"
        graphService.graphDb != null
        graphService.managementService != null
        graphService.tempDbPath != null
    }

    def "should not reinitialize if already initialized"() {
        given: "an already initialized service"
        graphService.graphDb = mockGraphDb

        when: "initializing again"
        graphService.initialize()

        then: "no new initialization occurs"
        0 * _
    }

    def "should import CLDF archive into Neo4j graph"() {
        given: "a CLDF archive with data"
        def archive = createSampleArchive()
        graphService.graphDb = mockGraphDb
        
        def archiveNode = Mock(Node)
        def locationNode = Mock(Node)
        def sessionNode = Mock(Node)
        def climbNode = Mock(Node)
        def tagNode = Mock(Node)
        
        mockGraphDb.beginTx() >> mockTransaction
        mockTransaction.createNode(GraphService.NodeLabel.Archive) >> archiveNode
        mockTransaction.createNode(GraphService.NodeLabel.Location) >> locationNode
        mockTransaction.createNode(GraphService.NodeLabel.Session) >> sessionNode
        mockTransaction.createNode(GraphService.NodeLabel.Climb) >> climbNode
        mockTransaction.createNode(GraphService.NodeLabel.Tag) >> tagNode
        mockTransaction.findNode(GraphService.NodeLabel.Tag, "name", "crimpy") >> null

        when: "importing the archive"
        graphService.importArchive(archive)

        then: "data is imported correctly"
        1 * mockTransaction.execute("MATCH (n) DETACH DELETE n")
        1 * archiveNode.setProperty("format", "CLDF")
        1 * archiveNode.setProperty("version", "1.0.0")
        1 * archiveNode.setProperty("createdAt", _ as String)
        
        1 * locationNode.setProperty("locationId", 1)
        1 * locationNode.setProperty("name", "Test Crag")
        1 * locationNode.setProperty("country", "USA")
        1 * locationNode.setProperty("state", "CA")
        1 * locationNode.setProperty("isIndoor", false)
        1 * locationNode.setProperty("latitude", 37.7749)
        1 * locationNode.setProperty("longitude", -122.4194)
        
        1 * sessionNode.setProperty("sessionId", "session1")
        1 * sessionNode.setProperty("date", "2024-01-01")
        1 * sessionNode.setProperty("locationName", "Test Crag")
        
        1 * climbNode.setProperty("climbId", 1)
        1 * climbNode.setProperty("date", "2024-01-01")
        1 * climbNode.setProperty("routeName", "Test Route")
        1 * climbNode.setProperty("grade", "5.10a")
        1 * climbNode.setProperty("finishType", "top")
        1 * climbNode.setProperty("attempts", 2)
        1 * climbNode.setProperty("rating", 4)
        
        1 * tagNode.setProperty("name", "crimpy")
        
        1 * archiveNode.createRelationshipTo(locationNode, GraphService.RelType.HAS_LOCATION)
        1 * archiveNode.createRelationshipTo(sessionNode, GraphService.RelType.HAS_SESSION)
        1 * archiveNode.createRelationshipTo(climbNode, GraphService.RelType.HAS_CLIMB)
        1 * sessionNode.createRelationshipTo(locationNode, GraphService.RelType.AT_LOCATION)
        1 * sessionNode.createRelationshipTo(climbNode, GraphService.RelType.INCLUDES_CLIMB)
        1 * climbNode.createRelationshipTo(tagNode, GraphService.RelType.TAGGED_WITH)
        
        1 * mockTransaction.commit()
    }

    def "should handle import errors gracefully"() {
        given: "a CLDF archive and a failing transaction"
        def archive = createSampleArchive()
        graphService.graphDb = mockGraphDb
        
        mockGraphDb.beginTx() >> mockTransaction
        mockTransaction.execute(_ as String) >> { throw new RuntimeException("DB error") }

        when: "importing the archive"
        graphService.importArchive(archive)

        then: "exception is thrown"
        thrown(RuntimeException)
    }

    def "should execute Cypher query and return results"() {
        given: "a graph database with query"
        graphService.graphDb = mockGraphDb
        def query = "MATCH (n:Climb) RETURN n.routeName as name"
        def parameters = [:]
        
        def result = Mock(Result)
        result.hasNext() >>> [true, true, false]
        result.next() >>> [["name": "Route 1"], ["name": "Route 2"]]
        
        mockGraphDb.beginTx() >> mockTransaction
        mockTransaction.execute(query, parameters) >> result

        when: "executing the query"
        def results = graphService.executeCypher(query, parameters)

        then: "results are returned"
        results.size() == 2
        results[0]["name"] == "Route 1"
        results[1]["name"] == "Route 2"
        1 * mockTransaction.commit()
    }

    def "should export graph back to CLDF archive"() {
        given: "a graph database with nodes"
        graphService.graphDb = mockGraphDb
        
        def archiveNode = Mock(Node)
        def locationNode = Mock(Node)
        def sessionNode = Mock(Node)
        def climbNode = Mock(Node)
        
        mockGraphDb.beginTx() >> mockTransaction
        mockTransaction.findNode(GraphService.NodeLabel.Archive, "format", "CLDF") >> archiveNode
        
        archiveNode.getProperty("format") >> "CLDF"
        archiveNode.getProperty("version") >> "1.0.0"
        
        locationNode.getProperty("locationId") >> 1
        locationNode.getProperty("name") >> "Test Crag"
        locationNode.getProperty("country", null) >> "USA"
        locationNode.getProperty("state", null) >> "CA"
        locationNode.getProperty("isIndoor", false) >> false
        
        sessionNode.getProperty("sessionId") >> "session1"
        sessionNode.getProperty("date") >> "2024-01-01"
        sessionNode.getProperty("locationName", null) >> "Test Crag"
        
        climbNode.getProperty("climbId") >> 1
        climbNode.getProperty("date") >> "2024-01-01"
        climbNode.getProperty("routeName", null) >> "Test Route"
        climbNode.getProperty("grade", null) >> "5.10a"
        climbNode.getProperty("finishType", null) >> "top"
        climbNode.getProperty("attempts", 1) >> 2
        climbNode.getProperty("rating", null) >> 4
        
        def locationIterator = [locationNode].iterator()
        def sessionIterator = [sessionNode].iterator()
        def climbIterator = [climbNode].iterator()
        
        mockTransaction.findNodes(GraphService.NodeLabel.Location) >> { 
            new ResourceIterator<Node>() {
                def iter = locationIterator
                boolean hasNext() { iter.hasNext() }
                Node next() { iter.next() }
                void close() {}
            }
        }
        mockTransaction.findNodes(GraphService.NodeLabel.Session) >> {
            new ResourceIterator<Node>() {
                def iter = sessionIterator
                boolean hasNext() { iter.hasNext() }
                Node next() { iter.next() }
                void close() {}
            }
        }
        mockTransaction.findNodes(GraphService.NodeLabel.Climb) >> {
            new ResourceIterator<Node>() {
                def iter = climbIterator
                boolean hasNext() { iter.hasNext() }
                Node next() { iter.next() }
                void close() {}
            }
        }

        when: "exporting to archive"
        def exportedArchive = graphService.exportToArchive()

        then: "archive is exported correctly"
        exportedArchive.manifest.format == "CLDF"
        exportedArchive.manifest.version == "1.0.0"
        exportedArchive.locations.size() == 1
        exportedArchive.locations[0].id == 1
        exportedArchive.locations[0].name == "Test Crag"
        exportedArchive.sessions.size() == 1
        exportedArchive.sessions[0].id == "session1"
        exportedArchive.climbs.size() == 1
        exportedArchive.climbs[0].id == 1
        exportedArchive.climbs[0].routeName == "Test Route"
        1 * mockTransaction.commit()
    }

    def "should shutdown database properly"() {
        given: "an initialized graph service"
        graphService.managementService = mockManagementService
        graphService.tempDbPath = tempDir

        when: "shutting down"
        graphService.shutdown()

        then: "management service is shut down"
        1 * mockManagementService.shutdown()
    }

    def "should handle null locations during import"() {
        given: "an archive with null locations"
        def archive = CLDFArchive.builder()
            .manifest(createManifest())
            .locations(null)
            .sessions([])
            .climbs([])
            .build()
            
        graphService.graphDb = mockGraphDb
        def archiveNode = Mock(Node)
        
        mockGraphDb.beginTx() >> mockTransaction
        mockTransaction.createNode(GraphService.NodeLabel.Archive) >> archiveNode

        when: "importing the archive"
        graphService.importArchive(archive)

        then: "import completes without errors"
        1 * mockTransaction.commit()
    }

    def "should handle non-numeric location IDs in sessions"() {
        given: "a session with non-numeric location ID"
        def session = Session.builder()
            .id("session1")
            .date(LocalDate.of(2024, 1, 1))
            .locationId("invalid-id")
            .build()
            
        def archive = CLDFArchive.builder()
            .manifest(createManifest())
            .locations([])
            .sessions([session])
            .climbs([])
            .build()
            
        graphService.graphDb = mockGraphDb
        def archiveNode = Mock(Node)
        def sessionNode = Mock(Node)
        
        mockGraphDb.beginTx() >> mockTransaction
        mockTransaction.createNode(GraphService.NodeLabel.Archive) >> archiveNode
        mockTransaction.createNode(GraphService.NodeLabel.Session) >> sessionNode

        when: "importing the archive"
        graphService.importArchive(archive)

        then: "import completes without errors"
        1 * mockTransaction.commit()
        0 * sessionNode.createRelationshipTo(_, GraphService.RelType.AT_LOCATION)
    }

    def "should create indexes during initialization"() {
        given: "a mock schema"
        def mockSchema = Mock(org.neo4j.graphdb.schema.Schema)
        def mockIndexDefinition = Mock(org.neo4j.graphdb.schema.IndexDefinition)
        
        graphService.graphDb = mockGraphDb
        mockGraphDb.beginTx() >> mockTransaction
        mockTransaction.schema() >> mockSchema
        
        def mockIndexCreator = Mock(IndexCreator)
        mockSchema.indexFor(_ as Label) >> mockIndexCreator
        mockIndexCreator.on(_ as String) >> mockIndexCreator
        mockIndexCreator.create() >> mockIndexDefinition

        when: "creating indexes"
        graphService.createIndexes()

        then: "all indexes are created"
        2 * mockTransaction.commit()
        1 * mockSchema.awaitIndexesOnline(2, java.util.concurrent.TimeUnit.MINUTES)
    }

    def "should handle existing tags during climb import"() {
        given: "a climb with tags where one already exists"
        def climb = Climb.builder()
            .id(1)
            .date(LocalDate.of(2024, 1, 1))
            .tags(["existing-tag"])
            .build()
            
        def archive = CLDFArchive.builder()
            .manifest(createManifest())
            .locations([])
            .sessions([])
            .climbs([climb])
            .build()
            
        graphService.graphDb = mockGraphDb
        def archiveNode = Mock(Node)
        def climbNode = Mock(Node)
        def existingTagNode = Mock(Node)
        
        mockGraphDb.beginTx() >> mockTransaction
        mockTransaction.createNode(GraphService.NodeLabel.Archive) >> archiveNode
        mockTransaction.createNode(GraphService.NodeLabel.Climb) >> climbNode
        mockTransaction.findNode(GraphService.NodeLabel.Tag, "name", "existing-tag") >> existingTagNode

        when: "importing the archive"
        graphService.importArchive(archive)

        then: "existing tag is reused"
        0 * mockTransaction.createNode(GraphService.NodeLabel.Tag)
        1 * climbNode.createRelationshipTo(existingTagNode, GraphService.RelType.TAGGED_WITH)
        1 * mockTransaction.commit()
    }

    def "should handle climbs without grades"() {
        given: "a climb node without grade"
        graphService.graphDb = mockGraphDb
        def climbNode = Mock(Node)
        
        climbNode.getProperty("climbId") >> 1
        climbNode.getProperty("date") >> "2024-01-01"
        climbNode.getProperty("routeName", null) >> "Test Route"
        climbNode.getProperty("grade", null) >> null
        climbNode.getProperty("finishType", null) >> null
        climbNode.getProperty("attempts", 1) >> 1
        climbNode.getProperty("rating", null) >> null

        when: "converting node to climb"
        def climb = graphService.nodeToClimb(climbNode)

        then: "climb is created without grades"
        climb.id == 1
        climb.grades == null
        climb.rating == null
        climb.finishType == null
    }

    def "should handle invalid finish type during conversion"() {
        given: "a climb node with invalid finish type"
        graphService.graphDb = mockGraphDb
        def climbNode = Mock(Node)
        
        climbNode.getProperty("climbId") >> 1
        climbNode.getProperty("date") >> "2024-01-01"
        climbNode.getProperty("routeName", null) >> null
        climbNode.getProperty("grade", null) >> null
        climbNode.getProperty("finishType", null) >> "INVALID_TYPE"
        climbNode.getProperty("attempts", 1) >> 1
        climbNode.getProperty("rating", null) >> null

        when: "converting node to climb"
        def climb = graphService.nodeToClimb(climbNode)

        then: "climb is created with null finish type"
        climb.finishType == null
    }

    def "should test parseFinishType method"() {
        when: "parsing valid finish type"
        def result1 = graphService.parseFinishType("top")
        
        then: "correct enum is returned"
        result1 == FinishType.TOP
        
        when: "parsing null finish type"
        def result2 = graphService.parseFinishType(null)
        
        then: "null is returned"
        result2 == null
        
        when: "parsing empty finish type"
        def result3 = graphService.parseFinishType("")
        
        then: "null is returned"
        result3 == null
        
        when: "parsing invalid finish type"
        def result4 = graphService.parseFinishType("INVALID")
        
        then: "null is returned"
        result4 == null
    }

    // Helper methods
    private CLDFArchive createSampleArchive() {
        def location = Location.builder()
            .id(1)
            .name("Test Crag")
            .country("USA")
            .state("CA")
            .isIndoor(false)
            .coordinates(Location.Coordinates.builder()
                .latitude(37.7749)
                .longitude(-122.4194)
                .build())
            .build()
            
        def session = Session.builder()
            .id("session1")
            .date(LocalDate.of(2024, 1, 1))
            .location("Test Crag")
            .locationId("1")
            .build()
            
        def climb = Climb.builder()
            .id(1)
            .date(LocalDate.of(2024, 1, 1))
            .sessionId(1)
            .routeName("Test Route")
            .grades(Climb.GradeInfo.builder()
                .grade("5.10a")
                .build())
            .finishType(FinishType.TOP)
            .attempts(2)
            .rating(4)
            .tags(["crimpy"])
            .build()
            
        return CLDFArchive.builder()
            .manifest(createManifest())
            .locations([location])
            .sessions([session])
            .climbs([climb])
            .build()
    }
    
    private Manifest createManifest() {
        return Manifest.builder()
            .format("CLDF")
            .version("1.0.0")
            .creationDate(OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC))
            .build()
    }
}
