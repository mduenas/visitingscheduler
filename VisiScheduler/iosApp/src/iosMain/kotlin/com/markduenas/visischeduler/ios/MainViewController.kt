package com.markduenas.visischeduler.ios

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import com.markduenas.visischeduler.di.initKoin
import com.markduenas.visischeduler.domain.repository.AuthRepository
import com.markduenas.visischeduler.presentation.AuthStateProvider
import com.markduenas.visischeduler.presentation.DefaultAuthStateProvider
import com.markduenas.visischeduler.presentation.VisiSchedulerApp
import com.markduenas.visischeduler.presentation.navigation.DeepLinkHandler
import org.koin.core.context.GlobalContext
import platform.UIKit.UIViewController

/**
 * iOS Main View Controller factory.
 * Creates the Compose UI hierarchy for the iOS app.
 */
fun MainViewController(): UIViewController {
    // Initialize Koin if not already initialized
    try {
        initKoin()
    } catch (e: Exception) {
        // Koin already initialized, ignore
    }

    return ComposeUIViewController {
        val deepLinkHandler = remember { DeepLinkHandler() }
        val authStateProvider = remember { DefaultAuthStateProvider() }

        // iOS theme wrapper would go here
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            VisiSchedulerApp(
                authStateProvider = authStateProvider,
                deepLinkHandler = deepLinkHandler,
                themeContent = { content ->
                    // Apply iOS-specific theme if needed
                    MaterialTheme {
                        content()
                    }
                }
            )

            // Auto-check authentication status on app start
            LaunchedEffect(Unit) {
                checkAuthenticationStatus(authStateProvider)
            }
        }
    }
}

/**
 * Create MainViewController with a pre-configured deep link URL.
 * Use this when the app is opened via a deep link.
 *
 * @param deepLinkUrl The deep link URL to handle
 */
fun MainViewControllerWithDeepLink(deepLinkUrl: String): UIViewController {
    // Initialize Koin if not already initialized
    try {
        initKoin()
    } catch (e: Exception) {
        // Koin already initialized, ignore
    }

    return ComposeUIViewController {
        val deepLinkHandler = remember {
            DeepLinkHandler().apply {
                handleDeepLink(deepLinkUrl, deferIfNotReady = true)
            }
        }
        val authStateProvider = remember { DefaultAuthStateProvider() }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            VisiSchedulerApp(
                authStateProvider = authStateProvider,
                deepLinkHandler = deepLinkHandler,
                themeContent = { content ->
                    MaterialTheme {
                        content()
                    }
                }
            )

            LaunchedEffect(Unit) {
                checkAuthenticationStatus(authStateProvider)
            }
        }
    }
}

/**
 * Handle a deep link after the app is already running.
 * Call this from Swift when a new deep link is received.
 *
 * @param url The deep link URL
 * @param handler The deep link handler instance
 */
fun handleDeepLink(url: String, handler: DeepLinkHandler) {
    handler.handleDeepLink(url, deferIfNotReady = false)
}

/**
 * Check authentication status via Firebase Auth and update state accordingly.
 */
private suspend fun checkAuthenticationStatus(authStateProvider: AuthStateProvider) {
    try {
        val authRepository = GlobalContext.get().get<AuthRepository>()
        if (authRepository.isAuthenticated()) {
            authStateProvider.setAuthenticated()
        } else {
            authStateProvider.setUnauthenticated()
        }
    } catch (e: Exception) {
        authStateProvider.setUnauthenticated()
    }
}

/**
 * Factory object for creating view controllers from Swift.
 */
object IOSViewControllerFactory {
    /**
     * Create the main view controller.
     */
    fun createMainViewController(): UIViewController = MainViewController()

    /**
     * Create a view controller with a deep link.
     */
    fun createWithDeepLink(url: String): UIViewController = MainViewControllerWithDeepLink(url)
}
