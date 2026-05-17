---
name: bloom-navigate
description: Modify your generated project: swap the active AI provider, add notification channels, configure runtime permissions, or cleanly remove any built-in feature
---

# kmp-navigate

Use this skill when modifying or removing existing features in the Catylst project.

---

## Swap the AI Provider

The AI layer uses a strategy pattern — swap providers by changing one line in `AppModule.kt`.

Open `composeApp/src/commonMain/kotlin/io/jadu/catylst/di/AppModule.kt` and change the binding:

```kotlin
/* Claude (Anthropic) */
single<AiProvider> { ClaudeProvider(get(), AppConfig.claudeApiKey) }

/* Groq (Llama, Gemma, Mixtral) */
single<AiProvider> { GroqProvider(get(), AppConfig.groqApiKey) }

/* Google Gemini */
single<AiProvider> { GeminiProvider(get(), AppConfig.geminiApiKey) }
```

Only one line should be active at a time.

### API Key Setup

```bash
cp local.properties.example local.properties
```

Edit `local.properties`:
```properties
claude.api.key=sk-ant-...
groq.api.key=gsk_...
gemini.api.key=AIza...
```

| Provider | Auth                             | Base URL |
|----------|----------------------------------|----------|
| Claude   | `x-api-key` + `anthropic-version: 2023-06-01` | `https://api.anthropic.com/v1/messages` |
| Groq     | `Authorization: Bearer <key>`    | `https://api.groq.com/openai/v1/chat/completions` |
| Gemini   | `?key=<key>` query param         | `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent` |

### Adding a Custom Provider

1. Create `ai/providers/MyProvider.kt` implementing `AiProvider`.
2. Add key to `AppConfig.kt` and inject in `MainActivity.onCreate`.
3. Add `buildConfigField` in `androidApp/build.gradle.kts`.
4. Register: `single<AiProvider> { MyProvider(get(), AppConfig.myApiKey) }`.

---

## Configure Notifications

Architecture:
```
commonMain:  NotificationScheduler.kt (expect), AppNotificationChannel.kt
androidMain: NotificationScheduler.android.kt, NotificationWorker.kt
iosMain:     NotificationScheduler.ios.kt
desktopMain: NotificationScheduler.jvm.kt
```

### Add a Channel

Open `notifications/AppNotificationChannel.kt`:
```kotlin
enum class AppNotificationChannel(val channelId: String, val channelName: String) {
    REMINDERS("reminders", "Reminders"),
    MY_CHANNEL("my_channel", "My Channel"),   // <-- add here
}
```

Android creates it automatically. iOS maps `channelId` to `categoryIdentifier`.

### Schedule / Cancel

```kotlin
val scheduler: NotificationScheduler = koinInject()

scheduler.schedule(
    id = "notif-1",
    title = "Hello",
    body = "Body text",
    delaySeconds = 60L,
    channel = AppNotificationChannel.REMINDERS
)

scheduler.cancel(id = "notif-1")
```

---

## Add a Runtime Permission

Architecture:
```
commonMain:  Permission.kt (enum), PermissionState.kt, PermissionController.kt (expect)
androidMain: PermissionController.android.kt
iosMain:     PermissionController.ios.kt
```

**Step 1** — Add to `Permission.kt` enum:
```kotlin
MY_NEW_PERMISSION,
```

**Step 2** — Map on Android in `PermissionController.android.kt`:
```kotlin
Permission.MY_NEW_PERMISSION -> android.Manifest.permission.MY_ANDROID_PERMISSION
```

**Step 3** — Handle on iOS in `PermissionController.ios.kt` (`checkPermission` + `requestPermission`).

**Step 4** — Declare in `AndroidManifest.xml` and `Info.plist`.

### Use in a Composable

```kotlin
val controller = rememberPermissionController()
val state by controller.permissionState(Permission.MY_NEW_PERMISSION).collectAsState()

when (state) {
    PermissionState.Granted -> { /* proceed */ }
    PermissionState.Denied  -> Button(onClick = { controller.requestPermission(Permission.MY_NEW_PERMISSION) }) { Text("Grant") }
    else -> { }
}
```

---

## Remove a Feature

After any removal, run `./gradlew :androidApp:assembleDebug` to confirm no errors.

### Remove AI Services

1. Delete `ai/` directory and `config/AppConfig.kt`.
2. Delete `ui/screens/AiDemoScreen.kt` and `ui/viewmodel/AiViewModel.kt`.
3. In `AppModule.kt` — remove `AiProvider`, `AiRepository`, `AiViewModel` bindings and imports.
4. In `Screen.kt` / `AppNavigation.kt` / `HomeScreen.kt` — remove `Screen.AiDemo` and its wiring.
5. In `MainActivity.kt` — remove `AppConfig.*apiKey = BuildConfig.*` lines.
6. In `androidApp/build.gradle.kts` — remove the three `buildConfigField` lines and `buildConfig = true`.

### Remove Notifications

1. Delete `notifications/` across all source sets.
2. Delete `ui/screens/NotificationDemoScreen.kt`.
3. Remove `NotificationScheduler` from all `PlatformModule` files.
4. Remove `Screen.Notifications` from `Screen.kt`, `AppNavigation.kt`, `HomeScreen.kt`.
5. Remove `workmanager` from `composeApp/build.gradle.kts` and `libs.versions.toml`.
6. Remove `POST_NOTIFICATIONS` / `SCHEDULE_EXACT_ALARM` from `AndroidManifest.xml`.

### Remove Permissions

1. Delete `permissions/` across all source sets.
2. Delete `ui/screens/PermissionDemoScreen.kt`.
3. Remove `Screen.Permissions` from `Screen.kt`, `AppNavigation.kt`, `HomeScreen.kt`.
4. Remove permission `<uses-permission>` entries from `AndroidManifest.xml` and `Info.plist`.

### Remove Preferences

1. Delete `data/preferences/AppPreferences.kt` and `ui/screens/PreferencesDemoScreen.kt`.
2. Remove `AppPreferences(get())` from `AppModule.kt` and `ObservableSettings`/`Settings` from `PlatformModule`.
3. Remove `Screen.Preferences` from navigation files.
4. Remove `multiplatform-settings` from `build.gradle.kts` and `libs.versions.toml`.

### Remove Room Database

1. Delete `data/local/` across all source sets.
2. Remove `AppDatabase` binding from `AppModule.kt`.
3. Remove `room.runtime`, `sqlite.bundled`, `ksp`, `room` plugin from `build.gradle.kts` and `libs.versions.toml`.

### Remove Networking (Ktor)

1. Delete `network/` directory.
2. Remove `HttpClient` and `ApiService` from `AppModule.kt`.
3. Remove all `ktor.*` entries from `build.gradle.kts` and `libs.versions.toml`.

---

## General Debugging

After any change, find broken imports:
```bash
./gradlew :androidApp:assembleDebug 2>&1 | grep "error:"
```
Fix each reported import by removing the dead reference.
