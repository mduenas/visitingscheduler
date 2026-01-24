package com.markduenas.visischeduler.presentation.ui.components.visitors

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Avatar sizes.
 */
enum class AvatarSize(val size: Dp, val fontSize: Int) {
    SMALL(32.dp, 12),
    MEDIUM(48.dp, 16),
    LARGE(64.dp, 24),
    EXTRA_LARGE(96.dp, 36)
}

/**
 * Generates a consistent color based on a string (e.g., name or email).
 */
private fun generateAvatarColor(seed: String): Color {
    val colors = listOf(
        Color(0xFF6366F1), // Indigo
        Color(0xFF8B5CF6), // Violet
        Color(0xFFEC4899), // Pink
        Color(0xFFEF4444), // Red
        Color(0xFFF97316), // Orange
        Color(0xFFF59E0B), // Amber
        Color(0xFF10B981), // Emerald
        Color(0xFF14B8A6), // Teal
        Color(0xFF06B6D4), // Cyan
        Color(0xFF3B82F6), // Blue
    )
    val index = seed.hashCode().let { if (it < 0) -it else it } % colors.size
    return colors[index]
}

/**
 * Extracts initials from a full name.
 */
private fun extractInitials(fullName: String): String {
    return fullName
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifEmpty { "?" }
}

/**
 * A circular avatar component that displays either an image or initials.
 *
 * @param fullName The full name of the visitor (used for initials)
 * @param imageUrl Optional URL for the visitor's profile image
 * @param size The size of the avatar
 * @param modifier Modifier to apply to the avatar
 */
@Composable
fun VisitorAvatar(
    fullName: String,
    imageUrl: String? = null,
    size: AvatarSize = AvatarSize.MEDIUM,
    modifier: Modifier = Modifier
) {
    val initials = extractInitials(fullName)
    val backgroundColor = generateAvatarColor(fullName)

    Box(
        modifier = modifier
            .size(size.size)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl != null) {
            // In a real implementation, use an image loading library like Coil or Kamel
            // For now, we'll show initials as a fallback
            Text(
                text = initials,
                color = Color.White,
                fontSize = size.fontSize.sp,
                fontWeight = FontWeight.Medium
            )
        } else {
            Text(
                text = initials,
                color = Color.White,
                fontSize = size.fontSize.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * A circular avatar with a status indicator badge.
 *
 * @param fullName The full name of the visitor
 * @param imageUrl Optional URL for the visitor's profile image
 * @param isOnline Whether the visitor is currently online
 * @param size The size of the avatar
 * @param modifier Modifier to apply to the avatar
 */
@Composable
fun VisitorAvatarWithStatus(
    fullName: String,
    imageUrl: String? = null,
    isOnline: Boolean = false,
    size: AvatarSize = AvatarSize.MEDIUM,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        VisitorAvatar(
            fullName = fullName,
            imageUrl = imageUrl,
            size = size
        )

        // Status indicator
        val indicatorSize = when (size) {
            AvatarSize.SMALL -> 8.dp
            AvatarSize.MEDIUM -> 12.dp
            AvatarSize.LARGE -> 14.dp
            AvatarSize.EXTRA_LARGE -> 18.dp
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(indicatorSize)
                .clip(CircleShape)
                .background(
                    if (isOnline) Color(0xFF10B981) else Color(0xFF9CA3AF)
                )
        )
    }
}

/**
 * A group avatar showing multiple visitors.
 *
 * @param names List of visitor names
 * @param maxDisplay Maximum number of avatars to display
 * @param size The size of each avatar
 * @param modifier Modifier to apply to the group
 */
@Composable
fun VisitorGroupAvatar(
    names: List<String>,
    maxDisplay: Int = 3,
    size: AvatarSize = AvatarSize.SMALL,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        val displayNames = names.take(maxDisplay)
        val remainingCount = names.size - maxDisplay

        displayNames.forEachIndexed { index, name ->
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
            ) {
                VisitorAvatar(
                    fullName = name,
                    size = size
                )
            }
        }

        if (remainingCount > 0) {
            Box(
                modifier = Modifier
                    .size(size.size)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+$remainingCount",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = size.fontSize.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
