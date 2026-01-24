package com.markduenas.visischeduler.presentation.viewmodel.visitors

import com.markduenas.visischeduler.common.error.AppException
import com.markduenas.visischeduler.domain.entities.ApprovalLevel
import com.markduenas.visischeduler.domain.entities.BeneficiaryConstraints
import com.markduenas.visischeduler.domain.entities.RestrictionScope
import com.markduenas.visischeduler.domain.entities.RestrictionType
import com.markduenas.visischeduler.domain.entities.TimeConstraints
import com.markduenas.visischeduler.domain.entities.VisitorConstraints
import com.markduenas.visischeduler.domain.repository.RestrictionRepository
import com.markduenas.visischeduler.presentation.viewmodel.BaseViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Recurrence options for restrictions.
 */
enum class RecurrenceType(val displayName: String) {
    NONE("Does not repeat"),
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly")
}

/**
 * UI State for the add restriction screen.
 */
data class AddRestrictionUiState(
    // Basic info
    val name: String = "",
    val description: String = "",
    val restrictionType: RestrictionType = RestrictionType.TIME_BASED,
    val scope: RestrictionScope = RestrictionScope.FACILITY_WIDE,
    val priority: Int = 0,

    // Date range
    val startDate: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
    val endDate: LocalDate? = null,
    val isPermanent: Boolean = true,
    val recurrence: RecurrenceType = RecurrenceType.NONE,

    // Time constraints
    val selectedDays: Set<DayOfWeek> = emptySet(),
    val blockedDays: Set<DayOfWeek> = emptySet(),
    val earliestStartTime: LocalTime? = null,
    val latestEndTime: LocalTime? = null,
    val maxDurationMinutes: Int? = null,
    val minAdvanceBookingHours: Int? = null,
    val maxAdvanceBookingDays: Int? = null,
    val requiredGapHours: Int? = null,

    // Visitor constraints
    val blockedVisitorIds: Set<String> = emptySet(),
    val allowedVisitorIds: Set<String> = emptySet(),
    val maxVisitsPerDay: Int? = null,
    val maxVisitsPerWeek: Int? = null,
    val maxVisitsPerMonth: Int? = null,
    val requiredApprovalLevel: ApprovalLevel = ApprovalLevel.ANY_COORDINATOR,
    val requiresEscort: Boolean = false,
    val canBringGuests: Boolean = true,
    val maxAdditionalGuests: Int? = null,

    // Capacity constraints
    val maxSimultaneousVisitors: Int? = null,
    val maxDailyVisits: Int? = null,
    val restPeriodHours: Int? = null,
    val requiresMedicalClearance: Boolean = false,
    val specialInstructions: String = "",

    // UI state
    val isLoading: Boolean = false,
    val error: AppException? = null,
    val validationErrors: Map<String, String> = emptyMap(),
    val isSaved: Boolean = false,

    // Dialogs
    val showTimePickerDialog: Boolean = false,
    val showDatePickerDialog: Boolean = false,
    val timePickerTarget: TimePickerTarget? = null,
    val datePickerTarget: DatePickerTarget? = null
) {
    val isValid: Boolean
        get() = name.isNotBlank() &&
                description.isNotBlank() &&
                validationErrors.isEmpty() &&
                hasValidTypeSpecificData

    private val hasValidTypeSpecificData: Boolean
        get() = when (restrictionType) {
            RestrictionType.TIME_BASED -> selectedDays.isNotEmpty() || blockedDays.isNotEmpty() ||
                    earliestStartTime != null || latestEndTime != null
            RestrictionType.VISITOR_BASED -> blockedVisitorIds.isNotEmpty() ||
                    allowedVisitorIds.isNotEmpty() || maxVisitsPerDay != null ||
                    maxVisitsPerWeek != null || maxVisitsPerMonth != null
            RestrictionType.CAPACITY_BASED -> maxSimultaneousVisitors != null || maxDailyVisits != null
            RestrictionType.BENEFICIARY_BASED -> restPeriodHours != null || requiresMedicalClearance
            RestrictionType.RELATIONSHIP_BASED -> true
            RestrictionType.COMBINED -> true
        }
}

