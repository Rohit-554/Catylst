---
name: kmp-remove-feature
description: Cleanly strip any built-in Catylst feature you don't need — AI Services, Notifications, Permissions, Preferences, Room Database, or Networking — with exact file and Gradle steps
---

# kmp-remove-feature

Catylst is a starter kit. Remove features you don't need to keep the project lean.
Each section below is self-contained — only follow the section(s) for what you're removing.

After any removal, run:
```bash
./gradlew :androidApp:assembleDebug
```
to confirm no compilation errors remain.

---

## Remove AI Services (Claude / Groq / Gemini)

1. Delete the AI source tree:
   ```
   composeApp/src/commonMain/kotlin/io/jadu/catylst/ai/
   ```

2. Delete the config object:
   ```
   composeApp/src/commonMain/kotlin/io/jadu/catylst/config/AppConfig.kt
   ```

3. Delete the AI demo screen and ViewModel:
   ```
   composeApp/src/commonMain/kotlin/io/jadu/catylst/ui/screens/AiDemoScreen.kt
   composeApp/src/commonMain/kotlin/io/jadu/catylst/ui/viewmodel/AiViewModel.kt
   ```

4. In `AppModule.kt` — remove these lines:
   ```kotlin
   single<AiProvider> { ... }
   single { AiRepository(get()) }
   viewModel { AiViewModel(get()) }
   ```
   Also remove the imports for `AiProvider`, `AiRepository`, `AiViewModel`, `ClaudeProvider`,
   `GroqProvider`, `GeminiProvider`, `AppConfig`.

5. In `Screen.kt` — remove `Screen.AiDemo`.

6. In `AppNavigation.kt` — remove the `Screen.AiDemo` subclass registration and Crossfade branch.

7. In `HomeScreen.kt` — remove the `onNavigateToAiDemo` parameter and its `OutlinedButton`.

8. In `MainActivity.kt` — remove:
   ```kotlin
   AppConfig.claudeApiKey = BuildConfig.CLAUDE_API_KEY
   AppConfig.groqApiKey   = BuildConfig.GROQ_API_KEY
   AppConfig.geminiApiKey = BuildConfig.GEMINI_API_KEY
   ```
   Remove the `import io.jadu.catylst.config.AppConfig` line.

9. In `androidApp/build.gradle.kts` — remove:
   ```kotlin
   val localProps = Properties()
   val localPropsFile = rootProject.file("local.properties")
   if (localPropsFile.exists()) localPropsFile.inputStream().use { localProps.load(it) }
   ```
   And remove the three `buildConfigField` lines for `CLAUDE_API_KEY`, `GROQ_API_KEY`,
   `GEMINI_API_KEY`. Remove `buildConfig = true` from `buildFeatures` if nothing else uses it.
   Remove `import java.util.Properties` at the top.

---

## Remove Notifications

1. Delete all notification source sets:
   ```
   composeApp/src/commonMain/kotlin/io/jadu/catylst/notifications/
   composeApp/src/androidMain/kotlin/io/jadu/catylst/notifications/
   composeApp/src/iosMain/kotlin/io/jadu/catylst/notifications/
   composeApp/src/desktopMain/kotlin/io/jadu/catylst/notifications/
   ```

2. Delete the demo screen:
   ```
   composeApp/src/commonMain/kotlin/io/jadu/catylst/ui/screens/NotificationDemoScreen.kt
   ```

3. In all platform `PlatformModule` files — remove:
   ```kotlin
   single { NotificationScheduler(androidContext()) }   // android
   single { NotificationScheduler() }                   // ios / desktop
   ```

4. In `Screen.kt` — remove `Screen.Notifications`.

5. In `AppNavigation.kt` — remove `Screen.Notifications` subclass and Crossfade branch.

6. In `HomeScreen.kt` — remove `onNavigateToNotifications` parameter and button.

7. In `composeApp/build.gradle.kts` — remove from `androidMain.dependencies`:
   ```kotlin
   implementation(libs.workmanager)
   ```
   Remove `workmanager` version from `libs.versions.toml` and the alias from `[libraries]`.

8. In `androidApp/src/main/AndroidManifest.xml` — remove:
   ```xml
   <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
   <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
   <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
   ```
   Also remove the `WorkManagerInitializer` `<provider>` block if present.

