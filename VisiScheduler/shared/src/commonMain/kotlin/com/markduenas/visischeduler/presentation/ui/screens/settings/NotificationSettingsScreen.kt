package com.markduenas.visischeduler.presentation.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pushEnabled by remember { mutableStateOf(true) }
    var emailEnabled by remember { mutableStateOf(true) }
    var smsEnabled by remember { mutableStateOf(false) }
    var reminder24h by remember { mutableStateOf(true) }
    var reminder2h by remember { mutableStateOf(true) }
    var reminder30m by remember { mutableStateOf(false) }
    var soundEnabled by remember { mutableStateOf(true) }
    var vibrationEnabled by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification Settings") },
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Channels Section
            item {
                Text(
                    text = "Notification Channels",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                NotificationToggleItem(
                    icon = Icons.Default.Notifications,
                    title = "Push Notifications",
                    subtitle = "Receive alerts on your device",
                    checked = pushEnabled,
                    onCheckedChange = { pushEnabled = it }
                )
            }

            item {
                NotificationToggleItem(
                    icon = Icons.Default.Email,
                    title = "Email Notifications",
                    subtitle = "Receive updates via email",
                    checked = emailEnabled,
                    onCheckedChange = { emailEnabled = it }
                )
            }

            item {
                NotificationToggleItem(
                    icon = Icons.Default.Sms,
                    title = "SMS Notifications",
                    subtitle = "Receive text message alerts",
                    checked = smsEnabled,
                    onCheckedChange = { smsEnabled = it }
                )
            }

            // Reminders Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Visit Reminders",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                NotificationToggleItem(
                    icon = Icons.Default.NotificationsActive,
                    title = "24 Hours Before",
                    subtitle = "Reminder one day before visit",
                    checked = reminder24h,
                    onCheckedChange = { reminder24h = it }
                )
            }

            item {
                NotificationToggleItem(
                    icon = Icons.Default.NotificationsActive,
                    title = "2 Hours Before",
                    subtitle = "Reminder 2 hours before visit",
                    checked = reminder2h,
                    onCheckedChange = { reminder2h = it }
                )
            }

            item {
                NotificationToggleItem(
                    icon = Icons.Default.NotificationsActive,
                    title = "30 Minutes Before",
                    subtitle = "Reminder 30 minutes before visit",
                    checked = reminder30m,
                    onCheckedChange = { reminder30m = it }
                )
            }

            // Sound & Vibration Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Sound & Vibration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                NotificationToggleItem(
                    icon = Icons.Default.VolumeUp,
                    title = "Notification Sound",
                    subtitle = "Play sound for notifications",
                    checked = soundEnabled,
                    onCheckedChange = { soundEnabled = it }
                )
            }

            item {
                NotificationToggleItem(
                    icon = Icons.Default.Vibration,
                    title = "Vibration",
                    subtitle = "Vibrate for notifications",
                    checked = vibrationEnabled,
                    onCheckedChange = { vibrationEnabled = it }
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun NotificationToggleItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}
