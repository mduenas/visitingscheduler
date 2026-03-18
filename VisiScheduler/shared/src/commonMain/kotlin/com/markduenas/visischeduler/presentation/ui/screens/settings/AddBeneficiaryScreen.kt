package com.markduenas.visischeduler.presentation.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.markduenas.visischeduler.presentation.viewmodel.settings.AddBeneficiaryViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBeneficiaryScreen(
    beneficiaryId: String? = null,
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: AddBeneficiaryViewModel = koinInject(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(beneficiaryId) {
        if (beneficiaryId != null) {
            viewModel.loadBeneficiary(beneficiaryId)
        }
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            onSaveSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditMode) "Edit Beneficiary" else "Add Beneficiary") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp).padding(end = 16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        TextButton(onClick = { viewModel.saveBeneficiary() }) {
                            Text("Save")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Personal Information Section
            SectionHeader(title = "Personal Information", icon = Icons.Default.Person)
            
            OutlinedTextField(
                value = uiState.firstName,
                onValueChange = { viewModel.onFirstNameChange(it) },
                label = { Text("First Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            OutlinedTextField(
                value = uiState.lastName,
                onValueChange = { viewModel.onLastNameChange(it) },
                label = { Text("Last Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            OutlinedTextField(
                value = uiState.dateOfBirth,
                onValueChange = { viewModel.onDobChange(it) },
                label = { Text("Date of Birth (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("1950-01-01") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            // Facility Information Section
            SectionHeader(title = "Facility Details", icon = Icons.Default.Business)
            
            OutlinedTextField(
                value = uiState.roomNumber,
                onValueChange = { viewModel.onRoomChange(it) },
                label = { Text("Room / Bed Number") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            OutlinedTextField(
                value = uiState.specialInstructions,
                onValueChange = { viewModel.onInstructionsChange(it) },
                label = { Text("Special Instructions") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                placeholder = { Text("Allergies, preferred visiting times, etc.") }
            )

            // Emergency Contact Section
            SectionHeader(title = "Emergency Contact", icon = Icons.Default.ContactPhone)
            
            OutlinedTextField(
                value = uiState.emergencyContactName,
                onValueChange = { viewModel.onContactNameChange(it) },
                label = { Text("Contact Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            OutlinedTextField(
                value = uiState.emergencyContactPhone,
                onValueChange = { viewModel.onContactPhoneChange(it) },
                label = { Text("Contact Phone") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true
            )
            
            OutlinedTextField(
                value = uiState.emergencyContactRelationship,
                onValueChange = { viewModel.onContactRelChange(it) },
                label = { Text("Relationship") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (uiState.error != null) {
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
