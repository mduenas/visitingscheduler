package com.markduenas.visischeduler.presentation.ui.components.visitors

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.datetime.DayOfWeek

/**
 * Gets the short name for a day of week.
 */
private fun DayOfWeek.shortName(): String {
    return when (this) {
        DayOfWeek.MONDAY -> "M"
        DayOfWeek.TUESDAY -> "T"
        DayOfWeek.WEDNESDAY -> "W"
        DayOfWeek.THURSDAY -> "T"
        DayOfWeek.FRIDAY -> "F"
        DayOfWeek.SATURDAY -> "S"
        DayOfWeek.SUNDAY -> "S"
    }
}

/**
 * Gets the full name for a day of week.
 */
private fun DayOfWeek.fullName(): String {
    return when (this) {
        DayOfWeek.MONDAY -> "Monday"
        DayOfWeek.TUESDAY -> "Tuesday"
        DayOfWeek.WEDNESDAY -> "Wednesday"
        DayOfWeek.THURSDAY -> "Thursday"
        DayOfWeek.FRIDAY -> "Friday"
        DayOfWeek.SATURDAY -> "Saturday"
        DayOfWeek.SUNDAY -> "Sunday"
    }
}

/**
 * Gets a 2-letter abbreviation for a day of week.
 */
private fun DayOfWeek.abbreviation(): String {
    return when (this) {
        DayOfWeek.MONDAY -> "Mo"
        DayOfWeek.TUESDAY -> "Tu"
        DayOfWeek.WEDNESDAY -> "We"
        DayOfWeek.THURSDAY -> "Th"
        DayOfWeek.FRIDAY -> "Fr"
        DayOfWeek.SATURDAY -> "Sa"
        DayOfWeek.SUNDAY -> "Su"
    }
}

/**
 * A component for selecting days of the week.
 *
 * @param selectedDays The set of currently selected days
 * @param onDayToggle Callback when a day is toggled
 * @param label Optional label for the selector
 * @param allowedColor The color for allowed/selected days
 * @param modifier Modifier to apply to the component
 */
@Composable
fun DayOfWeekSelector(
    selectedDays: Set<DayOfWeek>,
    onDayToggle: (DayOfWeek) -> Unit,
    label: String? = null,
    allowedColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    val orderedDays = listOf(
        DayOfWeek.SUNDAY,
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
        DayOfWeek.SATURDAY
    )

    Column(modifier = modifier) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            orderedDays.forEach { day ->
                DayCircle(
                    day = day,
                    selected = selectedDays.contains(day),
                    selectedColor = allowedColor,
                    onClick = { onDayToggle(day) }
                )
            }
        }

        // Quick select buttons
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickSelectButton(
                text = "Weekdays",
                onClick = {
                    val weekdays = setOf(
                        DayOfWeek.MONDAY,
                        DayOfWeek.TUESDAY,
                        DayOfWeek.WEDNESDAY,
                        DayOfWeek.THURSDAY,
                        DayOfWeek.FRIDAY
                    )
                    weekdays.forEach { day ->
                        if (!selectedDays.contains(day)) {
                            onDayToggle(day)
                        }
                    }
                    // Deselect weekends
                    if (selectedDays.contains(DayOfWeek.SATURDAY)) onDayToggle(DayOfWeek.SATURDAY)
                    if (selectedDays.contains(DayOfWeek.SUNDAY)) onDayToggle(DayOfWeek.SUNDAY)
                },
                modifier = Modifier.weight(1f)
            )
            QuickSelectButton(
                text = "Weekends",
                onClick = {
                    val weekends = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
                    weekends.forEach { day ->
                        if (!selectedDays.contains(day)) {
                            onDayToggle(day)
                        }
                    }
                    // Deselect weekdays
                    listOf(
                        DayOfWeek.MONDAY,
                        DayOfWeek.TUESDAY,
                        DayOfWeek.WEDNESDAY,
                        DayOfWeek.THURSDAY,
                        DayOfWeek.FRIDAY
                    ).forEach { day ->
                        if (selectedDays.contains(day)) {
                            onDayToggle(day)
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            )
            QuickSelectButton(
                text = "Clear",
                onClick = {
                    selectedDays.forEach { day ->
                        onDayToggle(day)
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * A circular day selector.
 */
@Composable
private fun DayCircle(
    day: DayOfWeek,
    selected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .size(40.dp)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = if (selected) selectedColor else Color.Transparent,
        border = if (!selected) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        } else null
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = day.abbreviation(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Quick select button.
 */
@Composable
private fun QuickSelectButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * A compact display of selected days.
 */
@Composable
fun SelectedDaysDisplay(
    selectedDays: Set<DayOfWeek>,
    modifier: Modifier = Modifier
) {
    val orderedDays = listOf(
        DayOfWeek.SUNDAY,
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
        DayOfWeek.SATURDAY
    )

    val displayText = when {
        selectedDays.isEmpty() -> "No days selected"
        selectedDays.size == 7 -> "Every day"
        selectedDays == setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) -> "Weekends only"
        selectedDays == setOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY
        ) -> "Weekdays only"
        else -> orderedDays
            .filter { selectedDays.contains(it) }
            .joinToString(", ") { it.abbreviation() }
    }

    Text(
        text = displayText,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
    )
}

/**
 * Blocked days selector with red color scheme.
 */
@Composable
fun BlockedDaysSelector(
    blockedDays: Set<DayOfWeek>,
    onDayToggle: (DayOfWeek) -> Unit,
    modifier: Modifier = Modifier
) {
    DayOfWeekSelector(
        selectedDays = blockedDays,
        onDayToggle = onDayToggle,
        label = "Blocked Days",
        allowedColor = MaterialTheme.colorScheme.error,
        modifier = modifier
    )
}
