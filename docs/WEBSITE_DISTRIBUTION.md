# Website Distribution Guide

This guide explains how to distribute HowManyHours via your own website instead of (or in addition to) app stores.

## Benefits of Website Distribution

- **Full Control**: No store approval process or delays
- **No Fees**: Unlike Play Store's 15-30% commission
- **Direct Updates**: Push updates on your schedule
- **Privacy**: No mandatory analytics or tracking
- **Flexibility**: Distribute beta versions easily

## Prerequisites

- Web hosting with HTTPS (required for downloads)
- Domain name (already have: luisico.net)
- FTP/SSH access to upload files
- Basic HTML knowledge (for download page)

## Quick Start

### 1. Build Release APK

```bash
./scripts/release.sh 1.0.0
```

This creates: `app/build/outputs/apk/release/app-release.apk`

### 2. Rename APK for Distribution

```bash
cp app/build/outputs/apk/release/app-release.apk \
   ~/Downloads/howmanyhours-v1.0.0.apk
```

Use semantic versioning in filename for clarity.

### 3. Upload to Your Website

Upload to a predictable URL structure:

```
https://luisico.net/apps/howmanyhours/
├── howmanyhours-latest.apk      # Always points to latest
├── howmanyhours-v1.0.0.apk      # Specific version
├── versions/
│   ├── howmanyhours-v1.0.0.apk
│   ├── howmanyhours-v1.0.1.apk
│   └── ...
└── index.html                    # Download page
```

### 4. Create Download Page

