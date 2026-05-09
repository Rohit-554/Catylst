package io.jadu.catylst.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

actual class NotificationScheduler(private val context: Context) {

    private val workManager = WorkManager.getInstance(context)

    actual fun schedule(
        id: String,
        title: String,
        body: String,
        delaySeconds: Int,
        channel: AppNotificationChannel,
    ) {
        if (!hasNotificationPermission()) {
            Log.w(TAG, "POST_NOTIFICATIONS permission not granted — notification '$id' dropped.")
            return
        }

        val data = Data.Builder()
            .putString(NotificationWorker.KEY_ID, id)
            .putString(NotificationWorker.KEY_TITLE, title)
            .putString(NotificationWorker.KEY_BODY, body)
            .putString(NotificationWorker.KEY_CHANNEL_ID, channel.channelId)
            .build()

        val request = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(delaySeconds.toLong(), TimeUnit.SECONDS)
            .setInputData(data)
            .addTag(id)
            .build()

        workManager.cancelAllWorkByTag(id)
        workManager.enqueue(request)
    }

    actual fun cancel(id: String) {
        workManager.cancelAllWorkByTag(id)
    }

    actual fun cancelAll() {
        workManager.cancelAllWork()
    }

    private fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private companion object {
        const val TAG = "NotificationScheduler"
    }
}
