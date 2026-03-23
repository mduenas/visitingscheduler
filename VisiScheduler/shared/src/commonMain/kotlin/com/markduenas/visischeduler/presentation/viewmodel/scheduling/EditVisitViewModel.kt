package com.markduenas.visischeduler.presentation.viewmodel.scheduling

import com.markduenas.visischeduler.common.error.AppException
import com.markduenas.visischeduler.domain.entities.AdditionalVisitor
import com.markduenas.visischeduler.domain.entities.Visit
import com.markduenas.visischeduler.domain.entities.VisitType
import com.markduenas.visischeduler.domain.repository.VisitRepository
import com.markduenas.visischeduler.presentation.viewmodel.BaseViewModel
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * UI state for the edit visit screen.
 */
data class EditVisitUiState(
    val originalVisit: Visit? = null,
    val selectedDate: LocalDate = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault()).date,
    val selectedStartTime: LocalTime? = null,
    val selectedDuration: VisitDuration = VisitDuration.ONE_HOUR,
    val visitType: VisitType = VisitType.IN_PERSON,
    val reason: String = "",
    val notes: String = "",
    val additionalVisitors: List<AdditionalVisitor> = emptyList(),
    val videoCallLink: String? = null,
    val videoCallPlatform: String? = null,
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: AppException? = null
) {
    val selectedEndTime: LocalTime?
        get() = selectedStartTime?.let { start ->
            val endMinutes = start.hour * 60 + start.minute + selectedDuration.minutes
            LocalTime((endMinutes / 60).coerceAtMost(23), endMinutes % 60)
        }

    val canSubmit: Boolean
        get() = originalVisit != null && selectedStartTime != null && !isSubmitting && !isLoading
}

/**
 * ViewModel for editing an existing visit.
 * Loads the visit by ID, pre-populates form state, and submits updates.
 */
class EditVisitViewModel(
    private val visitRepository: VisitRepository,
    private val visitId: String
) : BaseViewModel<EditVisitUiState>(EditVisitUiState()) {

    init {
        loadVisit()
    }

    private fun loadVisit() {
        updateState { copy(isLoading = true, error = null) }
        launchSafe {
            visitRepository.getVisitById(visitId).fold(
                onSuccess = { visit ->
                    val durationMinutes = (visit.endTime.hour * 60 + visit.endTime.minute) -
                            (visit.startTime.hour * 60 + visit.startTime.minute)
                    val duration = VisitDuration.entries.minByOrNull {
                        kotlin.math.abs(it.minutes - durationMinutes)
                    } ?: VisitDuration.ONE_HOUR

                    updateState {
                        copy(
                            originalVisit = visit,
                            selectedDate = visit.scheduledDate,
                            selectedStartTime = visit.startTime,
                            selectedDuration = duration,
                            visitType = visit.visitType,
                            reason = visit.purpose ?: "",
                            notes = visit.notes ?: "",
                            additionalVisitors = visit.additionalVisitors,
                            videoCallLink = visit.videoCallLink,
                            videoCallPlatform = visit.videoCallPlatform,
                            isLoading = false
                        )
                    }
                },
                onFailure = { error ->
                    val exception = when (error) {
                        is AppException -> error
                        else -> AppException.UnknownException(
                            error.message ?: "Failed to load visit",
                            error as? Exception
                        )
                    }
                    updateState { copy(isLoading = false, error = exception) }
                }
            )
        }
    }

    fun selectDate(date: LocalDate) {
        updateState { copy(selectedDate = date) }
    }

    fun setStartTime(time: LocalTime) {
        updateState { copy(selectedStartTime = time) }
    }

    fun setDuration(duration: VisitDuration) {
        updateState { copy(selectedDuration = duration) }
    }

    fun setVisitType(type: VisitType) {
        updateState { copy(visitType = type) }
    }

    fun setReason(reason: String) {
        updateState { copy(reason = reason) }
    }

    fun setNotes(notes: String) {
        updateState { copy(notes = notes) }
    }

    fun setVideoCallLink(link: String?) {
        updateState { copy(videoCallLink = link) }
    }

    fun setVideoCallPlatform(platform: String?) {
        updateState { copy(videoCallPlatform = platform) }
    }

    fun incrementGuestCount() {
        val state = currentState
        if (state.additionalVisitors.size < 5) {
            val nextId = (state.additionalVisitors.size + 1).toString()
            updateState {
                copy(
                    additionalVisitors = additionalVisitors + AdditionalVisitor(
                        id = nextId,
                        firstName = "Guest",
                        lastName = nextId,
                        relationship = "Friend"
                    )
                )
            }
        } else {
            showSnackbar("Maximum 5 additional visitors allowed")
        }
    }

    fun decrementGuestCount() {
        if (currentState.additionalVisitors.isNotEmpty()) {
            updateState { copy(additionalVisitors = additionalVisitors.dropLast(1)) }
        }
    }

    fun saveChanges() {
        val state = currentState
        val original = state.originalVisit ?: return
        val startTime = state.selectedStartTime ?: return
        val endTime = state.selectedEndTime ?: return

        updateState { copy(isSubmitting = true, error = null) }

        launchSafe {
            val updated = original.copy(
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

            visitRepository.updateVisit(updated).fold(
                onSuccess = {
                    updateState { copy(isSubmitting = false) }
                    showSnackbar("Visit updated successfully")
                    navigateBack()
                },
                onFailure = { error ->
                    val exception = when (error) {
                        is AppException -> error
                        else -> AppException.UnknownException(
                            error.message ?: "Failed to update visit",
                            error as? Exception
                        )
                    }
                    updateState { copy(isSubmitting = false, error = exception) }
                }
            )
        }
    }
}
