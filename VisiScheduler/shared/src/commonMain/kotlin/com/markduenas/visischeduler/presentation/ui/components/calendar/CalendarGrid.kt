package com.markduenas.visischeduler.presentation.ui.components.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * A month calendar grid component displaying days with visit indicators.
 *
 * @param monthDates List of dates for the month (null for padding days)
 * @param selectedDate Currently selected date
 * @param onDateSelected Callback when a date is selected
 * @param hasVisitsOnDate Function to check if a date has visits
 * @param getVisitCountForDate Function to get visit count for a date
 * @param modifier Modifier for the component
 */
@Composable
fun CalendarGrid(
    monthDates: List<LocalDate?>,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    hasVisitsOnDate: (LocalDate) -> Boolean = { false },
    getVisitCountForDate: (LocalDate) -> Int = { 0 },
    modifier: Modifier = Modifier
) {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    Column(modifier = modifier.fillMaxWidth()) {
        // Day of week headers
        WeekdayHeader()

        Spacer(modifier = Modifier.height(8.dp))

        // Calendar grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            items(monthDates) { date ->
                CalendarDayCell(
                    date = date,
                    isSelected = date == selectedDate,
                    isToday = date == today,
                    hasVisits = date?.let { hasVisitsOnDate(it) } ?: false,
                    visitCount = date?.let { getVisitCountForDate(it) } ?: 0,
                    onDateSelected = { date?.let { onDateSelected(it) } }
                )
            }
        }
    }
}

/**
 * Header row showing weekday abbreviations.
 */
@Composable
private fun WeekdayHeader() {
    val weekdays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        weekdays.forEach { day ->
            Text(
                text = day,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Individual day cell in the calendar grid.
 */
@Composable
private fun CalendarDayCell(
    date: LocalDate?,
    isSelected: Boolean,
    isToday: Boolean,
    hasVisits: Boolean,
    visitCount: Int,
    onDateSelected: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    else -> Color.Transparent
                }
            )
            .then(
                if (isToday && !isSelected) {
                    Modifier.border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp)
                    )
                } else Modifier
            )
            .clickable(enabled = date != null) { onDateSelected() },
        contentAlignment = Alignment.Center
    ) {
        if (date != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.onPrimary
                        isToday -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )

                // Visit indicator dots
                if (hasVisits) {
                    Spacer(modifier = Modifier.height(2.dp))
                    VisitIndicatorDots(
                        count = visitCount,
                        isSelected = isSelected
                    )
                }
            }
        }
    }
}

/**
 * Dots indicating visits on a day.
 */
@Composable
private fun VisitIndicatorDots(
    count: Int,
    isSelected: Boolean
) {
    val maxDots = minOf(count, 3)
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        repeat(maxDots) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
            )
        }
    }
}

/**
 * Compact week row for use in week view.
 */
@Composable
fun WeekRow(
    weekDates: List<LocalDate>,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    hasVisitsOnDate: (LocalDate) -> Boolean = { false },
    modifier: Modifier = Modifier
) {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        weekDates.forEach { date ->
            WeekDayCell(
                date = date,
                isSelected = date == selectedDate,
                isToday = date == today,
                hasVisits = hasVisitsOnDate(date),
                onDateSelected = { onDateSelected(date) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Compact day cell for week view.
 */
@Composable
private fun WeekDayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    hasVisits: Boolean,
    onDateSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    else -> Color.Transparent
                }
            )
            .clickable { onDateSelected() }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Day of week
        Text(
            text = date.dayOfWeek.name.take(3).lowercase()
                .replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelSmall,
            color = when {
                isSelected -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Date number
        Surface(
            shape = CircleShape,
            color = when {
                isSelected -> Color.Transparent
                isToday -> MaterialTheme.colorScheme.primaryContainer
                else -> Color.Transparent
            },
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.onPrimary
                        isToday -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }

        // Visit indicator
        if (hasVisits) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
            )
        }
    }
}
