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

## Build

```bash
cd cli-generator
./gradlew build
```

The fat JAR is output to:
```
build/libs/cli-generator-1.0.0.jar
```

## Usage

### Interactive Mode

```bash
./gradlew run --args="--interactive"
# or
java -jar build/libs/cli-generator-1.0.0.jar --interactive
```

### Non-Interactive Mode

```bash
java -jar cli-generator-1.0.0.jar \
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
| `--theme-color` | Theme seed color (hex) | Default M3 purple |
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
java -jar cli-generator-1.0.0.jar \
  --package com.minimal.app \
  --name MinimalApp \
  --features ktor \
  --no-sample \
  --ai-provider none
```

### Full project with custom pink theme and Groq AI

```bash
java -jar cli-generator-1.0.0.jar \
  --package com.mycompany.awesome \
  --name AwesomeApp \
  --features ai,notifications,permissions,room,preferences,ktor \
  --ai-provider groq \
  --theme-color '#E91E63' \
  --theme-expressive
```

### Include Ktor server backend

```bash
java -jar cli-generator-1.0.0.jar \
  --package com.fullstack.app \
  --name FullStackApp \
  --features ai,ktor,room,server \
  --ai-provider gemini
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

## License

Same as Catylst KMP Starter Kit — see [LICENSE](../LICENSE)
