#!/bin/bash
# Bundles the KMP template into a zip for the JetBrains plugin.
# Run this before building/releasing the plugin.
set -e
cd "$(dirname "$0")/.."

OUTPUT="catylst-plugin/src/main/resources/templates/catylst-template.zip"
mkdir -p "$(dirname "$OUTPUT")"

# Remove stale zip
rm -f "$OUTPUT"

# Patterns are matched against archive entry names (no leading "./"), so each junk
# directory needs both a root-level form ("name/*") and a nested form ("*/name/*").
zip -r "$OUTPUT" . \
  -x '.git/*' '*/.git/*' \
     'build/*' '*/build/*' \
     '.gradle/*' '*/.gradle/*' \
     '.kotlin/*' '*/.kotlin/*' \
     '.idea/*' '*/.idea/*' \
     'cli-generator/*' \
     'catylst-plugin/*' \
     'npm/*' '*/npm/*' \
     'docs/*' '*/docs/*' \
     '*/node_modules/*' \
     '*/xcuserdata/*' \
     '*.DS_Store' \
     'local.properties' \
     '.claude/settings.local.json' '*/.claude/settings.local.json' \
     'scripts/bundle-plugin-template.sh'

echo "✅ Bundled template → $OUTPUT ($(du -sh "$OUTPUT" | cut -f1))"
