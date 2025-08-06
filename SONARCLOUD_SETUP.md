# SonarCloud Analysis Setup

## Overview
This document describes the SonarCloud analysis setup for the CrushLog Data Format Specification project, which includes both Java and Dart modules analyzed as separate projects.

## Problem Solved
Previously, Java and Dart CI workflows were running separate SonarCloud analyses on the same project key, causing coverage data to overwrite each other. The new setup uses separate SonarCloud projects for each language while coordinating the analysis in a single workflow.

## Architecture

### 1. Individual CI Workflows
- **Java CI** (`java-ci.yml`): Builds, tests, and uploads coverage artifacts
- **Dart CI** (`dart-ci.yml`): Builds, tests, and uploads coverage artifacts
- Both workflows NO LONGER run SonarCloud analysis directly

### 2. Unified SonarCloud Workflow
- **SonarCloud Analysis** (`sonarcloud.yml`): 
  - Triggers after both Java CI and Dart CI complete successfully
  - Downloads coverage artifacts from both workflows
  - Runs separate SonarCloud analyses for each project

## Configuration Files

### Java Configuration
- **`clients/java/build.gradle.kts`**: Configured for Java-only analysis
- Project key: `petrmac_crushlog-data-format-spec`

### Dart Configuration
- **`clients/dart/cldf/sonar-project.properties`**: Configured for Dart-only analysis
- Project key: `petrmac_crushlog-data-format-spec-dart`

## Implementation Steps

1. **Create SonarCloud Projects**:
   - Java project: `petrmac_crushlog-data-format-spec` (existing)
   - Dart project: `petrmac_crushlog-data-format-spec-dart` (needs to be created)

2. **Update CI Workflows**:
   - Java CI and Dart CI no longer run SonarCloud analysis
   - Both upload coverage artifacts

3. **Ensure Artifacts are Named Correctly**:
   - Java coverage: `java-coverage-reports`
   - Dart coverage: `dart-coverage-reports`

4. **Configure SonarCloud Workflow**:
   - Downloads both coverage artifacts
   - Runs Java analysis using Gradle
   - Runs Dart analysis using SonarScanner

## Benefits

1. **Separate Projects**: Java and Dart have their own SonarCloud dashboards
2. **No Overwrites**: Each project maintains its own coverage data
3. **Coordinated**: Analysis runs together after both CI workflows complete
4. **Clear Separation**: Each language has its own quality gates and metrics

## Workflow Diagram

```
[Java Code Change] ──┐
                     ├──> [Java CI] ──> [Coverage Artifact] ──┐
                     │                                         │
[Dart Code Change] ──┤                                         ├──> [SonarCloud Analysis]
                     │                                         │
                     └──> [Dart CI] ──> [Coverage Artifact] ──┘
```

## Notes

- The SonarCloud workflow only runs on push events (not pull requests from forks)
- Coverage artifacts are retained for 7 days
- The workflow waits for both CI workflows to complete before running
- If either CI workflow fails, SonarCloud analysis is skipped