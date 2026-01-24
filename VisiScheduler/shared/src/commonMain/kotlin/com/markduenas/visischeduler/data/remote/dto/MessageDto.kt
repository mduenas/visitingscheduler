package com.markduenas.visischeduler.data.remote.dto

import com.markduenas.visischeduler.domain.entities.Contact
import com.markduenas.visischeduler.domain.entities.Conversation
import com.markduenas.visischeduler.domain.entities.ConversationParticipant
import com.markduenas.visischeduler.domain.entities.Message
import com.markduenas.visischeduler.domain.entities.MessageMetadata
import com.markduenas.visischeduler.domain.entities.MessageType
import com.markduenas.visischeduler.domain.entities.Role
import com.markduenas.visischeduler.domain.entities.TypingStatus
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Data Transfer Object for Message.
 */
@Serializable
data class MessageDto(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val content: String,
    val timestamp: String,
    val isRead: Boolean = false,
    val type: String = "TEXT",
    val metadata: MessageMetadataDto? = null,
    val attachmentUrl: String? = null,
    val replyToMessageId: String? = null,
    val editedAt: String? = null,
    val deletedAt: String? = null
) {
    fun toDomain(): Message {
        return Message(
            id = id,
            conversationId = conversationId,
            senderId = senderId,
            senderName = senderName,
            content = content,
            timestamp = Instant.parse(timestamp),
            isRead = isRead,
            type = MessageType.valueOf(type),
            metadata = metadata?.toDomain(),
            attachmentUrl = attachmentUrl,
            replyToMessageId = replyToMessageId,
            editedAt = editedAt?.let { Instant.parse(it) },
            deletedAt = deletedAt?.let { Instant.parse(it) }
        )
    }

    companion object {
        fun fromDomain(message: Message): MessageDto {
            return MessageDto(
                id = message.id,
                conversationId = message.conversationId,
                senderId = message.senderId,
                senderName = message.senderName,
                content = message.content,
                timestamp = message.timestamp.toString(),
                isRead = message.isRead,
                type = message.type.name,
                metadata = message.metadata?.let { MessageMetadataDto.fromDomain(it) },
                attachmentUrl = message.attachmentUrl,
                replyToMessageId = message.replyToMessageId,
                editedAt = message.editedAt?.toString(),
                deletedAt = message.deletedAt?.toString()
            )
        }
    }
}

/**
 * Data Transfer Object for MessageMetadata.
 */
@Serializable
data class MessageMetadataDto(
    val visitId: String? = null,
    val visitStatus: String? = null,
    val approvalRequestId: String? = null,
    val fileName: String? = null,
    val fileSize: Long? = null,
    val thumbnailUrl: String? = null
) {
    fun toDomain(): MessageMetadata {
        return MessageMetadata(
            visitId = visitId,
            visitStatus = visitStatus,
            approvalRequestId = approvalRequestId,
            fileName = fileName,
            fileSize = fileSize,
            thumbnailUrl = thumbnailUrl
        )
    }

    companion object {
        fun fromDomain(metadata: MessageMetadata): MessageMetadataDto {
            return MessageMetadataDto(
                visitId = metadata.visitId,
                visitStatus = metadata.visitStatus,
                approvalRequestId = metadata.approvalRequestId,
                fileName = metadata.fileName,
                fileSize = metadata.fileSize,
                thumbnailUrl = metadata.thumbnailUrl
            )
        }
    }
}

/**
 * Data Transfer Object for ConversationParticipant.
 */
@Serializable
data class ConversationParticipantDto(
    val userId: String,
    val userName: String,
    val userRole: String,
    val profileImageUrl: String? = null,
    val isActive: Boolean = true,
    val joinedAt: String,
    val lastReadMessageId: String? = null,
    val lastReadAt: String? = null
) {
    fun toDomain(): ConversationParticipant {
        return ConversationParticipant(
            userId = userId,
            userName = userName,
            userRole = Role.valueOf(userRole),
            profileImageUrl = profileImageUrl,
            isActive = isActive,
            joinedAt = Instant.parse(joinedAt),
            lastReadMessageId = lastReadMessageId,
            lastReadAt = lastReadAt?.let { Instant.parse(it) }
        )
    }

    companion object {
        fun fromDomain(participant: ConversationParticipant): ConversationParticipantDto {
            return ConversationParticipantDto(
                userId = participant.userId,
                userName = participant.userName,
                userRole = participant.userRole.name,
                profileImageUrl = participant.profileImageUrl,
                isActive = participant.isActive,
                joinedAt = participant.joinedAt.toString(),
                lastReadMessageId = participant.lastReadMessageId,
                lastReadAt = participant.lastReadAt?.toString()
            )
        }
    }
}

