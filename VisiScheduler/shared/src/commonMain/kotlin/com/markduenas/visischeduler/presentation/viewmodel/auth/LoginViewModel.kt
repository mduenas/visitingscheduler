package com.markduenas.visischeduler.presentation.viewmodel.auth

import com.markduenas.visischeduler.domain.entities.User
import com.markduenas.visischeduler.domain.repository.AuthRepository
import com.markduenas.visischeduler.domain.usecase.LoginException
import com.markduenas.visischeduler.domain.usecase.LoginUseCase
import com.markduenas.visischeduler.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * State for the login screen.
 */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val rememberMe: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val generalError: String? = null,
    val loginSuccess: Boolean = false,
    val user: User? = null,
    val isBiometricAvailable: Boolean = false,
    val requiresMfa: Boolean = false,
    val mfaChallenge: MfaChallenge? = null
)

/**
 * MFA challenge data when MFA is required.
 */
data class MfaChallenge(
    val challengeId: String,
    val method: MfaMethod,
    val maskedDestination: String? = null
)

/**
 * MFA methods supported.
 */
enum class MfaMethod {
    SMS,
    EMAIL,
    TOTP
}

/**
 * ViewModel for handling user login functionality.
 */
class LoginViewModel(
    private val loginUseCase: LoginUseCase,
    private val authRepository: AuthRepository
) : BaseViewModel<LoginUiState>(LoginUiState()) {

    private val _loginState = MutableStateFlow(LoginUiState())
    val loginState: StateFlow<LoginUiState> = _loginState.asStateFlow()

    init {
        checkBiometricAvailability()
    }

    /**
     * Update email field.
     */
    fun onEmailChange(email: String) {
        _loginState.value = _loginState.value.copy(
            email = email,
            emailError = null,
            generalError = null
        )
    }

    /**
     * Update password field.
     */
    fun onPasswordChange(password: String) {
        _loginState.value = _loginState.value.copy(
            password = password,
            passwordError = null,
            generalError = null
        )
    }

    /**
     * Toggle remember me checkbox.
     */
    fun onRememberMeChange(rememberMe: Boolean) {
        _loginState.value = _loginState.value.copy(rememberMe = rememberMe)
    }

    /**
     * Attempt to login with email and password.
     */
    fun login() {
        val currentState = _loginState.value

        // Validate fields
        val emailError = validateEmail(currentState.email)
        val passwordError = validatePassword(currentState.password)

        if (emailError != null || passwordError != null) {
            _loginState.value = currentState.copy(
                emailError = emailError,
                passwordError = passwordError
            )
            return
        }

        _loginState.value = currentState.copy(
            isLoading = true,
            emailError = null,
            passwordError = null,
            generalError = null
        )

        viewModelScope.launch {
            val result = loginUseCase(currentState.email, currentState.password)

            result.fold(
                onSuccess = { user ->
                    _loginState.value = _loginState.value.copy(
                        isLoading = false,
                        loginSuccess = true,
                        user = user
                    )
                    showSnackbar("Welcome back, ${user.firstName}!")
                },
                onFailure = { exception ->
                    handleLoginError(exception)
                }
            )
        }
    }

    /**
     * Attempt to login with biometric authentication.
     */
    fun loginWithBiometric() {
        _loginState.value = _loginState.value.copy(
            isLoading = true,
            generalError = null
        )

        viewModelScope.launch {
            val result = loginUseCase.loginWithBiometric()

            result.fold(
                onSuccess = { user ->
                    _loginState.value = _loginState.value.copy(
                        isLoading = false,
                        loginSuccess = true,
                        user = user
                    )
                    showSnackbar("Welcome back, ${user.firstName}!")
                },
                onFailure = { exception ->
                    handleLoginError(exception)
                }
            )
        }
    }

    /**
     * Verify MFA code.
     */
    fun verifyMfaCode(code: String) {
        val challenge = _loginState.value.mfaChallenge ?: return

        _loginState.value = _loginState.value.copy(
            isLoading = true,
            generalError = null
        )

        viewModelScope.launch {
            // In a real implementation, this would call the auth repository
            // authRepository.verifyMfa(challenge.challengeId, code)
            // For now, we'll simulate the MFA flow
            _loginState.value = _loginState.value.copy(
                isLoading = false,
                requiresMfa = false,
                mfaChallenge = null
            )
        }
    }

    /**
     * Cancel MFA flow.
     */
    fun cancelMfa() {
        _loginState.value = _loginState.value.copy(
            requiresMfa = false,
            mfaChallenge = null,
            isLoading = false
        )
    }

    /**
     * Clear error state.
     */
    fun clearError() {
        _loginState.value = _loginState.value.copy(
            emailError = null,
            passwordError = null,
            generalError = null
        )
    }

    /**
     * Reset login state.
     */
    fun resetState() {
        _loginState.value = LoginUiState(
            isBiometricAvailable = _loginState.value.isBiometricAvailable
        )
    }

    private fun checkBiometricAvailability() {
        viewModelScope.launch {
            val isAvailable = authRepository.isBiometricAvailable()
            _loginState.value = _loginState.value.copy(isBiometricAvailable = isAvailable)
        }
    }

    private fun validateEmail(email: String): String? {
        return when {
            email.isBlank() -> "Email is required"
            !isValidEmailFormat(email) -> "Please enter a valid email address"
            else -> null
        }
    }

    private fun validatePassword(password: String): String? {
        return when {
            password.isBlank() -> "Password is required"
            password.length < 8 -> "Password must be at least 8 characters"
            else -> null
        }
    }

    private fun isValidEmailFormat(email: String): Boolean {
        val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        return emailRegex.matches(email)
    }

    private fun handleLoginError(exception: Throwable) {
        val errorMessage = when (exception) {
            is LoginException.InvalidEmail -> {
                _loginState.value = _loginState.value.copy(
                    isLoading = false,
                    emailError = exception.message
                )
                return
            }
            is LoginException.InvalidPassword -> {
                _loginState.value = _loginState.value.copy(
                    isLoading = false,
                    passwordError = exception.message
                )
                return
            }
            is LoginException.InvalidCredentials -> "Invalid email or password"
            is LoginException.AccountLocked -> "Your account has been locked. Please contact support."
            is LoginException.AccountNotVerified -> "Please verify your email address before logging in."
            is LoginException.BiometricNotAvailable -> "Biometric authentication is not available"
            is LoginException.NetworkError -> "Network error. Please check your connection."
            else -> exception.message ?: "An unexpected error occurred"
        }

        _loginState.value = _loginState.value.copy(
            isLoading = false,
            generalError = errorMessage
        )
    }
}
