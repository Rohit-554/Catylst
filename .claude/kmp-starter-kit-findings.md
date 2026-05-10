# KMP Starter Kit — Findings from Chitti Project

This document captures all the hard-won knowledge from building the Chitti project (AGP 9 + Kotlin 2.3 + Compose Multiplatform) that should be preserved for the KMP starter kit.

---

## 0. Contest Requirements Checklist (Kotlin Foundation 2026)

| Requirement | Status | Section |
|---|---|---|
| Android + iOS targets | ✅ Covered | §3, §17 |
| AGP 9 | ✅ Covered | §1 |
| Networking (REST / AI services) | ✅ REST (Ktor) covered. 🟡 AI services — phase/6-ai-services | §2, §16-old |
| Local database | ✅ Room KMP | §2 |
| Local preferences | ✅ multiplatform-settings | §15 |
| Notifications | ✅ expect/actual + WorkManager (Android) | §13 |
| Permissions | ✅ expect/actual (no moko) | §14 |
| Testing | 🟡 deps wired, tests deferred to last phase | §16 |
| AGENTS.md | ⚠️ not yet created | §16 |
| SKILLS for development | ⚠️ not yet created | §16 |
| Agent build/test workflows | ⚠️ not yet created | §16 |
| MIT License | ⚠️ not yet created | §18 |
| Blog posts (DI, interop, testing) | ⚠️ outlines only, not yet written | §18 |
| Web / Desktop / Backend (optional) | ✅ Covered | §17 |

---

---

## 1. AGP 9 Migration Findings (from p1_agp9 branch)

### Official Skill Reference
Use the official JetBrains skill: `kotlin-tooling-agp9-migration` from https://github.com/Kotlin/kotlin-agent-skills
Do NOT maintain a custom AGP 9 migration skill — the official one is more comprehensive.

### What the Official Skill Misses (Our Additive Findings)

#### A. `packaging` and `buildTypes` Must Move to `:androidApp`
The official skill mentions "no build variants" in the KMP library plugin but doesn't explicitly tell you to move these blocks. During migration:

```kotlin
// OLD in composeApp/build.gradle.kts (remove these):
android {
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
    buildTypes { getByName("release") { isMinifyEnabled = false } }
}

// NEW in androidApp/build.gradle.kts (add these):
android {
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
    buildTypes { getByName("release") { isMinifyEnabled = false } }
}
```

#### B. Java 17 is Recommended (not just 11)
AGP 9 defaults to Java 11, but many modern KMP libraries expect Java 17:

```kotlin
android {
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
```

#### C. `settings.gradle.kts` `mavenContent` is Critical
The AGP 9 KMP library plugin artifacts require specific group patterns:

```kotlin
pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
```

#### D. `androidMain.dependencies` Can Be Re-added After Migration
The official skill says to remove `androidMain.dependencies` from `:composeApp`. This is correct during migration, but you can re-add it later for shared-module-specific Android dependencies:

```kotlin
kotlin {
    sourceSets {
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)   // Android-specific HTTP engine
            implementation(libs.koin.android)          // Android-specific Koin
        }
    }
}
```

---

## 2. Dependency Version Compatibility Matrix (from p2_dependencySetup branch)

### Known-working stack for AGP 9.1 + Kotlin 2.3

| Dependency | Version | Notes |
|---|---|---|
| AGP | 9.1.1 | Latest stable |
| Kotlin | 2.3.21 | AGP 9 requires KGP 2.2.10+ |
| Gradle | 9.5.0 | Compatible with AGP 9.1 |
| KSP | 2.3.7 | Version prefix still tracks Kotlin minor (2.3.x → ksp 2.3.x). Full independent versioning planned but not yet active. First AGP-9-compatible: 2.3.1+ |
| Compose Multiplatform | 1.11.0 | Latest stable (April 2026) |
| Room (KMP) | 3.0.0-alpha01 | **New artifact: `androidx.room3`** — full KMP rewrite (Mar 2026) |
| SQLite (bundled) | 2.6.2 | **Required** for Room KMP on iOS; latest stable (Nov 2025) |
| Koin | 4.1.1 | Stable (Apr 9 2026). BOM available |
| Ktor | 3.4.3 | Stable (Apr 22 2026) |
| Navigation3 | 1.1.1 | Use `navigation3-runtime` only for KMP — see pattern below |
| Kotlin Serialization | 2.3.21 | Same as Kotlin version |
| AndroidX Lifecycle | 2.10.0 | KMP-aware JetBrains fork |
| AndroidX Activity | 1.13.0 | `activity-compose` for KMP |
| Material3 | 1.11.0 | `org.jetbrains.compose.material3` — stable with CMP 1.11.0 |

### KSP Critical Notes

- **KSP 2.3.1+** is required for AGP 9.0 built-in Kotlin support
- **KSP issue #2729**: `Using kotlin.sourceSets DSL to add Kotlin sources is not allowed with built-in Kotlin`
  - Workaround: Add `android.disallowKotlinSourceSets=false` to `gradle.properties`
- **KSP 2.3.5** fixed circular dependency between KSP and KAPT in AGP 9.0
- **KSP 2.3.6** fixed generated Java sources being ignored with Android Kotlin Multiplatform
- KSP version numbering changed: old `2.1.20-1.0.32` → new `2.3.7` (tracks Kotlin minor version prefix)

### Room KMP Setup Pattern

> ⚠️ **Room 3.0 breaking change**: Artifact moved from `androidx.room` → `androidx.room3`. New gradle plugin and compiler coordinates:
> - Runtime: `androidx.room3:room3-runtime:3.0.0-alpha01`
> - Compiler: `androidx.room3:room3-compiler:3.0.0-alpha01`
> - Plugin: `androidx.room3:room3-gradle-plugin:3.0.0-alpha01`

```kotlin
// composeApp/build.gradle.kts
plugins {
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)  // now points to room3-gradle-plugin
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.room.runtime)
            implementation(libs.sqlite.bundled)  // REQUIRED for iOS
        }
    }
}

dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
}

room {
    schemaDirectory("$projectDir/schemas")
}
```

### Koin Setup Pattern

```kotlin
// commonMain
dependencies {
    implementation(libs.koin.core)
    implementation(libs.koin.compose)
}

// androidMain
dependencies {
    implementation(libs.koin.android)
}
```

### Ktor Setup Pattern

