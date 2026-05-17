# Catylst CLI Generator

A command-line tool to generate customized [Catylst KMP](https://github.com/Rohit-554/Catylst) projects with selectable features, AI providers, and theme customization.

## What It Does

Instead of manually cloning the Catylst repo and deleting files you don't need, just run this CLI and answer a few questions. It generates a **ready-to-build** KMP project with only the features you want.

## Features You Can Select

| Feature | What It Gives You |
|---------|-------------------|
| **AI** | Chat with Claude / Groq / Gemini |
| **Notifications** | Push notifications on Android & iOS |
| **Permissions** | Camera / Location / Notification permission handling |
| **Room** | Local SQLite database with DAOs |
| **Preferences** | Key-value settings storage |
| **Ktor** | HTTP client for API calls |
| **Server** | Backend Ktor server module |

Plus:
- **Sample code toggle** — include demo screens or start clean
- **AI provider picker** — Claude / Groq / Gemini / None
- **Theme generator** — pick any color, get full M3 light + dark theme

## Requirements

- JDK 17+ (`java -version` should show 17 or higher)

## Installation

### Option 1: Download Pre-built JAR

```bash
curl -L -o catylst-cli.jar https://github.com/Rohit-554/Catylst/releases/latest/download/catylst-cli.jar
```

### Option 2: Build from Source

```bash
cd cli-generator
./gradlew jar
# JAR will be at: build/libs/cli-generator-1.0.0.jar
```

## Usage

### Interactive Mode (Recommended) — Answer Questions

```bash
java -jar catylst-cli.jar --interactive
```

The CLI will ask you:

```
🚀 Welcome to Catylst KMP Project Generator!

📦 Package name (e.g., com.example.myapp):
→ com.mycompany.myapp

📱 App name (e.g., MyApp):
→ MyApp

📋 Available features:
  ai              — AI Integration [default]
  notifications   — Push Notifications [default]
  permissions     — Runtime Permissions [default]
  room            — Room Database [default]
  preferences     — Preferences [default]
  ktor            — Networking (Ktor) [default]
  server          — Ktor Server

Select features (comma-separated, or press Enter for defaults):
→ ai,ktor,room

Include sample/demo code? [Y/n] (default: Y):
→ n

Select AI provider [claude/groq/gemini] (default: claude):
→ groq

Theme seed color (hex, e.g., #6750A4, or press Enter to skip):
→ #E91E63

Enable M3 Expressive motion? [y/N] (default: N):
→ y

✅ Project generated successfully at: ./MyApp
```

Just press **Enter** to accept defaults, or type your own values.

### Non-Interactive Mode — One Command

For scripts or CI/CD:

```bash
java -jar catylst-cli.jar \
  --package com.example.myapp \
  --name MyApp \
  --features ai,ktor,room \
  --no-sample \
  --ai-provider groq \
  --theme-color '#E91E63' \
  --theme-expressive
```

### All Options

| Option | Description | Default |
|--------|-------------|---------|
| `--interactive`, `-i` | Run interactive wizard | Non-interactive |
| `--package`, `-p` | Application package name | *(required)* |
| `--name`, `-n` | Application display name | *(required)* |
| `--project` | Project directory name | Same as `--name` |
| `--features`, `-f` | Comma-separated feature IDs | All defaults |
| `--no-sample` | Exclude sample/demo code | Include samples |
| `--ai-provider`, `-a` | `claude`, `groq`, `gemini`, `none` | `claude` |
| `--theme-color` | Theme seed color (hex) | Skip theme |
| `--theme-expressive` | Enable M3 Expressive motion | Disabled |
| `--output`, `-o` | Output directory | Current directory |

## Examples

### Quick start — just press Enter for everything

```bash
java -jar catylst-cli.jar --interactive
# Package: com.example.app
# App: MyApp
# Features: (press Enter for all defaults)
# Sample code: (press Enter for Yes)
# AI provider: (press Enter for Claude)
# Theme: (press Enter to skip)
```

### Minimal project — no AI, no sample code

```bash
java -jar catylst-cli.jar --interactive
# Package: com.minimal.app
# App: MinimalApp
# Features: ktor
# Sample code: n
# Theme: (press Enter)
```

### Full project with pink theme and Groq

```bash
java -jar catylst-cli.jar --interactive
# Package: com.mycompany.awesome
# App: AwesomeApp
# Features: (press Enter for all)
# Sample code: (press Enter)
# AI provider: groq
# Theme: #E91E63
# Expressive: y
```

### Generate and build immediately

```bash
java -jar catylst-cli.jar --interactive
# ...answer prompts...
cd MyApp
./gradlew :androidApp:assembleDebug
```

## What Gets Generated

After running the CLI, you get a complete KMP project:

```
MyApp/
├── androidApp/          # Android entry point
├── composeApp/          # Shared Kotlin Multiplatform code
│   ├── src/commonMain/
│   │   ├── kotlin/com/example/app/
│   │   │   ├── ai/              # AI providers (if selected)
│   │   │   ├── data/local/      # Room entities/DAOs (if selected)
│   │   │   ├── data/preferences/# Settings (if selected)
│   │   │   ├── network/         # Ktor client (if selected)
│   │   │   ├── notifications/   # Notification scheduler (if selected)
│   │   │   ├── permissions/     # Permission controller (if selected)
│   │   │   ├── ui/screens/      # Screens + demo screens (if sample on)
│   │   │   └── ui/theme/        # Generated theme (if theme-color set)
│   │   └── composeResources/
│   ├── src/androidMain/         # Android actual implementations
│   ├── src/iosMain/             # iOS actual implementations
│   └── src/desktopMain/         # Desktop actual implementations
├── iosApp/              # iOS Xcode project
├── gradle/
│   └── libs.versions.toml       # Cleaned dependency catalog
├── settings.gradle.kts
├── build.gradle.kts
└── README.md            # Generated project README
```

## Development

### Run Tests

```bash
./gradlew test
```

### Run Locally

```bash
./gradlew run --args="--interactive"
```

### Build Fat JAR

```bash
./gradlew jar
# Output: build/libs/cli-generator-1.0.0.jar
```

## Troubleshooting

### `java.lang.UnsupportedClassVersionError`

Your JDK is too old. Upgrade to JDK 17+:
```bash
java -version  # Should show 17 or higher
```

### Generated project fails to build

Remove the old generated project first:
```bash
rm -rf ./MyApp
java -jar catylst-cli.jar --interactive
```

## License

Same as Catylst KMP Starter Kit — see [LICENSE](../LICENSE)
