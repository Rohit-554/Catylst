# Plan: Catylst KMP Starter Kit — JetBrains Plugin

## Goal
Transform the Catylst KMP starter kit into an installable **JetBrains IDE plugin** (IntelliJ IDEA / Android Studio) that lets users:
1. Select which features they want (AI, Notifications, Permissions, Room DB, Preferences, Networking, etc.)
2. Enter their package name and app name
3. Choose an AI provider (Claude / Groq / Gemini / None)
4. **Opt-in to include sample/demo code** for each selected feature (so users can see the feature in action and understand the integration flow)
5. Auto-generate a clean, renamed project with only the selected features
6. Optionally build the project automatically after generation

### Wizard Entry Point
The plugin integrates via **`DirectoryProjectGenerator` + `AbstractNewProjectStep`** — the same integration point JetBrains' own KMP wizard uses. Users access it via:
> **File → New → Project → Catylst KMP**

This renders the wizard inside the standard "New Project" dialog, matching IDE conventions and requiring no custom window management.

### Wizard Steps Overview
| Step | Name | Always shown? |
|------|------|--------------|
| 1 | Project Details (name, package) | Yes |
| 2 | Platforms (Android / iOS / Desktop) | Yes |
| 3 | Features (AI, Notifications, etc.) | Yes |
| 4 | Sample Code & AI Provider | Yes |
| 5 | Theme Customization | Yes |

---

## Current State Analysis

### What Exists Today
- **Template repo** on GitHub with ~80 source files across Android/iOS/Desktop
- **`scripts/setup.sh`** — one-time rename script (package, app name, directory tree)
- **`scripts/install.sh`** — remote installer via `curl | bash`
- **Built-in features:** AI (3 providers), Notifications, Permissions, Room DB, Preferences, Ktor networking, Navigation3, Compose Multiplatform
- **Skills system** in `.claude/skills/` for adding/removing features (add-screen, add-feature, remove-feature, ai-provider, etc.)

### Key Files That Must Be Modified During Generation
| File | What Changes |
|------|-------------|
| `settings.gradle.kts` | `rootProject.name` |
| All `.kt` / `.kts` / `.xml` / `.plist` / `.pbxproj` / `.xcconfig` / `.swift` | Package name replacement |
| Source directory trees | `io/jadu/catylst` → `com/company/app` |
| `androidApp/build.gradle.kts` | `applicationId`, `namespace`, BuildConfig fields |
| `composeApp/build.gradle.kts` | `namespace` |
| `strings.xml`, `AndroidManifest.xml`, `Info.plist`, `.pbxproj` | App name replacement |
| `AppModule.kt` | AI provider binding, Koin module wiring |
| `Screen.kt` | Remove unused screen objects |
| `AppNavigation.kt` | Remove unused screen branches + serializer registrations |
| `HomeScreen.kt` | Remove unused navigation buttons |
| `build.gradle.kts` (composeApp) | Remove unused dependencies (WorkManager, Alarmee, etc.) |
| `gradle/libs.versions.toml` | Remove unused library definitions |

---

## Architecture: Plugin + Template Engine

```
┌─────────────────────────────────────────────────────────────┐
│  JetBrains IDE (IntelliJ / Android Studio)                  │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  Catylst Plugin (Kotlin + IntelliJ Platform SDK)    │    │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  │    │
│  │  │  Wizard UI  │→ │  Template   │→ │  Project    │  │    │
│  │  │  (Swing)    │  │  Engine     │  │  Generator  │  │    │
│  │  └─────────────┘  └─────────────┘  └─────────────┘  │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
                    ┌─────────────────────┐
                    │  GitHub Release API │
                    │  (fetch template    │
                    │   zip, no git req)  │
                    └─────────────────────┘
```

---

## Plugin Module Structure