```kotlin
// commonMain
dependencies {
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)
}

// androidMain
dependencies {
    implementation(libs.ktor.client.okhttp)
}

// iosMain
dependencies {
    implementation(libs.ktor.client.darwin)
}
```

### Ktor HttpClient Factory (commonMain)

No `expect/actual` needed — engine resolves automatically per platform (OkHttp on Android, Darwin on iOS).

```kotlin
fun createHttpClient() = HttpClient {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = true
        })
    }
    install(Logging) {
        level = LogLevel.ALL  // use LogLevel.NONE in release
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 30_000
        connectTimeoutMillis = 15_000
    }
}
```

---

## 3. Project Structure Pattern

```
project/
├── androidApp/              # Android entry point only
│   ├── build.gradle.kts     # com.android.application + org.jetbrains.compose + composeCompiler
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── kotlin/.../MainActivity.kt
│       └── res/
├── composeApp/              # KMP shared module
│   ├── build.gradle.kts     # kotlinMultiplatform + androidKmpLibrary + compose
│   └── src/
│       ├── commonMain/      # Shared UI, ViewModels, Repositories, DAOs
│       ├── androidMain/     # Platform actuals (Database builder, HTTP client)
│       └── iosMain/         # Platform actuals
├── iosApp/                  # Xcode project
├── gradle/
│   └── libs.versions.toml   # Centralized version catalog
└── build.gradle.kts         # Root with all plugins apply false
```

---

## 4. Key Code Patterns to Preserve

### Room KMP with `expect`/`actual`

```kotlin
// commonMain
@Database(entities = [Entity::class], version = 1)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): EntityDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase>

expect fun createAppDatabaseBuilder(): RoomDatabase.Builder<AppDatabase>

fun createAppDatabase(): AppDatabase {
    return createAppDatabaseBuilder()
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}

// androidMain
actual fun createAppDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val dbFile = appContext.getDatabasePath("app.db")
    return Room.databaseBuilder(appContext, AppDatabase::class.java, dbFile.absolutePath)
}

// iosMain
@OptIn(ExperimentalForeignApi::class)
actual fun createAppDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val dbFile = documentDirectory() + "/app.db"
    return Room.databaseBuilder<AppDatabase>(name = dbFile)
}

// iosMain — documentDirectory() helper (required for iOS Room path)
@OptIn(ExperimentalForeignApi::class)
fun documentDirectory(): String {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null
    )
    return requireNotNull(documentDirectory?.path)
}
```

### Koin Initialization (Platform-specific)

```kotlin
// Android — in MainActivity
startKoin {
    androidLogger()
    androidContext(applicationContext)
    modules(appModule)
}

// iOS — in MainViewController
fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    modules(appModule)
}

fun MainViewController() = ComposeUIViewController(
    configure = { initKoin() }
) { App() }
```

### Navigation3 with `NavRuntime` + `Crossfade` (KMP-compatible)

> ⚠️ **`NavDisplay`** (from `navigation3-ui`) has **no KMP targets** — Android/JVM only.
> For CMP (iOS + Android), use `navigation3-runtime` only and implement display manually with `Crossfade`.

**Artifact (commonMain only):**
```toml
# libs.versions.toml
navigation3-runtime = { module = "org.jetbrains.androidx.navigation3:navigation3-runtime", version.ref = "navigation3" }
```

```kotlin
// commonMain
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.compose.animation.Crossfade
import kotlinx.serialization.Serializable

@Serializable
sealed interface Screen : NavKey {
    @Serializable data object Home : Screen
    @Serializable data class Detail(val id: Long) : Screen
}

@Composable
fun AppNavigation() {
    val backStack = rememberNavBackStack(Screen.Home)
    Crossfade(targetState = backStack.lastOrNull() ?: Screen.Home) { screen ->
        when (screen) {
            is Screen.Home -> HomeScreen(onNavigate = { backStack.add(Screen.Detail(it)) })
            is Screen.Detail -> DetailScreen(id = screen.id, onBack = { backStack.removeLastOrNull() })
        }
    }
}
```

### Platform Config with `expect object`

```kotlin
// commonMain
expect object AppConfig {
    val apiKey: String
}

// androidMain
actual object AppConfig {
    actual val apiKey: String = BuildConfig.API_KEY
}

// iosMain — set key via native iOS mechanism outside Kotlin (xcconfig etc.)
actual object AppConfig {
    actual val apiKey: String = ""
}
```

---

## 5. Material 3 Expressive Theme

Using `MaterialExpressiveTheme` (experimental):

```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialExpressiveTheme {
        content()
    }
}
```

Custom color tokens follow Material 3 tonal palette convention (0-100 scale):
- `StudyPurple0` through `StudyPurple100`
- `StudyIndigo0` through `StudyIndigo100`
- `StudyTeal0` through `StudyTeal100`

---

## 6. Gotchas & Lessons Learned

1. **Never apply `org.jetbrains.kotlin.android` in `:androidApp`** — AGP 9 has built-in Kotlin. Applying this plugin causes `Cannot add extension with name 'kotlin'` error.

2. **`kotlinOptions` is gone in AGP 9** — Two separate things now:
   - Java source/target compat: `compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }` (still in `android {}` block)
   - Kotlin JVM target: use `compilerOptions { jvmTarget = JvmTarget.JVM_17 }` inside the relevant `kotlin {}` compilation block — NOT `kotlinOptions`

3. **Room KMP needs `sqlite-bundled`** — Without it, iOS build succeeds but crashes at runtime with "no SQLite driver found".

4. **KSP per-target declarations** — You MUST declare `kspAndroid`, `kspIosArm64`, `kspIosSimulatorArm64` separately. `kspCommonMainMetadata` doesn't work the same way with the new plugin.

5. **`compose.resources { publicResClass = true }`** — Needed if you want to access generated resource classes publicly (e.g., from `:androidApp`).

6. **Navigation3 `NavDisplay` has no KMP targets** — `navigation3-ui` is Android/JVM only. On iOS/CMP always use `navigation3-runtime` (`rememberNavBackStack`) with `Crossfade`. Never add `navigation3-ui` to commonMain.

7. **`androidRuntimeClasspath`** — Replaces `debugImplementation` for Compose preview tooling in the KMP library module.

8. **`androidResources { enable = true }`** — Must be explicitly enabled in `kotlin { androidLibrary { ... } }` for Compose Resources to work.

9. **Namespace collision** — `:composeApp` and `:androidApp` MUST have different namespaces, or R class conflicts will occur.

