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
