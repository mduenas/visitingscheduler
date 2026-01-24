package com.markduenas.visischeduler.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import kotlinx.coroutines.flow.StateFlow

/**
 * Authentication flow states.
 */
sealed interface AuthFlowState {
    /** Initial splash state */
    data object Splash : AuthFlowState

    /** Session is being restored */
    data object RestoringSession : AuthFlowState

    /** Biometric authentication required */
    data object BiometricRequired : AuthFlowState

    /** User needs to login */
    data object Login : AuthFlowState

    /** User is registering */
    data object Register : AuthFlowState

    /** Password recovery flow */
    data object ForgotPassword : AuthFlowState

    /** MFA challenge pending */
    data class MfaChallenge(val challengeId: String) : AuthFlowState

    /** User is authenticated */
    data object Authenticated : AuthFlowState

    /** Session expired, needs re-authentication */
    data object SessionExpired : AuthFlowState
}

/**
 * Navigator specifically for authentication flows.
 * Manages the complete auth flow: Splash -> Login/Register -> MFA -> Dashboard
 *
 * @param authFlowState Current auth flow state
 * @param onAuthComplete Callback when authentication is complete
 * @param onSessionRestored Callback when session is restored
 * @param onBiometricRequired Callback when biometric prompt should be shown
 */
@Composable
fun AuthNavigator(
    authFlowState: StateFlow<AuthFlowState>,
    onAuthComplete: () -> Unit = {},
    onSessionRestored: () -> Unit = {},
    onBiometricRequired: () -> Unit = {}
) {
    val currentState by authFlowState.collectAsState()

    val initialScreen: AppScreen = when (currentState) {
        is AuthFlowState.Splash -> AppScreen.Splash
        is AuthFlowState.RestoringSession -> AppScreen.Splash
        is AuthFlowState.BiometricRequired -> AppScreen.Splash
        is AuthFlowState.Login -> AppScreen.Login
        is AuthFlowState.Register -> AppScreen.Register
        is AuthFlowState.ForgotPassword -> AppScreen.ForgotPassword
        is AuthFlowState.MfaChallenge -> {
            AppScreen.Mfa((currentState as AuthFlowState.MfaChallenge).challengeId)
        }
        is AuthFlowState.Authenticated -> AppScreen.Dashboard
        is AuthFlowState.SessionExpired -> AppScreen.Login
    }

    Navigator(
        screen = initialScreen,
        onBackPressed = { currentScreen ->
            // Handle back press in auth flow
            when (currentScreen) {
                is AppScreen.Login -> false // Don't allow back from login
                is AppScreen.Splash -> false // Don't allow back from splash
                else -> true
            }
        }
    ) { navigator ->
        // React to auth state changes
        LaunchedEffect(currentState) {
            handleAuthStateChange(
                navigator = navigator,
                state = currentState,
                onAuthComplete = onAuthComplete,
                onSessionRestored = onSessionRestored,
                onBiometricRequired = onBiometricRequired
            )
        }

        SlideTransition(navigator)
    }
}

/**
 * Handle auth state changes and navigate accordingly.
 */
private fun handleAuthStateChange(
    navigator: Navigator,
    state: AuthFlowState,
    onAuthComplete: () -> Unit,
    onSessionRestored: () -> Unit,
    onBiometricRequired: () -> Unit
) {
    when (state) {
        is AuthFlowState.Splash -> {
            // Stay on splash, initial state
        }
        is AuthFlowState.RestoringSession -> {
            // Stay on splash while restoring
        }
        is AuthFlowState.BiometricRequired -> {
            onBiometricRequired()
        }
        is AuthFlowState.Login -> {
            navigator.replaceAll(AppScreen.Login)
        }
        is AuthFlowState.Register -> {
            // Push register if not already there
            if (navigator.lastItem !is AppScreen.Register) {
                navigator.push(AppScreen.Register)
            }
        }
        is AuthFlowState.ForgotPassword -> {
            if (navigator.lastItem !is AppScreen.ForgotPassword) {
                navigator.push(AppScreen.ForgotPassword)
            }
        }
        is AuthFlowState.MfaChallenge -> {
            navigator.push(AppScreen.Mfa(state.challengeId))
        }
        is AuthFlowState.Authenticated -> {
            navigator.replaceAll(AppScreen.Dashboard)
            onAuthComplete()
        }
        is AuthFlowState.SessionExpired -> {
            navigator.replaceAll(AppScreen.Login)
        }
    }
}

/**
 * Auth navigation actions for use in ViewModels.
 */
sealed interface AuthNavAction {
    data object GoToLogin : AuthNavAction
    data object GoToRegister : AuthNavAction
    data object GoToForgotPassword : AuthNavAction
    data class GoToMfa(val challengeId: String) : AuthNavAction
    data object GoToDashboard : AuthNavAction
    data object GoBack : AuthNavAction
    data object Logout : AuthNavAction
}

/**
 * Execute auth navigation action on a Navigator.
 */
fun Navigator.executeAuthAction(action: AuthNavAction) {
    when (action) {
        is AuthNavAction.GoToLogin -> navigateToLogin()
        is AuthNavAction.GoToRegister -> navigateToRegister()
        is AuthNavAction.GoToForgotPassword -> navigateToForgotPassword()
        is AuthNavAction.GoToMfa -> navigateToMfa(action.challengeId)
        is AuthNavAction.GoToDashboard -> navigateToDashboardFromAuth()
        is AuthNavAction.GoBack -> navigateUp()
        is AuthNavAction.Logout -> navigateToLogin()
    }
}

/**
 * Auth result data that can be passed between screens.
 */
sealed interface AuthResult {
    /** Login was successful */
    data class LoginSuccess(val userId: String) : AuthResult

    /** Registration was successful */
    data class RegisterSuccess(val userId: String, val needsVerification: Boolean) : AuthResult

    /** MFA was completed */
    data class MfaComplete(val challengeId: String) : AuthResult

    /** Password reset email sent */
    data class PasswordResetSent(val email: String) : AuthResult

    /** Operation was cancelled */
    data object Cancelled : AuthResult
}

/**
 * Extension to check if user is on an auth screen.
 */
val Navigator.isOnAuthScreen: Boolean
    get() {
        val current = lastItem
        return current is AppScreen.Splash ||
                current is AppScreen.Login ||
                current is AppScreen.Register ||
                current is AppScreen.ForgotPassword ||
                current is AppScreen.Mfa
    }

/**
 * Extension to get the current auth screen, if any.
 */
val Navigator.currentAuthScreen: AppScreen?
    get() = if (isOnAuthScreen) lastItem as? AppScreen else null
