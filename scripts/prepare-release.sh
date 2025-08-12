#!/bin/bash

# Prepare release script
# This script updates version numbers across the project before release

set -e

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if version is provided
if [ $# -eq 0 ]; then
    echo -e "${RED}Error: Version number required${NC}"
    echo "Usage: $0 <version>"
    echo "Example: $0 1.2.3"
    exit 1
fi

VERSION=$1

# Validate version format
if ! [[ "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+(-[a-zA-Z0-9]+)?$ ]]; then
    echo -e "${RED}Error: Invalid version format${NC}"
    echo "Version must be in semantic version format (e.g., 1.2.3 or 1.2.3-beta1)"
    exit 1
fi

echo -e "${GREEN}Preparing release for version ${VERSION}${NC}"

# Update Java versions
echo -e "${YELLOW}Updating Java versions...${NC}"
sed -i.bak "s/version = \"[0-9.]*\"/version = \"$VERSION\"/" clients/java/build.gradle.kts
sed -i.bak "s/version = \"[0-9.]*\"/version = \"$VERSION\"/" clients/java/cldf-java/build.gradle.kts
sed -i.bak "s/version = \"[0-9.]*\"/version = \"$VERSION\"/" clients/java/cldf-tool/build.gradle.kts
rm clients/java/*.bak clients/java/*/*.bak 2>/dev/null || true

# Update Dart version
echo -e "${YELLOW}Updating Dart version...${NC}"
sed -i.bak "s/^version: .*/version: $VERSION/" clients/dart/cldf/pubspec.yaml
rm clients/dart/cldf/pubspec.yaml.bak 2>/dev/null || true

# Update sonar-project.properties versions
echo -e "${YELLOW}Updating SonarCloud versions...${NC}"
sed -i.bak "s/sonar.projectVersion=.*/sonar.projectVersion=$VERSION/" clients/java/sonar-project.properties 2>/dev/null || true
sed -i.bak "s/sonar.projectVersion=.*/sonar.projectVersion=$VERSION/" clients/dart/cldf/sonar-project.properties
rm clients/java/sonar-project.properties.bak clients/dart/cldf/sonar-project.properties.bak 2>/dev/null || true

# Update README versions
echo -e "${YELLOW}Updating README versions...${NC}"
sed -i.bak "s/cldf-java:[0-9.]*/cldf-java:$VERSION/" README.md
sed -i.bak "s/cldf: \^[0-9.]*/cldf: ^$VERSION/" README.md
rm README.md.bak 2>/dev/null || true

# Update CHANGELOG
echo -e "${YELLOW}Preparing CHANGELOG...${NC}"
if [ ! -f CHANGELOG.md ]; then
    echo "# Changelog" > CHANGELOG.md
    echo "" >> CHANGELOG.md
fi

# Add new version entry to CHANGELOG (if not already exists)
if ! grep -q "## \[$VERSION\]" CHANGELOG.md; then
    # Create temporary file with new version entry
    cat > CHANGELOG.tmp.md <<EOF
# Changelog

## [$VERSION] - $(date +%Y-%m-%d)

### Added
- _Add new features here_

### Changed
- _Add changes here_

### Fixed
- _Add fixes here_

### Deprecated
- _Add deprecations here_

### Removed
- _Add removals here_

### Security
- _Add security updates here_

EOF
    
    # Append existing changelog content (skip first line)
    tail -n +2 CHANGELOG.md >> CHANGELOG.tmp.md
    mv CHANGELOG.tmp.md CHANGELOG.md
    echo -e "${YELLOW}Added template for version $VERSION to CHANGELOG.md${NC}"
    echo -e "${RED}Please update CHANGELOG.md with actual changes before releasing${NC}"
fi

echo -e "${GREEN}✓ Version updated to $VERSION${NC}"
echo ""
echo "Next steps:"
echo "1. Review and update CHANGELOG.md with actual changes"
echo "2. Run tests to ensure everything works:"
echo "   - cd clients/java && ./gradlew clean build"
echo "   - cd clients/dart/cldf && dart test"
echo "3. Commit changes: git add -A && git commit -m \"chore: prepare release v$VERSION\""
echo "4. Create release using GitHub Actions:"
echo "   - Go to Actions → Release and Publish → Run workflow"
echo "   - Enter version: $VERSION"
echo ""
echo -e "${YELLOW}Note: Do not create tags manually. The GitHub Actions workflow will handle tagging.${NC}"