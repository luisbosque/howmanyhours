# How to Fix Icon Not Showing

The icons are correctly placed in `app/src/main/res/mipmap-*/`. The problem is that Android **caches launcher icons** aggressively. Simply rebuilding and reinstalling won't update the icon.

## Solution: Complete Uninstall and Reinstall

```bash
# 1. Uninstall the app completely (removes all cached icons)
adb uninstall net.luisico.howmanyhours

# 2. Clean build
./gradlew clean

# 3. Build fresh APK
./gradlew assembleRelease

# 4. Install
adb install app/build/outputs/apk/release/app-release.apk
```

Or use the provided script:
```bash
/tmp/reinstall_app.sh
```

## Why This Happens

Android launcher caches icons for performance. When you:
- Reinstall with `-r` flag: Old icon cache is kept
- Install over existing app: Icon cache not refreshed
- Uninstall completely: Icon cache is cleared

## After Reinstall

The new icon should appear immediately. If it still doesn't:

1. **Restart the launcher**:
   ```bash
   adb shell am force-stop <launcher_package>
   # Common launchers:
   # - Pixel Launcher: com.google.android.apps.nexuslauncher
   # - Samsung: com.sec.android.app.launcher
   ```

2. **Reboot the device**:
   ```bash
   adb reboot
   ```

3. **Clear launcher data** (last resort):
   ```bash
   adb shell pm clear <launcher_package>
   ```

## Verify Icons Are Correct

Check the icons files exist:
```bash
ls -la app/src/main/res/mipmap-*/ic_launcher*.png
```

All these should exist:
- `mipmap-mdpi/ic_launcher.png`
- `mipmap-hdpi/ic_launcher.png`
- `mipmap-xhdpi/ic_launcher.png`
- `mipmap-xxhdpi/ic_launcher.png`
- `mipmap-xxxhdpi/ic_launcher.png`

And adaptive icon parts:
- `ic_launcher_foreground.png` (in each density)
- `ic_launcher_background.png` (in each density)
