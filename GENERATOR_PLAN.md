# Catylst KMP Generator Platform

> A web platform similar to https://kmp.jetbrains.com/ — developers configure a KMP project (platforms, features, project name/package) and download a ready-to-build customized zip of the Catylst starter kit.

---

## Architecture

**Single Next.js app** inside `generator/` subdirectory of the Catylst repo.
- Next.js API routes = backend (no separate server, no CORS)
- Template bundled as a static snapshot in `generator/template/` (committed, no git-clone-at-runtime)
- `archiver` streams zip directly to browser — no temp files

---

## Directory Structure

```
Catylst/
  generator/
    template/              ← committed copy of Catylst source (exclude .git, build/, .gradle/)
    src/
      app/
        page.tsx           ← generator UI (step wizard)
        layout.tsx
        globals.css
        api/generate/
          route.ts         ← POST handler; validates input, runs processor, streams zip
      components/
        PlatformSelector.tsx
        FeatureToggle.tsx
        ProjectConfig.tsx
        GenerateButton.tsx
      lib/
        types.ts           ← GenerateRequest interface
        featureManifest.ts ← declarative map: feature → files to delete + surgical edits
        processor/
          index.ts         ← orchestrator
          packageRenamer.ts   ← ports setup.sh logic to TS
          featureStripper.ts  ← deletes files per manifest
          fileEditor.ts       ← surgical edit engine (remove_line, remove_block, remove_import)
          gradleEditor.ts     ← strips dependency blocks from build.gradle.kts
          zipBuilder.ts       ← archiver streaming
    scripts/
      sync-template.sh     ← rsync ../  template/ (run when Catylst template updates)
    package.json
    tsconfig.json
    tailwind.config.ts
    next.config.ts
```

---

## UI — 4-Step Wizard

**Step 1: Project Details**
- App Name (PascalCase, e.g. `MyApp`)
- Package Name (reverse-domain, e.g. `com.alice.myapp`)

**Step 2: Platforms**
- Android (checkbox + icon)
- iOS (checkbox + icon)
- Desktop (checkbox + icon)
- At least one required

**Step 3: Features** (6 toggleable cards with descriptions)
- AI Services — if ON, force-enables Networking (shown with tooltip)
- Notifications
- Permissions
- Room Database
- Networking
- Preferences

**Step 4: AI Provider** (shown only if AI Services is ON)
- Claude (default), Groq, Gemini

**Generate button** → `POST /api/generate` → browser triggers zip download

---

## API Contract

```typescript
// POST /api/generate
interface GenerateRequest {
  project: {
    appName: string;
    packageName: string;
    aiProvider: "claude" | "groq" | "gemini";
  };
  platforms: {
    android: boolean;
    ios: boolean;
    desktop: boolean;
  };
  features: {
    ai: boolean;
    notifications: boolean;
    permissions: boolean;
    roomDatabase: boolean;
    networking: boolean;
    preferences: boolean;
  };
}
// Response: 200 application/zip stream  OR  400 { error, details }
```

**Validation rules:**
- `appName` → `/^[A-Za-z][A-Za-z0-9]*$/`
- `packageName` → `/^[a-z][a-z0-9]*(\.[a-z][a-z0-9]*){1,}$/`
- At least one platform must be true
- `features.networking` must be true if `features.ai` is true

---

## Feature → Transformation Manifest

### AI Services OFF
**Delete:**
```
ai/
config/AppConfig.kt
ui/screens/AiDemoScreen.kt
ui/viewmodel/AiViewModel.kt
```

**Edit `di/AppModule.kt`:**
- Remove imports: `AiProvider`, `AiRepository`, `ClaudeProvider`, `GeminiProvider`, `GroqProvider`, `AppConfig`, `AiViewModel`
- Remove lines: `single<AiProvider> { ... }`, `single { AiRepository(get()) }`, `viewModel { AiViewModel(get()) }`

**Edit `navigation/Screen.kt`:** remove `AiDemo` entry

**Edit `navigation/AppNavigation.kt`:** remove `AiDemoScreen` import, `subclass(Screen.AiDemo)`, `onNavigateToAiDemo` param, `is Screen.AiDemo` branch

**Edit `ui/screens/HomeScreen.kt`:** remove `onNavigateToAiDemo` param + button

**Gradle:** remove Ktor deps only if Networking is also OFF

---

### Notifications OFF
**Delete:**
```
notifications/               (all source sets)
ui/screens/NotificationDemoScreen.kt
androidMain/.../NotificationScheduler.android.kt
androidMain/.../NotificationWorker.kt
iosMain/.../NotificationScheduler.ios.kt
desktopMain/.../NotificationScheduler.jvm.kt
```

**Edit AppModule / PlatformModules:** remove `NotificationScheduler` bindings

**Edit Screen.kt / AppNavigation.kt / HomeScreen.kt:** remove Notifications references

**Gradle:** remove `libs.workmanager` from androidMain deps

---

### Permissions OFF
**Delete:**
```
permissions/                 (all source sets)
ui/screens/PermissionDemoScreen.kt
```

**Edit Screen.kt / AppNavigation.kt / HomeScreen.kt:** remove Permissions references

**Edit `iosApp/iosApp/Info.plist`:** remove `NSLocationWhenInUseUsageDescription`, `NSCameraUsageDescription`, etc.

---

### Room Database OFF
**Delete:**
```
data/local/                  (all source sets)
```

**Edit AppModule.kt:** remove `AppDatabase` import + `single<AppDatabase>` binding

**Gradle:** remove:
- `libs.room.runtime`, `libs.sqlite.bundled`
- `kspAndroid(libs.room.compiler)`
- `kspIosArm64(libs.room.compiler)`, `kspIosSimulatorArm64(libs.room.compiler)`
- `alias(libs.plugins.room)` from plugins block
- `ksp { arg("room.schemaLocation", ...) }` block

