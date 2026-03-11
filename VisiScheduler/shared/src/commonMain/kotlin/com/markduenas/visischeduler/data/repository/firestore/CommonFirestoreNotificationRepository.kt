package com.markduenas.visischeduler.data.repository.firestore

import com.markduenas.visischeduler.domain.entities.Notification
import com.markduenas.visischeduler.domain.entities.NotificationPriority
import com.markduenas.visischeduler.domain.entities.NotificationType
import com.markduenas.visischeduler.domain.entities.RelatedEntityType
import com.markduenas.visischeduler.firebase.FirestoreDatabase
import dev.gitlive.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
        val snapshots = firestoreDatabase.listenToNotifications(userId).map { it }.first() // simplified
        // This is not efficient, but serves as a placeholder for the logic
    }

    /**
     * Convert Firestore snapshot to Notification entity.
     */
    private fun DocumentSnapshot.toNotification(): Notification {
        return Notification(
            id = id,
            userId = get<String>("userId"),
            title = get<String>("title"),
            message = get<String>("message"),
            type = NotificationType.valueOf(get<String>("type")),
            priority = get<String>("priority")?.let { NotificationPriority.valueOf(it) } ?: NotificationPriority.NORMAL,
            isRead = get<Boolean>("isRead"),
            readAt = get<String>("readAt")?.let { Instant.parse(it) },
            relatedEntityId = get<String>("relatedEntityId"),
            relatedEntityType = get<String>("relatedEntityType")?.let { RelatedEntityType.valueOf(it) },
            actionUrl = get<String>("actionUrl"),
            metadata = get<Map<String, String>>("metadata") ?: emptyMap(),
            expiresAt = get<String>("expiresAt")?.let { Instant.parse(it) },
            createdAt = get<String>("timestamp")?.let { Instant.parse(it) } ?: Instant.fromEpochMilliseconds(0)
        )
    }
}

// Extension to get first element of flow for simplified implementation
private suspend fun <T> Flow<T>.first(): T {
    var result: T? = null
    kotlinx.coroutines.flow.first().let { result = it }
    return result!!
}
