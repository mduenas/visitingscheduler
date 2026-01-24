package com.markduenas.visischeduler.presentation.viewmodel.checkin

import com.markduenas.visischeduler.domain.entities.CheckIn
import com.markduenas.visischeduler.domain.entities.Visit
import com.markduenas.visischeduler.domain.usecase.CheckOutRequest
import com.markduenas.visischeduler.domain.usecase.CheckOutUseCase
import com.markduenas.visischeduler.domain.repository.CheckInRepository
import com.markduenas.visischeduler.domain.repository.VisitRepository
import com.markduenas.visischeduler.presentation.viewmodel.BaseViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * ViewModel for check-out screen.
 */
class CheckOutViewModel(
    private val checkOutUseCase: CheckOutUseCase,
    private val checkInRepository: CheckInRepository,
    private val visitRepository: VisitRepository
) : BaseViewModel<CheckOutViewState>(CheckOutViewState()) {

    /**
     * Load check-in details for check-out.
     */
    fun loadCheckIn(checkInId: String) {
        launchSafe {
            updateState { copy(isLoading = true) }

            val checkInResult = checkInRepository.getCheckInById(checkInId)
            checkInResult.fold(
                onSuccess = { checkIn ->
                    // Load the associated visit
                    val visitResult = visitRepository.getVisitById(checkIn.visitId)
                    visitResult.fold(
                        onSuccess = { visit ->
                            updateState {
                                copy(
                                    checkIn = checkIn,
                                    visit = visit,
                                    isLoading = false
                                )
                            }
                        },
                        onFailure = { error ->
                            // Still show check-in even if visit load fails
                            updateState {
                                copy(
                                    checkIn = checkIn,
                                    isLoading = false,
                                    error = "Could not load visit details"
                                )
                            }
                        }
                    )
                },
                onFailure = { error ->
                    updateState { copy(isLoading = false, error = error.message) }
                    showSnackbar(error.message ?: "Failed to load check-in")
                }
            )
        }
    }

    /**
     * Update the notes field.
     */
    fun updateNotes(notes: String) {
        updateState { copy(notes = notes) }
    }

    /**
     * Update the rating.
     */
    fun updateRating(rating: Int) {
        if (rating in 1..5) {
            updateState { copy(rating = rating) }
        }
    }

    /**
     * Clear the rating.
     */
    fun clearRating() {
        updateState { copy(rating = null) }
    }

    /**
     * Perform check-out.
     */
    fun checkOut() {
        val checkInId = currentState.checkIn?.id ?: return

        launchSafe {
            updateState { copy(isCheckingOut = true) }

            val request = CheckOutRequest(
                checkInId = checkInId,
                notes = currentState.notes.takeIf { it.isNotBlank() },
                rating = currentState.rating
            )

            val result = checkOutUseCase(request)

            result.fold(
                onSuccess = { updatedCheckIn ->
                    updateState {
                        copy(
                            checkIn = updatedCheckIn,
                            isCheckingOut = false,
                            isCheckedOut = true
                        )
                    }
                    showSnackbar("Successfully checked out!")
                },
                onFailure = { error ->
                    updateState { copy(isCheckingOut = false, error = error.message) }
                    showSnackbar(error.message ?: "Check-out failed")
                }
            )
        }
    }

    /**
     * Navigate to schedule next visit.
     */
    fun scheduleNextVisit() {
        val beneficiaryId = currentState.visit?.beneficiaryId ?: return
        navigate("schedule/new?beneficiaryId=$beneficiaryId")
    }

    /**
     * Navigate back to dashboard.
     */
    fun goToDashboard() {
        navigate("dashboard")
    }

    /**
     * Clear any error state.
     */
    fun clearError() {
        updateState { copy(error = null) }
    }
}

/**
 * UI state for check-out screen.
 */
data class CheckOutViewState(
    val checkIn: CheckIn? = null,
    val visit: Visit? = null,
    val notes: String = "",
    val rating: Int? = null,
    val isLoading: Boolean = false,
    val isCheckingOut: Boolean = false,
    val isCheckedOut: Boolean = false,
    val error: String? = null
) {
    val canCheckOut: Boolean
        get() = checkIn != null && !checkIn.isCheckedOut && !isCheckingOut

    val visitDurationText: String?
        get() {
            val checkInTime = checkIn?.checkInTime ?: return null
            val now = Clock.System.now()
            val durationMillis = now.toEpochMilliseconds() - checkInTime.toEpochMilliseconds()
            val durationMinutes = (durationMillis / 60000).toInt()
            val hours = durationMinutes / 60
            val minutes = durationMinutes % 60
            return if (hours > 0) {
                "${hours}h ${minutes}m"
            } else {
                "${minutes}m"
            }
        }

    val notesCharacterCount: Int
        get() = notes.length

    val notesMaxLength: Int = 500

    val isNotesOverLimit: Boolean
        get() = notes.length > notesMaxLength
}
