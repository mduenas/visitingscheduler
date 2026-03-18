package com.markduenas.visischeduler.presentation.ui.screens.checkin

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.markduenas.visischeduler.platform.CameraScanner
import com.markduenas.visischeduler.presentation.viewmodel.checkin.QrScannerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScannerScreen(
    viewModel: QrScannerViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToVisitDetails: (String) -> Unit,
    onNavigateToCheckOut: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var manualCode by remember { mutableStateOf("") }

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan QR Code") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleFlash() }) {
                        Icon(
                            imageVector = if (state.isFlashEnabled) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                            contentDescription = if (state.isFlashEnabled) "Flash on" else "Flash off"
                        )
                    }
                    IconButton(onClick = { viewModel.showManualEntry() }) {
                        Icon(Icons.Filled.EditNote, contentDescription = "Enter code manually")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isProcessing -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Processing...", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }

                state.isCheckInSuccess -> {
                    ScanSuccessContent(
                        message = state.successMessage ?: "Check-in successful!",
                        visitId = state.scannedVisit?.id,
                        checkInId = state.checkIn?.id,
                        canCheckOut = state.canCheckOut,
                        onViewDetails = { visitId ->
                            onNavigateToVisitDetails(visitId)
                        },
                        onCheckOut = { checkInId ->
                            onNavigateToCheckOut(checkInId)
                        },
                        onScanAnother = { viewModel.clearAndRescan() }
                    )
                }

                state.error != null && !state.isScanning -> {
                    ScanErrorContent(
                        error = state.error ?: "Unknown error",
                        onRetry = { viewModel.clearAndRescan() }
                    )
                }

                else -> {
                    // Live camera preview
                    CameraScanner(
                        onQrCodeScanned = { qrData -> viewModel.processQrCode(qrData) },
                        isFlashEnabled = state.isFlashEnabled,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Scanning guide overlay
                    ScannerOverlay()
                }
            }
        }
    }

    // Manual entry dialog
    if (state.showManualEntryDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideManualEntry() },
            title = { Text("Enter Visit Code") },
            text = {
                OutlinedTextField(
                    value = manualCode,
                    onValueChange = { manualCode = it },
                    label = { Text("Visit code") },
                    placeholder = { Text("Paste or type the code here") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.processManualEntry(manualCode)
                        manualCode = ""
                        viewModel.hideManualEntry()
                    }
                ) {
                    Text("Submit")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideManualEntry() }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ScannerOverlay() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Dimmed background with a clear cutout
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
        )
        // Scan window
        Box(
            modifier = Modifier
                .size(260.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Transparent)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp)
        ) {
            Text(
                text = "Align QR code within the frame",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ScanSuccessContent(
    message: String,
    visitId: String?,
    checkInId: String?,
    canCheckOut: Boolean,
    onViewDetails: (String) -> Unit,
    onCheckOut: (String) -> Unit,
    onScanAnother: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (visitId != null) {
                Button(
                    onClick = { onViewDetails(visitId) },
                    colors = ButtonDefaults.outlinedButtonColors()
                ) {
                    Text("View Details")
                }
            }
            if (canCheckOut && checkInId != null) {
                Button(onClick = { onCheckOut(checkInId) }) {
                    Text("Check Out")
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onScanAnother) {
            Text("Scan Another")
        }
    }
}

@Composable
private fun ScanErrorContent(
    error: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Error,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = error,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry) {
            Text("Try Again")
        }
    }
}
