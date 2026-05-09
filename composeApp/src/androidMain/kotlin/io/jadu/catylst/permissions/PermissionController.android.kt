package io.jadu.catylst.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual class PermissionController(private val activity: ComponentActivity) {

    actual suspend fun checkPermission(permission: Permission): PermissionStatus {
        val manifest = permission.toManifestString() ?: return PermissionStatus.GRANTED
        return when {
            isGranted(manifest) -> PermissionStatus.GRANTED
            isPermanentlyDenied(manifest) -> PermissionStatus.DENIED_ALWAYS
            else -> PermissionStatus.DENIED
        }
    }

    actual suspend fun requestPermission(permission: Permission): PermissionStatus =
        suspendCancellableCoroutine { cont ->
            val manifest = permission.toManifestString() ?: run {
                cont.resume(PermissionStatus.GRANTED)
                return@suspendCancellableCoroutine
            }
            val key = "permission_${permission.name}_${System.currentTimeMillis()}"
            val launcher = activity.activityResultRegistry.register(
                key,
                ActivityResultContracts.RequestPermission(),
            ) { granted ->
                markAsked(manifest)
                cont.resume(resolvePostRequest(granted, manifest))
            }
            launcher.launch(manifest)
        }

    actual fun openAppSettings() {
        activity.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", activity.packageName, null)
            }
        )
    }

    private fun isGranted(manifest: String): Boolean =
        ContextCompat.checkSelfPermission(activity, manifest) == PackageManager.PERMISSION_GRANTED

    private fun isPermanentlyDenied(manifest: String): Boolean =
        !activity.shouldShowRequestPermissionRationale(manifest) && wasAsked(manifest)

    private fun resolvePostRequest(granted: Boolean, manifest: String): PermissionStatus = when {
        granted -> PermissionStatus.GRANTED
        !activity.shouldShowRequestPermissionRationale(manifest) -> PermissionStatus.DENIED_ALWAYS
        else -> PermissionStatus.DENIED
    }

    private fun wasAsked(manifest: String): Boolean =
        prefs().getBoolean(manifest, false)

    private fun markAsked(manifest: String) =
        prefs().edit().putBoolean(manifest, true).apply()

    private fun prefs() =
        activity.getSharedPreferences("permission_prefs", Context.MODE_PRIVATE)

    private fun Permission.toManifestString(): String? = when (this) {
        Permission.CAMERA -> Manifest.permission.CAMERA
        Permission.LOCATION -> Manifest.permission.ACCESS_FINE_LOCATION
        Permission.NOTIFICATIONS -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.POST_NOTIFICATIONS else null
        Permission.RECORD_AUDIO -> Manifest.permission.RECORD_AUDIO
        Permission.STORAGE -> Manifest.permission.READ_EXTERNAL_STORAGE
    }
}

@Composable
actual fun rememberPermissionController(): PermissionController {
    val activity = LocalContext.current as ComponentActivity
    return remember(activity) { PermissionController(activity) }
}
