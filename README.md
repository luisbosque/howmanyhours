# How Many Hours - Android Time Tracking App

A simple, clean Android app for tracking time spent on projects.

> **How the core project has been built:** This project was designed and directed by a human but largely implemented through vibe coding. The feature set and product direction came from me. If you're an experienced Android developer and some patterns look unconventional, that's probably why.
>
> **Early stage:** I've been using this app daily and it works well for my needs, but it hasn't been battle-tested by a wider audience yet. If you run into rough edges, [open an issue](https://github.com/luisbosque/howmanyhours/issues) - bug reports, feature ideas, and contributions are all welcome.

## Features

- Create and delete projects
- One button start/stop time tracking
- Named tracked intervals
- View tracking history
- Export time data to CSV
- Back up to csv
- Montly and manual-checkin tracking periods
- Fully offline and no ads

## Development Setup

### VS Code/Cursor Development on Fedora 42

This project is optimized for Fedora 42 with VS Code or Cursor.

#### Prerequisites
1. **Java Development Kit (JDK) 21**
   ```bash
   # Install Java 21 with required modules (recommended) or Java 25
   sudo dnf install java-21-openjdk-devel java-21-openjdk-jmods
   
   # Verify installation
   java -version
   javac -version
   
   # Set JAVA_HOME (add to ~/.bashrc)
   export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
   ```

2. **Android SDK Command Line Tools**
   ```bash
   # Create Android SDK directory
   mkdir -p ~/Android/Sdk/cmdline-tools
   
   # Download and extract command line tools
   cd ~/Android/Sdk/cmdline-tools
   wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
   unzip commandlinetools-linux-11076708_latest.zip
   mv cmdline-tools latest
   rm commandlinetools-linux-11076708_latest.zip
   
   # Add to ~/.bashrc
   export ANDROID_HOME=$HOME/Android/Sdk
   export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin
   export PATH=$PATH:$ANDROID_HOME/platform-tools
   export PATH=$PATH:$ANDROID_HOME/emulator
   
   # Reload shell
   source ~/.bashrc
   ```

3. **Install Android SDK packages**
   ```bash
   # Accept licenses
   sdkmanager --licenses
   
   # Install required SDK packages
   sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0" "emulator"
   ```

#### Quick Setup Script
Run the setup script to install everything automatically:

```bash
chmod +x scripts/setup.sh
./scripts/setup.sh
```

#### VS Code Extensions
Install these recommended extensions:
- **Kotlin Language** (by fwcd)
- **Extension Pack for Java** (by Microsoft)
- **Android iOS Emulator** (by DiemasMichiels)
- **Gradle for Java** (by Microsoft)

#### Development Commands
```bash
# Build the project
./scripts/build.sh

# Install on device/emulator (checks for connected devices)
./scripts/install.sh

# Clean the project
./scripts/clean.sh

# Direct Gradle commands
./gradlew build           # Build project
./gradlew test            # Run tests
./gradlew assembleDebug   # Build APK
```

#### Android Emulator Setup
If you don't have a physical Android device, set up an emulator:

```bash
# Set up Android emulator (one-time setup)
./scripts/setup-emulator.sh

# Start the emulator
./scripts/start-emulator.sh

# Check emulator/device status
./scripts/check-emulator.sh

# Stop the emulator
./scripts/stop-emulator.sh

# Install app (will check for connected devices)
./scripts/install.sh
```

## Installation

### Installing on Physical Android Device

To install the app directly on your Android phone:

#### Method 1: USB Installation (Recommended for Development)

1. **Enable Developer Options on your phone**:
   - Go to Settings > About phone
   - Tap "Build number" 7 times until you see "You are now a developer!"

2. **Enable USB Debugging**:
   - Go to Settings > Developer options
   - Enable "USB debugging"
   - Enable "Install via USB" (if available)

3. **Connect your phone to computer**:
   ```bash
   # Connect phone via USB cable
   # Allow USB debugging when prompted on phone
   
   # Check if device is detected
   ~/Android/Sdk/platform-tools/adb devices
   ```

4. **Build and install the app**:
   ```bash
   # Build the APK
   ./gradlew assembleDebug
   
   # Install directly to phone
   ~/Android/Sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
   
   # Or use the convenience script
   ./install.sh
   ```

#### Method 2: APK File Transfer

1. **Build the APK**:
   ```bash
   ./gradlew assembleDebug
   ```

2. **Transfer APK to phone**:
   - Copy `app/build/outputs/apk/debug/app-debug.apk` to your phone
   - Methods: USB transfer, email, cloud storage, etc.

3. **Install on phone**:
   - On your phone, go to Settings > Security
   - Enable "Install unknown apps" for your file manager
   - Open the APK file and tap "Install"

#### Method 3: Release Build for Distribution

For sharing with others or production use:

```bash
# Build release APK (unsigned)
./gradlew assembleRelease

# The APK will be at: app/build/outputs/apk/release/app-release-unsigned.apk
```


### Development vs Production Installation

#### Development Installation (Emulator)
```bash
# Start emulator
./scripts/start-emulator.sh

# Install debug build (includes debugging info, larger size)
./scripts/install.sh

# Quick development cycle
./gradlew assembleDebug && ~/Android/Sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
```

#### Production Installation (Phone)
```bash
# Build optimized release version
./gradlew assembleRelease

# Install release build (smaller, optimized)
~/Android/Sdk/platform-tools/adb install -r app/build/outputs/apk/release/app-release-unsigned.apk
```

#### Key Differences:

| Aspect | Debug Build (Emulator) | Release Build (Phone) |
|--------|----------------------|---------------------|
| **Size** | Larger (~8-12 MB) | Smaller (~4-6 MB) |
| **Performance** | Slower (debug info) | Faster (optimized) |
| **Debugging** | Full debug support | Limited debugging |
| **Logging** | Verbose logs | Production logs only |
| **Security** | Debug certificates | Production ready |
| **Use Case** | Development & testing | End users |

#### Debugging
1. **Physical Device**: Connect via USB with USB Debugging enabled
2. **Emulator**: Use `./scripts/start-emulator.sh` to start the virtual device
3. **VS Code**: Use built-in debugging with the Java Extension Pack
4. **Device Check**: Run `./scripts/check-emulator.sh` to see connected devices

### Alternative: Android Studio Development

If you prefer Android Studio on Fedora 42:

1. **Install Android Studio via Flatpak (recommended)**
   ```bash
   # Install Flatpak if not already installed
   sudo dnf install flatpak
   
   # Add Flathub repository
   flatpak remote-add --if-not-exists flathub https://flathub.org/repo/flathub.flatpakrepo
   
   # Install Android Studio
   flatpak install flathub com.google.AndroidStudio
   
   # Launch Android Studio
   flatpak run com.google.AndroidStudio
   ```

2. **Open the project** by selecting "Open an existing project" and choosing this directory
3. **Sync project** when prompted
4. **Run the app** using the green play button or Shift+F10

## Project Structure

```
app/src/main/java/net/luisico/howmanyhours/
├── data/
│   ├── entities/          # Room entities (Project, TimeEntry)
│   ├── dao/              # Data Access Objects
│   └── database/         # Room database setup
├── repository/           # Data repository layer
├── viewmodel/           # ViewModels for UI state management
├── ui/
│   ├── theme/           # Material Design theme
│   └── screens/         # Compose UI screens
└── utils/               # Utility classes
```

## Architecture

- **MVVM Architecture** with Repository pattern
- **Jetpack Compose** for modern UI development
- **Room Database** for local data persistence
- **Kotlin Coroutines** for asynchronous operations
- **Material Design 3** for consistent, modern UI


## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for the full text.

### Trademark

The project name, logos, icons, package namespace, and author's personal information are **not** licensed for use in derivative works. Forks must rebrand fully. See [TRADEMARK.md](TRADEMARK.md) for details.