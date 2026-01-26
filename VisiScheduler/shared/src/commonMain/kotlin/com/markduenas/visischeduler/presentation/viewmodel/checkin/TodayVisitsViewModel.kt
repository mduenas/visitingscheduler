package com.markduenas.visischeduler.presentation.viewmodel.checkin

import com.markduenas.visischeduler.domain.entities.CheckInMethod
import com.markduenas.visischeduler.domain.entities.ExpectedVisitor
import com.markduenas.visischeduler.domain.entities.ExpectedVisitorStatus
import com.markduenas.visischeduler.domain.usecase.CheckInRequest
import com.markduenas.visischeduler.domain.usecase.CheckInUseCase
import com.markduenas.visischeduler.domain.repository.CheckInRepository
import com.markduenas.visischeduler.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * ViewModel for today's expected visitors screen.
 */
class TodayVisitsViewModel(
    private val checkInRepository: CheckInRepository,
    private val checkInUseCase: CheckInUseCase
) : BaseViewModel<TodayVisitsViewState>(TodayVisitsViewState()) {

    init {
        loadTodayVisitors()
    }

    /**
     * Load today's expected visitors.
     */
    fun loadTodayVisitors() {
        launchSafe {
            updateState { copy(isLoading = true) }

            viewModelScope.launch {
                checkInRepository.getTodayExpectedVisitors().collect { visitors ->
                    val grouped = groupVisitors(visitors)
                    updateState {
                        copy(
                            allVisitors = visitors,
                            upcomingVisitors = grouped.upcoming,
                            checkedInVisitors = grouped.checkedIn,
                            checkedOutVisitors = grouped.checkedOut,
                            lateVisitors = grouped.late,
                            noShowVisitors = grouped.noShow,
                            isLoading = false,
                            lastUpdated = Clock.System.now()
                        )
                    }
                }
            }
        }
    }

    /**
     * Load visitors for a specific date.
     */
    fun loadVisitorsForDate(date: LocalDate) {
        launchSafe {
            updateState { copy(isLoading = true, selectedDate = date) }

            viewModelScope.launch {
                checkInRepository.getExpectedVisitorsForDate(date).collect { visitors ->
                    val grouped = groupVisitors(visitors)
                    updateState {
                        copy(
                            allVisitors = visitors,
                            upcomingVisitors = grouped.upcoming,
                            checkedInVisitors = grouped.checkedIn,
                            checkedOutVisitors = grouped.checkedOut,
                            lateVisitors = grouped.late,
                            noShowVisitors = grouped.noShow,
                            isLoading = false,
                            lastUpdated = Clock.System.now()
                        )
                    }
                }
            }
        }
    }

    /**
     * Quick check-in for a visitor.
     */
    fun quickCheckIn(visitor: ExpectedVisitor) {
        launchSafe {
            updateState { copy(checkingInVisitorId = visitor.visit.id) }

            val request = CheckInRequest(
                visitId = visitor.visit.id,
                method = CheckInMethod.MANUAL
            )

            val result = checkInUseCase(request)

            result.fold(
                onSuccess = { checkIn ->
                    updateState { copy(checkingInVisitorId = null) }
                    showSnackbar("${visitor.visitorName} checked in successfully!")
                    // Refresh the list
                    loadTodayVisitors()
                },
                onFailure = { error ->
                    updateState { copy(checkingInVisitorId = null) }
                    showSnackbar(error.message ?: "Check-in failed")
                }
            )
        }
    }

    /**
     * Navigate to visitor check-in details.
     */
    fun viewVisitorDetails(visitor: ExpectedVisitor) {
        navigate("visit/${visitor.visit.id}")
    }

    /**
     * Navigate to QR scanner.
     */
    fun openQrScanner() {
        navigate("scanner")
    }

    /**
     * Refresh the visitors list.
     */
    fun refresh() {
        val date = currentState.selectedDate
        if (date != null) {
            loadVisitorsForDate(date)
        } else {
            loadTodayVisitors()
        }
    }

    /**
     * Set filter for visitor status.
     */
    fun setStatusFilter(filter: VisitorStatusFilter) {
        updateState { copy(statusFilter = filter) }
    }

    /**
     * Search visitors.
     */
    fun searchVisitors(query: String) {
        updateState { copy(searchQuery = query) }
    }

    /**
     * Clear search.
     */
    fun clearSearch() {
        updateState { copy(searchQuery = "") }
    }

    private fun groupVisitors(visitors: List<ExpectedVisitor>): GroupedVisitors {
        return GroupedVisitors(
            upcoming = visitors.filter { it.checkInStatus == ExpectedVisitorStatus.NOT_ARRIVED },
            checkedIn = visitors.filter { it.checkInStatus == ExpectedVisitorStatus.CHECKED_IN },
            checkedOut = visitors.filter { it.checkInStatus == ExpectedVisitorStatus.CHECKED_OUT },
            late = visitors.filter { it.checkInStatus == ExpectedVisitorStatus.LATE },
            noShow = visitors.filter { it.checkInStatus == ExpectedVisitorStatus.NO_SHOW }
        )
    }
}

