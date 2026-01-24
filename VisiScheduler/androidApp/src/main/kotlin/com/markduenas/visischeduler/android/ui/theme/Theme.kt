package com.markduenas.visischeduler.android.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// VisiScheduler Brand Colors
private val VisiBlue = Color(0xFF1976D2)
private val VisiBlueLight = Color(0xFF63A4FF)
private val VisiLightBlue = Color(0xFF42A5F5)
private val VisiDarkBlue = Color(0xFF004BA0)
private val VisiGreen = Color(0xFF4CAF50)
private val VisiOrange = Color(0xFFFF9800)
private val VisiRed = Color(0xFFF44336)

private val LightColorScheme = lightColorScheme(
    primary = VisiBlue,
    onPrimary = Color.White,
    primaryContainer = VisiBlueLight,
    onPrimaryContainer = VisiDarkBlue,
    secondary = VisiLightBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3F2FD),
    onSecondaryContainer = VisiDarkBlue,
    tertiary = VisiGreen,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE8F5E9),
    onTertiaryContainer = Color(0xFF1B5E20),
    error = VisiRed,
    onError = Color.White,
    errorContainer = Color(0xFFFFEBEE),
    onErrorContainer = Color(0xFFB71C1C),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF1C1B1F),
    surface = Color.White,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E)
)

private val DarkColorScheme = darkColorScheme(
    primary = VisiLightBlue,
    onPrimary = VisiDarkBlue,
    primaryContainer = VisiBlue,
    onPrimaryContainer = Color(0xFFE3F2FD),
    secondary = VisiBlueLight,
    onSecondary = Color(0xFF003258),
    secondaryContainer = VisiDarkBlue,
    onSecondaryContainer = Color(0xFFE3F2FD),
    tertiary = Color(0xFF81C784),
    onTertiary = Color(0xFF003909),
    tertiaryContainer = Color(0xFF1B5E20),
    onTertiaryContainer = Color(0xFFA5D6A7),
    error = Color(0xFFEF9A9A),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFFB71C1C),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99)
)

@Composable
fun VisiSchedulerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
