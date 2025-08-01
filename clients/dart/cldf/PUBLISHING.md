# Publishing to pub.dev

This guide explains how to publish the CLDF Dart package to pub.dev.

## Pre-publication Checklist

1. **Verify package score**:
   ```bash
   dart pub publish --dry-run
   ```

2. **Run analyzer**:
   ```bash
   dart analyze
   ```

3. **Run tests**:
   ```bash
   dart test
   ```

4. **Check formatting**:
   ```bash
   dart format --set-exit-if-changed .
   ```

5. **Update version** in `pubspec.yaml` if needed

6. **Update CHANGELOG.md** with release notes

## Publishing Steps

1. **Login to pub.dev** (if not already logged in):
   ```bash
   dart pub login
   ```

2. **Publish the package**:
   ```bash
   dart pub publish
   ```

3. **Verify publication** at https://pub.dev/packages/cldf

## Post-publication

1. **Tag the release** in Git:
   ```bash
   git tag -a dart-v1.0.0 -m "CLDF Dart package v1.0.0"
   git push origin dart-v1.0.0
   ```

2. **Update Flutter app** to use published version:
   ```yaml
   dependencies:
     cldf: ^1.0.0
   ```

## Maintenance

- Monitor issues at https://github.com/crushlog/crushlog-data-format-spec/issues
- Keep package updated with CLDF specification changes
- Follow semantic versioning for updates