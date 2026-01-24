package com.markduenas.visischeduler.presentation.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.markduenas.visischeduler.presentation.viewmodel.settings.PasswordValidationState
import com.markduenas.visischeduler.presentation.viewmodel.settings.SecuritySettingsViewModel

/**
 * Screen for changing user password.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    viewModel: SecuritySettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showCurrentPassword by remember { mutableStateOf(false) }
    var showNewPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Change Password") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.resetPasswordForm()
                        onNavigateBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp)
        ) {
            // Current password field
            OutlinedTextField(
                value = uiState.currentPassword,
                onValueChange = { viewModel.updateCurrentPassword(it) },
                label = { Text("Current Password") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { showCurrentPassword = !showCurrentPassword }) {
                        Icon(
                            imageVector = if (showCurrentPassword) {
                                Icons.Default.VisibilityOff
                            } else {
                                Icons.Default.Visibility
                            },
                            contentDescription = if (showCurrentPassword) {
                                "Hide password"
                            } else {
                                "Show password"
                            }
                        )
                    }
                },
                visualTransformation = if (showCurrentPassword) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // New password field
            OutlinedTextField(
                value = uiState.newPassword,
                onValueChange = { viewModel.updateNewPassword(it) },
                label = { Text("New Password") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { showNewPassword = !showNewPassword }) {
                        Icon(
                            imageVector = if (showNewPassword) {
                                Icons.Default.VisibilityOff
                            } else {
                                Icons.Default.Visibility
                            },
                            contentDescription = if (showNewPassword) {
                                "Hide password"
                            } else {
                                "Show password"
                            }
                        )
                    }
                },
                visualTransformation = if (showNewPassword) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Confirm password field
            OutlinedTextField(
                value = uiState.confirmPassword,
                onValueChange = { viewModel.updateConfirmPassword(it) },
                label = { Text("Confirm New Password") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                        Icon(
                            imageVector = if (showConfirmPassword) {
                                Icons.Default.VisibilityOff
                            } else {
                                Icons.Default.Visibility
                            },
                            contentDescription = if (showConfirmPassword) {
                                "Hide password"
                            } else {
                                "Show password"
                            }
                        )
                    }
                },
                visualTransformation = if (showConfirmPassword) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                isError = uiState.confirmPassword.isNotEmpty() && !uiState.passwordErrors.passwordsMatch,
                supportingText = if (uiState.confirmPassword.isNotEmpty() && !uiState.passwordErrors.passwordsMatch) {
                    { Text("Passwords do not match") }
                } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Password requirements checklist
            PasswordRequirementsCard(
                validationState = uiState.passwordErrors
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Submit button
            Button(
                onClick = { viewModel.changePassword() },
                enabled = uiState.passwordErrors.isValid &&
                        uiState.currentPassword.isNotEmpty() &&
                        !uiState.isChangingPassword,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isChangingPassword) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Change Password")
            }

            // Error message
            uiState.error?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = error.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * Card showing password requirements with check marks.
 */
@Composable
private fun PasswordRequirementsCard(
    validationState: PasswordValidationState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Password Requirements",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            RequirementItem(
                text = "At least 8 characters",
                isMet = validationState.hasMinLength
            )

            RequirementItem(
                text = "One uppercase letter",
                isMet = validationState.hasUppercase
            )

            RequirementItem(
                text = "One lowercase letter",
                isMet = validationState.hasLowercase
            )

            RequirementItem(
                text = "One number",
                isMet = validationState.hasNumber
            )

            RequirementItem(
                text = "One special character",
                isMet = validationState.hasSpecialChar
            )

            RequirementItem(
                text = "Passwords match",
                isMet = validationState.passwordsMatch
            )
        }
    }
}

/**
 * Individual requirement item with check/cross icon.
 */
@Composable
private fun RequirementItem(
    text: String,
    isMet: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            imageVector = if (isMet) Icons.Default.Check else Icons.Default.Close,
            contentDescription = if (isMet) "Requirement met" else "Requirement not met",
            tint = if (isMet) {
                Color(0xFF4CAF50) // Green
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(18.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isMet) {
                Color(0xFF4CAF50)
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}
