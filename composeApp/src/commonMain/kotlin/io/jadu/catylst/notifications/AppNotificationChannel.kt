package io.jadu.catylst.notifications

/**
 * Defines the notification channels available in the app.
 *
 * Each channel maps to a system-level notification channel on Android and a
 * category identifier on iOS. Users of the template should add or remove entries
 * here to match their app's notification taxonomy.
 *
 * Usage:
 * ```kotlin
 * scheduler.schedule(
 *     id     = "order_shipped",
 *     title  = "Order Shipped",
 *     body   = "Your order is on the way!",
 *     channel = AppNotificationChannel.ALERTS,
 * )
 * ```
 */
enum class AppNotificationChannel(
    /** Stable identifier used as the Android channel ID and iOS category identifier. */
    val channelId: String,
    /** Human-readable name shown in Android system notification settings. */
    val channelName: String,
) {
    /** Time-sensitive reminders (alarms, due dates). High importance on Android. */
    REMINDERS(channelId = "catylst_reminders", channelName = "Reminders"),

    /** Urgent one-off alerts (errors, security notices). High importance on Android. */
    ALERTS(channelId = "catylst_alerts", channelName = "Alerts"),

    /** Non-urgent promotional or informational messages. Default importance on Android. */
    PROMOTIONS(channelId = "catylst_promotions", channelName = "Promotions"),
}
