package com.markduenas.visischeduler.presentation.ui.screens.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.markduenas.visischeduler.domain.entities.Role
import com.markduenas.visischeduler.presentation.ui.components.dashboard.BeneficiaryAvatar
import com.markduenas.visischeduler.presentation.ui.components.dashboard.DashboardPrimaryFab
import com.markduenas.visischeduler.presentation.ui.components.dashboard.QuickAction
import com.markduenas.visischeduler.presentation.ui.components.dashboard.QuickActionsFab
import com.markduenas.visischeduler.presentation.ui.components.dashboard.QuickActionsOverlay
import com.markduenas.visischeduler.presentation.ui.components.dashboard.StatCardsRow
import com.markduenas.visischeduler.presentation.ui.components.dashboard.StatItem
import com.markduenas.visischeduler.presentation.ui.components.dashboard.CoordinatorDashboardContent
import com.markduenas.visischeduler.presentation.ui.components.dashboard.VisitorDashboardContent
import com.markduenas.visischeduler.presentation.ui.components.navigation.BottomNavBar
import com.markduenas.visischeduler.presentation.ui.components.navigation.BottomNavDestination
import com.markduenas.visischeduler.presentation.ui.components.navigation.NavBadgeCounts
import com.markduenas.visischeduler.presentation.viewmodel.dashboard.DashboardUiState
import com.markduenas.visischeduler.presentation.viewmodel.dashboard.DashboardViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Main dashboard screen that displays role-aware content.
 *
 * @param viewModel The dashboard ViewModel
 * @param onNavigate Navigation callback
 * @param modifier Modifier for the screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val todayDate by viewModel.todayDate.collectAsState()
    var isFabExpanded by remember { mutableStateOf(false) }
    var currentNavRoute by remember { mutableStateOf(BottomNavDestination.Home.route) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            DashboardTopBar(
                userName = uiState.currentUser?.firstName ?: "User",
                userImageUrl = uiState.currentUser?.profileImageUrl,
                notificationCount = uiState.unreadNotificationCount,
                onNotificationsClick = { viewModel.onNotificationsClick() }
            )
        },
        bottomBar = {
            BottomNavBar(
                currentRoute = currentNavRoute,
                onNavigate = { destination ->
                    currentNavRoute = destination.route
                    onNavigate(destination.route)
                },
                userRole = uiState.currentUser?.role,
                badgeCounts = NavBadgeCounts(
                    requests = uiState.pendingRequests.size
                )
            )
        },
        floatingActionButton = {
            DashboardFab(
                isCoordinator = uiState.isCoordinator,
                isExpanded = isFabExpanded,
                onExpandedChange = { isFabExpanded = it },
                onRequestVisit = { viewModel.onRequestVisitClick() },
                onViewCalendar = { viewModel.onViewCalendarClick() }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (uiState.isLoading) {
                    DashboardLoadingContent()
                } else {
                    DashboardContent(
                        uiState = uiState,
                        todayDate = todayDate,
                        onVisitClick = { viewModel.onVisitClick(it) },
                        onViewAllPendingClick = { viewModel.onViewAllPendingClick() },
                        onViewCalendarClick = { viewModel.onViewCalendarClick() },
                        onBeneficiarySelected = { viewModel.selectBeneficiary(it) },
                        onClearBeneficiaryFilter = { viewModel.clearBeneficiaryFilter() }
                    )
                }
            }

            // Overlay for FAB expansion
            QuickActionsOverlay(
                isVisible = isFabExpanded,
                onDismiss = { isFabExpanded = false }
            )
        }
    }
}

/**
 * Dashboard top app bar with user avatar and notifications.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardTopBar(
    userName: String,
    userImageUrl: String?,
    notificationCount: Int,
    onNotificationsClick: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "VisiScheduler",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatTodayDate(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        navigationIcon = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                BeneficiaryAvatar(
                    name = userName,
                    photoUrl = userImageUrl,
                    size = 36
                )
            }
        },
        actions = {
            IconButton(onClick = onNotificationsClick) {
                if (notificationCount > 0) {
                    BadgedBox(
                        badge = {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ) {
                                Text(
                                    text = if (notificationCount > 9) "9+" else notificationCount.toString()
                                )
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
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

/**
 * Main dashboard content based on user role.
 */
