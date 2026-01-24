package com.markduenas.visischeduler.presentation.viewmodel.scheduling

import com.markduenas.visischeduler.common.error.AppException
import com.markduenas.visischeduler.domain.entities.User
import com.markduenas.visischeduler.domain.entities.Visit
import com.markduenas.visischeduler.domain.entities.VisitStatus
import com.markduenas.visischeduler.domain.repository.UserRepository
import com.markduenas.visischeduler.domain.repository.VisitRepository
import com.markduenas.visischeduler.domain.usecase.ApproveVisitUseCase
import com.markduenas.visischeduler.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * UI state for the visit details screen.
 */
data class VisitDetailsUiState(
    val visit: Visit? = null,
    val currentUser: User? = null,
    val isLoading: Boolean = true,
    val isProcessing: Boolean = false,
    val error: AppException? = null,
    val showCancelDialog: Boolean = false,
    val showDenyDialog: Boolean = false,
    val cancellationReason: String = "",
    val denialReason: String = ""
) {
    val canEdit: Boolean
        get() = visit?.let { v ->
            (v.status == VisitStatus.PENDING || v.status == VisitStatus.APPROVED) &&
            currentUser?.canScheduleVisits() == true
        } ?: false

    val canCancel: Boolean
        get() = visit?.isCancellable ?: false

    val canApprove: Boolean
        get() = visit?.status == VisitStatus.PENDING &&
                currentUser?.canApproveVisits() == true

    val canDeny: Boolean
        get() = canApprove

    val canCheckIn: Boolean
        get() = visit?.let { v ->
            v.status == VisitStatus.APPROVED &&
            v.checkInTime == null &&
            currentUser?.canApproveVisits() == true
        } ?: false

    val canCheckOut: Boolean
        get() = visit?.let { v ->
            v.status == VisitStatus.APPROVED &&
            v.checkInTime != null &&
            v.checkOutTime == null &&
            currentUser?.canApproveVisits() == true
        } ?: false

    val isVisitor: Boolean
        get() = visit?.visitorId == currentUser?.id

    val isCoordinator: Boolean
        get() = currentUser?.canApproveVisits() == true
}

/**
 * ViewModel for the visit details screen.
 * Handles viewing, editing, cancelling, and approving/denying visits.
 */
