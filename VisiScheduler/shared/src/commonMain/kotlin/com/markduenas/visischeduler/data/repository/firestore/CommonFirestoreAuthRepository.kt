package com.markduenas.visischeduler.data.repository.firestore

import com.markduenas.visischeduler.domain.entities.NotificationPreferences
import com.markduenas.visischeduler.domain.entities.Role
import com.markduenas.visischeduler.domain.entities.User
import com.markduenas.visischeduler.domain.repository.AuthRepository
import com.markduenas.visischeduler.firebase.FirestoreDatabase
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Instant
import kotlin.time.Clock

/**
 * Cross-platform Firebase Auth implementation of AuthRepository.
 * Uses GitLive Firebase KMP SDK for both Android and iOS.
 */
class CommonFirestoreAuthRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirestoreDatabase
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

        firestore.getUser(uid)?.toUser()
            ?: throw Exception("User profile not found")
    }

    override suspend fun loginWithBiometric(): Result<User> = runCatching {
        // Biometric login requires platform-specific implementation
        // This is a placeholder that should be overridden per platform
        throw UnsupportedOperationException("Biometric login not supported in common code")
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

    override suspend fun isBiometricAvailable(): Boolean {
        // Platform-specific implementation required
        return false
    }

    override suspend fun enableBiometric(): Result<Unit> = runCatching {
        throw UnsupportedOperationException("Biometric requires platform-specific implementation")
    }

    override suspend fun disableBiometric(): Result<Unit> = runCatching {
        throw UnsupportedOperationException("Biometric requires platform-specific implementation")
    }

    override suspend fun verifyMfa(challengeId: String, code: String): Result<User> = runCatching {
        // MFA verification would require additional Firebase setup
        throw UnsupportedOperationException("MFA not yet implemented")
    }

    override suspend fun resendMfaCode(challengeId: String): Result<Unit> = runCatching {
        throw UnsupportedOperationException("MFA not yet implemented")
    }

    override suspend fun setupMfa(
        method: com.markduenas.visischeduler.presentation.viewmodel.auth.MfaMethod,
        destination: String
    ): Result<String> = runCatching {
        throw UnsupportedOperationException("MFA setup not yet implemented")
    }

    override suspend fun confirmMfaSetup(challengeId: String, code: String): Result<Unit> = runCatching {
        throw UnsupportedOperationException("MFA setup not yet implemented")
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
                } ?: NotificationPreferences()
            )
        } catch (e: Exception) {
            null
        }
    }
}
