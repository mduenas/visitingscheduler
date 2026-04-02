package com.markduenas.visischeduler.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Pending
import androidx.compose.material.icons.outlined.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions

/**
 * Sealed class defining the bottom navigation tabs.
 */
sealed class MainTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: String,
    val tabIndex: UShort
) : Tab {

    /**
     * Home/Dashboard tab.
     */
    data object Home : MainTab(
        title = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        route = "home",
        tabIndex = 0u
    ) {
        @Composable
        override fun Content() {
            // Will be implemented by platform-specific UI
        }

        override val options: TabOptions
            @Composable
            get() = remember {
                TabOptions(
                    index = tabIndex,
                    title = title,
                    icon = null // Icons handled separately for selected/unselected states
                )
            }
    }

    /**
     * Calendar tab showing scheduled visits.
     */
    data object Calendar : MainTab(
        title = "Calendar",
        selectedIcon = Icons.Filled.CalendarMonth,
        unselectedIcon = Icons.Outlined.CalendarMonth,
        route = "calendar",
        tabIndex = 1u
    ) {
        @Composable
        override fun Content() {
            // Will be implemented by platform-specific UI
        }

        override val options: TabOptions
            @Composable
            get() = remember {
                TabOptions(
                    index = tabIndex,
                    title = title,
                    icon = null
                )
            }
    }

    /**
     * Pending requests tab.
     */
    data object Requests : MainTab(
        title = "Requests",
        selectedIcon = Icons.Filled.Pending,
        unselectedIcon = Icons.Outlined.Pending,
        route = "requests",
        tabIndex = 2u
    ) {
        @Composable
        override fun Content() {
            // Will be implemented by platform-specific UI
        }

        override val options: TabOptions
            @Composable
            get() = remember {
                TabOptions(
                    index = tabIndex,
                    title = title,
                    icon = null
                )
            }
    }

    /**
     * Messages tab.
     */
    data object Messages : MainTab(
        title = "Messages",
        selectedIcon = Icons.AutoMirrored.Filled.Message,
        unselectedIcon = Icons.AutoMirrored.Outlined.Message,
        route = "messages",
        tabIndex = 3u
    ) {
        @Composable
        override fun Content() {
            // Will be implemented by platform-specific UI
        }

        override val options: TabOptions
            @Composable
            get() = remember {
                TabOptions(
                    index = tabIndex,
                    title = title,
                    icon = null
                )
            }
    }

    /**
     * Profile tab.
     */
    data object Profile : MainTab(
        title = "Profile",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person,
        route = "profile",
        tabIndex = 4u
    ) {
        @Composable
        override fun Content() {
            // Will be implemented by platform-specific UI
        }

        override val options: TabOptions
            @Composable
            get() = remember {
                TabOptions(
                    index = tabIndex,
                    title = title,
                    icon = null
                )
            }
    }

    companion object {
        /**
         * All available tabs in order.
         */
        val entries: List<MainTab> = listOf(Home, Calendar, Requests, Messages, Profile)

        /**
         * Find a tab by its route.
         */
        fun fromRoute(route: String): MainTab? = entries.find { it.route == route }

        /**
         * Find a tab by its index.
         */
        fun fromIndex(index: Int): MainTab? = entries.getOrNull(index)
    }
}

/**
 * Data class representing badge state for a tab.
 */
data class TabBadge(
    val count: Int = 0,
    val showDot: Boolean = false
) {
    val hasContent: Boolean get() = count > 0 || showDot
    val displayText: String get() = if (count > 99) "99+" else count.toString()
}

/**
 * State holder for all tab badges.
 */
data class TabBadgeState(
    val homeBadge: TabBadge = TabBadge(),
    val calendarBadge: TabBadge = TabBadge(),
    val requestsBadge: TabBadge = TabBadge(),
    val messagesBadge: TabBadge = TabBadge(),
    val profileBadge: TabBadge = TabBadge()
) {
    /**
     * Get badge for a specific tab.
     */
    fun getBadge(tab: MainTab): TabBadge = when (tab) {
        MainTab.Home -> homeBadge
        MainTab.Calendar -> calendarBadge
        MainTab.Requests -> requestsBadge
        MainTab.Messages -> messagesBadge
        MainTab.Profile -> profileBadge
    }

    /**
     * Update badge for a specific tab.
     */
    fun updateBadge(tab: MainTab, badge: TabBadge): TabBadgeState = when (tab) {
        MainTab.Home -> copy(homeBadge = badge)
        MainTab.Calendar -> copy(calendarBadge = badge)
        MainTab.Requests -> copy(requestsBadge = badge)
        MainTab.Messages -> copy(messagesBadge = badge)
        MainTab.Profile -> copy(profileBadge = badge)
    }

    companion object {
        val Empty = TabBadgeState()
    }
}