```
catylst-plugin/
├── build.gradle.kts              # IntelliJ Platform Gradle Plugin 2.x
├── gradle.properties
├── settings.gradle.kts
├── src/
│   └── main/
│       ├── kotlin/
│       │   └── com/catylst/plugin/
│       │       ├── CatylstPlugin.kt              # Plugin entry point
│       │       ├── wizard/
│       │       │   ├── CatylstWizardDialog.kt    # Main wizard dialog
│       │       │   ├── FeatureSelectionStep.kt   # Feature checkboxes
│       │       │   ├── ProjectConfigStep.kt      # Package name, app name
│       │       │   ├── AiProviderStep.kt         # AI provider selection
│       │       │   └── ThemeCustomizationStep.kt # M3 Expressive + colors + fonts
│       │       ├── template/
│       │       │   ├── TemplateDownloader.kt     # GitHub release fetcher
│       │       │   ├── ProjectGenerator.kt       # Orchestrates generation
│       │       │   ├── PackageRenamer.kt         # Replaces package names
│       │       │   ├── FeatureRemover.kt         # Removes unselected features
│       │       │   ├── DependencyCleaner.kt      # Cleans build.gradle.kts + toml
│       │       │   ├── ThemeGenerator.kt         # Generates Color.kt/Theme.kt/Typography.kt
│       │       │   ├── FontCopier.kt             # Validates + copies .ttf/.otf files
│       │       │   └── BuildRunner.kt            # Optional: runs Gradle build
│       │       └── util/
│       │           ├── FileUtils.kt
│       │           └── Validation.kt
│       └── resources/
│           ├── META-INF/
│           │   └── plugin.xml                    # Plugin descriptor
│           ├── icons/
│           │   └── catylst.svg
│           └── templates/
│               ├── manifest.json                 # Feature metadata
│               └── catylst-template.zip          # Bundled template (offline fallback)
└── README.md
```

---

## Feature Dependency Constraints

Some features share infrastructure. These rules are enforced in the wizard UI (dependent toggle is force-enabled and grayed out) and re-validated server-side before generation:

| Feature | Requires |
|---------|----------|
| AI Integration | Networking (Ktor) — shares the `HttpClient` singleton |
| All others | No cross-feature dependencies |

---

## Feature Metadata System

A JSON manifest defines every feature, its files, dependencies, and removal rules.

### Schema additions vs original
- `"requires"` — feature IDs that must stay enabled when this feature is on
- `"platformBindings"` — koin bindings that live in `PlatformModule.*.kt` rather than `AppModule.kt` (engine edits the correct file per platform)
- `"kspProcessors"` — KSP processor lines to remove from build.gradle.kts (separate from regular deps)
- `"tomlVersionKeys"` — `[versions]` table keys to remove from `libs.versions.toml`
- `"tomlPluginKeys"` — `[plugins]` table keys to remove from `libs.versions.toml`
- `"iosInfoPlistKeys"` — `Info.plist` usage description keys to remove

