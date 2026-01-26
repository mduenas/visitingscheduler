package com.markduenas.visischeduler.presentation.viewmodel.scheduling

import com.markduenas.visischeduler.common.error.AppException
import com.markduenas.visischeduler.domain.entities.Visit
import com.markduenas.visischeduler.domain.entities.VisitStatus
import com.markduenas.visischeduler.domain.repository.VisitRepository
import com.markduenas.visischeduler.domain.usecase.ApproveVisitUseCase
import com.markduenas.visischeduler.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlin.time.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/**
 * Filter options for pending requests.
 */
enum class PendingRequestsFilter(val displayName: String) {
    ALL("All"),
    TODAY("Today"),
    THIS_WEEK("This Week"),
    UPCOMING("Upcoming")
}

/**
 * UI state for the pending requests screen.
 */
data class PendingRequestsUiState(
    val pendingRequests: List<Visit> = emptyList(),
    val filteredRequests: List<Visit> = emptyList(),
    val selectedFilter: PendingRequestsFilter = PendingRequestsFilter.ALL,
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isProcessing: Boolean = false,
    val error: AppException? = null,
    val selectedVisitIds: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
    val showBulkDenyDialog: Boolean = false,
    val bulkDenialReason: String = ""
) {
    val selectedCount: Int
        get() = selectedVisitIds.size

    val hasSelection: Boolean
        get() = selectedVisitIds.isNotEmpty()

    val isEmpty: Boolean
        get() = filteredRequests.isEmpty() && !isLoading

    val allSelected: Boolean
        get() = filteredRequests.isNotEmpty() &&
                selectedVisitIds.size == filteredRequests.size
}

/**
 * ViewModel for the pending requests screen.
 * Handles viewing, filtering, and batch processing of pending visit requests.
 */
