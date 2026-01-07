# App Icon Generation Guide

The app icon infrastructure is set up with adaptive icons for Android 8.0+ (API 26+) and backward compatibility for older versions. You need to generate PNG versions of the icon at various densities.

## Current Status

- ✅ Adaptive icon structure created (foreground + background layers)
- ✅ Vector drawables for adaptive icons
- ⚠️ **ACTION REQUIRED**: Generate PNG files for all densities

## Required PNG Files

You need to generate PNG icons at these densities:

| Density | Folder | Size | File Names |
|---------|--------|------|------------|
| mdpi | mipmap-mdpi | 48×48px | ic_launcher.png, ic_launcher_round.png |
| hdpi | mipmap-hdpi | 72×72px | ic_launcher.png, ic_launcher_round.png |
| xhdpi | mipmap-xhdpi | 96×96px | ic_launcher.png, ic_launcher_round.png |
| xxhdpi | mipmap-xxhdpi | 144×144px | ic_launcher.png, ic_launcher_round.png |
| xxxhdpi | mipmap-xxxhdpi | 192×192px | ic_launcher.png, ic_launcher_round.png |
| Play Store | play-store-assets/ | 512×512px | ic_launcher_playstore.png |

## Option 1: Using Android Asset Studio (Recommended)

**Online Tool**: https://icon.kitchen/

1. **Visit Icon Kitchen** (or https://romannurik.github.io/AndroidAssetStudio/icons-launcher.html)
2. **Upload Your Icon Design** or use the existing design:
   - The app currently uses a clock icon with time tracking dots
   - Colors: Primary #6750A4 (purple), Accent #4CAF50 (green)
3. **Configure**:
   - Icon type: Launcher Icons
   - Asset Type: Image or Clipart
   - Background: #6750A4 (or use adaptive icon with separate layers)
   - Foreground: White/transparent clock design
4. **Download**:
   - Download the generated ZIP file
   - Extract directly into `app/src/main/res/` (it will create all mipmap folders)

## Option 2: Using Android Studio

1. **Open Project in Android Studio**
2. **Right-click** on `res` folder → New → Image Asset
3. **Configure**:
   - Icon Type: Launcher Icons (Adaptive and Legacy)
   - Name: ic_launcher
   - Foreground Layer: Use `ic_launcher_foreground.xml` (already created)
   - Background Layer: Use `ic_launcher_background.xml` (already created)
4. **Click Next** → **Finish**

This will generate all required PNG files automatically.

## Option 3: Manual Creation with ImageMagick

If you have ImageMagick installed and want to use a single source image:

```bash
# Create a 512x512 source image first (icon.png)
# Then run these commands from the project root:

convert icon.png -resize 48x48 app/src/main/res/mipmap-mdpi/ic_launcher.png
convert icon.png -resize 72x72 app/src/main/res/mipmap-hdpi/ic_launcher.png
convert icon.png -resize 96x96 app/src/main/res/mipmap-xhdpi/ic_launcher.png
convert icon.png -resize 144x144 app/src/main/res/mipmap-xxhdpi/ic_launcher.png
convert icon.png -resize 192x192 app/src/main/res/mipmap-xxxhdpi/ic_launcher.png
cp icon.png play-store-assets/ic_launcher_playstore.png

# For round icons (same sizes)
convert icon.png -resize 48x48 app/src/main/res/mipmap-mdpi/ic_launcher_round.png
convert icon.png -resize 72x72 app/src/main/res/mipmap-hdpi/ic_launcher_round.png
convert icon.png -resize 96x96 app/src/main/res/mipmap-xhdpi/ic_launcher_round.png
convert icon.png -resize 144x144 app/src/main/res/mipmap-xxhdpi/ic_launcher_round.png
convert icon.png -resize 192x192 app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png
```

## Option 4: Using the Vector Design Directly

The app already has vector drawable icons defined in:
- `app/src/main/res/drawable/ic_launcher_foreground.xml`
- `app/src/main/res/drawable/ic_launcher_background.xml`

You can convert these to PNG using any of the above tools.

## Current Icon Design

The icon shows:
- **Purple background** (#6750A4) - Material Design 3 primary color
- **White clock face** with purple clock hands showing 3:00
- **Green dots** (#4CAF50) at the bottom representing time tracking entries
- Clean, professional design suitable for a productivity app

## Quick Start (Recommended)

1. Visit https://icon.kitchen/
2. Use "Icon" → "Clipart" → Search for "clock" or "time"
3. Set background color to #6750A4
4. Set foreground color to white
5. Add customization as needed
6. Download ZIP
7. Extract to `app/src/main/res/`
8. Verify all mipmap folders have ic_launcher.png and ic_launcher_round.png

## Verification

After generating the icons, verify they're in place:

```bash
find app/src/main/res/mipmap-* -name "*.png"
```

You should see 10 PNG files (ic_launcher.png and ic_launcher_round.png in each of 5 density folders).

## Build & Test

```bash
./gradlew assembleDebug
./install.sh
```

The launcher icon should now display properly on all Android versions and device densities.

## For Play Store Release

Don't forget to also create:
- **Feature Graphic**: 1024×500px (goes in play-store-assets/)
- **Screenshots**: Various sizes (see play-store-assets/screenshots/)
- **High-res icon**: 512×512px PNG (no transparency, no rounded corners)
