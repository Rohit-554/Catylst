package io.jadu.catylst.permissions

import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
internal class LocationPermissionDelegate(
    private val onResult: (PermissionStatus) -> Unit,
) : NSObject(), CLLocationManagerDelegateProtocol {

    private val manager = CLLocationManager()

    fun requestWhenInUse() {
        manager.delegate = this
        manager.requestWhenInUseAuthorization()
    }

    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        val status = when (manager.authorizationStatus()) {
            kCLAuthorizationStatusAuthorizedWhenInUse,
            kCLAuthorizationStatusAuthorizedAlways -> PermissionStatus.GRANTED
            kCLAuthorizationStatusDenied -> PermissionStatus.DENIED
            kCLAuthorizationStatusRestricted -> PermissionStatus.DENIED_ALWAYS
            else -> return
        }
        onResult(status)
    }
}
