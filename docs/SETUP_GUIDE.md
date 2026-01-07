# Development Setup Guide (All Platforms)

This guide covers setting up the HowManyHours development environment on different operating systems.

## Quick Links

- [Linux (Ubuntu/Debian)](#linux-ubuntudebian)
- [Linux (Fedora)](#linux-fedora-recommended)
- [macOS](#macos)
- [Windows](#windows)

## Common Requirements (All Platforms)

- **JDK 21** or higher
- **Android SDK** with:
  - Platform SDK 34
  - Build Tools 34.0.0
  - Platform Tools
- **Git**
- **At least 8GB RAM** recommended
- **10GB free disk space** for Android SDK and build artifacts

---

## Linux (Fedora) [Recommended]

### Automated Setup

```bash
chmod +x scripts/setup.sh
./scripts/setup.sh
```

The setup script will:
- Install Java 21
- Download and configure Android SDK
- Set up environment variables
- Create helper scripts
- Run initial build test

### Manual Setup

See detailed instructions in [README.md](README.md#development-setup).

---

## Linux (Ubuntu/Debian)

### 1. Install JDK 21

```bash
sudo apt update
sudo apt install -y openjdk-21-jdk openjdk-21-jre

# Verify
java -version
javac -version
```

### 2. Set JAVA_HOME

Add to `~/.bashrc` or `~/.zshrc`:

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH=$PATH:$JAVA_HOME/bin
```

Reload:
```bash
source ~/.bashrc
```

### 3. Install Android SDK

```bash
# Install required tools
sudo apt install -y wget unzip

# Create SDK directory
mkdir -p ~/Android/Sdk/cmdline-tools
cd ~/Android/Sdk/cmdline-tools

# Download command line tools
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip commandlinetools-linux-11076708_latest.zip
mv cmdline-tools latest
rm commandlinetools-linux-11076708_latest.zip
```

### 4. Set Android Environment Variables

Add to `~/.bashrc`:

```bash
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin
export PATH=$PATH:$ANDROID_HOME/platform-tools
export PATH=$PATH:$ANDROID_HOME/emulator
```

Reload:
```bash
source ~/.bashrc
```

### 5. Install SDK Packages

```bash
# Accept licenses
sdkmanager --licenses

# Install required packages
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0" "emulator"
```

### 6. Build the Project

```bash
cd /path/to/howmanyhours
chmod +x gradlew
./gradlew build
```

---

## macOS

### 1. Install Homebrew (if not already installed)

```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

### 2. Install JDK 21

```bash
# Using Homebrew
brew install openjdk@21

# Link it
sudo ln -sfn $(brew --prefix)/opt/openjdk@21/libexec/openjdk.jdk \
  /Library/Java/JavaVirtualMachines/openjdk-21.jdk

# Verify
java -version
```

### 3. Set JAVA_HOME

Add to `~/.zshrc` or `~/.bash_profile`:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH=$JAVA_HOME/bin:$PATH
```

Reload:
```bash
source ~/.zshrc
```

### 4. Install Android SDK

**Option A: Using Android Studio (Recommended)**

1. Download Android Studio from https://developer.android.com/studio
2. Install and open Android Studio
3. Go to Settings → Appearance & Behavior → System Settings → Android SDK
4. Install SDK Platform 34 and Build Tools 34.0.0

**Option B: Command Line Tools**

```bash
# Create SDK directory
mkdir -p ~/Library/Android/sdk/cmdline-tools
cd ~/Library/Android/sdk/cmdline-tools

# Download command line tools
curl -o commandlinetools.zip \
  https://dl.google.com/android/repository/commandlinetools-mac-11076708_latest.zip
unzip commandlinetools.zip
mv cmdline-tools latest
rm commandlinetools.zip
```

### 5. Set Android Environment Variables

Add to `~/.zshrc`:

```bash
export ANDROID_HOME=$HOME/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin
export PATH=$PATH:$ANDROID_HOME/platform-tools
export PATH=$PATH:$ANDROID_HOME/emulator
```

Reload:
```bash
source ~/.zshrc
```

### 6. Install SDK Packages

```bash
sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0" "emulator"
```

### 7. Build the Project

```bash
cd /path/to/howmanyhours
chmod +x gradlew
./gradlew build
```

---

## Windows

### 1. Install JDK 21

**Download and Install:**
1. Go to https://adoptium.net/
2. Download JDK 21 for Windows
3. Run installer (use defaults)

**Set JAVA_HOME:**
1. Open "Environment Variables" (System Properties → Advanced)
2. Add new System variable:
   - Name: `JAVA_HOME`
   - Value: `C:\Program Files\Eclipse Adoptium\jdk-21.0.x.y-hotspot`
3. Edit `Path` variable, add:
   - `%JAVA_HOME%\bin`

**Verify:**
```cmd
java -version
javac -version
```

### 2. Install Android SDK

**Option A: Android Studio (Recommended)**

1. Download from https://developer.android.com/studio
2. Install with default settings
3. SDK installs to: `C:\Users\YourName\AppData\Local\Android\Sdk`

**Option B: Command Line Tools**

1. Download from: https://developer.android.com/studio#command-tools
2. Extract to: `C:\Android\Sdk\cmdline-tools\latest`

### 3. Set Android Environment Variables

Add System variables:
- `ANDROID_HOME` = `C:\Users\YourName\AppData\Local\Android\Sdk`

Edit `Path`, add:
- `%ANDROID_HOME%\cmdline-tools\latest\bin`
- `%ANDROID_HOME%\platform-tools`
- `%ANDROID_HOME%\emulator`

### 4. Install SDK Packages

Open PowerShell or Command Prompt:

```cmd
sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0" "emulator"
```

### 5. Build the Project

```cmd
cd C:\path\to\howmanyhours
gradlew.bat build
```

### 6. Development Scripts

On Windows, use `.bat` alternatives or Git Bash:

```bash
# Using Git Bash (recommended)
./scripts/build.sh

# Or create Windows batch files
gradlew.bat assembleDebug
```

---

## IDE Setup

### VS Code / Cursor (All Platforms)

**Install Extensions:**
- Kotlin Language (fwcd.kotlin)
- Extension Pack for Java (vscjava.vscode-java-pack)
- Gradle for Java (vscjava.vscode-gradle)

**Open Project:**
```bash
code .
```

VS Code will detect the Gradle project and configure automatically.

**Recommended Settings:** (Already in `.vscode/settings.json`)
- Java auto-build enabled
- Gradle auto-sync enabled

### Android Studio (All Platforms)

1. Open Android Studio
2. Click "Open an Existing Project"
3. Select the `howmanyhours` directory
4. Wait for Gradle sync
5. Click Run (green play button) or Shift+F10

---

## Emulator Setup (All Platforms)

### Create AVD (Android Virtual Device)

Using command line:

```bash
# List available system images
sdkmanager --list | grep system-images

# Download system image (if needed)
sdkmanager "system-images;android-34;google_apis;x86_64"

# Create AVD
avdmanager create avd \
  -n Pixel_6_API_34 \
  -k "system-images;android-34;google_apis;x86_64" \
  -d "pixel_6"

# Start emulator
emulator -avd Pixel_6_API_34
```

Or use the automated script (Linux/macOS):
```bash
./scripts/setup-emulator.sh
```

### Physical Device (All Platforms)

1. Enable Developer Options on your device:
   - Go to Settings → About phone
   - Tap "Build number" 7 times

2. Enable USB Debugging:
   - Go to Settings → Developer options
   - Enable "USB debugging"

3. Connect via USB

4. Verify connection:
   ```bash
   adb devices
   ```

---

## Building & Running

### Build Commands (All Platforms)

```bash
# Debug build
./gradlew assembleDebug

# Release build (requires keystore)
./gradlew assembleRelease

# Run tests
./gradlew test

# Clean build
./gradlew clean build
```

### Install on Device

```bash
# Using helper script (Linux/macOS)
./scripts/install.sh

# Using Gradle
./gradlew installDebug

# Using ADB directly
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### On Windows

```cmd
gradlew.bat assembleDebug
gradlew.bat installDebug
```

---

## Troubleshooting

### "JAVA_HOME not set"

**Linux/macOS:**
```bash
echo 'export JAVA_HOME=/path/to/jdk' >> ~/.bashrc
source ~/.bashrc
```

**Windows:**
Set via System Environment Variables (see Windows setup above).

### "sdkmanager: command not found"

Android SDK cmdline-tools not in PATH. Add to PATH (see platform-specific instructions above).

### "Gradle sync failed"

1. Check internet connection (Gradle downloads dependencies)
2. Run: `./gradlew --refresh-dependencies`
3. Check JDK version: `java -version` (should be 21+)

### "Unable to locate adb"

Platform tools not installed:
```bash
sdkmanager "platform-tools"
```

### On macOS: "Cannot be opened because developer cannot be verified"

```bash
xattr -d com.apple.quarantine /path/to/file
```

### Windows: "execution of scripts is disabled"

Run PowerShell as Administrator:
```powershell
Set-ExecutionPolicy RemoteSigned
```

---

## Next Steps

1. Build the project: `./gradlew build`
2. Start emulator or connect device
3. Install app: `./scripts/install.sh` (or platform equivalent)
4. Make changes and test
5. See [CONTRIBUTING.md](CONTRIBUTING.md) for development workflow

## Platform-Specific Resources

- **Linux**: https://developer.android.com/studio/install#linux
- **macOS**: https://developer.android.com/studio/install#mac
- **Windows**: https://developer.android.com/studio/install#windows
- **Gradle**: https://docs.gradle.org/current/userguide/installation.html