```json
{
  "features": [
    {
      "id": "ai",
      "name": "AI Integration",
      "description": "Claude / Groq / Gemini AI chat",
      "default": true,
      "requires": ["ktor"],
      "files": [
        "composeApp/src/commonMain/kotlin/{pkg}/ai/",
        "composeApp/src/commonMain/kotlin/{pkg}/config/AppConfig.kt"
      ],
      "gradleDeps": [],
      "kspProcessors": [],
      "tomlVersionKeys": [],
      "tomlPluginKeys": [],
      "koinBindings": ["AiProvider", "AiRepository"],
      "platformBindings": {},
      "screens": ["AiDemo"],
      "buildConfigFields": ["CLAUDE_API_KEY", "GROQ_API_KEY", "GEMINI_API_KEY"],
      "iosInfoPlistKeys": []
    },
    {
      "id": "notifications",
      "name": "Push Notifications",
      "description": "WorkManager (Android) / UNUserNotificationCenter (iOS)",
      "default": true,
      "requires": [],
      "files": [
        "composeApp/src/commonMain/kotlin/{pkg}/notifications/",
        "composeApp/src/androidMain/kotlin/{pkg}/notifications/NotificationScheduler.android.kt",
        "composeApp/src/androidMain/kotlin/{pkg}/notifications/NotificationWorker.kt",
        "composeApp/src/iosMain/kotlin/{pkg}/notifications/NotificationScheduler.ios.kt",
        "composeApp/src/desktopMain/kotlin/{pkg}/notifications/NotificationScheduler.jvm.kt"
      ],
      "gradleDeps": ["workmanager", "alarmee"],
      "kspProcessors": [],
      "tomlVersionKeys": ["workmanager", "alarmee"],
      "tomlPluginKeys": [],
      "koinBindings": [],
      "platformBindings": {
        "android": ["NotificationScheduler"],
        "ios": ["NotificationScheduler"],
        "desktop": []
      },
      "screens": ["Notifications"],
      "buildConfigFields": [],
      "iosInfoPlistKeys": []
    },
    {
      "id": "permissions",
      "name": "Runtime Permissions",
      "description": "Camera, Location, Notifications permission handling",
      "default": true,
      "requires": [],
      "files": [
        "composeApp/src/commonMain/kotlin/{pkg}/permissions/",
        "composeApp/src/androidMain/kotlin/{pkg}/permissions/PermissionController.android.kt",
        "composeApp/src/iosMain/kotlin/{pkg}/permissions/PermissionController.ios.kt",
        "composeApp/src/iosMain/kotlin/{pkg}/permissions/LocationPermissionDelegate.kt",
        "composeApp/src/desktopMain/kotlin/{pkg}/permissions/PermissionController.jvm.kt"
      ],
      "gradleDeps": [],
      "kspProcessors": [],
      "tomlVersionKeys": [],
      "tomlPluginKeys": [],
      "koinBindings": [],
      "platformBindings": {
        "android": ["PermissionController"],
        "ios": ["PermissionController"],
        "desktop": ["PermissionController"]
      },
      "screens": ["Permissions"],
      "buildConfigFields": [],
      "iosInfoPlistKeys": [
        "NSCameraUsageDescription",
        "NSLocationWhenInUseUsageDescription",
        "NSMicrophoneUsageDescription",
        "NSPhotoLibraryUsageDescription"
      ]
    },
    {
      "id": "room",
      "name": "Room Database",
      "description": "Local persistence with Room 3.0",
      "default": true,
      "requires": [],
      "files": [
        "composeApp/src/commonMain/kotlin/{pkg}/data/local/",
        "composeApp/src/androidMain/kotlin/{pkg}/data/local/DatabaseFactory.android.kt",
        "composeApp/src/iosMain/kotlin/{pkg}/data/local/DatabaseFactory.ios.kt",
        "composeApp/src/iosMain/kotlin/{pkg}/data/local/DocumentDirectory.kt",
        "composeApp/src/desktopMain/kotlin/{pkg}/data/local/DatabaseFactory.jvm.kt"
      ],
      "gradleDeps": ["room-runtime", "sqlite-bundled"],
      "kspProcessors": ["room-compiler"],
      "tomlVersionKeys": ["room", "sqlite"],
      "tomlPluginKeys": ["room"],
      "koinBindings": ["AppDatabase"],
      "platformBindings": {},
      "screens": ["Detail"],
      "buildConfigFields": [],
      "iosInfoPlistKeys": []
    },
    {
      "id": "preferences",
      "name": "Preferences",
      "description": "Key-value storage with multiplatform-settings",
      "default": true,
      "requires": [],
      "files": [
        "composeApp/src/commonMain/kotlin/{pkg}/data/preferences/AppPreferences.kt"
      ],
      "gradleDeps": ["multiplatform-settings", "multiplatform-settings-coroutines", "multiplatform-settings-no-arg"],
      "kspProcessors": [],
      "tomlVersionKeys": ["multiplatform-settings"],
      "tomlPluginKeys": [],
      "koinBindings": ["AppPreferences"],
      "platformBindings": {
        "android": ["ObservableSettings"],
        "ios": ["ObservableSettings"],
        "desktop": ["ObservableSettings"]
      },
      "screens": ["Preferences"],
      "buildConfigFields": [],
      "iosInfoPlistKeys": []
    },
    {
      "id": "ktor",
      "name": "Networking (Ktor)",
      "description": "HTTP client with Ktor",
      "default": true,
      "requires": [],
      "files": [
        "composeApp/src/commonMain/kotlin/{pkg}/network/HttpClientFactory.kt",
        "composeApp/src/commonMain/kotlin/{pkg}/network/ApiService.kt"
      ],
      "gradleDeps": [
        "ktor-client-core",
        "ktor-client-content-negotiation",
        "ktor-serialization-kotlinx-json",
        "ktor-client-logging",
        "ktor-client-okhttp",
        "ktor-client-darwin"
      ],
      "kspProcessors": [],
      "tomlVersionKeys": ["ktor"],
      "tomlPluginKeys": [],
      "koinBindings": ["HttpClient", "ApiService"],
      "platformBindings": {},
      "screens": [],
      "buildConfigFields": [],
      "iosInfoPlistKeys": []
    },
    {
      "id": "server",
      "name": "Ktor Server",
      "description": "Backend server module",
      "default": false,
      "requires": [],
      "files": ["server/"],
      "gradleDeps": ["ktor-server-core", "ktor-server-netty", "logback"],
      "kspProcessors": [],
      "tomlVersionKeys": ["logback"],
      "tomlPluginKeys": [],
      "koinBindings": [],
      "platformBindings": {},
      "screens": [],
      "buildConfigFields": [],
      "iosInfoPlistKeys": [],
      "settingsInclude": [":server"]
    }
  ],
  "sampleCode": {
    "description": "Demo screens and ViewModels that show how to use each feature",
    "default": true,
    "perFeature": {
      "ai": {
        "files": [
          "composeApp/src/commonMain/kotlin/{pkg}/ui/screens/AiDemoScreen.kt",
          "composeApp/src/commonMain/kotlin/{pkg}/ui/viewmodel/AiViewModel.kt"
        ],
        "screens": ["AiDemo"],
        "koinBindings": ["AiViewModel"],
        "homeButtons": ["AI Demo"]
      },
      "notifications": {
        "files": [
          "composeApp/src/commonMain/kotlin/{pkg}/ui/screens/NotificationDemoScreen.kt"
        ],
        "screens": ["Notifications"],
        "koinBindings": [],
        "homeButtons": ["Notifications Demo"]
      },
      "permissions": {
        "files": [
          "composeApp/src/commonMain/kotlin/{pkg}/ui/screens/PermissionDemoScreen.kt"
        ],
        "screens": ["Permissions"],
        "koinBindings": [],
        "homeButtons": ["Permissions Demo"]
      },
      "preferences": {
        "files": [
          "composeApp/src/commonMain/kotlin/{pkg}/ui/screens/PreferencesDemoScreen.kt"
        ],
        "screens": ["Preferences"],
        "koinBindings": [],
        "homeButtons": ["Preferences Demo"]
      },
      "room": {
        "files": [
          "composeApp/src/commonMain/kotlin/{pkg}/ui/screens/DetailScreen.kt"
        ],
        "screens": ["Detail"],
        "koinBindings": [],
        "homeButtons": ["Go to Detail"]
      }
    }
  }
}
```

