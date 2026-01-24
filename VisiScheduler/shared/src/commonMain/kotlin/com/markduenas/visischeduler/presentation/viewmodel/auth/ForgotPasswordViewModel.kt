package com.markduenas.visischeduler.presentation.viewmodel.auth

import com.markduenas.visischeduler.domain.repository.AuthRepository
import com.markduenas.visischeduler.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * State for the forgot password screen.
 */
data class ForgotPasswordUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    val emailError: String? = null,
    val generalError: String? = null,
    val resetLinkSent: Boolean = false,
    val cooldownSeconds: Int = 0
)

/**
 * ViewModel for handling forgot password functionality.
 */
class ForgotPasswordViewModel(
    private val authRepository: AuthRepository
) : BaseViewModel<ForgotPasswordUiState>(ForgotPasswordUiState()) {

    private val _forgotPasswordState = MutableStateFlow(ForgotPasswordUiState())
    val forgotPasswordState: StateFlow<ForgotPasswordUiState> = _forgotPasswordState.asStateFlow()

    /**
     * Update email field.
     */
    fun onEmailChange(email: String) {
        _forgotPasswordState.value = _forgotPasswordState.value.copy(
            email = email,
            emailError = null,
            generalError = null
        )
    }

    /**
     * Request password reset link.
     */
    fun sendResetLink() {
        val currentState = _forgotPasswordState.value

        // Check cooldown
        if (currentState.cooldownSeconds > 0) {
            showSnackbar("Please wait ${currentState.cooldownSeconds} seconds before requesting another reset link.")
            return
        }

        // Validate email
        val emailError = validateEmail(currentState.email)
        if (emailError != null) {
            _forgotPasswordState.value = currentState.copy(emailError = emailError)
            return
        }

        _forgotPasswordState.value = currentState.copy(
            isLoading = true,
            emailError = null,
            generalError = null
        )

        viewModelScope.launch {
            val result = authRepository.requestPasswordReset(currentState.email.trim().lowercase())

            result.fold(
                onSuccess = {
                    _forgotPasswordState.value = _forgotPasswordState.value.copy(
                        isLoading = false,
                        resetLinkSent = true
                    )
                    startCooldown()
                    showSnackbar("Password reset link sent to your email")
                },
                onFailure = { exception ->
                    handleResetError(exception)
                }
            )
        }
    }

    /**
     * Resend the password reset link.
     */
    fun resendResetLink() {
        // Reset the sent state and trigger a new request
        _forgotPasswordState.value = _forgotPasswordState.value.copy(resetLinkSent = false)
        sendResetLink()
    }

    /**
     * Clear error state.
     */
    fun clearError() {
        _forgotPasswordState.value = _forgotPasswordState.value.copy(
            emailError = null,
            generalError = null
        )
    }

    /**
     * Reset state.
     */
    fun resetState() {
        _forgotPasswordState.value = ForgotPasswordUiState()
    }

    private fun validateEmail(email: String): String? {
        val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        return when {
            email.isBlank() -> "Email is required"
            !emailRegex.matches(email) -> "Please enter a valid email address"
            else -> null
        }
    }

    private fun startCooldown() {
        val cooldownDuration = 60 // 60 seconds cooldown

        viewModelScope.launch {
            _forgotPasswordState.value = _forgotPasswordState.value.copy(cooldownSeconds = cooldownDuration)

            for (i in cooldownDuration downTo 1) {
                _forgotPasswordState.value = _forgotPasswordState.value.copy(cooldownSeconds = i)
                kotlinx.coroutines.delay(1000)
            }

            _forgotPasswordState.value = _forgotPasswordState.value.copy(cooldownSeconds = 0)
        }
    }

    private fun handleResetError(exception: Throwable) {
        val errorMessage = when {
            exception.message?.contains("not found", ignoreCase = true) == true -> {
                // Don't reveal whether email exists for security
                // Just show success message anyway
                _forgotPasswordState.value = _forgotPasswordState.value.copy(
                    isLoading = false,
                    resetLinkSent = true
                )
                startCooldown()
                showSnackbar("If an account exists with this email, a reset link will be sent.")
                return
            }
            exception.message?.contains("rate limit", ignoreCase = true) == true ->
                "Too many requests. Please try again later."
            exception.message?.contains("network", ignoreCase = true) == true ->
                "Network error. Please check your connection."
            else -> "Failed to send reset link. Please try again."
        }

        _forgotPasswordState.value = _forgotPasswordState.value.copy(
            isLoading = false,
            generalError = errorMessage
        )
    }
}
