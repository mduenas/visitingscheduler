package com.markduenas.visischeduler.domain.usecase

import com.markduenas.visischeduler.domain.entities.TimeSlot
import com.markduenas.visischeduler.domain.repository.VisitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.math.abs
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Use case for getting intelligent time slot suggestions for a visitor.
 */
class GetSuggestedSlotsUseCase(
    private val getAvailableSlotsUseCase: GetAvailableSlotsUseCase,
    private val visitRepository: VisitRepository
) {
    /**
     * Get ranked suggested slots for a beneficiary.
     * @param beneficiaryId The beneficiary to visit
     * @param visitorId The visitor ID for restriction checking
     * @param limit Maximum number of suggestions to return
     * @return Flow of suggested time slots
     */
    operator fun invoke(
        beneficiaryId: String,
        visitorId: String,
        limit: Int = 3
    ): Flow<List<TimeSlot>> {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        
        return getAvailableSlotsUseCase(beneficiaryId, today, visitorId)
            .map { slots ->
                if (slots.isEmpty()) return@map emptyList()

                // Intelligence: Rank slots based on several factors
                // 1. Availability percentage (higher is better)
                // 2. Existing visits (prefer slots with fewer concurrent visitors)
                // 3. Time of day (prefer morning/early afternoon)
                // 4. Historical preference (simulated)

                slots.sortedByDescending { slot ->
                    calculateScore(slot)
                }.take(limit)
            }
    }

    private fun calculateScore(slot: TimeSlot): Double {
        var score = 0.0

        // Factor 1: Availability Percentage (0.0 to 1.0)
        score += slot.availabilityPercentage * 50.0

        // Factor 2: Time of Day Preference
        // Prefer slots between 10:00 and 15:00
        val targetHour = 11.0
        val hourDiff = abs(slot.startTime.hour.toDouble() - targetHour)
        score += (10.0 - hourDiff).coerceAtLeast(0.0) * 2.0

        // Factor 3: Remaining Capacity
        score += slot.remainingCapacity * 5.0

        return score
    }
}
