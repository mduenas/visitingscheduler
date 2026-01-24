package com.markduenas.visischeduler.presentation.ui.components.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.dp
import com.markduenas.visischeduler.domain.entities.Visit
import com.markduenas.visischeduler.domain.entities.VisitStatus
import com.markduenas.visischeduler.presentation.viewmodel.dashboard.ScheduleTimelineItem as ScheduleItem
import kotlinx.datetime.LocalTime

/**
 * A timeline item component for displaying schedule entries.
 *
 * @param item The schedule timeline item
 * @param isLast Whether this is the last item in the list
 * @param onClick Callback when item is clicked
 * @param modifier Modifier for the component
 */
@Composable
fun TimelineItem(
    item: ScheduleItem,
    isLast: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = getTimelineStatusColor(item.visit.status, item.isCurrentOrNext)
    val isCompleted = item.visit.status == VisitStatus.COMPLETED

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        // Timeline indicator column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(48.dp)
        ) {
            // Time display
            Text(
                text = formatTimeShort(item.visit.startTime),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (item.isCurrentOrNext) FontWeight.Bold else FontWeight.Normal,
                color = if (item.isCurrentOrNext) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Timeline dot
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(statusColor),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = Color.White
                    )
                }
            }

            // Connecting line
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(
                            if (isCompleted) {
                                Color(0xFF4CAF50).copy(alpha = 0.3f)
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            }
                        )
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Content card
        TimelineContentCard(
            item = item,
            isHighlighted = item.isCurrentOrNext,
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 8.dp)
        )
    }
}

/**
 * Content card for timeline item.
 */
@Composable
private fun TimelineContentCard(
    item: ScheduleItem,
    isHighlighted: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isHighlighted) 4.dp else 1.dp
        )
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.visitorName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (isHighlighted) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Now",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Visiting ${item.beneficiaryName}",
                style = MaterialTheme.typography.bodySmall,
                color = if (isHighlighted) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = item.timeRange,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = if (isHighlighted) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

/**
 * A simple timeline list component.
 */
@Composable
fun TimelineList(
    items: List<ScheduleItem>,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        items.forEachIndexed { index, item ->
            TimelineItem(
                item = item,
                isLast = index == items.lastIndex,
                onClick = { onItemClick(item.visit.id) }
            )
        }
    }
}

/**
 * Empty timeline state.
 */
@Composable
fun EmptyTimeline(
    message: String = "No scheduled visits today",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Schedule,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Helper functions

@Composable
private fun getTimelineStatusColor(status: VisitStatus, isCurrent: Boolean): Color {
    return when {
        isCurrent -> MaterialTheme.colorScheme.primary
        status == VisitStatus.COMPLETED -> Color(0xFF4CAF50)
        status == VisitStatus.APPROVED -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.outlineVariant
    }
}

private fun formatTimeShort(time: LocalTime): String {
    val hour = time.hour
    val minute = time.minute
    val period = if (hour < 12) "AM" else "PM"
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return "$displayHour:${minute.toString().padStart(2, '0')}"
}