/**
 * UI state for today's visitors screen.
 */
data class TodayVisitsViewState(
    val allVisitors: List<ExpectedVisitor> = emptyList(),
    val upcomingVisitors: List<ExpectedVisitor> = emptyList(),
    val checkedInVisitors: List<ExpectedVisitor> = emptyList(),
    val checkedOutVisitors: List<ExpectedVisitor> = emptyList(),
    val lateVisitors: List<ExpectedVisitor> = emptyList(),
    val noShowVisitors: List<ExpectedVisitor> = emptyList(),
    val selectedDate: LocalDate? = null,
    val searchQuery: String = "",
    val statusFilter: VisitorStatusFilter = VisitorStatusFilter.ALL,
    val isLoading: Boolean = false,
    val checkingInVisitorId: String? = null,
    val lastUpdated: kotlinx.datetime.Instant? = null
) {
    val displayedVisitors: List<ExpectedVisitor>
        get() {
            val filtered = when (statusFilter) {
                VisitorStatusFilter.ALL -> allVisitors
                VisitorStatusFilter.UPCOMING -> upcomingVisitors
                VisitorStatusFilter.CHECKED_IN -> checkedInVisitors
                VisitorStatusFilter.CHECKED_OUT -> checkedOutVisitors
                VisitorStatusFilter.LATE -> lateVisitors
                VisitorStatusFilter.NO_SHOW -> noShowVisitors
            }

            return if (searchQuery.isBlank()) {
                filtered
            } else {
                filtered.filter {
                    it.visitorName.contains(searchQuery, ignoreCase = true) ||
                    it.beneficiaryName.contains(searchQuery, ignoreCase = true) ||
                    it.beneficiaryRoom?.contains(searchQuery, ignoreCase = true) == true
                }
            }
        }

    val todayDate: LocalDate
        get() = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    val isViewingToday: Boolean
        get() = selectedDate == null || selectedDate == todayDate

    val totalCount: Int
        get() = allVisitors.size

    val checkedInCount: Int
        get() = checkedInVisitors.size

    val pendingCount: Int
        get() = upcomingVisitors.size + lateVisitors.size

    val isEmpty: Boolean
        get() = allVisitors.isEmpty()
}

/**
 * Filter options for visitor status.
 */
enum class VisitorStatusFilter {
    ALL,
    UPCOMING,
    CHECKED_IN,
    CHECKED_OUT,
    LATE,
    NO_SHOW
}

/**
 * Helper class to hold grouped visitors.
 */
private data class GroupedVisitors(
    val upcoming: List<ExpectedVisitor>,
    val checkedIn: List<ExpectedVisitor>,
    val checkedOut: List<ExpectedVisitor>,
    val late: List<ExpectedVisitor>,
    val noShow: List<ExpectedVisitor>
)
