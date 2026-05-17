#!/bin/bash
# Catylst CLI Generator Installer
# Usage: bash install.sh

set -e

INSTALL_DIR="${INSTALL_DIR:-$HOME/.local/bin}"
JAR_NAME="catylst-cli.jar"
JAR_URL="https://github.com/Rohit-554/Catylst/releases/latest/download/catylst-cli.jar"

echo "🚀 Installing Catylst CLI Generator..."

# Create install directory
mkdir -p "$INSTALL_DIR"

# Check if we're in the repo (dev mode) or downloading
if [ -f "build/libs/cli-generator-1.0.0.jar" ]; then
    echo "📦 Using local build..."
    cp "build/libs/cli-generator-1.0.0.jar" "$INSTALL_DIR/$JAR_NAME"
elif command -v curl &> /dev/null; then
    echo "📥 Downloading from GitHub releases..."
    curl -L -o "$INSTALL_DIR/$JAR_NAME" "$JAR_URL" || {
        echo "❌ Download failed. Build locally with: ./gradlew jar"
        exit 1
    }
else
    echo "❌ curl not found. Please install curl or build locally."
    exit 1
fi

# Create wrapper script — write to temp file first, then atomically install
# to prevent symlink attacks where a pre-placed symlink could cause the
# heredoc to overwrite a file at the symlink's target path.
WRAPPER_TMP=$(mktemp)
trap 'rm -f "$WRAPPER_TMP"' EXIT

cat > "$WRAPPER_TMP" << 'EOF'
#!/bin/bash
java -jar "$(dirname "$0")/catylst-cli.jar" "$@"
EOF

rm -f "$INSTALL_DIR/catylst"
install -m 755 "$WRAPPER_TMP" "$INSTALL_DIR/catylst"

# Check if install dir is in PATH
if [[ ":$PATH:" != *":$INSTALL_DIR:"* ]]; then
    echo ""
    echo "⚠️  $INSTALL_DIR is not in your PATH."
    echo "   Add this to your shell profile (~/.bashrc, ~/.zshrc, etc.):"
    echo ""
    echo "   export PATH=\"$INSTALL_DIR:\$PATH\""
    echo ""
fi

echo "✅ Catylst CLI installed to $INSTALL_DIR/catylst"
echo ""
echo "Usage:"
echo "  catylst --interactive              # Interactive mode"
echo "  catylst --package com.app --name App # Quick generate"
echo "  catylst --help                     # Show all options"
