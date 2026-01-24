package com.markduenas.visischeduler.presentation.viewmodel.visitors

import com.markduenas.visischeduler.common.error.AppException
import com.markduenas.visischeduler.domain.entities.Role
import com.markduenas.visischeduler.domain.entities.User
import com.markduenas.visischeduler.domain.entities.Visit
import com.markduenas.visischeduler.domain.entities.VisitStatus
import com.markduenas.visischeduler.domain.repository.UserRepository
import com.markduenas.visischeduler.domain.repository.VisitRepository
import com.markduenas.visischeduler.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.Instant

/**
 * Visit frequency statistics for a visitor.
 */
data class VisitFrequency(
    val totalVisits: Int = 0,
    val completedVisits: Int = 0,
    val cancelledVisits: Int = 0,
    val noShowVisits: Int = 0,
    val upcomingVisits: Int = 0,
    val lastVisitDate: Instant? = null,
    val averageVisitsPerMonth: Float = 0f
) {
    val completionRate: Float
        get() = if (totalVisits > 0) {
            (completedVisits.toFloat() / totalVisits) * 100
        } else 0f

    val reliabilityScore: Float
        get() = if (totalVisits > 0) {
            ((completedVisits.toFloat()) / (totalVisits - cancelledVisits).coerceAtLeast(1)) * 100
        } else 0f
}

/**
 * UI State for the visitor details screen.
 */
data class VisitorDetailsUiState(
    val visitor: User? = null,
    val visitHistory: List<Visit> = emptyList(),
    val visitFrequency: VisitFrequency = VisitFrequency(),
    val canEdit: Boolean = false,
    val canApprove: Boolean = false,
    val canBlock: Boolean = false,
    val canRemove: Boolean = false,
    val isLoading: Boolean = false,
    val isProcessing: Boolean = false,
    val error: AppException? = null,
    val showBlockDialog: Boolean = false,
    val showRemoveDialog: Boolean = false
) {
    val isBlocked: Boolean
        get() = visitor?.isActive == false

    val isPending: Boolean
        get() = visitor?.role == Role.PENDING_VISITOR

    val visitorStatus: String
        get() = when {
            isPending -> "Pending Approval"
            isBlocked -> "Blocked"
            else -> "Approved"
        }
}

/**
 * ViewModel for managing the visitor details screen.
 *
 * Provides functionality to:
 * - View visitor profile and contact information
 * - View visit history and statistics
 * - Approve, block, or remove visitors
 * - Edit visitor permissions
 */
