package com.markduenas.visischeduler.platform

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.markduenas.visischeduler.data.local.VisiSchedulerDatabase
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
    single<BiometricHandler> { AndroidBiometricHandler() }

    // Platform-specific notification handler
    single<NotificationHandler> { AndroidNotificationHandler(androidContext()) }
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
 */
class AndroidBiometricHandler : BiometricHandler {
    // Note: Actual implementation would use androidx.biometric.BiometricPrompt
    // This is a placeholder that should be properly implemented

    override fun isAvailable(): Boolean = false

    override fun canAuthenticate(): Boolean = false

    override suspend fun authenticate(
        title: String,
        subtitle: String?,
        negativeButtonText: String
    ): BiometricResult {
        return BiometricResult.NotAvailable
    }
}

/**
 * Android implementation of NotificationHandler.
 */
class AndroidNotificationHandler(private val context: Context) : NotificationHandler {
    // Note: Actual implementation would use NotificationManager
    // This is a placeholder that should be properly implemented

    override suspend fun requestPermission(): Boolean = false

    override suspend fun showNotification(
        title: String,
        body: String,
        data: Map<String, String>?
    ) {
        // Implementation using NotificationManager
    }

    override suspend fun cancelNotification(id: Int) {
        // Implementation using NotificationManager
    }

    override suspend fun cancelAllNotifications() {
        // Implementation using NotificationManager
    }
}
