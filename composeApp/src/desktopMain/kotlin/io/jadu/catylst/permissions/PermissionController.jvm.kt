package io.jadu.catylst.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

actual class PermissionController {
    actual suspend fun checkPermission(permission: Permission): PermissionStatus = PermissionStatus.GRANTED
    actual suspend fun requestPermission(permission: Permission): PermissionStatus = PermissionStatus.GRANTED
    actual fun openAppSettings() = Unit
}

@Composable
actual fun rememberPermissionController(): PermissionController = remember { PermissionController() }
