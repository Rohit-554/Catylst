# Catylst CLI Generator

A command-line tool to generate customized [Catylst KMP](https://github.com/Rohit-554/Catylst) projects with selectable features, AI providers, and theme customization.

## Features

- **Feature Selection** — Choose which features to include:
  - AI Integration (Claude / Groq / Gemini)
  - Push Notifications (WorkManager + Alarmee)
  - Runtime Permissions (Camera, Location, etc.)
  - Room Database (with KSP processors)
  - Preferences (multiplatform-settings)
  - Ktor Networking
  - Ktor Server (optional backend module)

- **Sample Code Toggle** — Include or exclude demo screens and ViewModels

- **AI Provider Selection** — Swap AI provider with one-line change

- **Theme Customization** — Generate Material 3 color schemes from a seed color
  - M3 Expressive motion support
  - Full tonal palette (light + dark)
  - Zero runtime computation (hardcoded `Color(0xFF...)` values)

- **Automatic Dependency Resolution** — Selecting AI auto-enables Ktor

## Requirements

- JDK 17+
- Kotlin 2.0+

## Installation

### Option 1: Download Pre-built JAR (Recommended)

```bash
# Download latest release
curl -L -o catylst-cli.jar https://github.com/Rohit-554/Catylst/releases/latest/download/catylst-cli.jar

# Run it
java -jar catylst-cli.jar --interactive
```

### Option 2: Install to PATH

```bash
cd cli-generator
bash install.sh

# Now you can use `catylst` anywhere
catylst --interactive
```

### Option 3: Build from Source

```bash
cd cli-generator
./gradlew jar

# Run the built JAR
java -jar build/libs/cli-generator-1.0.0.jar --interactive
```

### Option 4: Run with Gradle (Development)

```bash
cd cli-generator
./gradlew run --args="--interactive"
```

## Usage

### Interactive Mode

The easiest way — just answer the prompts:

```bash
java -jar catylst-cli.jar --interactive
```

### Non-Interactive Mode

For CI/CD or scripts, pass all options directly:

```bash
java -jar catylst-cli.jar \
  --package com.example.myapp \
  --name MyApp \
  --features ai,notifications,permissions,room,preferences,ktor \
  --ai-provider claude \
  --theme-color '#6750A4' \
  --output ./generated
```

### All Options

| Option | Description | Default |
|--------|-------------|---------|
| `--package`, `-p` | Application package name | *(required)* |
| `--name`, `-n` | Application display name | *(required)* |
| `--project` | Project directory name | Same as `--name` |
| `--features`, `-f` | Comma-separated feature IDs | All default features |
| `--no-sample` | Exclude sample/demo code | Include samples |
| `--ai-provider`, `-a` | AI provider: `claude`, `groq`, `gemini`, `none` | `claude` (if AI selected) |
| `--theme-color` | Theme seed color (hex, e.g., `#6750A4`) | Skip theme generation |
| `--theme-expressive` | Enable M3 Expressive motion | Disabled |
| `--output`, `-o` | Output directory | Current directory |
| `--interactive`, `-i` | Run interactive prompts | Non-interactive |

### Feature IDs

| ID | Name | Default |
|----|------|---------|
| `ai` | AI Integration | ✅ |
| `notifications` | Push Notifications | ✅ |
| `permissions` | Runtime Permissions | ✅ |
| `room` | Room Database | ✅ |
| `preferences` | Preferences | ✅ |
| `ktor` | Networking (Ktor) | ✅ |
| `server` | Ktor Server | ❌ |

## Examples

### Minimal project (Ktor only, no AI, no sample code)

```bash
java -jar catylst-cli.jar \
  --package com.minimal.app \
  --name MinimalApp \
  --features ktor \
  --no-sample \
  --ai-provider none
```

### Full project with custom pink theme and Groq AI

```bash
java -jar catylst-cli.jar \
  --package com.mycompany.awesome \
  --name AwesomeApp \
  --features ai,notifications,permissions,room,preferences,ktor \
  --ai-provider groq \
  --theme-color '#E91E63' \
  --theme-expressive
```

### Include Ktor server backend

```bash
java -jar catylst-cli.jar \
  --package com.fullstack.app \
  --name FullStackApp \
  --features ai,ktor,room,server \
  --ai-provider gemini
```

### Generate and build immediately

```bash
java -jar catylst-cli.jar \
  --package com.example.app \
  --name MyApp \
  --output ./projects && \
  cd ./projects/MyApp && \
  ./gradlew :androidApp:assembleDebug
```

## What Gets Generated

After running the CLI, you'll have a fully configured KMP project:

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

## Architecture

```
CatylstCli (Clikt)
  └── ProjectGenerator
        ├── TemplateDownloader      — Local project or GitHub release
        ├── PackageRenamer          — Rename package + app name
        ├── FeatureRemover          — Remove unselected features
        ├── DependencyCleaner       — Clean Gradle/TOML deps
        ├── NavigationEditor        — Regenerate Screen/AppNavigation/HomeScreen
        ├── HomeViewModelEditor     — Regenerate HomeViewModel
        ├── AiProviderSelector      — Swap AI provider binding
        ├── MainActivityEditor      — Clean MainActivity.kt
        ├── SampleCodeManager       — Remove sample code
        ├── ThemeGenerator          — Generate M3 color palette
        └── ProjectCleaner          — Clean docs/scripts/.git
```

## Development

### Run Tests

```bash
./gradlew test
```

### Run Locally

```bash
./gradlew run --args="--package com.test.app --name TestApp"
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

Make sure the output directory is clean:
```bash
rm -rf ./MyApp  # Remove old generated project first
java -jar catylst-cli.jar --package com.example.app --name MyApp
```

### Feature not found

Check the feature ID spelling. Valid IDs: `ai`, `notifications`, `permissions`, `room`, `preferences`, `ktor`, `server`.

## License

Same as Catylst KMP Starter Kit — see [LICENSE](../LICENSE)
