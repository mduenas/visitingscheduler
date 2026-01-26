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
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRestrictionScreen(
    onNavigateBack: () -> Unit,
    onRestrictionAdded: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedType by remember { mutableStateOf<RestrictionType?>(null) }
    var title by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf<LocalTime?>(LocalTime(9, 0)) }
    var endTime by remember { mutableStateOf<LocalTime?>(LocalTime(17, 0)) }
    var selectedDays by remember { mutableStateOf(DayOfWeek.entries.toSet()) } // All days
    var maxVisitors by remember { mutableStateOf("3") }
    var dailyLimit by remember { mutableStateOf("6") }
    var minAge by remember { mutableStateOf("") }
    var isPermanent by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }

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
            // Restriction Type Selection
            Text(
                text = "Restriction Type",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedType == RestrictionType.TIME_BASED,
                    onClick = { selectedType = RestrictionType.TIME_BASED },
                    label = { Text("Time") },
                    leadingIcon = {
                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedType == RestrictionType.CAPACITY_BASED,
                    onClick = { selectedType = RestrictionType.CAPACITY_BASED },
                    label = { Text("Capacity") },
                    leadingIcon = {
                        Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedType == RestrictionType.VISITOR_BASED,
                    onClick = { selectedType = RestrictionType.VISITOR_BASED },
                    label = { Text("Visitor") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider()

            // Type-specific fields
            when (selectedType) {
                RestrictionType.TIME_BASED -> {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Restriction Name") },
                        placeholder = { Text("e.g., Quiet Hours, Meal Time") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Text(
                        text = "Time Range",
                        style = MaterialTheme.typography.titleSmall
                    )
                    TimeRangePicker(
                        startTime = startTime,
                        endTime = endTime,
                        onStartTimeChange = { startTime = it },
                        onEndTimeChange = { endTime = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Apply On Days",
                        style = MaterialTheme.typography.titleSmall
                    )
                    DayOfWeekSelector(
                        selectedDays = selectedDays,
                        onDayToggle = { day ->
                            selectedDays = if (day in selectedDays) {
                                selectedDays - day
                            } else {
                                selectedDays + day
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Block visits during this time")
                        Switch(
                            checked = true,
                            onCheckedChange = { }
                        )
                    }
                }

                RestrictionType.CAPACITY_BASED -> {
                    OutlinedTextField(
                        value = maxVisitors,
                        onValueChange = { if (it.all { c -> c.isDigit() }) maxVisitors = it },
                        label = { Text("Maximum Simultaneous Visitors") },
                        leadingIcon = { Icon(Icons.Default.People, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = dailyLimit,
                        onValueChange = { if (it.all { c -> c.isDigit() }) dailyLimit = it },
                        label = { Text("Maximum Visits Per Day") },
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                "New visit requests that would exceed these limits will require manual approval.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                RestrictionType.VISITOR_BASED -> {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Restriction Name") },
                        placeholder = { Text("e.g., Age Requirement, Health Screening") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = minAge,
                        onValueChange = { if (it.all { c -> c.isDigit() }) minAge = it },
                        label = { Text("Minimum Age (Optional)") },
                        leadingIcon = { Icon(Icons.Default.Cake, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Health Screening Required")
                            Text(
                                "Visitors must confirm health status",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = false,
                            onCheckedChange = { }
                        )
                    }
                }

                null, RestrictionType.BENEFICIARY_BASED, RestrictionType.RELATIONSHIP_BASED -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.TouchApp,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Select a restriction type above",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (selectedType != null) {
                HorizontalDivider()

                // Duration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Permanent Restriction")
                    Switch(
                        checked = isPermanent,
                        onCheckedChange = { isPermanent = it }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Save Button
                Button(
                    onClick = {
                        isLoading = true
                        // TODO: Save restriction
                        onRestrictionAdded()
                    },
                    enabled = selectedType != null && !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) {
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
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