class PendingRequestsViewModel(
    private val visitRepository: VisitRepository,
    private val approveVisitUseCase: ApproveVisitUseCase
) : BaseViewModel<PendingRequestsUiState>(PendingRequestsUiState()) {

    private var loadJob: Job? = null

    init {
        loadPendingRequests()
    }

    /**
     * Load pending visit requests.
     */
    fun loadPendingRequests() {
        loadJob?.cancel()
        updateState { copy(isLoading = true, error = null) }

        loadJob = visitRepository.getPendingApprovalVisits()
            .map { visits -> visits.filter { it.status == VisitStatus.PENDING } }
            .onEach { visits ->
                updateState {
                    copy(
                        pendingRequests = visits,
                        filteredRequests = applyFilters(visits, selectedFilter, searchQuery),
                        isLoading = false,
                        isRefreshing = false
                    )
                }
            }
            .catch { e ->
                val exception = when (e) {
                    is AppException -> e
                    else -> AppException.UnknownException(
                        e.message ?: "Failed to load pending requests",
                        e
                    )
                }
                updateState {
                    copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = exception
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Refresh the list of pending requests.
     */
    fun refresh() {
        updateState { copy(isRefreshing = true) }
        loadPendingRequests()
    }

    /**
     * Set the filter type.
     */
    fun setFilter(filter: PendingRequestsFilter) {
        updateState {
            copy(
                selectedFilter = filter,
                filteredRequests = applyFilters(pendingRequests, filter, searchQuery),
                selectedVisitIds = emptySet()
            )
        }
    }

    /**
     * Update the search query.
     */
    fun setSearchQuery(query: String) {
        updateState {
            copy(
                searchQuery = query,
                filteredRequests = applyFilters(pendingRequests, selectedFilter, query)
            )
        }
    }

    /**
     * Enter selection mode.
     */
    fun enterSelectionMode() {
        updateState { copy(isSelectionMode = true) }
    }

    /**
     * Exit selection mode and clear selections.
     */
    fun exitSelectionMode() {
        updateState {
            copy(
                isSelectionMode = false,
                selectedVisitIds = emptySet()
            )
        }
    }

    /**
     * Toggle selection of a visit.
     */
    fun toggleSelection(visitId: String) {
        updateState {
            val newSelection = if (visitId in selectedVisitIds) {
                selectedVisitIds - visitId
            } else {
                selectedVisitIds + visitId
            }
            copy(
                selectedVisitIds = newSelection,
                isSelectionMode = isSelectionMode || newSelection.isNotEmpty()
            )
        }
    }

    /**
     * Select all visible requests.
     */
    fun selectAll() {
        updateState {
            copy(
                selectedVisitIds = filteredRequests.map { it.id }.toSet(),
                isSelectionMode = true
            )
        }
    }

    /**
     * Deselect all requests.
     */
    fun deselectAll() {
        updateState { copy(selectedVisitIds = emptySet()) }
    }

    /**
     * Approve a single visit.
     */
    fun approveVisit(visitId: String, notes: String? = null) {
        updateState { copy(isProcessing = true) }

        launchSafe {
            val result = approveVisitUseCase.approve(visitId, notes)

            result.fold(
                onSuccess = {
                    updateState { copy(isProcessing = false) }
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
     * Deny a single visit.
     */
    fun denyVisit(visitId: String, reason: String) {
        if (reason.length < 10) {
            showSnackbar("Denial reason must be at least 10 characters")
            return
        }

        updateState { copy(isProcessing = true) }

        launchSafe {
            val result = approveVisitUseCase.deny(visitId, reason)

            result.fold(
                onSuccess = {
                    updateState { copy(isProcessing = false) }
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
     * Bulk approve all selected visits.
     */
    fun bulkApprove(notes: String? = null) {
        val selectedIds = currentState.selectedVisitIds.toList()
        if (selectedIds.isEmpty()) {
            showSnackbar("No visits selected")
            return
        }

        updateState { copy(isProcessing = true) }

        launchSafe {
            val results = approveVisitUseCase.bulkApprove(selectedIds, notes)

            val successCount = results.count { it.value.isSuccess }
            val failCount = results.size - successCount

            updateState {
                copy(
                    isProcessing = false,
                    selectedVisitIds = emptySet(),
                    isSelectionMode = false
                )
            }

            if (failCount > 0) {
                showSnackbar("$successCount approved, $failCount failed")
            } else {
                showSnackbar("$successCount visits approved")
            }
        }
    }

    /**
     * Show the bulk deny dialog.
     */
    fun showBulkDenyDialog() {
        if (currentState.selectedVisitIds.isEmpty()) {
            showSnackbar("No visits selected")
            return
        }
        updateState { copy(showBulkDenyDialog = true) }
    }

    /**
     * Hide the bulk deny dialog.
     */
    fun hideBulkDenyDialog() {
        updateState { copy(showBulkDenyDialog = false, bulkDenialReason = "") }
    }

    /**
     * Update the bulk denial reason.
     */
    fun setBulkDenialReason(reason: String) {
        updateState { copy(bulkDenialReason = reason) }
    }

    /**
     * Bulk deny all selected visits.
     */
    fun bulkDeny() {
        val selectedIds = currentState.selectedVisitIds.toList()
        val reason = currentState.bulkDenialReason

        if (selectedIds.isEmpty()) {
            showSnackbar("No visits selected")
            return
        }

        if (reason.length < 10) {
            showSnackbar("Denial reason must be at least 10 characters")
            return
        }

        updateState { copy(isProcessing = true) }

        launchSafe {
            val results = approveVisitUseCase.bulkDeny(selectedIds, reason)

            val successCount = results.count { it.value.isSuccess }
            val failCount = results.size - successCount

            updateState {
                copy(
                    isProcessing = false,
                    selectedVisitIds = emptySet(),
                    isSelectionMode = false,
                    showBulkDenyDialog = false,
                    bulkDenialReason = ""
                )
            }

            if (failCount > 0) {
                showSnackbar("$successCount denied, $failCount failed")
            } else {
                showSnackbar("$successCount visits denied")
            }
        }
    }

    /**
     * Navigate to visit details.
     */
    fun openVisitDetails(visitId: String) {
        navigate("visitDetails/$visitId")
    }

    /**
     * Clear error state.
     */
    fun clearError() {
        updateState { copy(error = null) }
    }

    private fun applyFilters(
        visits: List<Visit>,
        filter: PendingRequestsFilter,
        query: String
    ): List<Visit> {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val weekEnd = today.plus(DatePeriod(days = 7))

        var filtered = when (filter) {
            PendingRequestsFilter.ALL -> visits
            PendingRequestsFilter.TODAY -> visits.filter { it.scheduledDate == today }
            PendingRequestsFilter.THIS_WEEK -> visits.filter {
                it.scheduledDate >= today && it.scheduledDate <= weekEnd
            }
            PendingRequestsFilter.UPCOMING -> visits.filter { it.scheduledDate > today }
        }

        if (query.isNotBlank()) {
            val lowerQuery = query.lowercase()
            filtered = filtered.filter { visit ->
                visit.visitorId.lowercase().contains(lowerQuery) ||
                visit.beneficiaryId.lowercase().contains(lowerQuery) ||
                visit.purpose?.lowercase()?.contains(lowerQuery) == true
            }
        }

        return filtered.sortedBy { it.scheduledDate }
    }

    override fun onCleared() {
        loadJob?.cancel()
        super.onCleared()
    }
}
