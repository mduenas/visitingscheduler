package com.markduenas.visischeduler.presentation.ui.screens.checkin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.markduenas.visischeduler.domain.entities.Visit
import com.markduenas.visischeduler.domain.entities.VisitStatus
import com.markduenas.visischeduler.presentation.ui.components.checkin.CheckInButton
import com.markduenas.visischeduler.presentation.ui.components.checkin.CheckInStatusCard
import com.markduenas.visischeduler.presentation.ui.components.checkin.QrCodeDisplay
import com.markduenas.visischeduler.presentation.viewmodel.checkin.CheckInViewModel

/**
 * Screen for checking in to a visit.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInScreen(
    viewModel: CheckInViewModel,
    visitId: String,
    onNavigateBack: () -> Unit,
    onNavigateToCheckOut: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val qrCodeData by viewModel.qrCodeData.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(visitId) {
        viewModel.loadVisit(visitId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Check In") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                state.visit == null -> {
                    Text(
                        text = "Visit not found",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    CheckInContent(
                        visit = state.visit!!,
                        isCheckedIn = state.isCheckedIn,
                        isCheckingIn = state.isCheckingIn,
                        isGeneratingQr = state.isGeneratingQr,
                        qrCodeData = qrCodeData?.let { "${it.visitId}:${it.signature}" },
                        onCheckIn = { viewModel.checkIn() },
                        onGenerateQrCode = { viewModel.generateQrCode() },
                        onNavigateToCheckOut = {
                            state.activeCheckIn?.id?.let { onNavigateToCheckOut(it) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CheckInContent(
    visit: Visit,
    isCheckedIn: Boolean,
    isCheckingIn: Boolean,
    isGeneratingQr: Boolean,
    qrCodeData: String?,
    onCheckIn: () -> Unit,
    onGenerateQrCode: () -> Unit,
    onNavigateToCheckOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Visit Details Header
        VisitDetailsCard(visit = visit)

        Spacer(modifier = Modifier.height(24.dp))

        // Check-in Status
        if (isCheckedIn) {
            CheckedInContent(
                visit = visit,
                onNavigateToCheckOut = onNavigateToCheckOut
            )
        } else {
            NotCheckedInContent(
                visit = visit,
                isCheckingIn = isCheckingIn,
                isGeneratingQr = isGeneratingQr,
                qrCodeData = qrCodeData,
                onCheckIn = onCheckIn,
                onGenerateQrCode = onGenerateQrCode
            )
        }
    }
}

@Composable
private fun VisitDetailsCard(
    visit: Visit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Visit Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            DetailRow(label = "Date", value = visit.scheduledDate.toString())
            DetailRow(
                label = "Time",
                value = "${visit.startTime} - ${visit.endTime}"
            )
            DetailRow(label = "Type", value = visit.visitType.name.replace("_", " "))
            DetailRow(
                label = "Status",
                value = visit.status.name.replace("_", " ")
            )
            visit.purpose?.let {
                DetailRow(label = "Purpose", value = it)
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun CheckedInContent(
    visit: Visit,
    onNavigateToCheckOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CheckInStatusCard(
            isCheckedIn = true,
            checkInTime = visit.checkInTime,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onNavigateToCheckOut,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            )
        ) {
            Icon(
                imageVector = Icons.Default.ExitToApp,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Check Out",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun NotCheckedInContent(
    visit: Visit,
    isCheckingIn: Boolean,
    isGeneratingQr: Boolean,
    qrCodeData: String?,
    onCheckIn: () -> Unit,
    onGenerateQrCode: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // QR Code Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Show this QR Code",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Have staff scan this code to check in",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (qrCodeData != null) {
                    QrCodeDisplay(
                        qrCodeData = qrCodeData,
                        size = 200,
                        modifier = Modifier.padding(8.dp)
                    )
                } else {
                    OutlinedButton(
                        onClick = onGenerateQrCode,
                        enabled = !isGeneratingQr && visit.status == VisitStatus.APPROVED
                    ) {
                        if (isGeneratingQr) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = null
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate QR Code")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Divider with "OR"
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.HorizontalDivider(
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "  OR  ",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            androidx.compose.material3.HorizontalDivider(
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Manual Check-in Button
        CheckInButton(
            onClick = onCheckIn,
            isLoading = isCheckingIn,
            enabled = visit.status == VisitStatus.APPROVED,
            modifier = Modifier.fillMaxWidth()
        )

        if (visit.status != VisitStatus.APPROVED) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Visit must be approved before check-in",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }
    }
}
