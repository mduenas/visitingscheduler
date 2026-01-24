package com.markduenas.visischeduler.android.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.markduenas.visischeduler.android.ui.components.auth.OtpResendTimer
import com.markduenas.visischeduler.android.ui.components.auth.OtpTextField
import com.markduenas.visischeduler.presentation.viewmodel.auth.MfaMethod
import com.markduenas.visischeduler.presentation.viewmodel.auth.MfaUiState
import com.markduenas.visischeduler.presentation.viewmodel.auth.MfaViewModel
import org.koin.compose.koinInject

/**
 * MFA verification screen.
 *
 * @param challengeId The MFA challenge ID
 * @param method The MFA method being used
 * @param maskedDestination The masked destination (phone number or email)
 */
class MfaScreen(
    private val challengeId: String,
    private val method: MfaMethod,
    private val maskedDestination: String?
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel: MfaViewModel = koinInject()
        val uiState by viewModel.mfaState.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }

        // Initialize with challenge data
        LaunchedEffect(Unit) {
            viewModel.initializeWithChallenge(challengeId, method, maskedDestination)
        }

        // Handle navigation on successful verification
        LaunchedEffect(uiState.verificationSuccess) {
            if (uiState.verificationSuccess) {
                // Navigate to home/dashboard, replacing the entire auth stack
                // navigator.replaceAll(HomeScreen())
            }
        }

        // Show snackbar for general errors
        LaunchedEffect(uiState.generalError) {
            uiState.generalError?.let { error ->
                snackbarHostState.showSnackbar(error)
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Verification") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            MfaContent(
                uiState = uiState,
                onCodeChange = viewModel::onCodeChange,
                onVerifyClick = viewModel::verify,
                onResendClick = viewModel::resendCode,
                onCancelClick = { navigator.pop() },
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
private fun MfaContent(
    uiState: MfaUiState,
    onCodeChange: (String) -> Unit,
    onVerifyClick: () -> Unit,
    onResendClick: () -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Security Icon
            Icon(
                imageVector = when (uiState.mfaMethod) {
                    MfaMethod.TOTP -> Icons.Default.Security
                    else -> Icons.Default.Lock
                },
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Two-Factor Authentication",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = getMethodDescription(uiState.mfaMethod, uiState.maskedDestination),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // OTP Input Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Enter Verification Code",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // OTP Input
                    OtpTextField(
                        value = uiState.code,
                        onValueChange = onCodeChange,
                        enabled = !uiState.isLoading && uiState.attemptsRemaining > 0,
                        error = uiState.codeError,
                        onComplete = { onVerifyClick() }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Verify Button
                    Button(
                        onClick = onVerifyClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !uiState.isLoading &&
                                uiState.code.length == 6 &&
                                uiState.attemptsRemaining > 0,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Verify",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    // Error Message
                    AnimatedVisibility(
                        visible = uiState.generalError != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = uiState.generalError ?: "",
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Resend Code Section (only for SMS and Email methods)
            if (uiState.mfaMethod != MfaMethod.TOTP) {
                OtpResendTimer(
                    secondsRemaining = uiState.resendCooldownSeconds,
                    onResend = onResendClick,
                    enabled = !uiState.isLoading
                )
            } else {
                // TOTP specific help text
                Text(
                    text = "Open your authenticator app to get the code",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Cancel Button
            TextButton(
                onClick = onCancelClick,
                enabled = !uiState.isLoading
            ) {
                Text("Cancel")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Help Text
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Having trouble?",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = getTroubleText(uiState.mfaMethod),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = { /* Navigate to help/support */ },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Get Help")
                    }
                }
            }
        }
    }
}

private fun getMethodDescription(method: MfaMethod, maskedDestination: String?): String {
    return when (method) {
        MfaMethod.SMS -> "Enter the 6-digit code sent to ${maskedDestination ?: "your phone"}"
        MfaMethod.EMAIL -> "Enter the 6-digit code sent to ${maskedDestination ?: "your email"}"
        MfaMethod.TOTP -> "Enter the 6-digit code from your authenticator app"
    }
}

private fun getTroubleText(method: MfaMethod): String {
    return when (method) {
        MfaMethod.SMS -> "Make sure you have cellular signal. The code may take a few minutes to arrive. Check if your phone number is correct in your account settings."
        MfaMethod.EMAIL -> "Check your spam or junk folder. The code may take a few minutes to arrive. Make sure your email address is correct in your account settings."
        MfaMethod.TOTP -> "Make sure the time on your device is synchronized. If you've lost access to your authenticator app, you may need to use a backup code."
    }
}
