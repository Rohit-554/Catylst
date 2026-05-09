package io.jadu.catylst.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.jadu.catylst.notifications.AppNotificationChannel
import io.jadu.catylst.notifications.NotificationScheduler
import io.jadu.catylst.notifications.schedule
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationDemoScreen(onBack: () -> Unit) {
    // Injected from Koin — the same instance available in ViewModels.
    val scheduler: NotificationScheduler = koinInject()

    var selectedChannel by remember { mutableStateOf(AppNotificationChannel.REMINDERS) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Schedule a local notification in 5 seconds. The notification survives app kill on Android (WorkManager) and uses UNUserNotificationCenter on iOS.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text("Channel", style = MaterialTheme.typography.labelLarge)

            // Channel picker — add your own AppNotificationChannel entries to extend this.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppNotificationChannel.entries.forEach { channel ->
                    FilterChip(
                        selected = selectedChannel == channel,
                        onClick = { selectedChannel = channel },
                        label = { Text(channel.channelName) },
                    )
                }
            }

            Button(
                onClick = {
                    scheduler.schedule(
                        id = "catylst_demo_${selectedChannel.channelId}",
                        title = "Catylst · ${selectedChannel.channelName}",
                        body = "Fired from channel: ${selectedChannel.channelId}",
                        delaySeconds = 5,
                        channel = selectedChannel,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Schedule in 5 s  (${selectedChannel.channelName})")
            }

            OutlinedButton(
                onClick = { scheduler.cancelAll() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Cancel All")
            }

            Text(
                text = "Tip: inject NotificationScheduler directly into your ViewModel via Koin for business-logic use cases.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )

            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Back")
            }
        }
    }
}
