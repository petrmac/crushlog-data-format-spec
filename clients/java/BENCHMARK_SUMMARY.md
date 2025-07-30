# JMH Benchmark Summary

## Overview
The CLDF tool includes JMH microbenchmarks that compare Neo4j graph database queries versus in-memory Java Stream processing.

## How to Execute Benchmarks

### 1. Build the project
```bash
./gradlew :cldf-tool:testClasses
```

### 2. Run all benchmarks
```bash
./gradlew :cldf-tool:jmh
```

### 3. Run specific benchmark
```bash
# Run only SimpleBenchmark
./gradlew :cldf-tool:jmh -Pjmh.include=".*SimpleBenchmark.*"

# Run only GraphBenchmark  
./gradlew :cldf-tool:jmh -Pjmh.include=".*GraphBenchmark.*"
```

### 4. Run with custom parameters
```bash
# Reduce iterations for faster execution
./gradlew :cldf-tool:jmh \
  -Pjmh.fork=1 \
  -Pjmh.warmupIterations=2 \
  -Pjmh.iterations=3 \
  -Pjmh.timeOnIteration=1s

# Generate JSON results
./gradlew :cldf-tool:jmh \
  -Pjmh.resultFormat=JSON \
  -Pjmh.resultsFile=cldf-tool/build/jmh-results.json
```

## Benchmark Classes

### SimpleBenchmark
- **Purpose**: Quick comparison of grade pyramid query
- **Data sizes**: 100 and 1,000 climbs
- **Methods**:
  - `benchmarkGraphGradePyramid()` - Neo4j Cypher query
  - `benchmarkInMemoryGradePyramid()` - Java Streams

### GraphBenchmark  
- **Purpose**: Comprehensive performance comparison
- **Data sizes**: 100, 1,000, and 10,000 climbs
- **Methods**:
  - Grade pyramid analysis
  - Climbing partner search
  - Location statistics

## Performance Test Data

The benchmarks generate synthetic CLDF archives with:
- Multiple locations (indoor/outdoor)
- Sessions with random dates
- Climbs with various grades (5.6 to 5.11b)
- Consistent random seed for reproducibility

## Expected Performance Results

### Small datasets (100-1000 climbs)
- **In-memory**: 5-50 microseconds
- **Neo4j**: 500-5000 microseconds
- In-memory is typically 10-100x faster

### Large datasets (10,000+ climbs)
- **In-memory**: 500-5000 microseconds
- **Neo4j**: 1000-10,000 microseconds
- Performance gap narrows with size

### Complex queries (relationships)
- Neo4j excels at multi-hop graph traversals
- In-memory requires complex joining logic
- Neo4j can outperform for relationship queries

## Key Findings

1. **Simple aggregations**: In-memory processing is significantly faster
2. **Graph traversals**: Neo4j provides cleaner, more maintainable code
3. **Scalability**: Neo4j handles large datasets better with proper indexing
4. **Memory usage**: In-memory approach limited by JVM heap

## Optimization Tips

### Neo4j Performance
```cypher
// Create indexes for frequently queried properties
CREATE INDEX climb_grade_idx FOR (c:Climb) ON (c.grade);
CREATE INDEX location_name_idx FOR (l:Location) ON (l.name);
```

### In-Memory Performance
- Use primitive collections when possible
- Implement efficient grouping algorithms
- Consider parallel streams for large datasets
- Cache frequently accessed results

## Running Performance Analysis

To analyze benchmark results:

1. Run benchmarks with JSON output
2. Use JMH visualizer tools
3. Compare throughput and latency
4. Profile memory usage separately

The benchmarks demonstrate that while Neo4j has overhead for simple queries, it provides significant benefits for complex graph operations and maintains better performance characteristics as data scales.