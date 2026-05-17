#!/bin/bash
# =============================================================================
# Catylst — Project Setup Script
#
# Run this ONCE after cloning the template to rename the project to your own
# package name and app name.
#
# Usage:
#   bash scripts/setup.sh <package> <AppName>
#
# Example:
#   bash scripts/setup.sh com.alice.myapp MyApp
#
# What this script does:
#   1. Replaces the package name in all source files and build scripts
#   2. Renames the source directory tree to match the new package
#   3. Replaces the app name in string resources and manifests
#   4. Updates the root project name in settings.gradle.kts
#   5. Deletes the docs/ folder (template documentation, not needed in your project)
#   6. Deletes itself (it is a one-time tool)
#
# After running, sync Gradle in your IDE and build once to verify.
# =============================================================================

set -e

# ─── Validation ──────────────────────────────────────────────────────────────

TEMPLATE_PACKAGE="io.jadu.catylst"
TEMPLATE_PACKAGE_PATH="io/jadu/catylst"
TEMPLATE_APP_NAME="Catylst"
TEMPLATE_ANDROID_PACKAGE="io.jadu.catylst.android"

NEW_PACKAGE="$1"
NEW_APP_NAME="$2"

if [ -z "$NEW_PACKAGE" ] || [ -z "$NEW_APP_NAME" ]; then
    echo ""
    echo "  Usage: bash scripts/setup.sh <package> <AppName>"
    echo ""
    echo "  Example: bash scripts/setup.sh com.alice.myapp MyApp"
    echo ""
    echo "  package  — reverse-domain package name (e.g. com.yourname.yourapp)"
    echo "  AppName  — PascalCase app name shown on the home screen (e.g. MyApp)"
    echo ""
    exit 1
fi

# Validate package format: at least two segments, lowercase letters and dots only
if ! echo "$NEW_PACKAGE" | grep -qE '^[a-z][a-z0-9]*(\.[a-z][a-z0-9]*){1,}$'; then
    echo "Error: package must be lowercase reverse-domain format, e.g. com.yourname.yourapp"
    exit 1
fi

# Validate app name: PascalCase, letters only
if ! echo "$NEW_APP_NAME" | grep -qE '^[A-Za-z][A-Za-z0-9]*$'; then
    echo "Error: AppName must start with a letter and contain only letters and digits, e.g. MyApp"
    exit 1
fi

# ─── Escape helper ───────────────────────────────────────────────────────────
# Escapes sed BRE/ERE metacharacters so user input is treated as a literal
# string in replacement patterns — prevents sed /e-flag injection and other
# metacharacter exploits.
escape_sed() {
    printf '%s' "$1" | sed 's/[\/&]/\\&/g'
}

# ─── Derived values ──────────────────────────────────────────────────────────

# Convert package to path  (com.alice.myapp → com/alice/myapp)
NEW_PACKAGE_PATH="${NEW_PACKAGE//.//}"

# Android module package is the new package + .android
NEW_ANDROID_PACKAGE="${NEW_PACKAGE}.android"

# Project name for settings.gradle.kts — use PascalCase app name
NEW_PROJECT_NAME="$NEW_APP_NAME"

echo ""
echo "  Template package : $TEMPLATE_PACKAGE"
echo "  New package      : $NEW_PACKAGE"
echo "  New app name     : $NEW_APP_NAME"
echo ""

# ─── Step 1: Replace package name in all source files ────────────────────────

echo "[1/6] Replacing package name in source files..."

ESC_TEMPLATE_ANDROID_PKG=$(escape_sed "$TEMPLATE_ANDROID_PACKAGE")
ESC_NEW_ANDROID_PKG=$(escape_sed "$NEW_ANDROID_PACKAGE")
ESC_TEMPLATE_PKG=$(escape_sed "$TEMPLATE_PACKAGE")
ESC_NEW_PKG=$(escape_sed "$NEW_PACKAGE")

