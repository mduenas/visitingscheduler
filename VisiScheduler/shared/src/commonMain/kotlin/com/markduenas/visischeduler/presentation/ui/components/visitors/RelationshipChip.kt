package com.markduenas.visischeduler.presentation.ui.components.visitors

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.markduenas.visischeduler.presentation.viewmodel.visitors.RelationshipType

/**
 * Data class representing relationship display properties.
 */
private data class RelationshipDisplay(
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val backgroundColor: Color
)

/**
 * Maps relationship types to display properties.
 */
private fun getRelationshipDisplay(relationship: String): RelationshipDisplay {
    return when (relationship.uppercase()) {
        "FAMILY", "FAMILY MEMBER" -> RelationshipDisplay(
            label = "Family",
            icon = Icons.Default.FamilyRestroom,
            color = Color(0xFF6366F1),
            backgroundColor = Color(0xFFEEF2FF)
        )
        "SPOUSE", "SPOUSE/PARTNER" -> RelationshipDisplay(
            label = "Spouse",
            icon = Icons.Default.Favorite,
            color = Color(0xFFEC4899),
            backgroundColor = Color(0xFFFCE7F3)
        )
        "CHILD" -> RelationshipDisplay(
            label = "Child",
            icon = Icons.Default.FamilyRestroom,
            color = Color(0xFF14B8A6),
            backgroundColor = Color(0xFFCCFBF1)
        )
        "PARENT" -> RelationshipDisplay(
            label = "Parent",
            icon = Icons.Default.FamilyRestroom,
            color = Color(0xFF6366F1),
            backgroundColor = Color(0xFFEEF2FF)
        )
        "SIBLING" -> RelationshipDisplay(
            label = "Sibling",
            icon = Icons.Default.Group,
            color = Color(0xFF8B5CF6),
            backgroundColor = Color(0xFFF3E8FF)
        )
        "FRIEND" -> RelationshipDisplay(
            label = "Friend",
            icon = Icons.Default.Group,
            color = Color(0xFF10B981),
            backgroundColor = Color(0xFFD1FAE5)
        )
        "HEALTHCARE PROVIDER", "HEALTHCARE_PROVIDER" -> RelationshipDisplay(
            label = "Healthcare",
            icon = Icons.Default.LocalHospital,
            color = Color(0xFF0EA5E9),
            backgroundColor = Color(0xFFE0F2FE)
        )
        "CLERGY", "CLERGY/RELIGIOUS LEADER" -> RelationshipDisplay(
            label = "Clergy",
            icon = Icons.Default.VolunteerActivism,
            color = Color(0xFF8B5CF6),
            backgroundColor = Color(0xFFF3E8FF)
        )
        "ATTORNEY", "ATTORNEY/LEGAL REPRESENTATIVE" -> RelationshipDisplay(
            label = "Attorney",
            icon = Icons.Default.Gavel,
            color = Color(0xFF64748B),
            backgroundColor = Color(0xFFF1F5F9)
        )
        "SOCIAL WORKER", "SOCIAL_WORKER" -> RelationshipDisplay(
            label = "Social Worker",
            icon = Icons.Default.Psychology,
            color = Color(0xFFF59E0B),
            backgroundColor = Color(0xFFFEF3C7)
        )
        else -> RelationshipDisplay(
            label = relationship,
            icon = Icons.Default.Person,
            color = Color(0xFF64748B),
            backgroundColor = Color(0xFFF1F5F9)
        )
    }
}

/**
 * A chip displaying the visitor's relationship to the beneficiary.
 *
 * @param relationship The relationship type string
 * @param showIcon Whether to show the relationship icon
 * @param modifier Modifier to apply to the chip
 */
@Composable
fun RelationshipChip(
    relationship: String,
    showIcon: Boolean = true,
    modifier: Modifier = Modifier
) {
    val display = getRelationshipDisplay(relationship)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = display.backgroundColor
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            if (showIcon) {
                Icon(
                    imageVector = display.icon,
                    contentDescription = null,
                    tint = display.color,
                    modifier = Modifier.size(14.dp)
                )
                androidx.compose.foundation.layout.Spacer(
                    modifier = Modifier.size(4.dp)
                )
            }
            Text(
                text = display.label,
                style = MaterialTheme.typography.labelSmall,
                color = display.color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * A selectable relationship chip for forms.
 *
 * @param relationshipType The relationship type
 * @param selected Whether this chip is selected
 * @param onClick Callback when the chip is clicked
 * @param modifier Modifier to apply to the chip
 */
@Composable
fun RelationshipSelectChip(
    relationshipType: RelationshipType,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val display = getRelationshipDisplay(relationshipType.displayName)

    FilterChip(
        modifier = modifier,
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = relationshipType.displayName,
                style = MaterialTheme.typography.labelMedium
            )
        },
        leadingIcon = if (selected) {
            {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else {
            {
                Icon(
                    imageVector = display.icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurface,
            selectedContainerColor = display.backgroundColor,
            selectedLabelColor = display.color,
            selectedLeadingIconColor = display.color
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            selectedBorderColor = display.color.copy(alpha = 0.5f),
            enabled = true,
            selected = selected
        )
    )
}

/**
 * A selector for choosing a relationship type.
 *
 * @param selectedRelationship The currently selected relationship
 * @param onRelationshipSelected Callback when a relationship is selected
 * @param modifier Modifier to apply to the selector
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RelationshipSelector(
    selectedRelationship: RelationshipType,
    onRelationshipSelected: (RelationshipType) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RelationshipType.entries.forEach { relationship ->
            RelationshipSelectChip(
                relationshipType = relationship,
                selected = relationship == selectedRelationship,
                onClick = { onRelationshipSelected(relationship) }
            )
        }
    }
}
