package com.markduenas.visischeduler.presentation.ui.screens.scheduling

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.markduenas.visischeduler.domain.entities.TimeSlot
import com.markduenas.visischeduler.presentation.ui.components.calendar.CalendarGrid
import com.markduenas.visischeduler.presentation.ui.components.calendar.TimeSlotPicker
import com.markduenas.visischeduler.presentation.ui.components.calendar.DurationSelector
import com.markduenas.visischeduler.presentation.viewmodel.scheduling.VisitDuration
import kotlin.time.Clock
import kotlinx.datetime.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleVisitScreen(
    beneficiaryId: String,
    onNavigateBack: () -> Unit,
    onVisitScheduled: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    var selectedDate by remember { mutableStateOf(today) }
    var selectedTimeSlot by remember { mutableStateOf<TimeSlot?>(null) }
    var selectedDuration by remember { mutableStateOf(VisitDuration.THIRTY_MINUTES) }

    // Generate month dates for calendar
    val monthDates = remember(selectedDate) {
        generateMonthDates(selectedDate.year, selectedDate.monthNumber)
    }

    // Sample time slots - in a real app these would come from a ViewModel
    val availableSlots = remember { emptyList<TimeSlot>() }
    var reason by remember { mutableStateOf("") }
    var guestCount by remember { mutableStateOf(1) }
    var isLoading by remember { mutableStateOf(false) }
    var showConfirmation by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Schedule Visit") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Date Selection
            Text(
                text = "Select Date",
                style = MaterialTheme.typography.titleMedium
            )
            CalendarGrid(
                monthDates = monthDates,
                selectedDate = selectedDate,
                onDateSelected = { selectedDate = it },
                modifier = Modifier.fillMaxWidth()
            )

            // Time Slot Selection
            Text(
                text = "Select Time",
                style = MaterialTheme.typography.titleMedium
            )
            TimeSlotPicker(
                slots = availableSlots,
                selectedSlot = selectedTimeSlot,
                onSlotSelected = { selectedTimeSlot = it },
                modifier = Modifier.fillMaxWidth()
            )

            // Duration Selection
            Text(
                text = "Duration",
                style = MaterialTheme.typography.titleMedium
            )
            DurationSelector(
                selectedDuration = selectedDuration,
                onDurationSelected = { selectedDuration = it },
                modifier = Modifier.fillMaxWidth()
            )

            // Guest Count
            Text(
                text = "Number of Guests",
                style = MaterialTheme.typography.titleMedium
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FilledTonalIconButton(
                    onClick = { if (guestCount > 1) guestCount-- },
                    enabled = guestCount > 1
                ) {
                    Text("-")
                }
                Text(
                    text = guestCount.toString(),
                    style = MaterialTheme.typography.headlineMedium
                )
                FilledTonalIconButton(
                    onClick = { if (guestCount < 5) guestCount++ },
                    enabled = guestCount < 5
                ) {
                    Text("+")
                }
            }

            // Reason (Optional)
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text("Reason for Visit (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Submit Button
            Button(
                onClick = { showConfirmation = true },
                enabled = selectedTimeSlot != null && !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Default.Schedule, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Request Visit")
                }
            }
        }
    }

    // Confirmation Dialog
    if (showConfirmation) {
        AlertDialog(
            onDismissRequest = { showConfirmation = false },
            title = { Text("Confirm Visit Request") },
            text = {
                Column {
                    Text("Date: $selectedDate")
                    Text("Time: ${selectedTimeSlot?.let { "${it.startTime} - ${it.endTime}" } ?: "Not selected"}")
                    Text("Duration: ${selectedDuration.displayName}")
                    Text("Guests: $guestCount")
                    if (reason.isNotBlank()) {
                        Text("Reason: $reason")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isLoading = true
                        showConfirmation = false
                        // TODO: Call ViewModel to schedule visit
                        onVisitScheduled("visit_id")
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Generates a list of dates for a month calendar grid.
 * Includes padding nulls for alignment.
 */
private fun generateMonthDates(year: Int, month: Int): List<LocalDate?> {
    val firstOfMonth = LocalDate(year, month, 1)
    val daysInMonth = when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
        else -> 30
    }

    // Start of week (Sunday = 0)
    val startDayOfWeek = firstOfMonth.dayOfWeek.ordinal

    val dates = mutableListOf<LocalDate?>()

    // Padding for days before first of month
    repeat(startDayOfWeek) {
        dates.add(null)
    }

    // Actual days of month
    for (day in 1..daysInMonth) {
        dates.add(LocalDate(year, month, day))
    }

    return dates
}