class VisitDetailsViewModel(
    private val visitRepository: VisitRepository,
    private val userRepository: UserRepository,
    private val approveVisitUseCase: ApproveVisitUseCase
) : BaseViewModel<VisitDetailsUiState>(VisitDetailsUiState()) {

    private var visitId: String? = null

    /**
     * Initialize with a visit ID.
     */
    fun loadVisit(visitId: String) {
        this.visitId = visitId
        updateState { copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                // Load current user
                val currentUser = userRepository.currentUser.first()

                // Load visit details
                val visitResult = visitRepository.getVisitById(visitId)

                visitResult.fold(
                    onSuccess = { visit ->
                        updateState {
                            copy(
                                visit = visit,
                                currentUser = currentUser,
                                isLoading = false
                            )
                        }
                    },
                    onFailure = { error ->
                        val exception = when (error) {
                            is AppException -> error
                            else -> AppException.DataException.NotFound(
                                "Visit",
                                visitId
                            )
                        }
                        updateState {
                            copy(
                                isLoading = false,
                                error = exception
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                val exception = when (e) {
                    is AppException -> e
                    else -> AppException.UnknownException(
                        e.message ?: "Failed to load visit",
                        e
                    )
                }
                updateState {
                    copy(
                        isLoading = false,
                        error = exception
                    )
                }
            }
        }
    }

    /**
     * Navigate to edit visit screen.
     */
    fun editVisit() {
        visitId?.let { id ->
            navigate("editVisit/$id")
        }
    }

    /**
     * Show the cancel confirmation dialog.
     */
    fun showCancelDialog() {
        updateState { copy(showCancelDialog = true) }
    }

    /**
     * Hide the cancel confirmation dialog.
     */
    fun hideCancelDialog() {
        updateState { copy(showCancelDialog = false, cancellationReason = "") }
    }

    /**
     * Update the cancellation reason.
     */
    fun setCancellationReason(reason: String) {
        updateState { copy(cancellationReason = reason) }
    }

    /**
     * Cancel the visit.
     */
    fun cancelVisit() {
        val id = visitId ?: return
        val reason = currentState.cancellationReason

        if (reason.isBlank()) {
            showSnackbar("Please provide a cancellation reason")
            return
        }

        updateState { copy(isProcessing = true) }

        launchSafe {
            val result = visitRepository.cancelVisit(id, reason)

            result.fold(
                onSuccess = { visit ->
                    updateState {
                        copy(
                            visit = visit,
                            isProcessing = false,
                            showCancelDialog = false,
                            cancellationReason = ""
                        )
                    }
                    showSnackbar("Visit cancelled successfully")
                },
                onFailure = { error ->
                    updateState { copy(isProcessing = false) }
                    val exception = when (error) {
                        is AppException -> error
                        else -> AppException.UnknownException(
                            error.message ?: "Failed to cancel visit",
                            error
                        )
                    }
                    handleError(exception)
                }
            )
        }
    }

    /**
     * Approve the visit request.
     */
    fun approveVisit(notes: String? = null) {
        val id = visitId ?: return

        updateState { copy(isProcessing = true) }

        launchSafe {
            val result = approveVisitUseCase.approve(id, notes)

            result.fold(
                onSuccess = { visit ->
                    updateState {
                        copy(
                            visit = visit,
                            isProcessing = false
                        )
                    }
                    showSnackbar("Visit approved")
                },
                onFailure = { error ->
                    updateState { copy(isProcessing = false) }
                    val exception = when (error) {
                        is AppException -> error
                        else -> AppException.UnknownException(
                            error.message ?: "Failed to approve visit",
                            error
                        )
                    }
                    handleError(exception)
                }
            )
        }
    }

    /**
     * Show the deny confirmation dialog.
     */
    fun showDenyDialog() {
        updateState { copy(showDenyDialog = true) }
    }

    /**
     * Hide the deny confirmation dialog.
     */
    fun hideDenyDialog() {
        updateState { copy(showDenyDialog = false, denialReason = "") }
    }

    /**
     * Update the denial reason.
     */
    fun setDenialReason(reason: String) {
        updateState { copy(denialReason = reason) }
    }

    /**
     * Deny the visit request.
     */
    fun denyVisit() {
        val id = visitId ?: return
        val reason = currentState.denialReason

        if (reason.length < 10) {
            showSnackbar("Please provide a detailed denial reason (at least 10 characters)")
            return
        }

        updateState { copy(isProcessing = true) }

        launchSafe {
            val result = approveVisitUseCase.deny(id, reason)

            result.fold(
                onSuccess = { visit ->
                    updateState {
                        copy(
                            visit = visit,
                            isProcessing = false,
                            showDenyDialog = false,
                            denialReason = ""
                        )
                    }
                    showSnackbar("Visit denied")
                },
                onFailure = { error ->
                    updateState { copy(isProcessing = false) }
                    val exception = when (error) {
                        is AppException -> error
                        else -> AppException.UnknownException(
                            error.message ?: "Failed to deny visit",
                            error
                        )
                    }
                    handleError(exception)
                }
            )
        }
    }

    /**
     * Check in to the visit.
     */
    fun checkIn() {
        val id = visitId ?: return

        updateState { copy(isProcessing = true) }

        launchSafe {
            val result = visitRepository.checkIn(id)

            result.fold(
                onSuccess = { visit ->
                    updateState {
                        copy(
                            visit = visit,
                            isProcessing = false
                        )
                    }
                    showSnackbar("Checked in successfully")
                },
                onFailure = { error ->
                    updateState { copy(isProcessing = false) }
                    val exception = when (error) {
                        is AppException -> error
                        else -> AppException.UnknownException(
                            error.message ?: "Failed to check in",
                            error
                        )
                    }
                    handleError(exception)
                }
            )
        }
    }

    /**
     * Check out from the visit.
     */
    fun checkOut() {
        val id = visitId ?: return

        updateState { copy(isProcessing = true) }

        launchSafe {
            val result = visitRepository.checkOut(id)

            result.fold(
                onSuccess = { visit ->
                    updateState {
                        copy(
                            visit = visit,
                            isProcessing = false
                        )
                    }
                    showSnackbar("Checked out successfully")
                },
                onFailure = { error ->
                    updateState { copy(isProcessing = false) }
                    val exception = when (error) {
                        is AppException -> error
                        else -> AppException.UnknownException(
                            error.message ?: "Failed to check out",
                            error
                        )
                    }
                    handleError(exception)
                }
            )
        }
    }

    /**
     * Mark the visit as no-show.
     */
    fun markAsNoShow() {
        val id = visitId ?: return

        updateState { copy(isProcessing = true) }

        launchSafe {
            val result = visitRepository.markAsNoShow(id)

            result.fold(
                onSuccess = { visit ->
                    updateState {
                        copy(
                            visit = visit,
                            isProcessing = false
                        )
                    }
                    showSnackbar("Marked as no-show")
                },
                onFailure = { error ->
                    updateState { copy(isProcessing = false) }
                    val exception = when (error) {
                        is AppException -> error
                        else -> AppException.UnknownException(
                            error.message ?: "Failed to mark as no-show",
                            error
                        )
                    }
                    handleError(exception)
                }
            )
        }
    }

    /**
     * Refresh visit details.
     */
    fun refresh() {
        visitId?.let { loadVisit(it) }
    }

    /**
     * Clear error state.
     */
    fun clearError() {
        updateState { copy(error = null) }
    }
}
