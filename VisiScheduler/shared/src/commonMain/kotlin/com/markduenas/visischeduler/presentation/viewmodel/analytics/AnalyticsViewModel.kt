package com.markduenas.visischeduler.presentation.viewmodel.analytics

import com.markduenas.visischeduler.domain.repository.UserRepository
import com.markduenas.visischeduler.domain.repository.VisitRepository
import com.markduenas.visischeduler.domain.repository.VisitStatistics
import com.markduenas.visischeduler.presentation.viewmodel.BaseViewModel

/**
 * ViewModel for the Analytics screen. Loads visit statistics from the repository
 * and derives computed rates for display.
 */
class AnalyticsViewModel(
    private val visitRepository: VisitRepository,
    private val userRepository: UserRepository
) : BaseViewModel<AnalyticsUiState>(AnalyticsUiState()) {

    init {
        loadAnalytics()
    }

    fun loadAnalytics() {
        launchSafe {
            updateState { copy(isLoading = true, error = null) }

            visitRepository.getVisitStatistics()
                .fold(
                    onSuccess = { stats ->
                        updateState {
                            copy(
                                isLoading = false,
                                statistics = stats,
                                completionRate = computeRate(stats.completedVisits, stats.totalVisits),
                                noShowRate = computeRate(stats.noShowVisits, stats.totalVisits),
                                cancellationRate = computeRate(stats.cancelledVisits, stats.totalVisits)
                            )
                        }
                    },
                    onFailure = { error ->
                        updateState {
                            copy(
                                isLoading = false,
                                error = error.message ?: "Failed to load analytics"
                            )
                        }
                    }
                )
        }
    }

    fun refresh() = loadAnalytics()

    private fun computeRate(part: Int, total: Int): Float =
        if (total == 0) 0f else (part.toFloat() / total.toFloat()).coerceIn(0f, 1f)
}

/**
 * UI state for the analytics screen.
 */
data class AnalyticsUiState(
    val isLoading: Boolean = true,
    val statistics: VisitStatistics? = null,
    val completionRate: Float = 0f,
    val noShowRate: Float = 0f,
    val cancellationRate: Float = 0f,
    val error: String? = null
) {
    val hasData: Boolean get() = statistics != null
    val totalVisits: Int get() = statistics?.totalVisits ?: 0
    val completedVisits: Int get() = statistics?.completedVisits ?: 0
    val cancelledVisits: Int get() = statistics?.cancelledVisits ?: 0
    val noShowVisits: Int get() = statistics?.noShowVisits ?: 0
    val upcomingVisits: Int get() = statistics?.upcomingVisits ?: 0
    val pendingVisits: Int get() = statistics?.pendingVisits ?: 0
    val completionRatePct: Int get() = (completionRate * 100).toInt()
    val noShowRatePct: Int get() = (noShowRate * 100).toInt()
    val cancellationRatePct: Int get() = (cancellationRate * 100).toInt()
}