10. **Hilt 2.59+ required for AGP 9** — If using Dagger/Hilt, upgrade to 2.59+.

11. **Kotlin/Native 2.x: ObjC category class methods are NOT on `Companion`** — In Kotlin/Native 2.x, `+` methods defined in an ObjC `@interface` category (e.g., `AVCaptureDevice (AVCaptureDeviceAuthorization)`) are generated as extension functions on the metaclass in the package namespace. Import them from the package, not from `ClassName.Companion`:
    ```kotlin
    // ❌ WRONG — fails with "Unresolved reference" in KN 2.x
    import platform.AVFoundation.AVCaptureDevice.Companion.authorizationStatusForMediaType

    // ✅ CORRECT — extension on AVCaptureDeviceMeta
    import platform.AVFoundation.authorizationStatusForMediaType
    ```
    Call sites (`AVCaptureDevice.authorizationStatusForMediaType(...)`) remain unchanged since the companion is of type `AVCaptureDeviceMeta`.

---

## 7. ViewModel in commonMain

Use `androidx.lifecycle:lifecycle-viewmodel` (JetBrains KMP fork, 2.10.0+). `viewModelScope` works on both platforms.

**libs.versions.toml:**
```toml
lifecycle = "2.10.0"
lifecycle-viewmodel = { module = "org.jetbrains.androidx.lifecycle:lifecycle-viewmodel", version.ref = "lifecycle" }
lifecycle-viewmodel-compose = { module = "org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycle" }
```

**commonMain dependency:**
```kotlin
commonMain.dependencies {
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.viewmodel.compose)
}
```

**ViewModel pattern:**
```kotlin
// commonMain
class HomeViewModel(private val repo: DataRepository) : ViewModel() {
    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = UiState.Success(repo.fetchData())
        }
    }
}

// Koin module — use viewModel DSL
val appModule = module {
    single<DataRepository> { DataRepositoryImpl(get()) }
    viewModel { HomeViewModel(get()) }
}
```

**In Composable (commonMain) — requires `koin-compose-viewmodel`:**
```kotlin
// Note: koin-compose does NOT include koinViewModel(). Add separately:
// koin-compose-viewmodel = { module = "io.insert-koin:koin-compose-viewmodel" }

@Composable
fun HomeScreen(vm: HomeViewModel = koinViewModel()) {
    val state by vm.state.collectAsState()
}
```

---

## 8. Compose Resources Setup

Directory structure in `composeApp/src/commonMain/composeResources/`:
```
composeResources/
├── drawable/        # PNG, WebP, SVG
├── font/            # .ttf, .otf
└── values/
    └── strings.xml
```

**strings.xml:**
```xml
<resources>
    <string name="app_name">My App</string>
    <string name="welcome">Welcome</string>
</resources>
```

**build.gradle.kts:**
```kotlin
commonMain.dependencies {
    implementation(compose.components.resources)
}

// Add if resource classes need to be accessed from :androidApp
compose.resources {
    publicResClass = true
}
```

**Usage in commonMain:**
```kotlin
import org.jetbrains.compose.resources.*

@Composable
fun MyScreen() {
    Text(stringResource(Res.string.welcome))
    Image(painter = painterResource(Res.drawable.logo), contentDescription = null)
    val font = FontFamily(Font(Res.font.inter_regular, FontWeight.Normal))
}
```

---

## 9. Coil 3 for KMP Image Loading

**Latest stable:** `3.4.0` — artifact group changed to `io.coil-kt.coil3`

**libs.versions.toml:**
```toml
coil3 = "3.4.0"
coil-compose = { module = "io.coil-kt.coil3:coil-compose", version.ref = "coil3" }
coil-network-ktor = { module = "io.coil-kt.coil3:coil-network-ktor3", version.ref = "coil3" }
```

**Dependencies:**
```kotlin
commonMain.dependencies {
    implementation(libs.coil.compose)
    implementation(libs.coil.network.ktor)  // uses same Ktor engines already declared
}
// No extra androidMain/iosMain setup — engine resolves automatically via Ktor
```

**Usage:**
```kotlin
import coil3.compose.AsyncImage

AsyncImage(
    model = "https://example.com/image.jpg",
    contentDescription = null,
    contentScale = ContentScale.Crop,
    modifier = Modifier.size(120.dp).clip(CircleShape)
)
```

---

## 10. kotlinx-datetime

**Latest stable:** `0.7.1` — serializers built-in, no extra module needed.

**libs.versions.toml:**
```toml
kotlinx-datetime = "0.7.1"
kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "kotlinx-datetime" }
```

**commonMain dependency:**
```kotlin
commonMain.dependencies {
    implementation(libs.kotlinx.datetime)
}
```

**Usage:**
```kotlin
import kotlinx.datetime.*

val now: Instant = Clock.System.now()
val today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
val future = today.plus(30, DateTimeUnit.DAY)
```

**Serialization (built-in, no extra config):**
```kotlin
@Serializable
data class Event(
    val name: String,
    val date: LocalDate,       // serializes as "2025-12-25"
    val createdAt: Instant     // serializes as ISO 8601
)
```

---

## 11. ProGuard / R8 Rules for Release Build

Most libraries ship consumer rules. Add these to `androidApp/proguard-rules.pro` as a safety baseline:

```proguard
# Preserve stack traces
-keepattributes SourceFile,LineNumberTable,Signature,*Annotation*,InnerClasses
-renamesourcefileattribute SourceFile
-dontwarn org.slf4j.**

# Room 3.x
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# Koin 4.x
-keep class org.koin.** { *; }
-dontwarn org.koin.**

# Ktor 3.x
-keep class io.ktor.client.** { *; }
-keep class io.ktor.http.** { *; }
-dontwarn io.ktor.**

# kotlinx.serialization
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class * { *** Companion; }
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <methods>;
}

# kotlinx-datetime
-keep class kotlinx.datetime.** { *; }
```

---

## 12. Required `gradle.properties` Baseline

```properties
# Kotlin / KMP
kotlin.code.style=official
kotlin.daemon.jvmargs=-Xmx3072M

# Gradle
org.gradle.jvmargs=-Xmx4096M
org.gradle.configuration-cache=true
org.gradle.caching=true

# Android
android.useAndroidX=true
android.nonTransitiveRClass=true

# KSP workaround — required when using KSP with AGP 9 built-in Kotlin
# Fixes: "Using kotlin.sourceSets DSL to add Kotlin sources is not allowed with built-in Kotlin"
android.disallowKotlinSourceSets=false
```

