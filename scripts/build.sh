#!/bin/bash
set -e

echo "Building Android debug APK..."
./gradlew :androidApp:assembleDebug

echo "Compiling iOS (simulator, arm64)..."
./gradlew :composeApp:compileKotlinIosSimulatorArm64

echo "Build complete."
