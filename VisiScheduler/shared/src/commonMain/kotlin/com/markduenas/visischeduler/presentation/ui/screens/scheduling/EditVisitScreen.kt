package com.markduenas.visischeduler.presentation.ui.screens.scheduling

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.markduenas.visischeduler.domain.entities.VisitType
import com.markduenas.visischeduler.presentation.ui.components.calendar.CalendarGrid
import com.markduenas.visischeduler.presentation.ui.components.calendar.DurationSelector
import com.markduenas.visischeduler.presentation.viewmodel.scheduling.EditVisitViewModel
import com.markduenas.visischeduler.presentation.viewmodel.scheduling.VisitDuration
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import kotlinx.datetime.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditVisitScreen(
    visitId: String,
    onNavigateBack: () -> Unit,
    viewModel: EditVisitViewModel = koinInject(parameters = { parametersOf(visitId) }),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    val monthDates = remember(uiState.selectedDate) {
        generateMonthDates(uiState.selectedDate.year, uiState.selectedDate.month.ordinal + 1)
    }

    var showConfirmation by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Visit") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

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
                text = "Date",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            CalendarGrid(
                monthDates = monthDates,
                selectedDate = uiState.selectedDate,
                onDateSelected = { viewModel.selectDate(it) },
                modifier = Modifier.fillMaxWidth()
            )

            // Start Time
            Text(
                text = "Start Time",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            OutlinedTextField(
                value = uiState.selectedStartTime?.let {
                    "${it.hour.toString().padStart(2, '0')}:${it.minute.toString().padStart(2, '0')}"
                } ?: "",
                onValueChange = { input ->
                    val parts = input.split(":")
                    if (parts.size == 2) {
                        val h = parts[0].toIntOrNull()
                        val m = parts[1].toIntOrNull()
                        if (h != null && m != null && h in 0..23 && m in 0..59) {
                            viewModel.setStartTime(LocalTime(h, m))
                        }
                    }
                },
                label = { Text("HH:MM (24-hour)") },
                placeholder = { Text("09:00") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) }
            )

            // Duration
            Text(
                text = "Duration",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            DurationSelector(
                selectedDuration = uiState.selectedDuration,
                onDurationSelected = { viewModel.setDuration(it) },
                modifier = Modifier.fillMaxWidth()
            )

            // Visit Type
            Text(
                text = "Visit Type",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val types = listOf(
                    VisitType.IN_PERSON to "In Person",
                    VisitType.VIDEO_CALL to "Video Call"
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
            if (uiState.visitType == VisitType.VIDEO_CALL) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Video Call Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedTextField(
                        value = uiState.videoCallLink ?: "",
                        onValueChange = { viewModel.setVideoCallLink(it.takeIf { s -> s.isNotBlank() }) },
                        label = { Text("Meeting Link (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("https://zoom.us/j/...") }
                    )
                    OutlinedTextField(
                        value = uiState.videoCallPlatform ?: "",
                        onValueChange = { viewModel.setVideoCallPlatform(it.takeIf { s -> s.isNotBlank() }) },
                        label = { Text("Platform (e.g. Zoom, Teams)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Guest Count
            Text(
                text = "Number of Guests",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
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

            // Notes
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = { viewModel.setNotes(it) },
                label = { Text("Notes (Optional)") },
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

            // Save Button
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
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Changes")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Confirmation Dialog
    if (showConfirmation) {
        AlertDialog(
            onDismissRequest = { showConfirmation = false },
            title = { Text("Confirm Changes") },
            text = {
                Column {
                    Text("Date: ${uiState.selectedDate}")
                    uiState.selectedStartTime?.let { Text("Start: $it") }
                    uiState.selectedEndTime?.let { Text("End: $it") }
                    Text("Duration: ${uiState.selectedDuration.displayName}")
                    Text("Total Visitors: ${uiState.additionalVisitors.size + 1}")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmation = false
                        viewModel.saveChanges()
                    }
                ) {
                    Text("Save")
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
    val startDayOfWeek = firstOfMonth.dayOfWeek.ordinal
    val dates = mutableListOf<LocalDate?>()
    repeat(startDayOfWeek) { dates.add(null) }
    for (day in 1..daysInMonth) {
        dates.add(LocalDate(year, month, day))
    }
    return dates
}
