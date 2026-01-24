package com.markduenas.visischeduler.presentation.ui.components.visitors

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.markduenas.visischeduler.presentation.viewmodel.visitors.AccessLevel

/**
 * Data class for access level display properties.
 */
private data class AccessLevelDisplay(
    val icon: ImageVector,
    val color: Color
)

/**
 * Maps access levels to display properties.
 */
private fun getAccessLevelDisplay(accessLevel: AccessLevel): AccessLevelDisplay {
    return when (accessLevel) {
        AccessLevel.AUTO_APPROVE -> AccessLevelDisplay(
            icon = Icons.Default.CheckCircle,
            color = Color(0xFF10B981)
        )
        AccessLevel.REQUIRES_APPROVAL -> AccessLevelDisplay(
            icon = Icons.Default.HourglassEmpty,
            color = Color(0xFFF59E0B)
        )
        AccessLevel.VIEW_ONLY -> AccessLevelDisplay(
            icon = Icons.Default.RemoveRedEye,
            color = Color(0xFF6366F1)
        )
    }
}

/**
 * A radio button selector for access levels.
 *
 * @param selectedLevel The currently selected access level
 * @param onLevelSelected Callback when an access level is selected
 * @param modifier Modifier to apply to the selector
 */
@Composable
fun AccessLevelSelector(
    selectedLevel: AccessLevel,
    onLevelSelected: (AccessLevel) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AccessLevel.entries.forEach { level ->
            AccessLevelOption(
                accessLevel = level,
                selected = level == selectedLevel,
                onClick = { onLevelSelected(level) }
            )
        }
    }
}

/**
 * A single access level option.
 */
@Composable
private fun AccessLevelOption(
    accessLevel: AccessLevel,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val display = getAccessLevelDisplay(accessLevel)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                display.color.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) display.color else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = display.color,
                    unselectedColor = MaterialTheme.colorScheme.outline
                )
            )

            Spacer(modifier = Modifier.width(12.dp))

            Icon(
                imageVector = display.icon,
                contentDescription = null,
                tint = if (selected) display.color else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = accessLevel.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) display.color else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = accessLevel.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = display.color,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * A compact access level selector as a horizontal chip group.
 */
@Composable
fun AccessLevelChipSelector(
    selectedLevel: AccessLevel,
    onLevelSelected: (AccessLevel) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AccessLevel.entries.forEach { level ->
            val display = getAccessLevelDisplay(level)
            val selected = level == selectedLevel

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onLevelSelected(level) },
                shape = RoundedCornerShape(8.dp),
                color = if (selected) {
                    display.color.copy(alpha = 0.1f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                },
                border = if (selected) {
                    BorderStroke(1.5.dp, display.color)
                } else {
                    null
                }
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = display.icon,
                        contentDescription = null,
                        tint = if (selected) display.color else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = level.displayName.split(" ").first(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) display.color else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
