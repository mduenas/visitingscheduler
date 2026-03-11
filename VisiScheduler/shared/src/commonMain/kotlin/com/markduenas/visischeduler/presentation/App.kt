package com.markduenas.visischeduler.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.markduenas.visischeduler.presentation.navigation.AppScreen
import com.markduenas.visischeduler.presentation.navigation.AuthState
import com.markduenas.visischeduler.presentation.navigation.DeepLinkHandler
import com.markduenas.visischeduler.presentation.navigation.navigateToDashboardFromAuth
import com.markduenas.visischeduler.presentation.navigation.navigateToLogin
import com.markduenas.visischeduler.presentation.navigation.navigateToMfa
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Root composable for VisiScheduler app.
 * Sets up theme, navigation, and handles auth state.
 *
 * @param authStateProvider Provider for authentication state (injected via Koin)
 * @param deepLinkHandler Handler for processing deep links
 * @param themeContent Theme wrapper composable
 */
@Composable
fun VisiSchedulerApp(
    authStateProvider: AuthStateProvider? = null,
    deepLinkHandler: DeepLinkHandler? = null,
    syncManager: com.markduenas.visischeduler.data.sync.SyncManager = org.koin.compose.koinInject(),
    themeContent: @Composable (content: @Composable () -> Unit) -> Unit = { content -> content() }
) {
    val authState = authStateProvider?.authState
        ?: remember { MutableStateFlow(AuthState.LOADING) }

    val currentAuthState by authState.collectAsState()
    val handler = deepLinkHandler ?: remember { DeepLinkHandler() }

    // Initialize sync when authenticated
    LaunchedEffect(currentAuthState) {
        if (currentAuthState == AuthState.AUTHENTICATED) {
            syncManager.startPeriodicSync()
        }
    }

    themeContent {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            VisiSchedulerNavHost(
                authState = authState,
                deepLinkHandler = handler
            )
        }
    }
}

/**
 * Main navigation host for the app.
 */
@Composable
private fun VisiSchedulerNavHost(
    authState: StateFlow<AuthState>,
    deepLinkHandler: DeepLinkHandler
) {
    val currentAuthState by authState.collectAsState()
    val mfaChallengeId = remember { MutableStateFlow<String?>(null) }

    val initialScreen: AppScreen = when (currentAuthState) {
        AuthState.LOADING -> AppScreen.Splash
        AuthState.UNAUTHENTICATED -> AppScreen.Login
        AuthState.AUTHENTICATED -> AppScreen.Dashboard
        AuthState.REQUIRES_MFA -> AppScreen.Mfa(mfaChallengeId.value ?: "")
    }

    Navigator(
        screen = initialScreen,
        onBackPressed = { currentScreen ->
            handleBackPress(currentScreen as? AppScreen)
        }
    ) { navigator ->
        // Handle auth state changes
        LaunchedEffect(currentAuthState) {
            handleAuthStateChange(navigator, currentAuthState, mfaChallengeId.value)
        }

        // Handle deep links
        LaunchedEffect(deepLinkHandler.pendingDeepLink) {
            deepLinkHandler.pendingDeepLink?.let { deepLink ->
                // Only handle deep links when authenticated
                if (currentAuthState == AuthState.AUTHENTICATED) {
                    handleDeepLinkNavigation(navigator, deepLink)
                    deepLinkHandler.clearPendingDeepLink()
                }
            }
        }

        SlideTransition(navigator)
    }
}

/**
 * Handle authentication state changes.
 */
private fun handleAuthStateChange(
    navigator: Navigator,
    authState: AuthState,
    mfaChallengeId: String?
) {
    when (authState) {
        AuthState.LOADING -> {
            // Stay on or navigate to splash
            if (navigator.lastItem !is AppScreen.Splash) {
                navigator.replaceAll(AppScreen.Splash)
            }
        }
        AuthState.UNAUTHENTICATED -> {
            navigator.navigateToLogin()
        }
        AuthState.AUTHENTICATED -> {
            // Navigate away from auth screens
            when (navigator.lastItem) {
                is AppScreen.Splash,
                is AppScreen.Login,
                is AppScreen.Register,
                is AppScreen.Mfa -> {
                    navigator.navigateToDashboardFromAuth()
                }
                else -> {
                    // Already on a main screen, do nothing
                }
            }
        }
        AuthState.REQUIRES_MFA -> {
            if (mfaChallengeId != null) {
                navigator.navigateToMfa(mfaChallengeId)
            }
        }
    }
}

/**
 * Handle deep link navigation.
 */
private fun handleDeepLinkNavigation(
    navigator: Navigator,
    deepLink: com.markduenas.visischeduler.presentation.navigation.DeepLink
) {
    when (deepLink) {
        is com.markduenas.visischeduler.presentation.navigation.DeepLink.Visit -> {
            navigator.push(AppScreen.VisitDetails(deepLink.visitId))
        }
        is com.markduenas.visischeduler.presentation.navigation.DeepLink.Invitation -> {
            navigator.push(AppScreen.AcceptInvitation(deepLink.inviteCode))
        }
        is com.markduenas.visischeduler.presentation.navigation.DeepLink.Calendar -> {
            navigator.push(AppScreen.Calendar)
        }
        is com.markduenas.visischeduler.presentation.navigation.DeepLink.Message -> {
            navigator.push(AppScreen.MessageThread(deepLink.conversationId))
        }
        is com.markduenas.visischeduler.presentation.navigation.DeepLink.Notification -> {
            navigator.push(AppScreen.Notifications)
        }
        is com.markduenas.visischeduler.presentation.navigation.DeepLink.Profile -> {
            navigator.push(AppScreen.Profile)
        }
        is com.markduenas.visischeduler.presentation.navigation.DeepLink.Settings -> {
            navigator.push(AppScreen.Settings)
        }
        is com.markduenas.visischeduler.presentation.navigation.DeepLink.Unknown -> {
            // Log or ignore unknown deep links
        }
    }
}

/**
 * Handle back press behavior for different screens.
 */
private fun handleBackPress(screen: AppScreen?): Boolean {
    return when (screen) {
        // Prevent back from these screens
        is AppScreen.Splash -> false
        is AppScreen.Login -> false
        is AppScreen.Dashboard -> false // Use explicit logout

        // Allow back from all other screens
        else -> true
    }
}

/**
 * Interface for providing authentication state.
 * Implemented by the auth service/repository.
 */
interface AuthStateProvider {
    val authState: StateFlow<AuthState>
    val mfaChallengeId: StateFlow<String?>

    fun setAuthenticated()
    fun setUnauthenticated()
    fun setRequiresMfa(challengeId: String)
    fun setLoading()
}

/**
 * Default implementation of AuthStateProvider for testing and preview.
 */
class DefaultAuthStateProvider : AuthStateProvider {
    private val _authState = MutableStateFlow(AuthState.LOADING)
    override val authState: StateFlow<AuthState> = _authState

    private val _mfaChallengeId = MutableStateFlow<String?>(null)
    override val mfaChallengeId: StateFlow<String?> = _mfaChallengeId

    override fun setAuthenticated() {
        _mfaChallengeId.value = null
        _authState.value = AuthState.AUTHENTICATED
    }

    override fun setUnauthenticated() {
        _mfaChallengeId.value = null
        _authState.value = AuthState.UNAUTHENTICATED
    }

    override fun setRequiresMfa(challengeId: String) {
        _mfaChallengeId.value = challengeId
        _authState.value = AuthState.REQUIRES_MFA
    }

    override fun setLoading() {
        _authState.value = AuthState.LOADING
    }
}