---

## Generation Pipeline

```
Step 1: Download Template
  → Try GitHub Releases API: fetch latest release zip (no git required)
  → On failure (offline / firewall): fall back to bundled zip at
    src/main/resources/templates/catylst-template.zip
    (show warning dialog: "Using bundled template vX.Y.Z — may not be latest")
  → Extract to temp directory

Step 2: Rename Project
  → Replace package name across all source files
    (io.jadu.catylst → com.company.app in .kt/.kts/.xml/.plist/.pbxproj/.xcconfig/.swift)
  → Rename source directory tree (io/jadu/catylst → com/company/app)
  → Replace app name in strings.xml, AndroidManifest, Info.plist, settings.gradle.kts
  → Update namespace + applicationId in build.gradle.kts files

Step 3: Apply Feature Selections
  Resolve dependency order: if AI is selected, mark ktor as required regardless of user toggle.
  For each UNSELECTED feature (process leaves before dependents):
    → Delete files listed in manifest "files" array (platform actuals included)
    → Remove "koinBindings" lines from AppModule.kt
    → Remove "platformBindings" entries from PlatformModule.android.kt /
      PlatformModule.ios.kt / PlatformModule.jvm.kt per platform key
    → Remove Screen objects from Screen.kt
    → Remove navigation branches + subclass() registrations from AppNavigation.kt
    → Remove navigation buttons from HomeScreen.kt
    → Remove "gradleDeps" from composeApp/build.gradle.kts
    → Remove "kspProcessors" lines (kspAndroid / kspIosArm64 / kspIosSimulatorArm64)
      from composeApp/build.gradle.kts
    → Remove alias(libs.plugins.X) from build.gradle.kts plugins block
      for each entry in "tomlPluginKeys"
    → Remove ksp { arg("room.schemaLocation", ...) } block if room is removed
    → Remove [versions] entries listed in "tomlVersionKeys" from libs.versions.toml
    → Remove [libraries] entries for all removed gradleDeps from libs.versions.toml
    → Remove [plugins] entries listed in "tomlPluginKeys" from libs.versions.toml
    → Remove "settingsInclude" entries from settings.gradle.kts (e.g., :server)
    → Remove "buildConfigFields" from androidApp/build.gradle.kts
    → Remove "iosInfoPlistKeys" from iosApp/iosApp/Info.plist

Step 4: Apply AI Provider Selection
  If AI is selected:
    → Set the chosen provider binding in AppModule.kt
      (single<AiProvider> { ClaudeProvider / GroqProvider / GeminiProvider })
    → Delete unused provider files from ai/providers/
    → Remove unused BuildConfig fields for the two unselected providers

Step 5: Apply Theme Customization
  If user configured theme options:
    → Run ThemeGenerator: compute full M3 tonal palette from seed color
      using material-color-utilities (Hct.fromInt → DynamicScheme → all color roles)
    → Write {pkg}/ui/theme/Color.kt   — hardcoded lightColorScheme + darkColorScheme
    → Write {pkg}/ui/theme/Theme.kt   — AppTheme composable
        Standard:    MaterialTheme(colorScheme, typography)
        Expressive:  MaterialTheme(colorScheme, typography, motionScheme = MotionScheme.expressive())
    → Write {pkg}/ui/theme/Typography.kt — FontFamily + all 15 M3 type roles
    → Run FontCopier: validate .ttf/.otf, copy to composeResources/font/
    → Edit App.kt: replace MaterialTheme { } with AppTheme { } + add import
  If no theme customization:
    → Skip — App.kt unchanged, no theme files generated

Step 6: Apply Sample Code Selection
  For each SELECTED feature where sample code is OFF:
    → Delete sample files listed in sampleCode.perFeature[id].files
    → Remove Screen objects from Screen.kt
    → Remove navigation branches + subclass() from AppNavigation.kt
    → Remove demo buttons from HomeScreen.kt
    → Remove koin bindings for sample ViewModels (e.g., AiViewModel)
  For each SELECTED feature where sample code is ON:
    → Keep all sample files — they serve as living documentation

Step 7: Clean Up
  → Remove .claude/ directory (skills not needed in generated project)
  → Remove docs/ folder
  → Remove scripts/setup.sh, scripts/install.sh
  → Remove AGENTS.md, Plugin.md, GENERATOR_PLAN.md
  → Replace README.md with generated README listing selected features + theme info
  → Delete .git/ directory (fresh start for user)

Step 8: Write Output
  → Copy from temp to user-selected directory

Step 9: Optional Build
  → Run ./gradlew :androidApp:assembleDebug
  → Stream output to IDE console tool window
  → Report success/failure with actionable error message
```

