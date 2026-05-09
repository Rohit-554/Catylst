package io.jadu.catylst.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch

@Composable
fun rememberPermissionState(
    permission: Permission,
    controller: PermissionController,
): PermissionStateHolder {
    var status by remember { mutableStateOf(PermissionStatus.NOT_DETERMINED) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(permission) {
        status = controller.checkPermission(permission)
    }

    return PermissionStateHolder(
        status = status,
        onRequest = {
            scope.launch { status = controller.requestPermission(permission) }
        },
        onOpenSettings = controller::openAppSettings,
    )
}

data class PermissionStateHolder(
    val status: PermissionStatus,
    val onRequest: () -> Unit,
    val onOpenSettings: () -> Unit,
) {
    val isGranted: Boolean get() = status == PermissionStatus.GRANTED
}
