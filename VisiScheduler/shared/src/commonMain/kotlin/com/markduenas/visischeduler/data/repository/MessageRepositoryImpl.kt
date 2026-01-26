package com.markduenas.visischeduler.data.repository

import com.markduenas.visischeduler.data.local.VisiSchedulerDatabase
import com.markduenas.visischeduler.data.remote.api.VisiSchedulerApi
import com.markduenas.visischeduler.data.remote.dto.CreateConversationRequestDto
import com.markduenas.visischeduler.data.remote.dto.EditMessageRequestDto
import com.markduenas.visischeduler.data.remote.dto.SendMessageRequestDto
import com.markduenas.visischeduler.data.remote.dto.UpdateConversationRequestDto
import com.markduenas.visischeduler.domain.entities.Contact
import com.markduenas.visischeduler.domain.entities.Conversation
import com.markduenas.visischeduler.domain.entities.ConversationParticipant
import com.markduenas.visischeduler.domain.entities.Message
import com.markduenas.visischeduler.domain.entities.MessageType
import com.markduenas.visischeduler.domain.entities.Role
import com.markduenas.visischeduler.domain.entities.TypingStatus
import com.markduenas.visischeduler.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Implementation of MessageRepository.
 */
class MessageRepositoryImpl(
    private val api: VisiSchedulerApi,
    private val database: VisiSchedulerDatabase,
    private val json: Json
) : MessageRepository {

    private val _unreadCount = MutableStateFlow(0)

    // ==================== Conversations ====================

    override fun getConversations(): Flow<List<Conversation>> = flow {
        // First emit cached data
        val cached = getCachedConversations()
        emit(cached)

        // Then fetch from API and update cache
        try {
            val conversations = api.getConversations().map { it.toDomain() }
            conversations.forEach { cacheConversation(it) }
            updateUnreadCount(conversations)
            emit(conversations)
        } catch (e: Exception) {
            // Keep cached data on error
        }
    }

    override fun getArchivedConversations(): Flow<List<Conversation>> = flow {
        // Emit cached archived conversations
        val cached = getCachedArchivedConversations()
        emit(cached)

        // Fetch from API
        try {
            val conversations = api.getArchivedConversations().map { it.toDomain() }
            emit(conversations)
        } catch (e: Exception) {
            // Keep cached data
        }
    }

    override suspend fun getConversationById(conversationId: String): Result<Conversation> {
        return try {
            val conversation = api.getConversationById(conversationId).toDomain()
            cacheConversation(conversation)
            Result.success(conversation)
        } catch (e: Exception) {
            // Try cache
            val cached = getCachedConversationById(conversationId)
            if (cached != null) {
                Result.success(cached)
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun getConversationByBeneficiaryId(beneficiaryId: String): Result<Conversation?> {
        return try {
            val conversation = api.getConversationByBeneficiaryId(beneficiaryId)?.toDomain()
            conversation?.let { cacheConversation(it) }
            Result.success(conversation)
        } catch (e: Exception) {
            // Try cache
            val cached = getCachedConversationByBeneficiaryId(beneficiaryId)
            Result.success(cached)
        }
    }

    override suspend fun createConversation(
        participantIds: List<String>,
        beneficiaryId: String,
        title: String?
    ): Result<Conversation> {
        return try {
            val request = CreateConversationRequestDto(
                participantIds = participantIds,
                beneficiaryId = beneficiaryId,
                title = title
            )
            val conversation = api.createConversation(request).toDomain()
            cacheConversation(conversation)
            Result.success(conversation)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateConversation(
        conversationId: String,
        title: String?,
        isPinned: Boolean?,
        isMuted: Boolean?
    ): Result<Conversation> {
        return try {
            val request = UpdateConversationRequestDto(
                title = title,
                isPinned = isPinned,
                isMuted = isMuted
            )
            val conversation = api.updateConversation(conversationId, request).toDomain()
            cacheConversation(conversation)
            Result.success(conversation)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun archiveConversation(conversationId: String): Result<Unit> {
        return try {
            api.archiveConversation(conversationId)
            // Update local cache
            database.visiSchedulerQueries.archiveConversation(
                archivedAt = Clock.System.now().toString(),
                id = conversationId
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unarchiveConversation(conversationId: String): Result<Unit> {
        return try {
            api.unarchiveConversation(conversationId)
            database.visiSchedulerQueries.unarchiveConversation(conversationId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteConversation(conversationId: String): Result<Unit> {
        return try {
            api.deleteConversation(conversationId)
            database.visiSchedulerQueries.deleteConversation(conversationId)
            database.visiSchedulerQueries.deleteMessagesForConversation(conversationId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addParticipants(
        conversationId: String,
        participantIds: List<String>
    ): Result<Conversation> {
        return try {
            val conversation = api.addConversationParticipants(conversationId, participantIds).toDomain()
            cacheConversation(conversation)
            Result.success(conversation)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeParticipant(
        conversationId: String,
        participantId: String
    ): Result<Conversation> {
        return try {
            val conversation = api.removeConversationParticipant(conversationId, participantId).toDomain()
            cacheConversation(conversation)
            Result.success(conversation)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun leaveConversation(conversationId: String): Result<Unit> {
        return try {
            api.leaveConversation(conversationId)
            database.visiSchedulerQueries.deleteConversation(conversationId)
            database.visiSchedulerQueries.deleteMessagesForConversation(conversationId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== Messages ====================

    override fun getMessages(conversationId: String): Flow<List<Message>> = flow {
        // First emit cached data
        val cached = getCachedMessages(conversationId)
        emit(cached)

        // Then fetch from API
        try {
            val messages = api.getMessages(conversationId).map { it.toDomain() }
            messages.forEach { cacheMessage(it) }
            emit(messages)
        } catch (e: Exception) {
            // Keep cached data
        }
    }

    override suspend fun getMessagesPaginated(
        conversationId: String,
        limit: Int,
        beforeMessageId: String?
    ): Result<List<Message>> {
        return try {
            val response = api.getMessagesPaginated(conversationId, limit, beforeMessageId)
            val messages = response.messages.map { it.toDomain() }
            messages.forEach { cacheMessage(it) }
            Result.success(messages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMessageById(messageId: String): Result<Message> {
        return try {
            val message = api.getMessageById(messageId).toDomain()
            cacheMessage(message)
            Result.success(message)
        } catch (e: Exception) {
            // Try cache
            val cached = getCachedMessageById(messageId)
            if (cached != null) {
                Result.success(cached)
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun sendMessage(
        conversationId: String,
        content: String,
        replyToMessageId: String?
    ): Result<Message> {
        return try {
            val request = SendMessageRequestDto(
                content = content,
                replyToMessageId = replyToMessageId
            )
            val message = api.sendMessage(conversationId, request).toDomain()
            cacheMessage(message)
            Result.success(message)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendMessageWithAttachment(
        conversationId: String,
        content: String?,
        attachmentPath: String,
        attachmentType: String
    ): Result<Message> {
        return try {
            val message = api.sendMessageWithAttachment(
                conversationId = conversationId,
                content = content,
                attachmentPath = attachmentPath,
                attachmentType = attachmentType
            ).toDomain()
            cacheMessage(message)
            Result.success(message)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun editMessage(messageId: String, newContent: String): Result<Message> {
        return try {
            val request = EditMessageRequestDto(content = newContent)
            val message = api.editMessage(messageId, request).toDomain()
            cacheMessage(message)
            Result.success(message)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteMessage(messageId: String): Result<Unit> {
        return try {
            api.deleteMessage(messageId)
            database.visiSchedulerQueries.deleteMessage(messageId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markAsRead(conversationId: String): Result<Unit> {
        return try {
            api.markConversationAsRead(conversationId)
            database.visiSchedulerQueries.markConversationMessagesAsRead(conversationId)
            database.visiSchedulerQueries.updateConversationUnreadCount(
                unreadCount = 0,
                id = conversationId
            )
            // Recalculate total unread count
            syncUnreadCount()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markMessagesAsRead(messageIds: List<String>): Result<Unit> {
        return try {
            api.markMessagesAsRead(messageIds)
            messageIds.forEach { messageId ->
                database.visiSchedulerQueries.markMessageAsRead(messageId)
            }
            syncUnreadCount()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchMessages(conversationId: String, query: String): Result<List<Message>> {
        return try {
            val messages = api.searchMessages(conversationId, query).map { it.toDomain() }
            Result.success(messages)
        } catch (e: Exception) {
            // Search in local cache
            val cached = getCachedMessages(conversationId)
                .filter { it.content.contains(query, ignoreCase = true) }
            Result.success(cached)
        }
    }

    override suspend fun searchAllMessages(query: String): Result<List<Message>> {
        return try {
            val messages = api.searchAllMessages(query).map { it.toDomain() }
            Result.success(messages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== Typing Indicators ====================

    override fun observeTypingStatus(conversationId: String): Flow<List<TypingStatus>> {
        // This would typically use WebSocket for real-time updates
        return flow {
            emit(emptyList())
        }
    }

    override suspend fun sendTypingIndicator(conversationId: String, isTyping: Boolean): Result<Unit> {
        return try {
            api.sendTypingIndicator(conversationId, isTyping)
            Result.success(Unit)
        } catch (e: Exception) {
            // Typing indicators are best-effort, don't fail
            Result.success(Unit)
        }
    }

    // ==================== Contacts ====================

    override fun getContacts(): Flow<List<Contact>> = flow {
        try {
            val contacts = api.getContacts().map { it.toDomain() }
            emit(contacts)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override suspend fun searchContacts(query: String): Result<List<Contact>> {
        return try {
            val contacts = api.searchContacts(query).map { it.toDomain() }
            Result.success(contacts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getRecentContacts(limit: Int): Result<List<Contact>> {
        return try {
            val contacts = api.getRecentContacts(limit).map { it.toDomain() }
            Result.success(contacts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== Notifications ====================

    override fun getUnreadCount(): Flow<Int> = _unreadCount

    override suspend fun registerForPushNotifications(token: String): Result<Unit> {
        return try {
            api.registerPushToken(token)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unregisterFromPushNotifications(): Result<Unit> {
        return try {
            api.unregisterPushToken()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== Sync ====================

    override suspend fun syncMessages(): Result<Unit> {
        return try {
            // Get all conversations first
            val conversations = api.getConversations().map { it.toDomain() }
            conversations.forEach { conversation ->
                val messages = api.getMessages(conversation.id).map { it.toDomain() }
                messages.forEach { cacheMessage(it) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncConversations(): Result<Unit> {
        return try {
            val conversations = api.getConversations().map { it.toDomain() }
            database.visiSchedulerQueries.deleteAllConversations()
            conversations.forEach { cacheConversation(it) }
            updateUnreadCount(conversations)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== Private Helper Methods ====================

    private fun getCachedConversations(): List<Conversation> {
        return database.visiSchedulerQueries
            .selectAllConversations()
            .executeAsList()
            .map { mapEntityToConversation(it) }
    }

    private fun getCachedArchivedConversations(): List<Conversation> {
        return database.visiSchedulerQueries
            .selectArchivedConversations()
            .executeAsList()
            .map { mapEntityToConversation(it) }
    }

    private fun getCachedConversationById(conversationId: String): Conversation? {
        return database.visiSchedulerQueries
            .selectConversationById(conversationId)
            .executeAsOneOrNull()
            ?.let { mapEntityToConversation(it) }
    }

    private fun getCachedConversationByBeneficiaryId(beneficiaryId: String): Conversation? {
        return database.visiSchedulerQueries
            .selectConversationByBeneficiaryId(beneficiaryId)
            .executeAsOneOrNull()
            ?.let { mapEntityToConversation(it) }
    }

    private fun getCachedMessages(conversationId: String): List<Message> {
        return database.visiSchedulerQueries
            .selectMessagesByConversationId(conversationId)
            .executeAsList()
            .map { mapEntityToMessage(it) }
    }

    private fun getCachedMessageById(messageId: String): Message? {
        return database.visiSchedulerQueries
            .selectMessageById(messageId)
            .executeAsOneOrNull()
            ?.let { mapEntityToMessage(it) }
    }

    private fun cacheConversation(conversation: Conversation) {
        database.visiSchedulerQueries.insertConversation(
            id = conversation.id,
            title = conversation.title,
            participants = json.encodeToString(conversation.participants.map {
                mapOf(
                    "userId" to it.userId,
                    "userName" to it.userName,
                    "userRole" to it.userRole.name,
                    "profileImageUrl" to it.profileImageUrl,
                    "isActive" to it.isActive.toString(),
                    "joinedAt" to it.joinedAt.toString()
                )
            }),
            lastMessageId = conversation.lastMessage?.id,
            unreadCount = conversation.unreadCount.toLong(),
            beneficiaryId = conversation.beneficiaryId,
            beneficiaryName = conversation.beneficiaryName,
            isGroupConversation = if (conversation.isGroupConversation) 1L else 0L,
            isPinned = if (conversation.isPinned) 1L else 0L,
            isMuted = if (conversation.isMuted) 1L else 0L,
            createdAt = conversation.createdAt.toString(),
            updatedAt = conversation.updatedAt.toString(),
            archivedAt = conversation.archivedAt?.toString()
        )

        // Cache the last message if present
        conversation.lastMessage?.let { cacheMessage(it) }
    }

    private fun cacheMessage(message: Message) {
        database.visiSchedulerQueries.insertMessage(
            id = message.id,
            conversationId = message.conversationId,
            senderId = message.senderId,
            senderName = message.senderName,
            content = message.content,
            timestamp = message.timestamp.toEpochMilliseconds(),
            isRead = if (message.isRead) 1L else 0L,
            type = message.type.name,
            metadata = message.metadata?.let { json.encodeToString(it) },
            attachmentUrl = message.attachmentUrl,
            replyToMessageId = message.replyToMessageId,
            editedAt = message.editedAt?.toString(),
            deletedAt = message.deletedAt?.toString()
        )
    }

    private fun mapEntityToConversation(entity: com.markduenas.visischeduler.data.local.ConversationEntity): Conversation {
        val participants: List<ConversationParticipant> = try {
            val participantMaps: List<Map<String, String?>> = json.decodeFromString(entity.participants)
            participantMaps.map { map ->
                ConversationParticipant(
                    userId = map["userId"] ?: "",
                    userName = map["userName"] ?: "",
                    userRole = Role.valueOf(map["userRole"] ?: "APPROVED_VISITOR"),
                    profileImageUrl = map["profileImageUrl"],
                    isActive = map["isActive"]?.toBoolean() ?: true,
                    joinedAt = Instant.parse(map["joinedAt"] ?: Clock.System.now().toString())
                )
            }
        } catch (e: Exception) {
            emptyList()
        }

        val lastMessage = entity.lastMessageId?.let { messageId ->
            getCachedMessageById(messageId)
        }

        return Conversation(
            id = entity.id,
            title = entity.title,
            participants = participants,
            lastMessage = lastMessage,
            unreadCount = entity.unreadCount.toInt(),
            beneficiaryId = entity.beneficiaryId,
            beneficiaryName = entity.beneficiaryName,
            isGroupConversation = entity.isGroupConversation == 1L,
            isPinned = entity.isPinned == 1L,
            isMuted = entity.isMuted == 1L,
            createdAt = Instant.parse(entity.createdAt),
            updatedAt = Instant.parse(entity.updatedAt),
            archivedAt = entity.archivedAt?.let { Instant.parse(it) }
        )
    }

    private fun mapEntityToConversation(entity: com.markduenas.visischeduler.data.local.SelectArchivedConversations): Conversation {
        val participants: List<ConversationParticipant> = try {
            val participantMaps: List<Map<String, String?>> = json.decodeFromString(entity.participants)
            participantMaps.map { map ->
                ConversationParticipant(
                    userId = map["userId"] ?: "",
                    userName = map["userName"] ?: "",
                    userRole = Role.valueOf(map["userRole"] ?: "APPROVED_VISITOR"),
                    profileImageUrl = map["profileImageUrl"],
                    isActive = map["isActive"]?.toBoolean() ?: true,
                    joinedAt = Instant.parse(map["joinedAt"] ?: Clock.System.now().toString())
                )
            }
        } catch (e: Exception) {
            emptyList()
        }

        val lastMessage = entity.lastMessageId?.let { messageId ->
            getCachedMessageById(messageId)
        }

        return Conversation(
            id = entity.id,
            title = entity.title,
            participants = participants,
            lastMessage = lastMessage,
            unreadCount = entity.unreadCount.toInt(),
            beneficiaryId = entity.beneficiaryId,
            beneficiaryName = entity.beneficiaryName,
            isGroupConversation = entity.isGroupConversation == 1L,
            isPinned = entity.isPinned == 1L,
            isMuted = entity.isMuted == 1L,
            createdAt = Instant.parse(entity.createdAt),
            updatedAt = Instant.parse(entity.updatedAt),
            archivedAt = entity.archivedAt?.let { Instant.parse(it) }
        )
    }

    private fun mapEntityToMessage(entity: com.markduenas.visischeduler.data.local.MessageEntity): Message {
        return Message(
            id = entity.id,
            conversationId = entity.conversationId,
            senderId = entity.senderId,
            senderName = entity.senderName,
            content = entity.content,
            timestamp = Instant.fromEpochMilliseconds(entity.timestamp),
            isRead = entity.isRead == 1L,
            type = MessageType.valueOf(entity.type),
            metadata = entity.metadata?.let { json.decodeFromString(it) },
            attachmentUrl = entity.attachmentUrl,
            replyToMessageId = entity.replyToMessageId,
            editedAt = entity.editedAt?.let { Instant.parse(it) },
            deletedAt = entity.deletedAt?.let { Instant.parse(it) }
        )
    }

    private fun updateUnreadCount(conversations: List<Conversation>) {
        val totalUnread = conversations.sumOf { it.unreadCount }
        _unreadCount.value = totalUnread
    }

    private suspend fun syncUnreadCount() {
        try {
            val conversations = getCachedConversations()
            updateUnreadCount(conversations)
        } catch (e: Exception) {
            // Ignore
        }
    }
}
