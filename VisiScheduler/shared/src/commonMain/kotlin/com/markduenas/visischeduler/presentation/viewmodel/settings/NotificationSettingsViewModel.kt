package com.markduenas.visischeduler.presentation.viewmodel.settings

import com.markduenas.visischeduler.common.error.AppException
import com.markduenas.visischeduler.domain.entities.NotificationPreferences
import com.markduenas.visischeduler.domain.repository.UserRepository
import com.markduenas.visischeduler.platform.SecureStorage
import com.markduenas.visischeduler.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Reminder time options before a visit.
 */
enum class ReminderTime(val displayName: String, val minutes: Int) {
    MINUTES_30("30 minutes before", 30),
    HOURS_1("1 hour before", 60),
    HOURS_2("2 hours before", 120),
    HOURS_24("24 hours before", 1440),
    DAYS_2("2 days before", 2880)
}

/**
 * UI state for notification settings.
 */
data class NotificationSettingsUiState(
    val pushEnabled: Boolean = true,
    val emailEnabled: Boolean = true,
    val smsEnabled: Boolean = false,
    val inAppEnabled: Boolean = true,
    val visitRemindersEnabled: Boolean = true,
    val approvalNotificationsEnabled: Boolean = true,
    val scheduleChangesEnabled: Boolean = true,
    val reminderTimes: Set<ReminderTime> = setOf(ReminderTime.HOURS_2, ReminderTime.HOURS_24),
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: String = "22:00",
    val quietHoursEnd: String = "07:00",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: AppException? = null
)

/**
 * ViewModel for managing notification settings.
 */
