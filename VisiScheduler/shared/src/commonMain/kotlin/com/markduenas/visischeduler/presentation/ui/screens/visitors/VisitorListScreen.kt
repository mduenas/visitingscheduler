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
import com.markduenas.visischeduler.presentation.ui.components.visitors.VisitorListItem

data class VisitorItem(
    val id: String,
    val name: String,
    val initials: String,
    val relationship: String,
    val status: VisitorStatus,
    val lastVisit: String?
)

enum class VisitorStatus {
    APPROVED, PENDING, BLOCKED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitorListScreen(
    onNavigateBack: () -> Unit,
    onAddVisitor: () -> Unit,
    onViewVisitor: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }

    val tabs = listOf("Approved", "Pending", "Blocked")

    // Mock data
    val allVisitors = remember {
        listOf(
            VisitorItem("1", "John Smith", "JS", "Son", VisitorStatus.APPROVED, "Jan 20, 2026"),
            VisitorItem("2", "Jane Smith", "JS", "Daughter", VisitorStatus.APPROVED, "Jan 18, 2026"),
            VisitorItem("3", "Bob Wilson", "BW", "Friend", VisitorStatus.APPROVED, "Jan 15, 2026"),
            VisitorItem("4", "Alice Brown", "AB", "Caregiver", VisitorStatus.APPROVED, null),
            VisitorItem("5", "Tom Davis", "TD", "Nephew", VisitorStatus.PENDING, null),
            VisitorItem("6", "Sarah Miller", "SM", "Friend", VisitorStatus.PENDING, null),
            VisitorItem("7", "Mike Johnson", "MJ", "Unknown", VisitorStatus.BLOCKED, null)
        )
    }

    val filteredVisitors = allVisitors.filter { visitor ->
        val matchesTab = when (selectedTab) {
            0 -> visitor.status == VisitorStatus.APPROVED
            1 -> visitor.status == VisitorStatus.PENDING
            2 -> visitor.status == VisitorStatus.BLOCKED
            else -> true
        }
        val matchesSearch = searchQuery.isEmpty() ||
                visitor.name.contains(searchQuery, ignoreCase = true)
        matchesTab && matchesSearch
    }

    Scaffold(
        topBar = {
            if (showSearch) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSearch = { },
                    active = false,
                    onActiveChange = { },
                    placeholder = { Text("Search visitors...") },
                    leadingIcon = {
                        IconButton(onClick = {
                            showSearch = false
                            searchQuery = ""
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close search")
                        }
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
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
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    val count = allVisitors.count { visitor ->
                        when (index) {
                            0 -> visitor.status == VisitorStatus.APPROVED
                            1 -> visitor.status == VisitorStatus.PENDING
                            2 -> visitor.status == VisitorStatus.BLOCKED
                            else -> false
                        }
                    }
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
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

            if (filteredVisitors.isEmpty()) {
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
                            when (selectedTab) {
                                0 -> Icons.Default.People
                                1 -> Icons.Default.HourglassEmpty
                                else -> Icons.Default.Block
                            },
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = when (selectedTab) {
                                0 -> "No approved visitors"
                                1 -> "No pending requests"
                                else -> "No blocked visitors"
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (selectedTab == 0) {
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
                    items(filteredVisitors) { visitor ->
                        VisitorListItem(
                            name = visitor.name,
                            initials = visitor.initials,
                            relationship = visitor.relationship,
                            subtitle = visitor.lastVisit?.let { "Last visit: $it" },
                            onClick = { onViewVisitor(visitor.id) },
                            trailingContent = {
                                when (visitor.status) {
                                    VisitorStatus.PENDING -> {
                                        Row {
                                            IconButton(onClick = { /* Deny */ }) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Deny",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                            IconButton(onClick = { /* Approve */ }) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = "Approve",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                    VisitorStatus.BLOCKED -> {
                                        TextButton(onClick = { /* Unblock */ }) {
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
}