enum class TimePickerTarget {
    EARLIEST_START,
    LATEST_END
}

enum class DatePickerTarget {
    START_DATE,
    END_DATE
}

/**
 * ViewModel for managing the add restriction screen.
 *
 * Provides functionality to:
 * - Select restriction type
 * - Configure type-specific parameters
 * - Set date range and recurrence
 * - Create complex restriction rules
 */
class AddRestrictionViewModel(
    private val restrictionRepository: RestrictionRepository
) : BaseViewModel<AddRestrictionUiState>(AddRestrictionUiState()) {

    // Basic info
    fun onNameChange(value: String) {
        updateState {
            copy(
                name = value,
                validationErrors = validationErrors - "name"
            )
        }
    }

    fun onDescriptionChange(value: String) {
        updateState {
            copy(
                description = value,
                validationErrors = validationErrors - "description"
            )
        }
    }

    fun onRestrictionTypeChange(type: RestrictionType) {
        updateState { copy(restrictionType = type) }
    }

    fun onScopeChange(scope: RestrictionScope) {
        updateState { copy(scope = scope) }
    }

    fun onPriorityChange(priority: Int) {
        updateState { copy(priority = priority.coerceIn(0, 10)) }
    }

    // Date range
    fun onStartDateChange(date: LocalDate) {
        updateState {
            copy(
                startDate = date,
                showDatePickerDialog = false,
                datePickerTarget = null
            )
        }
    }

    fun onEndDateChange(date: LocalDate?) {
        updateState {
            copy(
                endDate = date,
                showDatePickerDialog = false,
                datePickerTarget = null
            )
        }
    }

    fun onPermanentToggle(isPermanent: Boolean) {
        updateState {
            copy(
                isPermanent = isPermanent,
                endDate = if (isPermanent) null else endDate
            )
        }
    }

    fun onRecurrenceChange(recurrence: RecurrenceType) {
        updateState { copy(recurrence = recurrence) }
    }

    // Time constraints
    fun onDayToggle(day: DayOfWeek) {
        updateState {
            val newDays = if (selectedDays.contains(day)) {
                selectedDays - day
            } else {
                selectedDays + day
            }
            copy(selectedDays = newDays)
        }
    }

    fun onBlockedDayToggle(day: DayOfWeek) {
        updateState {
            val newDays = if (blockedDays.contains(day)) {
                blockedDays - day
            } else {
                blockedDays + day
            }
            copy(blockedDays = newDays)
        }
    }

    fun onEarliestStartTimeChange(time: LocalTime?) {
        updateState {
            copy(
                earliestStartTime = time,
                showTimePickerDialog = false,
                timePickerTarget = null
            )
        }
    }

    fun onLatestEndTimeChange(time: LocalTime?) {
        updateState {
            copy(
                latestEndTime = time,
                showTimePickerDialog = false,
                timePickerTarget = null
            )
        }
    }

    fun onMaxDurationChange(minutes: Int?) {
        updateState { copy(maxDurationMinutes = minutes?.coerceAtLeast(0)) }
    }

    fun onMinAdvanceBookingChange(hours: Int?) {
        updateState { copy(minAdvanceBookingHours = hours?.coerceAtLeast(0)) }
    }

    fun onMaxAdvanceBookingChange(days: Int?) {
        updateState { copy(maxAdvanceBookingDays = days?.coerceAtLeast(0)) }
    }

    fun onRequiredGapChange(hours: Int?) {
        updateState { copy(requiredGapHours = hours?.coerceAtLeast(0)) }
    }

    // Visitor constraints
    fun onAddBlockedVisitor(visitorId: String) {
        updateState { copy(blockedVisitorIds = blockedVisitorIds + visitorId) }
    }

    fun onRemoveBlockedVisitor(visitorId: String) {
        updateState { copy(blockedVisitorIds = blockedVisitorIds - visitorId) }
    }

    fun onAddAllowedVisitor(visitorId: String) {
        updateState { copy(allowedVisitorIds = allowedVisitorIds + visitorId) }
    }

    fun onRemoveAllowedVisitor(visitorId: String) {
        updateState { copy(allowedVisitorIds = allowedVisitorIds - visitorId) }
    }

    fun onMaxVisitsPerDayChange(value: Int?) {
        updateState { copy(maxVisitsPerDay = value?.coerceAtLeast(0)) }
    }

    fun onMaxVisitsPerWeekChange(value: Int?) {
        updateState { copy(maxVisitsPerWeek = value?.coerceAtLeast(0)) }
    }

    fun onMaxVisitsPerMonthChange(value: Int?) {
        updateState { copy(maxVisitsPerMonth = value?.coerceAtLeast(0)) }
    }

    fun onApprovalLevelChange(level: ApprovalLevel) {
        updateState { copy(requiredApprovalLevel = level) }
    }

    fun onRequiresEscortToggle(required: Boolean) {
        updateState { copy(requiresEscort = required) }
    }

    fun onCanBringGuestsToggle(canBring: Boolean) {
        updateState { copy(canBringGuests = canBring) }
    }

    fun onMaxAdditionalGuestsChange(max: Int?) {
        updateState { copy(maxAdditionalGuests = max?.coerceAtLeast(0)) }
    }

    // Capacity constraints
    fun onMaxSimultaneousVisitorsChange(max: Int?) {
        updateState { copy(maxSimultaneousVisitors = max?.coerceAtLeast(0)) }
    }

    fun onMaxDailyVisitsChange(max: Int?) {
        updateState { copy(maxDailyVisits = max?.coerceAtLeast(0)) }
    }

    fun onRestPeriodHoursChange(hours: Int?) {
        updateState { copy(restPeriodHours = hours?.coerceAtLeast(0)) }
    }

    fun onRequiresMedicalClearanceToggle(required: Boolean) {
        updateState { copy(requiresMedicalClearance = required) }
    }

    fun onSpecialInstructionsChange(instructions: String) {
        updateState { copy(specialInstructions = instructions) }
    }

    // Dialogs
    fun showTimePicker(target: TimePickerTarget) {
        updateState {
            copy(
                showTimePickerDialog = true,
                timePickerTarget = target
            )
        }
    }

    fun hideTimePicker() {
        updateState {
            copy(
                showTimePickerDialog = false,
                timePickerTarget = null
            )
        }
    }

    fun showDatePicker(target: DatePickerTarget) {
        updateState {
            copy(
                showDatePickerDialog = true,
                datePickerTarget = target
            )
        }
    }

    fun hideDatePicker() {
        updateState {
            copy(
                showDatePickerDialog = false,
                datePickerTarget = null
            )
        }
    }

    /**
     * Validates the form.
     */
    private fun validate(): Boolean {
        val errors = mutableMapOf<String, String>()

        if (currentState.name.isBlank()) {
            errors["name"] = "Name is required"
        }

        if (currentState.description.isBlank()) {
            errors["description"] = "Description is required"
        }

        if (!currentState.isPermanent && currentState.endDate == null) {
            errors["endDate"] = "End date is required for non-permanent restrictions"
        }

        if (currentState.endDate != null && currentState.endDate < currentState.startDate) {
            errors["endDate"] = "End date must be after start date"
        }

        if (currentState.earliestStartTime != null && currentState.latestEndTime != null &&
            currentState.earliestStartTime >= currentState.latestEndTime) {
            errors["time"] = "End time must be after start time"
        }

        updateState { copy(validationErrors = errors) }
        return errors.isEmpty()
    }

    /**
     * Saves the restriction.
     */
    fun saveRestriction() {
        if (!validate()) {
            showSnackbar("Please fix the validation errors")
            return
        }

        launchSafe {
            updateState { copy(isLoading = true) }

            val timeConstraints = if (currentState.restrictionType == RestrictionType.TIME_BASED ||
                currentState.restrictionType == RestrictionType.COMBINED) {
                TimeConstraints(
                    allowedDays = currentState.selectedDays.toList().takeIf { it.isNotEmpty() },
                    blockedDays = currentState.blockedDays.toList().takeIf { it.isNotEmpty() },
                    earliestStartTime = currentState.earliestStartTime,
                    latestEndTime = currentState.latestEndTime,
                    maxDurationMinutes = currentState.maxDurationMinutes,
                    minAdvanceBookingHours = currentState.minAdvanceBookingHours,
                    maxAdvanceBookingDays = currentState.maxAdvanceBookingDays,
                    requiredGapBetweenVisitsHours = currentState.requiredGapHours
                )
            } else null

            val visitorConstraints = if (currentState.restrictionType == RestrictionType.VISITOR_BASED ||
                currentState.restrictionType == RestrictionType.COMBINED) {
                VisitorConstraints(
                    blockedVisitorIds = currentState.blockedVisitorIds.toList().takeIf { it.isNotEmpty() },
                    allowedVisitorIds = currentState.allowedVisitorIds.toList().takeIf { it.isNotEmpty() },
                    maxVisitsPerDay = currentState.maxVisitsPerDay,
                    maxVisitsPerWeek = currentState.maxVisitsPerWeek,
                    maxVisitsPerMonth = currentState.maxVisitsPerMonth,
                    requiredApprovalLevel = currentState.requiredApprovalLevel,
                    requiresEscort = currentState.requiresEscort,
                    canBringGuests = currentState.canBringGuests,
                    maxAdditionalGuests = currentState.maxAdditionalGuests
                )
            } else null

            val beneficiaryConstraints = if (currentState.restrictionType == RestrictionType.CAPACITY_BASED ||
                currentState.restrictionType == RestrictionType.BENEFICIARY_BASED ||
                currentState.restrictionType == RestrictionType.COMBINED) {
                BeneficiaryConstraints(
                    maxSimultaneousVisitors = currentState.maxSimultaneousVisitors,
                    maxVisitsPerDay = currentState.maxDailyVisits,
                    restPeriodHours = currentState.restPeriodHours,
                    requiresMedicalClearance = currentState.requiresMedicalClearance,
                    specialInstructions = currentState.specialInstructions.takeIf { it.isNotBlank() }
                )
            } else null

            restrictionRepository.createRestriction(
                name = currentState.name,
                description = currentState.description,
                type = currentState.restrictionType,
                scope = currentState.scope,
                priority = currentState.priority,
                effectiveFrom = currentState.startDate,
                effectiveUntil = currentState.endDate,
                timeConstraints = timeConstraints,
                visitorConstraints = visitorConstraints,
                beneficiaryConstraints = beneficiaryConstraints
            )
                .onSuccess { restriction ->
                    updateState { copy(isLoading = false, isSaved = true) }
                    showSnackbar("Restriction created successfully")
                    navigateBack()
                }
                .onFailure { error ->
                    updateState {
                        copy(
                            isLoading = false,
                            error = error as? AppException
                        )
                    }
                    showSnackbar(error.message ?: "Failed to create restriction")
                }
        }
    }

    /**
     * Clears the form.
     */
    fun clearForm() {
        updateState { AddRestrictionUiState() }
    }

    /**
     * Clears the current error.
     */
    fun clearError() {
        updateState { copy(error = null) }
    }

    /**
     * Navigates back.
     */
    fun onBackClick() {
        navigateBack()
    }
}
