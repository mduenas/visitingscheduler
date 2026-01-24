package com.markduenas.visischeduler.presentation.ui.components.navigation

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.markduenas.visischeduler.domain.entities.Role

/**
 * Navigation destination for the bottom nav bar.
 */
sealed class BottomNavDestination(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val contentDescription: String,
    val requiresCoordinatorRole: Boolean = false
) {
    data object Home : BottomNavDestination(
        route = "home",
        title = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        contentDescription = "Home dashboard"
    )

    data object Calendar : BottomNavDestination(
        route = "calendar",
        title = "Calendar",
        selectedIcon = Icons.Filled.CalendarMonth,
        unselectedIcon = Icons.Outlined.CalendarMonth,
        contentDescription = "Visit calendar"
    )

    data object Requests : BottomNavDestination(
        route = "requests",
        title = "Requests",
        selectedIcon = Icons.Filled.Inbox,
        unselectedIcon = Icons.Outlined.Inbox,
        contentDescription = "Visit requests",
        requiresCoordinatorRole = true
    )

    data object Messages : BottomNavDestination(
        route = "messages",
        title = "Messages",
        selectedIcon = Icons.Filled.Message,
        unselectedIcon = Icons.Outlined.Message,
        contentDescription = "Messages"
    )

    data object Profile : BottomNavDestination(
        route = "profile",
        title = "Profile",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person,
        contentDescription = "User profile"
    )

    companion object {
        /**
         * Get all destinations for a given user role.
         */
        fun getDestinationsForRole(role: Role?): List<BottomNavDestination> {
            val isCoordinator = role?.let {
                it == Role.ADMIN ||
                it == Role.PRIMARY_COORDINATOR ||
                it == Role.SECONDARY_COORDINATOR
            } ?: false

            return listOf(Home, Calendar, Requests, Messages, Profile)
                .filter { !it.requiresCoordinatorRole || isCoordinator }
        }
    }
}

/**
 * Badge counts for navigation items.
 */
data class NavBadgeCounts(
    val requests: Int = 0,
    val messages: Int = 0
)

/**
 * Bottom navigation bar for the main app screens.
 *
 * @param currentRoute The currently selected route
 * @param onNavigate Callback when a destination is selected
 * @param userRole The current user's role
 * @param badgeCounts Badge counts for items with notifications
 * @param modifier Modifier for the component
 */
@Composable
fun BottomNavBar(
    currentRoute: String,
    onNavigate: (BottomNavDestination) -> Unit,
    userRole: Role?,
    badgeCounts: NavBadgeCounts = NavBadgeCounts(),
    modifier: Modifier = Modifier
) {
    val destinations = BottomNavDestination.getDestinationsForRole(userRole)

    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        destinations.forEach { destination ->
            val selected = currentRoute == destination.route
            val badgeCount = when (destination) {
                is BottomNavDestination.Requests -> badgeCounts.requests
                is BottomNavDestination.Messages -> badgeCounts.messages
                else -> 0
            }

            NavBarItem(
                destination = destination,
                selected = selected,
                badgeCount = badgeCount,
                onClick = { onNavigate(destination) }
            )
        }
    }
}

/**
 * Individual navigation bar item.
 */
@Composable
private fun RowScope.NavBarItem(
    destination: BottomNavDestination,
    selected: Boolean,
    badgeCount: Int,
    onClick: () -> Unit
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = {
            NavItemIcon(
                destination = destination,
                selected = selected,
                badgeCount = badgeCount
            )
        },
        label = {
            Text(
                text = destination.title,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            indicatorColor = MaterialTheme.colorScheme.primaryContainer
        )
    )
}

/**
 * Icon with optional badge for navigation items.
 */
@Composable
private fun NavItemIcon(
    destination: BottomNavDestination,
    selected: Boolean,
    badgeCount: Int
) {
    val icon = if (selected) destination.selectedIcon else destination.unselectedIcon

    if (badgeCount > 0) {
        BadgedBox(
            badge = {
                Badge(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ) {
                    Text(
                        text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        ) {
            Icon(
                imageVector = icon,
                contentDescription = destination.contentDescription,
                modifier = Modifier.size(24.dp)
            )
        }
    } else {
        Icon(
            imageVector = icon,
            contentDescription = destination.contentDescription,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Compact bottom nav for smaller screens.
 */
@Composable
fun CompactBottomNavBar(
    currentRoute: String,
    onNavigate: (BottomNavDestination) -> Unit,
    userRole: Role?,
    badgeCounts: NavBadgeCounts = NavBadgeCounts(),
    modifier: Modifier = Modifier
) {
    val destinations = BottomNavDestination.getDestinationsForRole(userRole)

    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        destinations.forEach { destination ->
            val selected = currentRoute == destination.route
            val badgeCount = when (destination) {
                is BottomNavDestination.Requests -> badgeCounts.requests
                is BottomNavDestination.Messages -> badgeCounts.messages
                else -> 0
            }

            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(destination) },
                icon = {
                    NavItemIcon(
                        destination = destination,
                        selected = selected,
                        badgeCount = badgeCount
                    )
                },
                label = null, // No labels in compact mode
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}
