package com.markduenas.visischeduler.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.markduenas.visischeduler.domain.entities.NotificationPreferences
import com.markduenas.visischeduler.domain.entities.Role
import com.markduenas.visischeduler.domain.entities.User
import com.markduenas.visischeduler.domain.repository.UserRepository
import com.markduenas.visischeduler.firebase.FirestoreDatabase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.datetime.Instant

/**
 * Firestore implementation of UserRepository.
 * Uses Firebase Firestore as the backend database.
 */
class FirestoreUserRepository(
    private val firestore: FirestoreDatabase,
    private val auth: FirebaseAuth
) : UserRepository {

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    override val currentUser: Flow<User?> = callbackFlow {
        val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val userId = firebaseAuth.currentUser?.uid
            if (userId != null) {
                // Note: In a real implementation, we'd listen to the user document
                // For simplicity, we'll just emit based on auth state
            }
            trySend(null)
        }
        auth.addAuthStateListener(authListener)

        // Also listen to user document if authenticated
        currentUserId?.let { userId ->
            firestore.listenToUser(userId).collect { doc ->
                trySend(doc?.toUser())
            }
        }

        awaitClose { auth.removeAuthStateListener(authListener) }
    }

    override suspend fun getUserById(userId: String): Result<User> = runCatching {
        firestore.getUser(userId)?.toUser()
            ?: throw Exception("User not found")
    }

    override suspend fun getUserByEmail(email: String): Result<User> = runCatching {
        val docs = firestore.query(FirestoreDatabase.COLLECTION_USERS, "email", email)
        docs.firstOrNull()?.toUser()
            ?: throw Exception("User not found")
    }

    override fun getAllUsers(): Flow<List<User>> {
        return firestore.listenToCollection(FirestoreDatabase.COLLECTION_USERS)
            .map { docs -> docs.mapNotNull { it.toUser() } }
    }

    override fun getUsersByRole(role: Role): Flow<List<User>> {
        return firestore.listenToQuery(
            FirestoreDatabase.COLLECTION_USERS,
            "role",
            role.name
        ).map { docs -> docs.mapNotNull { it.toUser() } }
    }

    override fun getPendingVisitors(): Flow<List<User>> {
        return getUsersByRole(Role.PENDING_VISITOR)
    }

    override suspend fun searchUsers(query: String): Result<List<User>> = runCatching {
        // Simple search - in production would use Algolia or similar
        val allUsers = firestore.getAll(FirestoreDatabase.COLLECTION_USERS)
        allUsers.mapNotNull { it.toUser() }
            .filter { user ->
                user.firstName.contains(query, ignoreCase = true) ||
                user.lastName.contains(query, ignoreCase = true) ||
                user.email.contains(query, ignoreCase = true)
            }
    }

    override suspend fun updateProfile(
        firstName: String?,
        lastName: String?,
        phoneNumber: String?,
        profileImageUrl: String?
    ): Result<User> = runCatching {
        val userId = currentUserId ?: throw Exception("User not authenticated")

        val updates = mutableMapOf<String, Any?>("updatedAt" to Timestamp.now())
        firstName?.let { updates["firstName"] = it }
        lastName?.let { updates["lastName"] = it }
        phoneNumber?.let { updates["phoneNumber"] = it }
        profileImageUrl?.let { updates["profileImageUrl"] = it }

        firestore.updateUser(userId, updates)

        firestore.getUser(userId)?.toUser()
            ?: throw Exception("User not found after update")
    }

    override suspend fun updateNotificationPreferences(
        preferences: NotificationPreferences
    ): Result<User> = runCatching {
        val userId = currentUserId ?: throw Exception("User not authenticated")

        val updates = mapOf<String, Any?>(
            "notificationPreferences" to mapOf(
                "emailNotifications" to preferences.emailNotifications,
                "pushNotifications" to preferences.pushNotifications,
                "smsNotifications" to preferences.smsNotifications,
                "visitReminders" to preferences.visitReminders,
                "approvalNotifications" to preferences.approvalNotifications,
                "scheduleChanges" to preferences.scheduleChanges
            ),
            "updatedAt" to Timestamp.now()
        )

        firestore.updateUser(userId, updates)

        firestore.getUser(userId)?.toUser()
            ?: throw Exception("User not found after update")
    }

    override suspend fun changePassword(
        currentPassword: String,
        newPassword: String
    ): Result<Unit> = runCatching {
        val user = auth.currentUser ?: throw Exception("User not authenticated")
        val email = user.email ?: throw Exception("User email not found")

        // Re-authenticate user
        val credential = com.google.firebase.auth.EmailAuthProvider
            .getCredential(email, currentPassword)
        user.reauthenticate(credential).await()

        // Update password
        user.updatePassword(newPassword).await()
    }

    override suspend fun updateUserRole(userId: String, newRole: Role): Result<User> = runCatching {
        val updates = mapOf<String, Any?>(
            "role" to newRole.name,
            "updatedAt" to Timestamp.now()
        )

        firestore.updateUser(userId, updates)

        firestore.getUser(userId)?.toUser()
            ?: throw Exception("User not found after update")
    }

    override suspend fun approveVisitor(userId: String): Result<User> = runCatching {
        val updates = mapOf<String, Any?>(
            "role" to Role.APPROVED_VISITOR.name,
            "updatedAt" to Timestamp.now()
        )

        firestore.updateUser(userId, updates)

        firestore.getUser(userId)?.toUser()
            ?: throw Exception("User not found after approval")
    }

    override suspend fun denyVisitor(userId: String, reason: String): Result<Unit> = runCatching {
        // Could store denial reason or delete user
        firestore.delete(FirestoreDatabase.COLLECTION_USERS, userId)
    }

    override suspend fun deactivateUser(userId: String): Result<User> = runCatching {
        val updates = mapOf<String, Any?>(
            "isActive" to false,
            "updatedAt" to Timestamp.now()
        )

        firestore.updateUser(userId, updates)

        firestore.getUser(userId)?.toUser()
            ?: throw Exception("User not found after deactivation")
    }

    override suspend fun reactivateUser(userId: String): Result<User> = runCatching {
        val updates = mapOf<String, Any?>(
            "isActive" to true,
            "updatedAt" to Timestamp.now()
        )

        firestore.updateUser(userId, updates)

        firestore.getUser(userId)?.toUser()
            ?: throw Exception("User not found after reactivation")
    }

    override suspend fun associateBeneficiary(beneficiaryId: String): Result<User> = runCatching {
        val userId = currentUserId ?: throw Exception("User not authenticated")
        val user = firestore.getUser(userId)?.toUser()
            ?: throw Exception("User not found")

        val newAssociations = (user.associatedBeneficiaryIds + beneficiaryId).distinct()

        val updates = mapOf<String, Any?>(
            "associatedBeneficiaryIds" to newAssociations,
            "updatedAt" to Timestamp.now()
        )

        firestore.updateUser(userId, updates)

        firestore.getUser(userId)?.toUser()
            ?: throw Exception("User not found after update")
    }

    override suspend fun removeBeneficiaryAssociation(beneficiaryId: String): Result<User> = runCatching {
        val userId = currentUserId ?: throw Exception("User not authenticated")
        val user = firestore.getUser(userId)?.toUser()
            ?: throw Exception("User not found")

        val newAssociations = user.associatedBeneficiaryIds - beneficiaryId

        val updates = mapOf<String, Any?>(
            "associatedBeneficiaryIds" to newAssociations,
            "updatedAt" to Timestamp.now()
        )

        firestore.updateUser(userId, updates)

        firestore.getUser(userId)?.toUser()
            ?: throw Exception("User not found after update")
    }

    override suspend fun uploadProfileImage(imageData: ByteArray): Result<String> = runCatching {
        // In production, would upload to Firebase Storage
        // For now, return a placeholder URL
        val userId = currentUserId ?: throw Exception("User not authenticated")
        "https://storage.example.com/profiles/$userId.jpg"
    }

    override suspend fun deleteAccount(): Result<Unit> = runCatching {
        val userId = currentUserId ?: throw Exception("User not authenticated")

        // Delete user document
        firestore.delete(FirestoreDatabase.COLLECTION_USERS, userId)

        // Delete Firebase Auth account
        auth.currentUser?.delete()?.await()
    }

    override suspend fun syncUser(): Result<User> = runCatching {
        val userId = currentUserId ?: throw Exception("User not authenticated")
        firestore.getUser(userId)?.toUser()
            ?: throw Exception("User not found")
    }

    // ==================== Mapping Functions ====================

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.toUser(): User? {
        return try {
            val prefs = (get("notificationPreferences") as? Map<String, Any?>)?.let { p ->
                NotificationPreferences(
                    emailNotifications = p["emailNotifications"] as? Boolean ?: true,
                    pushNotifications = p["pushNotifications"] as? Boolean ?: true,
                    smsNotifications = p["smsNotifications"] as? Boolean ?: false,
                    visitReminders = p["visitReminders"] as? Boolean ?: true,
                    approvalNotifications = p["approvalNotifications"] as? Boolean ?: true,
                    scheduleChanges = p["scheduleChanges"] as? Boolean ?: true
                )
            } ?: NotificationPreferences()

            User(
                id = id,
                email = getString("email") ?: return null,
                firstName = getString("firstName") ?: "",
                lastName = getString("lastName") ?: "",
                role = Role.valueOf(getString("role") ?: "PENDING_VISITOR"),
                phoneNumber = getString("phoneNumber"),
                profileImageUrl = getString("profileImageUrl"),
                isActive = getBoolean("isActive") ?: true,
                isEmailVerified = getBoolean("isEmailVerified") ?: false,
                createdAt = getTimestamp("createdAt")?.let {
                    Instant.fromEpochMilliseconds(it.toDate().time)
                } ?: Instant.fromEpochMilliseconds(0),
                updatedAt = getTimestamp("updatedAt")?.let {
                    Instant.fromEpochMilliseconds(it.toDate().time)
                } ?: Instant.fromEpochMilliseconds(0),
                lastLoginAt = getTimestamp("lastLoginAt")?.let {
                    Instant.fromEpochMilliseconds(it.toDate().time)
                },
                associatedBeneficiaryIds = (get("associatedBeneficiaryIds") as? List<*>)
                    ?.filterIsInstance<String>() ?: emptyList(),
                notificationPreferences = prefs
            )
        } catch (e: Exception) {
            null
        }
    }
}
