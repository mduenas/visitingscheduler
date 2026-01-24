package com.markduenas.visischeduler.presentation.viewmodel.messaging

import com.markduenas.visischeduler.domain.entities.Conversation
import com.markduenas.visischeduler.domain.entities.Message
import com.markduenas.visischeduler.domain.entities.TypingStatus
import com.markduenas.visischeduler.domain.repository.MessageRepository
import com.markduenas.visischeduler.domain.usecase.SendMessageRequest
import com.markduenas.visischeduler.domain.usecase.SendMessageUseCase
import com.markduenas.visischeduler.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * ViewModel for the chat screen.
 */
class ChatViewModel(
    private val conversationId: String,
    private val messageRepository: MessageRepository,
    private val sendMessageUseCase: SendMessageUseCase
) : BaseViewModel<ChatUiState>(ChatUiState(conversationId = conversationId)) {

    private var messagesJob: Job? = null
    private var typingJob: Job? = null
    private var typingIndicatorJob: Job? = null

    init {
        loadConversation()
        loadMessages()
        observeTypingStatus()
    }

    /**
     * Load conversation details.
     */
    private fun loadConversation() {
        viewModelScope.launch {
            messageRepository.getConversationById(conversationId)
                .onSuccess { conversation ->
                    updateState { copy(conversation = conversation) }
                }
                .onFailure { e ->
                    showSnackbar("Failed to load conversation: ${e.message}")
                }
        }
    }

    /**
     * Load messages for the conversation.
     */
    private fun loadMessages() {
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            updateState { copy(isLoading = true) }

            try {
                messageRepository.getMessages(conversationId).collectLatest { messages ->
                    updateState {
                        copy(
                            messages = messages,
                            isLoading = false,
                            isEmpty = messages.isEmpty()
                        )
                    }
                    // Mark messages as read when loaded
                    markMessagesAsRead()
                }
            } catch (e: Exception) {
                updateState {
                    copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load messages"
                    )
                }
            }
        }
    }

    /**
     * Observe typing status from other participants.
     */
    private fun observeTypingStatus() {
        viewModelScope.launch {
            messageRepository.observeTypingStatus(conversationId).collectLatest { statuses ->
                val typingUsers = statuses.filter { it.isTyping }.map { it.userName }
                updateState { copy(typingUsers = typingUsers) }
            }
        }
    }

    /**
     * Mark all messages as read.
     */
    private fun markMessagesAsRead() {
        viewModelScope.launch {
            messageRepository.markAsRead(conversationId)
        }
    }

    /**
     * Update the message input text.
     */
    fun onInputTextChanged(text: String) {
        updateState { copy(inputText = text) }

        // Send typing indicator (debounced)
        typingJob?.cancel()
        typingJob = viewModelScope.launch {
            sendTypingIndicator(true)
            delay(TYPING_DEBOUNCE_MS)
            sendTypingIndicator(false)
        }
    }

    /**
     * Send typing indicator to server.
     */
    private suspend fun sendTypingIndicator(isTyping: Boolean) {
        messageRepository.sendTypingIndicator(conversationId, isTyping)
    }

    /**
     * Send the current message.
     */
    fun sendMessage() {
        val content = currentState.inputText.trim()
        if (content.isBlank()) return

        viewModelScope.launch {
            updateState { copy(isSending = true) }

            // Stop typing indicator
            sendTypingIndicator(false)

            val request = SendMessageRequest(
                conversationId = conversationId,
                content = content,
                replyToMessageId = currentState.replyToMessage?.id
            )

            sendMessageUseCase(request)
                .onSuccess { message ->
                    updateState {
                        copy(
                            inputText = "",
                            isSending = false,
                            replyToMessage = null,
                            messages = messages + message
                        )
                    }
                }
                .onFailure { e ->
                    updateState { copy(isSending = false) }
                    showSnackbar("Failed to send: ${e.message}")
                }
        }
    }

    /**
     * Set a message to reply to.
     */
    fun setReplyToMessage(message: Message) {
        updateState { copy(replyToMessage = message) }
    }

    /**
     * Clear reply to message.
     */
    fun clearReplyToMessage() {
        updateState { copy(replyToMessage = null) }
    }

    /**
     * Delete a message.
     */
    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            messageRepository.deleteMessage(messageId)
                .onSuccess {
                    updateState {
                        copy(messages = messages.filter { it.id != messageId })
                    }
                    showSnackbar("Message deleted")
                }
                .onFailure { e ->
                    showSnackbar("Failed to delete: ${e.message}")
                }
        }
    }

    /**
     * Copy message content to clipboard.
     */
    fun copyMessageContent(message: Message) {
        updateState { copy(clipboardContent = message.content) }
        showSnackbar("Message copied")
    }

    /**
     * Search messages in the conversation.
     */
    fun searchMessages(query: String) {
        if (query.isBlank()) {
            updateState { copy(searchResults = null, searchQuery = "") }
            return
        }

        updateState { copy(searchQuery = query, isSearching = true) }

        viewModelScope.launch {
            messageRepository.searchMessages(conversationId, query)
                .onSuccess { results ->
                    updateState {
                        copy(
                            searchResults = results,
                            isSearching = false
                        )
                    }
                }
                .onFailure {
                    updateState { copy(isSearching = false) }
                }
        }
    }

    /**
     * Clear search.
     */
    fun clearSearch() {
        updateState { copy(searchResults = null, searchQuery = "", isSearching = false) }
    }

    /**
     * Load more messages (pagination).
     */
    fun loadMoreMessages() {
        if (currentState.isLoadingMore || !currentState.hasMoreMessages) return

        val oldestMessage = currentState.messages.firstOrNull() ?: return

        viewModelScope.launch {
            updateState { copy(isLoadingMore = true) }

            messageRepository.getMessagesPaginated(
                conversationId = conversationId,
                limit = PAGE_SIZE,
                beforeMessageId = oldestMessage.id
            ).onSuccess { olderMessages ->
                updateState {
                    copy(
                        messages = olderMessages + messages,
                        isLoadingMore = false,
                        hasMoreMessages = olderMessages.size == PAGE_SIZE
                    )
                }
            }.onFailure {
                updateState { copy(isLoadingMore = false) }
            }
        }
    }

    /**
     * Refresh messages.
     */
    fun refresh() {
        viewModelScope.launch {
            updateState { copy(isRefreshing = true) }
            try {
                messageRepository.syncMessages()
                loadMessages()
            } catch (e: Exception) {
                showSnackbar("Failed to refresh: ${e.message}")
            } finally {
                updateState { copy(isRefreshing = false) }
            }
        }
    }

    /**
     * Navigate back.
     */
    fun goBack() {
        navigateBack()
    }

    /**
     * Open conversation info/settings.
     */
    fun openConversationInfo() {
        navigate("conversation-info/$conversationId")
    }

    /**
     * Show/hide message actions sheet.
     */
    fun showMessageActions(message: Message?) {
        updateState { copy(selectedMessage = message) }
    }

    /**
     * Dismiss message actions sheet.
     */
    fun dismissMessageActions() {
        updateState { copy(selectedMessage = null) }
    }

    override fun onCleared() {
        messagesJob?.cancel()
        typingJob?.cancel()
        typingIndicatorJob?.cancel()
        // Send final typing indicator off
        viewModelScope.launch {
            sendTypingIndicator(false)
        }
        super.onCleared()
    }

    companion object {
        private const val TYPING_DEBOUNCE_MS = 3000L
        private const val PAGE_SIZE = 50
    }
}