---

## Technical Stack

| Component | Technology |
|-----------|-----------|
| Plugin Framework | IntelliJ Platform SDK (Kotlin) |
| Build System | IntelliJ Platform Gradle Plugin 2.x |
| UI | Kotlin UI DSL / Swing |
| Template Source | GitHub Releases API (zip download) + bundled zip fallback |
| File Operations | Kotlin stdlib + java.nio |
| Text Processing | Regex + Kotlin string templates |
| Gradle Parsing | Custom line-based parser (simpler than AST for this use case) |
| M3 Color Generation | `material-color-utilities:0.10.0` (Google, Apache 2.0, ~200 KB) |

---

## Key Implementation Details

### 1. Safe File Deletion
Use the manifest JSON to know exactly which files belong to which feature. For shared files (e.g., `AppModule.kt`), use **selective line removal** rather than file deletion. Platform `actual` files are explicitly listed in the manifest per feature — never delete entire source-set directories for a feature removal.

### 2. Gradle File Surgery
`libs.versions.toml`, `composeApp/build.gradle.kts`, and `androidApp/build.gradle.kts` need surgical edits. Three strategies — no Kotlin/TOML AST parser needed:

- **`remove_import(symbol)`** — delete line matching `import .*symbol`
- **`remove_line(pattern)`** — delete first line matching regex (single `single<>` / `viewModel {}` / `implementation(libs.X)` lines)
- **`remove_block(triggerPattern)`** — delete trigger line + balanced-brace block after it using a char-by-char brace counter (~30 lines of Kotlin). Used for `ksp { }`, `android { }`, `single<ObservableSettings> { }` blocks

