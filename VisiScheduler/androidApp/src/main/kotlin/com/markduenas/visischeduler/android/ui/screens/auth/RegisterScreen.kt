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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.markduenas.visischeduler.android.ui.components.auth.ConfirmPasswordTextField
import com.markduenas.visischeduler.android.ui.components.auth.EmailTextField
import com.markduenas.visischeduler.android.ui.components.auth.NameTextField
import com.markduenas.visischeduler.android.ui.components.auth.PasswordTextField
import com.markduenas.visischeduler.android.ui.components.auth.PhoneTextField
import com.markduenas.visischeduler.domain.entities.Role
import com.markduenas.visischeduler.presentation.viewmodel.auth.RegisterUiState
import com.markduenas.visischeduler.presentation.viewmodel.auth.RegisterViewModel
import org.koin.compose.koinInject

/**
 * Registration screen for new users.
 */
class RegisterScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel: RegisterViewModel = koinInject()
        val uiState by viewModel.registerState.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }

        // Handle navigation on successful registration
        LaunchedEffect(uiState.registrationSuccess) {
            if (uiState.registrationSuccess) {
                snackbarHostState.showSnackbar("Registration successful! Please check your email to verify your account.")
                navigator.pop()
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
                    title = { Text("Create Account") },
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
            RegisterContent(
                uiState = uiState,
                onFirstNameChange = viewModel::onFirstNameChange,
                onLastNameChange = viewModel::onLastNameChange,
                onEmailChange = viewModel::onEmailChange,
                onPhoneNumberChange = viewModel::onPhoneNumberChange,
                onPasswordChange = viewModel::onPasswordChange,
                onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
                onRoleSelect = viewModel::onRoleSelect,
                onTermsAcceptedChange = viewModel::onTermsAcceptedChange,
                onRegisterClick = viewModel::register,
                onLoginClick = { navigator.pop() },
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegisterContent(
    uiState: RegisterUiState,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPhoneNumberChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onRoleSelect: (Role) -> Unit,
    onTermsAcceptedChange: (Boolean) -> Unit,
    onRegisterClick: () -> Unit,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var roleDropdownExpanded by remember { mutableStateOf(false) }

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
            Text(
                text = "Join VisiScheduler",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Create an account to schedule visits",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Registration Form Card
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
                    // Name Fields Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        NameTextField(
                            value = uiState.firstName,
                            onValueChange = onFirstNameChange,
                            label = "First Name",
                            placeholder = "John",
                            enabled = !uiState.isLoading,
                            error = uiState.firstNameError,
                            imeAction = ImeAction.Next,
                            modifier = Modifier.weight(1f)
                        )

                        NameTextField(
                            value = uiState.lastName,
                            onValueChange = onLastNameChange,
                            label = "Last Name",
                            placeholder = "Doe",
                            enabled = !uiState.isLoading,
                            error = uiState.lastNameError,
                            imeAction = ImeAction.Next,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Email Field
                    EmailTextField(
                        value = uiState.email,
                        onValueChange = onEmailChange,
                        enabled = !uiState.isLoading,
                        error = uiState.emailError,
                        imeAction = ImeAction.Next
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Phone Number Field (Optional)
                    PhoneTextField(
                        value = uiState.phoneNumber,
                        onValueChange = onPhoneNumberChange,
                        enabled = !uiState.isLoading,
                        error = uiState.phoneError,
                        label = "Phone Number (Optional)",
                        imeAction = ImeAction.Next
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Role Selection Dropdown
                    ExposedDropdownMenuBox(
                        expanded = roleDropdownExpanded,
                        onExpandedChange = { roleDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = RegisterUiState.availableRoles.find { it.first == uiState.selectedRole }?.second
                                ?: "Select Role",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Account Type") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleDropdownExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            enabled = !uiState.isLoading,
                            shape = MaterialTheme.shapes.medium
                        )

                        ExposedDropdownMenu(
                            expanded = roleDropdownExpanded,
                            onDismissRequest = { roleDropdownExpanded = false }
                        ) {
                            RegisterUiState.availableRoles.forEach { (role, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        onRoleSelect(role)
                                        roleDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password Field
                    PasswordTextField(
                        value = uiState.password,
                        onValueChange = onPasswordChange,
                        enabled = !uiState.isLoading,
                        error = uiState.passwordError,
                        imeAction = ImeAction.Next,
                        showStrengthIndicator = true,
                        passwordStrength = uiState.passwordStrength
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Confirm Password Field
                    ConfirmPasswordTextField(
                        value = uiState.confirmPassword,
                        onValueChange = onConfirmPasswordChange,
                        enabled = !uiState.isLoading,
                        error = uiState.confirmPasswordError,
                        imeAction = ImeAction.Done,
                        onImeAction = {
                            if (uiState.acceptedTerms) {
                                onRegisterClick()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Terms and Conditions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Checkbox(
                            checked = uiState.acceptedTerms,
                            onCheckedChange = onTermsAcceptedChange,
                            enabled = !uiState.isLoading
                        )

                        Column(modifier = Modifier.padding(start = 8.dp, top = 12.dp)) {
                            Text(
                                text = buildAnnotatedString {
                                    append("I agree to the ")
                                    withStyle(style = SpanStyle(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )) {
                                        append("Terms of Service")
                                    }
                                    append(" and ")
                                    withStyle(style = SpanStyle(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )) {
                                        append("Privacy Policy")
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            AnimatedVisibility(
                                visible = uiState.termsError != null,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Text(
                                    text = uiState.termsError ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Register Button
                    Button(
                        onClick = onRegisterClick,
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
                                text = "Create Account",
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

            // Login Link
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = onLoginClick,
                    enabled = !uiState.isLoading,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "Sign In",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
