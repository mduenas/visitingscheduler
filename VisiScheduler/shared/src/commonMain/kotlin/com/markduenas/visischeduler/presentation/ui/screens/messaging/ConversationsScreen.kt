package com.markduenas.visischeduler.presentation.ui.screens.messaging

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.markduenas.visischeduler.domain.entities.Conversation
import com.markduenas.visischeduler.domain.usecase.ConversationSortBy
import kotlinx.datetime.toLocalDateTime
import com.markduenas.visischeduler.presentation.ui.components.messaging.ConversationItem
import com.markduenas.visischeduler.presentation.viewmodel.messaging.ConversationsViewModel

/**
 * Screen displaying the list of conversations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsScreen(
    viewModel: ConversationsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState()

    Scaffold(
        modifier = modifier,
        topBar = {
            ConversationsTopBar(
                totalUnreadCount = uiState.totalUnreadCount,
                onFilterClick = { viewModel.toggleUnreadFilter() },
                onSortClick = { viewModel.toggleSortSheet() },
                isFilterActive = uiState.filterUnreadOnly
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.startNewConversation() }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New message"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar
            ConversationsSearchBar(
                query = uiState.searchQuery,
                onQueryChange = { viewModel.onSearchQueryChanged(it) },
                onClear = { viewModel.clearSearch() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Content
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    uiState.isLoading && uiState.conversations.isEmpty() -> {
                        LoadingContent()
                    }
                    uiState.isEmpty -> {
                        EmptyConversationsContent(
                            isSearching = uiState.isSearching,
                            onStartConversation = { viewModel.startNewConversation() }
                        )
                    }
                    uiState.error != null -> {
                        ErrorContent(
                            message = uiState.error!!,
                            onRetry = { viewModel.loadConversations() }
                        )
                    }
                    else -> {
                        ConversationsList(
                            conversations = uiState.conversations,
                            onConversationClick = { viewModel.openConversation(it.id) },
                            onPinClick = { conversation ->
                                if (conversation.isPinned) {
                                    viewModel.unpinConversation(conversation.id)
                                } else {
                                    viewModel.pinConversation(conversation.id)
                                }
                            },
                            onMuteClick = { conversation ->
                                if (conversation.isMuted) {
                                    viewModel.unmuteConversation(conversation.id)
                                } else {
                                    viewModel.muteConversation(conversation.id)
                                }
                            },
                            onArchiveClick = { viewModel.archiveConversation(it.id) },
                            onDeleteClick = { viewModel.deleteConversation(it.id) }
                        )
                    }
                }
            }
        }
    }

    // Sort options bottom sheet
    if (uiState.showSortSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.toggleSortSheet() },
            sheetState = sheetState
        ) {
            SortOptionsContent(
                currentSortBy = uiState.sortBy,
                onSortSelected = { sortBy ->
                    viewModel.setSortBy(sortBy)
                    viewModel.toggleSortSheet()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationsTopBar(
    totalUnreadCount: Int,
    onFilterClick: () -> Unit,
    onSortClick: () -> Unit,
    isFilterActive: Boolean
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Messages")
                if (totalUnreadCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Badge {
                        Text(
                            text = if (totalUnreadCount > 99) "99+" else totalUnreadCount.toString()
                        )
                    }
                }
            }
        },
        actions = {
            IconButton(onClick = onFilterClick) {
                Icon(
                    imageVector = if (isFilterActive) Icons.Default.MarkEmailUnread else Icons.Default.FilterList,
                    contentDescription = "Filter",
                    tint = if (isFilterActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = onSortClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Sort,
                    contentDescription = "Sort"
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationsSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    @Suppress("DEPRECATION")
    SearchBar(
        query = query,
        onQueryChange = onQueryChange,
        onSearch = {},
        active = false,
        onActiveChange = {},
        placeholder = { Text("Search conversations") },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                }
            }
        },
        modifier = modifier
    ) {}
}

@Composable
private fun ConversationsList(
    conversations: List<Conversation>,
    onConversationClick: (Conversation) -> Unit,
    onPinClick: (Conversation) -> Unit,
    onMuteClick: (Conversation) -> Unit,
    onArchiveClick: (Conversation) -> Unit,
    onDeleteClick: (Conversation) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(
            items = conversations,
            key = { it.id }
        ) { conversation ->
            var showMenu by remember { mutableStateOf(false) }

            Box {
                ConversationItem(
                    name = conversation.displayName,
                    lastMessage = conversation.lastMessage?.content ?: "",
                    timestamp = conversation.lastMessage?.let {
                        val zone = kotlinx.datetime.TimeZone.currentSystemDefault()
                        val dateTime = it.timestamp.toLocalDateTime(zone)
                        val hour = dateTime.hour
                        val minute = dateTime.minute
                        val period = if (hour < 12) "AM" else "PM"
                        val displayHour = when {
                            hour == 0 -> 12
                            hour > 12 -> hour - 12
                            else -> hour
                        }
                        "$displayHour:${minute.toString().padStart(2, '0')} $period"
                    } ?: "",
                    unreadCount = conversation.unreadCount,
                    avatarInitials = conversation.displayName.split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString(""),
                    onClick = { onConversationClick(conversation) },
                    modifier = Modifier.fillMaxWidth()
                )

                ConversationContextMenu(
                    expanded = showMenu,
                    onDismiss = { showMenu = false },
                    conversation = conversation,
                    onPinClick = { onPinClick(conversation) },
                    onMuteClick = { onMuteClick(conversation) },
                    onArchiveClick = { onArchiveClick(conversation) },
                    onDeleteClick = { onDeleteClick(conversation) }
                )
            }
        }
    }
}

@Composable
private fun ConversationContextMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    conversation: Conversation,
    onPinClick: () -> Unit,
    onMuteClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = { Text(if (conversation.isPinned) "Unpin" else "Pin") },
            onClick = {
                onPinClick()
                onDismiss()
            },
            leadingIcon = {
                Icon(Icons.Default.PushPin, contentDescription = null)
            }
        )
        DropdownMenuItem(
            text = { Text(if (conversation.isMuted) "Unmute" else "Mute") },
            onClick = {
                onMuteClick()
                onDismiss()
            },
            leadingIcon = {
                Icon(
                    if (conversation.isMuted) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                    contentDescription = null
                )
            }
        )
        DropdownMenuItem(
            text = { Text("Archive") },
            onClick = {
                onArchiveClick()
                onDismiss()
            },
            leadingIcon = {
                Icon(Icons.Default.Archive, contentDescription = null)
            }
        )
        DropdownMenuItem(
            text = { Text("Delete") },
            onClick = {
                onDeleteClick()
                onDismiss()
            },
            leadingIcon = {
                Icon(Icons.Default.Delete, contentDescription = null)
            }
        )
    }
}

@Composable
private fun SortOptionsContent(
    currentSortBy: ConversationSortBy,
    onSortSelected: (ConversationSortBy) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Sort by",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        ConversationSortBy.entries.forEach { sortBy ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSortSelected(sortBy) }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (sortBy) {
                        ConversationSortBy.LAST_MESSAGE -> "Last message"
                        ConversationSortBy.UNREAD_COUNT -> "Unread count"
                        ConversationSortBy.NAME -> "Name"
                        ConversationSortBy.CREATED_DATE -> "Date created"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (sortBy == currentSortBy) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyConversationsContent(
    isSearching: Boolean,
    onStartConversation: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Message,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isSearching) "No conversations found" else "No messages yet",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isSearching) {
                "Try a different search term"
            } else {
                "Start a conversation with your care team"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )
        if (!isSearching) {
            Spacer(modifier = Modifier.height(24.dp))
            androidx.compose.material3.Button(onClick = onStartConversation) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("New Message")
            }
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Something went wrong",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        androidx.compose.material3.Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}
