package com.markduenas.visischeduler.presentation.ui.screens.restrictions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.markduenas.visischeduler.domain.entities.RestrictionType
import com.markduenas.visischeduler.presentation.ui.components.visitors.TimeRangePicker
import com.markduenas.visischeduler.presentation.ui.components.visitors.DayOfWeekSelector
import com.markduenas.visischeduler.presentation.viewmodel.visitors.AddRestrictionViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRestrictionScreen(
    onNavigateBack: () -> Unit,
    onRestrictionAdded: () -> Unit,
    viewModel: AddRestrictionViewModel = koinInject(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onRestrictionAdded()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Restriction") },
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Basic Info
            OutlinedTextField(
                value = uiState.name,
                onValueChange = { viewModel.onNameChange(it) },
                label = { Text("Name *") },
                isError = uiState.validationErrors.containsKey("name"),
                supportingText = uiState.validationErrors["name"]?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.description,
                onValueChange = { viewModel.onDescriptionChange(it) },
                label = { Text("Description *") },
                isError = uiState.validationErrors.containsKey("description"),
                supportingText = uiState.validationErrors["description"]?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            HorizontalDivider()

            // Restriction Type Selection
            Text(
                text = "Restriction Type",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RestrictionType.entries.forEach { type ->
                    FilterChip(
                        selected = uiState.restrictionType == type,
                        onClick = { viewModel.onRestrictionTypeChange(type) },
                        label = { Text(type.name.replace("_", " ").lowercase().capitalize()) },
                        leadingIcon = {
                            val icon = when(type) {
                                RestrictionType.TIME_BASED -> Icons.Default.Schedule
                                RestrictionType.VISITOR_BASED -> Icons.Default.Person
                                RestrictionType.CAPACITY_BASED -> Icons.Default.Groups
                                RestrictionType.BENEFICIARY_BASED -> Icons.Default.Person
                                RestrictionType.RELATIONSHIP_BASED -> Icons.Default.FamilyRestroom
                                RestrictionType.COMBINED -> Icons.Default.Tune
                            }
                            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                }
            }

            HorizontalDivider()

            // Type-specific fields
            when (uiState.restrictionType) {
                RestrictionType.TIME_BASED -> {
                    Text(
                        text = "Time Constraints",
                        style = MaterialTheme.typography.titleSmall
                    )
                    
                    TimeRangePicker(
                        startTime = uiState.earliestStartTime,
                        endTime = uiState.latestEndTime,
                        onStartTimeChange = { viewModel.onEarliestStartTimeChange(it) },
                        onEndTimeChange = { viewModel.onLatestEndTimeChange(it) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    DayOfWeekSelector(
                        selectedDays = uiState.selectedDays,
                        onDayToggle = { viewModel.onDayToggle(it) },
                        label = "Allowed Days",
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = uiState.maxDurationMinutes?.toString() ?: "",
                        onValueChange = { viewModel.onMaxDurationChange(it.toIntOrNull()) },
                        label = { Text("Max Duration (minutes)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                RestrictionType.CAPACITY_BASED -> {
                    OutlinedTextField(
                        value = uiState.maxSimultaneousVisitors?.toString() ?: "",
                        onValueChange = { viewModel.onMaxSimultaneousVisitorsChange(it.toIntOrNull()) },
                        label = { Text("Max Simultaneous Visitors") },
                        leadingIcon = { Icon(Icons.Default.People, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = uiState.maxDailyVisits?.toString() ?: "",
                        onValueChange = { viewModel.onMaxDailyVisitsChange(it.toIntOrNull()) },
                        label = { Text("Max Visits Per Day") },
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                RestrictionType.VISITOR_BASED -> {
                    OutlinedTextField(
                        value = uiState.maxVisitsPerWeek?.toString() ?: "",
                        onValueChange = { viewModel.onMaxVisitsPerWeekChange(it.toIntOrNull()) },
                        label = { Text("Max Visits Per Week") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Requires Escort")
                        Switch(
                            checked = uiState.requiresEscort,
                            onCheckedChange = { viewModel.onRequiresEscortToggle(it) }
                        )
                    }
                }
                
                else -> { /* Other types */ }
            }

            HorizontalDivider()

            // Date Range
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Permanent Restriction")
                Switch(
                    checked = uiState.isPermanent,
                    onCheckedChange = { viewModel.onPermanentToggle(it) }
                )
            }

            if (!uiState.isPermanent) {
                // Show date pickers
                Text("Effective Dates", style = MaterialTheme.typography.titleSmall)
                // Simplified date display for now
                Text("Starts: ${uiState.startDate}")
                uiState.endDate?.let { Text("Ends: $it") }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save Button
            Button(
                onClick = { viewModel.saveRestriction() },
                enabled = uiState.isValid && !uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Restriction")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Error handling
    uiState.error?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Error") },
            text = { Text(error.message ?: "An unknown error occurred") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }
}

// Extension to capitalize first letter
private fun String.capitalize() = this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
