package com.markduenas.visischeduler.data.repository

import com.markduenas.visischeduler.data.local.VisiSchedulerDatabase
import com.markduenas.visischeduler.data.remote.api.VisiSchedulerApi
import com.markduenas.visischeduler.data.remote.dto.SendMessageRequestDto
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

    override fun getArchivedConversations(): Flow<List<Conversation>> = flowOf(emptyList())

    override suspend fun getConversationById(conversationId: String): Result<Conversation> = runCatching {
        throw Exception("Not implemented")
    }

    override suspend fun getConversationByBeneficiaryId(beneficiaryId: String): Result<Conversation?> = runCatching {
        null
    }

    override suspend fun createConversation(participantIds: List<String>, beneficiaryId: String, title: String?): Result<Conversation> = runCatching {
        throw Exception("Not implemented")
    }

    override suspend fun updateConversation(conversationId: String, title: String?, isPinned: Boolean?, isMuted: Boolean?): Result<Conversation> = runCatching {
        throw Exception("Not implemented")
    }

    override suspend fun archiveConversation(conversationId: String): Result<Unit> = Result.success(Unit)
    override suspend fun unarchiveConversation(conversationId: String): Result<Unit> = Result.success(Unit)
    override suspend fun deleteConversation(conversationId: String): Result<Unit> = Result.success(Unit)

    override suspend fun addParticipants(conversationId: String, participantIds: List<String>): Result<Conversation> = runCatching {
        throw Exception("Not implemented")
    }

    override suspend fun removeParticipant(conversationId: String, participantId: String): Result<Conversation> = runCatching {
        throw Exception("Not implemented")
    }

    override suspend fun leaveConversation(conversationId: String): Result<Unit> = Result.success(Unit)

    override fun getMessages(conversationId: String): Flow<List<Message>> = flow {
        emit(emptyList())
        try {
            val remote = api.getMessages(conversationId).map { it.toDomain() }
            emit(remote)
        } catch (e: Exception) {}
    }

    override suspend fun getMessagesPaginated(conversationId: String, limit: Int, beforeMessageId: String?): Result<List<Message>> = runCatching {
        api.getMessages(conversationId).map { it.toDomain() }
    }

    override suspend fun getMessageById(messageId: String): Result<Message> = runCatching {
        throw Exception("Not implemented")
    }

    override suspend fun sendMessage(conversationId: String, content: String, replyToMessageId: String?): Result<Message> = runCatching {
        val request = SendMessageRequestDto(content, replyToMessageId)
        api.sendMessage(conversationId, request).toDomain()
    }

    override suspend fun sendMessageWithAttachment(conversationId: String, content: String?, attachmentPath: String, attachmentType: String): Result<Message> = runCatching {
        throw Exception("Not implemented")
    }

    override suspend fun editMessage(messageId: String, newContent: String): Result<Message> = runCatching {
        throw Exception("Not implemented")
    }

    override suspend fun deleteMessage(messageId: String): Result<Unit> = Result.success(Unit)

    override suspend fun markAsRead(conversationId: String): Result<Unit> = Result.success(Unit)

    override suspend fun markMessagesAsRead(messageIds: List<String>): Result<Unit> = Result.success(Unit)

    override suspend fun searchMessages(conversationId: String, query: String): Result<List<Message>> = Result.success(emptyList())

    override suspend fun searchAllMessages(query: String): Result<List<Message>> = Result.success(emptyList())

    override fun observeTypingStatus(conversationId: String): Flow<List<TypingStatus>> = flowOf(emptyList())

    override suspend fun sendTypingIndicator(conversationId: String, isTyping: Boolean): Result<Unit> = Result.success(Unit)

    override fun getContacts(): Flow<List<Contact>> = flowOf(emptyList())

    override suspend fun searchContacts(query: String): Result<List<Contact>> = Result.success(emptyList())

    override suspend fun getRecentContacts(limit: Int): Result<List<Contact>> = Result.success(emptyList())

    override fun getUnreadCount(): Flow<Int> = flowOf(0)

    override suspend fun registerForPushNotifications(token: String): Result<Unit> = Result.success(Unit)

    override suspend fun unregisterFromPushNotifications(): Result<Unit> = Result.success(Unit)

    override suspend fun syncMessages(): Result<Unit> = Result.success(Unit)

    override suspend fun syncConversations(): Result<Unit> = Result.success(Unit)
}
