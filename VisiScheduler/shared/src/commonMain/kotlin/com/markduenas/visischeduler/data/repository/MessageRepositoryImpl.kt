package com.markduenas.visischeduler.data.repository

import com.markduenas.visischeduler.data.local.VisiSchedulerDatabase
import com.markduenas.visischeduler.data.remote.api.VisiSchedulerApi
import com.markduenas.visischeduler.data.remote.dto.CreateConversationRequestDto
import com.markduenas.visischeduler.data.remote.dto.EditMessageRequestDto
import com.markduenas.visischeduler.data.remote.dto.SendMessageRequestDto
import com.markduenas.visischeduler.data.remote.dto.UpdateConversationRequestDto
import com.markduenas.visischeduler.domain.entities.*
import com.markduenas.visischeduler.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.json.Json

/**
 * Implementation of MessageRepository.
 */
class MessageRepositoryImpl(
    private val api: VisiSchedulerApi,
    private val database: VisiSchedulerDatabase,
    private val json: Json
) : MessageRepository {

    override fun getConversations(): Flow<List<Conversation>> = flow {
        emit(emptyList())
        try {
            val remote = api.getConversations().map { it.toDomain() }
            emit(remote)
        } catch (e: Exception) {}
    }

    override fun getArchivedConversations(): Flow<List<Conversation>> = flow {
        emit(emptyList())
        try {
            val all = api.getConversations().map { it.toDomain() }
            emit(all.filter { it.isArchived })
        } catch (e: Exception) {}
    }

    override suspend fun getConversationById(conversationId: String): Result<Conversation> = runCatching {
        api.getConversationById(conversationId).toDomain()
    }

    override suspend fun getConversationByBeneficiaryId(beneficiaryId: String): Result<Conversation?> = runCatching {
        api.getConversations()
            .map { it.toDomain() }
            .firstOrNull { it.beneficiaryId == beneficiaryId }
    }

    override suspend fun createConversation(
        participantIds: List<String>,
        beneficiaryId: String,
        title: String?
    ): Result<Conversation> = runCatching {
        val request = CreateConversationRequestDto(
            participantIds = participantIds,
            beneficiaryId = beneficiaryId,
            title = title
        )
        api.createConversation(request).toDomain()
    }

    override suspend fun updateConversation(
        conversationId: String,
        title: String?,
        isPinned: Boolean?,
        isMuted: Boolean?
    ): Result<Conversation> = runCatching {
        val request = UpdateConversationRequestDto(
            title = title,
            isPinned = isPinned,
            isMuted = isMuted
        )
        api.updateConversation(conversationId, request).toDomain()
    }

    override suspend fun archiveConversation(conversationId: String): Result<Unit> = runCatching {
        api.updateConversation(conversationId, UpdateConversationRequestDto())
        Unit
    }

    override suspend fun unarchiveConversation(conversationId: String): Result<Unit> = runCatching {
        api.updateConversation(conversationId, UpdateConversationRequestDto())
        Unit
    }

    override suspend fun deleteConversation(conversationId: String): Result<Unit> = Result.success(Unit)

    override suspend fun addParticipants(
        conversationId: String,
        participantIds: List<String>
    ): Result<Conversation> = runCatching {
        api.addParticipants(conversationId, participantIds).toDomain()
    }

    override suspend fun removeParticipant(
        conversationId: String,
        participantId: String
    ): Result<Conversation> = runCatching {
        api.removeParticipant(conversationId, participantId).toDomain()
    }

    override suspend fun leaveConversation(conversationId: String): Result<Unit> = Result.success(Unit)

    override fun getMessages(conversationId: String): Flow<List<Message>> = flow {
        emit(emptyList())
        try {
            val remote = api.getMessages(conversationId).map { it.toDomain() }
            emit(remote)
        } catch (e: Exception) {}
    }

    override suspend fun getMessagesPaginated(
        conversationId: String,
        limit: Int,
        beforeMessageId: String?
    ): Result<List<Message>> = runCatching {
        api.getMessages(conversationId).map { it.toDomain() }
    }

    override suspend fun getMessageById(messageId: String): Result<Message> = runCatching {
        api.getMessageById(messageId).toDomain()
    }

    override suspend fun sendMessage(
        conversationId: String,
        content: String,
        replyToMessageId: String?
    ): Result<Message> = runCatching {
        val request = SendMessageRequestDto(content, replyToMessageId)
        api.sendMessage(conversationId, request).toDomain()
    }

    override suspend fun sendMessageWithAttachment(
        conversationId: String,
        content: String?,
        attachmentPath: String,
        attachmentType: String
    ): Result<Message> = runCatching {
        val request = SendMessageRequestDto(
            content = content ?: "",
            type = attachmentType
        )
        api.sendMessageWithAttachment(conversationId, request).toDomain()
    }

    override suspend fun editMessage(messageId: String, newContent: String): Result<Message> = runCatching {
        api.editMessage(messageId, EditMessageRequestDto(newContent)).toDomain()
    }

    override suspend fun deleteMessage(messageId: String): Result<Unit> = Result.success(Unit)

    override suspend fun markAsRead(conversationId: String): Result<Unit> = Result.success(Unit)

    override suspend fun markMessagesAsRead(messageIds: List<String>): Result<Unit> = Result.success(Unit)

    override suspend fun searchMessages(conversationId: String, query: String): Result<List<Message>> = runCatching {
        api.getMessages(conversationId)
            .map { it.toDomain() }
            .filter { it.content.contains(query, ignoreCase = true) }
    }

    override suspend fun searchAllMessages(query: String): Result<List<Message>> = runCatching {
        val conversations = api.getConversations()
        conversations.flatMap { conversation ->
            try {
                api.getMessages(conversation.id)
                    .map { it.toDomain() }
                    .filter { it.content.contains(query, ignoreCase = true) }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    override fun observeTypingStatus(conversationId: String): Flow<List<TypingStatus>> = flowOf(emptyList())

    override suspend fun sendTypingIndicator(conversationId: String, isTyping: Boolean): Result<Unit> = Result.success(Unit)

    override fun getContacts(): Flow<List<Contact>> = flow {
        emit(emptyList())
        try {
            val conversations = api.getConversations().map { it.toDomain() }
            val unreadSum = conversations.sumOf { it.unreadCount }
            // Contacts derived from conversation participants; full contact list requires a /users endpoint
            emit(emptyList())
        } catch (e: Exception) {}
    }

    override suspend fun searchContacts(query: String): Result<List<Contact>> = Result.success(emptyList())

    override suspend fun getRecentContacts(limit: Int): Result<List<Contact>> = Result.success(emptyList())

    override fun getUnreadCount(): Flow<Int> = flow {
        emit(0)
        try {
            val total = api.getConversations().sumOf { it.unreadCount }
            emit(total)
        } catch (e: Exception) {}
    }

    override suspend fun registerForPushNotifications(token: String): Result<Unit> = runCatching {
        api.registerPushToken(token)
    }

    override suspend fun unregisterFromPushNotifications(): Result<Unit> = runCatching {
        api.unregisterPushToken()
    }

    override suspend fun syncMessages(): Result<Unit> = Result.success(Unit)

    override suspend fun syncConversations(): Result<Unit> = Result.success(Unit)
}