---

## Remove Permissions

1. Delete all permission source sets:
   ```
   composeApp/src/commonMain/kotlin/io/jadu/catylst/permissions/
   composeApp/src/androidMain/kotlin/io/jadu/catylst/permissions/
   composeApp/src/iosMain/kotlin/io/jadu/catylst/permissions/
   ```

2. Delete the demo screen:
   ```
   composeApp/src/commonMain/kotlin/io/jadu/catylst/ui/screens/PermissionDemoScreen.kt
   ```

3. In `Screen.kt` — remove `Screen.Permissions`.

4. In `AppNavigation.kt` — remove `Screen.Permissions` subclass and Crossfade branch.

5. In `HomeScreen.kt` — remove `onNavigateToPermissions` parameter and button.

6. In `androidApp/src/main/AndroidManifest.xml` — remove permission `<uses-permission>` entries
   that are no longer needed.

7. In `iosApp/iosApp/Info.plist` — remove `NSUsageDescription` keys for removed permissions.

---

## Remove Preferences (Multiplatform Settings)

1. Delete:
   ```
   composeApp/src/commonMain/kotlin/io/jadu/catylst/data/preferences/AppPreferences.kt
   composeApp/src/commonMain/kotlin/io/jadu/catylst/ui/screens/PreferencesDemoScreen.kt
   ```

2. In `AppModule.kt` — remove:
   ```kotlin
   single { AppPreferences(get()) }
   ```

3. In all platform `PlatformModule` files — remove the `ObservableSettings` / `Settings` binding.

4. In `Screen.kt` — remove `Screen.Preferences`.

5. In `AppNavigation.kt` — remove `Screen.Preferences` subclass and Crossfade branch.

6. In `HomeScreen.kt` — remove `onNavigateToPreferences` parameter and button.

7. In `composeApp/build.gradle.kts` — remove:
   ```kotlin
   implementation(libs.multiplatform.settings)
   implementation(libs.multiplatform.settings.noarg)   // if present
   ```

8. In `libs.versions.toml` — remove `multiplatform-settings` version and library aliases.

---

## Remove Room Database

1. Delete all Room source:
   ```
   composeApp/src/commonMain/kotlin/io/jadu/catylst/data/local/
   composeApp/src/androidMain/kotlin/io/jadu/catylst/data/local/   (if exists)
   composeApp/src/iosMain/kotlin/io/jadu/catylst/data/local/       (if exists)
   ```

2. In `AppModule.kt` — remove:
   ```kotlin
   single<AppDatabase> { createAppDatabase() }
   ```
   Remove imports for `AppDatabase` and `createAppDatabase`.

3. In `composeApp/build.gradle.kts` — remove from the relevant source sets:
   ```kotlin
   implementation(libs.room.runtime)
   implementation(libs.sqlite.bundled)
   ```
   Remove KSP compiler calls:
   ```kotlin
   add("kspAndroidMain", libs.room.compiler)
   add("kspIosArm64Main", libs.room.compiler)
   add("kspIosSimulatorArm64Main", libs.room.compiler)
   ```
   Remove plugin aliases from the `plugins {}` block:
   ```kotlin
   alias(libs.plugins.ksp)
   alias(libs.plugins.room)
   ```
   Remove the `room { schemaDirectory(...) }` block if present.

4. In `libs.versions.toml` — remove `room`, `ksp`, `sqliteBundled` versions and all related
   library/plugin aliases.

---

## Remove Networking (Ktor + ApiService)

1. Delete:
   ```
   composeApp/src/commonMain/kotlin/io/jadu/catylst/network/
   ```

2. In `AppModule.kt` — remove:
   ```kotlin
   single<HttpClient> { createHttpClient() }
   single { ApiService(get()) }
   ```
   Remove imports for `HttpClient`, `ApiService`, `createHttpClient`.

3. In `composeApp/build.gradle.kts` — remove all `ktor.*` dependency entries from all source sets.

4. In `libs.versions.toml` — remove the `ktor` version and all `ktor-*` library aliases.

---

## General Rule

After deleting files, search for remaining broken imports:

```bash
./gradlew :androidApp:assembleDebug 2>&1 | grep "error:"
```

Fix each reported import by removing the dead reference from the file shown.
When the build is clean, the removal is complete.
