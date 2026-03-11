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
import com.markduenas.visischeduler.domain.entities.VisitType
import com.markduenas.visischeduler.presentation.ui.components.calendar.VisitStatusBadge
import com.markduenas.visischeduler.presentation.viewmodel.scheduling.VisitDetailsViewModel
import org.koin.compose.koinInject
import kotlinx.datetime.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitDetailsScreen(
    visitId: String,
    onNavigateBack: () -> Unit,
    onEditVisit: (String) -> Unit,
    viewModel: VisitDetailsViewModel = koinInject(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(visitId) {
        viewModel.loadVisit(visitId)
    }

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
                    if (uiState.canEdit) {
                        IconButton(onClick = { onEditVisit(visitId) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.visit == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Visit not found")
            }
        } else {
            val visit = uiState.visit!!
            
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
                        text = "Visit #${visit.id.take(8)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    VisitStatusBadge(status = visit.status)
                }

                HorizontalDivider()

                // Date & Time Section
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Date", style = MaterialTheme.typography.labelMedium)
                                Text(formatDate(visit.scheduledDate), style = MaterialTheme.typography.bodyLarge)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Time", style = MaterialTheme.typography.labelMedium)
                                Text("${formatTime(visit.startTime)} - ${formatTime(visit.endTime)}", style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }

                // Visit Type Section (Virtual Details)
                if (visit.visitType == VisitType.VIDEO_CALL) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.VideoCall, contentDescription = null)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Virtual Visit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    visit.videoCallPlatform?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                                }
                            }
                            
                            visit.videoCallLink?.let { link ->
                                Button(
                                    onClick = { /* Handle opening link */ },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.Launch, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Join Call")
                                }
                            } ?: Text("Link will be available before the call starts", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // Visitor Info Section
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Visitor Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(visit.visitorName, style = MaterialTheme.typography.bodyLarge)
                                // relationship could be added to Visit entity if available
                            }
                        }

                        if (visit.additionalVisitors.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Group, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("${visit.additionalVisitors.size + 1} visitors total", style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }

                // Reason & Notes
                if (!visit.purpose.isNullOrBlank()) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Reason for Visit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(visit.purpose!!, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                if (!visit.notes.isNullOrBlank()) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Notes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(visit.notes!!, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action Buttons
                ActionButtons(
                    uiState = uiState,
                    onCheckIn = { viewModel.checkIn() },
                    onCheckOut = { viewModel.checkOut() },
                    onApprove = { viewModel.approveVisit() },
                    onDeny = { viewModel.showDenyDialog() },
                    onCancel = { viewModel.showCancelDialog() }
                )
            }
        }
    }

    // Dialogs
    if (uiState.showCancelDialog) {
        CancellationDialog(
            reason = uiState.cancellationReason,
            onReasonChange = { viewModel.setCancellationReason(it) },
            onDismiss = { viewModel.hideCancelDialog() },
            onConfirm = { viewModel.cancelVisit() },
            isProcessing = uiState.isProcessing
        )
    }

    if (uiState.showDenyDialog) {
        DenialDialog(
            reason = uiState.denialReason,
            onReasonChange = { viewModel.setDenialReason(it) },
            onDismiss = { viewModel.hideDenyDialog() },
            onConfirm = { viewModel.denyVisit() },
            isProcessing = uiState.isProcessing
        )
    }
}

@Composable
private fun ActionButtons(
    uiState: com.markduenas.visischeduler.presentation.viewmodel.scheduling.VisitDetailsUiState,
    onCheckIn: () -> Unit,
    onCheckOut: () -> Unit,
    onApprove: () -> Unit,
    onDeny: () -> Unit,
    onCancel: () -> Unit
) {
    val visit = uiState.visit ?: return
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (uiState.canCheckIn) {
            Button(onClick = onCheckIn, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Check In")
            }
        }

        if (uiState.canCheckOut) {
            Button(onClick = onCheckOut, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Check Out")
            }
        }

        if (uiState.canApprove) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onDeny,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Deny")
                }
                Button(onClick = onApprove, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Approve")
                }
            }
        }

        if (uiState.canCancel) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Cancel, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cancel Visit")
            }
        }
    }
}

@Composable
private fun CancellationDialog(
    reason: String,
    onReasonChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    isProcessing: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cancel Visit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Are you sure you want to cancel this visit?")
                OutlinedTextField(
                    value = reason,
                    onValueChange = onReasonChange,
                    label = { Text("Reason for cancellation") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isProcessing && reason.isNotBlank()) {
                Text("Cancel Visit", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Keep Visit") }
        }
    )
}

@Composable
private fun DenialDialog(
    reason: String,
    onReasonChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    isProcessing: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Deny Visit Request") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Please provide a reason for denying this request. The visitor will be notified.")
                OutlinedTextField(
                    value = reason,
                    onValueChange = onReasonChange,
                    label = { Text("Reason (min 10 characters)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isProcessing && reason.length >= 10) {
                Text("Deny Request", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Back") }
        }
    )
}

// Helpers
private fun formatDate(date: LocalDate): String {
    // Basic formatting - would use a proper multiplatform date formatter in real app
    return \"${date.month} ${date.dayOfMonth}, ${date.year}\"
}

private fun formatTime(time: LocalTime): String {
    val hour = if (time.hour == 0 || time.hour == 12) 12 else time.hour % 12
    val ampm = if (time.hour < 12) \"AM\" else \"PM\"
    return \"$hour:${time.minute.toString().padStart(2, '0')} $ampm\"
}
