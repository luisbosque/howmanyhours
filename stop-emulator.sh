#!/bin/bash

# Stop Android Emulator

echo "🛑 Stopping Android Emulator..."

# Kill emulator processes
pkill -f "emulator -avd" 2>/dev/null || true
pkill -f "qemu-system" 2>/dev/null || true

echo "✅ Emulator stopped"
