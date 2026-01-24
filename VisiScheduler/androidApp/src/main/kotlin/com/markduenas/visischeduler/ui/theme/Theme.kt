package com.markduenas.visischeduler.ui.theme

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

// Brand Colors
private val Primary = Color(0xFF1976D2)           // Blue - Trust & Reliability
private val PrimaryContainer = Color(0xFFBBDEFB)
private val Secondary = Color(0xFF43A047)          // Green - Health & Care
private val SecondaryContainer = Color(0xFFC8E6C9)
private val Tertiary = Color(0xFF7B1FA2)           // Purple - Premium Features
private val TertiaryContainer = Color(0xFFE1BEE7)
private val Error = Color(0xFFD32F2F)              // Red - Errors & Alerts
private val ErrorContainer = Color(0xFFFFCDD2)

// Surface Colors
private val SurfaceLight = Color(0xFFFEFBFF)
private val SurfaceDark = Color(0xFF1C1B1F)
private val OnSurfaceLight = Color(0xFF1C1B1F)
private val OnSurfaceDark = Color(0xFFE6E1E5)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = Color(0xFF0D47A1),
    secondary = Secondary,
    onSecondary = Color.White,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = Color(0xFF1B5E20),
    tertiary = Tertiary,
    onTertiary = Color.White,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = Color(0xFF4A148C),
    error = Error,
    onError = Color.White,
    errorContainer = ErrorContainer,
    onErrorContainer = Color(0xFFB71C1C),
    background = Color(0xFFFEFBFF),
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF90CAF9),
    onPrimary = Color(0xFF0D47A1),
    primaryContainer = Color(0xFF1565C0),
    onPrimaryContainer = Color(0xFFBBDEFB),
    secondary = Color(0xFFA5D6A7),
    onSecondary = Color(0xFF1B5E20),
    secondaryContainer = Color(0xFF2E7D32),
    onSecondaryContainer = Color(0xFFC8E6C9),
    tertiary = Color(0xFFCE93D8),
    onTertiary = Color(0xFF4A148C),
    tertiaryContainer = Color(0xFF6A1B9A),
    onTertiaryContainer = Color(0xFFE1BEE7),
    error = Color(0xFFEF9A9A),
    onError = Color(0xFFB71C1C),
    errorContainer = Color(0xFFC62828),
    onErrorContainer = Color(0xFFFFCDD2),
    background = SurfaceDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F)
)

/**
 * VisiScheduler theme composable.
 *
 * @param darkTheme Whether to use dark theme. Defaults to system setting.
 * @param dynamicColor Whether to use dynamic colors (Android 12+). Defaults to true.
 * @param content The content to display with this theme.
 */
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