---

### Networking OFF *(and AI also off)*
**Delete:**
```
network/
```

**Edit AppModule.kt:** remove `ApiService`, `createHttpClient`, `HttpClient` imports + their `single { }` lines

**Gradle:** remove Ktor client deps (safe only when AI is also OFF — AI reuses the same HttpClient)

---

### Preferences OFF
**Delete:**
```
data/preferences/
ui/screens/PreferencesDemoScreen.kt
```

**Edit AppModule.kt:** remove `AppPreferences` import + `single { AppPreferences(get()) }` line

**Edit PlatformModules (all platforms):** remove `ObservableSettings` bindings

**Edit Screen.kt / AppNavigation.kt / HomeScreen.kt:** remove Preferences references

**Gradle:** remove `libs.multiplatform.settings*` deps

---

### Desktop Platform OFF
**Delete:** `composeApp/src/desktopMain/`

**Gradle:** remove `jvm("desktop")` target + desktopMain dependency blocks

---

### iOS Platform OFF
**Delete:** `composeApp/src/iosMain/`, `iosApp/`

**Gradle:** remove `listOf(iosArm64(), iosSimulatorArm64())` block, iosMain deps, KSP iOS processors

---

### Android Platform OFF
**Delete:** `composeApp/src/androidMain/`, `androidApp/`

**settings.gradle.kts:** remove `include(":androidApp")`

**Gradle:** remove `alias(libs.plugins.androidLibrary)`, `android { }` block, androidMain deps, KSP Android processor

---

## Dependency Constraint Matrix

| Feature | Requires |
|---------|----------|
| AI Services | Networking (shares HttpClient) |
| Notifications | — |
| Permissions | — |
| Room Database | — |
| Networking | — |
| Preferences | — |

When AI Services is toggled ON → Networking is force-enabled and grayed out with tooltip: *"Required by AI Services"*

---

## Surgical Edit Engine (`fileEditor.ts`)

Three strategies — no Kotlin AST parser needed:

| Strategy | Description | Used for |
|----------|-------------|----------|
| `remove_import(symbol)` | Delete line matching `import .*symbol` | All feature removals |
| `remove_line(pattern)` | Delete first line matching regex | Single `single<>` / `viewModel {}` lines |
| `remove_block(triggerPattern)` | Delete trigger line + balanced-brace block (char-by-char counter) | `ksp { }` in Gradle, `android { }` block, `single<ObservableSettings>` |

`HomeScreen.kt` signature: **regenerate from a template string** keyed on active features — more reliable than surgical removal of a multi-parameter function.

---

## Package Renamer (`packageRenamer.ts`)

Port of `scripts/setup.sh` to TypeScript:
1. Replace `io.jadu.catylst` → `com.user.appname` in all text files
2. Rename directory tree `io/jadu/catylst` → `com/user/appname`
3. Replace `Catylst` → `AppName` in string resources, manifests, plist, settings.gradle.kts

Run **after** feature stripping so deleted files don't need renaming.

---

## Template Sync Script

```bash
# generator/scripts/sync-template.sh
rsync -av --delete \
  --exclude='.git' --exclude='.gradle' --exclude='.kotlin' \
  --exclude='build/' --exclude='local.properties' \
  --exclude='generator/' \
  ../  template/
```

Run this and commit whenever the Catylst template is updated.

---

## Implementation Phases

### Phase 1 — Generator Core (no UI)
- Set up Next.js project in `generator/`
- Install: `archiver`, `fast-glob`, `fs-extra`, `@types/*`
- Write all `lib/` files: `types.ts`, `featureManifest.ts`, `fileEditor.ts`, `gradleEditor.ts`, `packageRenamer.ts`, `featureStripper.ts`, `zipBuilder.ts`, `processor/index.ts`
- Sync template via `sync-template.sh`
- CLI smoke test: generate with all features → unzip → verify

### Phase 2 — API Route
- Write `app/api/generate/route.ts`
- Validate input, call processor, stream zip with `Content-Disposition: attachment; filename="AppName.zip"`

### Phase 3 — Frontend UI
- Build step wizard with Tailwind (JetBrains-inspired: clean whites, grey cards, blue accent)
- Implement AI → Networking force-constraint in UI state
- Wire Generate button to API, trigger browser download via Blob URL

### Phase 4 — Deployment
- Add `vercel.json` for Vercel one-click deployment
- Add `Dockerfile` for self-hosting
- Add `generator/README.md` documenting template sync workflow

---

## Critical Files to Edit

| File | Reason |
|------|--------|
| `composeApp/build.gradle.kts` | Dep/plugin stripping — most complex target |
| `di/AppModule.kt` | Central DI hub — every feature touches this |
| `navigation/AppNavigation.kt` | All screen registrations + nav params |
| `navigation/Screen.kt` | Sealed interface entries per feature |
| `ui/screens/HomeScreen.kt` | Navigation parameter signature |
| `androidApp/AndroidManifest.xml` | Permission declarations |
| `iosApp/iosApp/Info.plist` | iOS permission usage descriptions |

---

## Verification Checklist

- [ ] Generate zip with **all features** → `./gradlew :androidApp:assembleDebug` succeeds
- [ ] Generate zip with **no features** → gradle succeeds, HomeScreen renders alone
- [ ] Generate with AI off + Networking on → Ktor deps present, `ai/` directory absent
- [ ] Generate with iOS off → no `iosMain/`, no `iosApp/`, no iOS gradle targets
- [ ] Package rename `com.test.sample` + `SampleApp` → all occurrences replaced correctly
- [ ] Browser: click Generate → download starts within 2s, zip is valid and unzips cleanly
