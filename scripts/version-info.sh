#!/bin/bash
# Quick script to display current version information

BUILD_GRADLE="app/build.gradle.kts"

VERSION_NAME=$(grep "versionName = " "$BUILD_GRADLE" | sed 's/.*versionName = "//' | sed 's/".*//')
VERSION_CODE=$(grep "versionCode = " "$BUILD_GRADLE" | sed 's/.*versionCode = //' | sed 's/[^0-9]//g')

echo "Current Version Info:"
echo "  versionName: $VERSION_NAME"
echo "  versionCode: $VERSION_CODE"
