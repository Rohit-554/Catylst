# Catylst - Kotlin Multiplatform Starter Kit

![Kotlin](https://img.shields.io/badge/Kotlin-2.3-7F52FF?logo=kotlin)
![AGP](https://img.shields.io/badge/AGP-9.x-3DDC84?logo=android)
![CMP](https://img.shields.io/badge/Compose%20Multiplatform-1.11-4285F4)
![License](https://img.shields.io/badge/License-MIT-green)

**Batteries-included KMP starter kit for solo developers and small teams.** Android, iOS, Desktop from a single codebase — with working AI integration, push notifications, runtime permissions, local persistence, and modern navigation out of the box.

**Who this is for:**
- **Beginners** learning KMP who want a *working app* they can run, read, and modify — not an empty scaffold
- **Intermediate developers** shipping MVPs and side projects who need AI-powered features (Claude / Groq / Gemini) without wiring up providers from scratch
- **Teams** who want the latest stable versions (Navigation3, Material3 Expressive, Room 3, Ktor 3) pre-configured and battle-tested

If you need an enterprise-grade multi-module template with 30+ Gradle modules, heavy CI/CD infrastructure, and fintech-specific abstractions, check out [openMF/kmp-project-template](https://github.com/openMF/kmp-project-template) instead.

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
