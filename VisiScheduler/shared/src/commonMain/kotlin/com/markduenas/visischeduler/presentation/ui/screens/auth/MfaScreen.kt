package com.markduenas.visischeduler.presentation.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.markduenas.visischeduler.presentation.viewmodel.auth.MfaViewModel
import com.markduenas.visischeduler.presentation.viewmodel.auth.MfaMethod
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MfaScreen(
    challengeId: String,
    onNavigateBack: () -> Unit,
    onVerificationSuccess: () -> Unit,
    viewModel: MfaViewModel = koinInject(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.mfaState.collectAsState()

    // Initialize with challenge
    LaunchedEffect(challengeId) {
        viewModel.initializeWithChallenge(
            challengeId = challengeId,
            method = MfaMethod.SMS, // Default for now
            maskedDestination = null
        )
    }

    LaunchedEffect(uiState.verificationSuccess) {
        if (uiState.verificationSuccess) {
            onVerificationSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Verification") },
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Enter 6-digit Code",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            val methodText = when (uiState.mfaMethod) {
                MfaMethod.SMS -> "We've sent a text message with a code to your registered phone number."
                MfaMethod.EMAIL -> "We've sent an email with a code to your registered email address."
                MfaMethod.TOTP -> "Please enter the code from your authenticator app."
            }

            Text(
                text = methodText,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = uiState.code,
                onValueChange = { viewModel.onCodeChange(it) },
                label = { Text("Verification Code") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("000000") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                isError = uiState.codeError != null,
                supportingText = uiState.codeError?.let { { Text(it) } },
                enabled = !uiState.isLoading && uiState.attemptsRemaining > 0
            )

            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = { viewModel.verify() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.code.length == 6 && uiState.attemptsRemaining > 0
                ) {
                    Text("Verify")
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Didn't receive a code?",
                    style = MaterialTheme.typography.bodyMedium
                )
                TextButton(
                    onClick = { viewModel.resendCode() },
                    enabled = uiState.resendCooldownSeconds == 0 && !uiState.isLoading
                ) {
                    val label = if (uiState.resendCooldownSeconds > 0) {
                        "Resend in ${uiState.resendCooldownSeconds}s"
                    } else {
                        "Resend"
                    }
                    Text(label)
                }
            }

            if (uiState.generalError != null) {
                Text(
                    text = uiState.generalError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
