package com.markduenas.visischeduler.domain.usecase

import com.markduenas.visischeduler.domain.entities.Conversation
import com.markduenas.visischeduler.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Use case for retrieving conversations.
 */
class GetConversationsUseCase(
    private val messageRepository: MessageRepository
) {
    /**
     * Get all active conversations for the current user.
     * @param filter Optional filter to apply
     * @return Flow of filtered and sorted conversations
     */
    operator fun invoke(filter: ConversationFilter = ConversationFilter()): Flow<List<Conversation>> {
        return messageRepository.getConversations().map { conversations ->
            var filtered = conversations

            // Filter by search query
            if (filter.searchQuery.isNotBlank()) {
                val query = filter.searchQuery.lowercase()
                filtered = filtered.filter { conversation ->
                    conversation.displayName.lowercase().contains(query) ||
                        conversation.lastMessage?.content?.lowercase()?.contains(query) == true ||
                        conversation.beneficiaryName?.lowercase()?.contains(query) == true
                }
            }

            // Filter unread only
            if (filter.unreadOnly) {
                filtered = filtered.filter { it.hasUnreadMessages }
            }

            // Filter by beneficiary
            if (filter.beneficiaryId != null) {
                filtered = filtered.filter { it.beneficiaryId == filter.beneficiaryId }
            }

            // Exclude archived unless specifically requested
            if (!filter.includeArchived) {
                filtered = filtered.filter { !it.isArchived }
            }

            // Sort conversations
            filtered = when (filter.sortBy) {
                ConversationSortBy.LAST_MESSAGE -> {
                    filtered.sortedByDescending { it.lastMessage?.timestamp }
                }
                ConversationSortBy.UNREAD_COUNT -> {
                    filtered.sortedByDescending { it.unreadCount }
                }
                ConversationSortBy.NAME -> {
                    filtered.sortedBy { it.displayName.lowercase() }
                }
                ConversationSortBy.CREATED_DATE -> {
                    filtered.sortedByDescending { it.createdAt }
                }
            }

            // Pinned conversations always come first
            filtered.sortedByDescending { it.isPinned }
        }
    }

    /**
     * Get archived conversations.
     */
    fun getArchived(): Flow<List<Conversation>> {
        return messageRepository.getArchivedConversations()
    }
}

/**
 * Filter options for conversations.
 */
data class ConversationFilter(
    val searchQuery: String = "",
    val unreadOnly: Boolean = false,
    val beneficiaryId: String? = null,
    val includeArchived: Boolean = false,
    val sortBy: ConversationSortBy = ConversationSortBy.LAST_MESSAGE
)

/**
 * Sort options for conversations.
 */
enum class ConversationSortBy {
    LAST_MESSAGE,
    UNREAD_COUNT,
    NAME,
    CREATED_DATE
}
