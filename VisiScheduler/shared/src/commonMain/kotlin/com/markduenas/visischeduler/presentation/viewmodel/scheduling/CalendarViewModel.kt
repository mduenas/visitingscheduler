package com.markduenas.visischeduler.presentation.viewmodel.scheduling

import com.markduenas.visischeduler.common.error.AppException
import com.markduenas.visischeduler.domain.entities.Visit
import com.markduenas.visischeduler.domain.repository.VisitRepository
import com.markduenas.visischeduler.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/**
 * Calendar view mode options.
 */
enum class CalendarViewMode {
    DAY,
    WEEK,
    MONTH
}

/**
 * UI state for the calendar screen.
 */
data class CalendarUiState(
    val selectedDate: LocalDate = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault()).date,
    val viewMode: CalendarViewMode = CalendarViewMode.WEEK,
    val visitsForSelectedDate: List<Visit> = emptyList(),
    val visitsForDateRange: Map<LocalDate, List<Visit>> = emptyMap(),
    val isLoading: Boolean = false,
    val error: AppException? = null,
    val currentMonthStart: LocalDate = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault()).date.let { today ->
            LocalDate(today.year, today.month, 1)
        }
)

/**
 * ViewModel for the calendar screen.
 * Manages calendar display, view mode switching, and visit loading.
 */
class CalendarViewModel(
    private val visitRepository: VisitRepository
) : BaseViewModel<CalendarUiState>(CalendarUiState()) {

    private val _selectedDateVisits = MutableStateFlow<List<Visit>>(emptyList())
    val selectedDateVisits: StateFlow<List<Visit>> = _selectedDateVisits.asStateFlow()

    init {
        loadVisitsForCurrentRange()
    }

    /**
     * Select a specific date on the calendar.
     */
    fun selectDate(date: LocalDate) {
        updateState { copy(selectedDate = date) }
        loadVisitsForDate(date)
    }

    /**
     * Change the calendar view mode (day, week, month).
     */
    fun setViewMode(mode: CalendarViewMode) {
        updateState { copy(viewMode = mode) }
        loadVisitsForCurrentRange()
    }

    /**
     * Navigate to the next period (day, week, or month depending on view mode).
     */
    fun navigateNext() {
        val currentState = currentState
        val newDate = when (currentState.viewMode) {
            CalendarViewMode.DAY -> currentState.selectedDate.plus(DatePeriod(days = 1))
            CalendarViewMode.WEEK -> currentState.selectedDate.plus(DatePeriod(days = 7))
            CalendarViewMode.MONTH -> {
                val newMonthStart = currentState.currentMonthStart.plus(DatePeriod(months = 1))
                updateState { copy(currentMonthStart = newMonthStart) }
                newMonthStart
            }
        }
        selectDate(newDate)
    }

    /**
     * Navigate to the previous period (day, week, or month depending on view mode).
     */
    fun navigatePrevious() {
        val currentState = currentState
        val newDate = when (currentState.viewMode) {
            CalendarViewMode.DAY -> currentState.selectedDate.minus(DatePeriod(days = 1))
            CalendarViewMode.WEEK -> currentState.selectedDate.minus(DatePeriod(days = 7))
            CalendarViewMode.MONTH -> {
                val newMonthStart = currentState.currentMonthStart.minus(DatePeriod(months = 1))
                updateState { copy(currentMonthStart = newMonthStart) }
                newMonthStart
            }
        }
        selectDate(newDate)
    }

    /**
     * Navigate to today's date.
     */
    fun navigateToToday() {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val monthStart = LocalDate(today.year, today.month, 1)
        updateState { copy(currentMonthStart = monthStart) }
        selectDate(today)
    }

    /**
     * Refresh visits data.
     */
    fun refresh() {
        loadVisitsForCurrentRange()
    }

    private fun loadVisitsForCurrentRange() {
        val state = currentState
        val (startDate, endDate) = getDateRangeForViewMode(state.selectedDate, state.viewMode)

        updateState { copy(isLoading = true, error = null) }

        visitRepository.getVisitsInDateRange(startDate, endDate)
            .onEach { visits ->
                val groupedVisits = visits.groupBy { it.scheduledDate }
                val visitsForSelected = groupedVisits[state.selectedDate] ?: emptyList()

                updateState {
                    copy(
                        visitsForDateRange = groupedVisits,
                        visitsForSelectedDate = visitsForSelected,
                        isLoading = false
                    )
                }
                _selectedDateVisits.value = visitsForSelected
            }
            .catch { e ->
                val exception = when (e) {
                    is AppException -> e
                    else -> AppException.UnknownException(
                        e.message ?: "Failed to load visits",
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
            .launchIn(viewModelScope)
    }

    private fun loadVisitsForDate(date: LocalDate) {
        val visits = currentState.visitsForDateRange[date] ?: emptyList()
        updateState { copy(visitsForSelectedDate = visits) }
        _selectedDateVisits.value = visits
    }

    private fun getDateRangeForViewMode(
        selectedDate: LocalDate,
        viewMode: CalendarViewMode
    ): Pair<LocalDate, LocalDate> {
        return when (viewMode) {
            CalendarViewMode.DAY -> {
                selectedDate to selectedDate
            }
            CalendarViewMode.WEEK -> {
                val dayOfWeek = selectedDate.dayOfWeek.ordinal
                val weekStart = selectedDate.minus(DatePeriod(days = dayOfWeek))
                val weekEnd = weekStart.plus(DatePeriod(days = 6))
                weekStart to weekEnd
            }
            CalendarViewMode.MONTH -> {
                val monthStart = LocalDate(selectedDate.year, selectedDate.month, 1)
                val nextMonth = monthStart.plus(DatePeriod(months = 1))
                val monthEnd = nextMonth.minus(DatePeriod(days = 1))
                monthStart to monthEnd
            }
        }
    }

    /**
     * Get visits count for a specific date (for calendar indicators).
     */
    fun getVisitsCountForDate(date: LocalDate): Int {
        return currentState.visitsForDateRange[date]?.size ?: 0
    }

    /**
     * Check if a date has any visits.
     */
    fun hasVisitsOnDate(date: LocalDate): Boolean {
        return (currentState.visitsForDateRange[date]?.size ?: 0) > 0
    }

    /**
     * Get week dates for the current selection.
     */
    fun getWeekDates(): List<LocalDate> {
        val selectedDate = currentState.selectedDate
        val dayOfWeek = selectedDate.dayOfWeek.ordinal
        val weekStart = selectedDate.minus(DatePeriod(days = dayOfWeek))

        return (0..6).map { offset ->
            weekStart.plus(DatePeriod(days = offset))
        }
    }

    /**
     * Get month dates for the current selection (includes padding days from prev/next months).
     */
    fun getMonthDates(): List<LocalDate?> {
        val monthStart = currentState.currentMonthStart
        val nextMonth = monthStart.plus(DatePeriod(months = 1))
        val monthEnd = nextMonth.minus(DatePeriod(days = 1))

        val result = mutableListOf<LocalDate?>()

        // Add padding for days before month start
        val startDayOfWeek = monthStart.dayOfWeek.ordinal
        repeat(startDayOfWeek) {
            result.add(null)
        }

        // Add all days of the month
        var currentDate = monthStart
        while (currentDate <= monthEnd) {
            result.add(currentDate)
            currentDate = currentDate.plus(DatePeriod(days = 1))
        }

        // Add padding to complete last week (optional)
        while (result.size % 7 != 0) {
            result.add(null)
        }

        return result
    }
}
