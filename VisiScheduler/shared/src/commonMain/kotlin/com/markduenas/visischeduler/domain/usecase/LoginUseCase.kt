package com.markduenas.visischeduler.domain.usecase

import com.markduenas.visischeduler.domain.entities.User
import com.markduenas.visischeduler.domain.repository.AuthRepository

/**
 * Represents the two possible outcomes of a successful login attempt.
 */
sealed class LoginResult {
    data class Success(val user: User) : LoginResult()
    data class MfaRequired(val challengeId: String, val maskedEmail: String) : LoginResult()
}

/**
 * Use case for user login operations.
 */
class LoginUseCase(
    private val authRepository: AuthRepository
) {
    /**
     * Login with email and password.
     * Returns [LoginResult.MfaRequired] when the user has MFA enabled and a challenge has been sent.
     * Returns [LoginResult.Success] when authentication is complete.
     */
    suspend operator fun invoke(email: String, password: String): Result<LoginResult> {
        val trimmedEmail = email.trim()

        // Validate input
        if (trimmedEmail.isBlank()) {
            return Result.failure(LoginException.InvalidEmail("Email cannot be empty"))
        }
        if (!isValidEmail(trimmedEmail)) {
            return Result.failure(LoginException.InvalidEmail("Invalid email format"))
        }
        if (password.isBlank()) {
            return Result.failure(LoginException.InvalidPassword("Password cannot be empty"))
        }
        if (password.length < 8) {
            return Result.failure(LoginException.InvalidPassword("Password must be at least 8 characters"))
        }

        val loginResult = authRepository.login(trimmedEmail.lowercase(), password)
        return loginResult.map { user ->
            if (user.mfaEnabled) {
                val challengeResult = authRepository.loginWithMfaChallenge(user.id, user.mfaEmail ?: user.email)
                challengeResult.fold(
                    onSuccess = { challengeId ->
                        LoginResult.MfaRequired(challengeId, user.mfaEmail ?: user.email)
                    },
                    onFailure = { throw it }
                )
            } else {
                LoginResult.Success(user)
            }
        }
    }

    /**
     * Login with biometric authentication.
     */
    suspend fun loginWithBiometric(): Result<User> {
        if (!authRepository.isBiometricAvailable()) {
            return Result.failure(LoginException.BiometricNotAvailable("Biometric authentication is not available"))
        }
        return authRepository.loginWithBiometric()
    }

    /**
     * Check if user is already authenticated.
     */
    suspend fun isAuthenticated(): Boolean {
        return authRepository.isAuthenticated()
    }

    private fun isValidEmail(email: String): Boolean {
        val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        return emailRegex.matches(email)
    }
}

/**
 * Exceptions that can occur during login.
 */
sealed class LoginException(message: String) : Exception(message) {
    class InvalidEmail(message: String) : LoginException(message)
    class InvalidPassword(message: String) : LoginException(message)
    class InvalidCredentials(message: String) : LoginException(message)
    class AccountLocked(message: String) : LoginException(message)
    class AccountNotVerified(message: String) : LoginException(message)
    class BiometricNotAvailable(message: String) : LoginException(message)
    class NetworkError(message: String) : LoginException(message)
}
