#!/bin/bash

echo "================================="
echo "CLDF Tool JMH Benchmark Demo"
echo "================================="
echo ""
echo "This demonstrates how to run JMH microbenchmarks comparing"
echo "Neo4j graph queries vs in-memory filtering performance."
echo ""

# Show available benchmarks
echo "Available benchmarks in the project:"
echo "-----------------------------------"
find cldf-tool/src/test/java -name "*Benchmark*.java" -type f | while read file; do
    echo "- $(basename $file .java)"
done

echo ""
echo "To run the benchmarks, use one of these commands:"
echo ""
echo "1. Run all benchmarks (full suite):"
echo "   ./gradlew :cldf-tool:jmh"
echo ""
echo "2. Run specific benchmark class:"
echo "   ./gradlew :cldf-tool:jmh -Pjmh.include='.*SimpleBenchmark.*'"
echo ""
echo "3. Run with custom JMH options:"
echo "   ./gradlew :cldf-tool:jmh -Pjmh.options='-f 1 -wi 2 -i 3'"
echo ""
echo "4. Generate JSON results:"
echo "   ./gradlew :cldf-tool:jmh -Pjmh.resultFormat='JSON' -Pjmh.resultsFile='build/jmh-results.json'"
echo ""

# Check if we can show a sample
echo "Sample benchmark code (SimpleBenchmark.java):"
echo "--------------------------------------------"
head -60 cldf-tool/src/test/java/io/cldf/tool/benchmark/SimpleBenchmark.java | tail -20

echo ""
echo "The benchmarks compare performance between:"
echo "- Neo4j Cypher queries (graph database)"
echo "- Java Stream API (in-memory filtering)"
echo ""
echo "Results will show average execution time for each approach."