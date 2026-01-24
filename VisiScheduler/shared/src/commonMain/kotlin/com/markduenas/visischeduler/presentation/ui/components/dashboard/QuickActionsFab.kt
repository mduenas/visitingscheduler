package com.markduenas.visischeduler.presentation.ui.components.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Data class representing a quick action.
 */
data class QuickAction(
    val icon: ImageVector,
    val label: String,
    val contentDescription: String,
    val onClick: () -> Unit,
    val containerColor: Color? = null
)

/**
 * An expandable FAB with quick actions.
 *
 * @param isExpanded Whether the FAB is expanded
 * @param onExpandedChange Callback when expansion state changes
 * @param actions List of quick actions to display
 * @param modifier Modifier for the component
 */
@Composable
fun QuickActionsFab(
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    actions: List<QuickAction>,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 45f else 0f,
        label = "fab_rotation"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Action items
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                actions.forEach { action ->
                    QuickActionItem(
                        action = action,
                        onClick = {
                            action.onClick()
                            onExpandedChange(false)
                        }
                    )
                }
            }
        }

        // Main FAB
        FloatingActionButton(
            onClick = { onExpandedChange(!isExpanded) },
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = if (isExpanded) "Close menu" else "Open menu",
                modifier = Modifier.rotate(rotation)
            )
        }
    }
}

/**
 * Individual quick action item.
 */
@Composable
private fun QuickActionItem(
    action: QuickAction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        // Label
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp
        ) {
            Text(
                text = action.label,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Mini FAB
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = action.containerColor
                ?: MaterialTheme.colorScheme.secondaryContainer,
            contentColor = action.containerColor?.let { Color.White }
                ?: MaterialTheme.colorScheme.onSecondaryContainer,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 2.dp,
                pressedElevation = 4.dp
            )
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = action.contentDescription,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Overlay that closes the FAB when tapped.
 */
@Composable
fun QuickActionsOverlay(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )
    }
}

/**
 * Pre-configured quick actions for visitor dashboard.
 */
@Composable
fun getVisitorQuickActions(
    onRequestVisit: () -> Unit,
    onViewCalendar: () -> Unit,
    onViewHistory: () -> Unit
): List<QuickAction> {
    return listOf(
        QuickAction(
            icon = Icons.Default.EventNote,
            label = "Request Visit",
            contentDescription = "Request a new visit",
            onClick = onRequestVisit,
            containerColor = MaterialTheme.colorScheme.primary
        ),
        QuickAction(
            icon = Icons.Default.CalendarMonth,
            label = "View Calendar",
            contentDescription = "View calendar",
            onClick = onViewCalendar
        ),
        QuickAction(
            icon = Icons.Default.History,
            label = "Visit History",
            contentDescription = "View visit history",
            onClick = onViewHistory
        )
    )
}

/**
 * Pre-configured quick actions for coordinator dashboard.
 */
@Composable
fun getCoordinatorQuickActions(
    onScheduleVisit: () -> Unit,
    onViewCalendar: () -> Unit,
    onScanCheckIn: () -> Unit,
    onStartVideoCall: () -> Unit
): List<QuickAction> {
    return listOf(
        QuickAction(
            icon = Icons.Default.PersonAdd,
            label = "Schedule Visit",
            contentDescription = "Schedule a new visit",
            onClick = onScheduleVisit,
            containerColor = MaterialTheme.colorScheme.primary
        ),
        QuickAction(
            icon = Icons.Default.QrCodeScanner,
            label = "Scan Check-in",
            contentDescription = "Scan visitor check-in QR code",
            onClick = onScanCheckIn
        ),
        QuickAction(
            icon = Icons.Default.Videocam,
            label = "Start Video Call",
            contentDescription = "Start a video call visit",
            onClick = onStartVideoCall
        ),
        QuickAction(
            icon = Icons.Default.CalendarMonth,
            label = "Full Calendar",
            contentDescription = "View full calendar",
            onClick = onViewCalendar
        )
    )
}

/**
 * Simple primary action FAB for the dashboard.
 */
@Composable
fun DashboardPrimaryFab(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    extended: Boolean = true
) {
    if (extended) {
        ExtendedFloatingActionButton(
            onClick = onClick,
            modifier = modifier,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text)
        }
    } else {
        FloatingActionButton(
            onClick = onClick,
            modifier = modifier,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text
            )
        }
    }
}
