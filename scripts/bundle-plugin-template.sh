#!/bin/bash
# Bundles the KMP template into a zip for the JetBrains plugin.
# Run this before building/releasing the plugin.
set -e
cd "$(dirname "$0")/.."

OUTPUT="catylst-plugin/src/main/resources/templates/catylst-template.zip"
mkdir -p "$(dirname "$OUTPUT")"

# Remove stale zip
rm -f "$OUTPUT"

zip -r "$OUTPUT" . \
  --exclude "*/cli-generator/*" \
  --exclude "*/catylst-plugin/*" \
  --exclude "*/.git" \
  --exclude "*/.git/*" \
  --exclude "*/build" \
  --exclude "*/build/*" \
  --exclude "*/.gradle" \
  --exclude "*/.gradle/*" \
  --exclude "*/.idea" \
  --exclude "*/.idea/*" \
  --exclude "*/.kotlin" \
  --exclude "*/.kotlin/*" \
  --exclude "*/.DS_Store" \
  --exclude "*/xcuserdata/*" \
  --exclude "*/node_modules/*" \
  --exclude "*/npm/*" \
  --exclude "*/docs/*" \
  --exclude "*/scripts/bundle-plugin-template.sh"

echo "✅ Bundled template → $OUTPUT ($(du -sh "$OUTPUT" | cut -f1))"