---

## 13. Notifications

### Recommended Library: Alarmee

**Alarmee** (`io.github.tweener:alarmee`) is a Kotlin/Compose Multiplatform library for scheduling alarms, local notifications, and push notifications on Android and iOS.

**Latest version:** `2.4.0` (check for updates)

**libs.versions.toml:**
```toml
alarmee = "2.4.0"
alarmee = { group = "io.github.tweener", name = "alarmee", version.ref = "alarmee" }           # Local notifications only
alarmee-push = { group = "io.github.tweener", name = "alarmee-push", version.ref = "alarmee" } # Local + push notifications
```

**build.gradle.kts:**
```kotlin
commonMain.dependencies {
    implementation(libs.alarmee)        // or libs.alarmee.push for push support
}
```

### Setup Pattern

**1. Declare `expect` function in `commonMain`:**
```kotlin
// commonMain
expect fun createAlarmeePlatformConfiguration(): AlarmeePlatformConfiguration
```

**2. Provide `actual` in `androidMain`:**
```kotlin
// androidMain
actual fun createAlarmeePlatformConfiguration(): AlarmeePlatformConfiguration =
    AlarmeeAndroidPlatformConfiguration(
        notificationIconResId = R.drawable.ic_notification,
        notificationIconColor = Color.Red,
        useExactScheduling = true, // Android 12+, requires SCHEDULE_EXACT_ALARM permission
        notificationChannels = listOf(
            AlarmeeNotificationChannel(
                id = "dailyNewsChannelId",
                name = "Daily news notifications",
                importance = NotificationManager.IMPORTANCE_HIGH,
            ),
        )
    )
```

**3. Provide `actual` in `iosMain`:**
```kotlin
// iosMain
actual fun createAlarmeePlatformConfiguration(): AlarmeePlatformConfiguration =
    AlarmeeIosPlatformConfiguration
```

### Usage in Compose

```kotlin
@Composable
fun NotificationDemo() {
    val alarmService: AlarmeeService = rememberAlarmeeService(
        platformConfiguration = createAlarmeePlatformConfiguration()
    )
    val localService = alarmService.local

    Button(onClick = {
        localService.schedule(
            alarmee = Alarmee(
                uuid = "myAlarmId",
                notificationTitle = "Reminder",
                notificationBody = "This is your scheduled notification",
                scheduledDateTime = LocalDateTime.now().plusMinutes(5),
                androidNotificationConfiguration = AndroidNotificationConfiguration(
                    priority = AndroidNotificationPriority.HIGH,
                    channelId = "dailyNewsChannelId",
                ),
                iosNotificationConfiguration = IosNotificationConfiguration(),
            )
        )
    }) {
        Text("Schedule Notification")
    }
}
```

### Push Notifications (alarmee-push)

For FCM (Android) + APNs (iOS) support, use the `alarmee-push` artifact:

```kotlin
// commonMain
val alarmService: MobileAlarmeeService = rememberAlarmeeMobileService(
    platformConfiguration = createAlarmeePlatformConfiguration()
)

val pushService = alarmService.push // null on non-mobile targets

// Get FCM token
pushService?.getToken()?.onSuccess { token ->
    // Send token to your backend
}

// Handle incoming push messages
pushService?.onPushMessageReceived { payload ->
    // Custom processing
}
```

> ⚠️ **iOS Push Setup:** Add Firebase iOS SDK to Xcode project. Enable "Push notifications" and "Background Modes → Remote notifications" capabilities.

### Alternative: Expect/Actual Wrapper (Manual)

If you prefer minimal dependencies, wrap platform APIs directly:

```kotlin
// commonMain
expect class NotificationManager {
    fun showNotification(title: String, body: String)
    fun scheduleNotification(title: String, body: String, delayMs: Long)
}

// androidMain — use Android NotificationManager
// iosMain — use UNUserNotificationCenter
```

---

## 14. Permissions

### Approach: Pure `expect/actual` (no third-party library)

No moko-permissions. No external dependency. All permission logic implemented via `expect/actual` wrapping native platform APIs directly.

**No extra dependencies required** — uses only platform-native APIs.

---

### Step 1 — Define the common interface in `commonMain`

```kotlin
// commonMain/permissions/Permission.kt
enum class Permission {
    CAMERA,
    LOCATION,
    NOTIFICATIONS,
    RECORD_AUDIO,
    STORAGE,
}

enum class PermissionStatus {
    GRANTED,
    DENIED,
    DENIED_ALWAYS,  // user ticked "Don't ask again" (Android) / restricted (iOS)
    NOT_DETERMINED, // iOS: not asked yet
}

// The controller — one expect class, two actuals
expect class PermissionController {
    suspend fun checkPermission(permission: Permission): PermissionStatus
    suspend fun requestPermission(permission: Permission): PermissionStatus
    fun openAppSettings()
}
```

---

### Step 2 — `androidMain` actual

```kotlin
// androidMain/permissions/PermissionController.android.kt
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual class PermissionController(private val activity: ComponentActivity) {

    actual suspend fun checkPermission(permission: Permission): PermissionStatus {
        val manifestPermission = permission.toManifest() ?: return PermissionStatus.GRANTED
        return when {
            ContextCompat.checkSelfPermission(activity, manifestPermission)
                == PackageManager.PERMISSION_GRANTED -> PermissionStatus.GRANTED
            !activity.shouldShowRequestPermissionRationale(manifestPermission) &&
                wasAskedBefore(activity, manifestPermission) -> PermissionStatus.DENIED_ALWAYS
            else -> PermissionStatus.DENIED
        }
    }

    actual suspend fun requestPermission(permission: Permission): PermissionStatus =
        suspendCancellableCoroutine { cont ->
            val manifestPermission = permission.toManifest() ?: run {
                cont.resume(PermissionStatus.GRANTED)
                return@suspendCancellableCoroutine
            }
            val launcher = activity.activityResultRegistry.register(
                "permission_${permission.name}",
                ActivityResultContracts.RequestPermission()
            ) { granted ->
                markAsAsked(activity, manifestPermission)
                cont.resume(
                    if (granted) PermissionStatus.GRANTED
                    else if (!activity.shouldShowRequestPermissionRationale(manifestPermission))
                        PermissionStatus.DENIED_ALWAYS
                    else PermissionStatus.DENIED
                )
            }
            launcher.launch(manifestPermission)
        }

    actual fun openAppSettings() {
        activity.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", activity.packageName, null)
            }
        )
    }

    // Map enum → Android manifest string
    private fun Permission.toManifest(): String? = when (this) {
        Permission.CAMERA -> Manifest.permission.CAMERA
        Permission.LOCATION -> Manifest.permission.ACCESS_FINE_LOCATION
        Permission.NOTIFICATIONS -> if (android.os.Build.VERSION.SDK_INT >= 33)
            Manifest.permission.POST_NOTIFICATIONS else null // auto-granted below API 33
        Permission.RECORD_AUDIO -> Manifest.permission.RECORD_AUDIO
        Permission.STORAGE -> Manifest.permission.READ_EXTERNAL_STORAGE
    }

    // Track whether user has been asked before (needed to detect DENIED_ALWAYS)
    private fun wasAskedBefore(context: Context, permission: String): Boolean =
        context.getSharedPreferences("perm_prefs", Context.MODE_PRIVATE)
            .getBoolean(permission, false)

    private fun markAsAsked(context: Context, permission: String) {
        context.getSharedPreferences("perm_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean(permission, true).apply()
    }
}
```

