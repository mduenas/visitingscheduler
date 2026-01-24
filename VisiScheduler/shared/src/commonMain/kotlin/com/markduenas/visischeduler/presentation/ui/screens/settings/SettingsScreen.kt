package com.markduenas.visischeduler.presentation.ui.screens.settings

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToSecurity: () -> Unit,
    onNavigateToAppearance: () -> Unit,
    onNavigateToBeneficiarySettings: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onRemoveAdsClick: () -> Unit,
    onLogout: () -> Unit,
    showAds: Boolean = true,
    modifier: Modifier = Modifier
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    val isCoordinator = true // From user role

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
                    onClick = onNavigateToProfile
                )
            }
            item {
                SettingsNavItem(
                    icon = Icons.Default.Security,
                    title = "Security",
                    subtitle = "Password, biometrics, and sessions",
                    onClick = onNavigateToSecurity
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
                    onClick = onNavigateToNotifications
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
                    onClick = onNavigateToAppearance
                )
            }

            // Remove Ads (only show if ads are displayed)
            if (showAds) {
                item {
                    SettingsNavItem(
                        icon = Icons.Default.RemoveCircleOutline,
                        title = "Remove Ads",
                        subtitle = "One-time purchase to remove banner ads",
                        onClick = onRemoveAdsClick,
                        tintColor = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Beneficiary Settings (Coordinators only)
            if (isCoordinator) {
                item {
                    SettingsSectionHeader(title = "Beneficiary")
                }
                item {
                    SettingsNavItem(
                        icon = Icons.Default.Settings,
                        title = "Visit Settings",
                        subtitle = "Visiting hours, defaults, and limits",
                        onClick = onNavigateToBeneficiarySettings
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
                    title = "About VisiScheduler",
                    subtitle = "Version, terms, and privacy",
                    onClick = onNavigateToAbout
                )
            }
            item {
                SettingsNavItem(
                    icon = Icons.Default.Help,
                    title = "Help & Support",
                    subtitle = "FAQs and contact support",
                    onClick = { /* TODO */ }
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

    // Logout Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log Out?") },
            text = { Text("Are you sure you want to log out of VisiScheduler?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
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
        modifier = modifier.fillMaxWidth(),
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}
