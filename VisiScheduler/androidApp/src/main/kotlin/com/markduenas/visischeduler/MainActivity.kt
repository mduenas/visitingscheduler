package com.markduenas.visischeduler

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.markduenas.visischeduler.presentation.components.AdMobBanner
import com.markduenas.visischeduler.ui.screens.SplashScreen
import com.markduenas.visischeduler.ui.theme.VisiSchedulerTheme
import org.koin.android.ext.android.inject

/**
 * Main entry point activity for the VisiScheduler Android app.
 *
 * Uses Compose for UI and Voyager for navigation.
 * Implements edge-to-edge display for modern Android design.
 */
class MainActivity : ComponentActivity() {

    private val adRepository: AdRepositoryImpl by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set activity for ad repository (needed for purchase flow)
        adRepository.setActivity(this)

        // Enable edge-to-edge display
        enableEdgeToEdge()

        setContent {
            VisiSchedulerAppWithAds()
        }
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
