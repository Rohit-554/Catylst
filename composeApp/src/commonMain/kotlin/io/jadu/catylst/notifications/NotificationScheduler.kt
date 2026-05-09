package io.jadu.catylst.notifications

import androidx.compose.runtime.Composable
import org.koin.compose.koinInject

/**
 * Cross-platform notification scheduler.
 *
 * Inject it into a ViewModel via Koin:
 * ```kotlin
 * class MyViewModel(private val notifications: NotificationScheduler) : ViewModel()
 * ```
 *
 * Or obtain it inside a composable:
 * ```kotlin
 * val scheduler = rememberNotificationScheduler()
 * ```
 *
 * Platform behaviour:
 * - **Android** — backed by WorkManager; survives app kill and battery optimisation.
 * - **iOS** — backed by `UNUserNotificationCenter`; requests authorisation on first use.
 * - **Desktop** — no-op stub (local notifications are not supported on JVM desktop).
 */
expect class NotificationScheduler {

    /**
     * Schedules a local notification.
     *
     * @param id           Stable identifier for this notification. Scheduling with the same
     *                     [id] twice cancels the previous one before re-scheduling.
     * @param title        Notification title shown in bold.
     * @param body         Notification body text.
     * @param delaySeconds Seconds to wait before the notification is shown. Use `0` for immediate.
     * @param channel      The [AppNotificationChannel] this notification belongs to.
     *                     Determines Android channel importance and iOS category.
     */
    fun schedule(
        id: String,
        title: String,
        body: String,
        delaySeconds: Int,
        channel: AppNotificationChannel,
    )

    /**
     * Cancels a previously scheduled notification by [id].
     * Safe to call even if [id] was never scheduled.
     */
    fun cancel(id: String)

    /** Cancels all pending notifications scheduled by this scheduler. */
    fun cancelAll()
}

/**
 * Convenience overload that defaults to [AppNotificationChannel.REMINDERS] and
 * [delaySeconds] = 0 (immediate).
 */
fun NotificationScheduler.schedule(
    id: String,
    title: String,
    body: String,
    delaySeconds: Int = 0,
    channel: AppNotificationChannel = AppNotificationChannel.REMINDERS,
) = schedule(id, title, body, delaySeconds, channel)

/**
 * Returns the [NotificationScheduler] from the Koin graph, stable across recompositions.
 *
 * Prefer injecting [NotificationScheduler] directly into your ViewModel for business-logic
 * use cases and reserve this helper for purely UI-driven scheduling.
 */
@Composable
fun rememberNotificationScheduler(): NotificationScheduler = koinInject()