### 3. Koin Binding Removal — Two Files, Not One
Koin bindings live in **two different files** depending on the feature:

| Binding | File |
|---------|------|
| `HttpClient`, `ApiService`, `AiProvider`, `AiRepository`, `AppDatabase`, `AppPreferences` | `AppModule.kt` |
| `NotificationScheduler`, `ObservableSettings`, `PermissionController` | `PlatformModule.android.kt` / `PlatformModule.ios.kt` / `PlatformModule.jvm.kt` |

The manifest `"platformBindings"` field maps each platform to the bindings to remove from its `PlatformModule`. The engine edits the correct file per active platform.

### 4. KSP Processor Lines
KSP processors are declared as `add("kspAndroid", libs.room.compiler)` etc. — separate from regular `implementation()` deps. These are captured in `"kspProcessors"` in the manifest. When Room is removed, the engine removes:
```kotlin
add("kspAndroid", libs.room.compiler)
add("kspIosArm64", libs.room.compiler)
add("kspIosSimulatorArm64", libs.room.compiler)
```
and also removes `alias(libs.plugins.room)` from the plugins block and `ksp { arg("room.schemaLocation", ...) }` from the bottom of `composeApp/build.gradle.kts`.

### 5. `libs.versions.toml` Cleanup
The manifest `"tomlVersionKeys"` and `"tomlPluginKeys"` drive three separate removals in the toml:
- Remove matching line from `[versions]` table
- Remove all `[libraries]` lines that reference `libs.versions.X` for the removed version key
- Remove matching line from `[plugins]` table

Unused version catalog entries cause no build failure on their own, but leaving them creates confusion — clean them up.

### 6. Navigation Wiring Removal
`AppNavigation.kt` has three touch points per screen — all must be removed together:
- `subclass(Screen.Xxx::class)` in the `SerializersModule` block
- `is Screen.Xxx -> XxxScreen(...)` branch in the `Crossfade`
- `onNavigateToXxx = { backStack.add(Screen.Xxx) }` parameter in the `HomeScreen(...)` call

### 7. HomeScreen Signature
`HomeScreen` has one lambda parameter per navigation destination. Rather than surgically removing individual parameters (fragile with trailing commas), **regenerate the function signature** from a template string keyed on active features. This is safer and keeps the formatting clean.

### 8. PermissionController and `Info.plist`
`PermissionController` is an `expect` class — its `actual` implementations are in each platform source set. When permissions are removed:
- Delete all platform `actual` files (listed in manifest)
- Remove `PermissionController` from `platformBindings` in each `PlatformModule`
- Strip `"iosInfoPlistKeys"` entries from `iosApp/iosApp/Info.plist` to avoid App Store review failures for declared-but-unused permission strings

### 9. AI Feature and Ktor Dependency
`AI` declares `"requires": ["ktor"]`. The engine resolves this before processing:
- If AI is selected, `ktor` is force-kept regardless of the user's Networking toggle
- The wizard UI reflects this: Networking toggle is grayed out with tooltip "Required by AI Services"
- Ktor deps are owned entirely by the `ktor` feature entry — the `ai` feature entry has an empty `gradleDeps`

### 10. TemplateDownloader with Offline Fallback
```
1. Try: GET https://api.github.com/repos/Rohit-554/Catylst/releases/latest
         → parse assets[].browser_download_url for catylst-template.zip
         → download zip with progress dialog
2. Fallback (network error / timeout): load bundled zip from
         src/main/resources/templates/catylst-template.zip
         → show non-blocking warning: "Using bundled template vX.Y.Z"
3. Extract zip to system temp dir → proceed with pipeline
```
The bundled zip is updated at each plugin release via the same `sync-template.sh` approach.