class NotificationSettingsViewModel(
    private val secureStorage: SecureStorage,
    private val userRepository: UserRepository
) : BaseViewModel<NotificationSettingsUiState>(NotificationSettingsUiState()) {

    companion object {
        private const val KEY_PUSH_ENABLED = "notifications_push_enabled"
        private const val KEY_EMAIL_ENABLED = "notifications_email_enabled"
        private const val KEY_SMS_ENABLED = "notifications_sms_enabled"
        private const val KEY_IN_APP_ENABLED = "notifications_in_app_enabled"
        private const val KEY_VISIT_REMINDERS_ENABLED = "notifications_visit_reminders_enabled"
        private const val KEY_APPROVAL_NOTIFICATIONS_ENABLED = "notifications_approval_enabled"
        private const val KEY_SCHEDULE_CHANGES_ENABLED = "notifications_schedule_changes_enabled"
        private const val KEY_REMINDER_TIMES = "notifications_reminder_times"
        private const val KEY_SOUND_ENABLED = "notifications_sound_enabled"
        private const val KEY_VIBRATION_ENABLED = "notifications_vibration_enabled"
        private const val KEY_QUIET_HOURS_ENABLED = "notifications_quiet_hours_enabled"
        private const val KEY_QUIET_HOURS_START = "notifications_quiet_hours_start"
        private const val KEY_QUIET_HOURS_END = "notifications_quiet_hours_end"
    }

    init {
        loadSettings()
    }

    /**
     * Load notification settings from storage.
     */
    private fun loadSettings() {
        updateState { copy(isLoading = true) }

        val pushEnabled = secureStorage.getBoolean(KEY_PUSH_ENABLED) ?: true
        val emailEnabled = secureStorage.getBoolean(KEY_EMAIL_ENABLED) ?: true
        val smsEnabled = secureStorage.getBoolean(KEY_SMS_ENABLED) ?: false
        val inAppEnabled = secureStorage.getBoolean(KEY_IN_APP_ENABLED) ?: true
        val visitRemindersEnabled = secureStorage.getBoolean(KEY_VISIT_REMINDERS_ENABLED) ?: true
        val approvalNotificationsEnabled = secureStorage.getBoolean(KEY_APPROVAL_NOTIFICATIONS_ENABLED) ?: true
        val scheduleChangesEnabled = secureStorage.getBoolean(KEY_SCHEDULE_CHANGES_ENABLED) ?: true
        val soundEnabled = secureStorage.getBoolean(KEY_SOUND_ENABLED) ?: true
        val vibrationEnabled = secureStorage.getBoolean(KEY_VIBRATION_ENABLED) ?: true
        val quietHoursEnabled = secureStorage.getBoolean(KEY_QUIET_HOURS_ENABLED) ?: false
        val quietHoursStart = secureStorage.getString(KEY_QUIET_HOURS_START) ?: "22:00"
        val quietHoursEnd = secureStorage.getString(KEY_QUIET_HOURS_END) ?: "07:00"

        val reminderTimesString = secureStorage.getString(KEY_REMINDER_TIMES) ?: "HOURS_2,HOURS_24"
        val reminderTimes = reminderTimesString.split(",")
            .mapNotNull { name ->
                try {
                    ReminderTime.valueOf(name.trim())
                } catch (e: Exception) {
                    null
                }
            }
            .toSet()

        updateState {
            copy(
                pushEnabled = pushEnabled,
                emailEnabled = emailEnabled,
                smsEnabled = smsEnabled,
                inAppEnabled = inAppEnabled,
                visitRemindersEnabled = visitRemindersEnabled,
                approvalNotificationsEnabled = approvalNotificationsEnabled,
                scheduleChangesEnabled = scheduleChangesEnabled,
                reminderTimes = reminderTimes,
                soundEnabled = soundEnabled,
                vibrationEnabled = vibrationEnabled,
                quietHoursEnabled = quietHoursEnabled,
                quietHoursStart = quietHoursStart,
                quietHoursEnd = quietHoursEnd,
                isLoading = false
            )
        }
    }

    /**
     * Toggle push notifications.
     */
    fun togglePushNotifications(enabled: Boolean) {
        secureStorage.putBoolean(KEY_PUSH_ENABLED, enabled)
        updateState { copy(pushEnabled = enabled) }
        syncPreferencesToServer()
    }

    /**
     * Toggle email notifications.
     */
    fun toggleEmailNotifications(enabled: Boolean) {
        secureStorage.putBoolean(KEY_EMAIL_ENABLED, enabled)
        updateState { copy(emailEnabled = enabled) }
        syncPreferencesToServer()
    }

    /**
     * Toggle SMS notifications.
     */
    fun toggleSmsNotifications(enabled: Boolean) {
        secureStorage.putBoolean(KEY_SMS_ENABLED, enabled)
        updateState { copy(smsEnabled = enabled) }
        syncPreferencesToServer()
    }

    /**
     * Toggle in-app notifications.
     */
    fun toggleInAppNotifications(enabled: Boolean) {
        secureStorage.putBoolean(KEY_IN_APP_ENABLED, enabled)
        updateState { copy(inAppEnabled = enabled) }
    }

    /**
     * Toggle visit reminders.
     */
    fun toggleVisitReminders(enabled: Boolean) {
        secureStorage.putBoolean(KEY_VISIT_REMINDERS_ENABLED, enabled)
        updateState { copy(visitRemindersEnabled = enabled) }
        syncPreferencesToServer()
    }

    /**
     * Toggle approval notifications.
     */
    fun toggleApprovalNotifications(enabled: Boolean) {
        secureStorage.putBoolean(KEY_APPROVAL_NOTIFICATIONS_ENABLED, enabled)
        updateState { copy(approvalNotificationsEnabled = enabled) }
        syncPreferencesToServer()
    }

    /**
     * Toggle schedule change notifications.
     */
    fun toggleScheduleChanges(enabled: Boolean) {
        secureStorage.putBoolean(KEY_SCHEDULE_CHANGES_ENABLED, enabled)
        updateState { copy(scheduleChangesEnabled = enabled) }
        syncPreferencesToServer()
    }

    /**
     * Toggle a reminder time.
     */
    fun toggleReminderTime(reminderTime: ReminderTime) {
        val currentTimes = currentState.reminderTimes.toMutableSet()
        if (currentTimes.contains(reminderTime)) {
            currentTimes.remove(reminderTime)
        } else {
            currentTimes.add(reminderTime)
        }

        val timesString = currentTimes.joinToString(",") { it.name }
        secureStorage.putString(KEY_REMINDER_TIMES, timesString)
        updateState { copy(reminderTimes = currentTimes) }
    }

    /**
     * Toggle sound.
     */
    fun toggleSound(enabled: Boolean) {
        secureStorage.putBoolean(KEY_SOUND_ENABLED, enabled)
        updateState { copy(soundEnabled = enabled) }
    }

    /**
     * Toggle vibration.
     */
    fun toggleVibration(enabled: Boolean) {
        secureStorage.putBoolean(KEY_VIBRATION_ENABLED, enabled)
        updateState { copy(vibrationEnabled = enabled) }
    }

    /**
     * Toggle quiet hours.
     */
    fun toggleQuietHours(enabled: Boolean) {
        secureStorage.putBoolean(KEY_QUIET_HOURS_ENABLED, enabled)
        updateState { copy(quietHoursEnabled = enabled) }
    }

    /**
     * Set quiet hours start time.
     */
    fun setQuietHoursStart(time: String) {
        secureStorage.putString(KEY_QUIET_HOURS_START, time)
        updateState { copy(quietHoursStart = time) }
    }

    /**
     * Set quiet hours end time.
     */
    fun setQuietHoursEnd(time: String) {
        secureStorage.putString(KEY_QUIET_HOURS_END, time)
        updateState { copy(quietHoursEnd = time) }
    }

    /**
     * Sync notification preferences to server.
     */
    private fun syncPreferencesToServer() {
        viewModelScope.launch {
            val preferences = NotificationPreferences(
                emailNotifications = currentState.emailEnabled,
                pushNotifications = currentState.pushEnabled,
                smsNotifications = currentState.smsEnabled,
                visitReminders = currentState.visitRemindersEnabled,
                approvalNotifications = currentState.approvalNotificationsEnabled,
                scheduleChanges = currentState.scheduleChangesEnabled
            )

            userRepository.updateNotificationPreferences(preferences)
        }
    }

    /**
     * Clear error state.
     */
    fun clearError() {
        updateState { copy(error = null) }
    }
}