/**
 * Data Transfer Object for Conversation.
 */
@Serializable
data class ConversationDto(
    val id: String,
    val title: String? = null,
    val participants: List<ConversationParticipantDto>,
    val lastMessage: MessageDto? = null,
    val unreadCount: Int = 0,
    val beneficiaryId: String,
    val beneficiaryName: String? = null,
    val isGroupConversation: Boolean = false,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val createdAt: String,
    val updatedAt: String,
    val archivedAt: String? = null
) {
    fun toDomain(): Conversation {
        return Conversation(
            id = id,
            title = title,
            participants = participants.map { it.toDomain() },
            lastMessage = lastMessage?.toDomain(),
            unreadCount = unreadCount,
            beneficiaryId = beneficiaryId,
            beneficiaryName = beneficiaryName,
            isGroupConversation = isGroupConversation,
            isPinned = isPinned,
            isMuted = isMuted,
            createdAt = Instant.parse(createdAt),
            updatedAt = Instant.parse(updatedAt),
            archivedAt = archivedAt?.let { Instant.parse(it) }
        )
    }

    companion object {
        fun fromDomain(conversation: Conversation): ConversationDto {
            return ConversationDto(
                id = conversation.id,
                title = conversation.title,
                participants = conversation.participants.map { ConversationParticipantDto.fromDomain(it) },
                lastMessage = conversation.lastMessage?.let { MessageDto.fromDomain(it) },
                unreadCount = conversation.unreadCount,
                beneficiaryId = conversation.beneficiaryId,
                beneficiaryName = conversation.beneficiaryName,
                isGroupConversation = conversation.isGroupConversation,
                isPinned = conversation.isPinned,
                isMuted = conversation.isMuted,
                createdAt = conversation.createdAt.toString(),
                updatedAt = conversation.updatedAt.toString(),
                archivedAt = conversation.archivedAt?.toString()
            )
        }
    }
}

/**
 * Data Transfer Object for TypingStatus.
 */
@Serializable
data class TypingStatusDto(
    val conversationId: String,
    val userId: String,
    val userName: String,
    val isTyping: Boolean,
    val timestamp: String
) {
    fun toDomain(): TypingStatus {
        return TypingStatus(
            conversationId = conversationId,
            userId = userId,
            userName = userName,
            isTyping = isTyping,
            timestamp = Instant.parse(timestamp)
        )
    }

    companion object {
        fun fromDomain(status: TypingStatus): TypingStatusDto {
            return TypingStatusDto(
                conversationId = status.conversationId,
                userId = status.userId,
                userName = status.userName,
                isTyping = status.isTyping,
                timestamp = status.timestamp.toString()
            )
        }
    }
}

/**
 * Data Transfer Object for Contact.
 */
@Serializable
data class ContactDto(
    val user: UserDto,
    val lastMessageAt: String? = null,
    val conversationId: String? = null,
    val relationship: String? = null
) {
    fun toDomain(): Contact {
        return Contact(
            user = user.toDomain(),
            lastMessageAt = lastMessageAt?.let { Instant.parse(it) },
            conversationId = conversationId,
            relationship = relationship
        )
    }

    companion object {
        fun fromDomain(contact: Contact): ContactDto {
            return ContactDto(
                user = UserDto.fromDomain(contact.user),
                lastMessageAt = contact.lastMessageAt?.toString(),
                conversationId = contact.conversationId,
                relationship = contact.relationship
            )
        }
    }
}

/**
 * Request DTO for creating a conversation.
 */
@Serializable
data class CreateConversationRequestDto(
    val participantIds: List<String>,
    val beneficiaryId: String,
    val title: String? = null,
    val initialMessage: String? = null
)

/**
 * Request DTO for sending a message.
 */
@Serializable
data class SendMessageRequestDto(
    val content: String,
    val replyToMessageId: String? = null,
    val type: String = "TEXT",
    val metadata: MessageMetadataDto? = null
)

/**
 * Request DTO for updating conversation settings.
 */
@Serializable
data class UpdateConversationRequestDto(
    val title: String? = null,
    val isPinned: Boolean? = null,
    val isMuted: Boolean? = null
)

/**
 * Request DTO for editing a message.
 */
@Serializable
data class EditMessageRequestDto(
    val content: String
)

/**
 * Response DTO for paginated messages.
 */
@Serializable
data class PaginatedMessagesResponseDto(
    val messages: List<MessageDto>,
    val hasMore: Boolean,
    val totalCount: Int
)
