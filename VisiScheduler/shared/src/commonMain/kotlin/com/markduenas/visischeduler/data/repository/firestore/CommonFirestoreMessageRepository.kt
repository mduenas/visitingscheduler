package com.markduenas.visischeduler.data.repository.firestore

import com.markduenas.visischeduler.domain.entities.Contact
import com.markduenas.visischeduler.domain.entities.Conversation
import com.markduenas.visischeduler.domain.entities.ConversationParticipant
import com.markduenas.visischeduler.domain.entities.Message
import com.markduenas.visischeduler.domain.entities.MessageType
import com.markduenas.visischeduler.domain.entities.NotificationPreferences
import com.markduenas.visischeduler.domain.entities.Role
import com.markduenas.visischeduler.domain.entities.TypingStatus
import com.markduenas.visischeduler.domain.entities.User
import com.markduenas.visischeduler.domain.repository.MessageRepository
import com.markduenas.visischeduler.firebase.FirestoreDatabase
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlin.time.Instant
import kotlin.time.Clock

/**
 * Cross-platform Firestore implementation of MessageRepository.
 */
class CommonFirestoreMessageRepository(
    private val firestore: FirestoreDatabase,
    private val auth: FirebaseAuth
) : MessageRepository {

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    // Cache messageId → conversationId so deleteMessage/editMessage can find the right path
    private val messageConversationCache = mutableMapOf<String, String>()

    // ==================== Conversations ====================

    override fun getConversations(): Flow<List<Conversation>> {
        val userId = currentUserId ?: return flowOf(emptyList())
        return firestore.listenToConversations(userId)
            .map { docs -> docs.mapNotNull { it.toConversation() } }
    }

    override fun getArchivedConversations(): Flow<List<Conversation>> {
        return getConversations().map { conversations ->
            conversations.filter { it.isArchived }
        }
    }

    override suspend fun getConversationById(conversationId: String): Result<Conversation> = runCatching {
        firestore.getConversation(conversationId)?.toConversation()
            ?: throw Exception("Conversation not found")
    }

    override suspend fun getConversationByBeneficiaryId(beneficiaryId: String): Result<Conversation?> = runCatching {
        val conversations = firestore.query(
            FirestoreDatabase.COLLECTION_CONVERSATIONS,
            "beneficiaryId",
            beneficiaryId
        )
        conversations.firstOrNull()?.toConversation()
    }

    override suspend fun createConversation(
        participantIds: List<String>,
        beneficiaryId: String,
        title: String?
    ): Result<Conversation> = runCatching {
        val userId = currentUserId ?: throw Exception("Not authenticated")
        val now = Clock.System.now()

        val allParticipants = (participantIds + userId).distinct()

        val data = mapOf<String, Any?>(
            "participantIds" to allParticipants,
            "beneficiaryId" to beneficiaryId,
            "title" to title,
            "isPinned" to false,
            "isMuted" to false,
            "createdBy" to userId,
            "createdAt" to firestore.serverTimestamp(),
            "updatedAt" to firestore.serverTimestamp()
        )

        val id = firestore.createConversation(data)

        val participants = allParticipants.map { participantId ->
            ConversationParticipant(
                userId = participantId,
                userName = "",
                userRole = Role.PENDING_VISITOR,
                profileImageUrl = null,
                isActive = true,
                joinedAt = now,
                lastReadMessageId = null,
                lastReadAt = null
            )
        }

        Conversation(
            id = id,
            title = title,
            participants = participants,
            lastMessage = null,
            unreadCount = 0,
            beneficiaryId = beneficiaryId,
            beneficiaryName = null,
            isGroupConversation = participants.size > 2,
            isPinned = false,
            isMuted = false,
            createdAt = now,
            updatedAt = now,
            archivedAt = null
        )
    }

    override suspend fun updateConversation(
        conversationId: String,
        title: String?,
        isPinned: Boolean?,
        isMuted: Boolean?
    ): Result<Conversation> = runCatching {
        val updates = mutableMapOf<String, Any?>(
            "updatedAt" to firestore.serverTimestamp()
        )
        title?.let { updates["title"] = it }
        isPinned?.let { updates["isPinned"] = it }
        isMuted?.let { updates["isMuted"] = it }

        firestore.updateFromMap(FirestoreDatabase.COLLECTION_CONVERSATIONS, conversationId, updates)

        firestore.getConversation(conversationId)?.toConversation()
            ?: throw Exception("Conversation not found")
    }

    override suspend fun archiveConversation(conversationId: String): Result<Unit> = runCatching {
        firestore.updateFromMap(FirestoreDatabase.COLLECTION_CONVERSATIONS, conversationId, mapOf(
            "archivedAt" to firestore.serverTimestamp(),
            "updatedAt" to firestore.serverTimestamp()
        ))
    }

    override suspend fun unarchiveConversation(conversationId: String): Result<Unit> = runCatching {
        firestore.updateFromMap(FirestoreDatabase.COLLECTION_CONVERSATIONS, conversationId, mapOf(
            "archivedAt" to null,
            "updatedAt" to firestore.serverTimestamp()
        ))
    }

    override suspend fun deleteConversation(conversationId: String): Result<Unit> = runCatching {
        firestore.delete(FirestoreDatabase.COLLECTION_CONVERSATIONS, conversationId)
    }

    override suspend fun addParticipants(
        conversationId: String,
        participantIds: List<String>
    ): Result<Conversation> = runCatching {
        val conversation = firestore.getConversation(conversationId)?.toConversation()
            ?: throw Exception("Conversation not found")

        val existingIds = conversation.participants.map { it.userId }
        val newIds = (existingIds + participantIds).distinct()
        firestore.updateFromMap(FirestoreDatabase.COLLECTION_CONVERSATIONS, conversationId, mapOf(
            "participantIds" to newIds,
            "updatedAt" to firestore.serverTimestamp()
        ))

        firestore.getConversation(conversationId)?.toConversation()
            ?: throw Exception("Conversation not found")
    }

    override suspend fun removeParticipant(
        conversationId: String,
        participantId: String
    ): Result<Conversation> = runCatching {
        val conversation = firestore.getConversation(conversationId)?.toConversation()
            ?: throw Exception("Conversation not found")

        val newIds = conversation.participants.map { it.userId } - participantId
        firestore.updateFromMap(FirestoreDatabase.COLLECTION_CONVERSATIONS, conversationId, mapOf(
            "participantIds" to newIds,
            "updatedAt" to firestore.serverTimestamp()
        ))

        firestore.getConversation(conversationId)?.toConversation()
            ?: throw Exception("Conversation not found")
    }

    override suspend fun leaveConversation(conversationId: String): Result<Unit> = runCatching {
        val userId = currentUserId ?: throw Exception("Not authenticated")
        removeParticipant(conversationId, userId)
        Unit
    }

    // ==================== Messages ====================

    override fun getMessages(conversationId: String): Flow<List<Message>> {
        return firestore.listenToMessages(conversationId)
            .map { docs ->
                docs.mapNotNull { it.toMessage(conversationId) }.also { messages ->
                    messages.forEach { messageConversationCache[it.id] = conversationId }
                }
            }
    }

    override suspend fun getMessagesPaginated(
        conversationId: String,
        limit: Int,
        beforeMessageId: String?
    ): Result<List<Message>> = runCatching {
        val docs = firestore.query(
            "${FirestoreDatabase.COLLECTION_CONVERSATIONS}/$conversationId/${FirestoreDatabase.COLLECTION_MESSAGES}",
            "conversationId",
            conversationId
        )
        docs.mapNotNull { it.toMessage(conversationId) }.take(limit).also { messages ->
            messages.forEach { messageConversationCache[it.id] = conversationId }
        }
    }

    override suspend fun getMessageById(messageId: String): Result<Message> = runCatching {
        val conversationId = messageConversationCache[messageId]
            ?: throw Exception("Message $messageId not in cache — load conversation messages first")
        val docs = firestore.query(
            "${FirestoreDatabase.COLLECTION_CONVERSATIONS}/$conversationId/${FirestoreDatabase.COLLECTION_MESSAGES}",
            "id",
            messageId
        )
        docs.firstOrNull()?.toMessage(conversationId)
            ?: throw Exception("Message not found")
    }

    override suspend fun sendMessage(
        conversationId: String,
        content: String,
        replyToMessageId: String?
    ): Result<Message> = runCatching {
        val userId = currentUserId ?: throw Exception("Not authenticated")
        val now = Clock.System.now()

        val messageData = mapOf<String, Any?>(
            "senderId" to userId,
            "senderName" to "",
            "content" to content,
            "replyToMessageId" to replyToMessageId,
            "type" to MessageType.TEXT.name,
            "isRead" to false,
            "timestamp" to firestore.serverTimestamp()
        )

        val messageId = firestore.sendMessage(conversationId, messageData)

        Message(
            id = messageId,
            conversationId = conversationId,
            senderId = userId,
            senderName = "",
            content = content,
            timestamp = now,
            isRead = false,
            type = MessageType.TEXT,
            metadata = null,
            attachmentUrl = null,
            replyToMessageId = replyToMessageId,
            editedAt = null,
            deletedAt = null
        )
    }

    override suspend fun sendMessageWithAttachment(
        conversationId: String,
        content: String?,
        attachmentPath: String,
        attachmentType: String
    ): Result<Message> = runCatching {
        sendMessage(conversationId, content ?: "[Attachment]", null).getOrThrow()
    }

    override suspend fun editMessage(messageId: String, newContent: String): Result<Message> = runCatching {
        val conversationId = messageConversationCache[messageId]
            ?: throw Exception("Message $messageId not in cache — load conversation messages first")
        firestore.editMessage(conversationId, messageId, newContent)
        getMessageById(messageId).getOrThrow()
    }

    override suspend fun deleteMessage(messageId: String): Result<Unit> = runCatching {
        val conversationId = messageConversationCache[messageId]
            ?: throw Exception("Message $messageId not in cache — load conversation messages first")
        firestore.deleteMessage(conversationId, messageId)
        messageConversationCache.remove(messageId)
    }

    override suspend fun markAsRead(conversationId: String): Result<Unit> = runCatching {
        firestore.updateFromMap(FirestoreDatabase.COLLECTION_CONVERSATIONS, conversationId, mapOf(
            "unreadCount" to 0,
            "updatedAt" to firestore.serverTimestamp()
        ))
    }

    override suspend fun markMessagesAsRead(messageIds: List<String>): Result<Unit> = runCatching {
        // Simplified implementation
    }

    override suspend fun searchMessages(conversationId: String, query: String): Result<List<Message>> = runCatching {
        getMessages(conversationId).map { messages ->
            messages.filter { it.content.contains(query, ignoreCase = true) }
        }.let { emptyList() }
    }

    override suspend fun searchAllMessages(query: String): Result<List<Message>> = runCatching {
        emptyList()
    }

    // ==================== Typing Indicators ====================

    override fun observeTypingStatus(conversationId: String): Flow<List<TypingStatus>> {
        return flowOf(emptyList())
    }

    override suspend fun sendTypingIndicator(conversationId: String, isTyping: Boolean): Result<Unit> = runCatching {
        // Would update a typing status document
    }

    // ==================== Contacts ====================

    override fun getContacts(): Flow<List<Contact>> {
        return firestore.listenToCollection(FirestoreDatabase.COLLECTION_USERS)
            .map { docs ->
                docs.mapNotNull { doc ->
                    try {
                        val user = doc.toUser() ?: return@mapNotNull null
                        Contact(
                            user = user,
                            lastMessageAt = null,
                            conversationId = null,
                            relationship = null
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
            }
    }

    override suspend fun searchContacts(query: String): Result<List<Contact>> = runCatching {
        val users = firestore.getAll(FirestoreDatabase.COLLECTION_USERS)
        users.mapNotNull { doc ->
            try {
                val user = doc.toUser() ?: return@mapNotNull null
                val name = "${user.firstName} ${user.lastName}".trim()
                if (name.contains(query, ignoreCase = true) || user.email.contains(query, ignoreCase = true)) {
                    Contact(
                        user = user,
                        lastMessageAt = null,
                        conversationId = null,
                        relationship = null
                    )
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }

    override suspend fun getRecentContacts(limit: Int): Result<List<Contact>> = runCatching {
        searchContacts("").getOrThrow().take(limit)
    }

    // ==================== Notifications ====================

    override fun getUnreadCount(): Flow<Int> {
        return getConversations().map { conversations ->
            conversations.sumOf { it.unreadCount }
        }
    }

    override suspend fun registerForPushNotifications(token: String): Result<Unit> = runCatching {
        val userId = currentUserId ?: throw Exception("Not authenticated")
        firestore.updateUser(userId, mapOf(
            "fcmToken" to token,
            "updatedAt" to firestore.serverTimestamp()
        ))
    }

    override suspend fun unregisterFromPushNotifications(): Result<Unit> = runCatching {
        val userId = currentUserId ?: throw Exception("Not authenticated")
        firestore.updateUser(userId, mapOf(
            "fcmToken" to null,
            "updatedAt" to firestore.serverTimestamp()
        ))
    }

    // ==================== Sync ====================

    override suspend fun syncMessages(): Result<Unit> = runCatching {
        // Firestore handles sync automatically
    }

    override suspend fun syncConversations(): Result<Unit> = runCatching {
        // Firestore handles sync automatically
    }

    // ==================== Mapping Functions ====================

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.toConversation(): Conversation? {
        return try {
            val participantIds = get<List<String>?>("participantIds") ?: emptyList()
            val now = Clock.System.now()

            val participants = participantIds.map { participantId ->
                ConversationParticipant(
                    userId = participantId,
                    userName = "",
                    userRole = Role.PENDING_VISITOR,
                    profileImageUrl = null,
                    isActive = true,
                    joinedAt = now,
                    lastReadMessageId = null,
                    lastReadAt = null
                )
            }

            Conversation(
                id = id,
                title = get("title"),
                participants = participants,
                lastMessage = null,
                unreadCount = get<Long?>("unreadCount")?.toInt() ?: 0,
                beneficiaryId = get("beneficiaryId") ?: "",
                beneficiaryName = get("beneficiaryName"),
                isGroupConversation = participants.size > 2,
                isPinned = get("isPinned") ?: false,
                isMuted = get("isMuted") ?: false,
                createdAt = get<Long?>("createdAt")?.let { Instant.fromEpochMilliseconds(it) }
                    ?: Instant.fromEpochMilliseconds(0),
                updatedAt = get<Long?>("updatedAt")?.let { Instant.fromEpochMilliseconds(it) }
                    ?: Instant.fromEpochMilliseconds(0),
                archivedAt = get<Long?>("archivedAt")?.let { Instant.fromEpochMilliseconds(it) }
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun DocumentSnapshot.toMessage(conversationId: String): Message? {
        return try {
            Message(
                id = id,
                conversationId = conversationId,
                senderId = get("senderId") ?: return null,
                senderName = get("senderName") ?: "",
                content = get("content") ?: "",
                timestamp = get<Long?>("timestamp")?.let { Instant.fromEpochMilliseconds(it) }
                    ?: Instant.fromEpochMilliseconds(0),
                isRead = get("isRead") ?: false,
                type = try {
                    MessageType.valueOf(get("type") ?: "TEXT")
                } catch (e: Exception) {
                    MessageType.TEXT
                },
                metadata = null,
                attachmentUrl = get("attachmentUrl"),
                replyToMessageId = get("replyToMessageId"),
                editedAt = get<Long?>("editedAt")?.let { Instant.fromEpochMilliseconds(it) },
                deletedAt = get<Long?>("deletedAt")?.let { Instant.fromEpochMilliseconds(it) }
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun DocumentSnapshot.toUser(): User? {
        return try {
            val now = Clock.System.now()
            User(
                id = id,
                email = get("email") ?: return null,
                firstName = get("firstName") ?: "",
                lastName = get("lastName") ?: "",
                role = try {
                    Role.valueOf(get("role") ?: "PENDING_VISITOR")
                } catch (e: Exception) {
                    Role.PENDING_VISITOR
                },
                phoneNumber = get("phoneNumber"),
                profileImageUrl = get("profileImageUrl"),
                isActive = get("isActive") ?: true,
                isEmailVerified = get("isEmailVerified") ?: false,
                createdAt = get<Long?>("createdAt")?.let { Instant.fromEpochMilliseconds(it) } ?: now,
                updatedAt = get<Long?>("updatedAt")?.let { Instant.fromEpochMilliseconds(it) } ?: now,
                lastLoginAt = get<Long?>("lastLoginAt")?.let { Instant.fromEpochMilliseconds(it) },
                associatedBeneficiaryIds = get("associatedBeneficiaryIds") ?: emptyList(),
                notificationPreferences = NotificationPreferences()
            )
        } catch (e: Exception) {
            null
        }
    }
}
