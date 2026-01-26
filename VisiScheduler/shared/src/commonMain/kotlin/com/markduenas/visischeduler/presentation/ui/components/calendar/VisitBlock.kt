package com.markduenas.visischeduler.presentation.ui.components.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.markduenas.visischeduler.domain.entities.Visit
import com.markduenas.visischeduler.domain.entities.VisitStatus
import com.markduenas.visischeduler.domain.entities.VisitType

/**
 * A colored block representing a visit on a timeline view.
 *
 * @param visit The visit to display
 * @param onClick Callback when the block is clicked
 * @param modifier Modifier for the component
 * @param height Optional fixed height for the block
 */
@Composable
fun VisitBlock(
    visit: Visit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp? = null
) {
    val backgroundColor = getVisitStatusColor(visit.status)
    val borderColor = getVisitStatusBorderColor(visit.status)

    Surface(
        modifier = modifier
            .then(
                if (height != null) Modifier.height(height) else Modifier
            )
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        color = backgroundColor,
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 1.dp
    ) {
        Box(
            modifier = Modifier
                .border(
                    width = 2.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(8.dp)
        ) {
            Column {
                // Time row
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTime(visit.startTime.toString()),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = borderColor
                    )
                    Text(
                        text = " - ",
                        style = MaterialTheme.typography.labelMedium,
                        color = borderColor
                    )
                    Text(
                        text = formatTime(visit.endTime.toString()),
                        style = MaterialTheme.typography.labelMedium,
                        color = borderColor
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Visit type icon and info
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (visit.visitType) {
                            VisitType.VIDEO_CALL -> Icons.Default.VideoCall
                            else -> Icons.Default.Person
                        },
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = borderColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = visit.purpose ?: "Visit",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Additional visitors indicator
                if (visit.additionalVisitors.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "+${visit.additionalVisitors.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Compact visit block for week/month views.
 */
@Composable
fun CompactVisitBlock(
    visit: Visit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = getVisitStatusBorderColor(visit.status)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .clickable { onClick() },
        color = getVisitStatusColor(visit.status),
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status indicator dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(borderColor)
            )
            Spacer(modifier = Modifier.width(6.dp))

            // Time
            Text(
                text = formatTime(visit.startTime.toString()),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = borderColor
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Purpose or type
            Text(
                text = visit.purpose ?: visit.visitType.name.replace("_", " "),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Visit indicator for month view cells.
 */
@Composable
fun VisitIndicator(
    visit: Visit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = getVisitStatusBorderColor(visit.status)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(2.dp))
            .clickable { onClick() },
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(2.dp)
    ) {
        Text(
            text = formatTime(visit.startTime.toString()),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Get the background color for a visit status.
 */
@Composable
private fun getVisitStatusColor(status: VisitStatus): Color {
    return when (status) {
        VisitStatus.PENDING -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
        VisitStatus.APPROVED -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        VisitStatus.DENIED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
        VisitStatus.COMPLETED -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        VisitStatus.CANCELLED -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        VisitStatus.NO_SHOW -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        VisitStatus.CHECKED_IN -> Color(0xFF2196F3).copy(alpha = 0.5f)
    }
}

/**
 * Get the border/accent color for a visit status.
 */
@Composable
private fun getVisitStatusBorderColor(status: VisitStatus): Color {
    return when (status) {
        VisitStatus.PENDING -> MaterialTheme.colorScheme.tertiary
        VisitStatus.APPROVED -> MaterialTheme.colorScheme.primary
        VisitStatus.DENIED -> MaterialTheme.colorScheme.error
        VisitStatus.COMPLETED -> MaterialTheme.colorScheme.secondary
        VisitStatus.CANCELLED -> MaterialTheme.colorScheme.outline
        VisitStatus.NO_SHOW -> MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
        VisitStatus.CHECKED_IN -> Color(0xFF2196F3)
    }
}

/**
 * Format time for display.
 */
private fun formatTime(time: String): String {
    // Simple formatting - in production would use proper time formatting
    return time.take(5) // HH:mm
}
