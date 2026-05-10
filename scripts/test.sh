#!/bin/bash
set -e

echo "Running common (KMP) tests..."
./gradlew :composeApp:test

echo "Running Android unit tests..."
./gradlew :composeApp:testDebugUnitTest

echo "All tests passed."
