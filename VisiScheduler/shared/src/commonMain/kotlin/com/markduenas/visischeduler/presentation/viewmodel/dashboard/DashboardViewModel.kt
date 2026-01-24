package com.markduenas.visischeduler.presentation.viewmodel.dashboard

import com.markduenas.visischeduler.domain.entities.Beneficiary
import com.markduenas.visischeduler.domain.entities.Role
import com.markduenas.visischeduler.domain.entities.User
import com.markduenas.visischeduler.domain.entities.Visit
import com.markduenas.visischeduler.domain.entities.VisitStatus
import com.markduenas.visischeduler.domain.repository.BeneficiaryRepository
import com.markduenas.visischeduler.domain.repository.UserRepository
import com.markduenas.visischeduler.domain.repository.VisitRepository
import com.markduenas.visischeduler.domain.repository.VisitStatistics
import com.markduenas.visischeduler.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Dashboard state containing all data needed for the main dashboard screen.
 */
data class DashboardUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val currentUser: User? = null,
    val todayVisits: List<Visit> = emptyList(),
    val upcomingVisits: List<Visit> = emptyList(),
    val pendingRequests: List<Visit> = emptyList(),
    val statistics: DashboardStatistics = DashboardStatistics(),
    val beneficiaries: List<Beneficiary> = emptyList(),
    val selectedBeneficiaryId: String? = null,
    val unreadNotificationCount: Int = 0,
    val errorMessage: String? = null
) {
    val isCoordinator: Boolean
        get() = currentUser?.canApproveVisits() == true

    val isVisitor: Boolean
        get() = currentUser?.role == Role.APPROVED_VISITOR ||
                currentUser?.role == Role.PENDING_VISITOR

    val isAdmin: Boolean
        get() = currentUser?.role == Role.ADMIN

    val selectedBeneficiary: Beneficiary?
        get() = beneficiaries.find { it.id == selectedBeneficiaryId }

    val hasPendingRequests: Boolean
        get() = pendingRequests.isNotEmpty()

    val nextVisit: Visit?
        get() = upcomingVisits.firstOrNull()
}

/**
 * Dashboard statistics for quick overview.
 */
data class DashboardStatistics(
    val visitsToday: Int = 0,
    val pendingCount: Int = 0,
    val completedThisWeek: Int = 0,
    val upcomingCount: Int = 0
)

/**
 * Main dashboard ViewModel that manages the home screen state.
 * Provides role-aware data loading and actions.
 */
class DashboardViewModel(
    private val visitRepository: VisitRepository,
    private val userRepository: UserRepository,
    private val beneficiaryRepository: BeneficiaryRepository
) : BaseViewModel<DashboardUiState>(DashboardUiState()) {

    private val _todayDate = MutableStateFlow(getTodayDate())
    val todayDate: StateFlow<LocalDate> = _todayDate.asStateFlow()

    init {
        loadDashboardData()
        observeCurrentUser()
    }

    /**
     * Observes the current user and triggers data reload on changes.
     */
    private fun observeCurrentUser() {
        userRepository.currentUser
            .onEach { user ->
                updateState { copy(currentUser = user) }
                if (user != null) {
                    loadRoleSpecificData(user)
                }
            }
            .catch { handleError(it) }
            .launchIn(viewModelScope)
    }

    /**
     * Loads all dashboard data.
     */
    fun loadDashboardData() {
        launchSafe {
            updateState { copy(isLoading = true, errorMessage = null) }

            // Load visits
            observeVisits()

            // Load beneficiaries
            observeBeneficiaries()

            // Load statistics
            loadStatistics()

            updateState { copy(isLoading = false) }
        }
    }

    /**
     * Refreshes dashboard data (pull-to-refresh).
     */
    fun refresh() {
        viewModelScope.launch {
            updateState { copy(isRefreshing = true) }

            try {
                visitRepository.syncVisits()
                beneficiaryRepository.syncBeneficiaries()
                loadStatistics()
            } catch (e: Exception) {
                handleError(e)
            } finally {
                updateState { copy(isRefreshing = false) }
            }
        }
    }

    /**
     * Observes visit data streams.
     */
    private fun observeVisits() {
        // Observe upcoming visits
        visitRepository.getUpcomingVisits()
            .onEach { visits ->
                val today = getTodayDate()
                val todayVisits = visits.filter { it.scheduledDate == today }
                val upcoming = visits
                    .filter { it.scheduledDate > today || (it.scheduledDate == today && it.status == VisitStatus.APPROVED) }
                    .take(5)

                updateState {
                    copy(
                        todayVisits = todayVisits,
                        upcomingVisits = upcoming
                    )
                }
            }
            .catch { handleError(it) }
            .launchIn(viewModelScope)

        // Observe pending requests (coordinators only)
        visitRepository.getPendingApprovalVisits()
            .onEach { pending ->
                updateState { copy(pendingRequests = pending) }
            }
            .catch { /* Silently ignore for non-coordinators */ }
            .launchIn(viewModelScope)
    }

    /**
     * Observes beneficiary data.
     */
    private fun observeBeneficiaries() {
        beneficiaryRepository.getMyBeneficiaries()
            .onEach { beneficiaries ->
                updateState {
                    copy(
                        beneficiaries = beneficiaries,
                        selectedBeneficiaryId = selectedBeneficiaryId ?: beneficiaries.firstOrNull()?.id
                    )
                }
            }
            .catch { handleError(it) }
            .launchIn(viewModelScope)
    }

    /**
     * Loads role-specific data based on user role.
     */
    private fun loadRoleSpecificData(user: User) {
        when {
            user.canApproveVisits() -> loadCoordinatorData()
            user.canScheduleVisits() -> loadVisitorData()
        }
    }

    /**
     * Loads coordinator-specific data.
     */
    private fun loadCoordinatorData() {
        // Coordinator-specific data loading is handled by observeVisits()
        // which includes pending approvals
    }

    /**
     * Loads visitor-specific data.
     */
    private fun loadVisitorData() {
        // Visitor-specific data is handled by the general observeVisits()
    }

    /**
     * Loads visit statistics.
     */
    private fun loadStatistics() {
        viewModelScope.launch {
            visitRepository.getVisitStatistics()
                .onSuccess { stats ->
                    updateState {
                        copy(
                            statistics = DashboardStatistics(
                                visitsToday = currentState.todayVisits.size,
                                pendingCount = stats.pendingVisits,
                                completedThisWeek = stats.completedVisits,
                                upcomingCount = stats.upcomingVisits
                            )
                        )
                    }
                }
                .onFailure { handleError(it) }
        }
    }

    /**
     * Selects a beneficiary for filtering dashboard data.
     */
    fun selectBeneficiary(beneficiaryId: String) {
        updateState { copy(selectedBeneficiaryId = beneficiaryId) }
    }

    /**
     * Clears the beneficiary filter (show all).
     */
    fun clearBeneficiaryFilter() {
        updateState { copy(selectedBeneficiaryId = null) }
    }

    /**
     * Navigates to visit details.
     */
    fun onVisitClick(visitId: String) {
        navigate("visit/$visitId")
    }

    /**
     * Navigates to create new visit.
     */
    fun onRequestVisitClick() {
        navigate("visit/create")
    }

    /**
     * Navigates to pending approvals list.
     */
    fun onViewAllPendingClick() {
        navigate("approvals")
    }

    /**
     * Navigates to full calendar view.
     */
    fun onViewCalendarClick() {
        navigate("calendar")
    }

    /**
     * Navigates to notifications.
     */
    fun onNotificationsClick() {
        navigate("notifications")
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
