package com.markduenas.visischeduler.platform

import kotlin.coroutines.resume
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusDenied
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVAuthorizationStatusRestricted
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusDenied
import platform.UserNotifications.UNAuthorizationStatusNotDetermined
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNUserNotificationCenter

class IosPermissionManager : PermissionManager {

    override suspend fun checkPermission(permission: Permission): PermissionStatus = when (permission) {
        Permission.CAMERA -> checkCameraPermission()
        Permission.NOTIFICATIONS -> checkNotificationPermission()
        Permission.LOCATION -> PermissionStatus.NOT_DETERMINED
    }

    override suspend fun requestPermission(permission: Permission): PermissionStatus = when (permission) {
        Permission.CAMERA -> requestCameraPermission()
        Permission.NOTIFICATIONS -> requestNotificationPermission()
        Permission.LOCATION -> PermissionStatus.NOT_DETERMINED
    }

    override fun openAppSettings() {
        val settingsUrl = NSURL.URLWithString(UIApplicationOpenSettingsURLString)
        if (settingsUrl != null && UIApplication.sharedApplication.canOpenURL(settingsUrl)) {
            UIApplication.sharedApplication.openURL(settingsUrl)
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun checkCameraPermission(): PermissionStatus =
        when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
            AVAuthorizationStatusAuthorized -> PermissionStatus.GRANTED
            AVAuthorizationStatusDenied, AVAuthorizationStatusRestricted -> PermissionStatus.DENIED
            AVAuthorizationStatusNotDetermined -> PermissionStatus.NOT_DETERMINED
            else -> PermissionStatus.NOT_DETERMINED
        }

    private suspend fun checkNotificationPermission(): PermissionStatus =
        suspendCancellableCoroutine { cont ->
            UNUserNotificationCenter.currentNotificationCenter()
                .getNotificationSettingsWithCompletionHandler { settings ->
                    val status = when (settings?.authorizationStatus) {
                        UNAuthorizationStatusAuthorized,
                        UNAuthorizationStatusProvisional -> PermissionStatus.GRANTED
                        UNAuthorizationStatusDenied -> PermissionStatus.DENIED
                        UNAuthorizationStatusNotDetermined -> PermissionStatus.NOT_DETERMINED
                        else -> PermissionStatus.NOT_DETERMINED
                    }
                    cont.resume(status)
                }
        }

    @OptIn(ExperimentalForeignApi::class)
    private suspend fun requestCameraPermission(): PermissionStatus =
        suspendCancellableCoroutine { cont ->
            AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                cont.resume(if (granted) PermissionStatus.GRANTED else PermissionStatus.DENIED)
            }
        }

    private suspend fun requestNotificationPermission(): PermissionStatus =
        suspendCancellableCoroutine { cont ->
            UNUserNotificationCenter.currentNotificationCenter()
                .requestAuthorizationWithOptions(
                    UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
                ) { granted, _ ->
                    cont.resume(if (granted) PermissionStatus.GRANTED else PermissionStatus.DENIED)
                }
        }
}

/**
 * Actual implementation of the factory function for iOS.
 */
actual fun getPermissionManager(): PermissionManager = throw IllegalStateException("Use Koin to inject PermissionManager")

class IosPlatform : Platform {
    override val name: String = "iOS"
    override val version: String = platform.UIKit.UIDevice.currentDevice.systemVersion
    override val isDebug: Boolean = false
}

actual fun getPlatform(): Platform = IosPlatform()
