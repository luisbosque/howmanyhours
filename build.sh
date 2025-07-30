#!/bin/bash
echo "🔨 Building How Many Hours..."
./gradlew assembleDebug
echo "✅ Build complete! APK located at: app/build/outputs/apk/debug/app-debug.apk"
