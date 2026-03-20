package com.markduenas.visischeduler.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.markduenas.visischeduler.presentation.navigation.AppScreen
import dev.gitlive.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

/**
 * Splash screen displayed on app launch.
 *
 * Shows the app logo with animation, checks authentication state via Firebase,
 * then navigates to Dashboard (authenticated) or Login (unauthenticated).
 */
class SplashScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val auth: FirebaseAuth = koinInject()

        val alphaAnim = remember { Animatable(0f) }
        val scaleAnim = remember { Animatable(0.8f) }

        LaunchedEffect(key1 = true) {
            alphaAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500)
            )
            scaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500)
            )

            delay(1000)

            val destination = if (auth.currentUser != null) {
                AppScreen.Dashboard
            } else {
                AppScreen.Login
            }
            navigator.replace(destination)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .alpha(alphaAnim.value)
                    .scale(scaleAnim.value),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "VisiScheduler Logo",
                    modifier = Modifier.size(120.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )

                Text(
                    text = "VisiScheduler",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(top = 16.dp)
                )

                Text(
                    text = "Simplified Care Coordination",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Text(
                text = "Version 1.0.0",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            )
        }
    }
}
