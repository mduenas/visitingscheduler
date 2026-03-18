package com.markduenas.visischeduler.presentation.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.markduenas.visischeduler.presentation.viewmodel.auth.MfaMethod
import com.markduenas.visischeduler.presentation.viewmodel.settings.MfaSetupStep
import com.markduenas.visischeduler.presentation.viewmodel.settings.MfaSetupViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MfaSetupScreen(
    onNavigateBack: () -> Unit,
    onSetupComplete: () -> Unit,
    viewModel: MfaSetupViewModel = koinInject(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.setupSuccess) {
        if (uiState.setupSuccess) {
            // Give the user a moment to see the success state
            kotlinx.coroutines.delay(1500)
            onSetupComplete()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Set Up 2FA") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.setupStep == MfaSetupStep.SELECT_METHOD) {
                            onNavigateBack()
                        } else {
                            viewModel.goBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                when (uiState.setupStep) {
                    MfaSetupStep.SELECT_METHOD -> SelectMethodContent(
                        onMethodSelected = { viewModel.setMethod(it) }
                    )
                    MfaSetupStep.ENTER_DESTINATION -> EnterDestinationContent(
                        method = uiState.selectedMethod,
                        destination = uiState.destination,
                        onDestinationChange = { viewModel.onDestinationChange(it) },
                        onContinue = { viewModel.startSetup() },
                        isLoading = uiState.isLoading,
                        error = uiState.error
                    )
                    MfaSetupStep.VERIFY_CODE -> VerifyCodeContent(
                        method = uiState.selectedMethod,
                        destination = uiState.destination,
                        code = uiState.verificationCode,
                        onCodeChange = { viewModel.onCodeChange(it) },
                        onVerify = { viewModel.confirmSetup() },
                        isLoading = uiState.isLoading,
                        error = uiState.error
                    )
                    MfaSetupStep.SUCCESS -> SuccessContent()
                }
            }
        }
    }
}

@Composable
private fun SelectMethodContent(
    onMethodSelected: (MfaMethod) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            Icons.Default.Security,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Protect Your Account",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Add an extra layer of security by requiring a verification code when you sign in.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        MethodItem(
            title = "Text Message (SMS)",
            subtitle = "Get a code sent to your phone",
            icon = Icons.Default.Sms,
            onClick = { onMethodSelected(MfaMethod.SMS) }
        )
        
        MethodItem(
            title = "Email",
            subtitle = "Get a code sent to your inbox",
            icon = Icons.Default.Email,
            onClick = { onMethodSelected(MfaMethod.EMAIL) }
        )
    }
}

@Composable
private fun MethodItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun EnterDestinationContent(
    method: MfaMethod,
    destination: String,
    onDestinationChange: (String) -> Unit,
    onContinue: () -> Unit,
    isLoading: Boolean,
    error: String?
) {
    val title = if (method == MfaMethod.SMS) "Enter Phone Number" else "Enter Email Address"
    val placeholder = if (method == MfaMethod.SMS) "+1 (555) 000-0000" else "name@example.com"
    val keyboardType = if (method == MfaMethod.SMS) KeyboardType.Phone else KeyboardType.Email

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        OutlinedTextField(
            value = destination,
            onValueChange = onDestinationChange,
            label = { Text(if (method == MfaMethod.SMS) "Phone Number" else "Email") },
            placeholder = { Text(placeholder) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            isError = error != null,
            supportingText = error?.let { { Text(it) } }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
                enabled = destination.isNotBlank()
            ) {
                Text("Send Code")
            }
        }
    }
}

@Composable
private fun VerifyCodeContent(
    method: MfaMethod,
    destination: String,
    code: String,
    onCodeChange: (String) -> Unit,
    onVerify: () -> Unit,
    isLoading: Boolean,
    error: String?
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Verify Your Identity",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "We've sent a 6-digit verification code to $destination",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        OutlinedTextField(
            value = code,
            onValueChange = onCodeChange,
            label = { Text("Verification Code") },
            placeholder = { Text("000000") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            isError = error != null,
            supportingText = error?.let { { Text(it) } }
        )
        
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = onVerify,
                modifier = Modifier.fillMaxWidth(),
                enabled = code.length == 6
            ) {
                Text("Verify & Enable")
            }
        }
    }
}

@Composable
private fun SuccessContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Two-Factor Auth Enabled!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Your account is now more secure.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
