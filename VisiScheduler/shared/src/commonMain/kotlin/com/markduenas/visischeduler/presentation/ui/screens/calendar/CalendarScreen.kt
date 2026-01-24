package com.markduenas.visischeduler.presentation.ui.screens.calendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarViewDay
import androidx.compose.material.icons.filled.CalendarViewMonth
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.markduenas.visischeduler.domain.entities.Visit
import com.markduenas.visischeduler.presentation.state.UiEvent
import com.markduenas.visischeduler.presentation.viewmodel.scheduling.CalendarUiState
import com.markduenas.visischeduler.presentation.viewmodel.scheduling.CalendarViewModel
import com.markduenas.visischeduler.presentation.viewmodel.scheduling.CalendarViewMode
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/**
 * Main calendar screen with view mode toggle and visit display.
 *
 * @param viewModel The CalendarViewModel
 * @param onNavigateToSchedule Callback to navigate to schedule screen
 * @param onNavigateToVisitDetails Callback to navigate to visit details
 * @param modifier Modifier for the screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    onNavigateToSchedule: (LocalDate) -> Unit,
    onNavigateToVisitDetails: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is UiEvent.Navigate -> {
                    // Handle navigation based on route
                    if (event.route.startsWith("visitDetails/")) {
                        val visitId = event.route.removePrefix("visitDetails/")
                        onNavigateToVisitDetails(visitId)
                    }
                }
                else -> {}
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            CalendarTopBar(
                viewMode = uiState.viewMode,
                onViewModeChange = viewModel::setViewMode
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToSchedule(uiState.selectedDate) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Schedule Visit"
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                CalendarContent(
                    uiState = uiState,
                    viewModel = viewModel,
                    onVisitClick = { visit -> onNavigateToVisitDetails(visit.id) },
                    onTimeSlotClick = { time ->
                        // Navigate to schedule with pre-selected date and time
                        onNavigateToSchedule(uiState.selectedDate)
                    }
                )
            }

            // Error display
            uiState.error?.let { error ->
                ErrorBanner(
                    message = error.message ?: "An error occurred",
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }
    }
}

/**
 * Top app bar with view mode toggle.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarTopBar(
    viewMode: CalendarViewMode,
    onViewModeChange: (CalendarViewMode) -> Unit
) {
    TopAppBar(
        title = {
            Text("Calendar")
        },
        actions = {
            ViewModeSelector(
                selectedMode = viewMode,
                onModeSelected = onViewModeChange
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

/**
 * View mode selector (Day/Week/Month).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ViewModeSelector(
    selectedMode: CalendarViewMode,
    onModeSelected: (CalendarViewMode) -> Unit
) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier.padding(end = 8.dp)
    ) {
        CalendarViewMode.entries.forEachIndexed { index, mode ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = CalendarViewMode.entries.size
                ),
                onClick = { onModeSelected(mode) },
                selected = mode == selectedMode,
                icon = {
                    Icon(
                        imageVector = when (mode) {
                            CalendarViewMode.DAY -> Icons.Default.CalendarViewDay
                            CalendarViewMode.WEEK -> Icons.Default.CalendarViewWeek
                            CalendarViewMode.MONTH -> Icons.Default.CalendarViewMonth
                        },
                        contentDescription = mode.name
                    )
                }
            ) {
                // No label, icon only
            }
        }
    }
}

/**
 * Calendar content based on view mode.
 */
@Composable
private fun CalendarContent(
    uiState: CalendarUiState,
    viewModel: CalendarViewModel,
    onVisitClick: (Visit) -> Unit,
    onTimeSlotClick: (LocalTime) -> Unit
) {
    when (uiState.viewMode) {
        CalendarViewMode.DAY -> {
            DayViewContent(
                date = uiState.selectedDate,
                visits = uiState.visitsForSelectedDate,
                onVisitClick = onVisitClick,
                onTimeSlotClick = onTimeSlotClick,
                onPrevious = viewModel::navigatePrevious,
                onNext = viewModel::navigateNext
            )
        }
        CalendarViewMode.WEEK -> {
            WeekViewContent(
                weekDates = viewModel.getWeekDates(),
                selectedDate = uiState.selectedDate,
                visitsForWeek = uiState.visitsForDateRange,
                onDateSelected = viewModel::selectDate,
                onVisitClick = onVisitClick,
                onSwipeLeft = viewModel::navigateNext,
                onSwipeRight = viewModel::navigatePrevious
            )
        }
        CalendarViewMode.MONTH -> {
            MonthViewContent(
                monthDates = viewModel.getMonthDates(),
                monthStart = uiState.currentMonthStart,
                selectedDate = uiState.selectedDate,
                visitsForMonth = uiState.visitsForDateRange,
                onDateSelected = viewModel::selectDate,
                onVisitClick = onVisitClick,
                onPrevious = viewModel::navigatePrevious,
                onNext = viewModel::navigateNext,
                onTodayClick = viewModel::navigateToToday
            )
        }
    }
}

/**
 * Error banner component.
 */
@Composable
private fun ErrorBanner(
    message: String,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.medium
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(16.dp)
        )
    }
}

/**
 * Simplified CalendarScreen for use without ViewModel injection
 * (for preview or testing purposes).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreenPreview(
    selectedDate: LocalDate,
    viewMode: CalendarViewMode = CalendarViewMode.WEEK,
    visits: List<Visit> = emptyList(),
    onDateSelected: (LocalDate) -> Unit = {},
    onViewModeChange: (CalendarViewMode) -> Unit = {},
    onScheduleClick: () -> Unit = {},
    onVisitClick: (Visit) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            CalendarTopBar(
                viewMode = viewMode,
                onViewModeChange = onViewModeChange
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onScheduleClick) {
                Icon(Icons.Default.Add, contentDescription = "Schedule")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Text(
                text = "Calendar Preview",
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
