package com.markduenas.visischeduler.domain.entities

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Represents the type of message in a conversation.
 */
@Serializable
enum class MessageType {
    /** Standard text message */
    TEXT,
    /** Message about a visit update (scheduled, cancelled, etc.) */
    VISIT_UPDATE,
    /** Message requesting approval for a visit */
    APPROVAL_REQUEST,
    /** System-generated notification message */
    SYSTEM,
    /** Image attachment message */
    IMAGE,
    /** Document attachment message */
    DOCUMENT
}

/**
 * Represents a message in a conversation.
 */
@Serializable
data class Message(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val content: String,
    val timestamp: Instant,
    val isRead: Boolean = false,
    val type: MessageType = MessageType.TEXT,
    val metadata: MessageMetadata? = null,
    val attachmentUrl: String? = null,
    val replyToMessageId: String? = null,
    val editedAt: Instant? = null,
    val deletedAt: Instant? = null
) {
    /**
     * Whether this message has been edited.
     */
    val isEdited: Boolean
        get() = editedAt != null

    /**
     * Whether this message has been deleted.
     */
    val isDeleted: Boolean
        get() = deletedAt != null

    /**
     * Whether this message is a reply to another message.
     */
    val isReply: Boolean
        get() = replyToMessageId != null

    /**
     * Whether this message has an attachment.
     */
    val hasAttachment: Boolean
        get() = attachmentUrl != null

    /**
     * Whether this is a system message.
     */
    val isSystemMessage: Boolean
        get() = type == MessageType.SYSTEM
}

/**
 * Additional metadata for special message types.
 */
@Serializable
data class MessageMetadata(
    /** For VISIT_UPDATE: the visit ID */
    val visitId: String? = null,
    /** For VISIT_UPDATE: the new status */
    val visitStatus: String? = null,
    /** For APPROVAL_REQUEST: the approval request ID */
    val approvalRequestId: String? = null,
    /** For IMAGE/DOCUMENT: the file name */
    val fileName: String? = null,
    /** For IMAGE/DOCUMENT: the file size in bytes */
    val fileSize: Long? = null,
    /** For IMAGE: thumbnail URL */
    val thumbnailUrl: String? = null
)

/**
 * Represents a participant in a conversation.
 */
@Serializable
data class ConversationParticipant(
    val userId: String,
    val userName: String,
    val userRole: Role,
    val profileImageUrl: String? = null,
    val isActive: Boolean = true,
    val joinedAt: Instant,
    val lastReadMessageId: String? = null,
    val lastReadAt: Instant? = null
)

/**
 * Represents a conversation between users.
 */
@Serializable
data class Conversation(
    val id: String,
    val title: String? = null,
    val participants: List<ConversationParticipant>,
    val lastMessage: Message? = null,
    val unreadCount: Int = 0,
    val beneficiaryId: String,
    val beneficiaryName: String? = null,
    val isGroupConversation: Boolean = false,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant,
    val archivedAt: Instant? = null
) {
    /**
     * Display name for the conversation.
     * Uses title if set, otherwise generates from participant names.
     */
    val displayName: String
        get() = title ?: participants.take(3).joinToString(", ") { it.userName }
            .let { if (participants.size > 3) "$it and ${participants.size - 3} more" else it }

    /**
     * Whether the conversation has been archived.
     */
    val isArchived: Boolean
        get() = archivedAt != null

    /**
     * Whether there are unread messages.
     */
    val hasUnreadMessages: Boolean
        get() = unreadCount > 0

    /**
     * Get participant by user ID.
     */
    fun getParticipant(userId: String): ConversationParticipant? =
        participants.find { it.userId == userId }

    /**
     * Get other participants (excluding given user ID).
     */
    fun getOtherParticipants(currentUserId: String): List<ConversationParticipant> =
        participants.filter { it.userId != currentUserId }
}

/**
 * Represents a typing indicator for real-time messaging.
 */
@Serializable
data class TypingStatus(
    val conversationId: String,
    val userId: String,
    val userName: String,
    val isTyping: Boolean,
    val timestamp: Instant
)

/**
 * Represents a contact that can be messaged.
 */
@Serializable
data class Contact(
    val user: User,
    val lastMessageAt: Instant? = null,
    val conversationId: String? = null,
    val relationship: String? = null
) {
    /**
     * Whether an existing conversation exists with this contact.
     */
    val hasExistingConversation: Boolean
        get() = conversationId != null
}
