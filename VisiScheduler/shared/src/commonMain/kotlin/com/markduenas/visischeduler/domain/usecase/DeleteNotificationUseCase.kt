package com.markduenas.visischeduler.domain.usecase

import com.markduenas.visischeduler.domain.repository.NotificationRepository

/**
 * Use case for deleting a notification.
 */
class DeleteNotificationUseCase(
    private val notificationRepository: NotificationRepository
) {
    suspend fun delete(notificationId: String): Result<Unit> {
        return notificationRepository.deleteNotification(notificationId)
    }

    suspend fun clearAll(): Result<Unit> {
        return notificationRepository.clearAllNotifications()
    }
}
