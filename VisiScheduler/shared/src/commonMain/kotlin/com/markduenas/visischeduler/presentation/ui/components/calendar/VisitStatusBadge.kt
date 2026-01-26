package com.markduenas.visischeduler.presentation.ui.components.calendar

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.markduenas.visischeduler.domain.entities.VisitStatus

/**
 * Badge displaying the visit status with icon and color.
 *
 * @param status The visit status to display
 * @param modifier Modifier for the component
 * @param showIcon Whether to show the status icon
 */
@Composable
fun VisitStatusBadge(
    status: VisitStatus,
    modifier: Modifier = Modifier,
    showIcon: Boolean = true
) {
    val (backgroundColor, contentColor) = getStatusColors(status)
    val icon = getStatusIcon(status)
    val text = getStatusText(status)

    Surface(
        modifier = modifier,
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showIcon) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = contentColor
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

/**
 * Compact status badge without icon.
 */
@Composable
fun CompactStatusBadge(
    status: VisitStatus,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, contentColor) = getStatusColors(status)
    val text = getStatusText(status)

    Surface(
        modifier = modifier,
        color = backgroundColor,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * Status dot indicator.
 */
@Composable
fun StatusDot(
    status: VisitStatus,
    modifier: Modifier = Modifier
) {
    val (_, contentColor) = getStatusColors(status)

    Surface(
        modifier = modifier.size(12.dp),
        color = contentColor,
        shape = RoundedCornerShape(6.dp)
    ) {}
}

/**
 * Get colors for a visit status.
 */
@Composable
private fun getStatusColors(status: VisitStatus): Pair<Color, Color> {
    return when (status) {
        VisitStatus.PENDING -> Pair(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        VisitStatus.APPROVED -> Pair(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        VisitStatus.DENIED -> Pair(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
        VisitStatus.COMPLETED -> Pair(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
        VisitStatus.CANCELLED -> Pair(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
        VisitStatus.NO_SHOW -> Pair(
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
            MaterialTheme.colorScheme.onErrorContainer
        )
        VisitStatus.CHECKED_IN -> Pair(
            Color(0xFF2196F3).copy(alpha = 0.2f),
            Color(0xFF2196F3)
        )
    }
}

/**
 * Get icon for a visit status.
 */
private fun getStatusIcon(status: VisitStatus): ImageVector {
    return when (status) {
        VisitStatus.PENDING -> Icons.Default.HourglassEmpty
        VisitStatus.APPROVED -> Icons.Default.Check
        VisitStatus.DENIED -> Icons.Default.Close
        VisitStatus.COMPLETED -> Icons.Default.CheckCircle
        VisitStatus.CANCELLED -> Icons.Default.Cancel
        VisitStatus.NO_SHOW -> Icons.Default.PersonOff
        VisitStatus.CHECKED_IN -> Icons.Default.CheckCircle
    }
}

/**
 * Get display text for a visit status.
 */
private fun getStatusText(status: VisitStatus): String {
    return when (status) {
        VisitStatus.PENDING -> "Pending"
        VisitStatus.APPROVED -> "Approved"
        VisitStatus.DENIED -> "Denied"
        VisitStatus.COMPLETED -> "Completed"
        VisitStatus.CANCELLED -> "Cancelled"
        VisitStatus.NO_SHOW -> "No Show"
        VisitStatus.CHECKED_IN -> "Checked In"
    }
}

/**
 * Large status indicator with description.
 */
@Composable
fun StatusIndicator(
    status: VisitStatus,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, contentColor) = getStatusColors(status)
    val icon = getStatusIcon(status)
    val text = getStatusText(status)
    val description = getStatusDescription(status)

    Surface(
        modifier = modifier,
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = contentColor
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

/**
 * Get description for a visit status.
 */
private fun getStatusDescription(status: VisitStatus): String {
    return when (status) {
        VisitStatus.PENDING -> "Awaiting approval from coordinator"
        VisitStatus.APPROVED -> "Visit has been approved"
        VisitStatus.DENIED -> "Visit request was denied"
        VisitStatus.COMPLETED -> "Visit was completed successfully"
        VisitStatus.CANCELLED -> "Visit has been cancelled"
        VisitStatus.NO_SHOW -> "Visitor did not show up"
        VisitStatus.CHECKED_IN -> "Visitor has checked in"
    }
}
