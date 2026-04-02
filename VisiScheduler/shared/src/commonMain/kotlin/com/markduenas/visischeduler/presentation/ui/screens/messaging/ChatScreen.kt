package com.markduenas.visischeduler.presentation.ui.screens.messaging

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.markduenas.visischeduler.domain.entities.Message
import com.markduenas.visischeduler.presentation.ui.components.messaging.DateSeparator
import com.markduenas.visischeduler.presentation.ui.components.messaging.MessageBubble
import com.markduenas.visischeduler.presentation.ui.components.messaging.MessageInput
import com.markduenas.visischeduler.presentation.ui.components.messaging.SystemMessage
import com.markduenas.visischeduler.presentation.ui.components.messaging.TypingIndicator
import com.markduenas.visischeduler.presentation.viewmodel.messaging.ChatViewModel
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Screen for viewing and sending messages in a conversation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    currentUserId: String,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val sheetState = rememberModalBottomSheetState()

    // Scroll to bottom when new messages arrive
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            ChatTopBar(
                title = uiState.conversationTitle,
                subtitle = if (uiState.isTyping) uiState.typingText else null,
                onBackClick = { viewModel.goBack() },
                onSearchClick = { /* Toggle search mode */ },
                onInfoClick = { viewModel.openConversationInfo() }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
            ) {
                // Reply preview
                if (uiState.hasReply) {
                    ReplyPreview(
                        replyToMessage = uiState.replyToMessage!!,
                        onDismiss = { viewModel.clearReplyToMessage() }
                    )
                }

                // Message input
                MessageInput(
                    value = uiState.inputText,
                    onValueChange = { viewModel.onInputTextChanged(it) },
                    onSend = { viewModel.sendMessage() },
                    isSending = uiState.isSending,
                    enabled = !uiState.isSending,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading && uiState.messages.isEmpty() -> {
                    LoadingContent()
                }
                uiState.isEmpty -> {
                    EmptyChatContent()
                }
                uiState.error != null -> {
                    ErrorContent(
                        message = uiState.error!!,
                        onRetry = { viewModel.refresh() }
                    )
                }
                else -> {
                    MessagesList(
                        messages = uiState.messages,
                        currentUserId = currentUserId,
                        isLoadingMore = uiState.isLoadingMore,
                        typingUsers = uiState.typingUsers,
                        onMessageLongClick = { viewModel.showMessageActions(it) },
                        onLoadMore = { viewModel.loadMoreMessages() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    // Message actions bottom sheet
    if (uiState.showMessageActions && uiState.selectedMessage != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissMessageActions() },
            sheetState = sheetState
        ) {
            MessageActionsContent(
                message = uiState.selectedMessage!!,
                isOwnMessage = uiState.selectedMessage!!.senderId == currentUserId,
                onReply = {
                    viewModel.setReplyToMessage(uiState.selectedMessage!!)
                    viewModel.dismissMessageActions()
                },
                onCopy = {
                    viewModel.copyMessageContent(uiState.selectedMessage!!)
                    viewModel.dismissMessageActions()
                },
                onDelete = {
                    viewModel.deleteMessage(uiState.selectedMessage!!.id)
                    viewModel.dismissMessageActions()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopBar(
    title: String,
    subtitle: String?,
    onBackClick: () -> Unit,
    onSearchClick: () -> Unit,
    onInfoClick: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search"
                )
            }
            IconButton(onClick = onInfoClick) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Info"
                )
            }
        }
    )
}

@Composable
private fun MessagesList(
    messages: List<Message>,
    currentUserId: String,
    isLoadingMore: Boolean,
    typingUsers: List<String>,
    onMessageLongClick: (Message) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Group messages by date
    val groupedMessages = groupMessagesByDate(messages)

    // Detect when scrolled to top for loading more
    LaunchedEffect(listState.firstVisibleItemIndex) {
        if (listState.firstVisibleItemIndex == 0 && !isLoadingMore) {
            onLoadMore()
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        reverseLayout = false
    ) {
        // Loading indicator at top
        if (isLoadingMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        }

        groupedMessages.forEach { (date, messagesForDate) ->
            // Date separator
            item {
                DateSeparator(date = date)
            }

            // Messages for this date
            items(
                items = messagesForDate,
                key = { it.id }
            ) { message ->
                if (message.isSystemMessage) {
                    SystemMessage(
                        message = message,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    val isOwn = message.senderId == currentUserId
                    MessageBubble(
                        message = message,
                        isOwn = isOwn,
                        onLongClick = { onMessageLongClick(message) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Typing indicator
        if (typingUsers.isNotEmpty()) {
            item {
                TypingIndicator(
                    modifier = Modifier.padding(start = 8.dp, top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun ReplyPreview(
    replyToMessage: Message,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(40.dp)
                .background(MaterialTheme.colorScheme.primary)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Replying to ${replyToMessage.senderName}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = replyToMessage.content,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Cancel reply",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun MessageActionsContent(
    message: Message,
    isOwnMessage: Boolean,
    onReply: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Message actions",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Reply
        ActionItem(
            icon = Icons.AutoMirrored.Filled.Reply,
            text = "Reply",
            onClick = onReply
        )

        // Copy
        ActionItem(
            icon = Icons.Default.ContentCopy,
            text = "Copy",
            onClick = onCopy
        )

        // Delete (only for own messages)
        if (isOwnMessage) {
            ActionItem(
                icon = Icons.Default.Delete,
                text = "Delete",
                onClick = onDelete,
                isDestructive = true
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun ActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.TextButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
        }
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
private fun EmptyChatContent() {
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
            text = "No messages yet",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Send a message to start the conversation",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )
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
            text = "Failed to load messages",
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

/**
 * Group messages by date for display with date separators.
 */
private fun groupMessagesByDate(messages: List<Message>): Map<LocalDate, List<Message>> {
    return messages.groupBy { message ->
        message.timestamp.toLocalDateTime(TimeZone.currentSystemDefault()).date
    }
}
