package com.markduenas.visischeduler.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import kotlinx.coroutines.flow.StateFlow

/**
 * Authentication state for determining which flow to show.
 */
enum class AuthState {
    /** Still determining auth state */
    LOADING,
    /** User is not authenticated */
    UNAUTHENTICATED,
    /** User is authenticated */
    AUTHENTICATED,
    /** User needs to complete MFA */
    REQUIRES_MFA
}

/**
 * Main navigation host for the app.
 * Handles the root navigation setup and auth flow separation.
 *
 * @param authState Flow of current authentication state
 * @param mfaChallengeId Optional MFA challenge ID when auth state is REQUIRES_MFA
 * @param onBackPressed Callback for handling back press at root level
 * @param deepLinkHandler Handler for processing deep links
 */
@Composable
fun NavigationGraph(
    authState: StateFlow<AuthState>,
    mfaChallengeId: String? = null,
    onBackPressed: ((AppScreen) -> Boolean)? = null,
    deepLinkHandler: DeepLinkHandler? = null
) {
    val currentAuthState by authState.collectAsState()

    val initialScreen = when (currentAuthState) {
        AuthState.LOADING -> AppScreen.Splash
        AuthState.UNAUTHENTICATED -> AppScreen.Login
        AuthState.AUTHENTICATED -> AppScreen.Dashboard
        AuthState.REQUIRES_MFA -> AppScreen.Mfa(mfaChallengeId ?: "")
    }

    Navigator(
        screen = initialScreen,
        onBackPressed = { currentScreen ->
            val appScreen = currentScreen as? AppScreen
            if (appScreen != null && onBackPressed != null) {
                onBackPressed(appScreen)
            } else {
                true // Allow default back behavior
            }
        }
    ) { navigator ->
        // Handle auth state changes
        LaunchedEffect(currentAuthState) {
            when (currentAuthState) {
                AuthState.LOADING -> {
                    // Stay on splash or navigate to it
                    if (navigator.lastItem !is AppScreen.Splash) {
                        navigator.replaceAll(AppScreen.Splash)
                    }
                }
                AuthState.UNAUTHENTICATED -> {
                    navigator.navigateToLogin()
                }
                AuthState.AUTHENTICATED -> {
                    // Only navigate if currently on auth screens
                    val currentScreen = navigator.lastItem as? AppScreen
                    if (currentScreen is AppScreen.Splash ||
                        currentScreen is AppScreen.Login ||
                        currentScreen is AppScreen.Register ||
                        currentScreen is AppScreen.Mfa
                    ) {
                        navigator.navigateToDashboardFromAuth()
                    }
                }
                AuthState.REQUIRES_MFA -> {
                    if (mfaChallengeId != null) {
                        navigator.navigateToMfa(mfaChallengeId)
                    }
                }
            }
        }

        // Handle deep links
        deepLinkHandler?.let { handler ->
            LaunchedEffect(handler.pendingDeepLink) {
                handler.pendingDeepLink?.let { deepLink ->
                    handleDeepLink(navigator, deepLink)
                    handler.clearPendingDeepLink()
                }
            }
        }

        SlideTransition(navigator)
    }
}

/**
 * Simplified navigation graph for when auth state is managed externally.
 *
 * @param startScreen The initial screen to show
 * @param onBackPressed Callback for handling back press
 */
@Composable
fun SimpleNavigationGraph(
    startScreen: AppScreen = AppScreen.Splash,
    onBackPressed: ((AppScreen) -> Boolean)? = null
) {
    Navigator(
        screen = startScreen,
        onBackPressed = { currentScreen ->
            val appScreen = currentScreen as? AppScreen
            if (appScreen != null && onBackPressed != null) {
                onBackPressed(appScreen)
            } else {
                true
            }
        }
    ) { navigator ->
        SlideTransition(navigator)
    }
}

/**
 * Handle incoming deep link and navigate appropriately.
 */
private fun handleDeepLink(navigator: Navigator, deepLink: DeepLink) {
    when (deepLink) {
        is DeepLink.Visit -> {
            navigator.navigateToVisitDetails(deepLink.visitId)
        }
        is DeepLink.Invitation -> {
            navigator.navigateToAcceptInvitation(deepLink.inviteCode)
        }
        is DeepLink.Calendar -> {
            // Navigate to calendar, optionally with date
            navigator.navigateToCalendar()
            // Date handling would be done in the CalendarScreen
        }
        is DeepLink.Message -> {
            navigator.navigateToMessageThread(deepLink.conversationId)
        }
        is DeepLink.Notification -> {
            navigator.navigateToNotifications()
        }
        is DeepLink.Profile -> {
            navigator.navigateToProfile()
        }
        is DeepLink.Settings -> {
            navigator.navigateToSettings()
        }
        is DeepLink.Unknown -> {
            // Log unknown deep link, possibly navigate to dashboard
        }
    }
}

/**
 * Navigation events that can be emitted from ViewModels.
 */
sealed interface NavigationEvent {
    /** Navigate to a specific screen */
    data class NavigateTo(val screen: AppScreen) : NavigationEvent

    /** Navigate back */
    data object NavigateBack : NavigationEvent

    /** Navigate back to root */
    data object NavigateToRoot : NavigationEvent

    /** Clear stack and navigate */
    data class ClearAndNavigate(val screen: AppScreen) : NavigationEvent

    /** Handle deep link */
    data class HandleDeepLink(val deepLink: DeepLink) : NavigationEvent
}

/**
 * Process navigation events from a ViewModel.
 */
fun Navigator.handleNavigationEvent(event: NavigationEvent) {
    when (event) {
        is NavigationEvent.NavigateTo -> push(event.screen)
        is NavigationEvent.NavigateBack -> navigateUp()
        is NavigationEvent.NavigateToRoot -> popToRoot()
        is NavigationEvent.ClearAndNavigate -> clearAndNavigate(event.screen)
        is NavigationEvent.HandleDeepLink -> handleDeepLink(this, event.deepLink)
    }
}
