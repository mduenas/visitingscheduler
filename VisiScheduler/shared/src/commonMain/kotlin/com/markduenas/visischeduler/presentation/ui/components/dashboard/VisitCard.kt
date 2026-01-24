package com.markduenas.visischeduler.presentation.ui.components.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.markduenas.visischeduler.domain.entities.Visit
import com.markduenas.visischeduler.domain.entities.VisitStatus
import com.markduenas.visischeduler.domain.entities.VisitType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/**
 * A card displaying visit summary information.
 *
 * @param visit The visit to display
 * @param visitorName The name of the visitor
 * @param beneficiaryName The name of the beneficiary
 * @param onClick Callback when card is clicked
 * @param modifier Modifier for the card
 */
@Composable
fun VisitCard(
    visit: Visit,
    visitorName: String,
    beneficiaryName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = getStatusColor(visit.status)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status indicator
            Surface(
                shape = MaterialTheme.shapes.small,
                color = statusColor.copy(alpha = 0.12f),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = getVisitTypeIcon(visit.visitType),
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier
                        .padding(12.dp)
                        .size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Visit details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = visitorName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    VisitStatusChip(status = visit.status)
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Beneficiary
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = beneficiaryName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Date and time
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formatDate(visit.scheduledDate),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formatTimeRange(visit.startTime, visit.endTime),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * A compact visit card for list display.
 */
@Composable
fun CompactVisitCard(
    visit: Visit,
    visitorName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = getStatusColor(visit.status)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Time
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(60.dp)
            ) {
                Text(
                    text = formatTime(visit.startTime),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
                Text(
                    text = formatTime(visit.endTime),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Divider line
            Surface(
                modifier = Modifier
                    .width(2.dp)
                    .height(40.dp),
                color = statusColor
            ) {}

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = visitorName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = visit.visitType.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            VisitStatusChip(status = visit.status)
        }
    }
}

/**
 * Status chip for visit status display.
 */
@Composable
fun VisitStatusChip(
    status: VisitStatus,
    modifier: Modifier = Modifier
) {
    val color = getStatusColor(status)

    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.12f),
        modifier = modifier
    ) {
        Text(
            text = status.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

// Helper functions

@Composable
private fun getStatusColor(status: VisitStatus): Color {
    return when (status) {
        VisitStatus.PENDING -> MaterialTheme.colorScheme.tertiary
        VisitStatus.APPROVED -> MaterialTheme.colorScheme.primary
        VisitStatus.DENIED -> MaterialTheme.colorScheme.error
        VisitStatus.COMPLETED -> Color(0xFF4CAF50) // Green
        VisitStatus.CANCELLED -> MaterialTheme.colorScheme.outline
        VisitStatus.NO_SHOW -> MaterialTheme.colorScheme.error
    }
}

private fun getVisitTypeIcon(visitType: VisitType) = when (visitType) {
    VisitType.IN_PERSON -> Icons.Default.Person
    VisitType.VIDEO_CALL -> Icons.Default.Videocam
    VisitType.WINDOW_VISIT -> Icons.Default.Person
    VisitType.SPECIAL_EVENT -> Icons.Default.CalendarToday
}

private val VisitStatus.displayName: String
    get() = when (this) {
        VisitStatus.PENDING -> "Pending"
        VisitStatus.APPROVED -> "Approved"
        VisitStatus.DENIED -> "Denied"
        VisitStatus.COMPLETED -> "Completed"
        VisitStatus.CANCELLED -> "Cancelled"
        VisitStatus.NO_SHOW -> "No Show"
    }

private val VisitType.displayName: String
    get() = when (this) {
        VisitType.IN_PERSON -> "In Person"
        VisitType.VIDEO_CALL -> "Video Call"
        VisitType.WINDOW_VISIT -> "Window Visit"
        VisitType.SPECIAL_EVENT -> "Special Event"
    }

private fun formatDate(date: LocalDate): String {
    val monthNames = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )
    return "${monthNames[date.monthNumber - 1]} ${date.dayOfMonth}"
}

private fun formatTime(time: LocalTime): String {
    val hour = time.hour
    val minute = time.minute
    val period = if (hour < 12) "AM" else "PM"
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return "$displayHour:${minute.toString().padStart(2, '0')} $period"
}

private fun formatTimeRange(start: LocalTime, end: LocalTime): String {
    return "${formatTime(start)} - ${formatTime(end)}"
}
