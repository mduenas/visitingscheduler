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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.unit.dp
import com.markduenas.visischeduler.presentation.viewmodel.scheduling.VisitDuration

/**
 * A selector for visit duration options.
 *
 * @param selectedDuration Currently selected duration
 * @param onDurationSelected Callback when a duration is selected
 * @param availableDurations List of available duration options
 * @param modifier Modifier for the component
 */
@Composable
fun DurationSelector(
    selectedDuration: VisitDuration,
    onDurationSelected: (VisitDuration) -> Unit,
    availableDurations: List<VisitDuration> = VisitDuration.entries,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Duration",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            availableDurations.forEach { duration ->
                DurationChip(
                    duration = duration,
                    isSelected = duration == selectedDuration,
                    onSelected = { onDurationSelected(duration) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Individual duration chip.
 */
@Composable
private fun DurationChip(
    duration: VisitDuration,
    isSelected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onSelected() },
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        shape = RoundedCornerShape(8.dp),
        tonalElevation = if (isSelected) 0.dp else 1.dp
    ) {
        Box(
            modifier = Modifier
                .then(
                    if (!isSelected) {
                        Modifier.border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        )
                    } else Modifier
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = duration.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

/**
 * Horizontal duration selector with filter chips.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DurationChipSelector(
    selectedDuration: VisitDuration,
    onDurationSelected: (VisitDuration) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        VisitDuration.entries.forEach { duration ->
            FilterChip(
                selected = duration == selectedDuration,
                onClick = { onDurationSelected(duration) },
                label = { Text(duration.displayName) },
                leadingIcon = if (duration == selectedDuration) {
                    {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                } else null
            )
        }
    }
}

/**
 * Compact duration selector for limited space.
 */
@Composable
fun CompactDurationSelector(
    selectedDuration: VisitDuration,
    onDurationSelected: (VisitDuration) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        VisitDuration.entries.forEach { duration ->
            CompactDurationButton(
                duration = duration,
                isSelected = duration == selectedDuration,
                onSelected = { onDurationSelected(duration) }
            )
        }
    }
}

/**
 * Compact duration button.
 */
@Composable
private fun CompactDurationButton(
    duration: VisitDuration,
    isSelected: Boolean,
    onSelected: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color.Transparent
                }
            )
            .then(
                if (!isSelected) {
                    Modifier.border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(4.dp)
                    )
                } else Modifier
            )
            .clickable { onSelected() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = getShortDurationLabel(duration),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

/**
 * Get short label for duration.
 */
private fun getShortDurationLabel(duration: VisitDuration): String {
    return when (duration) {
        VisitDuration.FIFTEEN_MINUTES -> "15m"
        VisitDuration.THIRTY_MINUTES -> "30m"
        VisitDuration.ONE_HOUR -> "1h"
        VisitDuration.ONE_AND_HALF_HOURS -> "90m"
        VisitDuration.TWO_HOURS -> "2h"
    }
}

/**
 * Duration info display showing selected duration.
 */
@Composable
fun DurationDisplay(
    duration: VisitDuration,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Timer,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = duration.displayName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
