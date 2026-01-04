# Versioning & Release Process

## Version Numbers

- **versionName**: Semantic version shown to users (e.g., `1.2.3`)
  - Format: `MAJOR.MINOR.PATCH`
  - MAJOR: Breaking changes
  - MINOR: New features (backwards compatible)
  - PATCH: Bug fixes

- **versionCode**: Integer for Android, auto-incremented each release
  - Used by Play Store to determine which version is newer
  - Must always increase

## Release Steps

1. **Update version**: `./release.sh <version>` (e.g., `./release.sh 1.1.0`)
   - Updates versionName and increments versionCode
   - Updates CHANGELOG.md
   - Creates git commit and tag

2. **Build release**: `./gradlew assembleRelease`
   - Generates signed APK/AAB in `app/build/outputs/`

3. **Test**: Install and test the release build

4. **Push**: `git push && git push --tags`

5. **Publish**: Upload APK/AAB to store or website

## Manual Version Update

Edit `app/build.gradle.kts`:
```kotlin
versionCode = X      // Increment by 1
versionName = "X.Y.Z"  // Update semantic version
```

## Version Display

Version is automatically shown in Settings > About section (dynamically reads from build config).
