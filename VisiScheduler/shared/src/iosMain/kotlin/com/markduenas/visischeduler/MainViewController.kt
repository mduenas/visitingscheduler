package com.markduenas.visischeduler

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.markduenas.visischeduler.di.initKoin
import com.markduenas.visischeduler.di.iosAdModule
import com.markduenas.visischeduler.platform.SessionMonitor
import com.markduenas.visischeduler.platform.iosPlatformModule
import com.markduenas.visischeduler.presentation.AuthStateProvider
import com.markduenas.visischeduler.presentation.DefaultAuthStateProvider
import com.markduenas.visischeduler.presentation.VisiSchedulerApp
import com.markduenas.visischeduler.presentation.navigation.AuthState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    initKoin(
        platformModules = listOf(iosPlatformModule, iosAdModule)
    )

    // Register foreground lifecycle observer to update session activity
    val sessionMonitor = GlobalContext.get().get<SessionMonitor>()
    NSNotificationCenter.defaultCenter.addObserverForName(
        name = UIApplicationDidBecomeActiveNotification,
        `object` = null,
        queue = NSOperationQueue.mainQueue
    ) {
        CoroutineScope(Dispatchers.Default).launch { sessionMonitor.onForeground() }
    }

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
