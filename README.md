# Catylst

![Kotlin](https://img.shields.io/badge/Kotlin-2.3-7F52FF?style=flat-square&logo=kotlin&logoColor=white)
![AGP](https://img.shields.io/badge/AGP-9.0-3DDC84?style=flat-square&logo=android&logoColor=white)
![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.11-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)
![Room](https://img.shields.io/badge/Room-3.1-3DDC84?style=flat-square&logo=android&logoColor=white)
![Navigation3](https://img.shields.io/badge/Navigation-3.0-7F52FF?style=flat-square&logo=kotlin&logoColor=white)
![Material3](https://img.shields.io/badge/Material-M3%20Expressive-F06292?style=flat-square)
![npm](https://img.shields.io/npm/v/catylst?style=flat-square&logo=npm&color=CB3837)
![License](https://img.shields.io/badge/License-MIT-brightgreen?style=flat-square)

Kotlin Multiplatform starter kit for Android, iOS, and Desktop. Ships with AI integration, push notifications, runtime permissions, local database, networking, and modern navigation — all pre-wired and ready to build.

**Docs:** [rohit-554.github.io/Catylst](https://rohit-554.github.io/Catylst/)

---

## Demo (plugin) - Coming Soon
<img width="1301" height="936" alt="Screenshot 2026-05-24 at 12 39 44 AM" src="https://github.com/user-attachments/assets/8c1ee989-3d86-4a76-94cb-42879698b360" />


## Who this is for

- Developers learning KMP who want a working app to read and modify, not an empty scaffold
- Developers shipping MVPs who need AI features (Claude / Groq / Gemini) without wiring providers from scratch
- Teams who want Navigation3, Material3 Expressive, Room 3.1, and Ktor 3 pre-configured

---

## Quick start

Install the CLI and generate a project:

```bash
npm install -g catylst
catylst --interactive
```

Or use the install script:

```bash
bash <(curl -sSL https://raw.githubusercontent.com/Rohit-554/Catylst/main/scripts/install.sh)
catylst --interactive
```

---

## What is included

| Feature | Library |
|---------|---------|
| AI integration | Claude / Groq / Gemini — strategy pattern, swap in one line |
| Push notifications | WorkManager (Android) / UNUserNotificationCenter (iOS) |
| Runtime permissions | Camera / Location / Notifications — expect/actual |
| Local database | Room 3.1 + KSP + SQLite bundled |
| Preferences | multiplatform-settings |
| HTTP client | Ktor 3 |
| Backend | Ktor server module |
| Navigation | Navigation3 — type-safe, no string routes |
| Theme | Material 3 Expressive — seed color, light + dark |
| Dependency injection | Koin multiplatform |

---

## AI provider setup

```bash
cp local.properties.example local.properties
```

Edit `local.properties` and add your key. Swap the active provider in `di/AppModule.kt`:

```kotlin
single<AiProvider> { ClaudeProvider(get(), AppConfig.claudeApiKey) }
// single<AiProvider> { GroqProvider(get(), AppConfig.groqApiKey) }
// single<AiProvider> { GeminiProvider(get(), AppConfig.geminiApiKey) }
```

---

## Build commands

| Target | Command |
|--------|---------|
| Android debug | `./gradlew :androidApp:assembleDebug` |
| Android release | `./gradlew :androidApp:assembleRelease` |
| iOS compile check | `./gradlew :composeApp:compileKotlinIosSimulatorArm64` |
| Desktop | `./gradlew :composeApp:run` |
| Tests | `./gradlew :composeApp:test` |
| Regenerate Room | `./gradlew :composeApp:kspAndroidMain` |

---

## Agent skills

Skills install into `.claude/skills/` of the generated project and are picked up automatically by Claude Code.

| Skill | Purpose |
|-------|---------|
| `bloom-build` | Add screens end-to-end — composable, navigation, ViewModel, Room Entity/DAO/Repository, Koin DI |
| `bloom-navigate` | Modify the project — swap AI provider, configure notifications/permissions, remove features |

---

## License

MIT — see [LICENSE](LICENSE).
