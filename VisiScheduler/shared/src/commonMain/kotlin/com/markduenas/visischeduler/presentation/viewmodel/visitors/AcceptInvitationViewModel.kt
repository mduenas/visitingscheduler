package com.markduenas.visischeduler.presentation.viewmodel.visitors

import com.markduenas.visischeduler.common.error.AppException
import com.markduenas.visischeduler.domain.entities.User
import com.markduenas.visischeduler.domain.repository.AuthRepository
import com.markduenas.visischeduler.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * UI state for the Accept Invitation screen.
 */
data class AcceptInvitationUiState(
    val inviteCode: String = "",
    val inviterName: String = "",
    val beneficiaryName: String = "",
    val isLoading: Boolean = false,
    val isAccepted: Boolean = false,
    val error: AppException? = null
)

/**
 * ViewModel for handling visitor invitations.
 */
class AcceptInvitationViewModel(
    private val authRepository: AuthRepository,
    private val inviteCode: String
) : BaseViewModel<AcceptInvitationUiState>(AcceptInvitationUiState(inviteCode = inviteCode)) {

    init {
        loadInvitationDetails()
    }

    /**
     * Loads details about the invitation using the invite code.
     */
    private fun loadInvitationDetails() {
        launchSafe {
            updateState { copy(isLoading = true) }
            // Simulation of API call to get invite details
            kotlinx.coroutines.delay(1000)
            updateState {
                copy(
                    inviterName = "John Coordinator",
                    beneficiaryName = "Mary Patient",
                    isLoading = false
                )
            }
        }
    }

    /**
     * Accepts the invitation.
     */
    fun acceptInvitation() {
        launchSafe {
            updateState { copy(isLoading = true) }
            // Simulation of API call to accept invitation
            kotlinx.coroutines.delay(1500)
            updateState {
                copy(
                    isLoading = false,
                    isAccepted = true
                )
            }
            // Notify system or navigate
            showSnackbar("Invitation accepted successfully")
        }
    }

    /**
     * Clears the current error.
     */
    fun clearError() {
        updateState { copy(error = null) }
    }
}
