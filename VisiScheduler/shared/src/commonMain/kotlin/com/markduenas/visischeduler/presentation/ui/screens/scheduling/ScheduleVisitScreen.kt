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
import com.markduenas.visischeduler.presentation.viewmodel.scheduling.ScheduleVisitViewModel
import com.markduenas.visischeduler.presentation.viewmodel.scheduling.VisitDuration
import com.markduenas.visischeduler.domain.usecase.ScheduleVisitException
import org.koin.compose.koinInject
import kotlin.time.Clock
import kotlinx.datetime.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleVisitScreen(
    beneficiaryId: String,
    onNavigateBack: () -> Unit,
    onVisitScheduled: (String) -> Unit,
    viewModel: ScheduleVisitViewModel = koinInject(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // Initialize ViewModel with beneficiary
    LaunchedEffect(beneficiaryId) {
        viewModel.setBeneficiary(beneficiaryId, "Beneficiary") // Name would ideally be fetched
    }

    // Generate month dates for calendar
    val monthDates = remember(uiState.selectedDate) {
        generateMonthDates(uiState.selectedDate.year, uiState.selectedDate.monthNumber)
    }

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
            // Loading Indicator for Slots
            if (uiState.isLoadingSlots) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // Date Selection
            Text(
                text = "Select Date",
                style = MaterialTheme.typography.titleMedium
            )
            CalendarGrid(
                monthDates = monthDates,
                selectedDate = uiState.selectedDate,
                onDateSelected = { viewModel.selectDate(it) },
                modifier = Modifier.fillMaxWidth()
            )

            // Time Slot Selection
            Text(
                text = "Select Time Slot",
                style = MaterialTheme.typography.titleMedium
            )
            TimeSlotPicker(
                slots = uiState.availableSlots,
                selectedSlot = uiState.selectedTimeSlot,
                onSlotSelected = { viewModel.selectTimeSlot(it) },
                modifier = Modifier.fillMaxWidth()
            )

            // Duration Selection
            Text(
                text = \"Duration\",
                style = MaterialTheme.typography.titleMedium
            )
            DurationSelector(
                selectedDuration = uiState.selectedDuration,
                onDurationSelected = { viewModel.setDuration(it) },
                modifier = Modifier.fillMaxWidth()
            )

            // Visit Type Selection
            Text(
                text = \"Visit Type\",
                style = MaterialTheme.typography.titleMedium
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val types = listOf(
                    com.markduenas.visischeduler.domain.entities.VisitType.IN_PERSON to \"In Person\",
                    com.markduenas.visischeduler.domain.entities.VisitType.VIDEO_CALL to \"Video Call\"
                )
                types.forEachIndexed { index, (type, label) ->
                    SegmentedButton(
                        selected = uiState.visitType == type,
                        onClick = { viewModel.setVisitType(type) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = types.size)
                    ) {
                        Text(label)
                    }
                }
            }

            // Video Call Details (Conditional)
            if (uiState.visitType == com.markduenas.visischeduler.domain.entities.VisitType.VIDEO_CALL) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = \"Video Call Details\",
                        style = MaterialTheme.typography.titleMedium
                    )
                    OutlinedTextField(
                        value = uiState.videoCallLink ?: \"\",
                        onValueChange = { viewModel.setVideoCallLink(it) },
                        label = { Text(\"Meeting Link (Optional)\") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(\"https://zoom.us/j/...\") }
                    )
                    OutlinedTextField(
                        value = uiState.videoCallPlatform ?: \"\",
                        onValueChange = { viewModel.setVideoCallPlatform(it) },
                        label = { Text(\"Platform (e.g. Zoom, Teams)\") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

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
                    onClick = { viewModel.decrementGuestCount() },
                    enabled = uiState.additionalVisitors.isNotEmpty()
                ) {
                    Text("-")
                }
                Text(
                    text = (uiState.additionalVisitors.size + 1).toString(),
                    style = MaterialTheme.typography.headlineMedium
                )
                FilledTonalIconButton(
                    onClick = { viewModel.incrementGuestCount() },
                    enabled = uiState.additionalVisitors.size < 5
                ) {
                    Text("+")
                }
            }

            // Reason
            OutlinedTextField(
                value = uiState.reason,
                onValueChange = { viewModel.setReason(it) },
                label = { Text("Reason for Visit (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            // Error Display
            uiState.error?.let { error ->
                Text(
                    text = error.message ?: "An error occurred",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Submit Button
            Button(
                onClick = { showConfirmation = true },
                enabled = uiState.canSubmit,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isSubmitting) {
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
                    Text("Date: ${uiState.selectedDate}")
                    Text("Time: ${uiState.selectedStartTime} - ${uiState.selectedEndTime}")
                    Text("Duration: ${uiState.selectedDuration.displayName}")
                    Text("Total Visitors: ${uiState.additionalVisitors.size + 1}")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmation = false
                        viewModel.submitVisitRequest()
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

    // Start of week (Sunday = 0, Monday = 1... Saturday = 6)
    // Adjust based on platform/locale if needed
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
