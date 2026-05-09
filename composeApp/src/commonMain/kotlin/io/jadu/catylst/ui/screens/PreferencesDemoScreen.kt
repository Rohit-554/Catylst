package io.jadu.catylst.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import io.jadu.catylst.data.preferences.AppPreferences
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesDemoScreen(onBack: () -> Unit) {
    val prefs: AppPreferences = koinInject()

    var username by remember { mutableStateOf(prefs.username) }
    var onboardingDone by remember { mutableStateOf(prefs.onboardingComplete) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Preferences") },
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
            UsernameField(
                username = username,
                onUsernameChange = { username = it },
                onSave = { prefs.username = username },
            )

            OnboardingToggle(
                isComplete = onboardingDone,
                onToggle = {
                    onboardingDone = it
                    prefs.onboardingComplete = it
                },
            )

            StoredValuesSection(prefs)

            OutlinedButton(
                onClick = {
                    prefs.clearAll()
                    username = ""
                    onboardingDone = false
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Clear All Preferences")
            }

            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Back")
            }
        }
    }
}

@Composable
private fun UsernameField(
    username: String,
    onUsernameChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChange,
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
            Text("Save Username")
        }
    }
}

@Composable
private fun OnboardingToggle(isComplete: Boolean, onToggle: (Boolean) -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "Onboarding Complete",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = isComplete, onCheckedChange = onToggle)
    }
}

@Composable
private fun StoredValuesSection(prefs: AppPreferences) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Stored Values", style = MaterialTheme.typography.titleSmall)
        Text(
            text = "Username: ${prefs.username.ifEmpty { "(not set)" }}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Onboarding: ${if (prefs.onboardingComplete) "done" else "pending"}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
