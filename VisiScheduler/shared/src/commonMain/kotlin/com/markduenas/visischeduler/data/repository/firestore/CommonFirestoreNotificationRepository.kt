package com.markduenas.visischeduler.data.repository.firestore

import com.markduenas.visischeduler.domain.entities.Notification
import com.markduenas.visischeduler.domain.entities.NotificationPriority
import com.markduenas.visischeduler.domain.entities.NotificationType
import com.markduenas.visischeduler.domain.entities.RelatedEntityType
import com.markduenas.visischeduler.firebase.FirestoreDatabase
import dev.gitlive.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Instant

/**
 * Firestore implementation of notification repository logic.
 */
class CommonFirestoreNotificationRepository(
    private val firestoreDatabase: FirestoreDatabase
) {
    /**
     * Listen to notifications for a user.
     */
    fun listenToNotifications(userId: String): Flow<List<Notification>> {
        return firestoreDatabase.listenToNotifications(userId).map { snapshots ->
            snapshots.map { it.toNotification() }
        }
    }

    /**
     * Mark a notification as read.
     */
    suspend fun markAsRead(notificationId: String) {
        firestoreDatabase.markNotificationRead(notificationId)
    }

    /**
     * Mark all notifications as read for a user.
     */
    suspend fun markAllAsRead(userId: String) {
        // In a real app, this might be a batch update or a function
        try {
            val snapshots = firestoreDatabase.listenToNotifications(userId).first()
            for (snapshot in snapshots) {
                if (!(snapshot.get<Boolean>("isRead") ?: true)) {
                    markAsRead(snapshot.id)
                }
            }
        } catch (e: Exception) {
            // Log error
        }
    }

    /**
     * Delete a notification.
     */
    suspend fun deleteNotification(notificationId: String) {
        firestoreDatabase.deleteNotification(notificationId)
    }

    /**
     * Clear all notifications for a user.
     */
    suspend fun clearAllNotifications(userId: String) {
        try {
            val snapshots = firestoreDatabase.listenToNotifications(userId).first()
            for (snapshot in snapshots) {
                deleteNotification(snapshot.id)
            }
        } catch (e: Exception) {
            // Log error
        }
    }

    /**
     * Convert Firestore snapshot to Notification entity.
     */
    private fun DocumentSnapshot.toNotification(): Notification {
        return Notification(
            id = id,
            userId = get<String>("userId") ?: "",
            title = get<String>("title") ?: "",
            message = get<String>("message") ?: "",
            type = try { NotificationType.valueOf(get<String>("type") ?: "INFO") } catch(e: Exception) { NotificationType.INFO },
            priority = try { get<String>("priority")?.let { NotificationPriority.valueOf(it) } ?: NotificationPriority.NORMAL } catch(e: Exception) { NotificationPriority.NORMAL },
            isRead = get<Boolean>("isRead") ?: false,
            readAt = get<String>("readAt")?.let { Instant.parse(it) },
            relatedEntityId = get<String>("relatedEntityId"),
            relatedEntityType = try { get<String>("relatedEntityType")?.let { RelatedEntityType.valueOf(it) } } catch(e: Exception) { null },
            actionUrl = get<String>("actionUrl"),
            metadata = get<Map<String, String>>("metadata") ?: emptyMap(),
            expiresAt = get<String>("expiresAt")?.let { Instant.parse(it) },
            createdAt = get<String>("timestamp")?.let { Instant.parse(it) } ?: Instant.fromEpochMilliseconds(0)
        )
    }
}
