package com.markduenas.visischeduler.domain.usecase

import com.markduenas.visischeduler.domain.repository.NotificationRepository

/**
 * Use case for marking a notification as read.
 */
class MarkNotificationReadUseCase(
    private val notificationRepository: NotificationRepository
) {
    /**
     * Mark a specific notification as read.
     */
    suspend fun markAsRead(notificationId: String): Result<Unit> {
        return notificationRepository.markAsRead(notificationId)
    }

    /**
     * Mark all notifications as read for the current user.
     */
    suspend fun markAllAsRead(): Result<Unit> {
        return notificationRepository.markAllAsRead()
    }
}
