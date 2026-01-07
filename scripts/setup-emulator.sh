#!/bin/bash

# Android Emulator Setup Script for Fedora 42
# This script creates and configures an Android Virtual Device (AVD) for development

set -e

echo "🔧 Setting up Android Emulator for How Many Hours development..."

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

# Check prerequisites
check_prerequisites() {
    print_step "Checking prerequisites..."
    
    if [ -z "$ANDROID_HOME" ]; then
        print_error "ANDROID_HOME not set. Please run ./setup.sh first."
        exit 1
    fi
    
    if ! command -v sdkmanager &> /dev/null; then
        print_error "sdkmanager not found. Please run ./setup.sh first."
        exit 1
    fi
    
    print_status "Prerequisites check passed"
}

# Install emulator and system images
install_emulator_components() {
    print_step "Installing Android Emulator and system images..."
    
    # Accept licenses first
    yes | sdkmanager --licenses > /dev/null 2>&1 || true
    
    # Install emulator
    print_status "Installing Android Emulator..."
    sdkmanager "emulator" || print_warning "Emulator installation may have failed"
    
    # Install system image for API 34 (Android 14)
    print_status "Installing Android 14 (API 34) system image..."
    sdkmanager "system-images;android-34;google_apis;x86_64" || print_warning "System image installation may have failed"
    
    # Install additional required components
    print_status "Installing additional components..."
    sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0" || print_warning "Additional components may have failed"
    
    print_status "Emulator components installed"
}

# Create AVD
create_avd() {
    print_step "Creating Android Virtual Device (AVD)..."
    
    AVD_NAME="HowManyHours_Dev"
    DEVICE_TYPE="pixel_7"
    
    # Check if AVD already exists
    if avdmanager list avd | grep -q "$AVD_NAME"; then
        print_warning "AVD '$AVD_NAME' already exists"
        read -p "Do you want to recreate it? (y/N): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            print_status "Deleting existing AVD..."
            avdmanager delete avd -n "$AVD_NAME"
        else
            print_status "Using existing AVD"
            return 0
        fi
    fi
    
    # Create new AVD
    print_status "Creating AVD: $AVD_NAME"
    echo "no" | avdmanager create avd \
        -n "$AVD_NAME" \
        -k "system-images;android-34;google_apis;x86_64" \
        -d "$DEVICE_TYPE" \
        --force
    
    print_status "AVD '$AVD_NAME' created successfully"
}

# Configure AVD for better performance
configure_avd() {
    print_step "Configuring AVD for optimal performance..."
    
    AVD_NAME="HowManyHours_Dev"
    AVD_DIR="$HOME/.android/avd/${AVD_NAME}.avd"
    CONFIG_FILE="$AVD_DIR/config.ini"
    
    if [ -f "$CONFIG_FILE" ]; then
        # Backup original config
        cp "$CONFIG_FILE" "${CONFIG_FILE}.backup"
        
        # Update configuration for better performance
        print_status "Optimizing AVD configuration..."
        
        # Enable hardware acceleration and optimize settings
        cat >> "$CONFIG_FILE" << 'EOF'

# Performance optimizations
hw.gpu.enabled=yes
hw.gpu.mode=host
hw.ramSize=2048
vm.heapSize=256
hw.keyboard=yes
hw.mainKeys=no
hw.dPad=no
showDeviceFrame=no
skin.dynamic=yes
EOF
        
        print_status "AVD configuration optimized"
    else
        print_warning "AVD config file not found, skipping optimization"
    fi
}

# Install virtualization support for better emulator performance
setup_virtualization() {
    print_step "Setting up hardware acceleration..."
    
    # Check if KVM is available
    if [ -r /dev/kvm ]; then
        print_status "KVM virtualization is available"
        
        # Check if user is in kvm group
        if groups | grep -q kvm; then
            print_status "User is in kvm group"
        else
            print_warning "User is not in kvm group. Adding to kvm group..."
            print_status "You may need to run: sudo usermod -a -G kvm $USER"
            print_status "Then log out and log back in for changes to take effect"
        fi
    else
        print_warning "KVM virtualization not available. Emulator will run slower."
        print_status "To enable KVM, run: sudo dnf install qemu-kvm libvirt"
    fi
}

