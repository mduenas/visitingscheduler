package com.markduenas.visischeduler.platform

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.markduenas.visischeduler.data.local.VisiSchedulerDatabase
import kotlin.coroutines.resume
import kotlin.random.Random
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.dsl.module
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAErrorUserCancel
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter

/**
 * iOS-specific platform module providing platform implementations.
 */
val iosPlatformModule = module {

    // SQLDelight Native Driver for iOS
    single<SqlDriver> {
        NativeSqliteDriver(
            schema = VisiSchedulerDatabase.Schema,
            name = "visischeduler.db"
        )
    }

    // Platform-specific secure storage (Keychain)
    single<SecureStorage> { SecureStorageImpl() }

    // Platform-specific biometric handler (Face ID / Touch ID)
    single<BiometricHandler> { IosBiometricHandler() }

    // Platform-specific notification handler (APNs)
    single<NotificationHandler> { IosNotificationHandler() }

    // Platform-specific permission manager
    single<PermissionManager> { IosPermissionManager() }
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
 * iOS implementation of BiometricHandler using LocalAuthentication (Face ID / Touch ID).
 */
class IosBiometricHandler : BiometricHandler {

    @OptIn(ExperimentalForeignApi::class)
    override fun isAvailable(): Boolean {
        val context = LAContext()
        return context.canEvaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, error = null)
    }

    override fun canAuthenticate(): Boolean = isAvailable()

    override suspend fun authenticate(
        title: String,
        subtitle: String?,
        negativeButtonText: String
    ): BiometricResult {
        if (!isAvailable()) return BiometricResult.NotAvailable

        return suspendCancellableCoroutine { continuation ->
            LAContext().evaluatePolicy(
                LAPolicyDeviceOwnerAuthenticationWithBiometrics,
                localizedReason = title
            ) { success, error ->
                if (success) {
                    continuation.resume(BiometricResult.Success)
                } else {
                    val code = error?.code?.toInt() ?: -1
                    val result = if (code == LAErrorUserCancel.toInt()) {
                        BiometricResult.Cancelled
                    } else {
                        BiometricResult.Error(code, error?.localizedDescription ?: "Authentication failed")
                    }
                    continuation.resume(result)
                }
            }
        }
    }
}

/**
 * iOS implementation of NotificationHandler using UNUserNotificationCenter.
 */
class IosNotificationHandler : NotificationHandler {

    override suspend fun requestPermission(): Boolean = suspendCancellableCoroutine { cont ->
        UNUserNotificationCenter.currentNotificationCenter()
            .requestAuthorizationWithOptions(
                UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
            ) { granted, _ -> cont.resume(granted) }
    }

    override suspend fun showNotification(
        title: String,
        body: String,
        data: Map<String, String>?
    ) {
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(body)
            setSound(UNNotificationSound.defaultSound())
        }
        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = Random.nextLong().toString(),
            content = content,
            trigger = null
        )
        suspendCancellableCoroutine<Unit> { cont ->
            UNUserNotificationCenter.currentNotificationCenter()
                .addNotificationRequest(request) { _ -> cont.resume(Unit) }
        }
    }

    override suspend fun cancelNotification(id: Int) {
        UNUserNotificationCenter.currentNotificationCenter()
            .removePendingNotificationRequestsWithIdentifiers(listOf(id.toString()))
    }

    override suspend fun cancelAllNotifications() {
        UNUserNotificationCenter.currentNotificationCenter()
            .removeAllPendingNotificationRequests()
    }
}

