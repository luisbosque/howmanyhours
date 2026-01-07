# App Screenshots Guide

This folder contains screenshots and graphics for app distribution (Play Store, website, etc.).

## Folder Structure

```
screenshots/
├── phone/              # Phone screenshots (required)
├── tablet/             # Tablet screenshots (optional)
├── feature-graphic/    # Feature graphic (1024x500)
└── README.md          # This file
```

## Requirements

### Phone Screenshots (REQUIRED)

**Specifications:**
- **Minimum**: 2 screenshots
- **Maximum**: 8 screenshots
- **Format**: PNG or JPG
- **Dimensions**:
  - Minimum: 320px on short side
  - Maximum: 3840px on short side
  - Aspect ratio: 16:9 or 9:16
- **Recommended**: 1080x1920 (portrait) or 1920x1080 (landscape)

**File naming:**
- `01-main-screen.png`
- `02-time-tracking.png`
- `03-project-list.png`
- `04-settings.png`
- etc.

**What to Screenshot:**

1. **Main Screen** - Show project list and active timer
2. **Time Tracking** - Show running timer with time accumulation
3. **Project Detail** - Show monthly hours and time breakdown
4. **Settings** - Show export and backup features
5. **Period History** (optional) - Show historical data view

### Tablet Screenshots (Optional)

Same requirements as phone, but showcasing tablet-optimized layout.

### Feature Graphic (For Play Store/Website Header)

**Specifications:**
- **Dimensions**: 1024×500 pixels
- **Format**: PNG or JPG
- **No transparency**
- **File**: `feature-graphic/feature-graphic.png`

**Should Include:**
- App name: "HowManyHours"
- Tagline: "Simple Time Tracking"
- Visual element (clock icon, etc.)
- Keep text readable when scaled down

## Capturing Screenshots

### Method 1: Physical Device (Recommended)

1. Install app on your device:
   ```bash
   ./scripts/install.sh
   ```

2. Set up demo data:
   - Create 3-4 projects with different names
   - Add some completed time entries
   - Start one timer

3. Take screenshots:
   - **Android**: Power + Volume Down
   - Screenshots saved to: `Pictures/Screenshots/`

4. Transfer to computer:
   ```bash
   adb pull /sdcard/Pictures/Screenshots/ ./screenshots-temp/
   ```

5. Copy to project:
   ```bash
   cp screenshots-temp/Screenshot_*.png \
      play-store-assets/screenshots/phone/
   ```

### Method 2: Emulator

1. Start emulator:
   ```bash
   ./scripts/start-emulator.sh
   ```

2. Install app:
   ```bash
   ./scripts/install.sh
   ```

3. Take screenshots:
   - Click camera icon in emulator toolbar
   - Or: Ctrl+S (Cmd+S on Mac)

4. Screenshots saved to:
   - **Linux/Mac**: `~/Pictures/` or `~/Desktop/`
   - **Windows**: `C:\Users\YourName\Pictures\`

### Method 3: Using `screencap` Command

```bash
# Take screenshot via adb
adb shell screencap -p /sdcard/screenshot.png
adb pull /sdcard/screenshot.png ./
adb shell rm /sdcard/screenshot.png
```

## Editing Screenshots

### Recommended Tools

- **GIMP** (Free, cross-platform): https://www.gimp.org/
- **Figma** (Free, browser-based): https://www.figma.com/
- **Photopea** (Free, browser-based): https://www.photopea.com/

### Device Frames (Optional)

Add device frames to make screenshots more professional:

**Online Tools:**
- https://mockuphone.com/
- https://deviceframes.com/
- https://screenshots.pro/

**Local Tools:**
- Android Studio Device Art Generator

### Editing Checklist

- [ ] Crop to remove status bar (or keep for context)
- [ ] Ensure consistent aspect ratio across all screenshots
- [ ] Use demo data (no real project names/times)
- [ ] Check for sensitive information
- [ ] Optimize file size (use PNG compression)
- [ ] Test readability at thumbnail size

## Creating Feature Graphic

### Tools

- **Canva** (Free templates): https://www.canva.com/
- **Figma** (Design tool): https://www.figma.com/
- **GIMP/Photoshop** (Manual design)

### Template Ideas

**Option 1: Simple Text + Icon**
```
[Clock Icon]  How Many Hours
              Simple Time Tracking
```

**Option 2: Screenshot Showcase**
```
[3 overlapping phone screenshots]  How Many Hours
                                    Track Your Time, Effortlessly
```

**Option 3: Feature Highlights**
```
How Many Hours
✓ Project Tracking  ✓ CSV Export  ✓ Offline & Private
```

### Design Guidelines

- Use app's primary color: #6750A4 (purple)
- Ensure text is readable at small sizes
- Keep it simple and clean
- Avoid too much text
- Test how it looks when scaled down

## Example Screenshot Setup

### Demo Data to Use

Create these sample projects:
1. **"Client Website"** - 15.5 hours this month
2. **"Mobile App"** - 8.2 hours this month
3. **"Research"** - 3.0 hours this month
4. **"Admin Tasks"** - 1.5 hours this month

Start timer on "Client Website" showing "2:34:00" running time.

### Screenshot Sequence

1. **Main Screen**: Show project list with one active timer
2. **Running Timer**: Focus on the big time display
3. **Project Detail**: Show monthly breakdown and entries
4. **Export**: Show CSV export or settings screen
5. **Backup**: Show backup/restore screen (if implemented)

## Optimization

Before adding to repository:

```bash
# Optimize PNG files (requires optipng)
optipng -o7 play-store-assets/screenshots/phone/*.png

# Or use pngquant for better compression
pngquant --quality=65-80 play-store-assets/screenshots/phone/*.png
```

## Usage

### For Play Store

Upload screenshots in Play Console:
1. Go to Play Console → Your App → Store Presence → Main store listing
2. Upload phone screenshots (2-8 required)
3. Upload feature graphic (required)
4. Upload tablet screenshots (optional)

### For Website

Use optimized versions:

```html
<img src="screenshots/phone/01-main-screen.png"
     alt="HowManyHours main screen"
     width="300">
```

### For README

Include in project README:

```markdown
## Screenshots

![Main Screen](play-store-assets/screenshots/phone/01-main-screen.png)
![Time Tracking](play-store-assets/screenshots/phone/02-time-tracking.png)
```

## TODO

When ready to create screenshots:

- [ ] Install app on device with demo data
- [ ] Capture 4-6 screenshots showcasing main features
- [ ] Edit and crop screenshots to standard size
- [ ] Create feature graphic (1024x500)
- [ ] Optimize file sizes
- [ ] Add to this folder
- [ ] Update README.md with actual screenshots

## Privacy Note

Never include real user data in screenshots! Always use:
- Generic project names ("Client Work", "Project A")
- Rounded time values (15.0 hours, not 15.347)
- No personally identifiable information
- No real client/company names

## Resources

- [Play Store Screenshot Guidelines](https://support.google.com/googleplay/android-developer/answer/9866151)
- [Material Design Screenshot Best Practices](https://material.io/design/communication/imagery.html)
- [F-Droid Screenshot Guidelines](https://f-droid.org/docs/All_About_Descriptions_Graphics_and_Screenshots/)
