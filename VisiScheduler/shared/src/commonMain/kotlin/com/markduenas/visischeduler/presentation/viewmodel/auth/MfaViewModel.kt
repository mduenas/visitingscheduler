package com.markduenas.visischeduler.presentation.viewmodel.auth

import com.markduenas.visischeduler.domain.entities.User
import com.markduenas.visischeduler.domain.repository.AuthRepository
import com.markduenas.visischeduler.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * State for the MFA verification screen.
 */
data class MfaUiState(
    val code: String = "",
    val isLoading: Boolean = false,
    val codeError: String? = null,
    val generalError: String? = null,
    val verificationSuccess: Boolean = false,
    val user: User? = null,
    val mfaMethod: MfaMethod = MfaMethod.SMS,
    val maskedDestination: String? = null,
    val challengeId: String = "",
    val resendCooldownSeconds: Int = 0,
    val attemptsRemaining: Int = 3
)

/**
 * ViewModel for handling MFA verification.
 */
class MfaViewModel(
    private val authRepository: AuthRepository
) : BaseViewModel<MfaUiState>(MfaUiState()) {

    private val _mfaState = MutableStateFlow(MfaUiState())
    val mfaState: StateFlow<MfaUiState> = _mfaState.asStateFlow()

    private var cooldownJob: Job? = null

    /**
     * Initialize MFA screen with challenge data.
     */
    fun initializeWithChallenge(
        challengeId: String,
        method: MfaMethod,
        maskedDestination: String?
    ) {
        _mfaState.value = MfaUiState(
            challengeId = challengeId,
            mfaMethod = method,
            maskedDestination = maskedDestination
        )
    }

    /**
     * Update code field.
     * Only accepts digits and limits to 6 characters.
     */
    fun onCodeChange(code: String) {
        // Filter to only digits and limit to 6 characters
        val filteredCode = code.filter { it.isDigit() }.take(6)

        _mfaState.value = _mfaState.value.copy(
            code = filteredCode,
            codeError = null,
            generalError = null
        )

        // Auto-verify when 6 digits are entered
        if (filteredCode.length == 6) {
            verify()
        }
    }

    /**
     * Verify the MFA code.
     */
    fun verify() {
        val currentState = _mfaState.value

        // Validate code
        val codeError = validateCode(currentState.code)
        if (codeError != null) {
            _mfaState.value = currentState.copy(codeError = codeError)
            return
        }

        _mfaState.value = currentState.copy(
            isLoading = true,
            codeError = null,
            generalError = null
        )

        viewModelScope.launch {
            val result = authRepository.verifyMfa(
                challengeId = currentState.challengeId,
                code = currentState.code
            )

            result.fold(
                onSuccess = { user ->
                    _mfaState.value = _mfaState.value.copy(
                        isLoading = false,
                        verificationSuccess = true,
                        user = user
                    )
                    showSnackbar("Verification successful!")
                },
                onFailure = { exception ->
                    handleVerificationFailure()
                }
            )
        }
    }

    /**
     * Resend the MFA code.
     */
    fun resendCode() {
        val currentState = _mfaState.value

        if (currentState.resendCooldownSeconds > 0) {
            showSnackbar("Please wait ${currentState.resendCooldownSeconds} seconds before requesting a new code.")
            return
        }

        _mfaState.value = currentState.copy(isLoading = true)

        viewModelScope.launch {
            // In a real implementation, this would call the auth repository
            // val result = authRepository.resendMfaCode(currentState.challengeId)

            // Simulate resend
            kotlinx.coroutines.delay(500)

            _mfaState.value = _mfaState.value.copy(
                isLoading = false,
                code = "",
                attemptsRemaining = 3 // Reset attempts on new code
            )

            startResendCooldown()
            showSnackbar("A new verification code has been sent.")
        }
    }

    /**
     * Clear error state.
     */
    fun clearError() {
        _mfaState.value = _mfaState.value.copy(
            codeError = null,
            generalError = null
        )
    }

    /**
     * Reset state.
     */
    fun resetState() {
        cooldownJob?.cancel()
        _mfaState.value = MfaUiState()
    }

    override fun onCleared() {
        cooldownJob?.cancel()
        super.onCleared()
    }

    private fun validateCode(code: String): String? {
        return when {
            code.isBlank() -> "Please enter the verification code"
            code.length < 6 -> "Code must be 6 digits"
            !code.all { it.isDigit() } -> "Code must contain only numbers"
            else -> null
        }
    }

    private fun handleVerificationFailure() {
        val currentState = _mfaState.value
        val newAttemptsRemaining = currentState.attemptsRemaining - 1

        if (newAttemptsRemaining <= 0) {
            _mfaState.value = currentState.copy(
                isLoading = false,
                code = "",
                attemptsRemaining = 0,
                generalError = "Too many failed attempts. Please request a new code."
            )
        } else {
            _mfaState.value = currentState.copy(
                isLoading = false,
                code = "",
                attemptsRemaining = newAttemptsRemaining,
                codeError = "Invalid code. $newAttemptsRemaining attempt${if (newAttemptsRemaining > 1) "s" else ""} remaining."
            )
        }
    }

    private fun startResendCooldown() {
        cooldownJob?.cancel()

        val cooldownDuration = 30 // 30 seconds cooldown

        cooldownJob = viewModelScope.launch {
            _mfaState.value = _mfaState.value.copy(resendCooldownSeconds = cooldownDuration)

            for (i in cooldownDuration downTo 1) {
                _mfaState.value = _mfaState.value.copy(resendCooldownSeconds = i)
                kotlinx.coroutines.delay(1000)
            }

            _mfaState.value = _mfaState.value.copy(resendCooldownSeconds = 0)
        }
    }
}
