package com.markduenas.visischeduler.presentation.viewmodel.auth

import com.markduenas.visischeduler.domain.entities.Role
import com.markduenas.visischeduler.domain.entities.User
import com.markduenas.visischeduler.domain.repository.AuthRepository
import com.markduenas.visischeduler.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * State for the registration screen.
 */
data class RegisterUiState(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val selectedRole: Role = Role.PENDING_VISITOR,
    val acceptedTerms: Boolean = false,
    val isLoading: Boolean = false,
    val firstNameError: String? = null,
    val lastNameError: String? = null,
    val emailError: String? = null,
    val phoneError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val termsError: String? = null,
    val generalError: String? = null,
    val registrationSuccess: Boolean = false,
    val user: User? = null
) {
    /**
     * Available roles for registration.
     */
    companion object {
        val availableRoles = listOf(
            Role.PENDING_VISITOR to "Visitor",
            Role.APPROVED_VISITOR to "Approved Visitor"
        )
    }

    /**
     * Password strength indicator.
     */
    val passwordStrength: PasswordStrength
        get() = calculatePasswordStrength(password)
}

/**
 * Password strength levels.
 */
enum class PasswordStrength {
    WEAK,
    FAIR,
    GOOD,
    STRONG
}

/**
 * Calculate password strength based on criteria.
 */
private fun calculatePasswordStrength(password: String): PasswordStrength {
    if (password.isEmpty()) return PasswordStrength.WEAK

    var score = 0

    // Length check
    if (password.length >= 8) score++
    if (password.length >= 12) score++

    // Contains lowercase
    if (password.any { it.isLowerCase() }) score++

    // Contains uppercase
    if (password.any { it.isUpperCase() }) score++

    // Contains digit
    if (password.any { it.isDigit() }) score++

    // Contains special character
    if (password.any { !it.isLetterOrDigit() }) score++

    return when {
        score <= 2 -> PasswordStrength.WEAK
        score <= 3 -> PasswordStrength.FAIR
        score <= 4 -> PasswordStrength.GOOD
        else -> PasswordStrength.STRONG
    }
}

/**
 * ViewModel for handling user registration functionality.
 */
