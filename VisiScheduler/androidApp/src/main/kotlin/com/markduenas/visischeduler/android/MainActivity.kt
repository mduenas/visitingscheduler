package com.markduenas.visischeduler.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.markduenas.visischeduler.android.ui.theme.VisiSchedulerTheme
import com.markduenas.visischeduler.presentation.AuthStateProvider
import com.markduenas.visischeduler.presentation.DefaultAuthStateProvider
import com.markduenas.visischeduler.presentation.VisiSchedulerApp
import com.markduenas.visischeduler.presentation.navigation.DeepLinkHandler
import io.github.aakira.napier.Napier
import org.koin.android.ext.android.getKoin
import org.koin.core.component.KoinComponent

/**
 * Main entry point Activity for VisiScheduler Android app.
 * Handles app initialization, deep links, and navigation setup.
 */
class MainActivity : ComponentActivity(), KoinComponent {

    private lateinit var deepLinkHandler: DeepLinkHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        Napier.d { "MainActivity created" }

        // Initialize deep link handler
        deepLinkHandler = DeepLinkHandler()

        // Handle initial deep link if app was opened via deep link
        handleIntent(intent)

        setContent {
            VisiSchedulerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Get auth state provider from Koin or create default
                    val authStateProvider = try {
                        getKoin().getOrNull<AuthStateProvider>()
                    } catch (e: Exception) {
                        null
                    } ?: remember { DefaultAuthStateProvider() }

                    VisiSchedulerApp(
                        authStateProvider = authStateProvider,
                        deepLinkHandler = deepLinkHandler,
                        themeContent = { content ->
                            // Theme is already applied via VisiSchedulerTheme above
                            content()
                        }
                    )

                    // Auto-check authentication status on app start
                    LaunchedEffect(Unit) {
                        checkAuthenticationStatus(authStateProvider)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Napier.d { "onNewIntent: ${intent.data}" }
        handleIntent(intent)
    }

    /**
     * Handle incoming intent for deep links.
     */
    private fun handleIntent(intent: Intent?) {
        when {
            // Handle deep link from URL scheme (visischeduler://)
            intent?.action == Intent.ACTION_VIEW && intent.data != null -> {
                val uri = intent.data
                Napier.d { "Deep link received: $uri" }
                uri?.toString()?.let { url ->
                    deepLinkHandler.handleDeepLink(url, deferIfNotReady = true)
                }
            }
            // Handle deep link from push notification
            intent?.hasExtra(EXTRA_DEEP_LINK) == true -> {
                val deepLinkUrl = intent.getStringExtra(EXTRA_DEEP_LINK)
                Napier.d { "Deep link from notification: $deepLinkUrl" }
                deepLinkUrl?.let { url ->
                    deepLinkHandler.handleDeepLink(url, deferIfNotReady = true)
                }
            }
        }
    }

    /**
     * Check authentication status and update state accordingly.
     */
    private suspend fun checkAuthenticationStatus(authStateProvider: AuthStateProvider) {
        try {
            // In a real app, this would check with the auth repository
            // For now, simulate a brief loading then set to unauthenticated
            kotlinx.coroutines.delay(1500) // Simulate splash delay
            authStateProvider.setUnauthenticated()
        } catch (e: Exception) {
            Napier.e(e) { "Error checking auth status" }
            authStateProvider.setUnauthenticated()
        }
    }

    companion object {
        /** Extra key for deep link URL passed from notifications */
        const val EXTRA_DEEP_LINK = "deep_link_url"
    }
}
