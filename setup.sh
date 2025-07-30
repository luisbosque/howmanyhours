#!/bin/bash

# How Many Hours - Development Setup Script for Fedora 42
# This script sets up the development environment for VS Code/Cursor on Fedora 42

set -e

echo "🚀 Setting up How Many Hours development environment on Fedora 42..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_step() {
    echo -e "${BLUE}[STEP]${NC} $1"
}

# Check and install Java if needed
check_install_java() {
    if command -v java &> /dev/null; then
        JAVA_VERSION=$(java -version 2>&1 | head -n1 | cut -d'"' -f2)
        MAJOR_VERSION=$(echo $JAVA_VERSION | cut -d'.' -f1)
        print_status "Java found: $JAVA_VERSION"
        
        if [ "$MAJOR_VERSION" -ge 21 ]; then
            print_status "Java version is compatible"
            
            # Check if JAVA_HOME is set
            if [ -z "$JAVA_HOME" ]; then
                print_warning "JAVA_HOME not set. Setting it now..."
                export JAVA_HOME=/usr/lib/jvm/java-${MAJOR_VERSION}-openjdk
                echo "export JAVA_HOME=/usr/lib/jvm/java-${MAJOR_VERSION}-openjdk" >> ~/.bashrc
                print_status "JAVA_HOME set to $JAVA_HOME"
            fi
        elif [ "$MAJOR_VERSION" -ge 17 ]; then
            print_warning "Java $MAJOR_VERSION found. Java 21+ is recommended but this should work."
        else
            print_error "Java version too old. Installing Java 21..."
            install_java
        fi
    else
        print_error "Java not found. Installing Java 21..."
        install_java
    fi
}

install_java() {
    print_step "Installing Java 21 OpenJDK with required modules..."
    if sudo dnf install -y java-21-openjdk-devel java-21-openjdk-jmods; then
        export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
        echo "export JAVA_HOME=/usr/lib/jvm/java-21-openjdk" >> ~/.bashrc
        print_status "Java 21 installed successfully with jlink support"
    else
        print_error "Failed to install Java 21. Please install manually:"
        echo "  sudo dnf install java-21-openjdk-devel java-21-openjdk-jmods"
        exit 1
    fi
}

# Check and install Android SDK
check_install_android_sdk() {
    if [ -z "$ANDROID_HOME" ]; then
        print_step "Setting up Android SDK..."
        setup_android_sdk
    else
        print_status "ANDROID_HOME found: $ANDROID_HOME"
    fi

    if command -v sdkmanager &> /dev/null; then
        print_status "Android SDK Manager found"
        install_android_packages
    else
        print_error "sdkmanager not found after installation. Something went wrong."
        return 1
    fi
}

setup_android_sdk() {
    # Install required packages for Android development
    print_step "Installing required packages (wget, unzip)..."
    sudo dnf install -y wget unzip
    
    # Create Android SDK directory
    print_step "Creating Android SDK directory..."
    mkdir -p ~/Android/Sdk/cmdline-tools
    cd ~/Android/Sdk/cmdline-tools
    
    # Download Android command line tools
    print_step "Downloading Android Command Line Tools..."
    if ! wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip; then
        print_error "Failed to download Android Command Line Tools"
        exit 1
    fi
    
    print_step "Extracting Command Line Tools..."
    unzip -q commandlinetools-linux-11076708_latest.zip
    mv cmdline-tools latest
    rm commandlinetools-linux-11076708_latest.zip
    
    # Set environment variables
    export ANDROID_HOME=$HOME/Android/Sdk
    export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin
    export PATH=$PATH:$ANDROID_HOME/platform-tools
    export PATH=$PATH:$ANDROID_HOME/emulator
    
    # Add to bashrc
    print_step "Adding environment variables to ~/.bashrc..."
    {
        echo ""
        echo "# Android SDK"
        echo "export ANDROID_HOME=\$HOME/Android/Sdk"
        echo "export PATH=\$PATH:\$ANDROID_HOME/cmdline-tools/latest/bin"
        echo "export PATH=\$PATH:\$ANDROID_HOME/platform-tools"
        echo "export PATH=\$PATH:\$ANDROID_HOME/emulator"
    } >> ~/.bashrc
    
    print_status "Android SDK setup complete"
    cd - > /dev/null
}

install_android_packages() {
    print_step "Installing required Android SDK packages..."
    
    # Accept licenses
    print_status "Accepting Android SDK licenses..."
    yes | sdkmanager --licenses > /dev/null 2>&1 || true
    
    # Install required packages
    print_status "Installing SDK platforms and build tools..."
    sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0" "emulator" || {
        print_warning "Some SDK packages may have failed to install"
    }
    
    print_status "Android SDK packages installed"
}

# Check and setup Gradle Wrapper
check_setup_gradle() {
    if [ -f "./gradlew" ]; then
        print_status "Gradle Wrapper script found"
        chmod +x ./gradlew
        
        # Check if gradle-wrapper.jar exists
        if [ -f "./gradle/wrapper/gradle-wrapper.jar" ]; then
            print_status "Gradle Wrapper JAR found"
        else
            print_step "Downloading Gradle Wrapper JAR..."
            setup_gradle_wrapper
        fi
    else
        print_error "Gradle Wrapper not found. This should be included in the project."
        exit 1
    fi
}

