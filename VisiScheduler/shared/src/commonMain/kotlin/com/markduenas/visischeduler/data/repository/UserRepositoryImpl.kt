package com.markduenas.visischeduler.data.repository

import com.markduenas.visischeduler.data.local.VisiSchedulerDatabase
import com.markduenas.visischeduler.data.remote.api.VisiSchedulerApi
import com.markduenas.visischeduler.domain.entities.NotificationPreferences
import com.markduenas.visischeduler.domain.entities.Role
import com.markduenas.visischeduler.domain.entities.User
import com.markduenas.visischeduler.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Implementation of UserRepository.
 */
class UserRepositoryImpl(
    private val api: VisiSchedulerApi,
    private val database: VisiSchedulerDatabase,
    private val json: Json
) : UserRepository {

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: Flow<User?> = _currentUser.asStateFlow()

    override suspend fun getUserById(userId: String): Result<User> {
        return try {
            // Simplified for now
            val cached = database.visiSchedulerQueries.selectUserById(userId).executeAsOneOrNull()
            if (cached != null) {
                Result.success(mapEntityToUser(cached))
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserByEmail(email: String): Result<User> {
        return try {
            val cached = database.visiSchedulerQueries.selectUserByEmail(email).executeAsOneOrNull()
            if (cached != null) {
                Result.success(mapEntityToUser(cached))
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getAllUsers(): Flow<List<User>> = flow {
        val cached = database.visiSchedulerQueries.selectAllUsers().executeAsList().map { mapEntityToUser(it) }
        emit(cached)
    }

    override fun getUsersByRole(role: Role): Flow<List<User>> = flow {
        val cached = database.visiSchedulerQueries.selectUsersByRole(role.name).executeAsList().map { mapEntityToUser(it) }
        emit(cached)
    }

    override fun getPendingVisitors(): Flow<List<User>> = flow {
        val cached = database.visiSchedulerQueries.selectPendingVisitors().executeAsList().map { mapEntityToUser(it) }
        emit(cached)
    }

    override suspend fun searchUsers(query: String): Result<List<User>> {
        return Result.success(emptyList())
    }

    override suspend fun updateProfile(
        firstName: String?,
        lastName: String?,
        phoneNumber: String?,
        profileImageUrl: String?
    ): Result<User> {
        return try {
            val current = api.getCurrentUser()
            val updated = current.copy(
                firstName = firstName ?: current.firstName,
                lastName = lastName ?: current.lastName,
                phoneNumber = phoneNumber ?: current.phoneNumber,
                profileImageUrl = profileImageUrl ?: current.profileImageUrl
            )
            val result = api.updateProfile(updated).toDomain()
            cacheUser(result)
            _currentUser.value = result
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateNotificationPreferences(preferences: NotificationPreferences): Result<User> {
        return try {
            val user = api.getCurrentUser().toDomain()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun updateUserRole(userId: String, newRole: Role): Result<User> {
        return try {
            val user = api.getCurrentUser().toDomain()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun approveVisitor(userId: String): Result<User> {
        return try {
            // Simplified
            val user = api.getCurrentUser().toDomain()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun denyVisitor(userId: String, reason: String): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun deactivateUser(userId: String): Result<User> {
        return try {
            val user = api.getCurrentUser().toDomain()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun reactivateUser(userId: String): Result<User> {
        return try {
            val user = api.getCurrentUser().toDomain()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun associateBeneficiary(beneficiaryId: String): Result<User> {
        return try {
            val user = api.getCurrentUser().toDomain()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeBeneficiaryAssociation(beneficiaryId: String): Result<User> {
        return try {
            val user = api.getCurrentUser().toDomain()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadProfileImage(imageData: ByteArray): Result<String> {
        return Result.success("")
    }

    override suspend fun deleteAccount(): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun syncUser(): Result<User> {
        return try {
            val user = api.getCurrentUser().toDomain()
            _currentUser.value = user
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun cacheUser(user: User) {
        database.visiSchedulerQueries.insertUser(
            id = user.id,
            email = user.email,
            firstName = user.firstName,
            lastName = user.lastName,
            role = user.role.name,
            phoneNumber = user.phoneNumber,
            profileImageUrl = user.profileImageUrl,
            isActive = if (user.isActive) 1L else 0L,
            isEmailVerified = if (user.isEmailVerified) 1L else 0L,
            createdAt = user.createdAt.toString(),
            updatedAt = user.updatedAt.toString(),
            lastLoginAt = user.lastLoginAt?.toString(),
            associatedBeneficiaryIds = json.encodeToString(user.associatedBeneficiaryIds),
            notificationPreferences = json.encodeToString(user.notificationPreferences)
        )
    }

    private fun mapEntityToUser(entity: com.markduenas.visischeduler.data.local.UserEntity): User {
        return User(
            id = entity.id,
            email = entity.email,
            firstName = entity.firstName,
            lastName = entity.lastName,
            role = Role.valueOf(entity.role),
            phoneNumber = entity.phoneNumber,
            profileImageUrl = entity.profileImageUrl,
            isActive = entity.isActive == 1L,
            isEmailVerified = entity.isEmailVerified == 1L,
            createdAt = kotlin.time.Instant.parse(entity.createdAt),
            updatedAt = kotlin.time.Instant.parse(entity.updatedAt),
            lastLoginAt = entity.lastLoginAt?.let { kotlin.time.Instant.parse(it) },
            associatedBeneficiaryIds = json.decodeFromString(entity.associatedBeneficiaryIds),
            notificationPreferences = json.decodeFromString(entity.notificationPreferences)
        )
    }
}
