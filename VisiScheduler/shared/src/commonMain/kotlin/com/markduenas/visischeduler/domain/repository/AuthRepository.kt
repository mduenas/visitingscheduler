package com.markduenas.visischeduler.domain.repository

import com.markduenas.visischeduler.domain.entities.User
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for authentication operations.
 */
interface AuthRepository {
    /**
     * Get the currently authenticated user as a Flow.
     */
    val currentUser: Flow<User?>

    /**
     * Check if user is currently authenticated.
     */
    suspend fun isAuthenticated(): Boolean

    /**
     * Login with email and password.
     * @param email User's email address
     * @param password User's password
     * @return Result containing the authenticated User or an error
     */
    suspend fun login(email: String, password: String): Result<User>

    /**
     * Login with biometric authentication.
     * @return Result containing the authenticated User or an error
     */
    suspend fun loginWithBiometric(): Result<User>

    /**
     * Register a new user account.
     * @param email User's email address
     * @param password User's password
     * @param firstName User's first name
     * @param lastName User's last name
     * @param phoneNumber Optional phone number
     * @return Result containing the newly created User or an error
     */
    suspend fun register(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        phoneNumber: String? = null
    ): Result<User>

    /**
     * Logout the current user.
     */
    suspend fun logout(): Result<Unit>

    /**
     * Request password reset email.
     * @param email User's email address
     * @return Result indicating success or failure
     */
    suspend fun requestPasswordReset(email: String): Result<Unit>

    /**
     * Reset password with token.
     * @param token Reset token from email
     * @param newPassword New password
     * @return Result indicating success or failure
     */
    suspend fun resetPassword(token: String, newPassword: String): Result<Unit>

    /**
     * Verify email with token.
     * @param token Verification token
     * @return Result indicating success or failure
     */
    suspend fun verifyEmail(token: String): Result<Unit>

    /**
     * Refresh the authentication token.
     * @return Result containing the refreshed token or an error
     */
    suspend fun refreshToken(): Result<String>

    /**
     * Update the user's FCM token for push notifications.
     * @param fcmToken The new FCM token
     */
    suspend fun updateFcmToken(fcmToken: String): Result<Unit>

    /**
     * Check if biometric authentication is available and enabled.
     */
    suspend fun isBiometricAvailable(): Boolean

    /**
     * Enable biometric authentication.
     */
    suspend fun enableBiometric(): Result<Unit>

    /**
     * Disable biometric authentication.
     */
    suspend fun disableBiometric(): Result<Unit>
}
