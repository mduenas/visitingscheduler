package com.markduenas.visischeduler.presentation.viewmodel.dashboard

import com.markduenas.visischeduler.domain.entities.Beneficiary
import com.markduenas.visischeduler.domain.entities.User
import com.markduenas.visischeduler.domain.entities.Visit
import com.markduenas.visischeduler.domain.entities.VisitStatus
import com.markduenas.visischeduler.domain.repository.BeneficiaryRepository
import com.markduenas.visischeduler.domain.repository.UserRepository
import com.markduenas.visischeduler.domain.repository.VisitRepository
import com.markduenas.visischeduler.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * UI state for the visitor dashboard.
 */
data class VisitorDashboardUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val currentUser: User? = null,
    val nextScheduledVisit: Visit? = null,
    val upcomingVisits: List<Visit> = emptyList(),
    val recentVisits: List<Visit> = emptyList(),
    val pendingRequests: List<Visit> = emptyList(),
    val primaryBeneficiary: Beneficiary? = null,
    val associatedBeneficiaries: List<Beneficiary> = emptyList(),
    val visitHistorySummary: VisitHistorySummary = VisitHistorySummary(),
    val errorMessage: String? = null
) {
    val hasNextVisit: Boolean
        get() = nextScheduledVisit != null

    val hasPendingRequests: Boolean
        get() = pendingRequests.isNotEmpty()

    val canScheduleVisit: Boolean
        get() = currentUser?.canScheduleVisits() == true && associatedBeneficiaries.isNotEmpty()

    val welcomeMessage: String
        get() = currentUser?.let { "Welcome, ${it.firstName}!" } ?: "Welcome!"
}

/**
 * Summary of the visitor's visit history.
 */
data class VisitHistorySummary(
    val totalVisits: Int = 0,
    val completedVisits: Int = 0,
    val cancelledVisits: Int = 0,
    val lastVisitDate: LocalDate? = null
)

/**
 * ViewModel for visitor-specific dashboard functionality.
 * Focuses on upcoming visits, request shortcuts, and history.
 */
class VisitorDashboardViewModel(
    private val visitRepository: VisitRepository,
    private val userRepository: UserRepository,
    private val beneficiaryRepository: BeneficiaryRepository
) : BaseViewModel<VisitorDashboardUiState>(VisitorDashboardUiState()) {

    init {
        observeCurrentUser()
        loadDashboardData()
    }

    /**
     * Observes the current user.
     */
    private fun observeCurrentUser() {
        userRepository.currentUser
            .onEach { user ->
                updateState { copy(currentUser = user) }
                user?.let { loadBeneficiaries(it) }
            }
            .catch { handleError(it) }
            .launchIn(viewModelScope)
    }

    /**
     * Loads all visitor dashboard data.
     */
    fun loadDashboardData() {
        launchSafe {
            updateState { copy(isLoading = true, errorMessage = null) }
            observeUpcomingVisits()
            observePendingRequests()
            observePastVisits()
            updateState { copy(isLoading = false) }
        }
    }

    /**
     * Refreshes dashboard data.
     */
    fun refresh() {
        viewModelScope.launch {
            updateState { copy(isRefreshing = true) }
            try {
                visitRepository.syncVisits()
                beneficiaryRepository.syncBeneficiaries()
                loadVisitHistorySummary()
            } catch (e: Exception) {
                handleError(e)
            } finally {
                updateState { copy(isRefreshing = false) }
            }
        }
    }

    /**
     * Observes upcoming visits.
     */
    private fun observeUpcomingVisits() {
        visitRepository.getUpcomingVisits()
            .onEach { visits ->
                val today = getTodayDate()
                val approvedUpcoming = visits
                    .filter { it.status == VisitStatus.APPROVED && it.scheduledDate >= today }
                    .sortedWith(compareBy({ it.scheduledDate }, { it.startTime }))

                updateState {
                    copy(
                        nextScheduledVisit = approvedUpcoming.firstOrNull(),
                        upcomingVisits = approvedUpcoming.take(5)
                    )
                }
            }
            .catch { handleError(it) }
            .launchIn(viewModelScope)
    }

    /**
     * Observes pending visit requests.
     */
    private fun observePendingRequests() {
        visitRepository.getMyVisitsByStatus(VisitStatus.PENDING)
            .onEach { pending ->
                updateState { copy(pendingRequests = pending) }
            }
            .catch { handleError(it) }
            .launchIn(viewModelScope)
    }

    /**
     * Observes past visits for recent history.
     */
    private fun observePastVisits() {
        visitRepository.getPastVisits()
            .onEach { pastVisits ->
                val recent = pastVisits
                    .sortedByDescending { it.scheduledDate }
                    .take(5)

                updateState { copy(recentVisits = recent) }
                loadVisitHistorySummary()
            }
            .catch { handleError(it) }
            .launchIn(viewModelScope)
    }

    /**
     * Loads beneficiaries associated with the visitor.
     */
    private fun loadBeneficiaries(user: User) {
        beneficiaryRepository.getMyBeneficiaries()
            .onEach { beneficiaries ->
                updateState {
                    copy(
                        associatedBeneficiaries = beneficiaries,
                        primaryBeneficiary = beneficiaries.firstOrNull()
                    )
                }
            }
            .catch { handleError(it) }
            .launchIn(viewModelScope)
    }

    /**
     * Loads visit history summary statistics.
     */
    private fun loadVisitHistorySummary() {
        viewModelScope.launch {
            visitRepository.getVisitStatistics()
                .onSuccess { stats ->
                    val lastVisit = currentState.recentVisits
                        .filter { it.status == VisitStatus.COMPLETED }
                        .maxByOrNull { it.scheduledDate }

                    updateState {
                        copy(
                            visitHistorySummary = VisitHistorySummary(
                                totalVisits = stats.totalVisits,
                                completedVisits = stats.completedVisits,
                                cancelledVisits = stats.cancelledVisits,
                                lastVisitDate = lastVisit?.scheduledDate
                            )
                        )
                    }
                }
                .onFailure { handleError(it) }
        }
    }

    /**
     * Initiates a new visit request.
     */
    fun onRequestVisitClick() {
        if (!currentState.canScheduleVisit) {
            showSnackbar("Unable to schedule visits at this time")
            return
        }

        currentState.primaryBeneficiary?.let { beneficiary ->
            navigate("visit/create?beneficiaryId=${beneficiary.id}")
        } ?: navigate("visit/create")
    }

    /**
     * Navigates to visit details.
     */
    fun onVisitClick(visitId: String) {
        navigate("visit/$visitId")
    }

    /**
     * Navigates to visit history.
     */
    fun onViewHistoryClick() {
        navigate("visits/history")
    }

    /**
     * Navigates to beneficiary details.
     */
    fun onBeneficiaryClick(beneficiaryId: String) {
        navigate("beneficiary/$beneficiaryId")
    }

    /**
     * Navigates to pending requests list.
     */
    fun onViewPendingClick() {
        navigate("visits/pending")
    }

    /**
     * Cancels a pending visit request.
     */
    fun cancelPendingRequest(visitId: String, reason: String = "Cancelled by visitor") {
        viewModelScope.launch {
            try {
                visitRepository.cancelVisit(visitId, reason)
                    .onSuccess {
                        showSnackbar("Visit request cancelled")
                    }
                    .onFailure { handleError(it) }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    /**
     * Gets the current date.
     */
    private fun getTodayDate(): LocalDate {
        return Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
    }
}