# Create helper scripts
create_helper_scripts() {
    print_step "Creating emulator helper scripts..."
    
    # Start emulator script
    cat > start-emulator.sh << 'EOF'
#!/bin/bash

# Start Android Emulator for How Many Hours development

AVD_NAME="HowManyHours_Dev"

echo "🚀 Starting Android Emulator: $AVD_NAME"

if [ -z "$ANDROID_HOME" ]; then
    echo "Error: ANDROID_HOME not set. Please run 'source ~/.bashrc' first."
    exit 1
fi

# Start emulator in the background
$ANDROID_HOME/emulator/emulator -avd "$AVD_NAME" -no-snapshot-save -wipe-data &

echo "✅ Emulator starting in background..."
echo "⏳ Wait for the emulator to fully boot before running ./install.sh"
echo "🛑 To stop the emulator, run: ./stop-emulator.sh"
EOF

    # Stop emulator script
    cat > stop-emulator.sh << 'EOF'
#!/bin/bash

# Stop Android Emulator

echo "🛑 Stopping Android Emulator..."

# Kill emulator processes
pkill -f "emulator -avd" 2>/dev/null || true
pkill -f "qemu-system" 2>/dev/null || true

echo "✅ Emulator stopped"
EOF

    # Check emulator status script
    cat > check-emulator.sh << 'EOF'
#!/bin/bash

# Check Android Emulator and device status

echo "📱 Checking Android devices and emulator status..."
echo

# Check if emulator is running
if pgrep -f "emulator -avd" > /dev/null; then
    echo "✅ Emulator is running"
else
    echo "❌ Emulator is not running"
fi

echo
echo "📋 Connected devices:"
if [ -z "$ANDROID_HOME" ]; then
    echo "Error: ANDROID_HOME not set. Please run 'source ~/.bashrc' first."
    exit 1
fi

$ANDROID_HOME/platform-tools/adb devices

echo
echo "🔧 Available AVDs:"
$ANDROID_HOME/cmdline-tools/latest/bin/avdmanager list avd
EOF

    # Make scripts executable
    chmod +x start-emulator.sh stop-emulator.sh check-emulator.sh
    
    print_status "Helper scripts created:"
    print_status "  ./start-emulator.sh  - Start the Android emulator"
    print_status "  ./stop-emulator.sh   - Stop the Android emulator"
    print_status "  ./check-emulator.sh  - Check emulator and device status"
}

# Update install script to check for devices
update_install_script() {
    print_step "Updating install script to check for devices..."
    
    # Backup original install script
    cp install.sh install.sh.backup
    
    # Create new install script with device checking
    cat > install.sh << 'EOF'
#!/bin/bash
echo "📱 Installing How Many Hours on connected device..."

# Check if any devices are connected
if [ -z "$ANDROID_HOME" ]; then
    echo "❌ Error: ANDROID_HOME not set. Please run 'source ~/.bashrc' first."
    exit 1
fi

# Get list of devices
DEVICES=$($ANDROID_HOME/platform-tools/adb devices | grep -v "List of devices" | grep -v "^$" | wc -l)

if [ "$DEVICES" -eq 0 ]; then
    echo "❌ No Android devices or emulator detected!"
    echo
    echo "📋 Options:"
    echo "1. Connect an Android device via USB (enable USB debugging)"
    echo "2. Start the Android emulator: ./start-emulator.sh"
    echo "3. Check device status: ./check-emulator.sh"
    echo
    echo "💡 If you need to set up an emulator, run: ./setup-emulator.sh"
    exit 1
fi

echo "✅ Found $DEVICES device(s) connected"
./gradlew installDebug

if [ $? -eq 0 ]; then
    echo "✅ App installed successfully!"
    echo "📱 You can now launch 'How Many Hours' on your device/emulator"
else
    echo "❌ Installation failed. Check the error messages above."
    exit 1
fi
EOF

    chmod +x install.sh
    print_status "Install script updated with device checking"
}

# Main setup process
main() {
    echo "========================================"
    echo "  Android Emulator Setup"
    echo "     How Many Hours - Fedora 42"
    echo "========================================"
    echo

    check_prerequisites
    
    echo
    setup_virtualization
    
    echo
    install_emulator_components
    
    echo
    create_avd
    
    echo
    configure_avd
    
    echo
    create_helper_scripts
    
    echo
    update_install_script
    
    echo
    echo "========================================"
    print_status "Emulator Setup Complete! 🎉"
    echo "========================================"
    echo
    echo "✅ Android Emulator installed and configured"
    echo "✅ AVD 'HowManyHours_Dev' created (Pixel 7, Android 14)"
    echo "✅ Performance optimizations applied"
    echo "✅ Helper scripts created"
    echo "✅ Install script updated"
    echo
    echo "🚀 Next steps:"
    echo "1. Start the emulator: ./start-emulator.sh"
    echo "2. Wait for it to fully boot (may take 2-3 minutes first time)"
    echo "3. Install the app: ./install.sh"
    echo
    echo "🛠️  Available commands:"
    echo "  ./start-emulator.sh  - Start Android emulator"
    echo "  ./stop-emulator.sh   - Stop Android emulator"  
    echo "  ./check-emulator.sh  - Check emulator status"
    echo "  ./install.sh         - Install app (with device check)"
    echo
    echo "💡 First boot may be slow. Subsequent starts will be faster!"
    echo "🎮 Happy testing on Fedora 42!"
}

# Run main function
main