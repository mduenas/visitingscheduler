package com.markduenas.visischeduler.domain.usecase

import com.markduenas.visischeduler.domain.entities.User
import com.markduenas.visischeduler.domain.repository.AuthRepository

/**
 * Use case for user login operations.
 */
class LoginUseCase(
    private val authRepository: AuthRepository
) {
    /**
     * Login with email and password.
     * @param email User's email address
     * @param password User's password
     * @return Result containing the authenticated User or an error
     */
    suspend operator fun invoke(email: String, password: String): Result<User> {
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

        return authRepository.login(trimmedEmail.lowercase(), password)
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
