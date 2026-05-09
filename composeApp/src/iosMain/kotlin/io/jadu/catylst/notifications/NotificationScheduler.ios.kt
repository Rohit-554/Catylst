package io.jadu.catylst.notifications

import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter

actual class NotificationScheduler {

    private val center = UNUserNotificationCenter.currentNotificationCenter()

    init {
        val options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
        center.requestAuthorizationWithOptions(options) { _, _ -> }
    }

    actual fun schedule(
        id: String,
        title: String,
        body: String,
        delaySeconds: Int,
        channel: AppNotificationChannel,
    ) {
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(body)
            setSound(UNNotificationSound.defaultSound())
            setCategoryIdentifier(channel.channelId)
        }
        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
            timeInterval = delaySeconds.toDouble().coerceAtLeast(1.0),
            repeats = false,
        )
        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = id,
            content = content,
            trigger = trigger,
        )
        center.addNotificationRequest(request) { _ -> }
    }

    actual fun cancel(id: String) {
        center.removePendingNotificationRequestsWithIdentifiers(listOf(id))
    }

    actual fun cancelAll() {
        center.removeAllPendingNotificationRequests()
    }
}
