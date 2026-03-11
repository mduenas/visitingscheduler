package com.markduenas.visischeduler.domain.repository

import com.markduenas.visischeduler.domain.entities.Notification
import com.markduenas.visischeduler.domain.entities.NotificationChannel
import com.markduenas.visischeduler.domain.entities.NotificationPreferences
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for notification operations.
 */
interface NotificationRepository {
    /**
     * Get all notifications for the current user.
     */
    fun getNotifications(): Flow<List<Notification>>

    /**
     * Get the count of unread notifications.
     */
    fun getUnreadCount(): Flow<Int>

    /**
     * Mark a notification as read.
     */
    suspend fun markAsRead(notificationId: String): Result<Unit>

    /**
     * Mark all notifications as read.
     */
    suspend fun markAllAsRead(): Result<Unit>

    /**
     * Delete a notification.
     */
    suspend fun deleteNotification(notificationId: String): Result<Unit>

    /**
     * Clear all notifications.
     */
    suspend fun clearAllNotifications(): Result<Unit>

    /**
     * Get notification preferences for the current user.
     */
    suspend fun getNotificationPreferences(): Result<NotificationPreferences>

    /**
     * Update notification preferences.
     */
    suspend fun updateNotificationPreferences(preferences: NotificationPreferences): Result<Unit>

    /**
     * Get available notification channels.
     */
    suspend fun getNotificationChannels(): Result<List<NotificationChannel>>

    /**
     * Update notification channel configuration.
     */
    suspend fun updateChannelConfiguration(channelId: String, isEnabled: Boolean): Result<Unit>

    /**
     * Sync notifications from remote server.
     */
    suspend fun syncNotifications(): Result<Unit>
}
