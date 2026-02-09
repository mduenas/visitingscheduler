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
import com.markduenas.visischeduler.presentation.viewmodel.visitors.AccessLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVisitorScreen(
    onNavigateBack: () -> Unit,
    onVisitorAdded: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedRelationship by remember { mutableStateOf("") }
    var selectedAccessLevel by remember { mutableStateOf(AccessLevel.REQUIRES_APPROVAL) }
    var notes by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showRelationshipMenu by remember { mutableStateOf(false) }

    val relationships = listOf(
        "Family - Spouse",
        "Family - Child",
        "Family - Parent",
        "Family - Sibling",
        "Family - Other",
        "Friend",
        "Healthcare Provider",
        "Caregiver",
        "Clergy/Spiritual",
        "Other"
    )

    val isFormValid = name.isNotBlank() && email.isNotBlank() && selectedRelationship.isNotBlank()

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
            // Name Field
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name *") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Email Field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address *") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Phone Field
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number (Optional)") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
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
                    value = selectedRelationship,
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
                    relationships.forEach { relationship ->
                        DropdownMenuItem(
                            text = { Text(relationship) },
                            onClick = {
                                selectedRelationship = relationship
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
                selectedLevel = selectedAccessLevel,
                onLevelSelected = { selectedAccessLevel = it },
                modifier = Modifier.fillMaxWidth()
            )

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
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
                onClick = {
                    isLoading = true
                    // TODO: Save visitor
                    onVisitorAdded("new_visitor_id")
                },
                enabled = isFormValid && !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
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
}
