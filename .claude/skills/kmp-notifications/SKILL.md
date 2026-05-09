---
name: kmp-notifications
description: Customize the multi-channel notification system in Catylst KMP — add channels, schedule notifications, and understand the WorkManager / UNUserNotificationCenter architecture
---

# kmp-notifications

Catylst ships a production-ready notification system using WorkManager (Android) and
UNUserNotificationCenter (iOS). Notifications survive app kill because WorkManager runs in a
background process.

## Architecture

```
commonMain:
  NotificationScheduler.kt          (expect class — public API)
  AppNotificationChannel.kt         (enum of channels)

androidMain:
  NotificationScheduler.android.kt  (actual — enqueues WorkManager OneTimeWorkRequest)
  NotificationWorker.kt             (Worker — posts the actual notification)

iosMain:
  NotificationScheduler.ios.kt      (actual — schedules UNUserNotificationContent)

desktopMain:
  NotificationScheduler.jvm.kt      (actual — stub / system tray placeholder)
```

## Adding a New Channel

Open `composeApp/src/commonMain/kotlin/io/jadu/catylst/notifications/AppNotificationChannel.kt`.
Add a new enum entry:

```kotlin
enum class AppNotificationChannel(val channelId: String, val channelName: String) {
    REMINDERS("reminders", "Reminders"),
    ALERTS("alerts", "Alerts"),
    PROMOTIONS("promotions", "Promotions"),
    MY_CHANNEL("my_channel", "My Channel"),   // <-- add here
}
```

Android: the channel is auto-created in `NotificationWorker.ensureAllChannelsExist()` —
no additional Android code needed.

iOS: the `channelId` maps to `categoryIdentifier` on `UNMutableNotificationContent`.

## Scheduling a Notification

Inject `NotificationScheduler` via Koin in your composable or ViewModel:

```kotlin
/* in a composable */
val scheduler: NotificationScheduler = koinInject()

/* schedule immediately */
scheduler.schedule(
    id = "my-notification-1",
    title = "Hello",
    body = "This is a test notification",
    delaySeconds = 0L,
    channel = AppNotificationChannel.ALERTS
)

/* schedule with delay */
scheduler.schedule(
    id = "reminder-daily",
    title = "Daily Reminder",
    body = "Don't forget to check in!",
    delaySeconds = 60L,
    channel = AppNotificationChannel.REMINDERS
)
```

The convenience overload uses `delaySeconds = 0` and `channel = REMINDERS` as defaults.

## Cancelling a Notification

```kotlin
scheduler.cancel(id = "my-notification-1")
```

Android: cancels the WorkManager request by tag.
iOS: cancels the pending `UNNotificationRequest` by identifier.

## Permissions

Android: `POST_NOTIFICATIONS` is declared in `AndroidManifest.xml`.
Runtime check is performed in `NotificationScheduler.android.kt` on Android 13+.

iOS: The app must request permission via `UNUserNotificationCenter.requestAuthorization`
before scheduling — this is handled in `NotificationScheduler.ios.kt`.

To request permission in UI, use the Permissions system (see `kmp-permissions` skill).

## Removing Notifications Entirely

Use the `kmp-remove-feature` skill — "Remove Notifications" section.
