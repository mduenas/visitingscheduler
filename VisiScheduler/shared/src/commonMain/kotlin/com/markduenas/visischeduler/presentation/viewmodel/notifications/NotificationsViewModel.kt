package com.markduenas.visischeduler.presentation.viewmodel.notifications

import com.markduenas.visischeduler.domain.entities.Notification
import com.markduenas.visischeduler.domain.entities.NotificationType
import com.markduenas.visischeduler.domain.usecase.GetNotificationsUseCase
import com.markduenas.visischeduler.domain.usecase.MarkNotificationReadUseCase
import com.markduenas.visischeduler.domain.usecase.DeleteNotificationUseCase
import com.markduenas.visischeduler.presentation.state.NotificationFilter
import com.markduenas.visischeduler.presentation.state.NotificationsUiState
import com.markduenas.visischeduler.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * ViewModel for managing the Notifications list.
 */
class NotificationsViewModel(
    private val getNotificationsUseCase: GetNotificationsUseCase,
    private val markNotificationReadUseCase: MarkNotificationReadUseCase,
    private val deleteNotificationUseCase: DeleteNotificationUseCase
) : BaseViewModel<NotificationsUiState>(NotificationsUiState()) {

    init {
        loadNotifications()
    }

    /**
     * Loads notifications and listens for real-time updates.
     */
    fun loadNotifications() {
        updateState { copy(isLoading = true) }
        getNotificationsUseCase()
            .onEach { notifications ->
                val sorted = notifications.sortedByDescending { it.createdAt }
                updateState {
                    copy(
                        notifications = sorted,
                        filteredNotifications = filterNotifications(sorted, currentFilter),
                        isLoading = false
                    )
                }
            }
            .catch { error ->
                updateState { copy(isLoading = false, error = error.message) }
            }
            .launchIn(viewModelScope)
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
     * Marks a specific notification as read.
     */
    fun markAsRead(notificationId: String) {
        launchSafe {
            markNotificationReadUseCase.markAsRead(notificationId)
        }
    }

    /**
     * Marks all notifications as read.
     */
    fun markAllAsRead() {
        launchSafe {
            markNotificationReadUseCase.markAllAsRead()
        }
    }

    /**
     * Deletes a specific notification.
     */
    fun deleteNotification(notificationId: String) {
        launchSafe {
            deleteNotificationUseCase.delete(notificationId)
        }
    }

    /**
     * Clears all notifications.
     */
    fun clearAllNotifications() {
        launchSafe {
            deleteNotificationUseCase.clearAll()
        }
    }

    /**
     * Handles clicking on a notification for navigation.
     */
    fun onNotificationClick(notification: Notification) {
        if (!notification.isRead) {
            markAsRead(notification.id)
        }
        
        // Deep linking logic based on notification type/actionUrl
        notification.actionUrl?.let { navigate(it) }
    }
}
