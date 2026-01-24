package com.markduenas.visischeduler.domain.repository

import com.markduenas.visischeduler.domain.entities.NotificationPreferences
import com.markduenas.visischeduler.domain.entities.Role
import com.markduenas.visischeduler.domain.entities.User
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for user operations.
 */
interface UserRepository {
    /**
     * Get the current authenticated user.
     */
    val currentUser: Flow<User?>

    /**
     * Get a user by ID.
     */
    suspend fun getUserById(userId: String): Result<User>

    /**
     * Get a user by email.
     */
    suspend fun getUserByEmail(email: String): Result<User>

    /**
     * Get all users (admin only).
     */
    fun getAllUsers(): Flow<List<User>>

    /**
     * Get users by role.
     */
    fun getUsersByRole(role: Role): Flow<List<User>>

    /**
     * Get pending visitor requests (for approval).
     */
    fun getPendingVisitors(): Flow<List<User>>

    /**
     * Search users by name or email.
     */
    suspend fun searchUsers(query: String): Result<List<User>>

    /**
     * Update user profile.
     */
    suspend fun updateProfile(
        firstName: String? = null,
        lastName: String? = null,
        phoneNumber: String? = null,
        profileImageUrl: String? = null
    ): Result<User>

    /**
     * Update notification preferences.
     */
    suspend fun updateNotificationPreferences(
        preferences: NotificationPreferences
    ): Result<User>

    /**
     * Change user password.
     */
    suspend fun changePassword(
        currentPassword: String,
        newPassword: String
    ): Result<Unit>

    /**
     * Update user role (admin only).
     */
    suspend fun updateUserRole(userId: String, newRole: Role): Result<User>

    /**
     * Approve a pending visitor (coordinator action).
     */
    suspend fun approveVisitor(userId: String): Result<User>

    /**
     * Deny a pending visitor (coordinator action).
     */
    suspend fun denyVisitor(userId: String, reason: String): Result<Unit>

    /**
     * Deactivate a user account (admin only).
     */
    suspend fun deactivateUser(userId: String): Result<User>

    /**
     * Reactivate a user account (admin only).
     */
    suspend fun reactivateUser(userId: String): Result<User>

    /**
     * Associate a beneficiary with the current user.
     */
    suspend fun associateBeneficiary(beneficiaryId: String): Result<User>

    /**
     * Remove beneficiary association.
     */
    suspend fun removeBeneficiaryAssociation(beneficiaryId: String): Result<User>

    /**
     * Upload profile image.
     */
    suspend fun uploadProfileImage(imageData: ByteArray): Result<String>

    /**
     * Delete user account (self or admin).
     */
    suspend fun deleteAccount(): Result<Unit>

    /**
     * Sync user data from remote server.
     */
    suspend fun syncUser(): Result<User>
}
