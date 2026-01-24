package com.markduenas.visischeduler.presentation.ui.components.visitors

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.markduenas.visischeduler.domain.entities.Role
import com.markduenas.visischeduler.domain.entities.User

/**
 * Status badge colors and labels.
 */
private enum class VisitorStatus(val label: String, val color: Color, val backgroundColor: Color) {
    APPROVED("Approved", Color(0xFF059669), Color(0xFFD1FAE5)),
    PENDING("Pending", Color(0xFFD97706), Color(0xFFFEF3C7)),
    BLOCKED("Blocked", Color(0xFFDC2626), Color(0xFFFEE2E2))
}

/**
 * Determines the visitor status based on user data.
 */
private fun getVisitorStatus(user: User): VisitorStatus {
    return when {
        !user.isActive -> VisitorStatus.BLOCKED
        user.role == Role.PENDING_VISITOR -> VisitorStatus.PENDING
        else -> VisitorStatus.APPROVED
    }
}

/**
 * A list item component for displaying visitor information.
 *
 * @param visitor The visitor user object
 * @param relationship Optional relationship description
 * @param showActions Whether to show quick action buttons
 * @param onApproveClick Callback when approve action is clicked
 * @param onBlockClick Callback when block action is clicked
 * @param onUnblockClick Callback when unblock action is clicked
 * @param onClick Callback when the item is clicked
 * @param modifier Modifier to apply to the item
 */
@Composable
fun VisitorListItem(
    visitor: User,
    relationship: String? = null,
    showActions: Boolean = true,
    onApproveClick: (() -> Unit)? = null,
    onBlockClick: (() -> Unit)? = null,
    onUnblockClick: (() -> Unit)? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val status = getVisitorStatus(visitor)
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            VisitorAvatar(
                fullName = visitor.fullName,
                imageUrl = visitor.profileImageUrl,
                size = AvatarSize.MEDIUM
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = visitor.fullName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Status badge
                    StatusBadge(status = status)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = visitor.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (relationship != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    RelationshipChip(relationship = relationship)
                }
            }

            // Actions
            if (showActions) {
                when (status) {
                    VisitorStatus.PENDING -> {
                        Row {
                            if (onApproveClick != null) {
                                IconButton(
                                    onClick = onApproveClick,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Approve",
                                        tint = Color(0xFF059669)
                                    )
                                }
                            }
                            if (onBlockClick != null) {
                                IconButton(
                                    onClick = onBlockClick,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Block,
                                        contentDescription = "Block",
                                        tint = Color(0xFFDC2626)
                                    )
                                }
                            }
                        }
                    }
                    VisitorStatus.APPROVED -> {
                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More options",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                if (onBlockClick != null) {
                                    DropdownMenuItem(
                                        text = { Text("Block Visitor") },
                                        onClick = {
                                            showMenu = false
                                            onBlockClick()
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Block,
                                                contentDescription = null,
                                                tint = Color(0xFFDC2626)
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                    VisitorStatus.BLOCKED -> {
                        if (onUnblockClick != null) {
                            IconButton(
                                onClick = onUnblockClick,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Unblock",
                                    tint = Color(0xFF059669)
                                )
                            }
                        }
                    }
                }
            } else {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "View details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Status badge component.
 */
@Composable
private fun StatusBadge(
    status: VisitorStatus,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = status.backgroundColor
    ) {
        Text(
            text = status.label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = status.color,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Compact visitor list item for dense lists.
 */
@Composable
fun VisitorListItemCompact(
    visitor: User,
    selected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (selected) {
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
            VisitorAvatar(
                fullName = visitor.fullName,
                imageUrl = visitor.profileImageUrl,
                size = AvatarSize.SMALL
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = visitor.fullName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = visitor.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
