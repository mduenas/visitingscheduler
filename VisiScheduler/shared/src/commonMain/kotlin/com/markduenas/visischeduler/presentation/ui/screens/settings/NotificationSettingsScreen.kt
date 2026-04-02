package com.markduenas.visischeduler.presentation.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.markduenas.visischeduler.presentation.viewmodel.settings.NotificationSettingsViewModel
import com.markduenas.visischeduler.presentation.viewmodel.settings.ReminderTime
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: NotificationSettingsViewModel = koinInject(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

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
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
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
                        checked = uiState.pushEnabled,
                        onCheckedChange = { viewModel.togglePushNotifications(it) }
                    )
                }

                item {
                    NotificationToggleItem(
                        icon = Icons.Default.Email,
                        title = "Email Notifications",
                        subtitle = "Receive updates via email",
                        checked = uiState.emailEnabled,
                        onCheckedChange = { viewModel.toggleEmailNotifications(it) }
                    )
                }

                item {
                    NotificationToggleItem(
                        icon = Icons.Default.Sms,
                        title = "SMS Notifications",
                        subtitle = "Receive text message alerts",
                        checked = uiState.smsEnabled,
                        onCheckedChange = { viewModel.toggleSmsNotifications(it) }
                    )
                }

                // Subscriptions Section
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Alert Types",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                item {
                    NotificationToggleItem(
                        icon = Icons.AutoMirrored.Filled.FactCheck,
                        title = "Visit Approvals",
                        subtitle = "Notify when a visit is approved or denied",
                        checked = uiState.approvalNotificationsEnabled,
                        onCheckedChange = { viewModel.toggleApprovalNotifications(it) }
                    )
                }

                item {
                    NotificationToggleItem(
                        icon = Icons.Default.EditCalendar,
                        title = "Schedule Changes",
                        subtitle = "Notify when a visit is rescheduled or cancelled",
                        checked = uiState.scheduleChangesEnabled,
                        onCheckedChange = { viewModel.toggleScheduleChanges(it) }
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
                        title = "Enable Reminders",
                        subtitle = "Get notified before your visits start",
                        checked = uiState.visitRemindersEnabled,
                        onCheckedChange = { viewModel.toggleVisitReminders(it) }
                    )
                }

                if (uiState.visitRemindersEnabled) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Reminder Times", style = MaterialTheme.typography.labelMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                ReminderTime.entries.forEach { time ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = uiState.reminderTimes.contains(time),
                                            onCheckedChange = { viewModel.toggleReminderTime(time) }
                                        )
                                        Text(time.displayName, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
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
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        title = "Notification Sound",
                        subtitle = "Play sound for notifications",
                        checked = uiState.soundEnabled,
                        onCheckedChange = { viewModel.toggleSound(it) }
                    )
                }

                item {
                    NotificationToggleItem(
                        icon = Icons.Default.Vibration,
                        title = "Vibration",
                        subtitle = "Vibrate for notifications",
                        checked = uiState.vibrationEnabled,
                        onCheckedChange = { viewModel.toggleVibration(it) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
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
