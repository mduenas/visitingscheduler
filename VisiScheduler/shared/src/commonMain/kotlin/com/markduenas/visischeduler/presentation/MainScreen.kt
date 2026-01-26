package com.markduenas.visischeduler.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.markduenas.visischeduler.presentation.navigation.MainTab
import com.markduenas.visischeduler.presentation.navigation.MainNavigatorState
import com.markduenas.visischeduler.presentation.navigation.TabBadge
import com.markduenas.visischeduler.presentation.navigation.TabBadgeState
import com.markduenas.visischeduler.presentation.navigation.navigateToNotifications
import com.markduenas.visischeduler.presentation.navigation.navigateToScheduleVisit
import com.markduenas.visischeduler.presentation.navigation.navigateToComposeMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Main screen with bottom navigation, hosting all main tabs.
 * This is the primary screen after authentication.
 */
class MainScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        // In a real app, these would be injected via Koin
        val navigatorState = MainNavigatorState()
        val notificationCount = MutableStateFlow(0)

        MainScreenContent(
            navigatorState = navigatorState,
            notificationCount = notificationCount,
            onNotificationsClick = { navigator.navigateToNotifications() },
            onFabClick = { tab ->
                when (tab) {
                    MainTab.Home -> navigator.navigateToScheduleVisit("default")
                    MainTab.Calendar -> navigator.navigateToScheduleVisit("default")
                    MainTab.Messages -> navigator.navigateToComposeMessage()
                    else -> {}
                }
            },
            homeContent = { HomeTabContent() },
            calendarContent = { CalendarTabContent() },
            requestsContent = { RequestsTabContent() },
            messagesContent = { MessagesTabContent() },
            profileContent = { ProfileTabContent() }
        )
    }
}

/**
 * Main screen content with scaffold layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContent(
    navigatorState: MainNavigatorState,
    notificationCount: StateFlow<Int>,
    onNotificationsClick: () -> Unit,
    onFabClick: (MainTab) -> Unit,
    homeContent: @Composable () -> Unit,
    calendarContent: @Composable () -> Unit,
    requestsContent: @Composable () -> Unit,
    messagesContent: @Composable () -> Unit,
    profileContent: @Composable () -> Unit
) {
    val currentTab by navigatorState.currentTab.collectAsState()
    val badgeState by navigatorState.badgeState.collectAsState()
    val notifications by notificationCount.collectAsState()

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MainTopAppBar(
                currentTab = currentTab,
                notificationCount = notifications,
                onNotificationsClick = onNotificationsClick,
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            MainBottomBar(
                currentTab = currentTab,
                badgeState = badgeState,
                onTabSelected = { navigatorState.selectTab(it) }
            )
        },
        floatingActionButton = {
            if (shouldShowFab(currentTab)) {
                MainFloatingActionButton(
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

/**
 * Top app bar with title and notifications.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTopAppBar(
    currentTab: MainTab,
    notificationCount: Int,
    onNotificationsClick: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior
) {
    CenterAlignedTopAppBar(
        title = { Text(getTopBarTitle(currentTab)) },
        actions = {
            IconButton(onClick = onNotificationsClick) {
                if (notificationCount > 0) {
                    BadgedBox(
                        badge = {
                            Badge {
                                Text(if (notificationCount > 99) "99+" else notificationCount.toString())
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications"
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications"
                    )
                }
            }
        }
    )
}

/**
 * Bottom navigation bar.
 */
@Composable
private fun MainBottomBar(
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
                    TabIconWithBadge(
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
 * Tab icon with badge support.
 */
@Composable
private fun TabIconWithBadge(
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
            Icon(imageVector = icon, contentDescription = tab.title)
        }
    } else {
        Icon(imageVector = icon, contentDescription = tab.title)
    }
}

/**
 * Floating action button.
 */
@Composable
private fun MainFloatingActionButton(
    currentTab: MainTab,
    onClick: () -> Unit
) {
    FloatingActionButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = getFabContentDescription(currentTab)
        )
    }
}

/**
 * Get top bar title based on current tab.
 */
private fun getTopBarTitle(tab: MainTab): String = when (tab) {
    MainTab.Home -> "VisiScheduler"
    MainTab.Calendar -> "Calendar"
    MainTab.Requests -> "Pending Requests"
    MainTab.Messages -> "Messages"
    MainTab.Profile -> "Profile"
}

/**
 * Determine if FAB should be shown for current tab.
 */
private fun shouldShowFab(tab: MainTab): Boolean = when (tab) {
    MainTab.Home -> true
    MainTab.Calendar -> true
    MainTab.Requests -> false
    MainTab.Messages -> true
    MainTab.Profile -> false
}

/**
 * Get FAB content description for accessibility.
 */
private fun getFabContentDescription(tab: MainTab): String = when (tab) {
    MainTab.Home -> "Schedule new visit"
    MainTab.Calendar -> "Add event"
    MainTab.Messages -> "New message"
    else -> "Add"
}

// ==================== Tab Content Placeholders ====================

/**
 * Home tab content placeholder.
 */
@Composable
private fun HomeTabContent() {
    Box(modifier = Modifier.fillMaxSize()) {
        Text("Home Content")
    }
}

/**
 * Calendar tab content placeholder.
 */
@Composable
private fun CalendarTabContent() {
    Box(modifier = Modifier.fillMaxSize()) {
        Text("Calendar Content")
    }
}

/**
 * Requests tab content placeholder.
 */
@Composable
private fun RequestsTabContent() {
    Box(modifier = Modifier.fillMaxSize()) {
        Text("Pending Requests Content")
    }
}

/**
 * Messages tab content placeholder.
 */
@Composable
private fun MessagesTabContent() {
    Box(modifier = Modifier.fillMaxSize()) {
        Text("Messages Content")
    }
}

/**
 * Profile tab content placeholder.
 */
@Composable
private fun ProfileTabContent() {
    Box(modifier = Modifier.fillMaxSize()) {
        Text("Profile Content")
    }
}
