package com.markduenas.visischeduler.presentation.ui.components.messaging

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Message input component with text field and send button.
 */
@Composable
fun MessageInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    placeholder: String = "Type a message...",
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text(placeholder) },
                modifier = Modifier.weight(1f),
                enabled = enabled,
                maxLines = 4,
                shape = RoundedCornerShape(24.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onSend,
                enabled = enabled && value.isNotBlank()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (value.isNotBlank()) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Date separator shown between messages from different days.
 */
@Composable
fun DateSeparator(
    date: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f))
        Text(
            text = date,
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider(modifier = Modifier.weight(1f))
    }
}

/**
 * System message (e.g., "User joined the conversation").
 */
@Composable
fun SystemMessage(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Chat message bubble.
 */
@Composable
fun MessageBubble(
    message: String,
    isFromCurrentUser: Boolean,
    timestamp: String,
    senderName: String? = null,
    isRead: Boolean = false,
    modifier: Modifier = Modifier
) {
    val alignment = if (isFromCurrentUser) Alignment.CenterEnd else Alignment.CenterStart
    val backgroundColor = if (isFromCurrentUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (isFromCurrentUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = alignment
    ) {
        Column(
            horizontalAlignment = if (isFromCurrentUser) Alignment.End else Alignment.Start
        ) {
            if (senderName != null && !isFromCurrentUser) {
                Text(
                    text = senderName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isFromCurrentUser) 16.dp else 4.dp,
                    bottomEnd = if (isFromCurrentUser) 4.dp else 16.dp
                ),
                color = backgroundColor
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
            }

            Text(
                text = timestamp,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

/**
 * Typing indicator showing animated dots.
 */
@Composable
fun TypingIndicator(
    userName: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$userName is typing",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        // Simple dots indicator
        Text(
            text = "...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Conversation list item.
 */
@Composable
fun ConversationItem(
    name: String,
    lastMessage: String,
    timestamp: String,
    unreadCount: Int = 0,
    avatarInitials: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = avatarInitials,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = timestamp,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (unreadCount > 0) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = lastMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (unreadCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Badge {
                            Text(text = unreadCount.toString())
                        }
                    }
                }
            }
        }
    }
}

/**
 * Recipient chip for selected recipients.
 */
@Composable
fun RecipientChip(
    name: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    InputChip(
        selected = true,
        onClick = onRemove,
        label = { Text(name) },
        trailingIcon = {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove",
                modifier = Modifier.size(18.dp)
            )
        },
        modifier = modifier
    )
}

/**
 * Contact list item for recipient selection.
 */
@Composable
fun ContactListItem(
    name: String,
    subtitle: String?,
    initials: String,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