---

### Step 3 — `iosMain` actual

```kotlin
// iosMain/permissions/PermissionController.ios.kt
import platform.AVFoundation.*
import platform.CoreLocation.*
import platform.UserNotifications.*
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.Foundation.NSURL
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual class PermissionController {

    actual suspend fun checkPermission(permission: Permission): PermissionStatus =
        when (permission) {
            Permission.CAMERA -> avStatus(AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo))
            Permission.LOCATION -> coreLocationStatus()
            Permission.NOTIFICATIONS -> notificationStatus()
            Permission.RECORD_AUDIO -> avStatus(AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeAudio))
            Permission.STORAGE -> PermissionStatus.GRANTED // iOS has no storage permission
        }

    actual suspend fun requestPermission(permission: Permission): PermissionStatus =
        suspendCancellableCoroutine { cont ->
            when (permission) {
                Permission.CAMERA -> AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                    cont.resume(if (granted) PermissionStatus.GRANTED else PermissionStatus.DENIED)
                }
                Permission.RECORD_AUDIO -> AVCaptureDevice.requestAccessForMediaType(AVMediaTypeAudio) { granted ->
                    cont.resume(if (granted) PermissionStatus.GRANTED else PermissionStatus.DENIED)
                }
                Permission.LOCATION -> {
                    val delegate = LocationPermissionDelegate { status -> cont.resume(status) }
                    delegate.requestWhenInUse()
                }
                Permission.NOTIFICATIONS -> {
                    UNUserNotificationCenter.currentNotificationCenter()
                        .requestAuthorizationWithOptions(
                            UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
                        ) { granted, _ ->
                            cont.resume(if (granted) PermissionStatus.GRANTED else PermissionStatus.DENIED)
                        }
                }
                Permission.STORAGE -> cont.resume(PermissionStatus.GRANTED)
            }
        }

    actual fun openAppSettings() {
        val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
        UIApplication.sharedApplication.openURL(url)
    }

    private fun avStatus(status: AVAuthorizationStatus): PermissionStatus = when (status) {
        AVAuthorizationStatusAuthorized -> PermissionStatus.GRANTED
        AVAuthorizationStatusDenied -> PermissionStatus.DENIED
        AVAuthorizationStatusRestricted -> PermissionStatus.DENIED_ALWAYS
        else -> PermissionStatus.NOT_DETERMINED
    }

    private suspend fun coreLocationStatus(): PermissionStatus {
        val manager = CLLocationManager()
        return when (manager.authorizationStatus()) {
            kCLAuthorizationStatusAuthorizedWhenInUse,
            kCLAuthorizationStatusAuthorizedAlways -> PermissionStatus.GRANTED
            kCLAuthorizationStatusDenied -> PermissionStatus.DENIED
            kCLAuthorizationStatusRestricted -> PermissionStatus.DENIED_ALWAYS
            else -> PermissionStatus.NOT_DETERMINED
        }
    }

    private suspend fun notificationStatus(): PermissionStatus =
        suspendCancellableCoroutine { cont ->
            UNUserNotificationCenter.currentNotificationCenter()
                .getNotificationSettingsWithCompletionHandler { settings ->
                    val status = when (settings?.authorizationStatus) {
                        UNAuthorizationStatusAuthorized -> PermissionStatus.GRANTED
                        UNAuthorizationStatusDenied -> PermissionStatus.DENIED
                        else -> PermissionStatus.NOT_DETERMINED
                    }
                    cont.resume(status)
                }
        }
}

// Helper for CLLocationManager delegate pattern
private class LocationPermissionDelegate(
    private val onResult: (PermissionStatus) -> Unit
) : NSObject(), CLLocationManagerDelegateProtocol {
    private val manager = CLLocationManager()

    fun requestWhenInUse() {
        manager.delegate = this
        manager.requestWhenInUseAuthorization()
    }

    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        val status = when (manager.authorizationStatus()) {
            kCLAuthorizationStatusAuthorizedWhenInUse,
            kCLAuthorizationStatusAuthorizedAlways -> PermissionStatus.GRANTED
            kCLAuthorizationStatusDenied -> PermissionStatus.DENIED
            kCLAuthorizationStatusRestricted -> PermissionStatus.DENIED_ALWAYS
            else -> return // still NOT_DETERMINED, wait
        }
        onResult(status)
    }
}
```

---

### Step 4 — Compose helper in `commonMain`

```kotlin
// commonMain/permissions/PermissionState.kt
import androidx.compose.runtime.*

@Composable
fun rememberPermissionState(
    permission: Permission,
    controller: PermissionController
): PermissionStateHolder {
    var status by remember { mutableStateOf(PermissionStatus.NOT_DETERMINED) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(permission) {
        status = controller.checkPermission(permission)
    }

    return remember(permission, controller) {
        PermissionStateHolder(
            status = status,
            onRequest = {
                scope.launch {
                    status = controller.requestPermission(permission)
                }
            },
            onOpenSettings = { controller.openAppSettings() }
        )
    }
}

data class PermissionStateHolder(
    val status: PermissionStatus,
    val onRequest: () -> Unit,
    val onOpenSettings: () -> Unit,
) {
    val isGranted get() = status == PermissionStatus.GRANTED
}
```

---

### Step 5 — Usage in Compose screen

