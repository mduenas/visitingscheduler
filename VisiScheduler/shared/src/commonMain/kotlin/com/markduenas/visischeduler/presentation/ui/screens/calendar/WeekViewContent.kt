package com.markduenas.visischeduler.presentation.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.markduenas.visischeduler.domain.entities.Visit
import com.markduenas.visischeduler.presentation.ui.components.calendar.CompactStatusBadge
import com.markduenas.visischeduler.presentation.ui.components.calendar.CompactVisitBlock
import com.markduenas.visischeduler.presentation.ui.components.calendar.WeekRow
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * 7-day week view with compact visit blocks.
 *
 * @param weekDates List of 7 dates for the week
 * @param selectedDate Currently selected date
 * @param visitsForWeek Map of date to visits
 * @param onDateSelected Callback when a date is selected
 * @param onVisitClick Callback when a visit is clicked
 * @param onSwipeLeft Callback for swiping left (next week)
 * @param onSwipeRight Callback for swiping right (previous week)
 * @param modifier Modifier for the component
 */
@Composable
fun WeekViewContent(
    weekDates: List<LocalDate>,
    selectedDate: LocalDate,
    visitsForWeek: Map<LocalDate, List<Visit>>,
    onDateSelected: (LocalDate) -> Unit,
    onVisitClick: (Visit) -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val swipeThreshold = 100f

    Column(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        when {
                            dragOffset < -swipeThreshold -> onSwipeLeft()
                            dragOffset > swipeThreshold -> onSwipeRight()
                        }
                        dragOffset = 0f
                    },
                    onDragCancel = { dragOffset = 0f },
                    onHorizontalDrag = { _, delta ->
                        dragOffset += delta
                    }
                )
            }
    ) {
        // Week date selector row
        WeekRow(
            weekDates = weekDates,
            selectedDate = selectedDate,
            onDateSelected = onDateSelected,
            hasVisitsOnDate = { date -> (visitsForWeek[date]?.size ?: 0) > 0 },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp)
        )

        HorizontalDivider()

        // Visits for selected date
        val visitsForSelectedDate = visitsForWeek[selectedDate] ?: emptyList()

        if (visitsForSelectedDate.isEmpty()) {
            EmptyDayState(
                date = selectedDate,
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    Text(
                        text = "${visitsForSelectedDate.size} visit${if (visitsForSelectedDate.size > 1) "s" else ""}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                items(visitsForSelectedDate) { visit ->
                    WeekViewVisitCard(
                        visit = visit,
                        onClick = { onVisitClick(visit) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp)) // Space for FAB
                }
            }
        }
    }
}

/**
 * Visit card for the week view.
 */
@Composable
private fun WeekViewVisitCard(
    visit: Visit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Time column
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(60.dp)
            ) {
                Text(
                    text = formatTime(visit.startTime.toString()),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = formatTime(visit.endTime.toString()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Vertical divider
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(40.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(1.dp)
                    )
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Visit details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = visit.purpose ?: "Visit",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Visitor count
                    if (visit.totalVisitorCount > 1) {
                        Text(
                            text = "${visit.totalVisitorCount} visitors",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    // Visit type badge
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = visit.visitType.name.replace("_", " "),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Status badge
            CompactStatusBadge(status = visit.status)
        }
    }
}

/**
 * Empty state when no visits for selected date.
 */
@Composable
private fun EmptyDayState(
    date: LocalDate,
    modifier: Modifier = Modifier
) {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val isToday = date == today

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isToday) "No visits today" else "No visits",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Tap + to schedule a new visit",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

/**
 * Week header showing the date range.
 */
@Composable
fun WeekHeader(
    weekDates: List<LocalDate>,
    modifier: Modifier = Modifier
) {
    val startDate = weekDates.firstOrNull()
    val endDate = weekDates.lastOrNull()

    if (startDate != null && endDate != null) {
        Text(
            text = formatDateRange(startDate, endDate),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier
        )
    }
}

/**
 * Format time for display.
 */
private fun formatTime(time: String): String {
    return time.take(5) // HH:mm
}

/**
 * Format date range for display.
 */
private fun formatDateRange(start: LocalDate, end: LocalDate): String {
    val startMonth = start.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    val endMonth = end.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }

    return if (start.month == end.month) {
        "$startMonth ${start.dayOfMonth} - ${end.dayOfMonth}, ${start.year}"
    } else {
        "$startMonth ${start.dayOfMonth} - $endMonth ${end.dayOfMonth}, ${start.year}"
    }
}