Basic example (`index.html`):

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>HowManyHours - Simple Time Tracking</title>
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            max-width: 800px;
            margin: 50px auto;
            padding: 20px;
            line-height: 1.6;
        }
        .download-btn {
            display: inline-block;
            background: #6750A4;
            color: white;
            padding: 15px 30px;
            text-decoration: none;
            border-radius: 8px;
            font-size: 18px;
            margin: 20px 0;
        }
        .download-btn:hover {
            background: #5640A0;
        }
        .version { color: #666; }
        .install-steps { background: #f5f5f5; padding: 20px; border-radius: 8px; }
    </style>
</head>
<body>
    <h1>HowManyHours</h1>
    <p>Simple, privacy-focused time tracking for Android</p>

    <a href="howmanyhours-latest.apk" class="download-btn">
        Download Latest Version
    </a>
    <p class="version">Version 1.0.0 • Released Jan 2026 • 5.2 MB</p>

    <div class="install-steps">
        <h2>Installation Instructions</h2>
        <ol>
            <li>Download the APK file above</li>
            <li>Open the downloaded file on your Android device</li>
            <li>If prompted, enable "Install unknown apps" for your browser</li>
            <li>Tap "Install" and you're ready to go!</li>
        </ol>
        <p><strong>Note:</strong> Your device may show a security warning.
        This is normal for apps not from the Play Store. The app is safe.</p>
    </div>

    <h2>Features</h2>
    <ul>
        <li>Track time by project with simple start/stop</li>
        <li>View monthly hours and export to CSV</li>
        <li>100% offline - no cloud sync required</li>
        <li>No ads, no tracking, open source (MIT license)</li>
    </ul>

    <h2>Requirements</h2>
    <p>Android 9.0 (API 28) or higher</p>

    <h2>Privacy</h2>
    <p>All data stays on your device. No accounts, no servers, no tracking.</p>

    <h2>Previous Versions</h2>
    <ul>
        <li><a href="versions/howmanyhours-v1.0.0.apk">v1.0.0</a> (Jan 2026)</li>
    </ul>

    <footer>
        <p>
            <a href="https://github.com/luisico/howmanyhours">Source Code</a> •
            <a href="privacy-policy.html">Privacy Policy</a> •
            <a href="mailto:your-email@example.com">Support</a>
        </p>
    </footer>
</body>
</html>
```

## Distribution Checklist

### Before First Release

- [ ] Build signed release APK with `./scripts/release.sh`
- [ ] Test APK on multiple devices/Android versions
- [ ] Create download page (use template above)
- [ ] Write privacy policy (required, see play-store-assets/legal/privacy-policy.md)
- [ ] Set up HTTPS (most hosts provide free Let's Encrypt)
- [ ] Test download flow from mobile browser
- [ ] Prepare update mechanism (see below)

### For Each Release

- [ ] Update version in code: `./scripts/release.sh X.Y.Z`
- [ ] Run tests: `./gradlew test`
- [ ] Build signed APK (done by release.sh)
- [ ] Test on physical devices
- [ ] Rename APK with version number
- [ ] Upload new APK to website
- [ ] Update `howmanyhours-latest.apk` symlink/copy
- [ ] Update download page with new version number and changelog
- [ ] Announce release (email, social media, etc.)

## Update Notifications

Since there's no Play Store auto-updates, consider:

### Option 1: In-App Update Check (Future Enhancement)

Add code to check for updates:

```kotlin
// Check latest version from your server
val latestVersion = fetchLatestVersion("https://luisico.net/apps/howmanyhours/version.json")
if (latestVersion > BuildConfig.VERSION_NAME) {
    // Show update dialog with download link
}
```

Create `version.json`:
```json
{
  "latest_version": "1.0.1",
  "download_url": "https://luisico.net/apps/howmanyhours/howmanyhours-latest.apk",
  "release_notes": "Bug fixes and performance improvements",
  "min_supported_version": "1.0.0"
}
```

### Option 2: Email Newsletter

Maintain an email list for update notifications.

### Option 3: RSS Feed

Create an RSS feed for users who want automatic notifications.

## Security Considerations

### APK Signing

- **Always use the same keystore** for updates
- Users cannot update if signature changes (must uninstall first)
- See SIGNING.md for keystore management

### HTTPS Required

- Android requires HTTPS for APK downloads
- Use Let's Encrypt for free SSL certificates
- Test with: `curl -I https://luisico.net/apps/howmanyhours/howmanyhours-latest.apk`

### Checksum/Verification

Provide SHA-256 checksums for security-conscious users:

```bash
# Generate checksum
sha256sum howmanyhours-v1.0.0.apk > howmanyhours-v1.0.0.apk.sha256

# Display on website
echo "SHA-256: $(cat howmanyhours-v1.0.0.apk.sha256)"
```

Add to download page:
```html
<p><small>SHA-256: abc123...</small></p>
```

## File Hosting Options

### Your Own Server

**Pros**: Full control, no costs
**Cons**: Bandwidth limits, need to maintain

### GitHub Releases

**Pros**: Free, reliable, versioned
**Cons**: Not a traditional download page

```bash
# Create GitHub release
gh release create v1.0.0 \
  app/build/outputs/apk/release/app-release.apk \
  --title "v1.0.0" \
  --notes "Initial release"
```

Then link from your website:
```html
<a href="https://github.com/luisico/howmanyhours/releases/latest/download/app-release.apk">
    Download Latest
</a>
```

### CDN (e.g., Cloudflare)

**Pros**: Fast, free tier available, global distribution
**Cons**: Additional setup

## Analytics (Optional)

Track downloads without violating privacy:

### Server-Side Logs

Apache/Nginx logs show download counts:

```bash
grep "howmanyhours.*\.apk" /var/log/apache2/access.log | wc -l
```

### Simple Counter

Add a PHP counter (doesn't track users):

```php
<?php
// download-counter.php
$count_file = 'download_count.txt';
$count = (int)file_get_contents($count_file) + 1;
file_put_contents($count_file, $count);

header('Location: howmanyhours-latest.apk');
?>
```

Link to counter instead of direct APK:
```html
<a href="download-counter.php">Download</a>
```

## Legal Requirements

### Must Have

1. **Privacy Policy**: Even if you don't collect data, state this explicitly
   - Use template in play-store-assets/legal/privacy-policy.md
   - Host at https://luisico.net/apps/howmanyhours/privacy-policy.html

2. **Terms of Service** (optional but recommended):
   - Liability disclaimer
   - MIT license reference
   - Support policy

### Nice to Have

1. **FAQ Page**: Common installation/usage questions
2. **Changelog**: Version history
3. **Screenshots**: Show app features
4. **Contact Info**: Support email or GitHub issues

## SEO & Discovery

### App Name in URLs

Use descriptive URLs:
- ✅ `https://luisico.net/apps/howmanyhours`
- ❌ `https://luisico.net/app123`

### Meta Tags

```html
<meta name="description" content="Free, privacy-focused time tracking app for Android. Track project hours offline, export to CSV. No ads, no cloud.">
<meta property="og:title" content="HowManyHours - Time Tracking">
<meta property="og:image" content="screenshot.png">
```

### Submit to Alternative Stores

Consider listing on:
- F-Droid (if open source)
- APKMirror
- APKPure
- Amazon Appstore

## Bandwidth Estimation

Assume:
- APK size: ~5 MB
- 100 downloads/month
- Total: 500 MB/month

Most web hosts can handle this easily.

## Automation

### Auto-Deploy Script

```bash
#!/bin/bash
# deploy.sh

VERSION=$1
APK_PATH="app/build/outputs/apk/release/app-release.apk"
REMOTE="user@luisico.net:/var/www/apps/howmanyhours/"

# Build
./scripts/release.sh $VERSION

# Rename
cp $APK_PATH howmanyhours-v$VERSION.apk

# Upload
scp howmanyhours-v$VERSION.apk $REMOTE/versions/
scp howmanyhours-v$VERSION.apk $REMOTE/howmanyhours-latest.apk

# Update version.json
echo "{\"latest_version\": \"$VERSION\"}" > version.json
scp version.json $REMOTE/

echo "Deployed v$VERSION to website"
```

## Troubleshooting

### "App not installed" Error

- **Cause**: Signature mismatch
- **Fix**: Uninstall old version first, or use same keystore

### "Unknown source" Blocked

- **Cause**: Android security settings
- **Fix**: Enable "Install unknown apps" for browser in Settings

### APK Not Downloading

- **Cause**: Wrong MIME type
- **Fix**: Add to `.htaccess`:
  ```
  AddType application/vnd.android.package-archive .apk
  ```

## Next Steps

1. Set up basic download page
2. Test with beta users
3. Collect feedback
4. Consider Play Store for broader reach
5. Implement in-app update checks (optional)

## Resources

- [Android App Links](https://developer.android.com/training/app-links)
- [F-Droid Submission](https://f-droid.org/docs/Inclusion_Policy/)
- [Let's Encrypt](https://letsencrypt.org/)
