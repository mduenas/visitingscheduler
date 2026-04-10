package com.markduenas.visischeduler.data.repository.firestore

import com.markduenas.visischeduler.domain.entities.DeviceSession
import com.markduenas.visischeduler.domain.entities.NotificationPreferences
import com.markduenas.visischeduler.domain.entities.Role
import com.markduenas.visischeduler.domain.entities.User
import com.markduenas.visischeduler.domain.repository.AuthRepository
import com.markduenas.visischeduler.firebase.FirestoreDatabase
import com.markduenas.visischeduler.platform.BiometricHandler
import com.markduenas.visischeduler.platform.BiometricResult
import com.markduenas.visischeduler.platform.DeviceInfo
import com.markduenas.visischeduler.platform.SecureStorage
import com.markduenas.visischeduler.platform.SecureStorageKeys
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Instant
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.Uuid

/**
 * Cross-platform Firebase Auth implementation of AuthRepository.
 * Uses GitLive Firebase KMP SDK for both Android and iOS.
 */
class CommonFirestoreAuthRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirestoreDatabase,
    private val biometricHandler: BiometricHandler,
    private val secureStorage: SecureStorage,
    private val deviceInfo: DeviceInfo
) : AuthRepository {

    override val currentUser: Flow<User?>
        get() = auth.authStateChanged.map { firebaseUser ->
            firebaseUser?.uid?.let { uid ->
                firestore.getUser(uid)?.toUser()
            }
        }

    override suspend fun isAuthenticated(): Boolean {
        return auth.currentUser != null
    }

    override suspend fun login(email: String, password: String): Result<User> = runCatching {
        val result = auth.signInWithEmailAndPassword(email, password)
        val uid = result.user?.uid ?: throw Exception("Login failed: no user ID")

        // Update last login
        firestore.updateUser(uid, mapOf(
            "lastLoginAt" to firestore.serverTimestamp()
        ))

        // Register/update device session
        upsertSessionForCurrentDevice(uid)

        firestore.getUser(uid)?.toUser()
            ?: throw Exception("User profile not found")
    }

    override suspend fun loginWithBiometric(): Result<User> = runCatching {
        if (!biometricHandler.isAvailable()) {
            throw UnsupportedOperationException("Biometric authentication is not available on this device")
        }
        val result = biometricHandler.authenticate(
            title = "Verify Identity",
            subtitle = "Use biometrics to sign in",
            negativeButtonText = "Cancel"
        )
        when (result) {
            is BiometricResult.Success -> {
                val uid = auth.currentUser?.uid
                    ?: throw Exception("No active session — please log in with your password")
                firestore.getUser(uid)?.toUser()
                    ?: throw Exception("User profile not found")
            }
            is BiometricResult.Cancelled -> throw Exception("Biometric authentication cancelled")
            is BiometricResult.NotAvailable -> throw UnsupportedOperationException("Biometric not available")
            is BiometricResult.Error -> throw Exception(result.message)
        }
    }

    override suspend fun register(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        phoneNumber: String?
    ): Result<User> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email, password)
        val uid = result.user?.uid ?: throw Exception("Registration failed: no user ID")
        val now = Clock.System.now()

        val userData = mapOf<String, Any?>(
            "email" to email,
            "firstName" to firstName,
            "lastName" to lastName,
            "role" to Role.PENDING_VISITOR.name,
            "phoneNumber" to phoneNumber,
            "isActive" to true,
            "isEmailVerified" to false,
            "createdAt" to firestore.serverTimestamp(),
            "updatedAt" to firestore.serverTimestamp(),
            "associatedBeneficiaryIds" to emptyList<String>(),
            "notificationPreferences" to mapOf(
                "emailNotifications" to true,
                "pushNotifications" to true,
                "smsNotifications" to false,
                "visitReminders" to true,
                "approvalNotifications" to true,
                "scheduleChanges" to true
            )
        )

        firestore.createUser(uid, userData)

        User(
            id = uid,
            email = email,
            firstName = firstName,
            lastName = lastName,
            role = Role.PENDING_VISITOR,
            phoneNumber = phoneNumber,
            isActive = true,
            isEmailVerified = false,
            createdAt = now,
            updatedAt = now
        )
    }

    override suspend fun logout(): Result<Unit> = runCatching {
        auth.signOut()
    }

    override suspend fun requestPasswordReset(email: String): Result<Unit> = runCatching {
        auth.sendPasswordResetEmail(email)
    }

    override suspend fun resetPassword(token: String, newPassword: String): Result<Unit> = runCatching {
        auth.confirmPasswordReset(token, newPassword)
    }

    override suspend fun verifyEmail(token: String): Result<Unit> = runCatching {
        // Email verification is handled automatically by Firebase
        // This would be called after clicking the verification link
        auth.currentUser?.reload()
        val isVerified = auth.currentUser?.isEmailVerified ?: false
        if (!isVerified) {
            throw Exception("Email not verified")
        }

        // Update user profile
        auth.currentUser?.uid?.let { uid ->
            firestore.updateUser(uid, mapOf(
                "isEmailVerified" to true,
                "updatedAt" to firestore.serverTimestamp()
            ))
        }
    }

    override suspend fun refreshToken(): Result<String> = runCatching {
        auth.currentUser?.getIdToken(true)
            ?: throw Exception("No authenticated user")
    }

    override suspend fun updateFcmToken(fcmToken: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: throw Exception("No authenticated user")
        firestore.updateUser(uid, mapOf(
            "fcmToken" to fcmToken,
            "updatedAt" to firestore.serverTimestamp()
        ))
    }

    override suspend fun isBiometricAvailable(): Boolean = biometricHandler.isAvailable()

    override suspend fun enableBiometric(): Result<Unit> = runCatching {
        if (!biometricHandler.isAvailable()) {
            throw UnsupportedOperationException("Biometric authentication is not available on this device")
        }
        val result = biometricHandler.authenticate(
            title = "Enable Biometric Login",
            subtitle = "Confirm your identity to enable biometric sign-in",
            negativeButtonText = "Cancel"
        )
        when (result) {
            is BiometricResult.Success -> Unit
            is BiometricResult.Cancelled -> throw Exception("Biometric confirmation cancelled")
            is BiometricResult.NotAvailable -> throw UnsupportedOperationException("Biometric not available")
            is BiometricResult.Error -> throw Exception(result.message)
        }
    }

    override suspend fun disableBiometric(): Result<Unit> = Result.success(Unit)

    override suspend fun loginWithMfaChallenge(userId: String, email: String): Result<String> = runCatching {
        createAndSendMfaChallenge(userId, email)
    }

    override suspend fun verifyMfa(challengeId: String, code: String): Result<User> = runCatching {
        val userId = validateAndConsumeChallenge(challengeId, code)
        firestore.getUser(userId)?.toUser()
            ?: throw Exception("User profile not found after MFA verification")
    }

    override suspend fun resendMfaCode(challengeId: String): Result<String> = runCatching {
        val challenge = firestore.getMfaChallenge(challengeId)
            ?: throw Exception("Challenge not found")
        val userId = challenge.get<String>("userId") ?: throw Exception("Invalid challenge data")
        val destination = challenge.get<String>("destination") ?: throw Exception("Invalid challenge data")
        firestore.markMfaChallengeUsed(challengeId)
        createAndSendMfaChallenge(userId, destination)
    }

    override suspend fun setupMfa(
        method: com.markduenas.visischeduler.presentation.viewmodel.auth.MfaMethod,
        destination: String
    ): Result<String> = runCatching {
        if (method != com.markduenas.visischeduler.presentation.viewmodel.auth.MfaMethod.EMAIL) {
            throw UnsupportedOperationException("Only email MFA is currently supported")
        }
        val uid = auth.currentUser?.uid ?: throw Exception("Not authenticated")
        createAndSendMfaChallenge(uid, destination)
    }

    override suspend fun confirmMfaSetup(challengeId: String, code: String): Result<Unit> = runCatching {
        val userId = validateAndConsumeChallenge(challengeId, code)
        val challenge = firestore.getMfaChallenge(challengeId)
        val destination = challenge?.get<String>("destination") ?: return@runCatching
        firestore.updateUser(userId, mapOf(
            "mfaEnabled" to true,
            "mfaEmail" to destination,
            "updatedAt" to firestore.serverTimestamp()
        ))
    }

    // ==================== Session Management ====================

    override suspend fun getActiveSessions(): Result<List<DeviceSession>> = runCatching {
        val uid = auth.currentUser?.uid ?: throw Exception("Not authenticated")
        val currentDeviceId = getOrCreateDeviceId()
        firestore.getActiveSessions(uid).mapNotNull { doc ->
            doc.toDeviceSession(currentDeviceId)
        }
    }

    override suspend fun revokeSession(deviceId: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: throw Exception("Not authenticated")
        firestore.revokeSession(uid, deviceId)
        if (deviceId == getOrCreateDeviceId()) {
            auth.signOut()
        }
    }

    override suspend fun revokeAllSessions(): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: throw Exception("Not authenticated")
        firestore.revokeAllSessions(uid)
        auth.signOut()
    }

    override suspend fun updateCurrentSessionActivity(): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: return@runCatching
        val deviceId = getOrCreateDeviceId()
        firestore.upsertSession(uid, deviceId, mapOf(
            "lastActiveAt" to firestore.serverTimestamp()
        ))
    }

    // ==================== Session Helpers ====================

    private fun getOrCreateDeviceId(): String {
        return secureStorage.getString(SecureStorageKeys.DEVICE_ID) ?: run {
            @OptIn(kotlin.uuid.ExperimentalUuidApi::class)
            val newId = Uuid.random().toString()
            secureStorage.putString(SecureStorageKeys.DEVICE_ID, newId)
            newId
        }
    }

    private suspend fun upsertSessionForCurrentDevice(userId: String) {
        val deviceId = getOrCreateDeviceId()
        firestore.upsertSession(userId, deviceId, mapOf(
            "deviceId" to deviceId,
            "deviceName" to deviceInfo.deviceName,
            "deviceType" to deviceInfo.deviceType,
            "userId" to userId,
            "createdAt" to Clock.System.now().toEpochMilliseconds(),
            "lastActiveAt" to firestore.serverTimestamp(),
            "isRevoked" to false
        ))
    }

    private fun DocumentSnapshot.toDeviceSession(currentDeviceId: String): DeviceSession? {
        return try {
            DeviceSession(
                deviceId = id,
                deviceName = get("deviceName") ?: "Unknown Device",
                deviceType = get("deviceType") ?: "Unknown",
                userId = get("userId") ?: return null,
                createdAt = get<Long?>("createdAt")?.let { Instant.fromEpochMilliseconds(it) }
                    ?: Instant.fromEpochMilliseconds(0),
                lastActiveAt = get<Long?>("lastActiveAt")?.let { Instant.fromEpochMilliseconds(it) }
                    ?: Instant.fromEpochMilliseconds(0),
                isRevoked = get("isRevoked") ?: false,
                isCurrent = id == currentDeviceId
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Generates a 6-digit OTP, stores it in Firestore with a 10-minute TTL, and emails it.
     * Returns the new challenge document ID.
     */
    private suspend fun createAndSendMfaChallenge(userId: String, email: String): String {
        val code = (100000..999999).random().toString()
        val expiresAt = Clock.System.now().plus(10.minutes).toEpochMilliseconds()
        val challengeId = firestore.createMfaChallenge(mapOf(
            "userId" to userId,
            "code" to code,
            "method" to "EMAIL",
            "destination" to email,
            "expiresAt" to expiresAt,
            "used" to false
        ))
        firestore.sendMfaEmail(email, code)
        return challengeId
    }

    /**
     * Validates the OTP against the stored challenge, marks it used, and returns the userId.
     */
    private suspend fun validateAndConsumeChallenge(challengeId: String, code: String): String {
        val challenge = firestore.getMfaChallenge(challengeId)
            ?: throw Exception("Verification code not found or expired")
        val used = challenge.get<Boolean>("used") ?: false
        if (used) throw Exception("This code has already been used")
        val expiresAt = challenge.get<Long>("expiresAt") ?: 0L
        if (Clock.System.now().toEpochMilliseconds() > expiresAt) throw Exception("Verification code has expired")
        val storedCode = challenge.get<String>("code") ?: throw Exception("Invalid challenge data")
        if (storedCode != code) throw Exception("Incorrect verification code")
        firestore.markMfaChallengeUsed(challengeId)
        return challenge.get("userId") ?: throw Exception("Invalid challenge data")
    }

    // ==================== Mapping Functions ====================

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.toUser(): User? {
        return try {
            val notifPrefs = get<Map<String, Any?>?>("notificationPreferences")

            User(
                id = id,
                email = get("email") ?: return null,
                firstName = get("firstName") ?: return null,
                lastName = get("lastName") ?: return null,
                role = Role.valueOf(get("role") ?: "PENDING_VISITOR"),
                phoneNumber = get("phoneNumber"),
                profileImageUrl = get("profileImageUrl"),
                isActive = get("isActive") ?: true,
                isEmailVerified = get("isEmailVerified") ?: false,
                createdAt = get<Long?>("createdAt")?.let { Instant.fromEpochMilliseconds(it) }
                    ?: Instant.fromEpochMilliseconds(0),
                updatedAt = get<Long?>("updatedAt")?.let { Instant.fromEpochMilliseconds(it) }
                    ?: Instant.fromEpochMilliseconds(0),
                lastLoginAt = get<Long?>("lastLoginAt")?.let { Instant.fromEpochMilliseconds(it) },
                associatedBeneficiaryIds = get("associatedBeneficiaryIds") ?: emptyList(),
                notificationPreferences = notifPrefs?.let {
                    NotificationPreferences(
                        emailNotifications = it["emailNotifications"] as? Boolean ?: true,
                        pushNotifications = it["pushNotifications"] as? Boolean ?: true,
                        smsNotifications = it["smsNotifications"] as? Boolean ?: false,
                        visitReminders = it["visitReminders"] as? Boolean ?: true,
                        approvalNotifications = it["approvalNotifications"] as? Boolean ?: true,
                        scheduleChanges = it["scheduleChanges"] as? Boolean ?: true
                    )
                } ?: NotificationPreferences(),
                mfaEnabled = get("mfaEnabled") ?: false,
                mfaEmail = get("mfaEmail")
            )
        } catch (e: Exception) {
            null
        }
    }
}
