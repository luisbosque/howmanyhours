# Release Build Preparation Checklist

## 1. Code Preparation

### Version Management
- [ ] Update `versionCode` in `app/build.gradle.kts` (increment by 1)
- [ ] Update `versionName` in `app/build.gradle.kts` (e.g., "1.0.0")
- [ ] Update version in privacy policy and other docs

### Build Configuration
- [ ] Enable ProGuard/R8 minification:
```kotlin
buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

### Code Cleanup
- [ ] Remove debug logging: `Log.d()`, `println()`, etc.
- [ ] Remove test code and commented code
- [ ] Check TODOs and FIXMEs
- [ ] Verify no hardcoded test data

### Security
- [ ] No API keys or secrets in code
- [ ] No debug certificates in release
- [ ] Verify all permissions are necessary

## 2. App Signing

### Create Keystore (First Time Only)
```bash
keytool -genkey -v -keystore howmanyhours-release.keystore \
        -alias howmanyhours -keyalg RSA -keysize 2048 -validity 25000
```

### Sign APK/Bundle
```bash
./gradlew bundleRelease  # For AAB (recommended)
# OR
./gradlew assembleRelease  # For APK
```

### Keystore Security
- [ ] Store keystore file securely (backup multiple locations)
- [ ] Document keystore password securely  
- [ ] Never commit keystore to version control

## 3. Testing Release Build

### Functionality Testing
- [ ] Install release APK on test device
- [ ] Test all core features:
  - [ ] Create projects
  - [ ] Start/stop tracking
  - [ ] Manual time entry
  - [ ] Monthly summaries
  - [ ] CSV export
  - [ ] Project deletion
- [ ] Test app lifecycle (background/foreground)
- [ ] Test notifications
- [ ] Test with various time durations

### Performance Testing
- [ ] Check app startup time
- [ ] Monitor memory usage
- [ ] Test with many projects/entries
- [ ] Verify no memory leaks

## 4. Assets Preparation

### App Icon
- [ ] Create 512x512px high-res icon
- [ ] Test icon visibility on various backgrounds
- [ ] Ensure icon follows Material Design guidelines

### Screenshots
- [ ] Take 2-8 high-quality screenshots
- [ ] Use realistic project names and data
- [ ] Ensure clean UI (no debug overlays)
- [ ] Cover main app features

### Store Graphics
- [ ] Feature graphic: 1024x500px banner
- [ ] Icon: 512x512px PNG
- [ ] Optional: promotional video

## 5. Legal Documents

### Privacy Policy
- [ ] Host privacy policy on public URL (GitHub Pages, etc.)
- [ ] Ensure it matches actual data handling
- [ ] Include contact information

### Content Rating
- [ ] Complete Google's content rating questionnaire
- [ ] Review answers for accuracy
- [ ] Expected rating: "Everyone"

## 6. Play Console Setup

### Developer Account
- [ ] Register Google Play Developer account ($25)
- [ ] Verify identity and payment info
- [ ] Accept developer agreements

### App Listing
- [ ] Upload APK/AAB
- [ ] Fill all required store listing fields:
  - [ ] App title
  - [ ] Short description
  - [ ] Full description  
  - [ ] Screenshots
  - [ ] Icon
  - [ ] Category (Business/Productivity)
- [ ] Complete Data Safety section
- [ ] Set content rating
- [ ] Link privacy policy
- [ ] Set pricing (Free)

## 7. Quality Checks

### Pre-Launch Report
- [ ] Review Google's pre-launch report
- [ ] Fix any critical issues found
- [ ] Address performance warnings

### Policy Review
- [ ] Ensure compliance with Google Play policies
- [ ] No prohibited content
- [ ] Proper permissions usage
- [ ] Data handling transparency

## 8. Launch Preparation

### Rollout Strategy
- [ ] Consider staged rollout (start with 20% of users)
- [ ] Monitor crash reports and reviews
- [ ] Have update plan ready if issues found

### Post-Launch
- [ ] Monitor app performance in Play Console
- [ ] Respond to user reviews
- [ ] Track download/usage metrics
- [ ] Plan future updates

## 9. File Structure Check

```
app/
├── build.gradle.kts ✓ (release config)
├── proguard-rules.pro ✓ (if using ProGuard)
└── src/main/
    ├── AndroidManifest.xml ✓ (proper permissions)
    └── res/
        └── mipmap-xxxhdpi/
            └── ic_launcher.png ✓ (512x512 icon)

keystore/
└── howmanyhours-release.keystore ✓ (secure storage)

play-store-assets/ ✓ (this documentation folder)
├── legal/
├── screenshots/  
├── descriptions/
└── technical-docs/
```

## 10. Common Issues to Avoid

### Technical Issues
- [ ] Target SDK version too low
- [ ] Missing required permissions declarations
- [ ] Unsigned or debug-signed release
- [ ] Large APK size without reason

### Policy Issues  
- [ ] Missing privacy policy
- [ ] Inaccurate data safety declarations
- [ ] Inappropriate content rating
- [ ] Misleading app description

### Store Listing Issues
- [ ] Low-quality screenshots
- [ ] Generic app description
- [ ] Missing feature graphic
- [ ] Inappropriate category selection

---

**Estimated Timeline**: 1-2 weeks for first-time preparation, then 2-3 days for Google's review process.