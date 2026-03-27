package com.markduenas.visischeduler.presentation.viewmodel.settings

import com.markduenas.visischeduler.common.error.AppException
import com.markduenas.visischeduler.domain.repository.AuthRepository
import com.markduenas.visischeduler.domain.repository.UserRepository
import com.markduenas.visischeduler.platform.SecureStorage
import com.markduenas.visischeduler.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.launch

/**
 * Session timeout options.
 */
enum class SessionTimeout(val displayName: String, val minutes: Int) {
    MINUTES_5("5 minutes", 5),
    MINUTES_15("15 minutes", 15),
    MINUTES_30("30 minutes", 30),
    HOURS_1("1 hour", 60),
    HOURS_4("4 hours", 240),
    NEVER("Never", -1)
}

/**
 * Active session information.
 */
data class ActiveSession(
    val id: String,
    val deviceName: String,
    val deviceType: String,
    val location: String?,
    val lastActive: String,
    val isCurrent: Boolean
)

/**
 * UI state for security settings.
 */
data class SecuritySettingsUiState(
    val biometricEnabled: Boolean = false,
    val biometricAvailable: Boolean = false,
    val sessionTimeout: SessionTimeout = SessionTimeout.MINUTES_30,
    val mfaEnabled: Boolean = false,
    val mfaSetupInProgress: Boolean = false,
    val activeSessions: List<ActiveSession> = emptyList(),
    val isLoading: Boolean = false,
    val isChangingPassword: Boolean = false,
    val passwordChangeSuccess: Boolean = false,
    val error: AppException? = null,
    // Change password form state
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val passwordErrors: PasswordValidationState = PasswordValidationState()
)

/**
 * Password validation state.
 */
data class PasswordValidationState(
    val hasMinLength: Boolean = false,
    val hasUppercase: Boolean = false,
    val hasLowercase: Boolean = false,
    val hasNumber: Boolean = false,
    val hasSpecialChar: Boolean = false,
    val passwordsMatch: Boolean = false
) {
    val isValid: Boolean
        get() = hasMinLength && hasUppercase && hasLowercase &&
                hasNumber && hasSpecialChar && passwordsMatch
}

/**
 * ViewModel for managing security settings.
 */