find . \
    -not -path "./.git/*" \
    -not -path "./build/*" \
    -not -path "./.idea/*" \
    -not -path "*/build/*" \
    \( -name "*.kt" -o -name "*.kts" -o -name "*.xml" -o -name "*.plist" \
       -o -name "*.pbxproj" -o -name "*.xcconfig" -o -name "*.swift" \) \
    -exec sed -i '' \
        "s|${ESC_TEMPLATE_ANDROID_PKG}|${ESC_NEW_ANDROID_PKG}|g" {} +

find . \
    -not -path "./.git/*" \
    -not -path "./build/*" \
    -not -path "./.idea/*" \
    -not -path "*/build/*" \
    \( -name "*.kt" -o -name "*.kts" -o -name "*.xml" -o -name "*.plist" \
       -o -name "*.pbxproj" -o -name "*.xcconfig" -o -name "*.swift" \) \
    -exec sed -i '' \
        "s|${ESC_TEMPLATE_PKG}|${ESC_NEW_PKG}|g" {} +

# ─── Step 2: Replace app name in string resources, manifests, and iOS config ──

echo "[2/6] Replacing app name..."

ESC_TEMPLATE_APP=$(escape_sed "$TEMPLATE_APP_NAME")
ESC_NEW_APP=$(escape_sed "$NEW_APP_NAME")

find . \
    -not -path "./.git/*" \
    -not -path "./build/*" \
    -not -path "./.idea/*" \
    -not -path "*/build/*" \
    \( -name "strings.xml" -o -name "AndroidManifest.xml" \
       -o -name "settings.gradle.kts" -o -name "*.xcconfig" -o -name "*.pbxproj" \) \
    -exec sed -i '' \
        "s|${ESC_TEMPLATE_APP}|${ESC_NEW_APP}|g" {} +

# ─── Step 3: Update root project name in settings.gradle.kts ─────────────────

echo "[3/6] Updating settings.gradle.kts project name..."

sed -i '' \
    "s|rootProject.name = \"${ESC_TEMPLATE_APP}\"|rootProject.name = \"${ESC_NEW_APP}\"|g" \
    settings.gradle.kts

# ─── Step 4: Rename source directory trees ───────────────────────────────────

echo "[4/6] Renaming source directory trees..."

SOURCE_SETS=(
    "composeApp/src/commonMain/kotlin"
    "composeApp/src/androidMain/kotlin"
    "composeApp/src/iosMain/kotlin"
    "composeApp/src/desktopMain/kotlin"
    "androidApp/src/main/kotlin"
)

for BASE in "${SOURCE_SETS[@]}"; do
    OLD_DIR="${BASE}/${TEMPLATE_PACKAGE_PATH}"
    NEW_DIR="${BASE}/${NEW_PACKAGE_PATH}"

    if [ -d "$OLD_DIR" ]; then
        # Create new parent directory
        mkdir -p "$(dirname "$NEW_DIR")"
        # Move content
        mv "$OLD_DIR" "$NEW_DIR"
        # Clean up empty parent dirs left behind
        PARENT="${BASE}/$(echo "$TEMPLATE_PACKAGE_PATH" | cut -d'/' -f1)"
        if [ -d "$PARENT" ] && [ -z "$(ls -A "$PARENT")" ]; then
            rm -rf "$PARENT"
        fi
    fi
done

# ─── Step 5: Remove template-only files ──────────────────────────────────────

echo "[5/6] Removing template documentation..."

# docs/ contains template-specific docs (architecture, skills guide, etc.)
# They are not part of your project — delete them now.
rm -rf "$(dirname "$0")/../docs"

# ─── Step 6: Self-delete ─────────────────────────────────────────────────────

echo "[6/6] Cleaning up setup script..."
rm -- "$0"

# ─── Done ─────────────────────────────────────────────────────────────────────

echo ""
echo "Done! Project renamed to: $NEW_PACKAGE ($NEW_APP_NAME)"
echo ""
echo "Next steps:"
echo "  1. Sync Gradle in Android Studio (File → Sync Project with Gradle Files)"
echo "  2. Run: ./gradlew :androidApp:assembleDebug"
echo "  3. Copy local.properties.example → local.properties and add your API keys"
echo ""
