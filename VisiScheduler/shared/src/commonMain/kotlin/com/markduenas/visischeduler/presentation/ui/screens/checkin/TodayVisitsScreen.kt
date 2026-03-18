package com.markduenas.visischeduler.presentation.ui.screens.checkin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.markduenas.visischeduler.domain.entities.ExpectedVisitor
import com.markduenas.visischeduler.domain.entities.ExpectedVisitorStatus
import com.markduenas.visischeduler.presentation.viewmodel.checkin.TodayVisitsViewModel
import com.markduenas.visischeduler.presentation.viewmodel.checkin.VisitorStatusFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayVisitsScreen(
    viewModel: TodayVisitsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToVisitDetails: (String) -> Unit,
    onNavigateToQrScanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val filters = listOf(
        VisitorStatusFilter.ALL to "All (${state.totalCount})",
        VisitorStatusFilter.UPCOMING to "Upcoming (${state.pendingCount})",
        VisitorStatusFilter.CHECKED_IN to "Here (${state.checkedInCount})",
        VisitorStatusFilter.LATE to "Late (${state.lateVisitors.size})",
        VisitorStatusFilter.NO_SHOW to "No Show (${state.noShowVisitors.size})",
        VisitorStatusFilter.CHECKED_OUT to "Done (${state.checkedOutVisitors.size})"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Today's Visitors")
                        Text(
                            text = if (state.isViewingToday) "Today" else state.selectedDate?.toString() ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToQrScanner) {
                Icon(Icons.Filled.QrCodeScanner, contentDescription = "Scan QR code")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Status filter tabs
            ScrollableTabRow(
                selectedTabIndex = filters.indexOfFirst { it.first == state.statusFilter }
                    .coerceAtLeast(0),
                edgePadding = 8.dp
            ) {
                filters.forEachIndexed { index, (filter, label) ->
                    Tab(
                        selected = state.statusFilter == filter,
                        onClick = { viewModel.setStatusFilter(filter) },
                        text = { Text(label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            PullToRefreshBox(
                isRefreshing = state.isLoading,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize()
            ) {
                if (state.isLoading && state.allVisitors.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (state.displayedVisitors.isEmpty()) {
                    EmptyVisitorsContent(filter = state.statusFilter)
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 88.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.displayedVisitors, key = { it.visit.id }) { visitor ->
                            VisitorCard(
                                visitor = visitor,
                                isCheckingIn = state.checkingInVisitorId == visitor.visit.id,
                                onQuickCheckIn = { viewModel.quickCheckIn(visitor) },
                                onViewDetails = { onNavigateToVisitDetails(visitor.visit.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VisitorCard(
    visitor: ExpectedVisitor,
    isCheckingIn: Boolean,
    onQuickCheckIn: () -> Unit,
    onViewDetails: () -> Unit
) {
    val statusColor = when (visitor.checkInStatus) {
        ExpectedVisitorStatus.CHECKED_IN -> MaterialTheme.colorScheme.primary
        ExpectedVisitorStatus.CHECKED_OUT -> MaterialTheme.colorScheme.secondary
        ExpectedVisitorStatus.LATE -> MaterialTheme.colorScheme.error
        ExpectedVisitorStatus.NO_SHOW -> MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
        ExpectedVisitorStatus.NOT_ARRIVED -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        onClick = onViewDetails,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = visitor.visitorName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Visiting: ${visitor.beneficiaryName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                visitor.beneficiaryRoom?.let { room ->
                    Text(
                        text = "Room $room",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = visitor.checkInStatus.name.replace('_', ' '),
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    fontWeight = FontWeight.Medium
                )
            }

            if (visitor.checkInStatus == ExpectedVisitorStatus.NOT_ARRIVED ||
                visitor.checkInStatus == ExpectedVisitorStatus.LATE
            ) {
                if (isCheckingIn) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    OutlinedButton(
                        onClick = onQuickCheckIn,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Check In", style = MaterialTheme.typography.labelSmall)
                    }
                }
            } else {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "View details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyVisitorsContent(filter: VisitorStatusFilter) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = when (filter) {
                    VisitorStatusFilter.ALL -> "No visitors scheduled for today"
                    VisitorStatusFilter.UPCOMING -> "No upcoming visitors"
                    VisitorStatusFilter.CHECKED_IN -> "No one is currently checked in"
                    VisitorStatusFilter.LATE -> "No late visitors"
                    VisitorStatusFilter.NO_SHOW -> "No no-shows recorded"
                    VisitorStatusFilter.CHECKED_OUT -> "No completed visits yet"
                },
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
