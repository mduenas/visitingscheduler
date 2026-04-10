package com.markduenas.visischeduler.data.repository.firestore

import com.markduenas.visischeduler.domain.entities.NotificationPreferences
import com.markduenas.visischeduler.domain.entities.Role
import com.markduenas.visischeduler.domain.entities.User
import com.markduenas.visischeduler.domain.repository.UserRepository
import com.markduenas.visischeduler.firebase.FirestoreDatabase
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.firestore.DocumentSnapshot
import com.markduenas.visischeduler.platform.toStorageData
import dev.gitlive.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlin.time.Instant
import kotlin.time.Clock

/**
 * Cross-platform Firestore implementation of UserRepository.
 */
class CommonFirestoreUserRepository(
    private val firestore: FirestoreDatabase,
    private val auth: FirebaseAuth,
    private val storage: FirebaseStorage
) : UserRepository {

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    override val currentUser: Flow<User?>
        get() {
            val userId = currentUserId ?: return flowOf(null)
            return firestore.listenToUser(userId).map { doc ->
                doc?.toUser()
            }
        }

    override suspend fun getUserById(userId: String): Result<User> = runCatching {
        firestore.getUser(userId)?.toUser()
            ?: throw Exception("User not found")
    }

    override suspend fun getUserByEmail(email: String): Result<User> = runCatching {
        val users = firestore.query(FirestoreDatabase.COLLECTION_USERS, "email", email)
        users.firstOrNull()?.toUser()
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
        return firestore.listenToQuery(
            FirestoreDatabase.COLLECTION_USERS,
            "role",
            Role.PENDING_VISITOR.name
        ).map { docs ->
            docs.mapNotNull { it.toUser() }
                .filter { it.isActive }
        }
    }

    override suspend fun searchUsers(query: String): Result<List<User>> = runCatching {
        val allUsers = firestore.getAll(FirestoreDatabase.COLLECTION_USERS)
        allUsers.mapNotNull { it.toUser() }
            .filter { user ->
                user.firstName.contains(query, ignoreCase = true) ||
                user.lastName.contains(query, ignoreCase = true) ||
                user.fullName.contains(query, ignoreCase = true) ||
                user.email.contains(query, ignoreCase = true)
            }
    }

    override suspend fun updateProfile(
        firstName: String?,
        lastName: String?,
        phoneNumber: String?,
        profileImageUrl: String?
    ): Result<User> = runCatching {
        val userId = currentUserId ?: throw Exception("Not authenticated")

        val updates = mutableMapOf<String, Any?>(
            "updatedAt" to firestore.serverTimestamp()
        )
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
        val userId = currentUserId ?: throw Exception("Not authenticated")

        val preferencesMap = mapOf(
            "emailNotifications" to preferences.emailNotifications,
            "pushNotifications" to preferences.pushNotifications,
            "smsNotifications" to preferences.smsNotifications,
            "visitReminders" to preferences.visitReminders,
            "approvalNotifications" to preferences.approvalNotifications,
            "scheduleChanges" to preferences.scheduleChanges
        )

        firestore.updateUser(userId, mapOf(
            "notificationPreferences" to preferencesMap,
            "updatedAt" to firestore.serverTimestamp()
        ))

        firestore.getUser(userId)?.toUser()
            ?: throw Exception("User not found after update")
    }

    override suspend fun changePassword(
        currentPassword: String,
        newPassword: String
    ): Result<Unit> = runCatching {
        val user = auth.currentUser ?: throw Exception("Not authenticated")
        user.updatePassword(newPassword)
    }

    override suspend fun updateUserRole(userId: String, newRole: Role): Result<User> = runCatching {
        firestore.updateUser(userId, mapOf(
            "role" to newRole.name,
            "updatedAt" to firestore.serverTimestamp()
        ))

        firestore.getUser(userId)?.toUser()
            ?: throw Exception("User not found after update")
    }

    override suspend fun approveVisitor(userId: String): Result<User> = runCatching {
        firestore.updateUser(userId, mapOf(
            "role" to Role.APPROVED_VISITOR.name,
            "updatedAt" to firestore.serverTimestamp()
        ))

        firestore.getUser(userId)?.toUser()
            ?: throw Exception("User not found after approval")
    }

    override suspend fun denyVisitor(userId: String, reason: String): Result<Unit> = runCatching {
        firestore.updateUser(userId, mapOf(
            "isActive" to false,
            "denialReason" to reason,
            "updatedAt" to firestore.serverTimestamp()
        ))
    }

    override suspend fun deactivateUser(userId: String): Result<User> = runCatching {
        firestore.updateUser(userId, mapOf(
            "isActive" to false,
            "updatedAt" to firestore.serverTimestamp()
        ))

        firestore.getUser(userId)?.toUser()
            ?: throw Exception("User not found after deactivation")
    }

    override suspend fun reactivateUser(userId: String): Result<User> = runCatching {
        firestore.updateUser(userId, mapOf(
            "isActive" to true,
            "updatedAt" to firestore.serverTimestamp()
        ))

        firestore.getUser(userId)?.toUser()
            ?: throw Exception("User not found after reactivation")
    }

    override suspend fun associateBeneficiary(beneficiaryId: String): Result<User> = runCatching {
        val userId = currentUserId ?: throw Exception("Not authenticated")
        val user = firestore.getUser(userId)?.toUser()
            ?: throw Exception("User not found")

        val updatedIds = (user.associatedBeneficiaryIds + beneficiaryId).distinct()

        firestore.updateUser(userId, mapOf(
            "associatedBeneficiaryIds" to updatedIds,
            "updatedAt" to firestore.serverTimestamp()
        ))

        firestore.getUser(userId)?.toUser()
            ?: throw Exception("User not found after update")
    }

    override suspend fun removeBeneficiaryAssociation(beneficiaryId: String): Result<User> = runCatching {
        val userId = currentUserId ?: throw Exception("Not authenticated")
        val user = firestore.getUser(userId)?.toUser()
            ?: throw Exception("User not found")

        val updatedIds = user.associatedBeneficiaryIds - beneficiaryId

        firestore.updateUser(userId, mapOf(
            "associatedBeneficiaryIds" to updatedIds,
            "updatedAt" to firestore.serverTimestamp()
        ))

        firestore.getUser(userId)?.toUser()
            ?: throw Exception("User not found after update")
    }

    override suspend fun uploadProfileImage(imageData: ByteArray): Result<String> = runCatching {
        val userId = currentUserId ?: throw Exception("Not authenticated")
        val ref = storage.reference("profile_images/$userId.jpg")
        ref.putData(imageData.toStorageData())
        val downloadUrl = ref.getDownloadUrl()
        // Persist the URL in the user's Firestore document
        firestore.updateUser(userId, mapOf("photoUrl" to downloadUrl))
        downloadUrl
    }

    override suspend fun deleteAccount(): Result<Unit> = runCatching {
        val userId = currentUserId ?: throw Exception("Not authenticated")

        firestore.updateUser(userId, mapOf(
            "isActive" to false,
            "deletedAt" to firestore.serverTimestamp(),
            "updatedAt" to firestore.serverTimestamp()
        ))

        auth.currentUser?.delete()
    }

    override suspend fun syncUser(): Result<User> = runCatching {
        val userId = currentUserId ?: throw Exception("Not authenticated")
        firestore.getUser(userId)?.toUser()
            ?: throw Exception("User not found")
    }

    override suspend fun enableMfa(email: String): Result<Unit> = runCatching {
        val userId = currentUserId ?: throw Exception("Not authenticated")
        firestore.updateUser(userId, mapOf(
            "mfaEnabled" to true,
            "mfaEmail" to email,
            "updatedAt" to firestore.serverTimestamp()
        ))
    }

    override suspend fun disableMfa(): Result<Unit> = runCatching {
        val userId = currentUserId ?: throw Exception("Not authenticated")
        firestore.updateUser(userId, mapOf(
            "mfaEnabled" to false,
            "mfaEmail" to null,
            "updatedAt" to firestore.serverTimestamp()
        ))
    }

    // ==================== Mapping Functions ====================

    private fun DocumentSnapshot.toUser(): User? {
        return try {
            val now = Clock.System.now()
            User(
                id = id,
                email = get("email") ?: return null,
                firstName = get("firstName") ?: "",
                lastName = get("lastName") ?: "",
                role = try {
                    Role.valueOf(get("role") ?: "PENDING_VISITOR")
                } catch (e: Exception) {
                    Role.PENDING_VISITOR
                },
                phoneNumber = get("phoneNumber"),
                profileImageUrl = get("profileImageUrl"),
                isActive = get("isActive") ?: true,
                isEmailVerified = get("isEmailVerified") ?: false,
                createdAt = get<Long?>("createdAt")?.let { Instant.fromEpochMilliseconds(it) } ?: now,
                updatedAt = get<Long?>("updatedAt")?.let { Instant.fromEpochMilliseconds(it) } ?: now,
                lastLoginAt = get<Long?>("lastLoginAt")?.let { Instant.fromEpochMilliseconds(it) },
                associatedBeneficiaryIds = get("associatedBeneficiaryIds") ?: emptyList(),
                notificationPreferences = parseNotificationPreferences(this),
                mfaEnabled = get("mfaEnabled") ?: false,
                mfaEmail = get("mfaEmail")
            )
        } catch (e: Exception) {
            null
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseNotificationPreferences(doc: DocumentSnapshot): NotificationPreferences {
        return try {
            val prefsMap = doc.get<Map<String, Any?>?>("notificationPreferences")
            if (prefsMap != null) {
                NotificationPreferences(
                    emailNotifications = prefsMap["emailNotifications"] as? Boolean ?: true,
                    pushNotifications = prefsMap["pushNotifications"] as? Boolean ?: true,
                    smsNotifications = prefsMap["smsNotifications"] as? Boolean ?: false,
                    visitReminders = prefsMap["visitReminders"] as? Boolean ?: true,
                    approvalNotifications = prefsMap["approvalNotifications"] as? Boolean ?: true,
                    scheduleChanges = prefsMap["scheduleChanges"] as? Boolean ?: true
                )
            } else {
                NotificationPreferences()
            }
        } catch (e: Exception) {
            NotificationPreferences()
        }
    }
}
