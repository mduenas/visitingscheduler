package com.markduenas.visischeduler.presentation.viewmodel.settings

import com.markduenas.visischeduler.common.error.AppException
import com.markduenas.visischeduler.common.util.AppResult
import com.markduenas.visischeduler.domain.entities.User
import com.markduenas.visischeduler.domain.repository.AuthRepository
import com.markduenas.visischeduler.domain.repository.UserRepository
import com.markduenas.visischeduler.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * UI state for the Profile screen.
 */
data class ProfileUiState(
    val user: User? = null,
    val isEditing: Boolean = false,
    val avatarUri: String? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isUploadingAvatar: Boolean = false,
    val error: AppException? = null,
    val visitStats: VisitStats = VisitStats()
)

/**
 * Visit statistics for the profile.
 */
data class VisitStats(
    val totalVisitsCoordinated: Int = 0,
    val totalVisitsMade: Int = 0,
    val upcomingVisits: Int = 0,
    val pendingApprovals: Int = 0
)

/**
 * ViewModel for managing user profile state and actions.
 */
class ProfileViewModel(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : BaseViewModel<ProfileUiState>(ProfileUiState()) {

    init {
        loadCurrentUser()
    }

    /**
     * Load the current authenticated user.
     */
    private fun loadCurrentUser() {
        viewModelScope.launch {
            updateState { copy(isLoading = true, error = null) }
            userRepository.currentUser.collectLatest { user ->
                updateState {
                    copy(
                        user = user,
                        avatarUri = user?.profileImageUrl,
                        isLoading = false
                    )
                }
                user?.let { loadVisitStats(it) }
            }
        }
    }

    /**
     * Load visit statistics for the user.
     */
    private fun loadVisitStats(user: User) {
        // TODO: Implement when visit statistics repository is available
        // For now, we'll use placeholder stats
        updateState {
            copy(
                visitStats = VisitStats(
                    totalVisitsCoordinated = if (user.canApproveVisits()) 42 else 0,
                    totalVisitsMade = 15,
                    upcomingVisits = 3,
                    pendingApprovals = if (user.canApproveVisits()) 5 else 0
                )
            )
        }
    }

    /**
     * Enable edit mode.
     */
    fun startEditing() {
        updateState { copy(isEditing = true) }
    }

    /**
     * Cancel edit mode without saving.
     */
    fun cancelEditing() {
        updateState {
            copy(
                isEditing = false,
                avatarUri = user?.profileImageUrl
            )
        }
    }

    /**
     * Update profile with new information.
     */
    fun updateProfile(
        firstName: String,
        lastName: String,
        phoneNumber: String?
    ) {
        launchSafe {
            updateState { copy(isSaving = true, error = null) }

            when (val result = userRepository.updateProfile(
                firstName = firstName,
                lastName = lastName,
                phoneNumber = phoneNumber,
                profileImageUrl = currentState.avatarUri
            )) {
                is Result.Success -> {
                    updateState {
                        copy(
                            user = result.getOrNull(),
                            isEditing = false,
                            isSaving = false
                        )
                    }
                    showSnackbar("Profile updated successfully")
                }
                is Result.Failure -> {
                    val exception = AppException.UnknownException(
                        result.exceptionOrNull()?.message ?: "Failed to update profile"
                    )
                    updateState { copy(error = exception, isSaving = false) }
                    showSnackbar(exception.message)
                }
            }
        }
    }

    /**
     * Update avatar with new image.
     */
    fun updateAvatar(imageData: ByteArray) {
        launchSafe {
            updateState { copy(isUploadingAvatar = true, error = null) }

            when (val result = userRepository.uploadProfileImage(imageData)) {
                is Result.Success -> {
                    val imageUrl = result.getOrNull()
                    updateState { copy(avatarUri = imageUrl, isUploadingAvatar = false) }
                }
                is Result.Failure -> {
                    val exception = AppException.UnknownException(
                        result.exceptionOrNull()?.message ?: "Failed to upload image"
                    )
                    updateState { copy(error = exception, isUploadingAvatar = false) }
                    showSnackbar(exception.message)
                }
            }
        }
    }

    /**
     * Set avatar URI from local picker.
     */
    fun setAvatarUri(uri: String) {
        updateState { copy(avatarUri = uri) }
    }

    /**
     * Logout the current user.
     */
    fun logout() {
        launchSafe {
            updateState { copy(isLoading = true) }

            when (authRepository.logout()) {
                is Result.Success -> {
                    navigate("login")
                }
                is Result.Failure -> {
                    updateState { copy(isLoading = false) }
                    // Still navigate to login even if logout API call fails
                    navigate("login")
                }
            }
        }
    }

    /**
     * Sync user data from server.
     */
    fun refreshProfile() {
        launchSafe {
            updateState { copy(isLoading = true, error = null) }

            when (val result = userRepository.syncUser()) {
                is Result.Success -> {
                    val user = result.getOrNull()
                    updateState {
                        copy(
                            user = user,
                            avatarUri = user?.profileImageUrl,
                            isLoading = false
                        )
                    }
                    user?.let { loadVisitStats(it) }
                }
                is Result.Failure -> {
                    val exception = AppException.UnknownException(
                        result.exceptionOrNull()?.message ?: "Failed to refresh profile"
                    )
                    updateState { copy(error = exception, isLoading = false) }
                }
            }
        }
    }

    /**
     * Navigate to change password screen.
     */
    fun navigateToChangePassword() {
        navigate("settings/security/change-password")
    }

    /**
     * Navigate to settings screen.
     */
    fun navigateToSettings() {
        navigate("settings")
    }

    /**
     * Navigate to notifications screen.
     */
    fun navigateToNotifications() {
        navigate("notifications")
    }

    /**
     * Clear any error state.
     */
    fun clearError() {
        updateState { copy(error = null) }
    }
}
