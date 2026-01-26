package com.markduenas.visischeduler.presentation.ui.screens.notifications

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: String,
    val type: NotificationType,
    val isRead: Boolean,
    val actionData: String? = null
)

enum class NotificationType {
    VISIT_APPROVED, VISIT_DENIED, VISIT_REMINDER, VISIT_REQUEST,
    MESSAGE, SYSTEM, CHECK_IN
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsListScreen(
    onNavigateBack: () -> Unit,
    onNotificationClick: (NotificationItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val notifications = remember {
        mutableStateListOf(
            NotificationItem(
                "1", "Visit Approved",
                "Your visit request for Jan 25 at 2:00 PM has been approved.",
                "2 hours ago", NotificationType.VISIT_APPROVED, false
            ),
            NotificationItem(
                "2", "New Message",
                "John Smith sent you a message about tomorrow's visit.",
                "3 hours ago", NotificationType.MESSAGE, false
            ),
            NotificationItem(
                "3", "Visit Reminder",
                "Reminder: You have a visit scheduled for tomorrow at 3:00 PM.",
                "5 hours ago", NotificationType.VISIT_REMINDER, true
            ),
            NotificationItem(
                "4", "New Visit Request",
                "Jane Doe requested to visit on Jan 26 at 10:00 AM.",
                "Yesterday", NotificationType.VISIT_REQUEST, true
            ),
            NotificationItem(
                "5", "Check-in Confirmed",
                "John Smith has checked in for their 2:00 PM visit.",
                "Yesterday", NotificationType.CHECK_IN, true
            ),
            NotificationItem(
                "6", "System Update",
                "VisiScheduler has been updated with new features.",
                "2 days ago", NotificationType.SYSTEM, true
            )
        )
    }

    val unreadCount = notifications.count { !it.isRead }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (unreadCount > 0) {
                        TextButton(
                            onClick = {
                                // Mark all as read
                                val updated = notifications.map { it.copy(isRead = true) }
                                notifications.clear()
                                notifications.addAll(updated)
                            }
                        ) {
                            Text("Mark all read")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (notifications.isEmpty()) {
            // Empty State
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.NotificationsOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "No notifications",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "You're all caught up!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(
                    items = notifications,
                    key = { it.id }
                ) { notification ->
                    NotificationListItem(
                        notification = notification,
                        onClick = {
                            // Mark as read
                            val index = notifications.indexOfFirst { it.id == notification.id }
                            if (index >= 0) {
                                notifications[index] = notification.copy(isRead = true)
                            }
                            onNotificationClick(notification)
                        },
                        onDismiss = {
                            notifications.remove(notification)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationListItem(
    notification: NotificationItem,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDismiss()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        },
        modifier = modifier
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (notification.isRead) {
                    MaterialTheme.colorScheme.surface
                } else {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val (icon, tint) = when (notification.type) {
                        NotificationType.VISIT_APPROVED -> Icons.Default.CheckCircle to MaterialTheme.colorScheme.primary
                        NotificationType.VISIT_DENIED -> Icons.Default.Cancel to MaterialTheme.colorScheme.error
                        NotificationType.VISIT_REMINDER -> Icons.Default.Alarm to MaterialTheme.colorScheme.tertiary
                        NotificationType.VISIT_REQUEST -> Icons.Default.Schedule to MaterialTheme.colorScheme.secondary
                        NotificationType.MESSAGE -> Icons.Default.Message to MaterialTheme.colorScheme.primary
                        NotificationType.CHECK_IN -> Icons.Default.Login to MaterialTheme.colorScheme.primary
                        NotificationType.SYSTEM -> Icons.Default.Info to MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Content
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = notification.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold
                        )
                        Text(
                            text = notification.timestamp,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = notification.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Unread indicator
                if (!notification.isRead) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .align(Alignment.Top)
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primary
                        ) { }
                    }
                }
            }
        }
    }
}