setup_gradle_wrapper() {
    # Ensure gradle/wrapper directory exists
    mkdir -p gradle/wrapper
    
    # Download gradle-wrapper.jar
    if wget -q -O gradle/wrapper/gradle-wrapper.jar https://github.com/gradle/gradle/raw/v8.2.0/gradle/wrapper/gradle-wrapper.jar; then
        print_status "Gradle Wrapper JAR downloaded successfully"
    else
        print_error "Failed to download Gradle Wrapper JAR"
        exit 1
    fi
}

# Test build
test_build() {
    print_status "Testing project build..."
    if ./gradlew build > /dev/null 2>&1; then
        print_status "Build successful! ✅"
    else
        print_warning "Build failed. You may need to resolve dependencies manually."
        print_status "Try running: ./gradlew build --info"
    fi
}

# VS Code setup
setup_vscode() {
    print_status "Setting up VS Code configuration..."
    
    # Create .vscode directory if it doesn't exist
    mkdir -p .vscode

    # Create settings.json
    cat > .vscode/settings.json << 'EOF'
{
    "java.home": null,
    "java.configuration.updateBuildConfiguration": "automatic",
    "java.compile.nullAnalysis.mode": "automatic",
    "files.exclude": {
        "**/build": true,
        "**/.gradle": true
    },
    "java.format.settings.url": "./kotlin-style.xml"
}
EOF

    # Create launch.json for debugging
    cat > .vscode/launch.json << 'EOF'
{
    "version": "0.2.0",
    "configurations": [
        {
            "type": "java",
            "name": "Debug Android App",
            "request": "attach",
            "hostName": "localhost",
            "port": 5005
        }
    ]
}
EOF

    # Create tasks.json for build tasks
    cat > .vscode/tasks.json << 'EOF'
{
    "version": "2.0.0",
    "tasks": [
        {
            "label": "Build Debug APK",
            "type": "shell",
            "command": "./gradlew",
            "args": ["assembleDebug"],
            "group": "build",
            "presentation": {
                "echo": true,
                "reveal": "always",
                "focus": false,
                "panel": "shared"
            }
        },
        {
            "label": "Install Debug APK",
            "type": "shell",
            "command": "./gradlew",
            "args": ["installDebug"],
            "group": "build",
            "presentation": {
                "echo": true,
                "reveal": "always",
                "focus": false,
                "panel": "shared"
            }
        },
        {
            "label": "Clean Project",
            "type": "shell",
            "command": "./gradlew",
            "args": ["clean"],
            "group": "build"
        }
    ]
}
EOF

    print_status "VS Code configuration created"
}

# Create development helper scripts
create_helper_scripts() {
    print_status "Creating helper scripts..."

    # Build script
    cat > build.sh << 'EOF'
#!/bin/bash
echo "🔨 Building How Many Hours..."
./gradlew assembleDebug
echo "✅ Build complete! APK located at: app/build/outputs/apk/debug/app-debug.apk"
EOF

    # Install script
    cat > install.sh << 'EOF'
#!/bin/bash
echo "📱 Installing How Many Hours on connected device..."
./gradlew installDebug
echo "✅ App installed successfully!"
EOF

    # Clean script
    cat > clean.sh << 'EOF'
#!/bin/bash
echo "🧹 Cleaning project..."
./gradlew clean
echo "✅ Project cleaned!"
EOF

    chmod +x build.sh install.sh clean.sh
    print_status "Helper scripts created: build.sh, install.sh, clean.sh"
}

# Main setup process
main() {
    echo "========================================"
    echo "  How Many Hours - Development Setup"
    echo "     Optimized for Fedora 42"
    echo "========================================"
    echo

    print_step "Step 1: Checking/Installing Java..."
    check_install_java
    
    echo
    print_step "Step 2: Checking/Installing Android SDK..."
    if ! check_install_android_sdk; then
        print_error "Android SDK setup failed. Please check the logs above."
        exit 1
    fi
    
    echo
    print_step "Step 3: Checking/Setting up Gradle..."
    check_setup_gradle
    
    echo
    print_step "Step 4: Setting up VS Code configuration..."
    setup_vscode
    
    echo
    print_step "Step 5: Creating helper scripts..."
    create_helper_scripts
    
    echo
    print_step "Step 6: Running initial build test..."
    test_build
    
    echo
    echo "========================================"
    print_status "Setup Complete! 🎉"
    echo "========================================"
    echo
    echo "✅ Java $(java -version 2>&1 | head -n1 | cut -d'"' -f2) configured"
    echo "✅ Android SDK installed and configured"
    echo "✅ VS Code configuration created"
    echo "✅ Helper scripts created"
    echo
    echo "🔄 IMPORTANT: Reload your shell to update environment variables:"
    echo "   source ~/.bashrc"
    echo
    echo "📱 Next steps:"
    echo "1. Reload shell: source ~/.bashrc"
    echo "2. Open this project in VS Code or Cursor"
    echo "3. Install recommended extensions when prompted"
    echo "4. Connect an Android device (USB debugging enabled) or create an AVD"
    echo "5. Run: ./install.sh to install the app"
    echo
    echo "🛠️  Available commands:"
    echo "  ./build.sh   - Build the project"
    echo "  ./install.sh - Install on device/emulator"
    echo "  ./clean.sh   - Clean the project"
    echo
    echo "🚀 Happy coding on Fedora 42!"
}

# Run main function
main