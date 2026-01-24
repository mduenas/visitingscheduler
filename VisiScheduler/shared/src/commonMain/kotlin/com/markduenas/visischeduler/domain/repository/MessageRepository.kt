package com.markduenas.visischeduler.domain.repository

import com.markduenas.visischeduler.domain.entities.Contact
import com.markduenas.visischeduler.domain.entities.Conversation
import com.markduenas.visischeduler.domain.entities.Message
import com.markduenas.visischeduler.domain.entities.TypingStatus
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for messaging operations.
 */
interface MessageRepository {

    // ==================== Conversations ====================

    /**
     * Get all conversations for the current user.
     */
    fun getConversations(): Flow<List<Conversation>>

    /**
     * Get archived conversations for the current user.
     */
    fun getArchivedConversations(): Flow<List<Conversation>>

    /**
     * Get a specific conversation by ID.
     */
    suspend fun getConversationById(conversationId: String): Result<Conversation>

    /**
     * Get conversation by beneficiary ID.
     */
    suspend fun getConversationByBeneficiaryId(beneficiaryId: String): Result<Conversation?>

    /**
     * Create a new conversation with the specified participants.
     * @param participantIds List of user IDs to include in the conversation
     * @param beneficiaryId The beneficiary this conversation is about
     * @param title Optional title for group conversations
     * @return Result containing the created Conversation
     */
    suspend fun createConversation(
        participantIds: List<String>,
        beneficiaryId: String,
        title: String? = null
    ): Result<Conversation>

    /**
     * Update conversation settings.
     */
    suspend fun updateConversation(
        conversationId: String,
        title: String? = null,
        isPinned: Boolean? = null,
        isMuted: Boolean? = null
    ): Result<Conversation>

    /**
     * Archive a conversation.
     */
    suspend fun archiveConversation(conversationId: String): Result<Unit>

    /**
     * Unarchive a conversation.
     */
    suspend fun unarchiveConversation(conversationId: String): Result<Unit>

    /**
     * Delete a conversation (soft delete).
     */
    suspend fun deleteConversation(conversationId: String): Result<Unit>

    /**
     * Add participants to an existing conversation.
     */
    suspend fun addParticipants(
        conversationId: String,
        participantIds: List<String>
    ): Result<Conversation>

    /**
     * Remove a participant from a conversation.
     */
    suspend fun removeParticipant(
        conversationId: String,
        participantId: String
    ): Result<Conversation>

    /**
     * Leave a conversation.
     */
    suspend fun leaveConversation(conversationId: String): Result<Unit>

    // ==================== Messages ====================

    /**
     * Get messages for a conversation.
     */
    fun getMessages(conversationId: String): Flow<List<Message>>

    /**
     * Get messages for a conversation with pagination.
     * @param conversationId The conversation ID
     * @param limit Number of messages to fetch
     * @param beforeMessageId Fetch messages before this message ID (for pagination)
     */
    suspend fun getMessagesPaginated(
        conversationId: String,
        limit: Int = 50,
        beforeMessageId: String? = null
    ): Result<List<Message>>

    /**
     * Get a specific message by ID.
     */
    suspend fun getMessageById(messageId: String): Result<Message>

    /**
     * Send a message to a conversation.
     * @param conversationId The conversation to send to
     * @param content The message content
     * @param replyToMessageId Optional message ID to reply to
     * @return Result containing the sent Message
     */
    suspend fun sendMessage(
        conversationId: String,
        content: String,
        replyToMessageId: String? = null
    ): Result<Message>

    /**
     * Send a message with attachment.
     */
    suspend fun sendMessageWithAttachment(
        conversationId: String,
        content: String?,
        attachmentPath: String,
        attachmentType: String
    ): Result<Message>

    /**
     * Edit a message.
     */
    suspend fun editMessage(messageId: String, newContent: String): Result<Message>

    /**
     * Delete a message (soft delete).
     */
    suspend fun deleteMessage(messageId: String): Result<Unit>

    /**
     * Mark all messages in a conversation as read.
     * @param conversationId The conversation ID
     */
    suspend fun markAsRead(conversationId: String): Result<Unit>

    /**
     * Mark specific messages as read.
     */
    suspend fun markMessagesAsRead(messageIds: List<String>): Result<Unit>

    /**
     * Search messages within a conversation.
     */
    suspend fun searchMessages(
        conversationId: String,
        query: String
    ): Result<List<Message>>

    /**
     * Search all messages across conversations.
     */
    suspend fun searchAllMessages(query: String): Result<List<Message>>

    // ==================== Typing Indicators ====================

    /**
     * Observe typing status for a conversation.
     */
    fun observeTypingStatus(conversationId: String): Flow<List<TypingStatus>>

    /**
     * Send typing indicator.
     */
    suspend fun sendTypingIndicator(
        conversationId: String,
        isTyping: Boolean
    ): Result<Unit>

    // ==================== Contacts ====================

    /**
     * Get available contacts that can be messaged.
     */
    fun getContacts(): Flow<List<Contact>>

    /**
     * Search contacts by name or email.
     */
    suspend fun searchContacts(query: String): Result<List<Contact>>

    /**
     * Get recent contacts.
     */
    suspend fun getRecentContacts(limit: Int = 10): Result<List<Contact>>

    // ==================== Notifications ====================

    /**
     * Get total unread message count across all conversations.
     */
    fun getUnreadCount(): Flow<Int>

    /**
     * Register for push notifications.
     */
    suspend fun registerForPushNotifications(token: String): Result<Unit>

    /**
     * Unregister from push notifications.
     */
    suspend fun unregisterFromPushNotifications(): Result<Unit>

    // ==================== Sync ====================

    /**
     * Sync messages from remote server.
     */
    suspend fun syncMessages(): Result<Unit>

    /**
     * Sync conversations from remote server.
     */
    suspend fun syncConversations(): Result<Unit>
}
