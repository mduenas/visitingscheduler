package com.markduenas.visischeduler.android.ui.components.auth

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Biometric authentication button with fingerprint/face icon.
 *
 * @param onClick Callback when button is clicked
 * @param modifier Modifier for the button
 * @param enabled Whether the button is enabled
 * @param isLoading Whether to show loading indicator
 * @param text Button text
 * @param icon Icon to display (defaults to fingerprint)
 */
@Composable
fun BiometricButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    text: String = "Login with Biometrics",
    icon: ImageVector = Icons.Default.Fingerprint
) {
    val scale by animateFloatAsState(
        targetValue = if (enabled && !isLoading) 1f else 0.95f,
        animationSpec = tween(durationMillis = 150),
        label = "biometric_scale"
    )

    val borderColor by animateColorAsState(
        targetValue = if (enabled && !isLoading) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        },
        label = "border_color"
    )

    val contentColor by animateColorAsState(
        targetValue = if (enabled && !isLoading) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        },
        label = "content_color"
    )

    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .scale(scale),
        enabled = enabled && !isLoading,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = contentColor
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = contentColor,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = if (isLoading) "Authenticating..." else text,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

/**
 * Biometric icon button (smaller, icon-only variant).
 */
@Composable
fun BiometricIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector = Icons.Default.Fingerprint,
    contentDescription: String = "Login with biometrics"
) {
    val scale by animateFloatAsState(
        targetValue = if (enabled && !isLoading) 1f else 0.9f,
        animationSpec = tween(durationMillis = 150),
        label = "biometric_icon_scale"
    )

    val borderColor by animateColorAsState(
        targetValue = if (enabled && !isLoading) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        },
        label = "icon_border_color"
    )

    val contentColor by animateColorAsState(
        targetValue = if (enabled && !isLoading) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        },
        label = "icon_content_color"
    )

    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .size(56.dp)
            .scale(scale),
        enabled = enabled && !isLoading,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = contentColor
        ),
        contentPadding = ButtonDefaults.ContentPadding
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = contentColor,
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