```kotlin
// commonMain — requires PermissionController injected via Koin
@Composable
fun CameraScreen(controller: PermissionController = get()) {
    val camPerm = rememberPermissionState(Permission.CAMERA, controller)

    when {
        camPerm.isGranted -> CameraContent()
        camPerm.status == PermissionStatus.DENIED_ALWAYS -> {
            Text("Camera permission permanently denied.")
            Button(onClick = camPerm.onOpenSettings) { Text("Open Settings") }
        }
        else -> {
            Text("Camera permission required.")
            Button(onClick = camPerm.onRequest) { Text("Grant Permission") }
        }
    }
}
```

---

### Step 6 — Koin wiring

```kotlin
// androidMain — provide PermissionController with Activity reference
val androidPermissionModule = module {
    // Scoped to the Activity lifecycle
    factory { (activity: ComponentActivity) -> PermissionController(activity) }
}

// iosMain — no constructor params needed
val iosPermissionModule = module {
    factory { PermissionController() }
}
```

---

### Platform Manifest Requirements

**Android (`androidApp/src/main/AndroidManifest.xml`):**
```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />
```

**iOS (`iosApp/iosApp/Info.plist`):**
```xml
<key>NSCameraUsageDescription</key>
<string>This app needs camera access to take photos</string>
<key>NSLocationWhenInUseUsageDescription</key>
<string>This app needs location access to show nearby places</string>
<key>NSUserNotificationUsageDescription</key>
<string>This app sends you reminders and updates</string>
<key>NSMicrophoneUsageDescription</key>
<string>This app needs microphone access to record audio</string>
```

> ⚠️ **Android `PermissionController` constructor** — On Android the controller needs a `ComponentActivity` reference. Use `rememberPermissionController()` (expect/actual Composable) which casts `LocalContext.current as ComponentActivity` in the androidMain actual — no Koin wiring needed.

> ⚠️ **Kotlin/Native 2.x — ObjC category class methods** — In Kotlin/Native 2.x, ObjC category class methods (`+` methods defined in `@interface Foo (Category)`) are generated as **extension functions on the metaclass** in the package namespace, NOT as `Foo.Companion` members. Attempting `import platform.AVFoundation.AVCaptureDevice.Companion.authorizationStatusForMediaType` will fail with "Unresolved reference".
>
> **Correct imports:**
> ```kotlin
> import platform.AVFoundation.authorizationStatusForMediaType  // extension on AVCaptureDeviceMeta
> import platform.AVFoundation.requestAccessForMediaType        // extension on AVCaptureDeviceMeta
> ```
> **Call sites stay unchanged** — `AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)` resolves correctly because the companion object (`AVCaptureDeviceMeta`) is the receiver.
>
> Also note: `AVMediaTypeVideo` and `AVMediaTypeAudio` are `String?` (from `NS_EXTENSIBLE_STRING_ENUM`). Pass them directly — the extension function accepts `String?`.

---

## 15. Preferences (Local Key-Value Storage)

The contest requires "local data persistence (database **and** preferences)." Room covers structured DB. For lightweight key-value preferences use **multiplatform-settings** by russhwolf — zero-dependency, no moko needed.

**Latest stable:** `1.3.0`

**libs.versions.toml:**
```toml
multiplatform-settings = "1.3.0"
multiplatform-settings = { module = "com.russhwolf:multiplatform-settings", version.ref = "multiplatform-settings" }
multiplatform-settings-coroutines = { module = "com.russhwolf:multiplatform-settings-coroutines", version.ref = "multiplatform-settings" }
multiplatform-settings-no-arg = { module = "com.russhwolf:multiplatform-settings-no-arg", version.ref = "multiplatform-settings" }
```

**build.gradle.kts:**
```kotlin
commonMain.dependencies {
    implementation(libs.multiplatform.settings)
    implementation(libs.multiplatform.settings.coroutines)  // Flow observation
    implementation(libs.multiplatform.settings.no.arg)      // zero-arg factory (no-op on iOS, SharedPreferences on Android)
}
```

> `multiplatform-settings-no-arg` provides a `Settings()` factory with no constructor arguments — backed by `SharedPreferences` on Android and `NSUserDefaults` on iOS automatically.

### No `expect/actual` needed for basic use

```kotlin
// commonMain — works out-of-the-box on both platforms
import com.russhwolf.settings.Settings
import com.russhwolf.settings.coroutines.getStringOrNullFlow

class AppPreferences(private val settings: Settings = Settings()) {

    var authToken: String?
        get() = settings.getStringOrNull(KEY_TOKEN)
        set(value) = if (value != null) settings.putString(KEY_TOKEN, value)
                     else settings.remove(KEY_TOKEN)

    var onboardingComplete: Boolean
        get() = settings.getBoolean(KEY_ONBOARDING, false)
        set(value) = settings.putBoolean(KEY_ONBOARDING, value)

    val authTokenFlow = settings.getStringOrNullFlow(KEY_TOKEN) // StateFlow-like

    fun clear() = settings.clear()

    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_ONBOARDING = "onboarding_complete"
    }
}
```

### Koin wiring

```kotlin
// commonMain module — Settings() factory resolves platform automatically
val preferencesModule = module {
    single { AppPreferences() }
}
```

### When to use `expect/actual` with settings

Use `expect/actual` only when you need a **named** settings instance (e.g., separate namespaces per feature):

```kotlin
// commonMain
expect fun createSettings(name: String): Settings

// androidMain
actual fun createSettings(name: String): Settings =
    AndroidSettings(context.getSharedPreferences(name, Context.MODE_PRIVATE))

// iosMain
actual fun createSettings(name: String): Settings =
    NSUserDefaultsSettings(NSUserDefaults(suiteName = name))
```

---

## 16-old. AI Services in Networking (Ktor + Claude API)

Contest explicitly mentions "AI services" as a networking requirement. Use Ktor client + Anthropic REST API directly — no SDK needed for KMP.

**Pattern — Claude API call from `commonMain`:**

```kotlin
// commonMain/ai/ClaudeService.kt
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable
data class ClaudeRequest(
    val model: String = "claude-sonnet-4-6",
    val max_tokens: Int = 1024,
    val messages: List<Message>
)

@Serializable
data class Message(val role: String, val content: String)

@Serializable
data class ClaudeResponse(
    val content: List<ContentBlock>
)

@Serializable
data class ContentBlock(val type: String, val text: String)

class ClaudeService(private val client: HttpClient) {

    suspend fun chat(prompt: String): String {
        val response = client.post("https://api.anthropic.com/v1/messages") {
            header("x-api-key", AppConfig.claudeApiKey)
            header("anthropic-version", "2023-06-01")
            contentType(ContentType.Application.Json)
            setBody(
                ClaudeRequest(
                    messages = listOf(Message(role = "user", content = prompt))
                )
            )
        }
        val body = response.body<ClaudeResponse>()
        return body.content.firstOrNull()?.text ?: ""
    }
}
```

