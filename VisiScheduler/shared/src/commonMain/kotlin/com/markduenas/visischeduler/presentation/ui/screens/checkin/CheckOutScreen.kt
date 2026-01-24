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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
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
import androidx.compose.material3.OutlinedTextField
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
import com.markduenas.visischeduler.presentation.ui.components.checkin.StarRating
import com.markduenas.visischeduler.presentation.ui.components.checkin.VisitDurationDisplay
import com.markduenas.visischeduler.presentation.viewmodel.checkin.CheckOutViewModel

/**
 * Screen for checking out from a visit.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckOutScreen(
    viewModel: CheckOutViewModel,
    checkInId: String,
    onNavigateBack: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToSchedule: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(checkInId) {
        viewModel.loadCheckIn(checkInId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Check Out") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
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
                state.isCheckedOut -> {
                    CheckOutSuccessContent(
                        onNavigateToDashboard = onNavigateToDashboard,
                        onScheduleNextVisit = {
                            state.visit?.beneficiaryId?.let { onNavigateToSchedule(it) }
                        }
                    )
                }
                state.checkIn == null -> {
                    Text(
                        text = "Check-in not found",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    CheckOutContent(
                        visitDurationText = state.visitDurationText ?: "0m",
                        notes = state.notes,
                        rating = state.rating,
                        isCheckingOut = state.isCheckingOut,
                        notesCharacterCount = state.notesCharacterCount,
                        notesMaxLength = state.notesMaxLength,
                        onNotesChange = { viewModel.updateNotes(it) },
                        onRatingChange = { viewModel.updateRating(it) },
                        onCheckOut = { viewModel.checkOut() }
                    )
                }
            }
        }
    }
}

@Composable
private fun CheckOutContent(
    visitDurationText: String,
    notes: String,
    rating: Int?,
    isCheckingOut: Boolean,
    notesCharacterCount: Int,
    notesMaxLength: Int,
    onNotesChange: (String) -> Unit,
    onRatingChange: (Int) -> Unit,
    onCheckOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Visit Duration
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Visit Duration",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                VisitDurationDisplay(
                    durationText = visitDurationText,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Rating Section
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
                    text = "Rate Your Visit",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "How was your experience? (Optional)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                StarRating(
                    rating = rating ?: 0,
                    onRatingChange = onRatingChange,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Notes Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Notes (Optional)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = onNotesChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Add any notes about your visit...") },
                    minLines = 3,
                    maxLines = 5,
                    supportingText = {
                        Text(
                            text = "$notesCharacterCount / $notesMaxLength",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End
                        )
                    },
                    isError = notesCharacterCount > notesMaxLength
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Check Out Button
        Button(
            onClick = onCheckOut,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isCheckingOut && notesCharacterCount <= notesMaxLength,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            )
        ) {
            if (isCheckingOut) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onSecondary,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isCheckingOut) "Checking Out..." else "Complete Check Out",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun CheckOutSuccessContent(
    onNavigateToDashboard: () -> Unit,
    onScheduleNextVisit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Success Icon
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Check Out Complete!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Thank you for your visit. We hope to see you again soon!",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Schedule Next Visit Button
        OutlinedButton(
            onClick = onScheduleNextVisit,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Schedule Next Visit",
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Go to Dashboard Button
        Button(
            onClick = onNavigateToDashboard,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Go to Dashboard",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
