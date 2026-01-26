package com.markduenas.visischeduler

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.markduenas.visischeduler.di.initKoin
import com.markduenas.visischeduler.presentation.AuthStateProvider
import com.markduenas.visischeduler.presentation.DefaultAuthStateProvider
import com.markduenas.visischeduler.presentation.VisiSchedulerApp
import com.markduenas.visischeduler.presentation.navigation.AuthState
import kotlinx.coroutines.delay
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    initKoin()
    return ComposeUIViewController {
        // Create auth state provider that simulates session check
        val authStateProvider = remember { DefaultAuthStateProvider() }

        // Simulate session check on launch
        LaunchedEffect(Unit) {
            // Simulate checking for stored session
            delay(1500) // Show splash for 1.5 seconds

            // For now, always go to unauthenticated (login screen)
            // In a real app, this would check for a stored session token
            authStateProvider.setUnauthenticated()
        }

        VisiSchedulerApp(
            authStateProvider = authStateProvider
        )
    }
}
