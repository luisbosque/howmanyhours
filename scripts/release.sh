#!/bin/bash
set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if version argument is provided
if [ -z "$1" ]; then
    echo -e "${RED}Usage: ./release.sh <version>${NC}"
    echo "Example: ./release.sh 1.1.0"
    exit 1
fi

NEW_VERSION="$1"
BUILD_GRADLE="app/build.gradle.kts"

# Validate semantic version format
if ! [[ $NEW_VERSION =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo -e "${RED}Error: Version must be in format X.Y.Z (e.g., 1.2.0)${NC}"
    exit 1
fi

# Check if working directory is clean
if ! git diff-index --quiet HEAD --; then
    echo -e "${YELLOW}Warning: You have uncommitted changes${NC}"
    read -p "Continue anyway? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# Get current versionCode
CURRENT_VERSION_CODE=$(grep "versionCode = " "$BUILD_GRADLE" | sed 's/.*versionCode = //' | sed 's/[^0-9]//g')
NEW_VERSION_CODE=$((CURRENT_VERSION_CODE + 1))

echo -e "${GREEN}Updating version to ${NEW_VERSION} (versionCode: ${NEW_VERSION_CODE})${NC}"

# Update build.gradle.kts
sed -i "s/versionCode = .*/versionCode = ${NEW_VERSION_CODE}/" "$BUILD_GRADLE"
sed -i "s/versionName = .*/versionName = \"${NEW_VERSION}\"/" "$BUILD_GRADLE"

echo -e "${GREEN}✓ Updated build.gradle.kts${NC}"

# Check for keystore
if [ ! -f "keystore.properties" ]; then
    echo -e "${RED}Error: keystore.properties not found!${NC}"
    echo "Release builds require signing configuration."
    echo "See docs/RELEASE.md for setup instructions."
    exit 1
fi

echo -e "${GREEN}✓ Keystore configuration found${NC}"

# Update CHANGELOG.md
TODAY=$(date +%Y-%m-%d)
sed -i "s/## \[Unreleased\]/## [Unreleased]\n\n### Added\n### Changed\n### Fixed\n### Removed\n\n## [${NEW_VERSION}] - ${TODAY}/" CHANGELOG.md

echo -e "${GREEN}✓ Updated CHANGELOG.md${NC}"

# Create git commit and tag
git add "$BUILD_GRADLE" CHANGELOG.md
git commit -m "Bump version to ${NEW_VERSION}

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"

git tag -a "v${NEW_VERSION}" -m "Release version ${NEW_VERSION}"

echo -e "${GREEN}✓ Created git commit and tag v${NEW_VERSION}${NC}"
echo ""

# Build signed release
echo -e "${YELLOW}Building signed release APK and AAB...${NC}"
./gradlew clean assembleRelease bundleRelease

if [ $? -eq 0 ]; then
    echo ""
    echo -e "${GREEN}✓ Release build successful!${NC}"
    echo ""
    echo -e "${GREEN}Generated artifacts:${NC}"

    APK_PATH="app/build/outputs/apk/release/app-release.apk"
    AAB_PATH="app/build/outputs/bundle/release/app-release.aab"

    if [ -f "$APK_PATH" ]; then
        APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
        echo "  APK: $APK_PATH ($APK_SIZE)"
    fi

    if [ -f "$AAB_PATH" ]; then
        AAB_SIZE=$(du -h "$AAB_PATH" | cut -f1)
        echo "  AAB: $AAB_PATH ($AAB_SIZE)"
    fi

    echo ""
    echo -e "${YELLOW}Next steps:${NC}"
    echo "1. Test the release build:"
    echo "   adb install -r $APK_PATH"
    echo "2. Review the changes:"
    echo "   git show HEAD"
    echo "3. Push to repository:"
    echo "   git push && git push --tags"
    echo "4. Distribute:"
    echo "   - Upload APK to your website"
    echo "   - Or upload AAB to Google Play Console"
else
    echo ""
    echo -e "${RED}✗ Release build failed!${NC}"
    echo "Check the error messages above."
    exit 1
fi
