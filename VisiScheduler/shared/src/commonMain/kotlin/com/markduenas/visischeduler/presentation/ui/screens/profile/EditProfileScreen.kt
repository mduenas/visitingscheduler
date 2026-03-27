package com.markduenas.visischeduler.presentation.ui.screens.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.markduenas.visischeduler.platform.rememberCameraLauncher
import com.markduenas.visischeduler.platform.rememberGalleryLauncher
import com.markduenas.visischeduler.presentation.ui.components.settings.AvatarPicker
import com.markduenas.visischeduler.presentation.ui.components.settings.AvatarSource
import com.markduenas.visischeduler.presentation.viewmodel.settings.ProfileViewModel

/**
 * Edit profile screen for updating user information.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    viewModel: ProfileViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val user = uiState.user ?: return

    val launchGallery = rememberGalleryLauncher { bytes ->
        bytes?.let { viewModel.updateAvatar(it) }
    }
    val launchCamera = rememberCameraLauncher { bytes ->
        bytes?.let { viewModel.updateAvatar(it) }
    }

    var firstName by remember(user) { mutableStateOf(user.firstName) }
    var lastName by remember(user) { mutableStateOf(user.lastName) }
    var phoneNumber by remember(user) { mutableStateOf(user.phoneNumber ?: "") }

    var firstNameError by remember { mutableStateOf<String?>(null) }
    var lastNameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }

    fun validate(): Boolean {
        var isValid = true

        if (firstName.isBlank()) {
            firstNameError = "First name is required"
            isValid = false
        } else {
            firstNameError = null
        }

        if (lastName.isBlank()) {
            lastNameError = "Last name is required"
            isValid = false
        } else {
            lastNameError = null
        }

        if (phoneNumber.isNotBlank() && !isValidPhoneNumber(phoneNumber)) {
            phoneError = "Invalid phone number format"
            isValid = false
        } else {
            phoneError = null
        }

        return isValid
    }

    fun saveProfile() {
        if (validate()) {
            viewModel.updateProfile(
                firstName = firstName.trim(),
                lastName = lastName.trim(),
                phoneNumber = phoneNumber.trim().takeIf { it.isNotBlank() }
            )
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar section
            AvatarPicker(
                imageUrl = uiState.avatarUri,
                onSourceSelected = { source ->
                    when (source) {
                        AvatarSource.CAMERA -> launchCamera()
                        AvatarSource.GALLERY -> launchGallery()
                    }
                },
                size = 120.dp,
                isEditable = true,
                isUploading = uiState.isUploadingAvatar,
                initials = "${firstName.firstOrNull() ?: ""}${lastName.firstOrNull() ?: ""}"
            )

            Spacer(modifier = Modifier.height(32.dp))

            // First name field
            OutlinedTextField(
                value = firstName,
                onValueChange = {
                    firstName = it
                    firstNameError = null
                },
                label = { Text("First Name") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null
                    )
                },
                isError = firstNameError != null,
                supportingText = firstNameError?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Last name field
            OutlinedTextField(
                value = lastName,
                onValueChange = {
                    lastName = it
                    lastNameError = null
                },
                label = { Text("Last Name") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null
                    )
                },
                isError = lastNameError != null,
                supportingText = lastNameError?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Phone number field
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = {
                    phoneNumber = it
                    phoneError = null
                },
                label = { Text("Phone Number (Optional)") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null
                    )
                },
                placeholder = { Text("+1 (555) 123-4567") },
                isError = phoneError != null,
                supportingText = phoneError?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Email display (not editable)
            Text(
                text = "Email: ${user.email}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            )
            Text(
                text = "Contact support to change your email address.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Save button
            Button(
                onClick = { saveProfile() },
                enabled = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Save Changes")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Cancel button
            OutlinedButton(
                onClick = onNavigateBack,
                enabled = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }
        }
    }
}

/**
 * Validate phone number format.
 */
private fun isValidPhoneNumber(phone: String): Boolean {
    // Basic phone number validation
    val digitsOnly = phone.filter { it.isDigit() }
    return digitsOnly.length in 10..15
}
