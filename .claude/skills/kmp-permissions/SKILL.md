---
name: kmp-permissions
description: Add a new runtime permission to Catylst KMP — enum entry, Android manifest, iOS plist, platform actuals, and composable usage
---

# kmp-permissions

Catylst has a cross-platform permission abstraction. Adding a new permission requires changes
in three places: the shared enum, the Android actual, and the iOS actual.

## Architecture

```
commonMain:
  permissions/Permission.kt              (enum of all permissions)
  permissions/PermissionState.kt         (Granted / Denied / PermanentlyDenied / NotDetermined)
  permissions/PermissionController.kt    (expect class)

androidMain:
  permissions/PermissionController.android.kt   (actual — uses ActivityResultLauncher)

iosMain:
  permissions/PermissionController.ios.kt       (actual — uses AVFoundation / CLLocationManager / etc.)
```

## Step 1 — Add to the Permission enum

Open `composeApp/src/commonMain/kotlin/io/jadu/catylst/permissions/Permission.kt`:

```kotlin
enum class Permission {
    CAMERA,
    LOCATION,
    NOTIFICATIONS,
    MY_NEW_PERMISSION,   // <-- add here
}
```

## Step 2 — Map on Android

Open `composeApp/src/androidMain/kotlin/io/jadu/catylst/permissions/PermissionController.android.kt`.

In the `Permission.toManifest()` mapping function, add:

```kotlin
Permission.MY_NEW_PERMISSION -> android.Manifest.permission.MY_ANDROID_PERMISSION
```

## Step 3 — Handle on iOS

Open `composeApp/src/iosMain/kotlin/io/jadu/catylst/permissions/PermissionController.ios.kt`.

Add cases in both `checkPermission` and `requestPermission`:

```kotlin
Permission.MY_NEW_PERMISSION -> {
    /* use the relevant iOS framework API */
}
```

## Step 4 — Declare in manifests

**Android** — `androidApp/src/main/AndroidManifest.xml`:
```xml
<uses-permission android:name="android.permission.MY_NEW_PERMISSION" />
```

**iOS** — `iosApp/iosApp/Info.plist`:
```xml
<key>NSMyPermissionUsageDescription</key>
<string>We need this permission because...</string>
```

## Using Permissions in Composables

```kotlin
val controller = rememberPermissionController()
val state by controller.permissionState(Permission.MY_NEW_PERMISSION).collectAsState()

when (state) {
    PermissionState.Granted -> { /* proceed */ }
    PermissionState.Denied  -> {
        Button(onClick = { controller.requestPermission(Permission.MY_NEW_PERMISSION) }) {
            Text("Grant Permission")
        }
    }
    else -> { /* handle PermanentlyDenied, NotDetermined */ }
}
```

## Removing Permissions Entirely

Use the `kmp-remove-feature` skill — "Remove Permissions" section.
