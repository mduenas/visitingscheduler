package com.markduenas.visischeduler.platform

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.biometric.BiometricManager as AndroidBiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.markduenas.visischeduler.data.local.VisiSchedulerDatabase
import java.lang.ref.WeakReference
import kotlin.coroutines.resume
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Android-specific platform module providing platform implementations.
 */
val androidPlatformModule = module {

    // SQLDelight Android Driver
    single<SqlDriver> {
        AndroidSqliteDriver(
            schema = VisiSchedulerDatabase.Schema,
            context = androidContext(),
            name = "visischeduler.db"
        )
    }

    // Platform-specific secure storage
    single<SecureStorage> { SecureStorageImpl(androidContext()) }

    // Platform-specific biometric handler
    single<BiometricHandler> { AndroidBiometricHandler(androidContext()) }

    // Platform-specific notification handler
    single<NotificationHandler> { AndroidNotificationHandler(androidContext()) }

    // Platform-specific permission manager
    single<PermissionManager> { AndroidPermissionManager(androidContext()) }
}

/**
 * Interface for biometric authentication.
 */
interface BiometricHandler {
    fun isAvailable(): Boolean
    fun canAuthenticate(): Boolean
    suspend fun authenticate(
        title: String,
        subtitle: String?,
        negativeButtonText: String
    ): BiometricResult
}

/**
 * Result of biometric authentication.
 */
sealed class BiometricResult {
    data object Success : BiometricResult()
    data class Error(val code: Int, val message: String) : BiometricResult()
    data object Cancelled : BiometricResult()
    data object NotAvailable : BiometricResult()
}

/**
 * Interface for notification handling.
 */
interface NotificationHandler {
    suspend fun requestPermission(): Boolean
    suspend fun showNotification(
        title: String,
        body: String,
        data: Map<String, String>? = null
    )
    suspend fun cancelNotification(id: Int)
    suspend fun cancelAllNotifications()
}

/**
 * Android implementation of BiometricHandler using AndroidX Biometric.
 * Requires the current FragmentActivity to be registered via [setActivity]
 * before calling [authenticate] — call this from MainActivity.onResume().
 */
class AndroidBiometricHandler(private val context: Context) : BiometricHandler {

    private val biometricManager = AndroidBiometricManager.from(context)

    override fun isAvailable(): Boolean =
        biometricManager.canAuthenticate(
            AndroidBiometricManager.Authenticators.BIOMETRIC_STRONG or
            AndroidBiometricManager.Authenticators.BIOMETRIC_WEAK
        ) == AndroidBiometricManager.BIOMETRIC_SUCCESS

    override fun canAuthenticate(): Boolean = isAvailable()

    override suspend fun authenticate(
        title: String,
        subtitle: String?,
        negativeButtonText: String
    ): BiometricResult {
        val activity = currentActivity?.get()
            ?: return BiometricResult.Error(-1, "No activity available for biometric prompt")

        return suspendCancellableCoroutine { continuation ->
            val executor = ContextCompat.getMainExecutor(context)
            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    continuation.resume(BiometricResult.Success)
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    val result = if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        BiometricResult.Cancelled
                    } else {
                        BiometricResult.Error(errorCode, errString.toString())
                    }
                    continuation.resume(result)
                }
                override fun onAuthenticationFailed() {
                    // User's biometric did not match — let them try again; don't resume yet
                }
            }

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .apply { subtitle?.let { setSubtitle(it) } }
                .setNegativeButtonText(negativeButtonText)
                .setAllowedAuthenticators(
                    AndroidBiometricManager.Authenticators.BIOMETRIC_STRONG or
                    AndroidBiometricManager.Authenticators.BIOMETRIC_WEAK
                )
                .build()

            activity.runOnUiThread {
                BiometricPrompt(activity, executor, callback).authenticate(promptInfo)
            }
        }
    }

    companion object {
        /** Updated by MainActivity.onResume / cleared on onPause. */
        var currentActivity: WeakReference<FragmentActivity>? = null
    }
}

/**
 * Android implementation of NotificationHandler using NotificationManagerCompat.
 */
class AndroidNotificationHandler(private val context: Context) : NotificationHandler {

    private val notificationManager = NotificationManagerCompat.from(context)

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Visit Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for visit scheduling and approval updates"
                enableVibration(true)
            }
            context.getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(channel)
        }
    }

    override suspend fun requestPermission(): Boolean = notificationManager.areNotificationsEnabled()

    override suspend fun showNotification(
        title: String,
        body: String,
        data: Map<String, String>?
    ) {
        if (!notificationManager.areNotificationsEnabled()) return
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        val id = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        notificationManager.notify(id, notification)
    }

    override suspend fun cancelNotification(id: Int) {
        notificationManager.cancel(id)
    }

    override suspend fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }

    companion object {
        private const val CHANNEL_ID = "visischeduler_visits"
    }
}