### 12. Theme Customization — M3 Expressive, Colors, Fonts

**Wizard Step 5 — `ThemeCustomizationStep.kt`** has three sections:

**Section A — M3 Expressive Toggle**
- Checkbox: "Enable Material 3 Expressive" with an `(alpha)` badge
- When ON: `Theme.kt` is generated with `motionScheme = MotionScheme.expressive()` passed to `MaterialTheme`
- When OFF: standard `MaterialTheme(colorScheme, typography)`

**Section B — Color Scheme (Seed Color Approach)**
Why seed color instead of manual slot pickers: one color → full M3 tonal palette covering all 30+ role slots (primary, secondary, tertiary, error, neutral, neutral-variant) for both light and dark. Manual slot pickers would require the user to set values that must be harmonious — not practical in a wizard.

- Primary seed: `JColorChooser`-backed swatch, default `#6750A4` (M3 baseline purple)
- "Advanced" expandable panel: secondary + tertiary seed overrides
- Plugin bundles `material-color-utilities` as a compile dependency — `Hct.fromInt(seedArgb)` + `DynamicScheme` → extract all role colors
- Generated `Color.kt` contains only hardcoded `Color(0xFF...)` constants — zero runtime computation in the user's app

**Generated `Color.kt` shape:**
```kotlin
val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEADDFF),
    // ... all 30 roles
)
val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    // ... all 30 roles
)
```

**Section C — Font Family**
- **Simple tier** (default): one "Body font" file picker (`.ttf` / `.otf`) — applied to all Typography roles
- **Advanced tier** (expandable): three slot pickers — Display/Headline, Body, Label/Title — covering all 15 M3 type roles without overwhelming the user
- Drag-and-drop supported as secondary gesture on each picker panel
- "No custom font" → `Typography.kt` generated with M3 system defaults (no `FontFamily` declaration)
- `FontCopier.kt` validates MIME type, copies files to `composeResources/font/`, derives resource ID from filename

**Generated files (theme customized):**
```
{pkg}/ui/theme/Color.kt        ← hardcoded light + dark ColorScheme
{pkg}/ui/theme/Theme.kt        ← AppTheme composable
{pkg}/ui/theme/Typography.kt   ← FontFamily + Typography
composeResources/font/*.ttf    ← copied from user's picker selections
```

**Generated `Theme.kt` — standard variant:**
```kotlin
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, typography = AppTypography, content = content)
}
```

**Generated `Theme.kt` — M3 Expressive variant:**
```kotlin
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        motionScheme = MotionScheme.expressive(),
        content = content
    )
}
```

**`App.kt` edit** (applied only when theme is customized):
- `MaterialTheme { AppNavigation() }` → `AppTheme { AppNavigation() }`
- Import `{pkg}.ui.theme.AppTheme` added

**No new Gradle deps in the generated project** — font files in `composeResources/font/` are picked up automatically by Compose Resources; `material3` is already in `libs.versions.toml`. The `material-color-utilities` dep is only in the *plugin's* `build.gradle.kts`, not the generated project.

**`manifest.json` additions** — top-level `"theme"` key alongside `"features"` and `"sampleCode"`:
```json
"theme": {
  "expressiveMotion": false,
  "seedColor": "#6750A4",
  "advancedSeeds": {
    "secondary": null,
    "tertiary": null
  },
  "fonts": {
    "displayHeadline": null,
    "body": null,
    "labelTitle": null
  }
}
```

### 13. Sample Code Toggle
Each feature has two tiers of files:
- **Core files** — the actual feature implementation (always kept if feature is selected)
- **Sample files** — demo screens and ViewModels that show usage (kept only if user opts in)

The wizard shows a global "Include sample code" checkbox, plus per-feature overrides. When sample code is excluded, the core feature Koin bindings and platform files stay intact — only the demo screens and their navigation wiring are removed.

This lets users get a lean project with just the integration wiring, or a fully working demo they can run immediately.

---

## Testing Strategy

