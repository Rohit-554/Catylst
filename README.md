# Catylst — Kotlin Multiplatform Starter Kit

![Kotlin](https://img.shields.io/badge/Kotlin-2.3-7F52FF?logo=kotlin)
![AGP](https://img.shields.io/badge/AGP-9.x-3DDC84?logo=android)
![CMP](https://img.shields.io/badge/Compose%20Multiplatform-1.11-4285F4)
![License](https://img.shields.io/badge/License-MIT-green)

Production-ready KMP starter kit — Android, iOS, Desktop. Networking, persistence, DI, navigation, notifications, permissions, preferences, and provider-agnostic AI (Claude / Groq / Gemini) out of the box.

**Full documentation:** [`docs/index.html`](docs/index.html)

---

## Setup

```bash
git clone https://github.com/your-org/catylst.git myapp
cd myapp

# Rename the project to your package and app name (run once — then deletes itself)
bash scripts/setup.sh com.yourname.yourapp YourApp
```

After the script runs: **File → Sync Project with Gradle Files** in Android Studio, then build.

> The script replaces all `io.jadu.catylst` references, renames source directories, updates the app name in manifests and resources, removes the `docs/` folder, and deletes itself.

---

## AI Services

```bash
cp local.properties.example local.properties
# Add your key: claude.api.key / groq.api.key / gemini.api.key
```

Swap provider with one line in `composeApp/src/commonMain/.../di/AppModule.kt`:

```kotlin
single<AiProvider> { ClaudeProvider(get(), AppConfig.claudeApiKey) }
// single<AiProvider> { GroqProvider(get(), AppConfig.groqApiKey) }
// single<AiProvider> { GeminiProvider(get(), AppConfig.geminiApiKey) }
```

---

## Build

| Task | Command |
|------|---------|
| Android debug | `./gradlew :androidApp:assembleDebug` |
| Android release | `./gradlew :androidApp:assembleRelease` |
| iOS compile check | `./gradlew :composeApp:compileKotlinIosSimulatorArm64` |
| Desktop run | `./gradlew :composeApp:run` |
| Tests | `./gradlew :composeApp:test` |
| Regen Room | `./gradlew :composeApp:kspAndroidMain` |

---

## Claude Code Skills

| Skill | Purpose |
|-------|---------|
| `kmp-add-screen` | Add a screen end-to-end |
| `kmp-add-feature` | Entity → DAO → Repository → ViewModel → Screen |
| `kmp-ai-provider` | Swap or add an AI provider |
| `kmp-notifications` | Add channels, schedule notifications |
| `kmp-permissions` | Add a runtime permission |
| `kmp-remove-feature` | Strip any built-in feature you don't need |

---

## License

MIT — see [LICENSE](LICENSE).