class VisitorDetailsViewModel(
    private val userRepository: UserRepository,
    private val visitRepository: VisitRepository,
    private val visitorId: String
) : BaseViewModel<VisitorDetailsUiState>(VisitorDetailsUiState()) {

    init {
        loadVisitorDetails()
        observeCurrentUser()
    }

    private fun loadVisitorDetails() {
        launchSafe {
            updateState { copy(isLoading = true) }

            // Load visitor
            userRepository.getUserById(visitorId)
                .onSuccess { visitor ->
                    updateState {
                        copy(
                            visitor = visitor,
                            isLoading = false,
                            error = null
                        )
                    }
                    loadVisitHistory(visitor.id)
                }
                .onFailure { error ->
                    updateState {
                        copy(
                            isLoading = false,
                            error = error as? AppException
                        )
                    }
                }
        }
    }

    private fun observeCurrentUser() {
        userRepository.currentUser
            .onEach { currentUser ->
                currentUser?.let { user ->
                    updateState {
                        copy(
                            canEdit = user.canManageUsers() || user.canApproveVisits(),
                            canApprove = user.canApproveVisits(),
                            canBlock = user.canApproveVisits(),
                            canRemove = user.canManageUsers()
                        )
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private suspend fun loadVisitHistory(visitorId: String) {
        // Get all visits for this visitor
        val visits = visitRepository.getMyVisits().first()
            .filter { it.visitorId == visitorId }
            .sortedByDescending { it.createdAt }

        val frequency = calculateVisitFrequency(visits)

        updateState {
            copy(
                visitHistory = visits,
                visitFrequency = frequency
            )
        }
    }

    private fun calculateVisitFrequency(visits: List<Visit>): VisitFrequency {
        val totalVisits = visits.size
        val completedVisits = visits.count { it.status == VisitStatus.COMPLETED }
        val cancelledVisits = visits.count { it.status == VisitStatus.CANCELLED }
        val noShowVisits = visits.count { it.status == VisitStatus.NO_SHOW }
        val upcomingVisits = visits.count { it.isUpcoming }
        val lastVisitDate = visits
            .filter { it.status == VisitStatus.COMPLETED }
            .maxByOrNull { it.checkOutTime ?: it.createdAt }
            ?.checkOutTime

        // Calculate average visits per month (last 6 months)
        val averageVisitsPerMonth = if (visits.isNotEmpty()) {
            completedVisits.toFloat() / 6 // Simplified calculation
        } else 0f

        return VisitFrequency(
            totalVisits = totalVisits,
            completedVisits = completedVisits,
            cancelledVisits = cancelledVisits,
            noShowVisits = noShowVisits,
            upcomingVisits = upcomingVisits,
            lastVisitDate = lastVisitDate,
            averageVisitsPerMonth = averageVisitsPerMonth
        )
    }

    /**
     * Approves a pending visitor.
     */
    fun approveVisitor() {
        launchSafe {
            updateState { copy(isProcessing = true) }
            userRepository.approveVisitor(visitorId)
                .onSuccess {
                    showSnackbar("Visitor approved successfully")
                    updateState { copy(isProcessing = false) }
                    loadVisitorDetails()
                }
                .onFailure { error ->
                    updateState { copy(isProcessing = false, error = error as? AppException) }
                    showSnackbar(error.message ?: "Failed to approve visitor")
                }
        }
    }

    /**
     * Blocks a visitor.
     */
    fun blockVisitor(reason: String = "Blocked by coordinator") {
        launchSafe {
            updateState { copy(isProcessing = true, showBlockDialog = false) }
            userRepository.deactivateUser(visitorId)
                .onSuccess {
                    showSnackbar("Visitor blocked successfully")
                    updateState { copy(isProcessing = false) }
                    loadVisitorDetails()
                }
                .onFailure { error ->
                    updateState { copy(isProcessing = false, error = error as? AppException) }
                    showSnackbar(error.message ?: "Failed to block visitor")
                }
        }
    }

    /**
     * Unblocks a visitor.
     */
    fun unblockVisitor() {
        launchSafe {
            updateState { copy(isProcessing = true) }
            userRepository.reactivateUser(visitorId)
                .onSuccess {
                    showSnackbar("Visitor unblocked successfully")
                    updateState { copy(isProcessing = false) }
                    loadVisitorDetails()
                }
                .onFailure { error ->
                    updateState { copy(isProcessing = false, error = error as? AppException) }
                    showSnackbar(error.message ?: "Failed to unblock visitor")
                }
        }
    }

    /**
     * Removes a visitor from the system.
     */
    fun removeVisitor() {
        launchSafe {
            updateState { copy(isProcessing = true, showRemoveDialog = false) }
            userRepository.denyVisitor(visitorId, "Removed from system")
                .onSuccess {
                    showSnackbar("Visitor removed successfully")
                    navigateBack()
                }
                .onFailure { error ->
                    updateState { copy(isProcessing = false, error = error as? AppException) }
                    showSnackbar(error.message ?: "Failed to remove visitor")
                }
        }
    }

    /**
     * Shows the block confirmation dialog.
     */
    fun showBlockDialog() {
        updateState { copy(showBlockDialog = true) }
    }

    /**
     * Hides the block confirmation dialog.
     */
    fun hideBlockDialog() {
        updateState { copy(showBlockDialog = false) }
    }

    /**
     * Shows the remove confirmation dialog.
     */
    fun showRemoveDialog() {
        updateState { copy(showRemoveDialog = true) }
    }

    /**
     * Hides the remove confirmation dialog.
     */
    fun hideRemoveDialog() {
        updateState { copy(showRemoveDialog = false) }
    }

    /**
     * Navigates to edit visitor screen.
     */
    fun onEditClick() {
        navigate("visitors/edit/$visitorId")
    }

    /**
     * Navigates to a specific visit details.
     */
    fun onVisitClick(visitId: String) {
        navigate("visits/details/$visitId")
    }

    /**
     * Refreshes the visitor details.
     */
    fun refresh() {
        loadVisitorDetails()
    }

    /**
     * Clears the current error.
     */
    fun clearError() {
        updateState { copy(error = null) }
    }
}
