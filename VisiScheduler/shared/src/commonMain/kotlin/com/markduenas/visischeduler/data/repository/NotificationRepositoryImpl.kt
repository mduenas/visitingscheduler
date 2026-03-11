package com.markduenas.visischeduler.data.repository

import com.markduenas.visischeduler.data.local.VisiSchedulerDatabase
import com.markduenas.visischeduler.data.repository.firestore.CommonFirestoreNotificationRepository
import com.markduenas.visischeduler.domain.entities.Notification
import com.markduenas.visischeduler.domain.entities.NotificationChannel
import com.markduenas.visischeduler.domain.entities.NotificationPreferences
import com.markduenas.visischeduler.domain.repository.AuthRepository
import com.markduenas.visischeduler.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Implementation of NotificationRepository.
 */
class NotificationRepositoryImpl(
    private val database: VisiSchedulerDatabase,
    private val firestoreRepository: CommonFirestoreNotificationRepository,
    private val authRepository: AuthRepository
) : NotificationRepository {

    override fun getNotifications(): Flow<List<Notification>> {
        return authRepository.currentUser.flatMapLatest { user ->
            if (user == null) {
                flow { emit(emptyList()) }
            } else {
                firestoreRepository.listenToNotifications(user.id)
            }
        }
    }

    override fun getUnreadCount(): Flow<Int> {
        return getNotifications().map { notifications ->
            notifications.count { !it.isRead }
        }
    }

    override suspend fun markAsRead(notificationId: String): Result<Unit> {
        return try {
            firestoreRepository.markAsRead(notificationId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markAllAsRead(): Result<Unit> {
        return try {
            val user = authRepository.currentUser.first()
            if (user != null) {
                firestoreRepository.markAllAsRead(user.id)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteNotification(notificationId: String): Result<Unit> {
        // Implementation for deleting from Firestore/Local
        return Result.success(Unit)
    }

    override suspend fun clearAllNotifications(): Result<Unit> {
        // Implementation for clearing all
        return Result.success(Unit)
    }

    override suspend fun getNotificationPreferences(): Result<NotificationPreferences> {
        return try {
            val user = authRepository.currentUser.first()
            if (user != null) {
                Result.success(user.notificationPreferences)
            } else {
                Result.failure(Exception("User not authenticated"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateNotificationPreferences(preferences: NotificationPreferences): Result<Unit> {
        // This would call UserRepository to update the preferences
        return Result.success(Unit)
    }

    override suspend fun getNotificationChannels(): Result<List<NotificationChannel>> {
        // Mock data or fetch from remote config
        return Result.success(emptyList())
    }

    override suspend fun updateChannelConfiguration(channelId: String, isEnabled: Boolean): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun syncNotifications(): Result<Unit> {
        // Force a sync if needed
        return Result.success(Unit)
    }
}
