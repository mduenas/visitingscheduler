package com.markduenas.visischeduler.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabNavigator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Main navigator with bottom tab navigation.
 * Preserves state between tab switches.
 *
 * @param badgeState State flow for tab badges
 * @param onTabSelected Callback when a tab is selected
 * @param onFabClick Callback for FAB click
 * @param topBar Optional composable for the top app bar
 * @param content Content for each tab
 */
@Composable
fun MainTabNavigator(
    badgeState: StateFlow<TabBadgeState> = MutableStateFlow(TabBadgeState.Empty),
    onTabSelected: (MainTab) -> Unit = {},
    onFabClick: ((MainTab) -> Unit)? = null,
    topBar: @Composable (MainTab) -> Unit = {},
    homeContent: @Composable () -> Unit = {},
    calendarContent: @Composable () -> Unit = {},
    requestsContent: @Composable () -> Unit = {},
    messagesContent: @Composable () -> Unit = {},
    profileContent: @Composable () -> Unit = {}
) {
    val badges by badgeState.collectAsState()
    var selectedTabIndex by rememberSaveable { mutableStateOf(0) }

    TabNavigator(MainTab.Home) { tabNavigator ->
        val currentTab = tabNavigator.current as? MainTab ?: MainTab.Home

        Scaffold(
            topBar = { topBar(currentTab) },
            bottomBar = {
                MainBottomNavigationBar(
                    currentTab = currentTab,
                    badgeState = badges,
                    onTabSelected = { tab ->
                        tabNavigator.current = tab
                        selectedTabIndex = tab.tabIndex.toInt()
                        onTabSelected(tab)
                    }
                )
            },
            floatingActionButton = {
                if (onFabClick != null && shouldShowFab(currentTab)) {
                    MainFab(
                        currentTab = currentTab,
                        onClick = { onFabClick(currentTab) }
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Render content based on current tab
                when (currentTab) {
                    MainTab.Home -> homeContent()
                    MainTab.Calendar -> calendarContent()
                    MainTab.Requests -> requestsContent()
                    MainTab.Messages -> messagesContent()
                    MainTab.Profile -> profileContent()
                }
            }
        }
    }
}

/**
 * Bottom navigation bar with badge support.
 */
@Composable
private fun MainBottomNavigationBar(
    currentTab: MainTab,
    badgeState: TabBadgeState,
    onTabSelected: (MainTab) -> Unit
) {
    NavigationBar {
        MainTab.entries.forEach { tab ->
            val badge = badgeState.getBadge(tab)
            val isSelected = currentTab == tab

            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    TabIcon(
                        tab = tab,
                        isSelected = isSelected,
                        badge = badge
                    )
                },
                label = { Text(tab.title) }
            )
        }
    }
}

/**
 * Tab icon with optional badge.
 */
@Composable
private fun TabIcon(
    tab: MainTab,
    isSelected: Boolean,
    badge: TabBadge
) {
    val icon = if (isSelected) tab.selectedIcon else tab.unselectedIcon

    if (badge.hasContent) {
        BadgedBox(
            badge = {
                if (badge.count > 0) {
                    Badge { Text(badge.displayText) }
                } else if (badge.showDot) {
                    Badge()
                }
            }
        ) {
            Icon(
                imageVector = icon,
                contentDescription = tab.title
            )
        }
    } else {
        Icon(
            imageVector = icon,
            contentDescription = tab.title
        )
    }
}

/**
 * Floating action button that changes based on current tab.
 */
@Composable
private fun MainFab(
    currentTab: MainTab,
    onClick: () -> Unit
) {
    val fabConfig = getFabConfig(currentTab)

    if (fabConfig != null) {
        FloatingActionButton(onClick = onClick) {
            Icon(
                imageVector = fabConfig.icon,
                contentDescription = fabConfig.contentDescription
            )
        }
    }
}

/**
 * Configuration for the FAB based on current tab.
 */
private data class FabConfig(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val contentDescription: String
)

/**
 * Determine if FAB should be shown for the current tab.
 */
private fun shouldShowFab(tab: MainTab): Boolean = when (tab) {
    MainTab.Home -> true // Quick schedule
    MainTab.Calendar -> true // New event
    MainTab.Requests -> false
    MainTab.Messages -> true // New message
    MainTab.Profile -> false
}

/**
 * Get FAB configuration for the current tab.
 */
private fun getFabConfig(tab: MainTab): FabConfig? = when (tab) {
    MainTab.Home -> FabConfig(
        icon = Icons.Default.Add,
        contentDescription = "Schedule visit"
    )
    MainTab.Calendar -> FabConfig(
        icon = Icons.Default.Add,
        contentDescription = "Add event"
    )
    MainTab.Messages -> FabConfig(
        icon = Icons.Default.Add,
        contentDescription = "New message"
    )
    else -> null
}

/**
 * State holder for the main tab navigator.
 */
class MainNavigatorState {
    private val _currentTab = MutableStateFlow<MainTab>(MainTab.Home)
    val currentTab: StateFlow<MainTab> = _currentTab.asStateFlow()

    private val _badgeState = MutableStateFlow(TabBadgeState.Empty)
    val badgeState: StateFlow<TabBadgeState> = _badgeState.asStateFlow()

    /**
     * Select a tab.
     */
    fun selectTab(tab: MainTab) {
        _currentTab.value = tab
    }

    /**
     * Update badge for a tab.
     */
    fun updateBadge(tab: MainTab, badge: TabBadge) {
        _badgeState.value = _badgeState.value.updateBadge(tab, badge)
    }

    /**
     * Update message badge count.
     */
    fun updateMessageCount(count: Int) {
        updateBadge(MainTab.Messages, TabBadge(count = count))
    }

    /**
     * Update pending requests badge count.
     */
    fun updateRequestsCount(count: Int) {
        updateBadge(MainTab.Requests, TabBadge(count = count))
    }

    /**
     * Show notification dot on home tab.
     */
    fun showHomeNotification(show: Boolean) {
        updateBadge(MainTab.Home, TabBadge(showDot = show))
    }

    /**
     * Clear all badges.
     */
    fun clearAllBadges() {
        _badgeState.value = TabBadgeState.Empty
    }
}

/**
 * Remember and create a MainNavigatorState.
 */
@Composable
fun rememberMainNavigatorState(): MainNavigatorState {
    return remember { MainNavigatorState() }
}