**AppConfig expect/actual for the API key:**

```kotlin
// commonMain
expect object AppConfig {
    val claudeApiKey: String
    val apiBaseUrl: String
}

// androidMain
actual object AppConfig {
    actual val claudeApiKey: String = BuildConfig.CLAUDE_API_KEY
    actual val apiBaseUrl: String = BuildConfig.API_BASE_URL
}

// iosMain — keys injected via xcconfig at build time
actual object AppConfig {
    actual val claudeApiKey: String = ""        // set via xcconfig
    actual val apiBaseUrl: String = ""          // set via xcconfig
}
```

**Koin wiring:**
```kotlin
val networkModule = module {
    single { createHttpClient() }
    single { ClaudeService(get()) }
}
```

> ⚠️ Never hardcode API keys. On Android use `BuildConfig` + `local.properties`. On iOS use xcconfig files excluded from git.

---

## 16. Testing

### Test Directory Structure

```
composeApp/src/
├── commonTest/kotlin/           # Shared tests (run on ALL platforms)
│   ├── domain/
│   │   └── UseCaseTest.kt
│   ├── data/
│   │   └── RepositoryTest.kt
│   └── viewmodel/
│       └── HomeViewModelTest.kt
├── androidUnitTest/kotlin/      # Android-specific unit tests
│   └── PlatformApiTest.kt
└── iosTest/kotlin/              # iOS-specific tests (rarely needed)
```

### Test Dependencies

**libs.versions.toml:**
```toml
# Testing
kotlinx-coroutines-test = "1.10.2"
turbine = "1.2.0"
kotest = "5.9.1"
mockative = "2.2.0"

# Test libraries
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "kotlinx-coroutines-test" }
turbine = { module = "app.cash.turbine:turbine", version.ref = "turbine" }
kotest-assertions = { module = "io.kotest:kotest-assertions-core", version.ref = "kotest" }
mockative = { module = "io.mockative:mockative", version.ref = "mockative" }
mockative-processor = { module = "io.mockative:mockative-processor", version.ref = "mockative" }
```

**composeApp/build.gradle.kts:**
```kotlin
kotlin {
    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)              // Flow testing
            implementation(libs.kotest.assertions)    // Expressive assertions
            implementation(libs.mockative)            # KMP mocking
        }
        androidUnitTest.dependencies {
            implementation(kotlin("test-junit"))
        }
    }
}

// Mockative KSP processor (required for code generation)
dependencies {
    add("kspCommonMainMetadata", libs.mockative.processor)
    add("kspAndroid", libs.mockative.processor)
    add("kspIosArm64", libs.mockative.processor)
    add("kspIosSimulatorArm64", libs.mockative.processor)
}
```

### Testing Patterns

#### 1. ViewModel Testing with Turbine

```kotlin
// commonTest
class HomeViewModelTest {

    @Test
    fun `loadData emits loading then success`() = runTest {
        // Given
        val fakeRepo = FakeDataRepository()
        val viewModel = HomeViewModel(fakeRepo)

        // When / Then
        viewModel.state.test {
            awaitItem() shouldBe UiState.Loading
            awaitItem() shouldBe UiState.Success(fakeRepo.data)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loadData emits error on failure`() = runTest {
        // Given
        val fakeRepo = FakeDataRepository(shouldFail = true)
        val viewModel = HomeViewModel(fakeRepo)

        // When / Then
        viewModel.state.test {
            awaitItem() shouldBe UiState.Loading
            val error = awaitItem() as UiState.Error
            error.message shouldBe "Network error"
            cancelAndIgnoreRemainingEvents()
        }
    }
}

// Fake implementation (preferred over mocks for KMP)
class FakeDataRepository(
    private val shouldFail: Boolean = false
) : DataRepository {
    val data = listOf(Item("1", "Test"))

    override suspend fun fetchData(): List<Item> {
        if (shouldFail) throw Exception("Network error")
        return data
    }
}
```

#### 2. Repository Testing with Ktor MockEngine

```kotlin
// commonTest
class ApiRepositoryTest {