class SecuritySettingsViewModel(
    private val secureStorage: SecureStorage,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : BaseViewModel<SecuritySettingsUiState>(SecuritySettingsUiState()) {

    companion object {
        private const val KEY_BIOMETRIC_ENABLED = "security_biometric_enabled"
        private const val KEY_SESSION_TIMEOUT = "security_session_timeout"
        private const val KEY_MFA_ENABLED = "security_mfa_enabled"
        private const val MIN_PASSWORD_LENGTH = 8
    }

    init {
        loadSettings()
        checkBiometricAvailability()
        loadActiveSessions()
    }

    /**
     * Load security settings from storage.
     */
    private fun loadSettings() {
        updateState { copy(isLoading = true) }

        val biometricEnabled = secureStorage.getBoolean(KEY_BIOMETRIC_ENABLED) ?: false
        val sessionTimeoutValue = secureStorage.getString(KEY_SESSION_TIMEOUT)
            ?: SessionTimeout.MINUTES_30.name
        val mfaEnabled = secureStorage.getBoolean(KEY_MFA_ENABLED) ?: false

        val sessionTimeout = try {
            SessionTimeout.valueOf(sessionTimeoutValue)
        } catch (e: Exception) {
            SessionTimeout.MINUTES_30
        }

        updateState {
            copy(
                biometricEnabled = biometricEnabled,
                sessionTimeout = sessionTimeout,
                mfaEnabled = mfaEnabled,
                isLoading = false
            )
        }
    }

    /**
     * Check if biometric authentication is available on this device.
     */
    private fun checkBiometricAvailability() {
        launchSafe {
            val available = authRepository.isBiometricAvailable()
            updateState { copy(biometricAvailable = available) }
        }
    }

    /**
     * Load active sessions for current user.
     */
    private fun loadActiveSessions() {
        // TODO: Implement when session management API is available
        // For now, show a mock current session
        updateState {
            copy(
                activeSessions = listOf(
                    ActiveSession(
                        id = "current",
                        deviceName = "Current Device",
                        deviceType = "Mobile",
                        location = null,
                        lastActive = "Now",
                        isCurrent = true
                    )
                )
            )
        }
    }

    /**
     * Toggle biometric authentication.
     */
    fun toggleBiometric(enabled: Boolean) {
        launchSafe {
            if (enabled) {
                authRepository.enableBiometric().fold(
                    onSuccess = {
                        secureStorage.putBoolean(KEY_BIOMETRIC_ENABLED, true)
                        updateState { copy(biometricEnabled = true) }
                        showSnackbar("Biometric login enabled")
                    },
                    onFailure = {
                        showSnackbar("Failed to enable biometric login")
                    }
                )
            } else {
                authRepository.disableBiometric().fold(
                    onSuccess = {
                        secureStorage.putBoolean(KEY_BIOMETRIC_ENABLED, false)
                        updateState { copy(biometricEnabled = false) }
                        showSnackbar("Biometric login disabled")
                    },
                    onFailure = {
                        showSnackbar("Failed to disable biometric login")
                    }
                )
            }
        }
    }

    /**
     * Set session timeout.
     */
    fun setSessionTimeout(timeout: SessionTimeout) {
        secureStorage.putString(KEY_SESSION_TIMEOUT, timeout.name)
        updateState { copy(sessionTimeout = timeout) }
    }

    /**
     * Toggle MFA.
     */
    fun toggleMfa(enabled: Boolean) {
        if (enabled) {
            // Start MFA setup flow
            updateState { copy(mfaSetupInProgress = true) }
            navigate("mfa_setup")
        } else {
            // Disable MFA
            secureStorage.putBoolean(KEY_MFA_ENABLED, false)
            updateState { copy(mfaEnabled = false) }
            showSnackbar("Two-factor authentication disabled")
        }
    }

    /**
     * Complete MFA setup.
     */
    fun completeMfaSetup(success: Boolean) {
        if (success) {
            secureStorage.putBoolean(KEY_MFA_ENABLED, true)
            updateState { copy(mfaEnabled = true, mfaSetupInProgress = false) }
            showSnackbar("Two-factor authentication enabled")
        } else {
            updateState { copy(mfaSetupInProgress = false) }
        }
    }

    /**
     * Update current password field.
     */
    fun updateCurrentPassword(password: String) {
        updateState { copy(currentPassword = password) }
    }

    /**
     * Update new password field.
     */
    fun updateNewPassword(password: String) {
        val validation = validatePassword(password, currentState.confirmPassword)
        updateState {
            copy(
                newPassword = password,
                passwordErrors = validation
            )
        }
    }

    /**
     * Update confirm password field.
     */
    fun updateConfirmPassword(password: String) {
        val validation = validatePassword(currentState.newPassword, password)
        updateState {
            copy(
                confirmPassword = password,
                passwordErrors = validation
            )
        }
    }

    /**
     * Validate password against requirements.
     */
    private fun validatePassword(password: String, confirmPassword: String): PasswordValidationState {
        return PasswordValidationState(
            hasMinLength = password.length >= MIN_PASSWORD_LENGTH,
            hasUppercase = password.any { it.isUpperCase() },
            hasLowercase = password.any { it.isLowerCase() },
            hasNumber = password.any { it.isDigit() },
            hasSpecialChar = password.any { !it.isLetterOrDigit() },
            passwordsMatch = password.isNotEmpty() && password == confirmPassword
        )
    }

    /**
     * Change password.
     */
    fun changePassword() {
        if (!currentState.passwordErrors.isValid) {
            showSnackbar("Please fix password requirements")
            return
        }

        launchSafe {
            updateState { copy(isChangingPassword = true, error = null) }

            userRepository.changePassword(
                currentPassword = currentState.currentPassword,
                newPassword = currentState.newPassword
            ).fold(
                onSuccess = {
                    updateState {
                        copy(
                            isChangingPassword = false,
                            passwordChangeSuccess = true,
                            currentPassword = "",
                            newPassword = "",
                            confirmPassword = "",
                            passwordErrors = PasswordValidationState()
                        )
                    }
                    showSnackbar("Password changed successfully")
                    navigateBack()
                },
                onFailure = { error ->
                    val exception = AppException.UnknownException(
                        error.message ?: "Failed to change password"
                    )
                    updateState {
                        copy(
                            isChangingPassword = false,
                            error = exception
                        )
                    }
                    showSnackbar(exception.message)
                }
            )
        }
    }

    /**
     * Revoke a session.
     * For the current session this performs a full logout. Other sessions are removed
     * from local state (server-side revocation requires Firebase Admin SDK).
     */
    fun revokeSession(sessionId: String) {
        val session = currentState.activeSessions.find { it.id == sessionId } ?: return
        if (session.isCurrent) {
            launchSafe {
                authRepository.logout()
                navigate("login")
            }
        } else {
            updateState { copy(activeSessions = activeSessions.filter { it.id != sessionId }) }
            showSnackbar("Session revoked")
        }
    }

    /**
     * Logout from all devices by signing out of Firebase Auth.
     * This invalidates the current device token; other device sessions will
     * expire on their next token refresh.
     */
    fun logoutAllDevices() {
        launchSafe {
            updateState { copy(isLoading = true) }
            authRepository.logout().fold(
                onSuccess = { navigate("login") },
                onFailure = {
                    updateState { copy(isLoading = false) }
                    showSnackbar("Failed to sign out — please try again")
                }
            )
        }
    }

    /**
     * Navigate to change password screen.
     */
    fun navigateToChangePassword() {
        navigate("settings/security/change-password")
    }

    /**
     * Clear error state.
     */
    fun clearError() {
        updateState { copy(error = null) }
    }

    /**
     * Reset password change form.
     */
    fun resetPasswordForm() {
        updateState {
            copy(
                currentPassword = "",
                newPassword = "",
                confirmPassword = "",
                passwordErrors = PasswordValidationState(),
                passwordChangeSuccess = false
            )
        }
    }
}
