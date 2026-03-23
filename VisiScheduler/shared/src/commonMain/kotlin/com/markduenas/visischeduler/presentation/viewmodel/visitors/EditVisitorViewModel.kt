package com.markduenas.visischeduler.presentation.viewmodel.visitors

import com.markduenas.visischeduler.common.error.AppException
import com.markduenas.visischeduler.domain.repository.UserRepository
import com.markduenas.visischeduler.presentation.viewmodel.BaseViewModel

/**
 * UI state for the edit visitor screen.
 */
data class EditVisitorUiState(
    val firstName: String = "",
    val lastName: String = "",
    val phoneNumber: String = "",
    val email: String = "",
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: AppException? = null
) {
    val canSubmit: Boolean
        get() = firstName.isNotBlank() && lastName.isNotBlank() && !isSubmitting && !isLoading
}

/**
 * ViewModel for editing a visitor's profile information.
 * Loads the visitor by ID and submits updates via [UserRepository.updateProfile].
 */
class EditVisitorViewModel(
    private val userRepository: UserRepository,
    private val visitorId: String
) : BaseViewModel<EditVisitorUiState>(EditVisitorUiState()) {

    init {
        loadVisitor()
    }

    private fun loadVisitor() {
        updateState { copy(isLoading = true, error = null) }
        launchSafe {
            userRepository.getUserById(visitorId).fold(
                onSuccess = { user ->
                    updateState {
                        copy(
                            firstName = user.firstName,
                            lastName = user.lastName,
                            phoneNumber = user.phoneNumber ?: "",
                            email = user.email,
                            isLoading = false
                        )
                    }
                },
                onFailure = { error ->
                    val exception = when (error) {
                        is AppException -> error
                        else -> AppException.UnknownException(
                            error.message ?: "Failed to load visitor",
                            error as? Exception
                        )
                    }
                    updateState { copy(isLoading = false, error = exception) }
                }
            )
        }
    }

    fun setFirstName(value: String) {
        updateState { copy(firstName = value) }
    }

    fun setLastName(value: String) {
        updateState { copy(lastName = value) }
    }

    fun setPhoneNumber(value: String) {
        updateState { copy(phoneNumber = value) }
    }

    fun saveChanges() {
        val state = currentState
        updateState { copy(isSubmitting = true, error = null) }

        launchSafe {
            userRepository.updateProfile(
                firstName = state.firstName,
                lastName = state.lastName,
                phoneNumber = state.phoneNumber.takeIf { it.isNotBlank() }
            ).fold(
                onSuccess = {
                    updateState { copy(isSubmitting = false) }
                    showSnackbar("Profile updated successfully")
                    navigateBack()
                },
                onFailure = { error ->
                    val exception = when (error) {
                        is AppException -> error
                        else -> AppException.UnknownException(
                            error.message ?: "Failed to update visitor",
                            error as? Exception
                        )
                    }
                    updateState { copy(isSubmitting = false, error = exception) }
                }
            )
        }
    }
}
