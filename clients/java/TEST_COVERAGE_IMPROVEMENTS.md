# Test Coverage Improvements for CLDF Tool

## Overview
Added comprehensive Spock tests to improve test coverage for the cldf-tool module.

## New Test Files Created

### Command Tests
1. **ValidateCommandSpec.groovy**
   - Tests validation of CLDF archives
   - Tests strict mode validation
   - Tests checksum validation
   - Tests error handling for invalid files
   - Tests schema version validation

2. **ExtractCommandSpec.groovy**
   - Tests extraction of archive components
   - Tests filtering by component type
   - Tests date range filtering
   - Tests media extraction
   - Tests output to stdout
   - Tests pretty print formatting

3. **MergeCommandSpec.groovy**
   - Tests merging multiple archives
   - Tests duplicate handling strategies
   - Tests date filtering during merge
   - Tests glob pattern support
   - Tests empty archive handling
   - Tests custom merge rules

### Service Tests
4. **CLDFServiceSpec.groovy**
   - Tests reading/writing CLDF archives
   - Tests creating archives from JSON
   - Tests merging archives
   - Tests filtering by date and location
   - Tests statistics calculation
   - Tests reference validation
   - Tests format export

5. **ValidationServiceSpec.groovy**
   - Tests manifest validation
   - Tests location reference validation
   - Tests session reference validation
   - Tests duplicate ID detection
   - Tests grade format validation
   - Tests coordinate range validation
   - Tests media reference validation
   - Tests checksum validation

6. **GraphServiceSpec.groovy**
   - Tests Neo4j initialization
   - Tests archive import to graph
   - Tests relationship creation
   - Tests Cypher query execution
   - Tests graph export back to CLDF
   - Tests parameterized queries
   - Tests complex aggregations

## Test Coverage Areas

### Commands
- ✅ BaseCommand (existing)
- ✅ CreateCommand (existing)
- ✅ QueryCommand (existing)
- ✅ ValidateCommand (new)
- ✅ ExtractCommand (new)
- ✅ MergeCommand (new)
- ✅ LoadCommand (integration tests)
- ✅ GraphQueryCommand (integration tests)
- ❌ ConvertCommand (TODO)

### Services
- ✅ CLDFService (new)
- ✅ ValidationService (new)
- ✅ QueryService (existing)
- ✅ GraphService (new)

### Utilities
- ✅ InputHandler (existing)
- ✅ OutputHandler (existing)
- ❌ JsonUtils (TODO)

### Integration Tests
- ✅ MCPIntegrationSpec (existing)
- ✅ GraphIntegrationSpec (existing)

## Key Testing Patterns

### 1. Mock Injection
```groovy
CLDFService cldfService = Mock()
ValidationService validationService = Mock()

@Subject
ValidateCommand command = new ValidateCommand(
    cldfService: cldfService,
    validationService: validationService
)
```

### 2. Test Data Builders
```groovy
private CLDFArchive createTestArchive() {
    CLDFArchive.builder()
        .manifest(createManifest())
        .locations([createLocation(1, "Test")])
        .build()
}
```

### 3. Behavior Verification
```groovy
then:
1 * cldfService.readArchive(archiveFile) >> archive
1 * validationService.validate(archive) >> validationResult
```

### 4. Edge Case Testing
- Empty archives
- Invalid references
- Missing required fields
- IO errors
- Concurrent operations

## Running Tests

```bash
# Run all tests
./gradlew :cldf-tool:test

# Run with coverage report
./gradlew :cldf-tool:test jacocoTestReport

# Run specific test class
./gradlew :cldf-tool:test --tests "*ValidateCommandSpec"

# Run with detailed output
./gradlew :cldf-tool:test --info
```

## Coverage Report
After running tests with JaCoCo, view the coverage report at:
`cldf-tool/build/reports/jacoco/test/html/index.html`

## Next Steps
1. Add tests for ConvertCommand
2. Add tests for JsonUtils
3. Add more edge case tests
4. Add performance tests for large archives
5. Add stress tests for Neo4j operations