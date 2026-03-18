package com.markduenas.visischeduler.data.repository

import com.markduenas.visischeduler.data.local.VisiSchedulerDatabase
import com.markduenas.visischeduler.data.remote.api.VisiSchedulerApi
import com.markduenas.visischeduler.data.remote.dto.LoginRequestDto
import com.markduenas.visischeduler.data.remote.dto.RegisterRequestDto
import com.markduenas.visischeduler.domain.entities.User
import com.markduenas.visischeduler.domain.repository.AuthRepository
import com.markduenas.visischeduler.platform.SecureStorage
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Implementation of AuthRepository.
 */
class AuthRepositoryImpl(
    private val api: VisiSchedulerApi,
    private val database: VisiSchedulerDatabase,
    private val secureStorage: SecureStorage,
    private val json: Json
) : AuthRepository {

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: Flow<User?> = _currentUser.asStateFlow()

    override suspend fun isAuthenticated(): Boolean {
        val token = secureStorage.getString(KEY_ACCESS_TOKEN)
        return token != null && token.isNotEmpty()
    }

    override suspend fun login(email: String, password: String): Result<User> {
        return try {
            val response = api.login(LoginRequestDto(email, password))

            // Store tokens securely
            secureStorage.putString(KEY_ACCESS_TOKEN, response.accessToken)
            secureStorage.putString(KEY_REFRESH_TOKEN, response.refreshToken)
            secureStorage.putLong(KEY_TOKEN_EXPIRY, Clock.System.now().toEpochMilliseconds() + (response.expiresIn * 1000))

            // Set token for API
            api.setAuthToken(response.accessToken)

            // Cache user in database
            val user = response.user.toDomain()
            cacheUser(user)

            _currentUser.value = user
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun loginWithBiometric(): Result<User> {
        return try {
            // Get stored credentials
            val refreshToken = secureStorage.getString(KEY_REFRESH_TOKEN)
                ?: return Result.failure(Exception("No stored credentials"))

            // Refresh token
            val response = api.refreshToken(refreshToken)

            // Update tokens
            secureStorage.putString(KEY_ACCESS_TOKEN, response.accessToken)
            secureStorage.putString(KEY_REFRESH_TOKEN, response.refreshToken)
            secureStorage.putLong(KEY_TOKEN_EXPIRY, Clock.System.now().toEpochMilliseconds() + (response.expiresIn * 1000))

            api.setAuthToken(response.accessToken)

            val user = response.user.toDomain()
            cacheUser(user)

            _currentUser.value = user
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun register(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        phoneNumber: String?
    ): Result<User> {
        return try {
            val response = api.register(
                RegisterRequestDto(
                    email = email,
                    password = password,
                    firstName = firstName,
                    lastName = lastName,
                    phoneNumber = phoneNumber
                )
            )

            secureStorage.putString(KEY_ACCESS_TOKEN, response.accessToken)
            secureStorage.putString(KEY_REFRESH_TOKEN, response.refreshToken)
            secureStorage.putLong(KEY_TOKEN_EXPIRY, Clock.System.now().toEpochMilliseconds() + (response.expiresIn * 1000))

            api.setAuthToken(response.accessToken)

            val user = response.user.toDomain()
            cacheUser(user)

            _currentUser.value = user
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            api.logout()

            // Clear stored data
            secureStorage.remove(KEY_ACCESS_TOKEN)
            secureStorage.remove(KEY_REFRESH_TOKEN)
            secureStorage.remove(KEY_TOKEN_EXPIRY)
            secureStorage.remove(KEY_BIOMETRIC_ENABLED)

            api.setAuthToken(null)

            // Clear database
            database.visiSchedulerQueries.deleteAuthToken()

            _currentUser.value = null
            Result.success(Unit)
        } catch (e: Exception) {
            // Still clear local data even if API call fails
            secureStorage.remove(KEY_ACCESS_TOKEN)
            secureStorage.remove(KEY_REFRESH_TOKEN)
            secureStorage.remove(KEY_TOKEN_EXPIRY)
            api.setAuthToken(null)
            _currentUser.value = null
            Result.success(Unit)
        }
    }

    override suspend fun requestPasswordReset(email: String): Result<Unit> {
        return try {
            api.requestPasswordReset(email)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun resetPassword(token: String, newPassword: String): Result<Unit> {
        return try {
            api.resetPassword(token, newPassword)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun verifyEmail(token: String): Result<Unit> {
        return try {
            api.verifyEmail(token)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refreshToken(): Result<String> {
        return try {
            val refreshToken = secureStorage.getString(KEY_REFRESH_TOKEN)
                ?: return Result.failure(Exception("No refresh token available"))

            val response = api.refreshToken(refreshToken)

            secureStorage.putString(KEY_ACCESS_TOKEN, response.accessToken)
            secureStorage.putString(KEY_REFRESH_TOKEN, response.refreshToken)
            secureStorage.putLong(KEY_TOKEN_EXPIRY, Clock.System.now().toEpochMilliseconds() + (response.expiresIn * 1000))

            api.setAuthToken(response.accessToken)

            Result.success(response.accessToken)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateFcmToken(fcmToken: String): Result<Unit> {
        return try {
            // Implementation would call API to update FCM token
            secureStorage.putString(KEY_FCM_TOKEN, fcmToken)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isBiometricAvailable(): Boolean {
        return secureStorage.getBoolean(KEY_BIOMETRIC_ENABLED) ?: false
    }

    override suspend fun enableBiometric(): Result<Unit> {
        return try {
            secureStorage.putBoolean(KEY_BIOMETRIC_ENABLED, true)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun disableBiometric(): Result<Unit> {
        return try {
            secureStorage.putBoolean(KEY_BIOMETRIC_ENABLED, false)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun verifyMfa(challengeId: String, code: String): Result<User> {
        return try {
            val response = api.verifyMfa(challengeId, code)

            // Store tokens securely
            secureStorage.putString(KEY_ACCESS_TOKEN, response.accessToken)
            secureStorage.putString(KEY_REFRESH_TOKEN, response.refreshToken)
            secureStorage.putLong(KEY_TOKEN_EXPIRY, Clock.System.now().toEpochMilliseconds() + (response.expiresIn * 1000))

            api.setAuthToken(response.accessToken)

            val user = response.user.toDomain()
            cacheUser(user)

            _currentUser.value = user
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun resendMfaCode(challengeId: String): Result<Unit> {
        return try {
            api.resendMfaCode(challengeId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setupMfa(
        method: com.markduenas.visischeduler.presentation.viewmodel.auth.MfaMethod,
        destination: String
    ): Result<String> {
        return try {
            // In a real app, this would call api.setupMfa(method, destination)
            // For now, simulate a successful setup initiation
            kotlinx.coroutines.delay(500)
            Result.success("setup_challenge_" + Clock.System.now().toEpochMilliseconds())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun confirmMfaSetup(challengeId: String, code: String): Result<Unit> {
        return try {
            // In a real app, this would call api.confirmMfaSetup(challengeId, code)
            // For now, simulate a successful verification
            kotlinx.coroutines.delay(500)
            if (code == "123456") {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Invalid verification code"))
            }
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

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_TOKEN_EXPIRY = "token_expiry"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_FCM_TOKEN = "fcm_token"
    }
}
