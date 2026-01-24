package com.markduenas.visischeduler.presentation.ui.screens.scheduling

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
import com.markduenas.visischeduler.domain.entities.VisitStatus
import com.markduenas.visischeduler.presentation.ui.components.calendar.VisitStatusBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitDetailsScreen(
    visitId: String,
    onNavigateBack: () -> Unit,
    onEditVisit: (String) -> Unit,
    onCheckIn: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Mock data - would come from ViewModel
    var showCancelDialog by remember { mutableStateOf(false) }
    var showApproveDialog by remember { mutableStateOf(false) }
    val visitStatus = VisitStatus.APPROVED
    val isCoordinator = true // From user role

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Visit Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (visitStatus == VisitStatus.PENDING || visitStatus == VisitStatus.APPROVED) {
                        IconButton(onClick = { onEditVisit(visitId) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Visit #${visitId.take(8)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                VisitStatusBadge(status = visitStatus)
            }

            HorizontalDivider()

            // Date & Time Section
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Date", style = MaterialTheme.typography.labelMedium)
                            Text("January 25, 2026", style = MaterialTheme.typography.bodyLarge)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Time", style = MaterialTheme.typography.labelMedium)
                            Text("2:00 PM - 3:00 PM (1 hour)", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            // Visitor Info Section
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Visitor Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("John Smith", style = MaterialTheme.typography.bodyLarge)
                            Text("Family - Son", style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Group,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("2 guests total", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            // Reason Section
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Reason for Visit",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Regular weekly visit to check in and bring supplies.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Notes Section
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Notes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Please use entrance B. Parking available in Lot 3.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            when (visitStatus) {
                VisitStatus.PENDING -> {
                    if (isCoordinator) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showCancelDialog = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Deny")
                            }
                            Button(
                                onClick = { showApproveDialog = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Approve")
                            }
                        }
                    }
                }
                VisitStatus.APPROVED -> {
                    Button(
                        onClick = { onCheckIn(visitId) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Check In")
                    }
                    OutlinedButton(
                        onClick = { showCancelDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Cancel, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cancel Visit")
                    }
                }
                else -> { /* No actions for other statuses */ }
            }
        }
    }

    // Cancel Dialog
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel Visit?") },
            text = { Text("Are you sure you want to cancel this visit? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelDialog = false
                        // TODO: Cancel visit
                    }
                ) {
                    Text("Cancel Visit", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Keep Visit")
                }
            }
        )
    }

    // Approve Dialog
    if (showApproveDialog) {
        AlertDialog(
            onDismissRequest = { showApproveDialog = false },
            title = { Text("Approve Visit?") },
            text = { Text("This will confirm the visit and notify the visitor.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showApproveDialog = false
                        // TODO: Approve visit
                    }
                ) {
                    Text("Approve")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApproveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
