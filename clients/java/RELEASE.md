# Release Process for CLDF Java

This document describes the release process for the CLDF Java client library and CLI tool.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Release Workflow](#release-workflow)
- [Maven Central Setup](#maven-central-setup)
- [Manual Release Process](#manual-release-process)
- [Troubleshooting](#troubleshooting)

## Prerequisites

### For Automated Releases

1. **GitHub Repository Secrets** (required for Maven Central publishing):
   - `MAVEN_USERNAME`: Sonatype OSSRH username
   - `MAVEN_PASSWORD`: Sonatype OSSRH password
   - `SIGNING_KEY`: GPG private key (exported as ASCII-armored text)
   - `SIGNING_PASSWORD`: GPG key passphrase

2. **Version Bumping**:
   - Update version in `/clients/java/build.gradle` (allprojects block)
   - Update version in module READMEs
   - Update CHANGELOG.md

### For Manual Releases

1. **Local Environment**:
   - Java 21+ (Temurin or Oracle)
   - GraalVM 21+ (for native image builds)
   - GPG key for signing artifacts
   - Sonatype OSSRH account with permissions for `io.cldf` group

## Release Workflow

### Automated Release (Recommended)

1. **Prepare the Release**:
   ```bash
   # Update version in build.gradle
   # Update CHANGELOG.md
   # Commit changes
   git add .
   git commit -m "Prepare release v1.0.0"
   git push origin main
   ```

2. **Create and Push Tag**:
   ```bash
   git tag java-v1.0.0
   git push origin java-v1.0.0
   ```

3. **GitHub Actions will automatically**:
   - Build all modules for multiple platforms
   - Create native binaries (Linux, macOS Intel/ARM, Windows)
   - Create GitHub Release with artifacts
   - Publish to Maven Central (if secrets are configured)

4. **Monitor the Release**:
   - Check Actions tab for workflow progress
   - Verify artifacts in the GitHub Release
   - Check Maven Central (may take 10-30 minutes to appear)

### Manual Workflow Dispatch

1. Go to Actions → Java Release workflow
2. Click "Run workflow"
3. Enter version number (e.g., "1.0.0")
4. Click "Run workflow"

## Maven Central Setup

### One-Time Setup

1. **Create Sonatype OSSRH Account**:
   - Register at https://issues.sonatype.org
   - Create JIRA ticket requesting `io.cldf` namespace
   - Wait for approval (usually 1-2 business days)

2. **Generate GPG Key**:
   ```bash
   # Generate key
   gpg --gen-key
   
   # List keys to find your key ID
   gpg --list-secret-keys --keyid-format LONG
   
   # Export public key to key server
   gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
   
   # Export private key for GitHub secret
   gpg --export-secret-keys --armor YOUR_KEY_ID > private-key.asc
   ```

3. **Configure GitHub Secrets**:
   ```bash
   # In repository settings → Secrets and variables → Actions
   
   # Add MAVEN_USERNAME (your Sonatype username)
   # Add MAVEN_PASSWORD (your Sonatype password)
   # Add SIGNING_KEY (contents of private-key.asc)
   # Add SIGNING_PASSWORD (your GPG key passphrase)
   ```

### Publishing Requirements

The published artifacts must meet Maven Central requirements:

- ✅ Valid POM with required fields (name, description, URL, licenses, developers, SCM)
- ✅ Javadoc JAR
- ✅ Sources JAR
- ✅ GPG signatures for all artifacts
- ✅ Unique version (no overwrites allowed)

## Manual Release Process

### Building Locally

```bash
cd clients/java

# Clean build
./gradlew clean build

# Run tests
./gradlew test

# Build native image
./gradlew :cldf-tool:nativeCompile

# Create all artifacts
./gradlew :cldf-java:jar :cldf-java:javadocJar :cldf-java:sourcesJar
./gradlew :cldf-tool:jar :cldf-tool:shadowJar
```

### Publishing to Maven Central

```bash
# Set environment variables
export ORG_GRADLE_PROJECT_ossrhUsername="your-username"
export ORG_GRADLE_PROJECT_ossrhPassword="your-password"
export ORG_GRADLE_PROJECT_signingKey="$(cat private-key.asc)"
export ORG_GRADLE_PROJECT_signingPassword="your-gpg-passphrase"

# Publish to staging repository
./gradlew :cldf-java:publishToSonatype

# Close and release staging repository
./gradlew closeAndReleaseSonatypeStagingRepository
```

### Creating GitHub Release Manually

1. Go to Releases → "Draft a new release"
2. Create tag: `java-v1.0.0`
3. Title: `CLDF Java v1.0.0`
4. Upload artifacts:
   - Native binaries (cldf-linux-amd64.tar.gz, etc.)
   - JAR files from `cldf-java/build/libs/`
   - JAR files from `cldf-tool/build/libs/`
5. Copy release notes from CHANGELOG.md
6. Publish release

## Version Numbering

We follow [Semantic Versioning](https://semver.org/):

- **MAJOR.MINOR.PATCH** (e.g., 1.0.0)
- **MAJOR**: Breaking API changes
- **MINOR**: New features, backward compatible
- **PATCH**: Bug fixes, backward compatible

### Snapshot Versions

For development builds:
```gradle
version = '1.0.1-SNAPSHOT'
```

## Troubleshooting

### Common Issues

1. **GPG Signing Fails**:
   ```
   Error: gpg: signing failed: Inappropriate ioctl for device
   ```
   Solution: Add to ~/.gnupg/gpg.conf:
   ```
   use-agent
   pinentry-mode loopback
   ```

2. **Maven Central Sync Delays**:
   - New artifacts can take 10-30 minutes to appear
   - Search may take up to 2 hours to index
   - Check: https://repo1.maven.org/maven2/io/cldf/

3. **Native Image Build Fails**:
   - Ensure GraalVM is installed
   - Check Jackson initialization issues
   - Review reflection configuration

4. **Sonatype Staging Issues**:
   - Login to https://oss.sonatype.org
   - Check staging repositories
   - Look for validation errors
   - Drop failed staging repos before retrying

### Rollback Procedure

GitHub releases can be deleted, but Maven Central artifacts are permanent.
For critical issues:

1. Delete GitHub release and tag
2. Create new patch version with fix
3. Document known issues in release notes
4. Consider yanking recommendation in security advisories

## Checklist

### Pre-Release Checklist

- [ ] All tests passing
- [ ] Version updated in build.gradle
- [ ] CHANGELOG.md updated
- [ ] README versions updated
- [ ] API documentation current
- [ ] Native image builds successfully
- [ ] Manual testing completed

### Post-Release Checklist

- [ ] GitHub Release created
- [ ] Artifacts downloadable
- [ ] Maven Central sync confirmed
- [ ] Documentation updated
- [ ] Announcement prepared
- [ ] Next version set to SNAPSHOT

## Support

For issues with the release process:

1. Check GitHub Actions logs
2. Review Sonatype OSSRH documentation
3. File issue in repository
4. Contact maintainers