class RegisterViewModel(
    private val authRepository: AuthRepository
) : BaseViewModel<RegisterUiState>(RegisterUiState()) {

    private val _registerState = MutableStateFlow(RegisterUiState())
    val registerState: StateFlow<RegisterUiState> = _registerState.asStateFlow()

    /**
     * Update first name field.
     */
    fun onFirstNameChange(firstName: String) {
        _registerState.value = _registerState.value.copy(
            firstName = firstName,
            firstNameError = null,
            generalError = null
        )
    }

    /**
     * Update last name field.
     */
    fun onLastNameChange(lastName: String) {
        _registerState.value = _registerState.value.copy(
            lastName = lastName,
            lastNameError = null,
            generalError = null
        )
    }

    /**
     * Update email field.
     */
    fun onEmailChange(email: String) {
        _registerState.value = _registerState.value.copy(
            email = email,
            emailError = null,
            generalError = null
        )
    }

    /**
     * Update phone number field.
     */
    fun onPhoneNumberChange(phoneNumber: String) {
        _registerState.value = _registerState.value.copy(
            phoneNumber = phoneNumber,
            phoneError = null,
            generalError = null
        )
    }

    /**
     * Update password field.
     */
    fun onPasswordChange(password: String) {
        _registerState.value = _registerState.value.copy(
            password = password,
            passwordError = null,
            confirmPasswordError = if (password != _registerState.value.confirmPassword &&
                _registerState.value.confirmPassword.isNotEmpty()) {
                "Passwords do not match"
            } else null,
            generalError = null
        )
    }

    /**
     * Update confirm password field.
     */
    fun onConfirmPasswordChange(confirmPassword: String) {
        _registerState.value = _registerState.value.copy(
            confirmPassword = confirmPassword,
            confirmPasswordError = if (confirmPassword != _registerState.value.password) {
                "Passwords do not match"
            } else null,
            generalError = null
        )
    }

    /**
     * Update selected role.
     */
    fun onRoleSelect(role: Role) {
        _registerState.value = _registerState.value.copy(
            selectedRole = role,
            generalError = null
        )
    }

    /**
     * Toggle terms acceptance.
     */
    fun onTermsAcceptedChange(accepted: Boolean) {
        _registerState.value = _registerState.value.copy(
            acceptedTerms = accepted,
            termsError = null,
            generalError = null
        )
    }

    /**
     * Attempt to register new user.
     */
    fun register() {
        val currentState = _registerState.value

        // Validate all fields
        val validationErrors = validateAllFields(currentState)

        if (validationErrors.hasErrors) {
            _registerState.value = currentState.copy(
                firstNameError = validationErrors.firstNameError,
                lastNameError = validationErrors.lastNameError,
                emailError = validationErrors.emailError,
                phoneError = validationErrors.phoneError,
                passwordError = validationErrors.passwordError,
                confirmPasswordError = validationErrors.confirmPasswordError,
                termsError = validationErrors.termsError
            )
            return
        }

        _registerState.value = currentState.copy(
            isLoading = true,
            generalError = null
        )

        viewModelScope.launch {
            val result = authRepository.register(
                email = currentState.email.trim().lowercase(),
                password = currentState.password,
                firstName = currentState.firstName.trim(),
                lastName = currentState.lastName.trim(),
                phoneNumber = currentState.phoneNumber.takeIf { it.isNotBlank() }?.trim()
            )

            result.fold(
                onSuccess = { user ->
                    _registerState.value = _registerState.value.copy(
                        isLoading = false,
                        registrationSuccess = true,
                        user = user
                    )
                    showSnackbar("Registration successful! Please check your email to verify your account.")
                },
                onFailure = { exception ->
                    handleRegistrationError(exception)
                }
            )
        }
    }

    /**
     * Clear error state.
     */
    fun clearError() {
        _registerState.value = _registerState.value.copy(
            firstNameError = null,
            lastNameError = null,
            emailError = null,
            phoneError = null,
            passwordError = null,
            confirmPasswordError = null,
            termsError = null,
            generalError = null
        )
    }

    /**
     * Reset registration state.
     */
    fun resetState() {
        _registerState.value = RegisterUiState()
    }

    private data class ValidationErrors(
        val firstNameError: String? = null,
        val lastNameError: String? = null,
        val emailError: String? = null,
        val phoneError: String? = null,
        val passwordError: String? = null,
        val confirmPasswordError: String? = null,
        val termsError: String? = null
    ) {
        val hasErrors: Boolean
            get() = listOfNotNull(
                firstNameError,
                lastNameError,
                emailError,
                phoneError,
                passwordError,
                confirmPasswordError,
                termsError
            ).isNotEmpty()
    }

    private fun validateAllFields(state: RegisterUiState): ValidationErrors {
        return ValidationErrors(
            firstNameError = validateFirstName(state.firstName),
            lastNameError = validateLastName(state.lastName),
            emailError = validateEmail(state.email),
            phoneError = validatePhoneNumber(state.phoneNumber),
            passwordError = validatePassword(state.password),
            confirmPasswordError = validateConfirmPassword(state.password, state.confirmPassword),
            termsError = validateTerms(state.acceptedTerms)
        )
    }

    private fun validateFirstName(firstName: String): String? {
        return when {
            firstName.isBlank() -> "First name is required"
            firstName.length < 2 -> "First name must be at least 2 characters"
            firstName.length > 50 -> "First name must be less than 50 characters"
            !firstName.all { it.isLetter() || it == ' ' || it == '-' || it == '\'' } ->
                "First name contains invalid characters"
            else -> null
        }
    }

    private fun validateLastName(lastName: String): String? {
        return when {
            lastName.isBlank() -> "Last name is required"
            lastName.length < 2 -> "Last name must be at least 2 characters"
            lastName.length > 50 -> "Last name must be less than 50 characters"
            !lastName.all { it.isLetter() || it == ' ' || it == '-' || it == '\'' } ->
                "Last name contains invalid characters"
            else -> null
        }
    }

    private fun validateEmail(email: String): String? {
        val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        return when {
            email.isBlank() -> "Email is required"
            !emailRegex.matches(email) -> "Please enter a valid email address"
            else -> null
        }
    }

    private fun validatePhoneNumber(phoneNumber: String): String? {
        if (phoneNumber.isBlank()) return null // Phone is optional

        // Remove common formatting characters for validation
        val digitsOnly = phoneNumber.filter { it.isDigit() }

        return when {
            digitsOnly.length < 10 -> "Phone number must be at least 10 digits"
            digitsOnly.length > 15 -> "Phone number is too long"
            else -> null
        }
    }

    private fun validatePassword(password: String): String? {
        return when {
            password.isBlank() -> "Password is required"
            password.length < 8 -> "Password must be at least 8 characters"
            password.length > 128 -> "Password is too long"
            !password.any { it.isUpperCase() } -> "Password must contain at least one uppercase letter"
            !password.any { it.isLowerCase() } -> "Password must contain at least one lowercase letter"
            !password.any { it.isDigit() } -> "Password must contain at least one number"
            !password.any { !it.isLetterOrDigit() } -> "Password must contain at least one special character"
            else -> null
        }
    }

    private fun validateConfirmPassword(password: String, confirmPassword: String): String? {
        return when {
            confirmPassword.isBlank() -> "Please confirm your password"
            confirmPassword != password -> "Passwords do not match"
            else -> null
        }
    }

    private fun validateTerms(accepted: Boolean): String? {
        return if (!accepted) "You must accept the terms and conditions" else null
    }

    private fun handleRegistrationError(exception: Throwable) {
        val errorMessage = when {
            exception.message?.contains("email", ignoreCase = true) == true &&
                exception.message?.contains("exists", ignoreCase = true) == true -> {
                _registerState.value = _registerState.value.copy(
                    isLoading = false,
                    emailError = "An account with this email already exists"
                )
                return
            }
            exception.message?.contains("network", ignoreCase = true) == true ->
                "Network error. Please check your connection and try again."
            exception.message?.contains("timeout", ignoreCase = true) == true ->
                "Request timed out. Please try again."
            else -> exception.message ?: "Registration failed. Please try again."
        }

        _registerState.value = _registerState.value.copy(
            isLoading = false,
            generalError = errorMessage
        )
    }
}
