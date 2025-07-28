# CLDF Tool Performance Benchmarks

## Test Data Generation

The benchmarks use synthetically generated CLDF archives with the following characteristics:

### Data Sizes
- **Small dataset**: 100 climbs, 10 sessions, 5 locations
- **Medium dataset**: 1,000 climbs, 100 sessions, 5 locations  
- **Large dataset**: 10,000 climbs, 1,000 sessions, 5 locations

### Test Queries

1. **Grade Pyramid Query**
   - Groups climbs by grade and counts occurrences
   - Common analytics query for understanding climbing progression

2. **Climbing Partners Query**
   - Finds climbers who have climbed together
   - Complex graph traversal with relationships

3. **Location Statistics Query**
   - Aggregates climb data by location
   - Tests join performance between locations and climbs

## Benchmark Execution

To run the benchmarks:

```bash
# Run all benchmarks
./gradlew :cldf-tool:jmh

# Run specific benchmark
./gradlew :cldf-tool:jmh -Pjmh.include=".*GraphBenchmark.*"

# Run with custom parameters
java -cp cldf-tool/build/classes/java/test:... \
  org.openjdk.jmh.Main \
  -f 1 -wi 3 -i 5 \
  ".*SimpleBenchmark.*"
```

## Expected Results

### Neo4j Graph Performance
- Excellent for complex relationship queries
- Overhead for simple aggregations
- Benefits from indexing and query optimization

### In-Memory Performance  
- Faster for simple filtering and aggregation
- Memory intensive for large datasets
- Limited by JVM heap size

## Performance Considerations

1. **Neo4j Advantages**:
   - Complex graph traversals
   - Relationship-based queries
   - Scalability with proper indexing
   - ACID compliance

2. **In-Memory Advantages**:
   - Simple aggregations
   - Small datasets
   - No I/O overhead
   - Predictable performance

3. **Hybrid Approach**:
   - Use Neo4j for complex analytics
   - Cache simple aggregations in memory
   - Lazy loading for large datasets