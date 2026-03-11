package com.markduenas.visischeduler.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.markduenas.visischeduler.presentation.navigation.*
import com.markduenas.visischeduler.presentation.ui.screens.dashboard.DashboardScreen
import com.markduenas.visischeduler.presentation.ui.screens.calendar.CalendarScreen
import com.markduenas.visischeduler.presentation.ui.screens.scheduling.PendingRequestsScreen
import com.markduenas.visischeduler.presentation.ui.screens.messaging.ConversationsScreen
import com.markduenas.visischeduler.presentation.ui.screens.profile.ProfileScreen
import com.markduenas.visischeduler.presentation.viewmodel.dashboard.DashboardViewModel
import com.markduenas.visischeduler.presentation.viewmodel.scheduling.CalendarViewModel
import com.markduenas.visischeduler.presentation.viewmodel.messaging.ConversationsViewModel
import com.markduenas.visischeduler.presentation.viewmodel.settings.ProfileViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

/**
 * Main screen with bottom navigation, hosting all main tabs.
 */
class MainScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val navigatorState = rememberMainNavigatorState()
        
        // Injected ViewModels for each tab
        val dashboardViewModel: DashboardViewModel = koinInject()
        val calendarViewModel: CalendarViewModel = koinInject()
        val conversationsViewModel: ConversationsViewModel = koinInject()
        val profileViewModel: ProfileViewModel = koinInject()

        // Sync manager for offline reliability
        val syncManager: com.markduenas.visischeduler.data.sync.SyncManager = koinInject()
        
        LaunchedEffect(Unit) {
            syncManager.startPeriodicSync()
        }

        MainScreenContent(
            navigatorState = navigatorState,
            onNotificationsClick = { navigator.navigateToNotifications() },
            onFabClick = { tab ->
                when (tab) {
                    MainTab.Home -> navigator.navigateToScheduleVisit("default")
                    MainTab.Calendar -> navigator.navigateToScheduleVisit("default")
                    MainTab.Messages -> navigator.navigateToComposeMessage()
                    else -> {}
                }
            },
            homeContent = {
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    onNavigate = { route ->
                        when (route) {
                            "calendar" -> navigatorState.selectTab(MainTab.Calendar)
                            "pending_requests" -> navigatorState.selectTab(MainTab.Requests)
                            "messages" -> navigatorState.selectTab(MainTab.Messages)
                            "profile" -> navigatorState.selectTab(MainTab.Profile)
                            "notifications" -> navigator.navigateToNotifications()
                            "settings" -> navigator.navigateToSettings()
                            else -> { /* Handle deep link routes if needed */ }
                        }
                    }
                )
            },
            calendarContent = {
                CalendarScreen(
                    viewModel = calendarViewModel,
                    onNavigateToSchedule = { date ->
                        navigator.navigateToScheduleVisit("default")
                    },
                    onNavigateToVisitDetails = { visitId ->
                        navigator.navigateToVisitDetails(visitId)
                    }
                )
            },
            requestsContent = {
                PendingRequestsScreen(
                    onNavigateBack = { navigatorState.selectTab(MainTab.Home) },
                    onViewDetails = { visitId ->
                        navigator.navigateToVisitDetails(visitId)
                    }
                )
            },
            messagesContent = {
                ConversationsScreen(
                    viewModel = conversationsViewModel
                )
            },
            profileContent = {
                ProfileScreen(
                    viewModel = profileViewModel,
                    onNavigateToEditProfile = {
                        navigator.push(AppScreen.EditProfile)
                    },
                    onNavigateToSettings = {
                        navigator.navigateToSettings()
                    },
                    onNavigateToNotifications = {
                        navigator.navigateToNotifications()
                    }
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContent(
    navigatorState: MainNavigatorState,
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

    Scaffold(
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { tab ->
                    val badge = badgeState.getBadge(tab)
                    val isSelected = currentTab == tab

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { navigatorState.selectTab(tab) },
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
        },
        floatingActionButton = {
            if (shouldShowFab(currentTab)) {
                FloatingActionButton(onClick = { onFabClick(currentTab) }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = getFabContentDescription(currentTab)
                    )
                }
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

private fun shouldShowFab(tab: MainTab): Boolean = when (tab) {
    MainTab.Home -> true
    MainTab.Calendar -> true
    MainTab.Requests -> false
    MainTab.Messages -> true
    MainTab.Profile -> false
}

private fun getFabContentDescription(tab: MainTab): String = when (tab) {
    MainTab.Home -> "Schedule new visit"
    MainTab.Calendar -> "Add event"
    MainTab.Messages -> "New message"
    else -> "Add"
}
