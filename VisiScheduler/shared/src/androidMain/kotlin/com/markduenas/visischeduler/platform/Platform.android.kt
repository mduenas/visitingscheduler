package com.markduenas.visischeduler.platform

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.lang.ref.WeakReference
import kotlinx.coroutines.CompletableDeferred

class AndroidPlatform : Platform {
    override val name: String = "Android"
    override val version: String = Build.VERSION.RELEASE
    override val isDebug: Boolean = false
}

actual fun getPlatform(): Platform = AndroidPlatform()

class AndroidPermissionManager(private val context: Context) : PermissionManager {

    override suspend fun checkPermission(permission: Permission): PermissionStatus {
        val androidPermission = manifestPermissionFor(permission) ?: return PermissionStatus.GRANTED
        return if (ContextCompat.checkSelfPermission(context, androidPermission) == PackageManager.PERMISSION_GRANTED) {
            PermissionStatus.GRANTED
        } else {
            val activity = currentActivity?.get()
            if (activity != null && ActivityCompat.shouldShowRequestPermissionRationale(activity, androidPermission)) {
                PermissionStatus.SHOW_RATIONALE
            } else {
                PermissionStatus.NOT_DETERMINED
            }
        }
    }

    override suspend fun requestPermission(permission: Permission): PermissionStatus {
        val androidPermission = manifestPermissionFor(permission) ?: return PermissionStatus.GRANTED
        if (ContextCompat.checkSelfPermission(context, androidPermission) == PackageManager.PERMISSION_GRANTED) {
            return PermissionStatus.GRANTED
        }
        val launcher = permissionLauncher ?: return PermissionStatus.DENIED
        val deferred = CompletableDeferred<Boolean>()
        pendingResult = deferred
        launcher.launch(androidPermission)
        val granted = deferred.await()
        return if (granted) {
            PermissionStatus.GRANTED
        } else {
            val activity = currentActivity?.get()
            if (activity != null && ActivityCompat.shouldShowRequestPermissionRationale(activity, androidPermission)) {
                PermissionStatus.SHOW_RATIONALE
            } else {
                PermissionStatus.DENIED
            }
        }
    }

    override fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun manifestPermissionFor(permission: Permission): String? = when (permission) {
        Permission.CAMERA -> Manifest.permission.CAMERA
        Permission.NOTIFICATIONS -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.POST_NOTIFICATIONS
        } else null
        Permission.LOCATION -> Manifest.permission.ACCESS_FINE_LOCATION
    }

    companion object {
        /** Set from MainActivity.onCreate via registerForActivityResult. */
        var permissionLauncher: ActivityResultLauncher<String>? = null
        var currentActivity: WeakReference<FragmentActivity>? = null
        var pendingResult: CompletableDeferred<Boolean>? = null

        fun onPermissionsResult(granted: Boolean) {
            pendingResult?.complete(granted)
            pendingResult = null
        }
    }
}

/**
 * Actual implementation of the factory function for Android.
 */
actual fun getPermissionManager(): PermissionManager = throw IllegalStateException("Use Koin to inject PermissionManager")
