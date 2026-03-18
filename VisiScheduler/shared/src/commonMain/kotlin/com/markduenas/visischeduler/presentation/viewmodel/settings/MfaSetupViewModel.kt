package com.markduenas.visischeduler.presentation.viewmodel.settings

import com.markduenas.visischeduler.domain.repository.AuthRepository
import com.markduenas.visischeduler.presentation.viewmodel.BaseViewModel
import com.markduenas.visischeduler.presentation.viewmodel.auth.MfaMethod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI State for MFA setup screen.
 */
data class MfaSetupUiState(
    val isLoading: Boolean = false,
    val setupStep: MfaSetupStep = MfaSetupStep.SELECT_METHOD,
    val selectedMethod: MfaMethod = MfaMethod.SMS,
    val destination: String = "",
    val challengeId: String? = null,
    val verificationCode: String = "",
    val error: String? = null,
    val setupSuccess: Boolean = false
)

/**
 * Steps in the MFA setup process.
 */
enum class MfaSetupStep {
    SELECT_METHOD,
    ENTER_DESTINATION,
    VERIFY_CODE,
    SUCCESS
}

/**
 * ViewModel for managing MFA setup process.
 */
class MfaSetupViewModel(
    private val authRepository: AuthRepository
) : BaseViewModel<MfaSetupUiState>(MfaSetupUiState()) {

    /**
     * Set the MFA method (SMS, Email, etc).
     */
    fun setMethod(method: MfaMethod) {
        updateState { copy(selectedMethod = method, error = null) }
        nextStep()
    }

    /**
     * Update the destination (phone number or email).
     */
    fun onDestinationChange(destination: String) {
        updateState { copy(destination = destination, error = null) }
    }

    /**
     * Start the setup process.
     */
    fun startSetup() {
        if (currentState.destination.isBlank()) {
            updateState { copy(error = "Please enter a valid destination") }
            return
        }

        updateState { copy(isLoading = true, error = null) }

        viewModelScope.launch {
            authRepository.setupMfa(currentState.selectedMethod, currentState.destination)
                .onSuccess { challengeId ->
                    updateState { 
                        copy(
                            challengeId = challengeId, 
                            isLoading = false,
                            setupStep = MfaSetupStep.VERIFY_CODE
                        ) 
                    }
                }
                .onFailure { error ->
                    updateState { copy(isLoading = false, error = error.message) }
                }
        }
    }

    /**
     * Update verification code.
     */
    fun onCodeChange(code: String) {
        val filtered = code.filter { it.isDigit() }.take(6)
        updateState { copy(verificationCode = filtered, error = null) }
        
        if (filtered.length == 6) {
            confirmSetup()
        }
    }

    /**
     * Confirm setup with code.
     */
    fun confirmSetup() {
        val challengeId = currentState.challengeId ?: return
        
        updateState { copy(isLoading = true, error = null) }
        
        viewModelScope.launch {
            authRepository.confirmMfaSetup(challengeId, currentState.verificationCode)
                .onSuccess {
                    updateState { 
                        copy(
                            isLoading = false, 
                            setupStep = MfaSetupStep.SUCCESS,
                            setupSuccess = true
                        ) 
                    }
                }
                .onFailure { error ->
                    updateState { copy(isLoading = false, error = error.message) }
                }
        }
    }

    /**
     * Move to next step in flow.
     */
    private fun nextStep() {
        val next = when (currentState.setupStep) {
            MfaSetupStep.SELECT_METHOD -> MfaSetupStep.ENTER_DESTINATION
            MfaSetupStep.ENTER_DESTINATION -> MfaSetupStep.VERIFY_CODE
            MfaSetupStep.VERIFY_CODE -> MfaSetupStep.SUCCESS
            MfaSetupStep.SUCCESS -> MfaSetupStep.SUCCESS
        }
        updateState { copy(setupStep = next) }
    }

    /**
     * Go back to previous step.
     */
    fun goBack() {
        val prev = when (currentState.setupStep) {
            MfaSetupStep.SELECT_METHOD -> MfaSetupStep.SELECT_METHOD
            MfaSetupStep.ENTER_DESTINATION -> MfaSetupStep.SELECT_METHOD
            MfaSetupStep.VERIFY_CODE -> MfaSetupStep.ENTER_DESTINATION
            MfaSetupStep.SUCCESS -> MfaSetupStep.SUCCESS
        }
        updateState { copy(setupStep = prev, error = null) }
    }
}
