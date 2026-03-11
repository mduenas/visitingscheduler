package com.markduenas.visischeduler.presentation.viewmodel.notifications

import com.markduenas.visischeduler.domain.entities.Notification
import com.markduenas.visischeduler.domain.usecase.GetNotificationsUseCase
import com.markduenas.visischeduler.domain.usecase.MarkNotificationReadUseCase
import com.markduenas.visischeduler.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * UI State for the Notifications list screen.
 */
data class NotificationsUiState(
    val notifications: List<Notification> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val unreadCount: Int
        get() = notifications.count { !it.isRead }
}

/**
 * ViewModel for managing the Notifications list.
 */
class NotificationsViewModel(
    private val getNotificationsUseCase: GetNotificationsUseCase,
    private val markNotificationReadUseCase: MarkNotificationReadUseCase
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
                updateState {
                    copy(
                        notifications = notifications.sortedByDescending { it.createdAt },
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
