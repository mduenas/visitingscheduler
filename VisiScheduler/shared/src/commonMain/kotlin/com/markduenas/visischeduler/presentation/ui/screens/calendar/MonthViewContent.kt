package com.markduenas.visischeduler.presentation.ui.screens.calendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.markduenas.visischeduler.domain.entities.Visit
import com.markduenas.visischeduler.presentation.ui.components.calendar.CalendarGrid
import com.markduenas.visischeduler.presentation.ui.components.calendar.CompactStatusBadge
import com.markduenas.visischeduler.presentation.ui.components.calendar.MonthHeader
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Traditional month calendar view with visit indicators.
 *
 * @param monthDates List of dates for the month (null for padding)
 * @param monthStart First day of the current month
 * @param selectedDate Currently selected date
 * @param visitsForMonth Map of date to visits
 * @param onDateSelected Callback when a date is selected
 * @param onVisitClick Callback when a visit is clicked
 * @param onPrevious Callback for previous month
 * @param onNext Callback for next month
 * @param onTodayClick Callback for today button
 * @param modifier Modifier for the component
 */
@Composable
fun MonthViewContent(
    monthDates: List<LocalDate?>,
    monthStart: LocalDate,
    selectedDate: LocalDate,
    visitsForMonth: Map<LocalDate, List<Visit>>,
    onDateSelected: (LocalDate) -> Unit,
    onVisitClick: (Visit) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onTodayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Month header with navigation
        MonthHeader(
            monthStart = monthStart,
            onPrevious = onPrevious,
            onNext = onNext,
            onTodayClick = onTodayClick
        )

        // Calendar grid
        CalendarGrid(
            monthDates = monthDates,
            selectedDate = selectedDate,
            onDateSelected = onDateSelected,
            hasVisitsOnDate = { date -> (visitsForMonth[date]?.size ?: 0) > 0 },
            getVisitCountForDate = { date -> visitsForMonth[date]?.size ?: 0 },
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        HorizontalDivider()

        // Selected date visits
        SelectedDateVisitsList(
            date = selectedDate,
            visits = visitsForMonth[selectedDate] ?: emptyList(),
            onVisitClick = onVisitClick,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * List of visits for the selected date.
 */
@Composable
private fun SelectedDateVisitsList(
    date: LocalDate,
    visits: List<Visit>,
    onVisitClick: (Visit) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    Column(modifier = modifier.fillMaxWidth()) {
        // Date header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = formatSelectedDate(date),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (date == today) {
                    Text(
                        text = "Today",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = "${visits.size} visit${if (visits.size != 1) "s" else ""}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (visits.isEmpty()) {
            MonthViewEmptyState(
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 80.dp // Space for FAB
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(visits.sortedBy { it.startTime }) { visit ->
                    MonthViewVisitItem(
                        visit = visit,
                        onClick = { onVisitClick(visit) }
                    )
                }
            }
        }
    }
}

/**
 * Visit item for the month view list.
 */
@Composable
private fun MonthViewVisitItem(
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
            // Time
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = formatTime(visit.startTime.toString()),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "-",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatTime(visit.endTime.toString()),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.padding(horizontal = 12.dp))

            // Visit info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = visit.purpose ?: "Visit",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = visit.visitType.name.replace("_", " "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Status
            CompactStatusBadge(status = visit.status)
        }
    }
}

/**
 * Empty state for month view.
 */
@Composable
private fun MonthViewEmptyState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No visits scheduled",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Select a date and tap + to add one",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

/**
 * Format selected date for display.
 */
private fun formatSelectedDate(date: LocalDate): String {
    val dayOfWeek = date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
    val month = date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    return "$dayOfWeek, $month ${date.dayOfMonth}"
}

/**
 * Format time for display.
 */
private fun formatTime(time: String): String {
    return time.take(5) // HH:mm
}
