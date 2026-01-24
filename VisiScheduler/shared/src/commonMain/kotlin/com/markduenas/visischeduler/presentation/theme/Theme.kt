package com.markduenas.visischeduler.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// VisiScheduler Brand Colors
private val VisiPrimary = Color(0xFF2E7D32) // Green - care, growth, health
private val VisiOnPrimary = Color(0xFFFFFFFF)
private val VisiPrimaryContainer = Color(0xFFA5D6A7)
private val VisiOnPrimaryContainer = Color(0xFF1B5E20)

private val VisiSecondary = Color(0xFF00796B) // Teal - trust, calm
private val VisiOnSecondary = Color(0xFFFFFFFF)
private val VisiSecondaryContainer = Color(0xFF80CBC4)
private val VisiOnSecondaryContainer = Color(0xFF004D40)

private val VisiTertiary = Color(0xFF5C6BC0) // Indigo - professionalism
private val VisiOnTertiary = Color(0xFFFFFFFF)
private val VisiTertiaryContainer = Color(0xFFC5CAE9)
private val VisiOnTertiaryContainer = Color(0xFF1A237E)

private val VisiError = Color(0xFFD32F2F)
private val VisiOnError = Color(0xFFFFFFFF)
private val VisiErrorContainer = Color(0xFFFFCDD2)
private val VisiOnErrorContainer = Color(0xFFB71C1C)

// Light Theme
private val LightColorScheme = lightColorScheme(
    primary = VisiPrimary,
    onPrimary = VisiOnPrimary,
    primaryContainer = VisiPrimaryContainer,
    onPrimaryContainer = VisiOnPrimaryContainer,
    secondary = VisiSecondary,
    onSecondary = VisiOnSecondary,
    secondaryContainer = VisiSecondaryContainer,
    onSecondaryContainer = VisiOnSecondaryContainer,
    tertiary = VisiTertiary,
    onTertiary = VisiOnTertiary,
    tertiaryContainer = VisiTertiaryContainer,
    onTertiaryContainer = VisiOnTertiaryContainer,
    error = VisiError,
    onError = VisiOnError,
    errorContainer = VisiErrorContainer,
    onErrorContainer = VisiOnErrorContainer,
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E)
)

// Dark Theme
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF81C784),
    onPrimary = Color(0xFF1B5E20),
    primaryContainer = Color(0xFF2E7D32),
    onPrimaryContainer = Color(0xFFA5D6A7),
    secondary = Color(0xFF4DB6AC),
    onSecondary = Color(0xFF004D40),
    secondaryContainer = Color(0xFF00796B),
    onSecondaryContainer = Color(0xFF80CBC4),
    tertiary = Color(0xFF9FA8DA),
    onTertiary = Color(0xFF1A237E),
    tertiaryContainer = Color(0xFF3949AB),
    onTertiaryContainer = Color(0xFFC5CAE9),
    error = Color(0xFFEF9A9A),
    onError = Color(0xFFB71C1C),
    errorContainer = Color(0xFFC62828),
    onErrorContainer = Color(0xFFFFCDD2),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
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
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = VisiSchedulerTypography,
        content = content
    )
}
