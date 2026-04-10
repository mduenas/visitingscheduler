package com.markduenas.visischeduler

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.markduenas.visischeduler.data.repository.AdRepositoryImpl
import com.markduenas.visischeduler.domain.repository.AdRepository
import com.markduenas.visischeduler.platform.AndroidBiometricHandler
import com.markduenas.visischeduler.platform.AndroidPermissionManager
import com.markduenas.visischeduler.platform.SessionMonitor
import com.markduenas.visischeduler.presentation.components.AdMobBanner
import com.markduenas.visischeduler.ui.screens.SplashScreen
import com.markduenas.visischeduler.ui.theme.VisiSchedulerTheme
import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * Main entry point activity for the VisiScheduler Android app.
 *
 * Uses Compose for UI and Voyager for navigation.
 * Implements edge-to-edge display for modern Android design.
 */
class MainActivity : FragmentActivity() {

    private val adRepository: AdRepository by inject()
    private val sessionMonitor: SessionMonitor by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Register permission launcher for AndroidPermissionManager
        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted -> AndroidPermissionManager.onPermissionsResult(granted) }
        AndroidPermissionManager.permissionLauncher = permissionLauncher

        // Set activity for ad repository (needed for purchase flow)
        (adRepository as? AdRepositoryImpl)?.setActivity(this)

        // Enable edge-to-edge display
        enableEdgeToEdge()

        setContent {
            VisiSchedulerAppWithAds()
        }
    }

    override fun onResume() {
        super.onResume()
        val ref = WeakReference(this as FragmentActivity)
        AndroidBiometricHandler.currentActivity = ref
        AndroidPermissionManager.currentActivity = ref
        CoroutineScope(Dispatchers.IO).launch { sessionMonitor.onForeground() }
    }

    override fun onPause() {
        super.onPause()
        AndroidBiometricHandler.currentActivity = null
        AndroidPermissionManager.currentActivity = null
    }
}

/**
 * Root composable for the VisiScheduler app with ad banner.
 */
@Composable
fun VisiSchedulerAppWithAds() {
    VisiSchedulerTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Banner ad at the top
                AdMobBanner(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )

                // Main app content
                Navigator(screen = SplashScreen()) { navigator ->
                    SlideTransition(navigator)
                }
            }
        }
    }
}
