package com.markduenas.visischeduler.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.markduenas.visischeduler.domain.entities.Role
import com.markduenas.visischeduler.domain.entities.User
import com.markduenas.visischeduler.domain.repository.UserRepository
import com.markduenas.visischeduler.firebase.FirestoreDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant

/**
 * Firestore implementation of UserRepository.
 * Uses Firebase Firestore as the backend database.
 */
class FirestoreUserRepository(
    private val firestore: FirestoreDatabase
) : UserRepository {

    override suspend fun createUser(user: User): Result<User> = runCatching {
        val userData = user.toFirestoreMap()
        firestore.createUser(user.id, userData)
        user
    }

    override suspend fun getUser(userId: String): Result<User?> = runCatching {
        firestore.getUser(userId)?.toUser()
    }

    override suspend fun getUserByEmail(email: String): Result<User?> = runCatching {
        val docs = firestore.query(FirestoreDatabase.COLLECTION_USERS, "email", email)
        docs.firstOrNull()?.toUser()
    }

    override suspend fun updateUser(user: User): Result<User> = runCatching {
        firestore.updateUser(user.id, user.toFirestoreMap())
        user
    }

    override suspend fun deleteUser(userId: String): Result<Unit> = runCatching {
        firestore.delete(FirestoreDatabase.COLLECTION_USERS, userId)
    }

    override suspend fun getApprovedVisitors(beneficiaryId: String): Result<List<User>> = runCatching {
        val docs = firestore.query(
            FirestoreDatabase.COLLECTION_USERS,
            "approvedBeneficiaries",
            beneficiaryId,
            com.markduenas.visischeduler.firebase.QueryOperator.ARRAY_CONTAINS
        )
        docs.mapNotNull { it.toUser() }
            .filter { it.role == Role.APPROVED_VISITOR }
    }

    override suspend fun getPendingVisitors(beneficiaryId: String): Result<List<User>> = runCatching {
        val docs = firestore.query(
            FirestoreDatabase.COLLECTION_USERS,
            "pendingBeneficiaries",
            beneficiaryId,
            com.markduenas.visischeduler.firebase.QueryOperator.ARRAY_CONTAINS
        )
        docs.mapNotNull { it.toUser() }
            .filter { it.role == Role.PENDING_VISITOR }
    }

    override suspend fun getBlockedVisitors(beneficiaryId: String): Result<List<User>> = runCatching {
        val docs = firestore.query(
            FirestoreDatabase.COLLECTION_USERS,
            "blockedBeneficiaries",
            beneficiaryId,
            com.markduenas.visischeduler.firebase.QueryOperator.ARRAY_CONTAINS
        )
        docs.mapNotNull { it.toUser() }
    }

    override suspend fun getCoordinators(beneficiaryId: String): Result<List<User>> = runCatching {
        val docs = firestore.query(
            FirestoreDatabase.COLLECTION_USERS,
            "coordinatingBeneficiaries",
            beneficiaryId,
            com.markduenas.visischeduler.firebase.QueryOperator.ARRAY_CONTAINS
        )
        docs.mapNotNull { it.toUser() }
            .filter { it.role == Role.PRIMARY_COORDINATOR || it.role == Role.SECONDARY_COORDINATOR }
    }

    override suspend fun approveVisitor(
        visitorId: String,
        beneficiaryId: String
    ): Result<User> = runCatching {
        val user = firestore.getUser(visitorId)?.toUser()
            ?: throw Exception("User not found")

        val updates = mapOf(
            "role" to Role.APPROVED_VISITOR.name,
            "approvedBeneficiaries" to (user.approvedBeneficiaries + beneficiaryId),
            "pendingBeneficiaries" to (user.pendingBeneficiaries - beneficiaryId),
            "updatedAt" to Timestamp.now()
        )
        firestore.updateUser(visitorId, updates)

        firestore.getUser(visitorId)?.toUser()
            ?: throw Exception("User not found after approval")
    }

    override suspend fun blockVisitor(
        visitorId: String,
        beneficiaryId: String
    ): Result<User> = runCatching {
        val user = firestore.getUser(visitorId)?.toUser()
            ?: throw Exception("User not found")

        val updates = mapOf(
            "approvedBeneficiaries" to (user.approvedBeneficiaries - beneficiaryId),
            "blockedBeneficiaries" to (user.blockedBeneficiaries + beneficiaryId),
            "updatedAt" to Timestamp.now()
        )
        firestore.updateUser(visitorId, updates)

        firestore.getUser(visitorId)?.toUser()
            ?: throw Exception("User not found after blocking")
    }

    override suspend fun unblockVisitor(
        visitorId: String,
        beneficiaryId: String
    ): Result<User> = runCatching {
        val user = firestore.getUser(visitorId)?.toUser()
            ?: throw Exception("User not found")

        val updates = mapOf(
            "blockedBeneficiaries" to (user.blockedBeneficiaries - beneficiaryId),
            "updatedAt" to Timestamp.now()
        )
        firestore.updateUser(visitorId, updates)

        firestore.getUser(visitorId)?.toUser()
            ?: throw Exception("User not found after unblocking")
    }

    override fun observeUser(userId: String): Flow<User?> {
        return firestore.listenToUser(userId).map { it?.toUser() }
    }

    // ==================== Mapping Functions ====================

    private fun User.toFirestoreMap(): Map<String, Any?> = mapOf(
        "email" to email,
        "firstName" to firstName,
        "lastName" to lastName,
        "phoneNumber" to phoneNumber,
        "role" to role.name,
        "avatarUrl" to avatarUrl,
        "isActive" to isActive,
        "isEmailVerified" to isEmailVerified,
        "mfaEnabled" to mfaEnabled,
        "approvedBeneficiaries" to approvedBeneficiaries,
        "pendingBeneficiaries" to pendingBeneficiaries,
        "blockedBeneficiaries" to blockedBeneficiaries,
        "coordinatingBeneficiaries" to coordinatingBeneficiaries,
        "createdAt" to Timestamp.now(),
        "updatedAt" to Timestamp.now()
    )

    private fun DocumentSnapshot.toUser(): User? {
        return try {
            User(
                id = id,
                email = getString("email") ?: return null,
                firstName = getString("firstName") ?: "",
                lastName = getString("lastName") ?: "",
                phoneNumber = getString("phoneNumber"),
                role = Role.valueOf(getString("role") ?: "PENDING_VISITOR"),
                avatarUrl = getString("avatarUrl"),
                isActive = getBoolean("isActive") ?: true,
                isEmailVerified = getBoolean("isEmailVerified") ?: false,
                mfaEnabled = getBoolean("mfaEnabled") ?: false,
                approvedBeneficiaries = (get("approvedBeneficiaries") as? List<*>)
                    ?.filterIsInstance<String>() ?: emptyList(),
                pendingBeneficiaries = (get("pendingBeneficiaries") as? List<*>)
                    ?.filterIsInstance<String>() ?: emptyList(),
                blockedBeneficiaries = (get("blockedBeneficiaries") as? List<*>)
                    ?.filterIsInstance<String>() ?: emptyList(),
                coordinatingBeneficiaries = (get("coordinatingBeneficiaries") as? List<*>)
                    ?.filterIsInstance<String>() ?: emptyList(),
                createdAt = getTimestamp("createdAt")?.let {
                    Instant.fromEpochMilliseconds(it.toDate().time)
                } ?: Instant.fromEpochMilliseconds(0),
                updatedAt = getTimestamp("updatedAt")?.let {
                    Instant.fromEpochMilliseconds(it.toDate().time)
                } ?: Instant.fromEpochMilliseconds(0),
                lastLoginAt = getTimestamp("lastLoginAt")?.let {
                    Instant.fromEpochMilliseconds(it.toDate().time)
                }
            )
        } catch (e: Exception) {
            null
        }
    }

    // Extension properties for User (would be in actual User entity)
    private val User.approvedBeneficiaries: List<String>
        get() = emptyList() // Would be actual field

    private val User.pendingBeneficiaries: List<String>
        get() = emptyList()

    private val User.blockedBeneficiaries: List<String>
        get() = emptyList()

    private val User.coordinatingBeneficiaries: List<String>
        get() = emptyList()
}
