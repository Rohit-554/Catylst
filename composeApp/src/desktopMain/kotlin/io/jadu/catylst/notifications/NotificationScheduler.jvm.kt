package io.jadu.catylst.notifications

actual class NotificationScheduler {
    actual fun schedule(id: String, title: String, body: String, delaySeconds: Int, channel: AppNotificationChannel) = Unit
    actual fun cancel(id: String) = Unit
    actual fun cancelAll() = Unit
}
