package io.jadu.catylst.permissions

import androidx.compose.runtime.Composable

expect class PermissionController {
    suspend fun checkPermission(permission: Permission): PermissionStatus
    suspend fun requestPermission(permission: Permission): PermissionStatus
    fun openAppSettings()
}

@Composable
expect fun rememberPermissionController(): PermissionController
