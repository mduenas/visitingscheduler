package com.markduenas.visischeduler.domain.usecase

import com.markduenas.visischeduler.domain.entities.Message
import com.markduenas.visischeduler.domain.repository.MessageRepository

/**
 * Use case for sending messages in a conversation.
 */
class SendMessageUseCase(
    private val messageRepository: MessageRepository
) {
    /**
     * Send a message to a conversation.
     * @param request The message send request
     * @return Result containing the sent Message or an error
     */
    suspend operator fun invoke(request: SendMessageRequest): Result<Message> {
        // Validate the request
        val validationError = validateRequest(request)
        if (validationError != null) {
            return Result.failure(validationError)
        }

        // Send the message
        return messageRepository.sendMessage(
            conversationId = request.conversationId,
            content = request.content.trim(),
            replyToMessageId = request.replyToMessageId
        )
    }

    private fun validateRequest(request: SendMessageRequest): SendMessageException? {
        // Check for empty content
        if (request.content.isBlank()) {
            return SendMessageException.EmptyContent("Message content cannot be empty")
        }

        // Check for content length (max 5000 characters)
        if (request.content.length > MAX_MESSAGE_LENGTH) {
            return SendMessageException.ContentTooLong(
                "Message cannot exceed $MAX_MESSAGE_LENGTH characters"
            )
        }

        // Check for conversation ID
        if (request.conversationId.isBlank()) {
            return SendMessageException.InvalidConversation("Conversation ID is required")
        }

        return null
    }

    companion object {
        const val MAX_MESSAGE_LENGTH = 5000
    }
}

/**
 * Request data for sending a message.
 */
data class SendMessageRequest(
    val conversationId: String,
    val content: String,
    val replyToMessageId: String? = null
)

/**
 * Exceptions that can occur during message sending.
 */
sealed class SendMessageException(message: String) : Exception(message) {
    class EmptyContent(message: String) : SendMessageException(message)
    class ContentTooLong(message: String) : SendMessageException(message)
    class InvalidConversation(message: String) : SendMessageException(message)
    class ConversationNotFound(message: String) : SendMessageException(message)
    class NotAuthorized(message: String) : SendMessageException(message)
    class NetworkError(message: String) : SendMessageException(message)
    class RateLimited(message: String, val retryAfterSeconds: Int) : SendMessageException(message)
}
