package com.markduenas.visischeduler.presentation.ui.components.visitors

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.markduenas.visischeduler.domain.entities.Restriction
import com.markduenas.visischeduler.domain.entities.RestrictionType

/**
 * Data class for restriction type display properties.
 */
private data class RestrictionTypeDisplay(
    val icon: ImageVector,
    val color: Color,
    val backgroundColor: Color
)

/**
 * Maps restriction types to display properties.
 */
private fun getRestrictionTypeDisplay(type: RestrictionType): RestrictionTypeDisplay {
    return when (type) {
        RestrictionType.TIME_BASED -> RestrictionTypeDisplay(
            icon = Icons.Default.AccessTime,
            color = Color(0xFF6366F1),
            backgroundColor = Color(0xFFEEF2FF)
        )
        RestrictionType.VISITOR_BASED -> RestrictionTypeDisplay(
            icon = Icons.Default.PersonOff,
            color = Color(0xFFEF4444),
            backgroundColor = Color(0xFFFEE2E2)
        )
        RestrictionType.CAPACITY_BASED -> RestrictionTypeDisplay(
            icon = Icons.Default.Group,
            color = Color(0xFFF59E0B),
            backgroundColor = Color(0xFFFEF3C7)
        )
        RestrictionType.BENEFICIARY_BASED -> RestrictionTypeDisplay(
            icon = Icons.Default.Person,
            color = Color(0xFF10B981),
            backgroundColor = Color(0xFFD1FAE5)
        )
        RestrictionType.RELATIONSHIP_BASED -> RestrictionTypeDisplay(
            icon = Icons.Default.FamilyRestroom,
            color = Color(0xFF8B5CF6),
            backgroundColor = Color(0xFFF3E8FF)
        )
        RestrictionType.COMBINED -> RestrictionTypeDisplay(
            icon = Icons.Default.Tune,
            color = Color(0xFF64748B),
            backgroundColor = Color(0xFFF1F5F9)
        )
    }
}

/**
 * A card displaying a restriction with toggle and actions.
 *
 * @param restriction The restriction to display
 * @param onToggle Callback when the restriction is toggled
 * @param onEdit Callback when edit is clicked
 * @param onDelete Callback when delete is clicked
 * @param onClick Callback when the card is clicked
 * @param modifier Modifier to apply to the card
 */
@Composable
fun RestrictionCard(
    restriction: Restriction,
    onToggle: (Boolean) -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val display = getRestrictionTypeDisplay(restriction.type)
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (restriction.isActive) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (restriction.isActive) 2.dp else 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type icon
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (restriction.isActive) {
                    display.backgroundColor
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Icon(
                    imageVector = display.icon,
                    contentDescription = null,
                    tint = if (restriction.isActive) {
                        display.color
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier
                        .padding(8.dp)
                        .size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = restriction.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (restriction.isActive) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = restriction.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                RestrictionTypeBadge(type = restriction.type)
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Toggle switch
            Switch(
                checked = restriction.isActive,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = display.color,
                    checkedTrackColor = display.color.copy(alpha = 0.5f)
                )
            )

            // More menu
            if (onEdit != null || onDelete != null) {
                androidx.compose.foundation.layout.Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp)
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
                        if (onEdit != null) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                onClick = {
                                    showMenu = false
                                    onEdit()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null
                                    )
                                }
                            )
                        }
                        if (onDelete != null) {
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = Color(0xFFDC2626)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * A badge showing the restriction type.
 */
@Composable
fun RestrictionTypeBadge(
    type: RestrictionType,
    modifier: Modifier = Modifier
) {
    val display = getRestrictionTypeDisplay(type)
    val label = when (type) {
        RestrictionType.TIME_BASED -> "Time"
        RestrictionType.VISITOR_BASED -> "Visitor"
        RestrictionType.CAPACITY_BASED -> "Capacity"
        RestrictionType.BENEFICIARY_BASED -> "Beneficiary"
        RestrictionType.RELATIONSHIP_BASED -> "Relationship"
        RestrictionType.COMBINED -> "Combined"
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = display.backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = display.icon,
                contentDescription = null,
                tint = display.color,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = display.color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * A compact restriction list item.
 */
@Composable
fun RestrictionListItem(
    restriction: Restriction,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val display = getRestrictionTypeDisplay(restriction.type)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = display.icon,
                contentDescription = null,
                tint = if (restriction.isActive) display.color else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = restriction.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = restriction.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Switch(
                checked = restriction.isActive,
                onCheckedChange = onToggle,
                modifier = Modifier.padding(start = 8.dp)
            )

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