@Composable
private fun DashboardContent(
    uiState: DashboardUiState,
    todayDate: LocalDate,
    onVisitClick: (String) -> Unit,
    onViewAllPendingClick: () -> Unit,
    onViewCalendarClick: () -> Unit,
    onBeneficiarySelected: (String) -> Unit,
    onClearBeneficiaryFilter: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Welcome message
        WelcomeSection(
            userName = uiState.currentUser?.firstName ?: "User",
            role = uiState.currentUser?.role
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Stats cards
        StatsSection(
            visitsToday = uiState.statistics.visitsToday,
            pendingCount = uiState.statistics.pendingCount,
            completedThisWeek = uiState.statistics.completedThisWeek,
            upcomingCount = uiState.statistics.upcomingCount,
            isCoordinator = uiState.isCoordinator
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Role-specific content
        if (uiState.isCoordinator) {
            CoordinatorDashboardContent(
                pendingRequests = uiState.pendingRequests,
                todayVisits = uiState.todayVisits,
                beneficiaries = uiState.beneficiaries,
                selectedBeneficiaryId = uiState.selectedBeneficiaryId,
                onVisitClick = onVisitClick,
                onViewAllPendingClick = onViewAllPendingClick,
                onBeneficiarySelected = onBeneficiarySelected,
                onClearBeneficiaryFilter = onClearBeneficiaryFilter
            )
        } else {
            VisitorDashboardContent(
                nextVisit = uiState.nextVisit,
                upcomingVisits = uiState.upcomingVisits,
                beneficiary = uiState.selectedBeneficiary,
                onVisitClick = onVisitClick,
                onViewCalendarClick = onViewCalendarClick
            )
        }

        // Bottom padding for FAB
        Spacer(modifier = Modifier.height(80.dp))
    }
}

/**
 * Welcome section with personalized greeting.
 */
@Composable
private fun WelcomeSection(
    userName: String,
    role: Role?
) {
    Column {
        Text(
            text = getGreeting(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = userName,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        role?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = getRoleDisplayName(it),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Statistics section with quick stats cards.
 */
@Composable
private fun StatsSection(
    visitsToday: Int,
    pendingCount: Int,
    completedThisWeek: Int,
    upcomingCount: Int,
    isCoordinator: Boolean
) {
    val stats = if (isCoordinator) {
        listOf(
            StatItem(
                icon = Icons.Default.CalendarToday,
                value = visitsToday,
                label = "Today",
                tint = MaterialTheme.colorScheme.primary
            ),
            StatItem(
                icon = Icons.Default.HourglassEmpty,
                value = pendingCount,
                label = "Pending",
                tint = MaterialTheme.colorScheme.tertiary
            ),
            StatItem(
                icon = Icons.Default.CheckCircle,
                value = completedThisWeek,
                label = "This Week",
                tint = androidx.compose.ui.graphics.Color(0xFF4CAF50)
            )
        )
    } else {
        listOf(
            StatItem(
                icon = Icons.Default.Schedule,
                value = upcomingCount,
                label = "Upcoming",
                tint = MaterialTheme.colorScheme.primary
            ),
            StatItem(
                icon = Icons.Default.HourglassEmpty,
                value = pendingCount,
                label = "Pending",
                tint = MaterialTheme.colorScheme.tertiary
            ),
            StatItem(
                icon = Icons.Default.CheckCircle,
                value = completedThisWeek,
                label = "Completed",
                tint = androidx.compose.ui.graphics.Color(0xFF4CAF50)
            )
        )
    }

    StatCardsRow(stats = stats)
}

/**
 * Loading state content.
 */
@Composable
private fun DashboardLoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading dashboard...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * FAB based on user role.
 */
@Composable
private fun DashboardFab(
    isCoordinator: Boolean,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onRequestVisit: () -> Unit,
    onViewCalendar: () -> Unit
) {
    if (isCoordinator) {
        QuickActionsFab(
            isExpanded = isExpanded,
            onExpandedChange = onExpandedChange,
            actions = listOf(
                QuickAction(
                    icon = Icons.Default.EventNote,
                    label = "Schedule Visit",
                    contentDescription = "Schedule a new visit",
                    onClick = onRequestVisit,
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                QuickAction(
                    icon = Icons.Default.CalendarToday,
                    label = "View Calendar",
                    contentDescription = "View full calendar",
                    onClick = onViewCalendar
                )
            )
        )
    } else {
        DashboardPrimaryFab(
            text = "Request Visit",
            icon = Icons.Default.EventNote,
            onClick = onRequestVisit
        )
    }
}

// Helper functions

private fun formatTodayDate(): String {
    val today = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date

    val dayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    val dayOfWeek = dayNames[(today.dayOfWeek.ordinal)]
    val month = monthNames[today.monthNumber - 1]

    return "$dayOfWeek, $month ${today.dayOfMonth}"
}

private fun getGreeting(): String {
    val hour = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .hour

    return when {
        hour < 12 -> "Good morning,"
        hour < 17 -> "Good afternoon,"
        else -> "Good evening,"
    }
}

private fun getRoleDisplayName(role: Role): String {
    return when (role) {
        Role.ADMIN -> "Administrator"
        Role.PRIMARY_COORDINATOR -> "Primary Coordinator"
        Role.SECONDARY_COORDINATOR -> "Secondary Coordinator"
        Role.APPROVED_VISITOR -> "Approved Visitor"
        Role.PENDING_VISITOR -> "Pending Approval"
    }
}
