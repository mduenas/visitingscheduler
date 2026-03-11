package com.markduenas.visischeduler.presentation.ui.screens.visitors

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.markduenas.visischeduler.domain.entities.User
import com.markduenas.visischeduler.presentation.ui.components.visitors.VisitorListItem
import com.markduenas.visischeduler.presentation.viewmodel.visitors.VisitorFilter
import com.markduenas.visischeduler.presentation.viewmodel.visitors.VisitorListViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitorListScreen(
    onNavigateBack: () -> Unit,
    onAddVisitor: () -> Unit,
    onViewVisitor: (String) -> Unit,
    viewModel: VisitorListViewModel = koinInject(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSearch by remember { mutableStateOf(false) }

    val tabs = listOf(
        VisitorFilter.APPROVED to "Approved",
        VisitorFilter.PENDING to "Pending",
        VisitorFilter.BLOCKED to "Blocked"
    )

    Scaffold(
        topBar = {
            if (showSearch) {
                @Suppress("DEPRECATION")
                SearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = { viewModel.onSearchQueryChange(it) },
                    onSearch = { },
                    active = false,
                    onActiveChange = { },
                    placeholder = { Text("Search visitors...") },
                    leadingIcon = {
                        IconButton(onClick = {
                            showSearch = false
                            viewModel.onSearchQueryChange("")
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close search")
                        }
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { }
            } else {
                TopAppBar(
                    title = { Text("Visitors") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showSearch = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddVisitor) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Add visitor")
            }
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tabs
            @Suppress("DEPRECATION")
            SecondaryTabRow(
                selectedTabIndex = tabs.indexOfFirst { it.first == uiState.selectedFilter }.coerceAtLeast(0)
            ) {
                tabs.forEach { (filter, title) ->
                    val count = when (filter) {
                        VisitorFilter.APPROVED -> uiState.approvedVisitors.size
                        VisitorFilter.PENDING -> uiState.pendingVisitors.size
                        VisitorFilter.BLOCKED -> uiState.blockedVisitors.size
                        else -> 0
                    }
                    Tab(
                        selected = uiState.selectedFilter == filter,
                        onClick = { viewModel.onFilterChange(filter) },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(title)
                                if (count > 0) {
                                    Badge { Text(count.toString()) }
                                }
                            }
                        }
                    )
                }
            }

            if (uiState.isLoading && !uiState.hasVisitors) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.isEmptyForFilter) {
                // Empty State
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            when (uiState.selectedFilter) {
                                VisitorFilter.APPROVED -> Icons.Default.People
                                VisitorFilter.PENDING -> Icons.Default.HourglassEmpty
                                else -> Icons.Default.Block
                            },
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = when (uiState.selectedFilter) {
                                VisitorFilter.APPROVED -> "No approved visitors"
                                VisitorFilter.PENDING -> "No pending requests"
                                else -> "No blocked visitors"
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (uiState.selectedFilter == VisitorFilter.APPROVED) {
                            Button(onClick = onAddVisitor) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Invite Visitor")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.filteredVisitors) { visitor ->
                        VisitorListItem(
                            name = visitor.fullName,
                            initials = "${visitor.firstName.firstOrNull() ?: ""}${visitor.lastName.firstOrNull() ?: ""}",
                            relationship = visitor.metadata["relationship"] ?: "Visitor",
                            subtitle = visitor.email,
                            onClick = { onViewVisitor(visitor.id) },
                            trailingContent = {
                                when (uiState.selectedFilter) {
                                    VisitorFilter.PENDING -> {
                                        Row {
                                            IconButton(onClick = { viewModel.denyVisitor(visitor.id, "Denied by coordinator") }) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Deny",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                            IconButton(onClick = { viewModel.approveVisitor(visitor.id) }) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = "Approve",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                    VisitorFilter.BLOCKED -> {
                                        TextButton(onClick = { viewModel.unblockVisitor(visitor.id) }) {
                                            Text("Unblock")
                                        }
                                    }
                                    else -> {
                                        Icon(
                                            Icons.Default.ChevronRight,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Error handling
    uiState.error?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Error") },
            text = { Text(error.message ?: "An unknown error occurred") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }
}