    @Test
    fun `fetchUsers returns parsed list`() = runTest {
        // Given
        val mockEngine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/users" -> respond(
                    content = """[{"id":"1","name":"Alice"}]""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respond("Not Found", HttpStatusCode.NotFound)
            }
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json() }
        }
        val repo = UserRepository(client)

        // When
        val users = repo.fetchUsers()

        // Then
        users.size shouldBe 1
        users[0].name shouldBe "Alice"
    }
}
```

#### 3. Mocking with Mockative (when fakes are too verbose)

```kotlin
// commonTest
@Mockable
class MockedRepositoryTest {

    @Mock
    val api = mock(classOf<ApiService>())

    @Test
    fun `mocked api returns data`() = runTest {
        // Given
        every { api.fetchData() }.returns(listOf(Item("1", "Mocked")))
        val repo = DataRepository(api)

        // When
        val result = repo.getData()

        // Then
        result.size shouldBe 1
        verify { api.fetchData() }.wasInvoked(exactly = 1)
    }
}
```

### Testing Best Practices

1. **Write tests in `commonTest` by default** — they run on all platforms automatically.
2. **Prefer fakes over mocks** — simpler, works everywhere, easier to debug.
3. **Use Turbine for Flow testing** — never collect Flows manually in tests.
4. **Use `runTest` from kotlinx-coroutines-test** — provides virtual time for coroutines.
5. **Test error cases** — for every happy path, write at least one error case.
6. **In-memory databases** — use Room's in-memory builder for database tests.

### Running Tests

```bash
# Run all common tests (Android + iOS simulators)
./gradlew :composeApp:test

# Run Android unit tests only
./gradlew :composeApp:testDebugUnitTest

# Run iOS tests (requires macOS + Xcode)
./gradlew :composeApp:iosSimulatorArm64Test
```

---

## 16. AI-Ready Workflows

### AGENTS.md Best Practices

The starter kit should include an `AGENTS.md` at project root with:

```markdown
# AGENTS.md — KMP Starter Kit

## Project Overview
- Kotlin Multiplatform with Compose Multiplatform
- Targets: Android, iOS (optional: Web, Desktop)
- Architecture: MVVM + Repository pattern
- DI: Koin | Database: Room KMP | Network: Ktor

## Build Commands
- `./gradlew :composeApp:build` — Build shared module
- `./gradlew :androidApp:assembleDebug` — Build Android APK
- `./gradlew :composeApp:test` — Run all tests
- `./gradlew :composeApp:testDebugUnitTest` — Android tests only

## Code Conventions
- Use `commonMain` for all shared code
- Platform-specific code goes in `androidMain` / `iosMain`
- Use `expect/actual` for platform APIs
- ViewModels live in `commonMain`
- Repositories abstract data sources

## Common Tasks
- Adding a new screen: Create Composable + ViewModel + add to Navigation
- Adding a database table: Entity + DAO + update Database class + re-run KSP
- Adding an API endpoint: Add to Ktor client interface + Repository method
```

### SKILL.md Templates

Include `.claude/skills/` directory with reusable skills:

```
.claude/skills/
├── kmp-testing/
│   └── SKILL.md          # How to write tests for KMP
├── kmp-notifications/
│   └── SKILL.md          # How to add notifications
├── kmp-permissions/
│   └── SKILL.md          # How to request permissions
└── kmp-navigation/
    └── SKILL.md          # How to add screens with Navigation3
```

### Build/Test Scripts

Add convenience scripts for AI agents:

```bash
#!/bin/bash
# scripts/build.sh — Build all targets
set -e
./gradlew :composeApp:build
./gradlew :androidApp:assembleDebug
```

```bash
#!/bin/bash
# scripts/test.sh — Run all tests
set -e
./gradlew :composeApp:test
```

---

## 17. Web / Desktop / Backend Targets (Optional)

The contest welcomes Web, Desktop, and Backend support. Here's how to add them.

### Adding Targets to `composeApp/build.gradle.kts`

```kotlin
kotlin {
    // Existing targets
    androidTarget()
    iosArm64()
    iosSimulatorArm64()

    // Desktop (JVM)
    jvm("desktop")

    // Web (Kotlin/Wasm — recommended over JS for performance)
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    // Backend (optional — creates separate server module)
    // jvm("server")

    sourceSets {
        val desktopMain by getting
        val wasmJsMain by getting

        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
        }

        wasmJsMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
        }
    }
}
```

### Desktop Entry Point

```kotlin
// composeApp/src/desktopMain/kotlin/main.kt
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "My KMP App"
    ) {
        App()
    }
}
```

### Web Entry Point

```kotlin
// composeApp/src/wasmJsMain/kotlin/main.kt
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    CanvasBasedWindow(canvasElementId = "ComposeTarget") {
        App()
    }
}
```

### Web Compatibility Mode (JS fallback for older browsers)

```bash
# Build both wasmJs and js targets with compatibility
./gradlew composeCompatibilityBrowserDistribution

# Output: composeApp/build/dist/composeWebCompatibility/productionExecutable/
```

### Important Notes

| Target | Status | Notes |
|--------|--------|-------|
| `jvm("desktop")` | ✅ Stable | Full Compose Multiplatform support |
| `wasmJs` | ⚠️ Beta (2026) | Compose for Web Beta — good for production |
| `js` | ✅ Stable | Use as fallback for older browsers |
| `jvm("server")` | ✅ Stable | Ktor server — separate module recommended |

> ⚠️ **Not all libraries support all targets.** Check Klibs.io before adding dependencies.

---

## 18. Sample App Template

### README.md Template

```markdown
# KMP Starter Kit — Sample App

A production-ready Kotlin Multiplatform sample app demonstrating:
- ✅ Android + iOS ( + optional Web/Desktop)
- ✅ Compose Multiplatform UI
- ✅ Room KMP database
- ✅ Ktor networking
- ✅ Koin dependency injection
- ✅ Navigation3
- ✅ Notifications (Alarmee)
- ✅ Permissions (expect/actual, no moko)
- ✅ Preferences (multiplatform-settings)
- ✅ AI Services (Claude API via Ktor)
- ✅ Testing (commonTest + Turbine)

## Screenshots

| Android | iOS |
|---------|-----|
| ![Android](docs/screenshots/android.png) | ![iOS](docs/screenshots/ios.png) |

## Architecture

```
┌─────────────────────────────────────────┐
│  UI Layer (Compose Multiplatform)       │
│  Screens → ViewModels → StateFlow       │
├─────────────────────────────────────────┤
│  Domain Layer (commonMain)              │
│  Use Cases → Models → Repository IF     │
├─────────────────────────────────────────┤
│  Data Layer (commonMain)                │
│  Repository Impl → DAO / API / Prefs    │
├─────────────────────────────────────────┤
│  Platform Layer (androidMain / iosMain) │
│  Database Builder / HTTP Engine / etc   │
└─────────────────────────────────────────┘
```

## Quick Start

1. Clone the repo
2. Open in Android Studio or IntelliJ IDEA
3. Sync Gradle
4. Run Android: `:androidApp` configuration
5. Run iOS: `:iosApp` configuration (requires macOS + Xcode)

## Testing

```bash
./gradlew :composeApp:test           # All tests
./gradlew :composeApp:testDebugUnitTest  # Android only
```

## License

MIT License — see [LICENSE](LICENSE)
```

### Required Files Checklist

| File | Purpose |
|------|---------|
| `LICENSE` | MIT License (contest requirement) |
| `README.md` | Project overview, setup, screenshots |
| `AGENTS.md` | AI agent guidance |
| `.claude/skills/*.md` | Reusable development skills |
| `docs/architecture.md` | Architecture decisions |
| `docs/blog/` | Blog post drafts (DI, interop, testing) |

### Blog Post Outlines

**Post 1: Dependency Injection in KMP with Koin**
- Why Koin over Hilt for KMP
- Setting up Koin in `commonMain`
- Platform-specific modules (Android context, iOS setup)
- Testing with Koin

**Post 2: Platform Interop with Expect/Actual**
- When to use `expect/actual` vs interfaces
- Real examples: Database builder, File system, Notifications
- Common pitfalls and how to avoid them

**Post 3: Testing KMP Apps**
- Test pyramid for KMP
- Writing shared tests in `commonTest`
- Flow testing with Turbine
- Mocking vs fakes

---

*End of KMP Starter Kit Findings — updated for Kotlin Foundation Contest 2026*
