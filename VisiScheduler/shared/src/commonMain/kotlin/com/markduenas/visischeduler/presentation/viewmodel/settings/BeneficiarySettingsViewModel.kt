package com.markduenas.visischeduler.presentation.viewmodel.settings

import com.markduenas.visischeduler.common.error.AppException
import com.markduenas.visischeduler.domain.entities.Beneficiary
import com.markduenas.visischeduler.platform.SecureStorage
import com.markduenas.visischeduler.presentation.viewmodel.BaseViewModel
import kotlinx.datetime.DayOfWeek

/**
 * Visiting hours configuration for a day.
 */
data class VisitingHours(
    val dayOfWeek: DayOfWeek,
    val isEnabled: Boolean = true,
    val startTime: String = "09:00",
    val endTime: String = "17:00"
)

/**
 * Auto-approve settings.
 */
data class AutoApproveSettings(
    val enabled: Boolean = false,
    val approvedVisitorsOnly: Boolean = true,
    val maxVisitorsPerSlot: Int = 2,
    val requirePhotoId: Boolean = false
)

/**
 * UI state for beneficiary settings.
 */
data class BeneficiarySettingsUiState(
    val beneficiary: Beneficiary? = null,
    val visitingHours: List<VisitingHours> = defaultVisitingHours(),
    val defaultDuration: Int = 60, // minutes
    val maxVisitorsPerSlot: Int = 2,
    val maxVisitsPerDay: Int = 2,
    val maxVisitsPerWeek: Int = 7,
    val bufferTimeBetweenVisits: Int = 15, // minutes
    val autoApproveSettings: AutoApproveSettings = AutoApproveSettings(),
    val specialInstructions: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: AppException? = null
)

/**
 * Default visiting hours for all days.
 */
private fun defaultVisitingHours(): List<VisitingHours> {
    return DayOfWeek.entries.map { day ->
        VisitingHours(
            dayOfWeek = day,
            isEnabled = day != DayOfWeek.SUNDAY,
            startTime = "09:00",
            endTime = "17:00"
        )
    }
}

/**
 * ViewModel for managing beneficiary-specific settings.
 * Only available to coordinators.
 */
