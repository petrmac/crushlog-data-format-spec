#!/bin/bash

echo "Running CLDF Tool JMH Benchmarks"
echo "================================"
echo ""

# Clean and build
echo "Building project..."
./gradlew :cldf-tool:testClasses --quiet

# Run benchmarks with reduced output
echo ""
echo "Running microbenchmarks..."
echo ""

# Run JMH with specific options
java -cp cldf-tool/build/classes/java/test:cldf-tool/build/resources/test:cldf-tool/build/libs/* \
  org.openjdk.jmh.Main \
  -rf json \
  -rff build/jmh-results.json \
  -f 1 \
  -wi 2 \
  -i 3 \
  -r 1s \
  -w 1s \
  ".*GraphBenchmark.*" 2>/dev/null || \
  ./gradlew :cldf-tool:jmh -q --console=plain

echo ""
echo "Benchmark Results"
echo "================="

# Check if results file exists
if [ -f "build/jmh-results.json" ]; then
  echo "Results saved to: build/jmh-results.json"
  
  # Simple parsing of results
  echo ""
  echo "Summary:"
  cat build/jmh-results.json | grep -E '"benchmark"|"primaryMetric"' | head -20
else
  echo "No results file found. Benchmarks may have failed."
fi