# CI Workflow Structure

The Java CI workflow has been restructured for better efficiency and clarity. The workflow now runs in a sequential pipeline with the following stages:

## 1. Lint and Code Style (`lint`)
- **Runs on**: ubuntu-latest only
- **Purpose**: Quick validation of code formatting
- **Tasks**:
  - Spotless check for Java and Groovy code formatting
- **Duration**: ~30 seconds

## 2. Build, Test and Coverage (`build-test-coverage`)
- **Runs on**: ubuntu-latest only (single platform)
- **Purpose**: Main build and xxktest execution
- **Dependencies**: Requires lint to pass
- **Tasks**:
  - Full project build
  - Run all tests
  - Generate JaCoCo coverage reports
  - Generate test reports
- **Artifacts**:
  - Test results and reports
  - Coverage reports
  - JAR files
- **Duration**: ~2-3 minutes

## 3. SonarQube Analysis (`sonarqube`)
- **Runs on**: ubuntu-latest only
- **Purpose**: Code quality analysis
- **Dependencies**: Requires build-test-coverage to pass
- **Conditions**: Only runs on push events or PRs from the same repository
- **Tasks**:
  - SonarQube/SonarCloud analysis with coverage data
- **Duration**: ~1-2 minutes

## 4. Native Binary Builds (`build-native`)
- **Runs on**: Multiple platforms in parallel
  - ubuntu-latest (amd64)
  - macos-latest (amd64)
  - macos-latest (arm64) - cross-compiled
  - windows-latest (amd64)
- **Purpose**: Build native executables
- **Dependencies**: Requires build-test-coverage to pass
- **Tasks**:
  - GraalVM native compilation
  - Native binary testing (except cross-compiled ARM64)
- **Artifacts**:
  - Native binaries for each platform
- **Duration**: ~5-10 minutes per platform

## 5. Publish Artifacts (`publish-artifacts`)
- **Runs on**: ubuntu-latest only
- **Purpose**: Prepare all release artifacts
- **Dependencies**: Requires both build-test-coverage and build-native to pass
- **Conditions**: Only runs on push to main branch
- **Tasks**:
  - Build fat/standalone JAR
  - Build source JARs
  - Build JavaDoc JARs
  - Collect all artifacts (JARs and native binaries)
  - Generate SHA256 checksums
- **Artifacts**:
  - Complete release package with all artifacts
- **Duration**: ~2-3 minutes

## Key Improvements

1. **Sequential Dependencies**: Each stage depends on the previous, preventing unnecessary work if earlier stages fail
2. **Single Test Run**: Tests run only once in the build-test-coverage stage, not repeated across platforms
3. **Parallel Native Builds**: Native compilation happens in parallel for all platforms
4. **Conditional Execution**: SonarQube and publishing only run when appropriate
5. **Artifact Management**: Clear artifact flow from build to publishing stages
6. **Efficient Caching**: Gradle and SonarQube caches are utilized throughout

## Total Pipeline Duration

- **On success**: ~10-15 minutes (limited by native builds running in parallel)
- **On early failure**: 30 seconds (if lint fails)

## Required Secrets

- `SONAR_TOKEN`: For SonarQube/SonarCloud analysis
- `GITHUB_TOKEN`: Automatically provided by GitHub Actions
- Optional for Maven publishing:
  - `MAVEN_USERNAME`
  - `MAVEN_PASSWORD`
  - `SIGNING_KEY`
  - `SIGNING_PASSWORD`