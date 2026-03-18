package com.markduenas.visischeduler.presentation.ui.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.markduenas.visischeduler.domain.entities.Role
import com.markduenas.visischeduler.presentation.ui.components.dashboard.CoordinatorDashboardContent
import com.markduenas.visischeduler.presentation.ui.components.dashboard.VisitorDashboardContent
import com.markduenas.visischeduler.presentation.viewmodel.dashboard.DashboardUiState
import com.markduenas.visischeduler.presentation.viewmodel.dashboard.DashboardViewModel
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val todayDate by viewModel.todayDate.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("VisiScheduler", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(formatTodayDate(), style = MaterialTheme.typography.bodySmall)
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigate("notifications") }) {
                        BadgedBox(
                            badge = {
                                if (uiState.unreadNotificationCount > 0) {
                                    Badge { Text(uiState.unreadNotificationCount.toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Welcome
                    Column {
                        Text(getGreeting(), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            uiState.currentUser?.firstName ?: "User",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Role-specific content
                    if (uiState.isCoordinator) {
                        CoordinatorDashboardContent(
                            pendingRequests = uiState.pendingRequests,
                            todayVisits = uiState.todayVisits,
                            beneficiaries = uiState.beneficiaries,
                            selectedBeneficiaryId = uiState.selectedBeneficiaryId,
                            onVisitClick = { onNavigate("visit/$it") },
                            onViewAllPendingClick = { onNavigate("pending_requests") },
                            onBeneficiarySelected = { viewModel.selectBeneficiary(it) },
                            onClearBeneficiaryFilter = { viewModel.clearBeneficiaryFilter() },
                            onTodayVisitsClick = { onNavigate("today_visits") },
                            onAnalyticsClick = { onNavigate("analytics") }
                        )
                    } else {
                        VisitorDashboardContent(
                            nextVisit = uiState.nextVisit,
                            upcomingVisits = uiState.upcomingVisits,
                            suggestedSlots = uiState.suggestedSlots,
                            beneficiary = uiState.selectedBeneficiary,
                            onVisitClick = { onNavigate("visit/$it") },
                            onViewCalendarClick = { onNavigate("calendar") }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

private fun formatTodayDate(): String {
    val today = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date
    return today.toString()
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