### Unit Tests (no IDE runtime needed)
| Test class | What it covers |
|------------|---------------|
| `PackageRenamerTest` | Known input strings → expected output; directory tree rename |
| `FeatureRemoverTest` | Fixture `AppModule.kt` → remove AI bindings → assert exact output |
| `DependencyCleanerTest` | Fixture `build.gradle.kts` → remove Room deps → assert exact output |
| `TomlCleanerTest` | Fixture `libs.versions.toml` → remove room entries → assert exact output |
| `NavigationEditorTest` | Fixture `AppNavigation.kt` → remove AiDemo screen → assert output |
| `ManifestLoaderTest` | Parse `manifest.json` → validate all feature IDs resolve, no dangling `requires` |
| `ThemeGeneratorTest` | Seed `0xFFE91E63` → assert `Color.kt` contains non-default pink palette values |
| `ThemeGeneratorExpressiveTest` | Expressive ON → assert `Theme.kt` contains `MotionScheme.expressive()` |
| `FontCopierTest` | Valid `.ttf` file → copied to `composeResources/font/`; invalid file → error thrown |

Fixtures live in `src/test/resources/fixtures/` as `.kt` / `.kts` / `.toml` files. Each test snapshots expected output — if the template changes, update both the fixture and the snapshot.

### Integration Tests (`@Tag("slow")`, CI only)
| Test | Asserts |
|------|---------|
| `GenerateAllOffTest` | All features off, no theme → `assembleDebug` passes; no `AiProvider`/`AppDatabase` references in output |
| `GenerateThemeCustomizedTest` | Seed `#E91E63` + custom font → `Color.kt`, `Theme.kt`, `Typography.kt` present; `App.kt` uses `AppTheme`; `assembleDebug` passes |
| `GenerateExpressiveTest` | M3 Expressive ON → `Theme.kt` contains `motionScheme`; `assembleDebug` passes |
| `GenerateNoThemeTest` | No theme customization → `App.kt` still uses `MaterialTheme`; no `ui/theme/` directory created |

---

## Plugin Distribution

1. **JetBrains Marketplace** — publish via `gradle publishPlugin`
2. **GitHub Releases** — attach `.zip` distribution for manual install
3. **Plugin ID:** `com.catylst.kmp.starter`

---

## Milestones

| Phase | Deliverable | Effort |
|-------|------------|--------|
| 1 | Plugin skeleton + `DirectoryProjectGenerator` wizard wiring + UI steps (feature checkboxes, package/app name inputs, sample-code toggle, AI provider) | 2 days |
| 1.5 | `ThemeCustomizationStep` UI + `ThemeGenerator` (seed → M3 palette via `material-color-utilities`) + `FontCopier`; pipeline Step 5 wired | 2 days |
| 2 | `TemplateDownloader` with offline fallback + `PackageRenamer` (port setup.sh logic) | 2 days |
| 3 | Feature removal engine: `FeatureRemover` (file deletion + manifest resolution), `DependencyCleaner` (Gradle + TOML surgery, KSP processors, plugin aliases), `PlatformModuleEditor` (platformBindings per platform) | 4 days |
| 4 | Navigation + HomeScreen editors; sample code toggle engine | 1 day |
| 5 | AI provider selector (swap provider binding, delete unused provider files, strip BuildConfig fields) + `BuildRunner` | 1 day |
| 6 | Unit tests + integration tests (fixture snapshots, generate-all-off, theme compile checks) | 2 days |
| 7 | Polish: icons, progress dialogs, error dialogs, offline warning, generated README with theme summary | 1 day |
| 8 | JetBrains Marketplace submission + GitHub Release with bundled `.zip` | 1 day |

**Total: ~16 days**

---

## Alternative: CLI-First Approach (Lower Effort)

If a JetBrains plugin feels like too much initial investment, an intermediate step is a **rich CLI tool** (Kotlin/Clikt or Node.js) that does the same generation pipeline. Users run:

```bash
npx create-catylst-app@latest
# or
java -jar catylst-generator.jar
```

This gives the same feature-selection experience without the IDE plugin complexity. The plugin can be built later as a thin wrapper around the CLI.

---

## Recommendation

**Build the JetBrains plugin directly** — the user base for this starter kit is Android Studio users, so an in-IDE wizard is the highest-value delivery. The generation logic is well-contained and testable. Start with Phase 1 (plugin skeleton + wizard) and iterate.
