package com.markduenas.visischeduler.presentation.viewmodel.messaging

import com.markduenas.visischeduler.domain.entities.Conversation
import com.markduenas.visischeduler.domain.repository.MessageRepository
import com.markduenas.visischeduler.domain.usecase.ConversationFilter
import com.markduenas.visischeduler.domain.usecase.ConversationSortBy
import com.markduenas.visischeduler.domain.usecase.GetConversationsUseCase
import com.markduenas.visischeduler.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * ViewModel for the conversations list screen.
 */
class ConversationsViewModel(
    private val getConversationsUseCase: GetConversationsUseCase,
    private val messageRepository: MessageRepository
) : BaseViewModel<ConversationsUiState>(ConversationsUiState()) {

    private var conversationsJob: Job? = null

    init {
        loadConversations()
        observeUnreadCount()
    }

    /**
     * Load conversations with current filter settings.
     */
    fun loadConversations() {
        conversationsJob?.cancel()
        conversationsJob = viewModelScope.launch {
            updateState { copy(isLoading = true, error = null) }

            try {
                val filter = ConversationFilter(
                    searchQuery = currentState.searchQuery,
                    unreadOnly = currentState.filterUnreadOnly,
                    sortBy = currentState.sortBy,
                    includeArchived = false
                )

                getConversationsUseCase(filter).collectLatest { conversations ->
                    updateState {
                        copy(
                            conversations = conversations,
                            isLoading = false,
                            isEmpty = conversations.isEmpty()
                        )
                    }
                }
            } catch (e: Exception) {
                updateState {
                    copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load conversations"
                    )
                }
            }
        }
    }

    /**
     * Observe total unread message count.
     */
    private fun observeUnreadCount() {
        viewModelScope.launch {
            messageRepository.getUnreadCount().collectLatest { count ->
                updateState { copy(totalUnreadCount = count) }
            }
        }
    }

    /**
     * Search conversations by query.
     */
    fun onSearchQueryChanged(query: String) {
        updateState { copy(searchQuery = query) }
        loadConversations()
    }

    /**
     * Clear search query.
     */
    fun clearSearch() {
        updateState { copy(searchQuery = "") }
        loadConversations()
    }

    /**
     * Toggle unread only filter.
     */
    fun toggleUnreadFilter() {
        updateState { copy(filterUnreadOnly = !filterUnreadOnly) }
        loadConversations()
    }

    /**
     * Change sort order.
     */
    fun setSortBy(sortBy: ConversationSortBy) {
        updateState { copy(sortBy = sortBy) }
        loadConversations()
    }

    /**
     * Refresh conversations from server.
     */
    fun refresh() {
        viewModelScope.launch {
            updateState { copy(isRefreshing = true) }
            try {
                messageRepository.syncConversations()
                loadConversations()
            } catch (e: Exception) {
                showSnackbar("Failed to refresh: ${e.message}")
            } finally {
                updateState { copy(isRefreshing = false) }
            }
        }
    }

    /**
     * Pin a conversation.
     */
    fun pinConversation(conversationId: String) {
        viewModelScope.launch {
            messageRepository.updateConversation(
                conversationId = conversationId,
                isPinned = true
            ).onSuccess {
                showSnackbar("Conversation pinned")
            }.onFailure { e ->
                showSnackbar("Failed to pin: ${e.message}")
            }
        }
    }

    /**
     * Unpin a conversation.
     */
    fun unpinConversation(conversationId: String) {
        viewModelScope.launch {
            messageRepository.updateConversation(
                conversationId = conversationId,
                isPinned = false
            ).onSuccess {
                showSnackbar("Conversation unpinned")
            }.onFailure { e ->
                showSnackbar("Failed to unpin: ${e.message}")
            }
        }
    }

    /**
     * Mute a conversation.
     */
    fun muteConversation(conversationId: String) {
        viewModelScope.launch {
            messageRepository.updateConversation(
                conversationId = conversationId,
                isMuted = true
            ).onSuccess {
                showSnackbar("Conversation muted")
            }.onFailure { e ->
                showSnackbar("Failed to mute: ${e.message}")
            }
        }
    }

    /**
     * Unmute a conversation.
     */
    fun unmuteConversation(conversationId: String) {
        viewModelScope.launch {
            messageRepository.updateConversation(
                conversationId = conversationId,
                isMuted = false
            ).onSuccess {
                showSnackbar("Conversation unmuted")
            }.onFailure { e ->
                showSnackbar("Failed to unmute: ${e.message}")
            }
        }
    }

    /**
     * Archive a conversation.
     */
    fun archiveConversation(conversationId: String) {
        viewModelScope.launch {
            messageRepository.archiveConversation(conversationId)
                .onSuccess {
                    showSnackbar("Conversation archived", "Undo")
                    loadConversations()
                }
                .onFailure { e ->
                    showSnackbar("Failed to archive: ${e.message}")
                }
        }
    }

    /**
     * Delete a conversation.
     */
    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            messageRepository.deleteConversation(conversationId)
                .onSuccess {
                    showSnackbar("Conversation deleted")
                    loadConversations()
                }
                .onFailure { e ->
                    showSnackbar("Failed to delete: ${e.message}")
                }
        }
    }

    /**
     * Navigate to chat screen.
     */
    fun openConversation(conversationId: String) {
        navigate("chat/$conversationId")
    }

    /**
     * Navigate to new message screen.
     */
    fun startNewConversation() {
        navigate("new-message")
    }

    /**
     * Show/hide the sort options sheet.
     */
    fun toggleSortSheet() {
        updateState { copy(showSortSheet = !showSortSheet) }
    }

    override fun onCleared() {
        conversationsJob?.cancel()
        super.onCleared()
    }
}

/**
 * UI State for the conversations list screen.
 */
data class ConversationsUiState(
    val conversations: List<Conversation> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isEmpty: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val filterUnreadOnly: Boolean = false,
    val sortBy: ConversationSortBy = ConversationSortBy.LAST_MESSAGE,
    val totalUnreadCount: Int = 0,
    val showSortSheet: Boolean = false
) {
    val hasUnreadMessages: Boolean
        get() = totalUnreadCount > 0

    val isSearching: Boolean
        get() = searchQuery.isNotEmpty()
}
