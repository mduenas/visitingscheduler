package com.markduenas.visischeduler.android.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.markduenas.visischeduler.android.ui.components.auth.BiometricButton
import com.markduenas.visischeduler.android.ui.components.auth.EmailTextField
import com.markduenas.visischeduler.android.ui.components.auth.PasswordTextField
import com.markduenas.visischeduler.presentation.viewmodel.auth.LoginUiState
import com.markduenas.visischeduler.presentation.viewmodel.auth.LoginViewModel
import org.koin.compose.koinInject

/**
 * Login screen for user authentication.
 */
class LoginScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel: LoginViewModel = koinInject()
        val uiState by viewModel.loginState.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }

        // Handle navigation on successful login
        LaunchedEffect(uiState.loginSuccess) {
            if (uiState.loginSuccess) {
                // Navigate to home/dashboard
                // navigator.replace(HomeScreen())
            }
        }

        // Handle MFA requirement
        LaunchedEffect(uiState.requiresMfa) {
            if (uiState.requiresMfa && uiState.mfaChallenge != null) {
                navigator.push(
                    MfaScreen(
                        challengeId = uiState.mfaChallenge!!.challengeId,
                        method = uiState.mfaChallenge!!.method,
                        maskedDestination = uiState.mfaChallenge!!.maskedDestination
                    )
                )
            }
        }

        // Show snackbar for general errors
        LaunchedEffect(uiState.generalError) {
            uiState.generalError?.let { error ->
                snackbarHostState.showSnackbar(error)
            }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            LoginContent(
                uiState = uiState,
                onEmailChange = viewModel::onEmailChange,
                onPasswordChange = viewModel::onPasswordChange,
                onRememberMeChange = viewModel::onRememberMeChange,
                onLoginClick = viewModel::login,
                onBiometricClick = viewModel::loginWithBiometric,
                onForgotPasswordClick = { navigator.push(ForgotPasswordScreen()) },
                onRegisterClick = { navigator.push(RegisterScreen()) },
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
private fun LoginContent(
    uiState: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onRememberMeChange: (Boolean) -> Unit,
    onLoginClick: () -> Unit,
    onBiometricClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onRegisterClick: () -> Unit,
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
            Spacer(modifier = Modifier.height(48.dp))

            // Logo and App Name
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = "VisiScheduler Logo",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "VisiScheduler",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Schedule visits with ease",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Login Form Card
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
                    Text(
                        text = "Welcome Back",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = "Sign in to continue",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Email Field
                    EmailTextField(
                        value = uiState.email,
                        onValueChange = onEmailChange,
                        enabled = !uiState.isLoading,
                        error = uiState.emailError,
                        imeAction = ImeAction.Next
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password Field
                    PasswordTextField(
                        value = uiState.password,
                        onValueChange = onPasswordChange,
                        enabled = !uiState.isLoading,
                        error = uiState.passwordError,
                        imeAction = ImeAction.Done,
                        onImeAction = onLoginClick
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Remember Me and Forgot Password Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = uiState.rememberMe,
                                onCheckedChange = onRememberMeChange,
                                enabled = !uiState.isLoading
                            )
                            Text(
                                text = "Remember me",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        TextButton(
                            onClick = onForgotPasswordClick,
                            enabled = !uiState.isLoading
                        ) {
                            Text(text = "Forgot Password?")
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Login Button
                    Button(
                        onClick = onLoginClick,
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
                                text = "Sign In",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    // Biometric Login Option
                    AnimatedVisibility(
                        visible = uiState.isBiometricAvailable,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                HorizontalDivider(modifier = Modifier.weight(1f))
                                Text(
                                    text = "or",
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                HorizontalDivider(modifier = Modifier.weight(1f))
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            BiometricButton(
                                onClick = onBiometricClick,
                                enabled = !uiState.isLoading,
                                isLoading = uiState.isLoading
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

            Spacer(modifier = Modifier.height(32.dp))

            // Register Link
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Don't have an account? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = onRegisterClick,
                    enabled = !uiState.isLoading,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "Sign Up",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
