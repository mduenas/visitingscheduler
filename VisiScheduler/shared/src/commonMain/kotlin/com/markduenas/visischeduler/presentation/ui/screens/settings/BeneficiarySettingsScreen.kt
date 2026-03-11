package com.markduenas.visischeduler.presentation.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.markduenas.visischeduler.presentation.viewmodel.settings.BeneficiarySettingsViewModel
import com.markduenas.visischeduler.presentation.viewmodel.settings.VisitingHours
import kotlinx.datetime.DayOfWeek
import org.koin.compose.koinInject

/**
 * Screen for managing beneficiary-specific visiting rules and settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeneficiarySettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: BeneficiarySettingsViewModel = koinInject(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Visit Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(Icons.Default.RestartAlt, contentDescription = "Reset to defaults")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // General Section
                item {
                    Text(
                        text = "General Constraints",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumericSettingRow(
                            label = "Default Duration (min)",
                            value = uiState.defaultDuration,
                            onValueChange = { viewModel.setDefaultDuration(it) }
                        )
                        NumericSettingRow(
                            label = "Max Visitors per Slot",
                            value = uiState.maxVisitorsPerSlot,
                            onValueChange = { viewModel.setMaxVisitorsPerSlot(it) }
                        )
                        NumericSettingRow(
                            label = "Max Visits per Day",
                            value = uiState.maxVisitsPerDay,
                            onValueChange = { viewModel.setMaxVisitsPerDay(it) }
                        )
                        NumericSettingRow(
                            label = "Buffer Between Visits (min)",
                            value = uiState.bufferTimeBetweenVisits,
                            onValueChange = { viewModel.setBufferTime(it) }
                        )
                    }
                }

                // Auto-Approve Section
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Automation",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Auto-Approve Requests", style = MaterialTheme.typography.bodyLarge)
                                Switch(
                                    checked = uiState.autoApproveSettings.enabled,
                                    onCheckedChange = { viewModel.toggleAutoApprove(it) }
                                )
                            }
                            
                            if (uiState.autoApproveSettings.enabled) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Approved Visitors Only", style = MaterialTheme.typography.bodyMedium)
                                    Checkbox(
                                        checked = uiState.autoApproveSettings.approvedVisitorsOnly,
                                        onCheckedChange = { viewModel.toggleAutoApproveApprovedOnly(it) }
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Require Photo ID", style = MaterialTheme.typography.bodyMedium)
                                    Checkbox(
                                        checked = uiState.autoApproveSettings.requirePhotoId,
                                        onCheckedChange = { viewModel.toggleRequirePhotoId(it) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Visiting Hours Section
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Visiting Hours",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(uiState.visitingHours) { hours ->
                    VisitingHoursDayItem(
                        hours = hours,
                        onToggle = { enabled -> viewModel.toggleDayEnabled(hours.dayOfWeek, enabled) },
                        onStartTimeChange = { time -> viewModel.setStartTime(hours.dayOfWeek, time) },
                        onEndTimeChange = { time -> viewModel.setEndTime(hours.dayOfWeek, time) }
                    )
                }

                // Special Instructions
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Special Instructions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    OutlinedTextField(
                        value = uiState.specialInstructions,
                        onValueChange = { viewModel.setSpecialInstructions(it) },
                        label = { Text("Instructions for all visitors") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 10
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.saveSettings() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isSaving
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text("Save All Settings")
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Settings?") },
            text = { Text("This will restore all visiting rules and hours to their default values.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        viewModel.resetToDefaults()
                    }
                ) {
                    Text("Reset", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun NumericSettingRow(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            IconButton(onClick = { if (value > 0) onValueChange(value - 1) }) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease")
            }
            Text(value.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            IconButton(onClick = { onValueChange(value + 1) }) {
                Icon(Icons.Default.Add, contentDescription = "Increase")
            }
        }
    }
}

@Composable
private fun VisitingHoursDayItem(
    hours: VisitingHours,
    onToggle: (Boolean) -> Unit,
    onStartTimeChange: (String) -> Unit,
    onEndTimeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = hours.dayOfWeek.name.lowercase().capitalize(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Switch(checked = hours.isEnabled, onCheckedChange = onToggle)
            }
            
            if (hours.isEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = hours.startTime,
                        onValueChange = onStartTimeChange,
                        label = { Text("From") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = hours.endTime,
                        onValueChange = onEndTimeChange,
                        label = { Text("To") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }
        }
    }
}

private fun String.capitalize() = this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
