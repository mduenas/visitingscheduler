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
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * UI state for the coordinator dashboard.
 */
data class CoordinatorDashboardUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val currentUser: User? = null,
    val pendingApprovalCount: Int = 0,
    val pendingVisits: List<Visit> = emptyList(),
    val todaySchedule: List<ScheduleTimelineItem> = emptyList(),
    val beneficiaries: List<Beneficiary> = emptyList(),
    val selectedBeneficiaryId: String? = null,
    val processingVisitId: String? = null,
    val errorMessage: String? = null
) {
    val selectedBeneficiary: Beneficiary?
        get() = beneficiaries.find { it.id == selectedBeneficiaryId }

    val filteredPendingVisits: List<Visit>
        get() = selectedBeneficiaryId?.let { id ->
            pendingVisits.filter { it.beneficiaryId == id }
        } ?: pendingVisits

    val filteredSchedule: List<ScheduleTimelineItem>
        get() = selectedBeneficiaryId?.let { id ->
            todaySchedule.filter { it.visit.beneficiaryId == id }
        } ?: todaySchedule
}

/**
 * Represents a timeline item in the schedule.
 */
data class ScheduleTimelineItem(
    val visit: Visit,
    val visitorName: String,
    val beneficiaryName: String,
    val timeRange: String,
    val isCurrentOrNext: Boolean = false
)

/**
 * ViewModel for coordinator-specific dashboard functionality.
 * Handles pending approvals, schedule management, and beneficiary switching.
 */
class CoordinatorDashboardViewModel(
    private val visitRepository: VisitRepository,
    private val userRepository: UserRepository,
    private val beneficiaryRepository: BeneficiaryRepository
) : BaseViewModel<CoordinatorDashboardUiState>(CoordinatorDashboardUiState()) {

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
            }
            .catch { handleError(it) }
            .launchIn(viewModelScope)
    }

    /**
     * Loads all coordinator dashboard data.
     */
    fun loadDashboardData() {
        launchSafe {
            updateState { copy(isLoading = true, errorMessage = null) }
            observePendingVisits()
            observeTodaySchedule()
            observeBeneficiaries()
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
            } catch (e: Exception) {
                handleError(e)
            } finally {
                updateState { copy(isRefreshing = false) }
            }
        }
    }

    /**
     * Observes pending visit requests.
     */
    private fun observePendingVisits() {
        visitRepository.getPendingApprovalVisits()
            .onEach { pending ->
                updateState {
                    copy(
                        pendingVisits = pending,
                        pendingApprovalCount = pending.size
                    )
                }
            }
            .catch { handleError(it) }
            .launchIn(viewModelScope)
    }

    /**
     * Observes today's schedule.
     */
    private fun observeTodaySchedule() {
        val today = getTodayDate()
        val endDate = today

        visitRepository.getVisitsInDateRange(today, endDate)
            .onEach { visits ->
                val approvedVisits = visits
                    .filter { it.status == VisitStatus.APPROVED || it.status == VisitStatus.COMPLETED }
                    .sortedBy { it.startTime }

                val currentTime = getCurrentTime()
                val timelineItems = approvedVisits.map { visit ->
                    ScheduleTimelineItem(
                        visit = visit,
                        visitorName = getVisitorName(visit.visitorId),
                        beneficiaryName = getBeneficiaryName(visit.beneficiaryId),
                        timeRange = formatTimeRange(visit.startTime, visit.endTime),
                        isCurrentOrNext = isCurrentOrNextVisit(visit, currentTime)
                    )
                }

                updateState { copy(todaySchedule = timelineItems) }
            }
            .catch { handleError(it) }
            .launchIn(viewModelScope)
    }

    /**
     * Observes beneficiaries for the selector.
     */
    private fun observeBeneficiaries() {
        beneficiaryRepository.getAllBeneficiaries()
            .onEach { beneficiaries ->
                updateState { copy(beneficiaries = beneficiaries) }
            }
            .catch { handleError(it) }
            .launchIn(viewModelScope)
    }

    /**
     * Quickly approves a visit.
     */
    fun quickApprove(visitId: String) {
        viewModelScope.launch {
            updateState { copy(processingVisitId = visitId) }
            try {
                visitRepository.approveVisit(visitId)
                    .onSuccess {
                        showSnackbar("Visit approved")
                    }
                    .onFailure { handleError(it) }
            } finally {
                updateState { copy(processingVisitId = null) }
            }
        }
    }

    /**
     * Quickly denies a visit.
     */
    fun quickDeny(visitId: String, reason: String = "Denied by coordinator") {
        viewModelScope.launch {
            updateState { copy(processingVisitId = visitId) }
            try {
                visitRepository.denyVisit(visitId, reason)
                    .onSuccess {
                        showSnackbar("Visit denied")
                    }
                    .onFailure { handleError(it) }
            } finally {
                updateState { copy(processingVisitId = null) }
            }
        }
    }

    /**
     * Selects a beneficiary for filtering.
     */
    fun selectBeneficiary(beneficiaryId: String?) {
        updateState { copy(selectedBeneficiaryId = beneficiaryId) }
    }

    /**
     * Navigates to visit details for review.
     */
    fun onVisitClick(visitId: String) {
        navigate("visit/$visitId")
    }

    /**
     * Navigates to approval list.
     */
    fun onViewAllPending() {
        navigate("approvals")
    }

    /**
     * Navigates to full schedule view.
     */
    fun onViewFullSchedule() {
        navigate("calendar")
    }

    // Helper functions

    private fun getTodayDate(): LocalDate {
        return Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
    }

    private fun getCurrentTime(): LocalTime {
        return Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .time
    }

    private fun formatTimeRange(start: LocalTime, end: LocalTime): String {
        return "${formatTime(start)} - ${formatTime(end)}"
    }

    private fun formatTime(time: LocalTime): String {
        val hour = time.hour
        val minute = time.minute
        val period = if (hour < 12) "AM" else "PM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return "$displayHour:${minute.toString().padStart(2, '0')} $period"
    }

    private fun isCurrentOrNextVisit(visit: Visit, currentTime: LocalTime): Boolean {
        // Visit is current if time is between start and end
        if (currentTime >= visit.startTime && currentTime <= visit.endTime) {
            return true
        }
        // Visit is next if it's the first one after current time
        return visit.startTime > currentTime &&
                currentState.todaySchedule.none {
                    it.visit.startTime > currentTime && it.visit.startTime < visit.startTime
                }
    }

    private suspend fun getVisitorName(visitorId: String): String {
        return try {
            userRepository.getUserById(visitorId).getOrNull()?.fullName ?: "Unknown Visitor"
        } catch (_: Exception) {
            "Unknown Visitor"
        }
    }

    private suspend fun getBeneficiaryName(beneficiaryId: String): String {
        return try {
            beneficiaryRepository.getBeneficiaryById(beneficiaryId).getOrNull()?.fullName ?: "Unknown"
        } catch (_: Exception) {
            "Unknown"
        }
    }
}
