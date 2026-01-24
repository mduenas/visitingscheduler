package com.markduenas.visischeduler.presentation.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.markduenas.visischeduler.domain.entities.Visit
import com.markduenas.visischeduler.presentation.ui.components.calendar.CompactDateHeader
import com.markduenas.visischeduler.presentation.ui.components.calendar.VisitBlock
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Hour-by-hour timeline view for a single day.
 *
 * @param date The date being displayed
 * @param visits List of visits for this date
 * @param onVisitClick Callback when a visit is clicked
 * @param onTimeSlotClick Callback when an empty time slot is clicked
 * @param onPrevious Callback for previous day
 * @param onNext Callback for next day
 * @param modifier Modifier for the component
 */
@Composable
fun DayViewContent(
    date: LocalDate,
    visits: List<Visit>,
    onVisitClick: (Visit) -> Unit,
    onTimeSlotClick: (LocalTime) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    startHour: Int = 6,
    endHour: Int = 22
) {
    val hourHeight = 60.dp
    val timeColumnWidth = 56.dp

    Column(modifier = modifier.fillMaxSize()) {
        // Date header with navigation
        CompactDateHeader(
            date = date,
            onPrevious = onPrevious,
            onNext = onNext,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        HorizontalDivider()

        // Timeline
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(
                items = (startHour..endHour).toList()
            ) { index, hour ->
                HourRow(
                    hour = hour,
                    hourHeight = hourHeight,
                    timeColumnWidth = timeColumnWidth,
                    visits = getVisitsForHour(visits, hour),
                    isCurrentHour = isCurrentHour(date, hour),
                    currentMinuteOffset = getCurrentMinuteOffset(date, hour),
                    onVisitClick = onVisitClick,
                    onTimeSlotClick = { onTimeSlotClick(LocalTime(hour, 0)) }
                )
            }
        }
    }
}

/**
 * Single hour row in the day view.
 */
@Composable
private fun HourRow(
    hour: Int,
    hourHeight: Dp,
    timeColumnWidth: Dp,
    visits: List<Visit>,
    isCurrentHour: Boolean,
    currentMinuteOffset: Float?,
    onVisitClick: (Visit) -> Unit,
    onTimeSlotClick: () -> Unit
) {
    val currentTimeLineColor = MaterialTheme.colorScheme.error

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(hourHeight)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Time column
            Box(
                modifier = Modifier
                    .width(timeColumnWidth)
                    .fillMaxHeight()
                    .padding(end = 8.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                Text(
                    text = formatHour(hour),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.offset(y = (-6).dp)
                )
            }

            // Content area with divider line
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .drawBehind {
                        // Hour line
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.5f),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = 1f
                        )
                    }
                    .clickable { onTimeSlotClick() }
                    .padding(horizontal = 8.dp)
            ) {
                // Visits in this hour
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    visits.forEach { visit ->
                        val startMinute = visit.startTime.minute
                        val durationMinutes = calculateDuration(visit.startTime, visit.endTime)
                        val heightFraction = (durationMinutes.toFloat() / 60f).coerceAtMost(1f)

                        Box(
                            modifier = Modifier
                                .offset(y = (startMinute * hourHeight.value / 60).dp)
                        ) {
                            VisitBlock(
                                visit = visit,
                                onClick = { onVisitClick(visit) },
                                height = (hourHeight.value * heightFraction).dp,
                                modifier = Modifier.fillMaxWidth(0.95f)
                            )
                        }
                    }
                }
            }
        }

        // Current time indicator
        if (isCurrentHour && currentMinuteOffset != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (currentMinuteOffset * hourHeight.value).dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(timeColumnWidth - 4.dp))
                    Box(
                        modifier = Modifier
                            .width(8.dp)
                            .height(8.dp)
                            .background(
                                currentTimeLineColor,
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .background(currentTimeLineColor)
                    )
                }
            }
        }
    }
}

/**
 * Get visits that start within a specific hour.
 */
private fun getVisitsForHour(visits: List<Visit>, hour: Int): List<Visit> {
    return visits.filter { visit ->
        visit.startTime.hour == hour
    }
}

/**
 * Check if the given hour is the current hour on the given date.
 */
private fun isCurrentHour(date: LocalDate, hour: Int): Boolean {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    return now.date == date && now.hour == hour
}

/**
 * Get the minute offset (0-1) for the current time indicator.
 */
private fun getCurrentMinuteOffset(date: LocalDate, hour: Int): Float? {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    return if (now.date == date && now.hour == hour) {
        now.minute.toFloat() / 60f
    } else {
        null
    }
}

/**
 * Format hour for display.
 */
private fun formatHour(hour: Int): String {
    return when {
        hour == 0 -> "12 AM"
        hour < 12 -> "$hour AM"
        hour == 12 -> "12 PM"
        else -> "${hour - 12} PM"
    }
}

/**
 * Calculate duration in minutes between two times.
 */
private fun calculateDuration(startTime: LocalTime, endTime: LocalTime): Int {
    val startMinutes = startTime.hour * 60 + startTime.minute
    val endMinutes = endTime.hour * 60 + endTime.minute
    return endMinutes - startMinutes
}

/**
 * Empty day view when there are no visits.
 */
@Composable
fun EmptyDayView(
    date: LocalDate,
    onScheduleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "No visits scheduled",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Tap the + button to schedule a visit",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.weight(1f))
    }
}
