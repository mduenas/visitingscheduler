package com.markduenas.visischeduler.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.markduenas.visischeduler.domain.entities.Contact
import com.markduenas.visischeduler.domain.entities.Conversation
import com.markduenas.visischeduler.domain.entities.ConversationParticipant
import com.markduenas.visischeduler.domain.entities.Message
import com.markduenas.visischeduler.domain.entities.MessageMetadata
import com.markduenas.visischeduler.domain.entities.MessageType
import com.markduenas.visischeduler.domain.entities.Role
import com.markduenas.visischeduler.domain.entities.TypingStatus
import com.markduenas.visischeduler.domain.entities.User
import com.markduenas.visischeduler.domain.repository.MessageRepository
import com.markduenas.visischeduler.firebase.FirestoreDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant

/**
 * Firestore implementation of MessageRepository.
 */
class FirestoreMessageRepository(
    private val firestore: FirestoreDatabase,
    private val currentUserId: () -> String?,
    private val getCurrentUserName: () -> String?
) : MessageRepository {

    // ==================== Conversations ====================

    override fun getConversations(): Flow<List<Conversation>> {
        val userId = currentUserId() ?: return flowOf(emptyList())
        return firestore.listenToConversations(userId)
            .map { docs ->
                docs.mapNotNull { it.toConversation() }
                    .filter { it.archivedAt == null }
            }
    }

    override fun getArchivedConversations(): Flow<List<Conversation>> {
        val userId = currentUserId() ?: return flowOf(emptyList())
        return firestore.listenToConversations(userId)
            .map { docs ->
                docs.mapNotNull { it.toConversation() }
                    .filter { it.archivedAt != null }
            }
    }

    override suspend fun getConversationById(conversationId: String): Result<Conversation> = runCatching {
        firestore.getById(FirestoreDatabase.COLLECTION_CONVERSATIONS, conversationId)?.toConversation()
            ?: throw Exception("Conversation not found")
    }

    override suspend fun getConversationByBeneficiaryId(beneficiaryId: String): Result<Conversation?> = runCatching {
        val userId = currentUserId() ?: throw Exception("User not authenticated")
        val docs = firestore.query(
            FirestoreDatabase.COLLECTION_CONVERSATIONS,
            "beneficiaryId",
            beneficiaryId
        )
        docs.mapNotNull { it.toConversation() }
            .find { conversation ->
                conversation.participants.any { it.userId == userId }
            }
    }

    override suspend fun createConversation(
        participantIds: List<String>,
        beneficiaryId: String,
        title: String?
    ): Result<Conversation> = runCatching {
        val userId = currentUserId() ?: throw Exception("User not authenticated")
        val allParticipants = (participantIds + userId).distinct()

        val data = mapOf(
            "title" to title,
            "participantIds" to allParticipants,
            "participants" to emptyList<Map<String, Any>>(), // Will be populated separately
            "beneficiaryId" to beneficiaryId,
            "isGroupConversation" to (allParticipants.size > 2),
            "isPinned" to false,
            "isMuted" to false,
            "createdAt" to Timestamp.now(),
            "updatedAt" to Timestamp.now(),
            "lastMessageAt" to Timestamp.now()
        )

        val id = firestore.createConversation(data)

        firestore.getById(FirestoreDatabase.COLLECTION_CONVERSATIONS, id)?.toConversation()
            ?: throw Exception("Failed to create conversation")
    }

    override suspend fun updateConversation(
        conversationId: String,
        title: String?,
        isPinned: Boolean?,
        isMuted: Boolean?
    ): Result<Conversation> = runCatching {
        val updates = mutableMapOf<String, Any>("updatedAt" to Timestamp.now())
        title?.let { updates["title"] = it }
        isPinned?.let { updates["isPinned"] = it }
        isMuted?.let { updates["isMuted"] = it }

        firestore.update(FirestoreDatabase.COLLECTION_CONVERSATIONS, conversationId, updates)

        firestore.getById(FirestoreDatabase.COLLECTION_CONVERSATIONS, conversationId)?.toConversation()
            ?: throw Exception("Conversation not found")
    }

    override suspend fun archiveConversation(conversationId: String): Result<Unit> = runCatching {
        firestore.update(FirestoreDatabase.COLLECTION_CONVERSATIONS, conversationId, mapOf(
            "archivedAt" to Timestamp.now(),
            "updatedAt" to Timestamp.now()
        ))
    }

    override suspend fun unarchiveConversation(conversationId: String): Result<Unit> = runCatching {
        firestore.updateFromMap(FirestoreDatabase.COLLECTION_CONVERSATIONS, conversationId, mapOf(
            "archivedAt" to null,
            "updatedAt" to Timestamp.now()
        ))
    }

    override suspend fun deleteConversation(conversationId: String): Result<Unit> = runCatching {
        firestore.delete(FirestoreDatabase.COLLECTION_CONVERSATIONS, conversationId)
    }

    override suspend fun addParticipants(
        conversationId: String,
        participantIds: List<String>
    ): Result<Conversation> = runCatching {
        val conversation = getConversationById(conversationId).getOrThrow()
        val existingIds = conversation.participants.map { it.userId }
        val newIds = (existingIds + participantIds).distinct()

        firestore.update(FirestoreDatabase.COLLECTION_CONVERSATIONS, conversationId, mapOf(
            "participantIds" to newIds,
            "isGroupConversation" to (newIds.size > 2),
            "updatedAt" to Timestamp.now()
        ))

        getConversationById(conversationId).getOrThrow()
    }

    override suspend fun removeParticipant(
        conversationId: String,
        participantId: String
    ): Result<Conversation> = runCatching {
        val conversation = getConversationById(conversationId).getOrThrow()
        val newIds = conversation.participants.map { it.userId }.filter { it != participantId }

        firestore.update(FirestoreDatabase.COLLECTION_CONVERSATIONS, conversationId, mapOf(
            "participantIds" to newIds,
            "updatedAt" to Timestamp.now()
        ))

        getConversationById(conversationId).getOrThrow()
    }

    override suspend fun leaveConversation(conversationId: String): Result<Unit> = runCatching {
        val userId = currentUserId() ?: throw Exception("User not authenticated")
        removeParticipant(conversationId, userId).getOrThrow()
        Unit
    }

    // ==================== Messages ====================

    override fun getMessages(conversationId: String): Flow<List<Message>> {
        return firestore.listenToMessages(conversationId)
            .map { docs -> docs.mapNotNull { it.toMessage() } }
    }

    override suspend fun getMessagesPaginated(
        conversationId: String,
        limit: Int,
        beforeMessageId: String?
    ): Result<List<Message>> = runCatching {
        // For now, get all messages and filter
        val allMessages = firestore.getAll("${FirestoreDatabase.COLLECTION_CONVERSATIONS}/$conversationId/${FirestoreDatabase.COLLECTION_MESSAGES}")
        val messages = allMessages.mapNotNull { it.toMessage() }
            .sortedByDescending { it.timestamp }

        if (beforeMessageId != null) {
            val index = messages.indexOfFirst { it.id == beforeMessageId }
            if (index >= 0) {
                messages.drop(index + 1).take(limit)
            } else {
                messages.take(limit)
            }
        } else {
            messages.take(limit)
        }
    }

    override suspend fun getMessageById(messageId: String): Result<Message> = runCatching {
        throw Exception("Message lookup by ID across conversations not supported")
    }

    override suspend fun sendMessage(
        conversationId: String,
        content: String,
        replyToMessageId: String?
    ): Result<Message> = runCatching {
        val userId = currentUserId() ?: throw Exception("User not authenticated")
        val userName = getCurrentUserName() ?: "Unknown"

        val messageData = mapOf(
            "senderId" to userId,
            "senderName" to userName,
            "content" to content,
            "timestamp" to Timestamp.now(),
            "isRead" to false,
            "type" to MessageType.TEXT.name,
            "replyToMessageId" to replyToMessageId
        )

        val id = firestore.sendMessage(conversationId, messageData)

        Message(
            id = id,
            conversationId = conversationId,
            senderId = userId,
            senderName = userName,
            content = content,
            timestamp = Instant.fromEpochMilliseconds(System.currentTimeMillis()),
            isRead = false,
            type = MessageType.TEXT,
            replyToMessageId = replyToMessageId
        )
    }

    override suspend fun sendMessageWithAttachment(
        conversationId: String,
        content: String?,
        attachmentPath: String,
        attachmentType: String
    ): Result<Message> = runCatching {
        val userId = currentUserId() ?: throw Exception("User not authenticated")
        val userName = getCurrentUserName() ?: "Unknown"

        // In a full implementation, we would upload the attachment to Firebase Storage first
        val messageData = mapOf(
            "senderId" to userId,
            "senderName" to userName,
            "content" to (content ?: ""),
            "timestamp" to Timestamp.now(),
            "isRead" to false,
            "type" to if (attachmentType.startsWith("image")) MessageType.IMAGE.name else MessageType.DOCUMENT.name,
            "attachmentUrl" to attachmentPath, // Would be storage URL in production
            "metadata" to mapOf(
                "fileName" to attachmentPath.substringAfterLast("/"),
                "fileSize" to 0L
            )
        )

        val id = firestore.sendMessage(conversationId, messageData)

        Message(
            id = id,
            conversationId = conversationId,
            senderId = userId,
            senderName = userName,
            content = content ?: "",
            timestamp = Instant.fromEpochMilliseconds(System.currentTimeMillis()),
            isRead = false,
            type = if (attachmentType.startsWith("image")) MessageType.IMAGE else MessageType.DOCUMENT,
            attachmentUrl = attachmentPath
        )
    }

    override suspend fun editMessage(messageId: String, newContent: String): Result<Message> = runCatching {
        throw Exception("Edit message requires conversation context")
    }

    override suspend fun deleteMessage(messageId: String): Result<Unit> = runCatching {
        throw Exception("Delete message requires conversation context")
    }

    override suspend fun markAsRead(conversationId: String): Result<Unit> = runCatching {
        // In production, would update all unread messages
    }

    override suspend fun markMessagesAsRead(messageIds: List<String>): Result<Unit> = runCatching {
        // In production, would update specific messages
    }

    override suspend fun searchMessages(
        conversationId: String,
        query: String
    ): Result<List<Message>> = runCatching {
        getMessages(conversationId).map { messages ->
            messages.filter { it.content.contains(query, ignoreCase = true) }
        }.let { emptyList() } // Simplified
    }

    override suspend fun searchAllMessages(query: String): Result<List<Message>> = runCatching {
        emptyList() // Would require full-text search
    }

    // ==================== Typing Indicators ====================

    override fun observeTypingStatus(conversationId: String): Flow<List<TypingStatus>> {
        return flowOf(emptyList()) // Would implement with real-time updates
    }

    override suspend fun sendTypingIndicator(
        conversationId: String,
        isTyping: Boolean
    ): Result<Unit> = runCatching {
        // Would update typing status document
    }

    // ==================== Contacts ====================

    override fun getContacts(): Flow<List<Contact>> {
        return flowOf(emptyList()) // Would query users collection
    }

    override suspend fun searchContacts(query: String): Result<List<Contact>> = runCatching {
        emptyList()
    }

    override suspend fun getRecentContacts(limit: Int): Result<List<Contact>> = runCatching {
        emptyList()
    }

    // ==================== Notifications ====================

    override fun getUnreadCount(): Flow<Int> {
        return getConversations().map { conversations ->
            conversations.sumOf { it.unreadCount }
        }
    }

    override suspend fun registerForPushNotifications(token: String): Result<Unit> = runCatching {
        val userId = currentUserId() ?: throw Exception("User not authenticated")
        firestore.update(FirestoreDatabase.COLLECTION_USERS, userId, mapOf(
            "fcmToken" to token
        ))
    }

    override suspend fun unregisterFromPushNotifications(): Result<Unit> = runCatching {
        val userId = currentUserId() ?: throw Exception("User not authenticated")
        firestore.updateUser(userId, mapOf(
            "fcmToken" to null
        ))
    }

    // ==================== Sync ====================

    override suspend fun syncMessages(): Result<Unit> = runCatching { }

    override suspend fun syncConversations(): Result<Unit> = runCatching { }

    // ==================== Mapping Functions ====================

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.toConversation(): Conversation? {
        return try {
            val participantsList = (get("participants") as? List<Map<String, Any>>)?.map { p ->
                ConversationParticipant(
                    userId = p["userId"] as? String ?: "",
                    userName = p["userName"] as? String ?: "",
                    userRole = Role.valueOf(p["userRole"] as? String ?: "PENDING_VISITOR"),
                    profileImageUrl = p["profileImageUrl"] as? String,
                    isActive = p["isActive"] as? Boolean ?: true,
                    joinedAt = (p["joinedAt"] as? Timestamp)?.let {
                        Instant.fromEpochMilliseconds(it.toDate().time)
                    } ?: Instant.fromEpochMilliseconds(0),
                    lastReadMessageId = p["lastReadMessageId"] as? String,
                    lastReadAt = (p["lastReadAt"] as? Timestamp)?.let {
                        Instant.fromEpochMilliseconds(it.toDate().time)
                    }
                )
            } ?: emptyList()

            Conversation(
                id = id,
                title = getString("title"),
                participants = participantsList,
                lastMessage = null, // Would need separate query
                unreadCount = getLong("unreadCount")?.toInt() ?: 0,
                beneficiaryId = getString("beneficiaryId") ?: "",
                beneficiaryName = getString("beneficiaryName"),
                isGroupConversation = getBoolean("isGroupConversation") ?: false,
                isPinned = getBoolean("isPinned") ?: false,
                isMuted = getBoolean("isMuted") ?: false,
                createdAt = getTimestamp("createdAt")?.let {
                    Instant.fromEpochMilliseconds(it.toDate().time)
                } ?: Instant.fromEpochMilliseconds(0),
                updatedAt = getTimestamp("updatedAt")?.let {
                    Instant.fromEpochMilliseconds(it.toDate().time)
                } ?: Instant.fromEpochMilliseconds(0),
                archivedAt = getTimestamp("archivedAt")?.let {
                    Instant.fromEpochMilliseconds(it.toDate().time)
                }
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun DocumentSnapshot.toMessage(): Message? {
        return try {
            val metadata = (get("metadata") as? Map<*, *>)?.let { m ->
                MessageMetadata(
                    visitId = m["visitId"] as? String,
                    visitStatus = m["visitStatus"] as? String,
                    approvalRequestId = m["approvalRequestId"] as? String,
                    fileName = m["fileName"] as? String,
                    fileSize = (m["fileSize"] as? Number)?.toLong(),
                    thumbnailUrl = m["thumbnailUrl"] as? String
                )
            }

            Message(
                id = id,
                conversationId = getString("conversationId") ?: "",
                senderId = getString("senderId") ?: return null,
                senderName = getString("senderName") ?: "",
                content = getString("content") ?: "",
                timestamp = getTimestamp("timestamp")?.let {
                    Instant.fromEpochMilliseconds(it.toDate().time)
                } ?: Instant.fromEpochMilliseconds(0),
                isRead = getBoolean("isRead") ?: false,
                type = MessageType.valueOf(getString("type") ?: "TEXT"),
                metadata = metadata,
                attachmentUrl = getString("attachmentUrl"),
                replyToMessageId = getString("replyToMessageId"),
                editedAt = getTimestamp("editedAt")?.let {
                    Instant.fromEpochMilliseconds(it.toDate().time)
                },
                deletedAt = getTimestamp("deletedAt")?.let {
                    Instant.fromEpochMilliseconds(it.toDate().time)
                }
            )
        } catch (e: Exception) {
            null
        }
    }
}