class BeneficiarySettingsViewModel(
    private val secureStorage: SecureStorage,
    private val beneficiaryId: String? = null
) : BaseViewModel<BeneficiarySettingsUiState>(BeneficiarySettingsUiState()) {

    companion object {
        private const val KEY_PREFIX = "beneficiary_settings_"
        private const val KEY_DEFAULT_DURATION = "default_duration"
        private const val KEY_MAX_VISITORS_PER_SLOT = "max_visitors_per_slot"
        private const val KEY_MAX_VISITS_PER_DAY = "max_visits_per_day"
        private const val KEY_MAX_VISITS_PER_WEEK = "max_visits_per_week"
        private const val KEY_BUFFER_TIME = "buffer_time"
        private const val KEY_AUTO_APPROVE_ENABLED = "auto_approve_enabled"
        private const val KEY_AUTO_APPROVE_APPROVED_ONLY = "auto_approve_approved_only"
        private const val KEY_REQUIRE_PHOTO_ID = "require_photo_id"
        private const val KEY_SPECIAL_INSTRUCTIONS = "special_instructions"
    }

    private val keyPrefix: String
        get() = "$KEY_PREFIX${beneficiaryId ?: "default"}_"

    init {
        loadSettings()
    }

    /**
     * Load beneficiary settings.
     */
    private fun loadSettings() {
        updateState { copy(isLoading = true) }

        // Load basic settings
        val defaultDuration = secureStorage.getString("${keyPrefix}$KEY_DEFAULT_DURATION")?.toIntOrNull() ?: 60
        val maxVisitorsPerSlot = secureStorage.getString("${keyPrefix}$KEY_MAX_VISITORS_PER_SLOT")?.toIntOrNull() ?: 2
        val maxVisitsPerDay = secureStorage.getString("${keyPrefix}$KEY_MAX_VISITS_PER_DAY")?.toIntOrNull() ?: 2
        val maxVisitsPerWeek = secureStorage.getString("${keyPrefix}$KEY_MAX_VISITS_PER_WEEK")?.toIntOrNull() ?: 7
        val bufferTime = secureStorage.getString("${keyPrefix}$KEY_BUFFER_TIME")?.toIntOrNull() ?: 15
        val specialInstructions = secureStorage.getString("${keyPrefix}$KEY_SPECIAL_INSTRUCTIONS") ?: ""

        // Load auto-approve settings
        val autoApproveEnabled = secureStorage.getBoolean("${keyPrefix}$KEY_AUTO_APPROVE_ENABLED") ?: false
        val autoApproveApprovedOnly = secureStorage.getBoolean("${keyPrefix}$KEY_AUTO_APPROVE_APPROVED_ONLY") ?: true
        val requirePhotoId = secureStorage.getBoolean("${keyPrefix}$KEY_REQUIRE_PHOTO_ID") ?: false

        // Load visiting hours for each day
        val visitingHours = DayOfWeek.entries.map { day ->
            val dayKey = "${keyPrefix}visiting_hours_${day.name.lowercase()}"
            val enabled = secureStorage.getBoolean("${dayKey}_enabled") ?: (day != DayOfWeek.SUNDAY)
            val startTime = secureStorage.getString("${dayKey}_start") ?: "09:00"
            val endTime = secureStorage.getString("${dayKey}_end") ?: "17:00"

            VisitingHours(
                dayOfWeek = day,
                isEnabled = enabled,
                startTime = startTime,
                endTime = endTime
            )
        }

        updateState {
            copy(
                visitingHours = visitingHours,
                defaultDuration = defaultDuration,
                maxVisitorsPerSlot = maxVisitorsPerSlot,
                maxVisitsPerDay = maxVisitsPerDay,
                maxVisitsPerWeek = maxVisitsPerWeek,
                bufferTimeBetweenVisits = bufferTime,
                autoApproveSettings = AutoApproveSettings(
                    enabled = autoApproveEnabled,
                    approvedVisitorsOnly = autoApproveApprovedOnly,
                    maxVisitorsPerSlot = maxVisitorsPerSlot,
                    requirePhotoId = requirePhotoId
                ),
                specialInstructions = specialInstructions,
                isLoading = false
            )
        }
    }

    /**
     * Update visiting hours for a specific day.
     */
    fun updateVisitingHours(dayOfWeek: DayOfWeek, hours: VisitingHours) {
        val dayKey = "${keyPrefix}visiting_hours_${dayOfWeek.name.lowercase()}"

        secureStorage.putBoolean("${dayKey}_enabled", hours.isEnabled)
        secureStorage.putString("${dayKey}_start", hours.startTime)
        secureStorage.putString("${dayKey}_end", hours.endTime)

        val updatedHours = currentState.visitingHours.map {
            if (it.dayOfWeek == dayOfWeek) hours else it
        }
        updateState { copy(visitingHours = updatedHours) }
    }

    /**
     * Toggle a day's visiting hours enabled state.
     */
    fun toggleDayEnabled(dayOfWeek: DayOfWeek, enabled: Boolean) {
        val currentHours = currentState.visitingHours.find { it.dayOfWeek == dayOfWeek } ?: return
        updateVisitingHours(dayOfWeek, currentHours.copy(isEnabled = enabled))
    }

    /**
     * Set start time for a day.
     */
    fun setStartTime(dayOfWeek: DayOfWeek, time: String) {
        val currentHours = currentState.visitingHours.find { it.dayOfWeek == dayOfWeek } ?: return
        updateVisitingHours(dayOfWeek, currentHours.copy(startTime = time))
    }

    /**
     * Set end time for a day.
     */
    fun setEndTime(dayOfWeek: DayOfWeek, time: String) {
        val currentHours = currentState.visitingHours.find { it.dayOfWeek == dayOfWeek } ?: return
        updateVisitingHours(dayOfWeek, currentHours.copy(endTime = time))
    }

    /**
     * Set default visit duration.
     */
    fun setDefaultDuration(minutes: Int) {
        secureStorage.putString("${keyPrefix}$KEY_DEFAULT_DURATION", minutes.toString())
        updateState { copy(defaultDuration = minutes) }
    }

    /**
     * Set maximum visitors per slot.
     */
    fun setMaxVisitorsPerSlot(count: Int) {
        secureStorage.putString("${keyPrefix}$KEY_MAX_VISITORS_PER_SLOT", count.toString())
        updateState {
            copy(
                maxVisitorsPerSlot = count,
                autoApproveSettings = autoApproveSettings.copy(maxVisitorsPerSlot = count)
            )
        }
    }

    /**
     * Set maximum visits per day.
     */
    fun setMaxVisitsPerDay(count: Int) {
        secureStorage.putString("${keyPrefix}$KEY_MAX_VISITS_PER_DAY", count.toString())
        updateState { copy(maxVisitsPerDay = count) }
    }

    /**
     * Set maximum visits per week.
     */
    fun setMaxVisitsPerWeek(count: Int) {
        secureStorage.putString("${keyPrefix}$KEY_MAX_VISITS_PER_WEEK", count.toString())
        updateState { copy(maxVisitsPerWeek = count) }
    }

    /**
     * Set buffer time between visits.
     */
    fun setBufferTime(minutes: Int) {
        secureStorage.putString("${keyPrefix}$KEY_BUFFER_TIME", minutes.toString())
        updateState { copy(bufferTimeBetweenVisits = minutes) }
    }

    /**
     * Toggle auto-approve.
     */
    fun toggleAutoApprove(enabled: Boolean) {
        secureStorage.putBoolean("${keyPrefix}$KEY_AUTO_APPROVE_ENABLED", enabled)
        updateState {
            copy(autoApproveSettings = autoApproveSettings.copy(enabled = enabled))
        }
    }

    /**
     * Toggle auto-approve for approved visitors only.
     */
    fun toggleAutoApproveApprovedOnly(enabled: Boolean) {
        secureStorage.putBoolean("${keyPrefix}$KEY_AUTO_APPROVE_APPROVED_ONLY", enabled)
        updateState {
            copy(autoApproveSettings = autoApproveSettings.copy(approvedVisitorsOnly = enabled))
        }
    }

    /**
     * Toggle require photo ID.
     */
    fun toggleRequirePhotoId(enabled: Boolean) {
        secureStorage.putBoolean("${keyPrefix}$KEY_REQUIRE_PHOTO_ID", enabled)
        updateState {
            copy(autoApproveSettings = autoApproveSettings.copy(requirePhotoId = enabled))
        }
    }

    /**
     * Set special instructions.
     */
    fun setSpecialInstructions(instructions: String) {
        secureStorage.putString("${keyPrefix}$KEY_SPECIAL_INSTRUCTIONS", instructions)
        updateState { copy(specialInstructions = instructions) }
    }

    /**
     * Save all settings.
     */
    fun saveSettings() {
        updateState { copy(isSaving = true) }

        // All settings are saved individually, so just show success
        updateState { copy(isSaving = false) }
        showSnackbar("Settings saved successfully")
        navigateBack()
    }

    /**
     * Reset settings to defaults.
     */
    fun resetToDefaults() {
        // Clear all saved settings
        DayOfWeek.entries.forEach { day ->
            val dayKey = "${keyPrefix}visiting_hours_${day.name.lowercase()}"
            secureStorage.remove("${dayKey}_enabled")
            secureStorage.remove("${dayKey}_start")
            secureStorage.remove("${dayKey}_end")
        }

        secureStorage.remove("${keyPrefix}$KEY_DEFAULT_DURATION")
        secureStorage.remove("${keyPrefix}$KEY_MAX_VISITORS_PER_SLOT")
        secureStorage.remove("${keyPrefix}$KEY_MAX_VISITS_PER_DAY")
        secureStorage.remove("${keyPrefix}$KEY_MAX_VISITS_PER_WEEK")
        secureStorage.remove("${keyPrefix}$KEY_BUFFER_TIME")
        secureStorage.remove("${keyPrefix}$KEY_AUTO_APPROVE_ENABLED")
        secureStorage.remove("${keyPrefix}$KEY_AUTO_APPROVE_APPROVED_ONLY")
        secureStorage.remove("${keyPrefix}$KEY_REQUIRE_PHOTO_ID")
        secureStorage.remove("${keyPrefix}$KEY_SPECIAL_INSTRUCTIONS")

        // Reload defaults
        loadSettings()
        showSnackbar("Settings reset to defaults")
    }

    /**
     * Clear error state.
     */
    fun clearError() {
        updateState { copy(error = null) }
    }
}
