package com.markduenas.visischeduler.domain.usecase

import com.markduenas.visischeduler.domain.entities.Notification
import com.markduenas.visischeduler.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case for getting the list of notifications for the current user.
 */
class GetNotificationsUseCase(
    private val notificationRepository: NotificationRepository
) {
    operator fun invoke(): Flow<List<Notification>> {
        return notificationRepository.getNotifications()
    }
}
