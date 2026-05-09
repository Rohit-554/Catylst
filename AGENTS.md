# AGENTS.md — Catylst KMP Starter Kit

Reference for AI agents. Read this before writing any code or running any command.
Full documentation (architecture, features, pitfalls): [`docs/index.html`](docs/index.html)

---

## Setup Check

If `scripts/setup.sh` exists → setup has NOT been run. Ask the user to run it first:
```bash
bash scripts/setup.sh com.yourname.yourapp YourApp
```
If the file does not exist → project is already renamed. Proceed normally.

---

## Module Structure

```
androidApp/   — Android entry point only (MainActivity, BuildConfig)
composeApp/   — ALL shared code
  src/
    commonMain/   — Kotlin + Compose shared across all platforms
    androidMain/  — actual implementations for Android
    iosMain/      — actual implementations for iOS (Kotlin/Native)
    desktopMain/  — actual implementations for Desktop JVM
```

**Rule:** Everything in `commonMain`. Only `expect`/`actual` touches platform source sets.

---

## Key Files

| File | Role |
|------|------|
| `composeApp/.../di/AppModule.kt` | All Koin bindings — start here |
| `composeApp/.../navigation/Screen.kt` | Sealed class of all screens |
| `composeApp/.../navigation/AppNavigation.kt` | Routing via Navigation3 |
| `composeApp/.../config/AppConfig.kt` | AI key holder (set before startKoin) |
| `androidApp/.../MainActivity.kt` | Android entry — keys injected here |

---

## Build Commands

```bash
./gradlew :androidApp:assembleDebug                        # Android debug APK
./gradlew :androidApp:assembleRelease                      # Android release APK
./gradlew :composeApp:compileKotlinIosSimulatorArm64       # iOS compile check
./gradlew :composeApp:test                                 # Common unit tests
./gradlew :composeApp:testDebugUnitTest                    # Android unit tests
./gradlew :composeApp:kspAndroidMain                       # Regen Room after entity changes
bash scripts/build.sh                                      # Android + iOS compile
bash scripts/test.sh                                       # All unit tests
```

---

## Android Device Tasks — use `android-cli` skill

Do not write raw `adb` commands. Invoke the `android-cli` Claude Code skill for:
- Install and launch debug APK on device/emulator
- Start emulator / create AVD
- Stream logcat filtered by package
- List connected devices

---

## Architecture Pattern

MVVM + Repository. ViewModels in `commonMain`, `StateFlow` for state, Koin for DI.

```
UI (screens/) → ViewModel (viewmodel/) → Repository (data/repository/, ai/) → Room / Ktor
```

Koin starts at: `MainActivity.onCreate` (Android), `MainViewController.kt` (iOS), `main.kt` (Desktop).

---

## Critical Notes

- **Navigation:** Use `rememberNavBackStack()` + `Crossfade`. Never `NavDisplay` — not KMP-compatible.
- **BuildConfig:** Lives in `:androidApp` only. Use `AppConfig` object in `commonMain` instead.
- **AI keys:** Set on `AppConfig` in `MainActivity.onCreate` before `startKoin` runs.
- **Room changes:** Run `./gradlew :composeApp:kspAndroidMain` after any Entity/DAO change.
- **iOS KN 2.x:** ObjC category methods — use `ClassName.Companion.method()` if top-level call fails.
- **buildConfigField:** Use `"\"${value}\""` format in `.kts` — bare quotes get stripped.
