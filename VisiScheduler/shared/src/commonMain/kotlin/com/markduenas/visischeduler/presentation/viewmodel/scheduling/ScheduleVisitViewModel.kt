package com.markduenas.visischeduler.presentation.viewmodel.scheduling

import com.markduenas.visischeduler.common.error.AppException
import com.markduenas.visischeduler.domain.entities.AdditionalVisitor
import com.markduenas.visischeduler.domain.entities.TimeSlot
import com.markduenas.visischeduler.domain.entities.Visit
import com.markduenas.visischeduler.domain.entities.VisitType
import com.markduenas.visischeduler.domain.usecase.GetAvailableSlotsUseCase
import com.markduenas.visischeduler.domain.usecase.ScheduleVisitRequest
import com.markduenas.visischeduler.domain.usecase.ScheduleVisitUseCase
import com.markduenas.visischeduler.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.time.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/**
 * Duration options for visits in minutes.
 */
enum class VisitDuration(val minutes: Int, val displayName: String) {
    FIFTEEN_MINUTES(15, "15 min"),
    THIRTY_MINUTES(30, "30 min"),
    ONE_HOUR(60, "1 hour"),
    ONE_AND_HALF_HOURS(90, "1.5 hours"),
    TWO_HOURS(120, "2 hours")
}

/**
 * UI state for the schedule visit screen.
 */
data class ScheduleVisitUiState(
    val beneficiaryId: String? = null,
    val beneficiaryName: String = "",
    val selectedDate: LocalDate = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault()).date,
    val selectedTimeSlot: TimeSlot? = null,
    val selectedStartTime: LocalTime? = null,
    val availableSlots: List<TimeSlot> = emptyList(),
    val selectedDuration: VisitDuration = VisitDuration.ONE_HOUR,
    val visitType: VisitType = VisitType.IN_PERSON,
    val reason: String = "",
    val notes: String = \"\",
    val additionalVisitors: List<AdditionalVisitor> = emptyList(),
    val videoCallLink: String? = null,
    val videoCallPlatform: String? = null,
    val isLoadingSlots: Boolean = false,

    val isSubmitting: Boolean = false,
    val error: AppException? = null,
    val validationErrors: Map<String, String> = emptyMap(),
    val isSlotSelectionExpanded: Boolean = false,
    val minDate: LocalDate = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault()).date,
    val maxDate: LocalDate = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault()).date
        .plus(DatePeriod(days = 90))
) {
    val canSubmit: Boolean
        get() = beneficiaryId != null &&
                selectedTimeSlot != null &&
                selectedStartTime != null &&
                !isSubmitting &&
                validationErrors.isEmpty()

    val selectedEndTime: LocalTime?
        get() = selectedStartTime?.let { start ->
            val endMinutes = start.hour * 60 + start.minute + selectedDuration.minutes
            val endHour = (endMinutes / 60).coerceAtMost(23)
            val endMinute = endMinutes % 60
            LocalTime(endHour, endMinute)
        }
}

/**
 * ViewModel for scheduling new visits.
 * Handles date selection, time slot loading, and visit submission.
 */
