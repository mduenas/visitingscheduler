package com.markduenas.visischeduler.presentation.viewmodel.visitors

import com.markduenas.visischeduler.common.error.AppException
import com.markduenas.visischeduler.domain.repository.UserRepository
import com.markduenas.visischeduler.presentation.viewmodel.BaseViewModel

/**
 * Relationship types for visitors.
 */
enum class RelationshipType(val displayName: String) {
    FAMILY("Family Member"),
    SPOUSE("Spouse/Partner"),
    CHILD("Child"),
    PARENT("Parent"),
    SIBLING("Sibling"),
    FRIEND("Friend"),
    HEALTHCARE_PROVIDER("Healthcare Provider"),
    CLERGY("Clergy/Religious Leader"),
    ATTORNEY("Attorney/Legal Representative"),
    SOCIAL_WORKER("Social Worker"),
    OTHER("Other")
}

/**
 * Access levels for visitors.
 */
enum class AccessLevel(val displayName: String, val description: String) {
    AUTO_APPROVE("Auto-Approve", "Visits are automatically approved without coordinator review"),
    REQUIRES_APPROVAL("Requires Approval", "Each visit requires coordinator approval"),
    VIEW_ONLY("View Only", "Can view schedule but cannot request visits")
}

/**
 * UI State for the add visitor screen.
 */
data class AddVisitorUiState(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phone: String = "",
    val relationship: RelationshipType = RelationshipType.FAMILY,
    val accessLevel: AccessLevel = AccessLevel.REQUIRES_APPROVAL,
    val customRestrictionsEnabled: Boolean = false,
    val notes: String = "",
    val isLoading: Boolean = false,
    val error: AppException? = null,
    val validationErrors: Map<String, String> = emptyMap(),
    val invitationSent: Boolean = false
) {
    val fullName: String
        get() = "$firstName $lastName".trim()

    val isValid: Boolean
        get() = firstName.isNotBlank() &&
                lastName.isNotBlank() &&
                email.isNotBlank() &&
                isEmailValid &&
                validationErrors.isEmpty()

    val isEmailValid: Boolean
        get() = email.matches(EMAIL_REGEX)

    val isPhoneValid: Boolean
        get() = phone.isBlank() || phone.matches(PHONE_REGEX)

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        private val PHONE_REGEX = Regex("^[+]?[(]?[0-9]{3}[)]?[-\\s.]?[0-9]{3}[-\\s.]?[0-9]{4,6}$")
    }
}

/**
 * ViewModel for managing the add visitor screen.
 *
 * Provides functionality to:
 * - Enter visitor information (name, email, phone)
 * - Select relationship type
 * - Set access level
 * - Enable custom restrictions
 * - Send invitation to new visitor
 */
class AddVisitorViewModel(
    private val userRepository: UserRepository
) : BaseViewModel<AddVisitorUiState>(AddVisitorUiState()) {

    /**
     * Updates the first name.
     */
    fun onFirstNameChange(value: String) {
        updateState {
            copy(
                firstName = value,
                validationErrors = validationErrors - "firstName"
            )
        }
    }

    /**
     * Updates the last name.
     */
    fun onLastNameChange(value: String) {
        updateState {
            copy(
                lastName = value,
                validationErrors = validationErrors - "lastName"
            )
        }
    }

    /**
     * Updates the email.
     */
    fun onEmailChange(value: String) {
        updateState {
            copy(
                email = value.trim(),
                validationErrors = validationErrors - "email"
            )
        }
    }

    /**
     * Updates the phone number.
     */
    fun onPhoneChange(value: String) {
        updateState {
            copy(
                phone = value,
                validationErrors = validationErrors - "phone"
            )
        }
    }

    /**
     * Updates the relationship type.
     */
    fun onRelationshipChange(relationship: RelationshipType) {
        updateState { copy(relationship = relationship) }
    }

    /**
     * Updates the access level.
     */
    fun onAccessLevelChange(accessLevel: AccessLevel) {
        updateState { copy(accessLevel = accessLevel) }
    }

    /**
     * Toggles custom restrictions.
     */
    fun onCustomRestrictionsToggle(enabled: Boolean) {
        updateState { copy(customRestrictionsEnabled = enabled) }
    }

    /**
     * Updates the notes.
     */
    fun onNotesChange(value: String) {
        updateState { copy(notes = value) }
    }

    /**
     * Validates the form and returns true if valid.
     */
    private fun validate(): Boolean {
        val errors = mutableMapOf<String, String>()

        if (currentState.firstName.isBlank()) {
            errors["firstName"] = "First name is required"
        }

        if (currentState.lastName.isBlank()) {
            errors["lastName"] = "Last name is required"
        }

        if (currentState.email.isBlank()) {
            errors["email"] = "Email is required"
        } else if (!currentState.isEmailValid) {
            errors["email"] = "Invalid email format"
        }

        if (currentState.phone.isNotBlank() && !currentState.isPhoneValid) {
            errors["phone"] = "Invalid phone number format"
        }

        updateState { copy(validationErrors = errors) }
        return errors.isEmpty()
    }

    /**
     * Sends an invitation to the new visitor.
     */
    fun sendInvitation() {
        if (!validate()) {
            showSnackbar("Please fix the validation errors")
            return
        }

        launchSafe {
            updateState { copy(isLoading = true) }

            // Check if email already exists
            userRepository.getUserByEmail(currentState.email)
                .onSuccess { existingUser ->
                    updateState {
                        copy(
                            isLoading = false,
                            validationErrors = mapOf("email" to "This email is already registered")
                        )
                    }
                }
                .onFailure { error ->
                    // User doesn't exist, proceed with invitation
                    if (error is AppException.DataException.NotFound) {
                        sendInvitationEmail()
                    } else {
                        updateState {
                            copy(
                                isLoading = false,
                                error = error as? AppException
                            )
                        }
                    }
                }
        }
    }

    private suspend fun sendInvitationEmail() {
        // In a real implementation, this would call an API to send the invitation
        // For now, we'll simulate success
        try {
            // Simulated delay for sending email
            kotlinx.coroutines.delay(1000)

            updateState {
                copy(
                    isLoading = false,
                    invitationSent = true
                )
            }
            showSnackbar("Invitation sent to ${currentState.email}")
            navigate("visitors/invite?email=${currentState.email}&name=${currentState.fullName}")
        } catch (e: Exception) {
            updateState {
                copy(
                    isLoading = false,
                    error = AppException.UnknownException("Failed to send invitation", e)
                )
            }
        }
    }

    /**
     * Navigates to custom restrictions screen.
     */
    fun onConfigureRestrictionsClick() {
        if (currentState.customRestrictionsEnabled) {
            navigate("restrictions/add?visitor=${currentState.email}")
        }
    }

    /**
     * Clears the form.
     */
    fun clearForm() {
        updateState { AddVisitorUiState() }
    }

    /**
     * Clears the current error.
     */
    fun clearError() {
        updateState { copy(error = null) }
    }

    /**
     * Navigates back.
     */
    fun onBackClick() {
        navigateBack()
    }
}
