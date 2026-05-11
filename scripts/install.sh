#!/usr/bin/env bash
# install.sh - Remote installer for Catylst KMP Starter Kit
#
# Usage:
#   bash <(curl -sSL https://raw.githubusercontent.com/rohit-554/Catylst/main/scripts/install.sh) <package.name> <AppName> [target-dir]
#
# Example:
#   bash <(curl -sSL .../install.sh) com.mycompany.myapp MyApp

set -e

REPO_URL="https://github.com/rohit-554/Catylst.git"

# --- argument validation ---

PACKAGE_NAME="$1"
APP_NAME="$2"
TARGET_DIR="${3:-$APP_NAME}"

if [[ -z "$PACKAGE_NAME" || -z "$APP_NAME" ]]; then
    echo "Usage: install.sh <package.name> <AppName> [target-dir]"
    echo "  package.name  Reverse-domain format, lowercase  (e.g. com.mycompany.myapp)"
    echo "  AppName       PascalCase, letters only           (e.g. MyApp)"
    exit 1
fi

if ! echo "$PACKAGE_NAME" | grep -qE '^[a-z][a-z0-9]*(\.[a-z][a-z0-9]*){2,}$'; then
    echo "Error: package name must be lowercase reverse-domain with at least 3 segments (e.g. com.mycompany.myapp)"
    exit 1
fi

if ! echo "$APP_NAME" | grep -qE '^[A-Z][A-Za-z0-9]+$'; then
    echo "Error: app name must be PascalCase letters/digits starting with uppercase (e.g. MyApp)"
    exit 1
fi

if [[ -e "$TARGET_DIR" ]]; then
    echo "Error: '$TARGET_DIR' already exists. Provide a different target directory as the third argument."
    exit 1
fi

# --- check dependencies ---

for cmd in git curl bash; do
    if ! command -v "$cmd" &>/dev/null; then
        echo "Error: '$cmd' is required but not installed."
        exit 1
    fi
done

# --- clone ---

echo ""
echo "Catylst KMP Starter Kit"
echo "========================"
echo "Package : $PACKAGE_NAME"
echo "App name: $APP_NAME"
echo "Folder  : $TARGET_DIR"
echo ""
echo "Cloning..."
git clone --depth 1 "$REPO_URL" "$TARGET_DIR"

# --- setup ---

echo ""
echo "Renaming project..."
cd "$TARGET_DIR"
bash scripts/setup.sh "$PACKAGE_NAME" "$APP_NAME"

# --- next steps ---

echo ""
echo "Done. Your project is ready in: $(pwd)"
echo ""
echo "Next steps:"
echo "  1. Open the '$TARGET_DIR' folder in Android Studio"
echo "  2. File -> Sync Project with Gradle Files"
echo "  3. Copy local.properties.example to local.properties and add your AI API keys"
echo "  4. Run on Android or iOS"
echo ""
