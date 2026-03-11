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
import com.markduenas.visischeduler.domain.entities.Notification
import com.markduenas.visischeduler.domain.entities.NotificationType
import com.markduenas.visischeduler.presentation.viewmodel.notifications.NotificationsViewModel
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsListScreen(
    onNavigateBack: () -> Unit,
    viewModel: NotificationsViewModel = koinInject(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

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
                    if (uiState.unreadCount > 0) {
                        TextButton(
                            onClick = { viewModel.markAllAsRead() }
                        ) {
                            Text("Mark all read")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = modifier.fillMaxSize().padding(padding)) {
            if (uiState.isLoading && uiState.notifications.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.notifications.isEmpty()) {
                // Empty State
                Box(
                    modifier = Modifier.fillMaxSize(),
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
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(
                        items = uiState.notifications,
                        key = { it.id }
                    ) { notification ->
                        NotificationListItem(
                            notification = notification,
                            onClick = { viewModel.onNotificationClick(notification) },
                            onDismiss = {
                                // TODO: Add delete functionality to ViewModel if needed
                            }
                        )
                    }
                }
            }
            
            if (uiState.error != null) {
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.loadNotifications() }) {
                            Text("Retry")
                        }
                    }
                ) {
                    Text(uiState.error ?: "An error occurred")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationListItem(
    notification: Notification,
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
        enableDismissFromStartToEnd = false,
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
                        NotificationType.VISIT_CANCELLED -> Icons.Default.Block to MaterialTheme.colorScheme.error
                        NotificationType.VISIT_REMINDER -> Icons.Default.Alarm to MaterialTheme.colorScheme.tertiary
                        NotificationType.VISIT_REQUESTED -> Icons.Default.Schedule to MaterialTheme.colorScheme.secondary
                        NotificationType.VISIT_CHECKED_IN -> Icons.Default.Login to MaterialTheme.colorScheme.primary
                        NotificationType.VISIT_COMPLETED -> Icons.Default.DoneAll to MaterialTheme.colorScheme.primary
                        NotificationType.SCHEDULE_CHANGE -> Icons.Default.Update to MaterialTheme.colorScheme.secondary
                        NotificationType.RESTRICTION_APPLIED -> Icons.Default.Gavel to MaterialTheme.colorScheme.error
                        NotificationType.ACCOUNT_STATUS -> Icons.Default.AccountCircle to MaterialTheme.colorScheme.primary
                        NotificationType.SYSTEM_ANNOUNCEMENT -> Icons.Default.Announcement to MaterialTheme.colorScheme.onSurfaceVariant
                        NotificationType.APPROVAL_REQUEST -> Icons.Default.Pending to MaterialTheme.colorScheme.secondary
                        NotificationType.INFO -> Icons.Default.Info to MaterialTheme.colorScheme.onSurfaceVariant
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
                            text = formatTimestamp(notification.createdAt),
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

private fun formatTimestamp(timestamp: Instant): String {
    // Simplified timestamp formatting
    val now = kotlinx.datetime.Clock.System.now()
    val diff = now - timestamp
    
    return when {
        diff.inWholeMinutes < 1 -> "Just now"
        diff.inWholeHours < 1 -> "${diff.inWholeMinutes}m ago"
        diff.inWholeDays < 1 -> "${diff.inWholeHours}h ago"
        diff.inWholeDays < 7 -> "${diff.inWholeDays}d ago"
        else -> {
            val date = timestamp.toLocalDateTime(TimeZone.currentSystemDefault())
            "${date.month.name.take(3)} ${date.dayOfMonth}"
        }
    }
}
