package com.markduenas.visischeduler.presentation.ui.components.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.markduenas.visischeduler.domain.entities.SlotType
import com.markduenas.visischeduler.domain.entities.TimeSlot

/**
 * A grid of selectable time slots.
 *
 * @param slots List of available time slots
 * @param selectedSlot Currently selected slot
 * @param onSlotSelected Callback when a slot is selected
 * @param isLoading Whether slots are being loaded
 * @param modifier Modifier for the component
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimeSlotPicker(
    slots: List<TimeSlot>,
    selectedSlot: TimeSlot?,
    onSlotSelected: (TimeSlot) -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (slots.isEmpty() && !isLoading) {
            EmptyTimeSlots()
        } else {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                slots.forEach { slot ->
                    TimeSlotChip(
                        slot = slot,
                        isSelected = slot.id == selectedSlot?.id,
                        onSelected = { onSlotSelected(slot) }
                    )
                }
            }
        }
    }
}

/**
 * Individual time slot chip.
 */
@Composable
fun TimeSlotChip(
    slot: TimeSlot,
    isSelected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        slot.isFull -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        slot.availabilityPercentage >= 0.5f -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.tertiaryContainer
    }

    val contentColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        slot.isFull -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = !slot.isFull) { onSelected() },
        color = backgroundColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }

            Text(
                text = formatTimeRange(slot.startTime.toString(), slot.endTime.toString()),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = contentColor
            )

            if (!slot.isFull && slot.remainingCapacity <= 3) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${slot.remainingCapacity} left",
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Detailed time slot card showing more information.
 */
@Composable
fun TimeSlotCard(
    slot: TimeSlot,
    isSelected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = slot.isAvailable && !slot.isFull) { onSelected() },
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primaryContainer
                !slot.isAvailable || slot.isFull -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatTimeRange(slot.startTime.toString(), slot.endTime.toString()),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Selection indicator
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Capacity information
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.People,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${slot.remainingCapacity}/${slot.maxCapacity} available",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Availability bar
            LinearProgressIndicator(
                progress = { slot.availabilityPercentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = when {
                    slot.availabilityPercentage >= 0.5f -> MaterialTheme.colorScheme.primary
                    slot.availabilityPercentage >= 0.25f -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.error
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            // Slot type badge
            if (slot.slotType != SlotType.REGULAR) {
                Spacer(modifier = Modifier.height(8.dp))
                SlotTypeBadge(slotType = slot.slotType)
            }

            // Notes
            slot.notes?.let { notes ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Badge showing the slot type.
 */
@Composable
private fun SlotTypeBadge(slotType: SlotType) {
    val (backgroundColor, text) = when (slotType) {
        SlotType.REGULAR -> MaterialTheme.colorScheme.primaryContainer to "Regular"
        SlotType.EXTENDED -> MaterialTheme.colorScheme.secondaryContainer to "Extended Hours"
        SlotType.HOLIDAY -> MaterialTheme.colorScheme.tertiaryContainer to "Holiday"
        SlotType.VIDEO_ONLY -> MaterialTheme.colorScheme.surfaceVariant to "Video Only"
        SlotType.SPECIAL_EVENT -> MaterialTheme.colorScheme.errorContainer to "Special Event"
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * List of time slot cards.
 */
@Composable
fun TimeSlotList(
    slots: List<TimeSlot>,
    selectedSlot: TimeSlot?,
    onSlotSelected: (TimeSlot) -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (isLoading) {
            item {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (slots.isEmpty() && !isLoading) {
            item {
                EmptyTimeSlots()
            }
        } else {
            items(slots) { slot ->
                TimeSlotCard(
                    slot = slot,
                    isSelected = slot.id == selectedSlot?.id,
                    onSelected = { onSlotSelected(slot) }
                )
            }
        }
    }
}

/**
 * Empty state when no time slots are available.
 */
@Composable
private fun EmptyTimeSlots() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.AccessTime,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No time slots available",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Please try selecting a different date",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

/**
 * Format time range for display.
 */
private fun formatTimeRange(startTime: String, endTime: String): String {
    // Simple formatting - in production would use proper time formatting
    return "$startTime - $endTime"
}
