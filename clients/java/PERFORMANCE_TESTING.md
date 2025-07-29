# Performance Testing Guide

## Overview

The CLDF tool includes JMH (Java Microbenchmark Harness) benchmarks to compare the performance of Neo4j graph queries versus in-memory data processing.

## Running Benchmarks

### Quick Start

```bash
# Run all benchmarks with default settings
./gradlew :cldf-tool:jmh

# Run specific benchmark
./gradlew :cldf-tool:jmh -Pjmh.include='.*SimpleBenchmark.*'
```

### Advanced Options

```bash
# Custom JMH parameters
./gradlew :cldf-tool:jmh \
  -Pjmh.fork=1 \
  -Pjmh.warmupIterations=2 \
  -Pjmh.iterations=5 \
  -Pjmh.timeUnit=ms

# Generate JSON results
./gradlew :cldf-tool:jmh \
  -Pjmh.resultFormat=JSON \
  -Pjmh.resultsFile=build/jmh-results.json
```

## Benchmark Classes

### GraphBenchmark
- Full-featured benchmark comparing Neo4j vs in-memory
- Tests multiple query patterns (grade pyramid, partners, location stats)
- Multiple data sizes (100, 1000, 10000 climbs)

### SimpleBenchmark  
- Simplified benchmark for quick testing
- Focuses on grade pyramid query
- Easier to understand and modify

## Performance Expectations

### Small Datasets (< 1000 climbs)
- In-memory: ~5-50 µs per query
- Neo4j: ~500-5000 µs per query
- In-memory is typically 10-100x faster

### Large Datasets (> 10000 climbs)
- In-memory: ~500-5000 µs per query  
- Neo4j: ~1000-10000 µs per query
- Performance gap narrows with dataset size

### Complex Queries (relationships, traversals)
- Neo4j excels at graph traversals
- In-memory requires complex joining logic
- Neo4j can be faster for multi-hop queries

## Interpreting Results

JMH outputs results in the format:
```
Benchmark                                  (dataSize)  Mode  Cnt    Score    Error  Units
GraphBenchmark.benchmarkGraphGradePyramid        100  avgt    5    4.112 ±  0.805  us/op
GraphBenchmark.benchmarkInMemoryGradePyramid     100  avgt    5    0.234 ±  0.045  us/op
```

- **Score**: Average execution time
- **Error**: Standard deviation (±)
- **Units**: Microseconds per operation (us/op)

## Best Practices

1. **Warm-up**: Always include warm-up iterations
2. **Multiple runs**: Use at least 3-5 measurement iterations
3. **Isolation**: Run benchmarks on a quiet system
4. **Profiling**: Use JMH profilers for deeper analysis

## Sample Data Generation

The benchmarks generate synthetic CLDF data:
- Random grades from common climbing scales
- Realistic session/climb ratios
- Varied location types (indoor/outdoor)
- Consistent seed for reproducibility

## Optimization Tips

### Neo4j Performance
- Create indexes on frequently queried properties
- Use parameters in Cypher queries
- Batch imports for better performance
- Configure heap and page cache sizes

### In-Memory Performance
- Use primitive collections when possible
- Implement efficient sorting/grouping
- Consider parallel streams for large data
- Cache computed results