# Android App Signing Guide

This document explains how the app signing is configured and how to manage the signing keys securely.

## Overview

Android apps must be digitally signed to be installed on devices. The same signature must be used for all updates to your app. **Losing your signing key means you cannot update your app on the Play Store.**

## Current Setup

A release keystore has been generated at the project root:
- **File**: `howmanyhours-release.jks`
- **Alias**: `howmanyhours`
- **Validity**: 10,000 days (~27 years)
- **Algorithm**: RSA 2048-bit

## Configuration Files

### keystore.properties (NEVER commit this!)

This file contains your signing credentials and is excluded from git via `.gitignore`:

```properties
storeFile=howmanyhours-release.jks
storePassword=YOUR_PASSWORD
keyAlias=howmanyhours
keyPassword=YOUR_PASSWORD
```

### Build Configuration

The signing configuration is in `app/build.gradle.kts`:
- **Debug builds**: Signed with Android debug certificate (automatic)
- **Release builds**: Signed with the release keystore

The build file includes Java imports for keystore handling:
```kotlin
import java.util.Properties
import java.io.FileInputStream
```

These imports are required in Gradle Kotlin DSL to load the keystore properties.

## Security & Backup

### CRITICAL: Backup Your Keystore

1. **Store the keystore file safely**:
   ```bash
   # Example: Copy to secure location
   cp howmanyhours-release.jks ~/Documents/Backups/
   cp keystore.properties ~/Documents/Backups/
   ```

2. **Backup locations** (choose multiple):
   - Encrypted USB drive
   - Password manager (store keystore + passwords)
   - Encrypted cloud storage (personal, not shared)
   - Physical secure location

3. **NEVER**:
   - Commit keystore files to git (already in .gitignore)
   - Share keystores in public repositories
   - Email keystores unencrypted
   - Store passwords in plain text in code

### Password Management

The initial passwords are set to `android123` for development. **Before public release**:

1. **Change to strong passwords**:
   ```bash
   # Change keystore password
   keytool -storepasswd -keystore howmanyhours-release.jks

   # Change key password
   keytool -keypasswd -alias howmanyhours -keystore howmanyhours-release.jks
   ```

2. **Update keystore.properties** with new passwords

3. **Store passwords securely** (password manager recommended)

## Generating a New Keystore

If you need to generate a new keystore (e.g., for a different app):

```bash
keytool -genkey -v \
  -keystore your-app-name.jks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias your-app-alias \
  -dname "CN=Your Name"
```

You'll be prompted for passwords. Then update `keystore.properties` accordingly.

## Verifying Your Keystore

To view keystore information:

```bash
keytool -list -v -keystore howmanyhours-release.jks -alias howmanyhours
```

To verify an APK signature:

```bash
jarsigner -verify -verbose -certs app/build/outputs/apk/release/app-release.apk
```

Or using apksigner:

```bash
$ANDROID_HOME/build-tools/34.0.0/apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
```

## Building Signed Releases

### Using Release Script (Recommended)

```bash
./scripts/release.sh 1.0.0
```

This will:
1. Update version numbers
2. Update CHANGELOG
3. Build signed release APK/AAB
4. Create git tag

### Manual Build

```bash
# Build signed release APK
./gradlew assembleRelease

# Output: app/build/outputs/apk/release/app-release.apk

# Build signed AAB (for Play Store)
./gradlew bundleRelease

# Output: app/build/outputs/bundle/release/app-release.aab
```

## CI/CD Integration

For automated builds (GitHub Actions, etc.):

1. **Encode keystore as base64**:
   ```bash
   base64 howmanyhours-release.jks > keystore.b64
   ```

2. **Add as GitHub Secret**:
   - Go to repo Settings → Secrets → New repository secret
   - Name: `KEYSTORE_FILE` (paste base64 content)
   - Name: `KEYSTORE_PASSWORD` (paste password)
   - Name: `KEY_ALIAS` (value: `howmanyhours`)
   - Name: `KEY_PASSWORD` (paste key password)

3. **In workflow**, decode and use:
   ```yaml
   - name: Decode keystore
     run: echo "${{ secrets.KEYSTORE_FILE }}" | base64 -d > app/keystore.jks

   - name: Build signed APK
     env:
       KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
       KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
     run: ./gradlew assembleRelease
   ```

See `.github/workflows/release.yml` for the complete implementation.

## Play Store App Signing

Google Play offers **Play App Signing** which adds an extra layer of security:

1. You sign with your **upload key** (this keystore)
2. Google re-signs with the **app signing key** they manage
3. If you lose your upload key, Google can reset it
4. The app signing key is secured by Google

**Recommended**: Enroll in Play App Signing when publishing to Play Store.

## Troubleshooting

### "keystore.properties not found"

Create the file from the template:
```bash
cp keystore.properties.example keystore.properties
```

Then edit with your actual passwords.

### "Wrong password" error

Verify your password:
```bash
keytool -list -keystore howmanyhours-release.jks
```

### APK won't install

Ensure you're installing the release build and it's properly signed:
```bash
./gradlew assembleRelease
adb install -r app/build/outputs/apk/release/app-release.apk
```

## Key Rotation (Future)

If you need to rotate keys (not recommended unless compromised):

1. Generate new keystore
2. For Play Store: Use Play Console to update upload key
3. For direct distribution: Users must uninstall old app first (different signature)

## Summary

- ✅ Keystore generated and configured
- ✅ Build system configured for signing
- ⚠️ **BACKUP your keystore file NOW**
- ⚠️ **Change default passwords before public release**
- ⚠️ **Never commit keystore.properties to git**
- ✅ Ready to build signed releases

For more information: https://developer.android.com/studio/publish/app-signing
