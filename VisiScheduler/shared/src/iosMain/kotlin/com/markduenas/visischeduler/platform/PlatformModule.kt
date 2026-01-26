package com.markduenas.visischeduler.platform

import app.cash.sqldelight.driver.native.NativeSqliteDriver
import org.koin.dsl.module

/**
 * iOS-specific platform module providing platform implementations.
 */
val iosPlatformModule = module {

    // SQLDelight Native Driver for iOS
    single {
        NativeSqliteDriver(
            // schema = VisiSchedulerDatabase.Schema,
            name = "visischeduler.db"
        )
    }

    // Platform-specific secure storage (Keychain)
    single<SecureStorage> { SecureStorageImpl() }

    // Platform-specific biometric handler (Face ID / Touch ID)
    single<BiometricHandler> { IosBiometricHandler() }

    // Platform-specific notification handler (APNs)
    single<NotificationHandler> { IosNotificationHandler() }
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
 * iOS implementation of BiometricHandler using LocalAuthentication.
 */
class IosBiometricHandler : BiometricHandler {
    // Note: Actual implementation would use LAContext from LocalAuthentication
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
 * iOS implementation of NotificationHandler using APNs.
 */
class IosNotificationHandler : NotificationHandler {
    // Note: Actual implementation would use UNUserNotificationCenter
    // This is a placeholder that should be properly implemented

    override suspend fun requestPermission(): Boolean = false

    override suspend fun showNotification(
        title: String,
        body: String,
        data: Map<String, String>?
    ) {
        // Implementation using UNUserNotificationCenter
    }

    override suspend fun cancelNotification(id: Int) {
        // Implementation using UNUserNotificationCenter
    }

    override suspend fun cancelAllNotifications() {
        // Implementation using UNUserNotificationCenter
    }
}
