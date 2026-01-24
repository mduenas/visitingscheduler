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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.markduenas.visischeduler.android.ui.components.auth.EmailTextField
import com.markduenas.visischeduler.presentation.viewmodel.auth.ForgotPasswordUiState
import com.markduenas.visischeduler.presentation.viewmodel.auth.ForgotPasswordViewModel
import org.koin.compose.koinInject

/**
 * Forgot password screen for password reset requests.
 */
class ForgotPasswordScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel: ForgotPasswordViewModel = koinInject()
        val uiState by viewModel.forgotPasswordState.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }

        // Show snackbar for general errors
        LaunchedEffect(uiState.generalError) {
            uiState.generalError?.let { error ->
                snackbarHostState.showSnackbar(error)
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Reset Password") },
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
            ForgotPasswordContent(
                uiState = uiState,
                onEmailChange = viewModel::onEmailChange,
                onSendResetLinkClick = viewModel::sendResetLink,
                onResendClick = viewModel::resendResetLink,
                onBackToLoginClick = { navigator.pop() },
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
private fun ForgotPasswordContent(
    uiState: ForgotPasswordUiState,
    onEmailChange: (String) -> Unit,
    onSendResetLinkClick: () -> Unit,
    onResendClick: () -> Unit,
    onBackToLoginClick: () -> Unit,
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

            // Show different content based on whether reset link was sent
            if (uiState.resetLinkSent) {
                SuccessContent(
                    email = uiState.email,
                    cooldownSeconds = uiState.cooldownSeconds,
                    isLoading = uiState.isLoading,
                    onResendClick = onResendClick,
                    onBackToLoginClick = onBackToLoginClick
                )
            } else {
                RequestContent(
                    uiState = uiState,
                    onEmailChange = onEmailChange,
                    onSendResetLinkClick = onSendResetLinkClick,
                    onBackToLoginClick = onBackToLoginClick
                )
            }
        }
    }
}

@Composable
private fun RequestContent(
    uiState: ForgotPasswordUiState,
    onEmailChange: (String) -> Unit,
    onSendResetLinkClick: () -> Unit,
    onBackToLoginClick: () -> Unit
) {
    // Email Icon
    Icon(
        imageVector = Icons.Default.Email,
        contentDescription = null,
        modifier = Modifier.size(80.dp),
        tint = MaterialTheme.colorScheme.primary
    )

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = "Forgot Your Password?",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Enter your email address and we'll send you a link to reset your password.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(32.dp))

    // Form Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            // Email Field
            EmailTextField(
                value = uiState.email,
                onValueChange = onEmailChange,
                enabled = !uiState.isLoading,
                error = uiState.emailError,
                imeAction = ImeAction.Done,
                onImeAction = onSendResetLinkClick
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Send Reset Link Button
            Button(
                onClick = onSendResetLinkClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !uiState.isLoading,
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
                        text = "Send Reset Link",
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

    // Back to Login Button
    TextButton(
        onClick = onBackToLoginClick,
        enabled = !uiState.isLoading
    ) {
        Text(
            text = "Back to Sign In",
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SuccessContent(
    email: String,
    cooldownSeconds: Int,
    isLoading: Boolean,
    onResendClick: () -> Unit,
    onBackToLoginClick: () -> Unit
) {
    // Success Icon
    Icon(
        imageVector = Icons.Default.CheckCircle,
        contentDescription = null,
        modifier = Modifier.size(80.dp),
        tint = MaterialTheme.colorScheme.primary
    )

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = "Check Your Email",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "We've sent a password reset link to:",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = email,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(32.dp))

    // Instructions Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "Instructions:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            listOf(
                "1. Check your inbox (and spam folder)",
                "2. Click the reset link in the email",
                "3. Create a new password",
                "4. Sign in with your new password"
            ).forEach { instruction ->
                Text(
                    text = instruction,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Resend Link Section
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Didn't receive the email?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (cooldownSeconds > 0) {
            Text(
                text = "Resend available in $cooldownSeconds seconds",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            OutlinedButton(
                onClick = onResendClick,
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Resend Email")
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(32.dp))

    // Back to Login Button
    Button(
        onClick = onBackToLoginClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Text(
            text = "Back to Sign In",
            style = MaterialTheme.typography.labelLarge
        )
    }
}
