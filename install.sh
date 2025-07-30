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
