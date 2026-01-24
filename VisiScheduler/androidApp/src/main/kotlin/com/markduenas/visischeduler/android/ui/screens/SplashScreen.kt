package com.markduenas.visischeduler.android.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.markduenas.visischeduler.domain.repository.AuthRepository
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

/**
 * Splash screen shown on app launch.
 */
class SplashScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val authRepository: AuthRepository = koinInject()

        var startAnimation by remember { mutableStateOf(false) }
        val alphaAnim by animateFloatAsState(
            targetValue = if (startAnimation) 1f else 0f,
            animationSpec = tween(durationMillis = 1000),
            label = "splash_alpha"
        )

        LaunchedEffect(key1 = true) {
            startAnimation = true
            delay(2000)

            // Check authentication status and navigate
            val isAuthenticated = authRepository.isAuthenticated()
            if (isAuthenticated) {
                // Navigate to home/dashboard
                // navigator.replace(HomeScreen())
            } else {
                // Navigate to login
                // navigator.replace(LoginScreen())
            }
        }

        SplashContent(alpha = alphaAnim)
    }
}

@Composable
private fun SplashContent(alpha: Float) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.alpha(alpha)
        ) {
            // App Icon
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = "VisiScheduler Logo",
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // App Name
            Text(
                text = "VisiScheduler",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tagline
            Text(
                text = "Schedule visits with ease",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Loading indicator
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 3.dp
            )
        }
    }
}
