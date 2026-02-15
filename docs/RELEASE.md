# Release Process

This document explains how to create a release of How Many Hours.

## Creating a Release

### 1. Bump the Version

Edit `app/build.gradle.kts` and update both version fields:

```kotlin
versionCode = 2          // Increment by 1 each release
versionName = "1.1.0"    // Semantic versioning
```

Commit the version bump:
```bash
git add app/build.gradle.kts
git commit -m "Bump version to 1.1.0"
git push
```

### 2. Tag and Push

```bash
git tag v1.1.0
git push origin v1.1.0
```

The tag push triggers the release workflow automatically.

### 3. What Happens Next

The GitHub Actions workflow (`.github/workflows/release.yml`) will:

1. Run tests
2. Build a release APK (signed or unsigned, depending on whether signing secrets are configured)
3. If signing secrets are present, also build a signed AAB (for Google Play)
4. Create a GitHub Release with the artifacts attached
5. Include SHA-256 checksums for verification

**No signing secrets?** That's fine. The workflow builds an unsigned release APK that users can install directly. See [Signing (optional)](#signing-optional) below.

## Installing the APK

Users can install the APK directly on their Android device (Android 9.0+):

1. Download the `.apk` file from the [latest release](https://github.com/luisbosque/howmanyhours/releases/latest)
2. On Android, go to **Settings > Security** (or **Settings > Apps > Special access**)
3. Enable **Install unknown apps** for your browser or file manager
4. Open the downloaded APK and tap **Install**

Alternatively, install via ADB:
```bash
adb install howmanyhours-vX.Y.Z.apk
```

## Signing (optional)

Signing is only required if you plan to publish to the Google Play Store. For direct APK distribution via GitHub Releases, unsigned builds work fine.

### What signing gives you

- **Play Store requirement**: Google Play only accepts signed APKs/AABs
- **Update continuity**: Once users install a signed APK, future updates must use the same key. If you switch keys, users must uninstall and reinstall.

### Setting up signing secrets

If you want signed builds, add four secrets in your GitHub repo under **Settings > Secrets and variables > Actions**:

| Secret | Description |
|--------|-------------|
| `KEYSTORE_FILE` | Base64-encoded `.jks` keystore file |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias (e.g., `howmanyhours`) |
| `KEY_PASSWORD` | Key password |

To encode your keystore:
```bash
base64 -w0 howmanyhours-release.jks | pbcopy   # macOS
base64 -w0 howmanyhours-release.jks | xclip     # Linux
```

See [SIGNING.md](SIGNING.md) for details on generating and managing keystores.

### Publishing to Google Play

The release workflow builds the artifacts but does **not** upload to Google Play. To publish:

1. Download the signed `.aab` file from the GitHub Release
2. Upload it manually in [Google Play Console](https://play.google.com/console)

Automated Play Store uploads (via Fastlane or the `r0adkll/upload-google-play` action) require a separate Google Play service account key - that's a different credential from the signing keystore.

## Troubleshooting

### APK won't install on device
Ensure the device runs Android 9.0 (API 28) or higher and that "Install unknown apps" is enabled for the source app.

### Workflow fails at signing step
If you configured signing secrets, verify all four are set correctly. The keystore file must be base64-encoded without line breaks.
