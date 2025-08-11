# CLDF Release Process

## Overview

This document describes the release process for CLDF Java and Dart libraries. The process is largely automated through GitHub Actions.

## Prerequisites

### For Maven Central (Java)

1. **OSSRH Account**: Create an account at https://s01.oss.sonatype.org/
2. **GPG Key**: Generate a GPG key for signing artifacts
3. **GitHub Secrets**: Configure the following repository secrets:
   - `OSSRH_USERNAME`: Your OSSRH username
   - `OSSRH_TOKEN`: Your OSSRH token (not password)
   - `MAVEN_GPG_PRIVATE_KEY`: Your GPG private key (exported with `gpg --armor --export-secret-keys`)
   - `MAVEN_GPG_PASSPHRASE`: Your GPG key passphrase

### For pub.dev (Dart)

1. **pub.dev Account**: Ensure you have a verified pub.dev account
2. **Package Ownership**: Be listed as an uploader for the `cldf` package
3. **Authentication**: The GitHub Action uses OIDC authentication (no secrets needed)

## Release Process

### 1. Prepare the Release

Run the prepare script to update version numbers:

```bash
./scripts/prepare-release.sh 1.2.3
```

This script will:
- Update version numbers in all configuration files
- Create a CHANGELOG template entry
- Provide instructions for next steps

### 2. Update CHANGELOG

Edit `CHANGELOG.md` and document:
- New features added
- Changes to existing functionality
- Bug fixes
- Breaking changes (if any)
- Deprecations

### 3. Run Tests Locally

Ensure all tests pass:

```bash
# Java tests
cd clients/java
./gradlew clean build

# Dart tests
cd clients/dart/cldf
dart pub get
dart run build_runner build --delete-conflicting-outputs
dart test
```

### 4. Commit Changes

```bash
git add -A
git commit -m "chore: prepare release v1.2.3"
git push origin main
```

### 5. Create Release via GitHub Actions

1. Go to the [Actions tab](https://github.com/petrmac/crushlog-data-format-spec/actions)
2. Select "Release and Publish" workflow
3. Click "Run workflow"
4. Fill in the parameters:
   - **Version**: The version to release (e.g., `1.2.3`)
   - **Publish Java**: Whether to publish to Maven Central
   - **Publish Dart**: Whether to publish to pub.dev
   - **Create GitHub Release**: Whether to create a GitHub release
5. Click "Run workflow"

The workflow will:
- Validate the version format
- Build and test both Java and Dart packages
- Publish to Maven Central (if selected)
- Publish to pub.dev (if selected)
- Create a Git tag
- Create a GitHub release with artifacts

### 6. Verify Release

After the workflow completes:

1. **GitHub Release**: Check https://github.com/petrmac/crushlog-data-format-spec/releases
2. **Maven Central**: Search for `io.cldf:cldf-java` at https://central.sonatype.com/
3. **pub.dev**: Check https://pub.dev/packages/cldf

## Manual Release (Emergency Only)

If the automated process fails, you can release manually:

### Java (Maven Central)

```bash
cd clients/java
./gradlew publishToSonatype closeAndReleaseSonatypeStagingRepository
```

### Dart (pub.dev)

```bash
cd clients/dart/cldf
dart pub publish
```

### Create Git Tag

```bash
git tag -a v1.2.3 -m "Release v1.2.3"
git push origin v1.2.3
```

## Version Numbering

We follow [Semantic Versioning](https://semver.org/):

- **MAJOR**: Incompatible API changes
- **MINOR**: New functionality (backwards compatible)
- **PATCH**: Bug fixes (backwards compatible)
- **Pre-release**: Use suffixes like `-beta1`, `-rc1`

Examples:
- `1.0.0` - First stable release
- `1.1.0` - New features added
- `1.1.1` - Bug fixes
- `2.0.0` - Breaking changes
- `2.0.0-beta1` - Beta release

## Rollback Process

If a release has issues:

1. **Do NOT delete the tag or release** (packages cannot be unpublished)
2. Fix the issue in a new commit
3. Release a new patch version
4. Update documentation noting the issue with the previous version

## Release Checklist

- [ ] Version numbers updated (use `prepare-release.sh`)
- [ ] CHANGELOG.md updated with all changes
- [ ] All tests passing locally
- [ ] Documentation updated if needed
- [ ] Breaking changes clearly documented
- [ ] Migration guide provided (for major versions)
- [ ] PR merged to main branch
- [ ] GitHub Actions workflow run successfully
- [ ] Packages visible on Maven Central and pub.dev
- [ ] GitHub release created with artifacts

## Troubleshooting

### Maven Central Issues

1. **401 Unauthorized**: Check `OSSRH_USERNAME` and `OSSRH_TOKEN` secrets
2. **Signing failed**: Verify `MAVEN_GPG_PRIVATE_KEY` and passphrase
3. **Staging repository not closing**: Check Sonatype requirements (javadoc, sources, POM metadata)

### pub.dev Issues

1. **Authentication failed**: Ensure GitHub Actions has proper OIDC permissions
2. **Package validation failed**: Run `dart pub publish --dry-run` locally
3. **Version conflict**: Ensure version hasn't been published before

### GitHub Actions Issues

1. **Workflow not found**: Ensure you're on the main branch
2. **Permission denied**: Check repository settings for Actions permissions
3. **Secrets not found**: Verify all required secrets are configured

## Support

For issues with the release process:
1. Check the [GitHub Actions logs](https://github.com/petrmac/crushlog-data-format-spec/actions)
2. Open an issue in the repository
3. Contact the maintainers