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
