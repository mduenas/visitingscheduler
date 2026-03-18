package com.markduenas.visischeduler.platform

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

class AndroidPlatform : Platform {
    override val name: String = "Android"
    override val version: String = Build.VERSION.RELEASE
    override val isDebug: Boolean = false
}

actual fun getPlatform(): Platform = AndroidPlatform()

class AndroidPermissionManager(private val context: Context) : PermissionManager {
    
    override suspend fun checkPermission(permission: Permission): PermissionStatus {
        val androidPermission = getAndroidPermission(permission) ?: return PermissionStatus.GRANTED
        
        return when {
            ContextCompat.checkSelfPermission(context, androidPermission) == PackageManager.PERMISSION_GRANTED -> {
                PermissionStatus.GRANTED
            }
            else -> PermissionStatus.DENIED
        }
    }

    override suspend fun requestPermission(permission: Permission): PermissionStatus {
        // In a real Android implementation, this would trigger an Activity-based request
        // Since we're in a shared module, we usually handle the request in the UI layer
        // and use this manager primarily for status checking and settings navigation.
        return checkPermission(permission)
    }

    override fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun getAndroidPermission(permission: Permission): String? {
        return when (permission) {
            Permission.CAMERA -> Manifest.permission.CAMERA
            Permission.NOTIFICATIONS -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.POST_NOTIFICATIONS
            } else {
                null
            }
            Permission.LOCATION -> Manifest.permission.ACCESS_FINE_LOCATION
        }
    }
}

/**
 * Actual implementation of the factory function for Android.
 */
actual fun getPermissionManager(): PermissionManager = throw IllegalStateException("Use Koin to inject PermissionManager")
