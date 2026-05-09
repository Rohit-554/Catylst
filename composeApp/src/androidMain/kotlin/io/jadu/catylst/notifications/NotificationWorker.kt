package io.jadu.catylst.notifications

import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters

internal class NotificationWorker(
    context: Context,
    params: WorkerParameters,
) : Worker(context, params) {

    override fun doWork(): Result {
        val id = inputData.getString(KEY_ID) ?: return Result.failure()
        val title = inputData.getString(KEY_TITLE) ?: return Result.failure()
        val body = inputData.getString(KEY_BODY) ?: return Result.failure()
        val channelId = inputData.getString(KEY_CHANNEL_ID) ?: AppNotificationChannel.REMINDERS.channelId

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureAllChannelsExist(manager)

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        manager.notify(id.hashCode(), notification)
        return Result.success()
    }

    private fun ensureAllChannelsExist(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        AppNotificationChannel.entries.forEach { appChannel ->
            val importance = when (appChannel) {
                AppNotificationChannel.PROMOTIONS -> NotificationManager.IMPORTANCE_DEFAULT
                else -> NotificationManager.IMPORTANCE_HIGH
            }
            val channel = NotificationChannel(appChannel.channelId, appChannel.channelName, importance)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val KEY_ID = "notification_id"
        const val KEY_TITLE = "notification_title"
        const val KEY_BODY = "notification_body"
        const val KEY_CHANNEL_ID = "notification_channel_id"
    }
}
