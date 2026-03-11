package com.markduenas.visischeduler.platform

import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusDenied
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVAuthorizationStatusRestricted
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusDenied
import platform.UserNotifications.UNAuthorizationStatusNotDetermined
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNUserNotificationCenter
import platform.UIKit.UIApplication
import platform.Foundation.NSURL
import platform.UIKit.UIApplicationOpenSettingsURLString

class IosPermissionManager : PermissionManager {
    
    override suspend fun checkPermission(permission: Permission): PermissionStatus {
        return when (permission) {
            Permission.CAMERA -> {
                val status = authorizationStatusForMediaType(AVMediaTypeVideo)
                when (status) {
                    AVAuthorizationStatusAuthorized -> PermissionStatus.GRANTED
                    AVAuthorizationStatusDenied, AVAuthorizationStatusRestricted -> PermissionStatus.DENIED
                    AVAuthorizationStatusNotDetermined -> PermissionStatus.NOT_DETERMINED
                    else -> PermissionStatus.DENIED
                }
            }
            Permission.NOTIFICATIONS -> {
                // Since this is async on iOS, we'd ideally await the status
                // Simplified for now
                PermissionStatus.NOT_DETERMINED
            }
            Permission.LOCATION -> PermissionStatus.NOT_DETERMINED
        }
    }

    override suspend fun requestPermission(permission: Permission): PermissionStatus {
        return when (permission) {
            Permission.CAMERA -> {
                // Trigger native iOS dialog
                PermissionStatus.GRANTED // Simplified
            }
            Permission.NOTIFICATIONS -> {
                PermissionStatus.GRANTED // Simplified
            }
            Permission.LOCATION -> PermissionStatus.GRANTED // Simplified
        }
    }

    override fun openAppSettings() {
        val settingsUrl = NSURL.URLWithString(UIApplicationOpenSettingsURLString)
        if (settingsUrl != null && UIApplication.sharedApplication.canOpenURL(settingsUrl)) {
            UIApplication.sharedApplication.openURL(settingsUrl)
        }
    }
}

/**
 * Actual implementation of the factory function for iOS.
 */
actual fun getPermissionManager(): PermissionManager = throw IllegalStateException("Use Koin to inject PermissionManager")
