package com.markduenas.visischeduler.presentation.ui.components.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.markduenas.visischeduler.presentation.viewmodel.settings.AppTheme

/**
 * A theme selector component with visual cards.
 */
@Composable
fun ThemeSelector(
    selectedTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ThemeCard.entries.forEach { themeCard ->
                ThemeOptionCard(
                    themeCard = themeCard,
                    isSelected = selectedTheme == themeCard.theme,
                    onClick = { onThemeSelected(themeCard.theme) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Theme card configuration.
 */
private enum class ThemeCard(
    val theme: AppTheme,
    val label: String,
    val icon: ImageVector,
    val previewBackground: Color,
    val previewContent: Color
) {
    LIGHT(
        theme = AppTheme.LIGHT,
        label = "Light",
        icon = Icons.Default.LightMode,
        previewBackground = Color(0xFFF5F5F5),
        previewContent = Color(0xFF1A1A1A)
    ),
    DARK(
        theme = AppTheme.DARK,
        label = "Dark",
        icon = Icons.Default.DarkMode,
        previewBackground = Color(0xFF1A1A1A),
        previewContent = Color(0xFFF5F5F5)
    ),
    SYSTEM(
        theme = AppTheme.SYSTEM,
        label = "System",
        icon = Icons.Default.SettingsBrightness,
        previewBackground = Color(0xFF808080),
        previewContent = Color.White
    )
}

/**
 * Individual theme option card.
 */
@Composable
private fun ThemeOptionCard(
    themeCard: ThemeCard,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Theme preview box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(themeCard.previewBackground),
                contentAlignment = Alignment.Center
            ) {
                // Mini preview of UI elements
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(themeCard.previewContent)
                    )
                    Box(
                        modifier = Modifier
                            .width(30.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(themeCard.previewContent.copy(alpha = 0.5f))
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Theme icon and label
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = themeCard.icon,
                    contentDescription = null,
                    tint = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = themeCard.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            // Selection indicator
            if (isSelected) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * A horizontal theme selector as a row of buttons.
 */
@Composable
fun ThemeSelectorRow(
    selectedTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AppTheme.entries.forEach { theme ->
            val isSelected = selectedTheme == theme
            val icon = when (theme) {
                AppTheme.LIGHT -> Icons.Default.LightMode
                AppTheme.DARK -> Icons.Default.DarkMode
                AppTheme.SYSTEM -> Icons.Default.SettingsBrightness
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                    .clickable { onThemeSelected(theme) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = theme.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}
