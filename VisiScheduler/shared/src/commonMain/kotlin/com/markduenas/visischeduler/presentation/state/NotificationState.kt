package com.markduenas.visischeduler.presentation.state

import com.markduenas.visischeduler.domain.entities.Notification
import com.markduenas.visischeduler.domain.entities.NotificationType

/**
 * Filter options for notifications.
 */
enum class NotificationFilter {
    ALL,
    UNREAD,
    VISITS,
    APPROVALS,
    SYSTEM
}

/**
 * UI state for notifications list.
 */
data class NotificationsUiState(
    val notifications: List<Notification> = emptyList(),
    val filteredNotifications: List<Notification> = emptyList(),
    val currentFilter: NotificationFilter = NotificationFilter.ALL,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
) {
    val unreadCount: Int
        get() = notifications.count { !it.isRead }

    val isEmpty: Boolean
        get() = filteredNotifications.isEmpty() && !isLoading

    val hasUnread: Boolean
        get() = unreadCount > 0
}