class ScheduleVisitViewModel(
    private val scheduleVisitUseCase: ScheduleVisitUseCase,
    private val getAvailableSlotsUseCase: GetAvailableSlotsUseCase
) : BaseViewModel<ScheduleVisitUiState>(ScheduleVisitUiState()) {

    private var slotsJob: Job? = null

    /**
     * Initialize with a beneficiary.
     */
    fun setBeneficiary(beneficiaryId: String, beneficiaryName: String) {
        updateState {
            copy(
                beneficiaryId = beneficiaryId,
                beneficiaryName = beneficiaryName
            )
        }
        loadAvailableSlots()
    }

    /**
     * Select a date for the visit.
     */
    fun selectDate(date: LocalDate) {
        updateState {
            copy(
                selectedDate = date,
                selectedTimeSlot = null,
                selectedStartTime = null,
                validationErrors = validationErrors - "date"
            )
        }
        loadAvailableSlots()
    }

    /**
     * Select a time slot.
     */
    fun selectTimeSlot(slot: TimeSlot) {
        updateState {
            copy(
                selectedTimeSlot = slot,
                selectedStartTime = slot.startTime,
                isSlotSelectionExpanded = false,
                validationErrors = validationErrors - "timeSlot"
            )
        }
    }

    /**
     * Select a specific start time within a slot.
     */
    fun selectStartTime(time: LocalTime) {
        updateState {
            copy(
                selectedStartTime = time,
                validationErrors = validationErrors - "startTime"
            )
        }
    }

    /**
     * Set the visit duration.
     */
    fun setDuration(duration: VisitDuration) {
        updateState {
            copy(
                selectedDuration = duration,
                validationErrors = validationErrors - "duration"
            )
        }
    }

    /**
     * Set the visit type.
     */
    fun setVisitType(type: VisitType) {
        updateState { copy(visitType = type) }
    }

    /**
     * Update the visit reason/purpose.
     */
    fun setReason(reason: String) {
        updateState {
            copy(
                reason = reason,
                validationErrors = validationErrors - "reason"
            )
        }
    }

    /**
     * Set the video call link.
     */
    fun setVideoCallLink(link: String?) {
        updateState { copy(videoCallLink = link) }
    }

    /**
     * Set the video call platform.
     */
    fun setVideoCallPlatform(platform: String?) {
        updateState { copy(videoCallPlatform = platform) }
    }

    /**
     * Update additional notes.
     */
    fun setNotes(notes: String) {
        updateState { copy(notes = notes) }
    }

    /**
     * Add an additional visitor.
     */
    fun addAdditionalVisitor(visitor: AdditionalVisitor) {
        val currentVisitors = currentState.additionalVisitors
        if (currentVisitors.size < 5) {
            updateState {
                copy(
                    additionalVisitors = currentVisitors + visitor,
                    validationErrors = validationErrors - "additionalVisitors"
                )
            }
        } else {
            showSnackbar("Maximum 5 additional visitors allowed")
        }
    }

    /**
     * Remove an additional visitor.
     */
    fun removeAdditionalVisitor(visitorId: String) {
        updateState {
            copy(
                additionalVisitors = additionalVisitors.filter { it.id != visitorId }
            )
        }
    }

    /**
     * Increment the number of additional guests.
     */
    fun incrementGuestCount() {
        if (currentState.additionalVisitors.size < 5) {
            val nextId = (currentState.additionalVisitors.size + 1).toString()
            addAdditionalVisitor(AdditionalVisitor(nextId, "Guest $nextId", ""))
        }
    }

    /**
     * Decrement the number of additional guests.
     */
    fun decrementGuestCount() {
        if (currentState.additionalVisitors.isNotEmpty()) {
            val lastVisitorId = currentState.additionalVisitors.last().id
            removeAdditionalVisitor(lastVisitorId)
        }
    }

    /**
     * Toggle slot selection expansion.
     */
    fun toggleSlotSelection() {
        updateState { copy(isSlotSelectionExpanded = !isSlotSelectionExpanded) }
    }

    /**
     * Submit the visit request.
     */
    fun submitVisitRequest() {
        val state = currentState

        // Validate inputs
        val errors = mutableMapOf<String, String>()

        if (state.beneficiaryId == null) {
            errors["beneficiary"] = "Please select a beneficiary"
        }

        if (state.selectedTimeSlot == null) {
            errors["timeSlot"] = "Please select a time slot"
        }

        if (state.selectedStartTime == null) {
            errors["startTime"] = "Please select a start time"
        }

        if (errors.isNotEmpty()) {
            updateState { copy(validationErrors = errors) }
            return
        }

        // Calculate end time
        val startTime = state.selectedStartTime!!
        val endTime = state.selectedEndTime!!

        // Validate end time is within slot
        state.selectedTimeSlot?.let { slot ->
            if (endTime > slot.endTime) {
                updateState {
                    copy(
                        validationErrors = mapOf(
                            "duration" to "Visit would extend beyond available slot end time"
                        )
                    )
                }
                return
            }
        }

        updateState { copy(isSubmitting = true, error = null) }

        launchSafe {
            val request = ScheduleVisitRequest(
                visitorId = \"\", // Will be populated by use case from current user
                beneficiaryId = state.beneficiaryId!!,
                scheduledDate = state.selectedDate,
                startTime = startTime,
                endTime = endTime,
                visitType = state.visitType,
                purpose = state.reason.takeIf { it.isNotBlank() },
                notes = state.notes.takeIf { it.isNotBlank() },
                additionalVisitors = state.additionalVisitors,
                videoCallLink = state.videoCallLink,
                videoCallPlatform = state.videoCallPlatform
            )


            val result = scheduleVisitUseCase(request)

            result.fold(
                onSuccess = { visit ->
                    updateState { copy(isSubmitting = false) }
                    showSnackbar("Visit scheduled successfully")
                    onVisitScheduled(visit)
                },
                onFailure = { error ->
                    val exception = when (error) {
                        is AppException -> error
                        else -> AppException.UnknownException(
                            error.message ?: "Failed to schedule visit",
                            error
                        )
                    }
                    updateState {
                        copy(
                            isSubmitting = false,
                            error = exception
                        )
                    }
                }
            )
        }
    }

    /**
     * Clear any error state.
     */
    fun clearError() {
        updateState { copy(error = null) }
    }

    /**
     * Reset the form to initial state.
     */
    fun resetForm() {
        val beneficiaryId = currentState.beneficiaryId
        val beneficiaryName = currentState.beneficiaryName
        updateState {
            ScheduleVisitUiState(
                beneficiaryId = beneficiaryId,
                beneficiaryName = beneficiaryName
            )
        }
        loadAvailableSlots()
    }

    private fun loadAvailableSlots() {
        val beneficiaryId = currentState.beneficiaryId ?: return
        val date = currentState.selectedDate

        slotsJob?.cancel()
        updateState { copy(isLoadingSlots = true, error = null) }

        slotsJob = getAvailableSlotsUseCase(beneficiaryId, date)
            .onEach { slots ->
                updateState {
                    copy(
                        availableSlots = slots,
                        isLoadingSlots = false
                    )
                }
            }
            .catch { e ->
                val exception = when (e) {
                    is AppException -> e
                    else -> AppException.UnknownException(
                        e.message ?: "Failed to load available slots",
                        e
                    )
                }
                updateState {
                    copy(
                        isLoadingSlots = false,
                        error = exception
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun onVisitScheduled(visit: Visit) {
        navigate("visitDetails/${visit.id}")
    }

    override fun onCleared() {
        slotsJob?.cancel()
        super.onCleared()
    }
}
