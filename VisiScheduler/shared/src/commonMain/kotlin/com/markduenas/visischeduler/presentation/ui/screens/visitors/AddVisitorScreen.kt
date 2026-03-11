package com.markduenas.visischeduler.presentation.ui.screens.visitors

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.markduenas.visischeduler.presentation.ui.components.visitors.AccessLevelSelector
import com.markduenas.visischeduler.presentation.viewmodel.visitors.AddVisitorViewModel
import com.markduenas.visischeduler.presentation.viewmodel.visitors.RelationshipType
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVisitorScreen(
    onNavigateBack: () -> Unit,
    onVisitorAdded: (String) -> Unit,
    viewModel: AddVisitorViewModel = koinInject(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showRelationshipMenu by remember { mutableStateOf(false) }

    // Navigation trigger on success
    LaunchedEffect(uiState.invitationSent) {
        if (uiState.invitationSent) {
            onVisitorAdded(uiState.email)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Visitor") },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // First Name Field
            OutlinedTextField(
                value = uiState.firstName,
                onValueChange = { viewModel.onFirstNameChange(it) },
                label = { Text("First Name *") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                isError = uiState.validationErrors.containsKey("firstName"),
                supportingText = uiState.validationErrors["firstName"]?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Last Name Field
            OutlinedTextField(
                value = uiState.lastName,
                onValueChange = { viewModel.onLastNameChange(it) },
                label = { Text("Last Name *") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                isError = uiState.validationErrors.containsKey("lastName"),
                supportingText = uiState.validationErrors["lastName"]?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Email Field
            OutlinedTextField(
                value = uiState.email,
                onValueChange = { viewModel.onEmailChange(it) },
                label = { Text("Email Address *") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                isError = uiState.validationErrors.containsKey("email"),
                supportingText = uiState.validationErrors["email"]?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Phone Field
            OutlinedTextField(
                value = uiState.phone,
                onValueChange = { viewModel.onPhoneChange(it) },
                label = { Text("Phone Number (Optional)") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                isError = uiState.validationErrors.containsKey("phone"),
                supportingText = uiState.validationErrors["phone"]?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Relationship Dropdown
            @Suppress("DEPRECATION")
            ExposedDropdownMenuBox(
                expanded = showRelationshipMenu,
                onExpandedChange = { showRelationshipMenu = it }
            ) {
                OutlinedTextField(
                    value = uiState.relationship.displayName,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Relationship *") },
                    leadingIcon = { Icon(Icons.Default.People, contentDescription = null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showRelationshipMenu) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = showRelationshipMenu,
                    onDismissRequest = { showRelationshipMenu = false }
                ) {
                    RelationshipType.entries.forEach { relationship ->
                        DropdownMenuItem(
                            text = { Text(relationship.displayName) },
                            onClick = {
                                viewModel.onRelationshipChange(relationship)
                                showRelationshipMenu = false
                            }
                        )
                    }
                }
            }

            // Access Level
            Text(
                text = "Access Level",
                style = MaterialTheme.typography.titleMedium
            )
            AccessLevelSelector(
                selectedLevel = uiState.accessLevel,
                onLevelSelected = { viewModel.onAccessLevelChange(it) },
                modifier = Modifier.fillMaxWidth()
            )

            // Custom Restrictions Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Custom Restrictions",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Apply specific visiting hours or rules for this visitor",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = uiState.customRestrictionsEnabled,
                    onCheckedChange = { viewModel.onCustomRestrictionsToggle(it) }
                )
            }

            if (uiState.customRestrictionsEnabled) {
                OutlinedButton(
                    onClick = { viewModel.onConfigureRestrictionsClick() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Configure Restrictions")
                }
            }

            // Notes
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = { viewModel.onNotesChange(it) },
                label = { Text("Notes (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Info Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "An invitation email will be sent to the visitor with instructions on how to request visits.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Submit Button
            Button(
                onClick = { viewModel.sendInvitation() },
                enabled = uiState.isValid && !uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Default.PersonAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Send Invitation")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Error handling
    uiState.error?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Error") },
            text = { Text(error.message ?: "An unknown error occurred") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }
}