/**
 * UI State for the chat screen.
 */
data class ChatUiState(
    val conversationId: String,
    val conversation: Conversation? = null,
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isSending: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isEmpty: Boolean = false,
    val hasMoreMessages: Boolean = true,
    val error: String? = null,
    val typingUsers: List<String> = emptyList(),
    val replyToMessage: Message? = null,
    val selectedMessage: Message? = null,
    val searchQuery: String = "",
    val searchResults: List<Message>? = null,
    val isSearching: Boolean = false,
    val clipboardContent: String? = null
) {
    val conversationTitle: String
        get() = conversation?.displayName ?: "Chat"

    val canSend: Boolean
        get() = inputText.isNotBlank() && !isSending

    val isTyping: Boolean
        get() = typingUsers.isNotEmpty()

    val typingText: String
        get() = when {
            typingUsers.isEmpty() -> ""
            typingUsers.size == 1 -> "${typingUsers[0]} is typing..."
            typingUsers.size == 2 -> "${typingUsers[0]} and ${typingUsers[1]} are typing..."
            else -> "${typingUsers[0]} and ${typingUsers.size - 1} others are typing..."
        }

    val hasReply: Boolean
        get() = replyToMessage != null

    val showMessageActions: Boolean
        get() = selectedMessage != null

    val isInSearchMode: Boolean
        get() = searchResults != null
}
