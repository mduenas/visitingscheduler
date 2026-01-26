package com.markduenas.visischeduler.presentation.viewmodel.settings

import com.markduenas.visischeduler.common.error.AppException
import com.markduenas.visischeduler.domain.entities.Notification
import com.markduenas.visischeduler.domain.entities.NotificationPriority
import com.markduenas.visischeduler.domain.entities.NotificationType
import com.markduenas.visischeduler.presentation.viewmodel.BaseViewModel
import kotlin.time.Clock

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
data class NotificationsListUiState(
    val notifications: List<Notification> = emptyList(),
    val filteredNotifications: List<Notification> = emptyList(),
    val currentFilter: NotificationFilter = NotificationFilter.ALL,
    val unreadCount: Int = 0,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: AppException? = null
) {
    val isEmpty: Boolean
        get() = filteredNotifications.isEmpty() && !isLoading

    val hasUnread: Boolean
        get() = unreadCount > 0
}

/**
 * ViewModel for managing notifications list.
 */
class NotificationsListViewModel(
    // TODO: Inject NotificationRepository when available
) : BaseViewModel<NotificationsListUiState>(NotificationsListUiState()) {

    init {
        loadNotifications()
    }

    /**
     * Load notifications from repository.
     */
    fun loadNotifications() {
        launchSafe {
            updateState { copy(isLoading = true, error = null) }

            // TODO: Replace with actual repository call
            // For now, use mock data
            val mockNotifications = createMockNotifications()

            updateState {
                copy(
                    notifications = mockNotifications,
                    filteredNotifications = filterNotifications(mockNotifications, currentFilter),
                    unreadCount = mockNotifications.count { !it.isRead },
                    isLoading = false
                )
            }
        }
    }

    /**
     * Refresh notifications.
     */
    fun refresh() {
        launchSafe {
            updateState { copy(isRefreshing = true) }
            loadNotifications()
            updateState { copy(isRefreshing = false) }
        }
    }

    /**
     * Set notification filter.
     */
    fun setFilter(filter: NotificationFilter) {
        val filtered = filterNotifications(currentState.notifications, filter)
        updateState {
            copy(
                currentFilter = filter,
                filteredNotifications = filtered
            )
        }
    }

    /**
     * Filter notifications by type.
     */
    private fun filterNotifications(
        notifications: List<Notification>,
        filter: NotificationFilter
    ): List<Notification> {
        return when (filter) {
            NotificationFilter.ALL -> notifications
            NotificationFilter.UNREAD -> notifications.filter { !it.isRead }
            NotificationFilter.VISITS -> notifications.filter {
                it.type in listOf(
                    NotificationType.VISIT_REQUESTED,
                    NotificationType.VISIT_APPROVED,
                    NotificationType.VISIT_DENIED,
                    NotificationType.VISIT_CANCELLED,
                    NotificationType.VISIT_REMINDER,
                    NotificationType.VISIT_CHECKED_IN,
                    NotificationType.VISIT_COMPLETED
                )
            }
            NotificationFilter.APPROVALS -> notifications.filter {
                it.type == NotificationType.APPROVAL_REQUEST
            }
            NotificationFilter.SYSTEM -> notifications.filter {
                it.type in listOf(
                    NotificationType.SYSTEM_ANNOUNCEMENT,
                    NotificationType.ACCOUNT_STATUS,
                    NotificationType.INFO
                )
            }
        }
    }

    /**
     * Mark a notification as read.
     */
    fun markAsRead(notificationId: String) {
        launchSafe {
            // TODO: Call repository to mark as read
            val updatedNotifications = currentState.notifications.map {
                if (it.id == notificationId) {
                    it.copy(isRead = true, readAt = Clock.System.now())
                } else {
                    it
                }
            }

            updateState {
                copy(
                    notifications = updatedNotifications,
                    filteredNotifications = filterNotifications(updatedNotifications, currentFilter),
                    unreadCount = updatedNotifications.count { !it.isRead }
                )
            }
        }
    }

    /**
     * Mark all notifications as read.
     */
    fun markAllAsRead() {
        launchSafe {
            // TODO: Call repository to mark all as read
            val now = Clock.System.now()
            val updatedNotifications = currentState.notifications.map {
                if (!it.isRead) {
                    it.copy(isRead = true, readAt = now)
                } else {
                    it
                }
            }

            updateState {
                copy(
                    notifications = updatedNotifications,
                    filteredNotifications = filterNotifications(updatedNotifications, currentFilter),
                    unreadCount = 0
                )
            }

            showSnackbar("All notifications marked as read")
        }
    }

    /**
     * Delete a notification.
     */
    fun deleteNotification(notificationId: String) {
        launchSafe {
            // TODO: Call repository to delete
            val updatedNotifications = currentState.notifications.filter { it.id != notificationId }

            updateState {
                copy(
                    notifications = updatedNotifications,
                    filteredNotifications = filterNotifications(updatedNotifications, currentFilter),
                    unreadCount = updatedNotifications.count { !it.isRead }
                )
            }
        }
    }

    /**
     * Handle notification tap action.
     */
    fun onNotificationTap(notification: Notification) {
        // Mark as read
        markAsRead(notification.id)

        // Navigate based on notification type
        notification.actionUrl?.let { url ->
            navigate(url)
            return
        }

        // Navigate based on related entity
        when (notification.relatedEntityType) {
            com.markduenas.visischeduler.domain.entities.RelatedEntityType.VISIT -> {
                notification.relatedEntityId?.let { visitId ->
                    navigate("visits/$visitId")
                }
            }
            com.markduenas.visischeduler.domain.entities.RelatedEntityType.USER -> {
                notification.relatedEntityId?.let { userId ->
                    navigate("users/$userId")
                }
            }
            else -> {
                // No specific navigation
            }
        }
    }

    /**
     * Clear error state.
     */
    fun clearError() {
        updateState { copy(error = null) }
    }

    /**
     * Create mock notifications for testing.
     */
    private fun createMockNotifications(): List<Notification> {
        val now = Clock.System.now()
        return listOf(
            Notification(
                id = "1",
                userId = "user1",
                title = "Visit Approved",
                message = "Your visit request for tomorrow at 2:00 PM has been approved.",
                type = NotificationType.VISIT_APPROVED,
                priority = NotificationPriority.NORMAL,
                isRead = false,
                relatedEntityId = "visit1",
                relatedEntityType = com.markduenas.visischeduler.domain.entities.RelatedEntityType.VISIT,
                createdAt = now
            ),
            Notification(
                id = "2",
                userId = "user1",
                title = "Upcoming Visit Reminder",
                message = "Reminder: You have a visit scheduled for today at 3:00 PM.",
                type = NotificationType.VISIT_REMINDER,
                priority = NotificationPriority.HIGH,
                isRead = false,
                relatedEntityId = "visit2",
                relatedEntityType = com.markduenas.visischeduler.domain.entities.RelatedEntityType.VISIT,
                createdAt = now
            ),
            Notification(
                id = "3",
                userId = "user1",
                title = "New Approval Request",
                message = "John Doe has requested to schedule a visit on Friday.",
                type = NotificationType.APPROVAL_REQUEST,
                priority = NotificationPriority.NORMAL,
                isRead = true,
                relatedEntityId = "visit3",
                relatedEntityType = com.markduenas.visischeduler.domain.entities.RelatedEntityType.VISIT,
                createdAt = now
            ),
            Notification(
                id = "4",
                userId = "user1",
                title = "System Maintenance",
                message = "Scheduled maintenance will occur on Sunday from 2 AM to 4 AM.",
                type = NotificationType.SYSTEM_ANNOUNCEMENT,
                priority = NotificationPriority.LOW,
                isRead = true,
                createdAt = now
            )
        )
    }
}
