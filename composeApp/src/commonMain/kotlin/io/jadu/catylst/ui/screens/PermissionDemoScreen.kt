package io.jadu.catylst.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.jadu.catylst.permissions.Permission
import io.jadu.catylst.permissions.PermissionController
import io.jadu.catylst.permissions.PermissionStateHolder
import io.jadu.catylst.permissions.PermissionStatus
import io.jadu.catylst.permissions.rememberPermissionController
import io.jadu.catylst.permissions.rememberPermissionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionDemoScreen(onBack: () -> Unit) {
    val controller = rememberPermissionController()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Permissions") },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Permission.entries.forEach { permission ->
                PermissionRow(permission = permission, controller = controller)
            }

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Back")
            }
        }
    }
}

@Composable
private fun PermissionRow(permission: Permission, controller: PermissionController) {
    val state = rememberPermissionState(permission, controller)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = permission.name, style = MaterialTheme.typography.titleSmall)
        PermissionStatusLabel(state)
        PermissionAction(state)
    }
}

@Composable
private fun PermissionStatusLabel(state: PermissionStateHolder) {
    Text(
        text = state.status.name,
        style = MaterialTheme.typography.bodySmall,
        color = when (state.status) {
            PermissionStatus.GRANTED -> MaterialTheme.colorScheme.primary
            PermissionStatus.DENIED_ALWAYS -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
    )
}

@Composable
private fun PermissionAction(state: PermissionStateHolder) {
    when {
        state.isGranted -> Unit
        state.status == PermissionStatus.DENIED_ALWAYS -> {
            Button(onClick = state.onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                Text("Open Settings")
            }
        }
        else -> {
            Button(onClick = state.onRequest, modifier = Modifier.fillMaxWidth()) {
                Text("Grant")
            }
        }
    }
}
