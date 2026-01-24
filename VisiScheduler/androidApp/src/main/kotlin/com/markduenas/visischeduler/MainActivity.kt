package com.markduenas.visischeduler

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.markduenas.visischeduler.ui.screens.SplashScreen
import com.markduenas.visischeduler.ui.theme.VisiSchedulerTheme

/**
 * Main entry point activity for the VisiScheduler Android app.
 *
 * Uses Compose for UI and Voyager for navigation.
 * Implements edge-to-edge display for modern Android design.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display
        enableEdgeToEdge()

        setContent {
            VisiSchedulerApp()
        }
    }
}

/**
 * Root composable for the VisiScheduler app.
 */
@Composable
fun VisiSchedulerApp() {
    VisiSchedulerTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Navigator(screen = SplashScreen()) { navigator ->
                SlideTransition(navigator)
            }
        }
    }
}
