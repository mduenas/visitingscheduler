package com.markduenas.visischeduler.presentation.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.markduenas.visischeduler.presentation.viewmodel.settings.SettingsViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = koinInject(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                // Account Section
                item {
                    SettingsSectionHeader(title = "Account")
                }
                item {
                    SettingsNavItem(
                        icon = Icons.Default.Person,
                        title = "Profile",
                        subtitle = "Edit your personal information",
                        onClick = { viewModel.navigateToProfile() }
                    )
                }
                item {
                    SettingsNavItem(
                        icon = Icons.Default.Security,
                        title = "Security",
                        subtitle = "Password, biometrics, and sessions",
                        onClick = { viewModel.navigateToSecuritySettings() }
                    )
                }

                // Notifications Section
                item {
                    SettingsSectionHeader(title = "Notifications")
                }
                item {
                    SettingsNavItem(
                        icon = Icons.Default.Notifications,
                        title = "Notification Preferences",
                        subtitle = "Push, email, and SMS settings",
                        onClick = { viewModel.navigateToNotificationSettings() }
                    )
                }

                // Appearance Section
                item {
                    SettingsSectionHeader(title = "Appearance")
                }
                item {
                    SettingsNavItem(
                        icon = Icons.Default.Palette,
                        title = "Theme & Display",
                        subtitle = "Dark mode, text size, language",
                        onClick = { viewModel.navigateToAppearanceSettings() }
                    )
                }

                // Beneficiary Settings (Coordinators only)
                if (uiState.isCoordinator) {
                    item {
                        SettingsSectionHeader(title = "Beneficiary")
                    }
                    item {
                        SettingsNavItem(
                            icon = Icons.Default.Settings,
                            title = "Visit Settings",
                            subtitle = "Visiting hours, defaults, and limits",
                            onClick = { viewModel.navigateToBeneficiarySettings() }
                        )
                    }
                }

                // About Section
                item {
                    SettingsSectionHeader(title = "About")
                }
                item {
                    SettingsNavItem(
                        icon = Icons.Default.Info,
                        title = "About KindVisit",
                        subtitle = "Version ${uiState.appVersion}, terms, and privacy",
                        onClick = { viewModel.navigateToAbout() }
                    )
                }
                item {
                    @Suppress("DEPRECATION")
                    SettingsNavItem(
                        icon = Icons.Default.Help,
                        title = "Help & Support",
                        subtitle = "FAQs and contact support",
                        onClick = { viewModel.contactSupport() }
                    )
                }

                // Logout
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                }
                item {
                    SettingsNavItem(
                        icon = Icons.AutoMirrored.Filled.Logout,
                        title = "Log Out",
                        subtitle = null,
                        onClick = { showLogoutDialog = true },
                        tintColor = MaterialTheme.colorScheme.error
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    // Logout Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log Out?") },
            text = { Text("Are you sure you want to log out of KindVisit?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                    }
                ) {
                    Text("Log Out", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsNavItem(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tintColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    ListItem(
        headlineContent = {
            Text(
                text = title,
                color = tintColor
            )
        },
        supportingContent = subtitle?.let {
            { Text(it) }
        },
        leadingContent = {
            Icon(
                icon,
                contentDescription = null,
                tint = tintColor
            )
        },
        trailingContent = {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}
